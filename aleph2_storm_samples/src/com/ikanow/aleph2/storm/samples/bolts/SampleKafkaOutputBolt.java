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

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

/**
 * Output a jsonNode to the iEnrichmentStreamingTopology
 * 
 * @author Burch
 *
 */
public class SampleKafkaOutputBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;
	public static final Logger logger = LoggerFactory.getLogger(SampleKafkaOutputBolt.class);
	private IEnrichmentModuleContext context;
	
	public SampleKafkaOutputBolt() {	
		//TODO get IEnrichmentModuleContext
	}
	
	
	@Override
	public void execute(Tuple tuple) {
		//this tuple contains 3 fields, keyA, keyB, message, push these into a jsonNodeObject and return						
		if ( tuple.size() == 3 ) {
			String keyA = tuple.getString(0);
			String keyB = tuple.getString(1);
			String message = tuple.getString(2);
			
			try {
				JsonNode output_node = createJsonNode(keyA, keyB, message);
				//TODO send to service we want or is there some json output?
				long id = 0; //TODO what is this suppose to be?
				context.emitImmutableObject(id, output_node, Optional.empty(), Optional.empty(), Optional.empty());
				_collector.ack(tuple);
			} catch (Exception e) {
				logger.error("Error parsing json",e);
				//probably ack a failure
				_collector.fail(tuple);
			}
		} else {
			logger.error("tuple size was not 3");
			//probably ack a failure
			_collector.fail(tuple);
		}		
	}

	private JsonNode createJsonNode(String keyA, String keyB, String message) throws JsonProcessingException, IOException {
		ObjectNode objNode = (ObjectNode) new ObjectMapper().readTree(message);
		objNode.put("modifiedKeyA", keyA);
		objNode.put("modifiedKeyB", keyB);		
		return objNode;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		//we don't emit from this bolt ever so we have no output fields
		declarer.declare(new Fields());
	}

}
