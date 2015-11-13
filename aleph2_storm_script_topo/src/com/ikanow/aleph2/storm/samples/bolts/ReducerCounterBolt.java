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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class ReducerCounterBolt extends BaseRichBolt {
	
	private static final long serialVersionUID = -754177901046983751L;
	private OutputCollector _collector;	
	private static final Logger logger = LogManager.getLogger(JavaScriptBolt.class);
	private Map<String,Long> networkCounters = new HashMap<String,Long>();
	
	protected int threshold = 10;
			
	@Override
	public void execute(Tuple tuple) {
		String val0 = tuple.getString(0);
		logger.debug("IndexerBolt Received tuple:"+tuple+" val0:"+val0);

/*		String line = tuple.getString(0);
		Matcher matcher = pattern.matcher(line);
		while ( matcher.find() ) {
			_collector.emit(tuple, new Values( matcher.group(0).trim()));
		}		
				
		//always ack the tuple to acknowledge we've processed it, otherwise a fail message will be reported back
		//to the spout
		 * */
		 String ipNo = tuple.getString(0);
		 String network = tuple.getString(1);
		 @SuppressWarnings("unused")
		String subnet = tuple.getString(2);
		 Long netCount = networkCounters.get(network);
		 netCount++;
		 networkCounters.put(network, netCount);
		 if(netCount> threshold){
			_collector.emit(tuple, new Values( ipNo, netCount));
		 }
		_collector.ack(tuple);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		 declarer.declare(new Fields("ipNo","ipCount")); 
	}
}
