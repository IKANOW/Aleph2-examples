/*******************************************************************************
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
 *******************************************************************************/
package com.ikanow.aleph2.example.flume_harvester.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;
import au.com.bytecode.opencsv.CSVParser;

import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.ISearchIndexService;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IDataServiceProvider;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IDataWriteService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.Patterns;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.data_model.utils.TimeUtils;
import com.ikanow.aleph2.data_model.utils.Tuples;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean.OutputConfig.CsvConfig;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean.OutputConfig.JsonConfig;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeUtils;

import fj.data.Either;
import fj.data.Validation;

/** Harvest sink - will provide some basic parsing (and maybe JS manipulation functionality in the future)
 * @author alex
 */
public class FlumeHarvesterSink extends AbstractSink implements Configurable {
	final static protected Logger _logger = LogManager.getLogger();
	
	final protected static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	IHarvestContext _context;	
	Optional<FlumeBucketConfigBean> _config;
	DataBucketBean _bucket;
	Optional<String> _time_field;
	
	/* (non-Javadoc)
	 * @see org.apache.flume.conf.Configurable#configure(org.apache.flume.Context)
	 */
	@Override
	public void configure(Context flume_context) {
		try {
			_context = Lambdas.wrap_u(() ->
						ContextUtils.getHarvestContext(
								FlumeUtils.decodeSignature(flume_context.getString("context_signature", ""))))
						.get();
	
			_bucket = _context.getBucket().get();
			
			_logger.debug("Bucket = " + BeanTemplateUtils.toJson(_bucket));
			
			// Get config from bucket 
			_config = Optional.of(_bucket)
								.map(b -> b.harvest_configs())
								.filter(h -> !h.isEmpty())
								.map(h -> h.iterator().next())
								.map(hcfg -> hcfg.config())
								.map(hmap -> BeanTemplateUtils.from(hmap, FlumeBucketConfigBean.class).get())
								;
	
			_logger.debug("_config = " + _config.map(BeanTemplateUtils::toJson).map(JsonNode::toString).orElse("(not present)"));		
			
			_time_field = _config.flatMap(cfg -> Optionals.of(() -> cfg.output().add_time_with_name())).map(Optional::of) // prio #1: if manually specified
									.orElse(Optionals.of(() -> _bucket.data_schema().temporal_schema())
													.filter(schema -> Optional.ofNullable(schema.enabled()).orElse(true)) // prio #2: (but only if temporal enabled)...
													.map(schema -> schema.time_field()) // ...use the time field
											)
					;
			
			_time_field = Optionals.of(() -> _bucket.data_schema().temporal_schema().time_field());
			
			//DEBUG
			_logger.debug("_time_field = " + _time_field);		
		}
		catch (Throwable t) {
			_logger.error("Error initializing flume", t);
			throw t;
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.flume.Sink#process()
	 */
	@Override
	public Status process() throws EventDeliveryException {
		Status status = null;

		// Start transaction
		final Channel ch = getChannel();
		final Transaction txn = ch.getTransaction();
		txn.begin();
		try {
			// This try clause includes whatever Channel operations you want to do

			final Event event = ch.take();

			final Optional<JsonNode> maybe_json_event = 
								getEventJson(event, _config)
										// Extra step
										.map(json -> _time_field
														.filter(tf -> !json.has(tf)) // (ie filter out JSON objects with the timestamp field, those are passed unchanged by the orElse 
														.<JsonNode>map(tf -> ((ObjectNode) json).put(tf, LocalDateTime.now().toString())) // put the timestamp field in
													.orElse(json))
										;
			
			maybe_json_event.ifPresent(json_event -> {
				if (_config.map(cfg -> cfg.output()).map(out -> out.direct_output()).isPresent())
				{
					this.directOutput(json_event, _config.get(), _bucket);
				}
				else { //TODO: streaming vs batch 				
					_context.sendObjectToStreamingPipeline(Optional.empty(), Either.left(json_event));
				}
			});
		
			txn.commit();
			status = Status.READY;
		} catch (Throwable t) {
			//DEBUG
			//_logger.warn("Error", t);
			
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
		if (null == evt) { // (seem to get lots of these)
			return Optional.empty();
		}
		if (config.isPresent()) {
			final FlumeBucketConfigBean cfg = config.get();
			if (null != cfg.output()) {
				if (null != cfg.output().csv() && cfg.output().csv().enabled()) {
					return getCsvEventJson(evt, cfg.output().csv());
				}
				else if (null != cfg.output().json() && cfg.output().json().enabled()) {
					return getJsonEventJson(evt, cfg.output().json());
				}
			}
		}
		// Backstop:
		return getDefaultEventJson(evt);
	}
	
	/** Default output - creates a JSON object with "message" containing the body of the event, and a timestamp
	 * TODO (ALEPH-10): make @timestamp derived (eg from temporal schema?)
	 * @param evt
	 * @return
	 */
	protected Optional<JsonNode> getDefaultEventJson(final Event evt) {
		try {
			final JsonNode initial = _mapper.convertValue(evt.getHeaders(), JsonNode.class);
			return Optional.of(((ObjectNode)initial).put("message", new String(evt.getBody(), "UTF-8")));
		}
		catch (Exception e) {
			return Optional.empty();
		}		
	}
	
	/** State object for JSON output
	 * @author Alex
	 */
	public static class JsonState {
		public JsonConfig.JsonPolicy policy;
		public String include_body_with_name;
	};
	protected final SetOnce<JsonState> _json = new SetOnce<>();
	/** Generates a JSON object assuming the event body is a JSON object 
	 * @param evt
	 * @param config
	 * @return
	 */
	protected Optional<JsonNode> getJsonEventJson(final Event evt, JsonConfig config) {
		try {
			// Lazy initialization
			if (!_json.isSet()) {
				final JsonState json = new JsonState();
				json.policy = config.json_policy();
				json.include_body_with_name = Optional.ofNullable(config.include_body_with_name()).orElse("message");
				_json.set(json);
			}
			
			// Different cases depending on policy
			
			final JsonState json = _json.get();
			if (JsonConfig.JsonPolicy.body == json.policy) {
				final String body = new String(evt.getBody(), "UTF-8");
				final JsonNode initial = _mapper.readValue(body, JsonNode.class);
				return Optional.of(initial);
			}
			else if (JsonConfig.JsonPolicy.body_plus_headers == json.policy) {
				final String body = new String(evt.getBody(), "UTF-8");
				final ObjectNode initial = (ObjectNode) _mapper.readValue(body, JsonNode.class);
				final JsonNode to_return = evt.getHeaders().entrySet().stream().reduce(initial, (acc, v) -> acc.put(v.getKey(), v.getValue()), (acc1, acc2) -> acc1);
				return Optional.of(to_return);				
			}
			else if (JsonConfig.JsonPolicy.event == json.policy) {
				final String body = new String(evt.getBody(), "UTF-8");
				final ObjectNode initial = (ObjectNode) _mapper.convertValue(evt.getHeaders(), JsonNode.class);
				return Optional.of(initial.put(json.include_body_with_name, body));
			}
			else if (JsonConfig.JsonPolicy.event_no_body == json.policy) {				
				final JsonNode initial = _mapper.convertValue(evt.getHeaders(), JsonNode.class);
				return Optional.of(initial);
			}
			else return Optional.empty(); // not support/possible
			
		}
		catch (Throwable t) {
			return Optional.empty();
		}
	}
	
	/** State object for CSV output
	 * @author Alex
	 */
	public static class CsvState {
		public CSVParser parser = null;
		public ArrayList<String> headers;
		public Pattern ignore_regex;
		public Map<String, String> type_map;
	};
	protected final SetOnce<CsvState> _csv = new SetOnce<>();
	
	/** Generates a JSON object assuming the event body is a CSV 
	 * @param evt
	 * @param config
	 * @return
	 */
	protected Optional<JsonNode> getCsvEventJson(final Event evt, CsvConfig config) {
		if (!_csv.isSet()) {
			final CsvState csv = new CsvState();
			// Lazy initialization:
			csv.parser = new CSVParser(Optional.ofNullable(config.separator().charAt(0)).orElse(','),
									Optional.ofNullable(config.quote_char().charAt(0)).orElse('"'),
									Optional.ofNullable(config.escape_char().charAt(0)).orElse('\\'));
			csv.headers = new ArrayList<String>(Optionals.ofNullable(config.header_fields()));
			
			csv.type_map = !config.non_string_types().isEmpty()
					? config.non_string_types()
					: config.non_string_type_map().entrySet().stream() // (reverse the order of the map to get fieldname -> type)
							.<Tuple2<String, String>>flatMap(kv -> kv.getValue().stream().map(v -> Tuples._2T(kv.getKey(), v)))
							.collect(Collectors.toMap(t2 -> t2._2().toString(), t2 -> t2._1().toString()));								
					
			Optional.ofNullable(config.ignore_regex()).ifPresent(regex -> csv.ignore_regex = Pattern.compile(regex));
			_csv.set(csv);
		}
		try {
			final CsvState csv = _csv.get();
			final String line = new String(evt.getBody(), "UTF-8");
			if ((null != csv.ignore_regex) && csv.ignore_regex.matcher(line).matches()) {
				return Optional.empty();
			}
			final String[] fields = csv.parser.parseLine(line);
			final ObjectNode ret_val = StreamUtils.zipWithIndex(Arrays.stream(fields))
				.reduce(_mapper.createObjectNode(), 
						(acc, v) -> {
							if (v.getIndex() >= csv.headers.size()) return acc;
							else {
								final String field_name = csv.headers.get((int)v.getIndex());
								if ((null == field_name) || field_name.isEmpty()) {
									return acc;
								}
								else {
									try {
										return Patterns.match((String) csv.type_map.get(field_name)).<ObjectNode>andReturn()
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
	
	/** State object for direct output
	 * @author Alex
	 */
	public static class DirectOutputState {
		public Optional<IDataWriteService<JsonNode>> search_index_service;
		public Optional<IDataWriteService.IBatchSubservice<JsonNode>> batch_search_index_service;
		public Optional<IDataWriteService<JsonNode>> storage_service;
		public Optional<IDataWriteService.IBatchSubservice<JsonNode>> batch_storage_service;
		public boolean also_stream;
	}
	final protected SetOnce<DirectOutputState> _direct = new SetOnce<>(); 
	
	/** Outputs data directly into the Aleph2 data services without going via batch or streaming enrichment
	 * @param json
	 * @param config
	 */
	protected void directOutput(final JsonNode json, FlumeBucketConfigBean config, final DataBucketBean bucket) {
		try {
			// Lazy initialization
			
			if (!_direct.isSet()) { 
				final DirectOutputState direct = new DirectOutputState();
				final Optional<ISearchIndexService> search_index_service = 
						(config.output().direct_output().contains("search_index_service") 
								? _context.getServiceContext().getSearchIndexService()
								: Optional.empty());
				
				direct.search_index_service = 
						search_index_service
							.flatMap(IDataServiceProvider::getDataService)
							.flatMap(s -> s.getWritableDataService(JsonNode.class, bucket, Optional.empty(), Optional.empty()))
							;
				
				direct.batch_search_index_service = direct.search_index_service.flatMap(IDataWriteService::getBatchWriteSubservice);
				
				final Optional<IStorageService> storage_service = 
						(config.output().direct_output().contains("storage_service")
								? Optional.of(_context.getServiceContext().getStorageService())
								: Optional.empty());
						
				direct.storage_service = 
						storage_service
							.flatMap(IDataServiceProvider::getDataService)
							.flatMap(s -> s.getWritableDataService(JsonNode.class, bucket, Optional.of(IStorageService.StorageStage.processed.toString()), Optional.empty()))
							;
	
				direct.batch_storage_service = direct.storage_service.flatMap(IDataWriteService::getBatchWriteSubservice);
				
				direct.also_stream = config.output().direct_output().contains("stream");
						
				_direct.set(direct);
			}
			final DirectOutputState direct = _direct.get();
			
			// Search index service
			
			direct.search_index_service.ifPresent(search_index_service -> {
				if (direct.batch_search_index_service.isPresent()) {
					direct.batch_search_index_service.get().storeObject(json);
				}
				else {
					search_index_service.storeObject(json);
				}
			});
			
			// Storage service
			
			direct.storage_service.ifPresent(storage_service -> {
				if (direct.batch_storage_service.isPresent()) {
					direct.batch_storage_service.get().storeObject(json);
				}
				else {
					storage_service.storeObject(json);
				}
			});
	
			// Streaming
			
			if (direct.also_stream) {
				_context.sendObjectToStreamingPipeline(Optional.empty(), Either.left(json));
			}
		}
		catch (Throwable t) {
			//DEBUG
			//_logger.warn("Error", t);
		}
	}
}
