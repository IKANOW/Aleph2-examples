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
package com.ikanow.aleph2.storm.samples.spouts;

import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * Spout sends out timer messages  every  
 * @author Joern Freydank
 *
 */
public class TimerSpout extends BaseRichSpout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6230942751188930047L;
	protected long timeoutMillis;
	protected long lastTime = -1;
	private SpoutOutputCollector _collector;

	public TimerSpout(long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void nextTuple() {
		long now = System.currentTimeMillis();
		if(now-lastTime>=timeoutMillis){			
			_collector.emit(new Values(now));
			lastTime = now;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("timer"));
	}

}
