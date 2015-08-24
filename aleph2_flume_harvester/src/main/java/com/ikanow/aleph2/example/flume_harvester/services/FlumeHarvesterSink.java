/******************************************************************************
 * Copyright 2015, The IKANOW Open Source Project.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.aleph2.example.flume_harvester.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;

import au.com.bytecode.opencsv.CSVParser;

import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.Patterns;
import com.ikanow.aleph2.data_model.utils.TimeUtils;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean.OutputConfig.CsvConfig;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeUtils;

import fj.data.Validation;

/** Harvest sink - will provide some basic parsing (and maybe JS manipulation functionality in the future)
 * @author alex
 */
public class FlumeHarvesterSink extends AbstractSink implements Configurable {
	final protected static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	IHarvestContext _context;	
	Optional<FlumeBucketConfigBean> _config;
	
	/* (non-Javadoc)
	 * @see org.apache.flume.conf.Configurable#configure(org.apache.flume.Context)
	 */
	@Override
	public void configure(Context flume_context) {
		_context = Lambdas.wrap_u(() ->
					ContextUtils.getHarvestContext(
							FlumeUtils.decodeSignature(flume_context.getString("context_signature", ""))))
					.get();

		// Get config from bucket 
		_config = _context.getBucket()
							.map(b -> b.harvest_configs()).filter(h -> h.isEmpty())
							.map(h -> h.iterator().next()).map(hcfg -> hcfg.config())
							.map(hmap -> BeanTemplateUtils.from(hmap, FlumeBucketConfigBean.class).get())
							;
	}

	/* (non-Javadoc)
	 * @see org.apache.flume.Sink#process()
	 */
	@Override
	public Status process() throws EventDeliveryException {
		Status status = null;

		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		txn.begin();
		try {
			// This try clause includes whatever Channel operations you want to do

			Event event = ch.take();

			Optional<JsonNode> maybe_json_event = getEventJson(event, _config);
			maybe_json_event.ifPresent(json_event -> 
				_context.sendObjectToStreamingPipeline(Optional.empty(), json_event));

			txn.commit();
			status = Status.READY;
		} catch (Throwable t) {
			txn.rollback();

			// Log exception, handle individual exceptions as needed

			status = Status.BACKOFF;

			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error)t;
			}
		} finally {
			txn.close();
		}
		return status;
	}

	/** Uses whatever parser is configured to create a JsonNode out of the object
	 *  TODO (ALEPH-10): for now just does a simple JSON mapping
	 * @param e
	 * @param config
	 * @return
	 */
	protected Optional<JsonNode> getEventJson(final Event evt, final Optional<FlumeBucketConfigBean> config) {
		if (config.isPresent()) {
			final FlumeBucketConfigBean cfg = config.get();
			if (null != cfg.output()) {
				if (null != cfg.output().csv()) {
					return getCsvEventJson(evt, cfg.output().csv());
				}
			}
		}
		// Backstop:
		return getDefaultEventJson(evt);
	}
	protected Optional<JsonNode> getDefaultEventJson(final Event evt) {
		try {
			final JsonNode initial = _mapper.convertValue(evt.getHeaders(), JsonNode.class);
			return Optional.of(((ObjectNode)initial).put("message", new String(evt.getBody(), "UTF-8")).put("@timestamp", LocalDateTime.now().toString()));
		}
		catch (Exception e) {
			return Optional.empty();
		}		
	}
	
	protected CSVParser _parser = null;
	protected ArrayList<String> _headers;
	protected Pattern _ignore_regex; 
	
	/** Generates a 
	 * @param evt
	 * @param config
	 * @return
	 */
	protected Optional<JsonNode> getCsvEventJson(final Event evt, CsvConfig config) {
		if (null == _parser) {
			// Lazy initialization:
			_parser = new CSVParser(Optional.ofNullable(config.separator().charAt(0)).orElse(','),
									Optional.ofNullable(config.quote_char().charAt(0)).orElse('"'),
									Optional.ofNullable(config.escape_char().charAt(0)).orElse('\\'));
			_headers = new ArrayList<String>(Optionals.ofNullable(config.header_fields()));
			
			Optional.ofNullable(config.ignore_regex()).ifPresent(regex -> _ignore_regex = Pattern.compile(regex));
		}
		try {
			final String line = new String(evt.getBody(), "UTF-8");
			if ((null != _ignore_regex) && _ignore_regex.matcher(line).matches()) {
				return Optional.empty();
			}
			final String[] fields = _parser.parseLine(line);
			final ObjectNode ret_val = StreamUtils.zipWithIndex(Arrays.stream(fields))
				.reduce(_mapper.createObjectNode(), 
						(acc, v) -> {
							if (v.getIndex() >= _headers.size()) return acc;
							else {
								final String field_name = _headers.get((int)v.getIndex());
								if ((null == field_name) || field_name.isEmpty()) {
									return acc;
								}
								else {
									try {
										return Patterns.match((String) config.non_string_types().get(field_name)).<ObjectNode>andReturn()
														.when(t -> null == t, __ -> acc.put(field_name, v.getValue())) //(string)
														.when(t -> t.equalsIgnoreCase("long"),		__ -> acc.put(field_name, Long.parseLong(v.getValue())))
														.when(t -> t.equalsIgnoreCase("int") || t.equalsIgnoreCase("integer"), 
																									__ -> acc.put(field_name, Integer.parseInt(v.getValue())))
														.when(t -> t.equalsIgnoreCase("double") || t.equalsIgnoreCase("numeric"), 
																									__ -> acc.put(field_name, Double.parseDouble(v.getValue())))
														.when(t -> t.equalsIgnoreCase("float"),		__ -> acc.put(field_name, Float.parseFloat(v.getValue())))
														.when(t -> t.equalsIgnoreCase("boolean"),	__ -> acc.put(field_name, Boolean.parseBoolean(v.getValue())))
														.when(t -> t.equalsIgnoreCase("hex"),		__ -> acc.put(field_name, Long.parseLong(v.getValue(), 16)))
														.when(t -> t.equalsIgnoreCase("date"),		__ -> { 
															Validation<String, Date> res = TimeUtils.getSchedule(v.getValue(), Optional.empty());
															return res.validation(left -> acc, right -> acc.put(field_name, right.toString()));
														})
														.otherwise(__ -> acc.put(field_name, v.getValue())) // (string)
														;
									}
									catch (Exception e) {
										return acc;
									}
									//return acc.put(field_name, v.getValue());
								}
							}
						},
						(acc1, acc2) -> acc1); // (can't occur in practice)
			;
			return Optional.of(ret_val);
		}
		catch (Exception e) {
			return Optional.empty();
		}		
	}
}
