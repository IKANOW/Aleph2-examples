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
package com.ikanow.aleph2.storm.samples.bolts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SampleProxyParserBolt extends BaseRichBolt {
	
	private static final long serialVersionUID = -754177901046983750L;
	private OutputCollector _collector;
	//private Logger logger = LogManager.getLogger();
	private static final Pattern QUOTE_PATTERN = Pattern.compile("[\"]+");
	private static final Pattern SPACE_PATTERN = Pattern.compile("[ ]+");

	private Date assignedDate = null;
	private final SimpleDateFormat elasticsearchDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final String MESSAGE_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	private static final String MESSAGE_DATE_ONLY_PATTERN = "yyyy-MM-dd";
	private final SimpleDateFormat messageDateTimePattern = new SimpleDateFormat(MESSAGE_DATE_TIME_PATTERN);
	private final SimpleDateFormat messageDateOnlyPattern = new SimpleDateFormat(MESSAGE_DATE_ONLY_PATTERN);
	
	
	@Override
	public void execute(Tuple tuple) {
		Map<String, Object> parsed_entry = null;
		try {
			parsed_entry = parseEntry(tuple.getString(0));
		} catch (Exception e) {			
			e.printStackTrace();
		}
		if ( parsed_entry != null ) {
			_collector.emit(tuple, new Values(parsed_entry));			
		}
		//always ack the tuple to acknowledge we've processed it, otherwise a fail message will be reported back
		//to the spout
		_collector.ack(tuple);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("proxy_parsed"));
	}
	
	public Map<String, Object> parseEntry(String entry) throws Exception {

		Map<String, Object> returnObj = null;
		
		try {
			if (entry == null) {
				throw new Exception("Entry cannot be null!");
			}
			
			if (entry.isEmpty()) {
				//logger.trace("Skipping blank entry.");
				return null;
			}
			
			if (entry.charAt(0) == '#') { // skip commented lines				
				//logger.trace("Line Commented out, skipping {}", entry);
				return null;
			}

			returnObj = new HashMap<String, Object>();
			
			String[] quote_tokens = QUOTE_PATTERN.split(entry);
			//if there were no quotes (not sure its possible), just push the entire line in
			if (quote_tokens.length == 0) {
				quote_tokens = new String[] { entry };
			}

			//loop over every quote section, splitting out the text blocks
			//every odd index will be a quote section, and should just be
			//taken as one entry e.g.
			//term1 term2 "term3a term3b" term4 term5
			// so when split we get [term1 term2, term3a term3b, term4 term5]
			int token_counter = 0;
			for ( int i = 0; i < quote_tokens.length; i++ ) {
				String [] tokens;
				if ( i % 2 == 1 ) { //if odd
					tokens = new String[1];
					tokens[0] = quote_tokens[i];
				} else {
					tokens = SPACE_PATTERN.split(quote_tokens[i].trim());
				}

				//handle date separately, it will be i==0, tokens[0],[1] case
				int token_start = 0;
				if ( i == 0 ) {
					Date es_date = null;
					if (assignedDate != null) {
						// Use the assigned date instead of the date portion of the entry timestamp (still parse the time portion of the entry timestamp)
						String time_string = tokens[1];
						String date_string = messageDateOnlyPattern.format(assignedDate);
						String fullDateTimeString = date_string + " " + time_string;
						es_date = messageDateTimePattern.parse(fullDateTimeString);
					} else {
						String fullDateTimeString = tokens[0] + " " + tokens[1];
						es_date = messageDateTimePattern.parse(fullDateTimeString);
					}
					String es_date_string = elasticsearchDateFormat.format(es_date);
					returnObj.put(SampleProxyLogEntry.TIMESTAMP, es_date_string);

					token_start=2;
					token_counter=2;
				}

				for (int j = token_start; j < tokens.length; j++) {
					String paramName = SampleProxyLogEntry.getParamName(token_counter);
					String token = tokens[j];
					switch (token_counter) {
						case 11:
						case 21:
							try {
								//parse as int
								int number_entry = Integer.parseInt(token);
								returnObj.put(paramName, number_entry);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							break;
						case 2:
						case 7:
						case 18:
							try {
								//parse as long
								long number_entry = Long.parseLong(token);
								returnObj.put(paramName, number_entry);
							} catch (Exception ex) {
								ex.printStackTrace();
								//logger.error("Error parsing: " + paramName + "=" + token + " as a long", ex);
							}
							break;
						default:
							//insert as string, make sure only '-' is considered null 
							if (!(token == null || token.equals("-"))) {
								returnObj.put(paramName, token);
							}
							break;
					}

					token_counter++;
				}
			}
			returnObj.put(SampleProxyLogEntry.MESSAGE, entry);
		} catch (Exception e) {
			throw new Exception("Exception while parsing message: \"" + entry + "\"!  Skipping message...", e);
		}
		return returnObj;
	}

}
