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

import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SampleEnrichmentBolt extends BaseRichBolt {
	
	private static final long serialVersionUID = -754177901046983751L;
	private OutputCollector _collector;	
	
	@Override
	public void execute(Tuple tuple) {
		//collect the fields I need from the tuple
		String keyA = tuple.getString(0);
		String keyB = tuple.getString(1);
		String message = tuple.getString(2);
		
		//perform some enrichment, either from external source or just some processing
		String enrichmentKeyA = doEnrichment(keyA, keyB);
		String enrichmentKeyB = doEnrichment(keyB, keyA);
		
		//emit out the additional fields
		_collector.emit(tuple, new Values(keyA, keyB, enrichmentKeyA, enrichmentKeyB, message));	
				
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
		declarer.declare(new Fields("keyA","keyB","enrichmentKeyA","enrichmentKeyB","message"));
	}

	private static String doEnrichment(String A, String B) {
		return A.concat(B);
	}
}
