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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SampleKafkaBolt extends BaseRichBolt {
	
	private static final long serialVersionUID = -754177901046983751L;
	private OutputCollector _collector;	
	public static final Logger logger = LoggerFactory.getLogger(SampleKafkaBolt.class);
	
	@Override
	public void execute(Tuple tuple) {
		String kafka_input = tuple.getString(0);
		logger.error("tuple: " + kafka_input);
		//TODO take kafka_input and convert to a JSONNode
		try {
			JsonNode jsonNode = new ObjectMapper().readTree(kafka_input);
			//TODO take jsonnode and grab fields I'll need in my topology, stick message as last field
			//We do not want to pass around the json node because it will get serialized/deserialized in every bolt			
			String keyA = jsonNode.get("keyA").toString();
			String keyB = jsonNode.get("keyB").toString();
			logger.error("Emitting: a: " + keyA + " b: " +keyB + " msg: " + kafka_input);
			_collector.emit(tuple, new Values(keyA, keyB, kafka_input));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		declarer.declare(new Fields("keyA","keyB","message"));
	}
}
