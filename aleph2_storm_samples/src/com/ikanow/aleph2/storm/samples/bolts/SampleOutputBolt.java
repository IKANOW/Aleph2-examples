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
******************************************************************************/
package com.ikanow.aleph2.storm.samples.bolts;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.utils.ContextUtils;

import fj.data.Either;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SampleOutputBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;
	private String harvest_signature;

	public SampleOutputBolt(String harvest_signature) {
		this.harvest_signature = harvest_signature;
	}
	
	@Override
	public void execute(Tuple tuple) {
		String line = tuple.getString(0);
		String keyA = tuple.getString(1);
		String keyB = tuple.getString(2);
		//convert string to a map to send to streaming pipeline
		Map<String, Object> parsed_entry = new HashMap<String, Object>();
		parsed_entry.put("line", line);
		parsed_entry.put("keyA", keyA);
		parsed_entry.put("keyB", keyB);
		
		//instead of emiting the tuple, we save it to the harvest context
		try {
			//System.out.println("sending: " + parsed_entry.toString() + " to stream");			
			IHarvestContext harvest_context = ContextUtils.getHarvestContext(harvest_signature);
			harvest_context.sendObjectToStreamingPipeline(Optional.empty(), Either.right(parsed_entry));
			
		} catch (Exception e) {
			//TODO handle failing to get harvest context
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
		declarer.declare(new Fields());
	}

}
