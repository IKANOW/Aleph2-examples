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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.ikanow.aleph2.storm.samples.script.CompiledScriptFactory;
import com.ikanow.aleph2.storm.samples.script.NoSecurityManager;
import com.ikanow.aleph2.storm.samples.script.PropertyBasedScriptProvider;

public class JavaScriptBolt extends BaseRichBolt {
	private static final Logger logger = LogManager.getLogger(JavaScriptBolt.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -17206092588932701L;
	private OutputCollector _collector;	
	protected transient CompiledScriptFactory compiledScriptFactory = null;
	private String propertyFileName;
	
	protected static String CHECK_CALL = "check();";
	protected static String SPLITIP_CALL = "splitIP();";
	
	
	public JavaScriptBolt(String propertyFileName){		
		
		this.propertyFileName  = propertyFileName;
		
	}
	
	protected CompiledScriptFactory getCompiledScriptFactory(){
		if(compiledScriptFactory == null){
			this.compiledScriptFactory = new CompiledScriptFactory(new PropertyBasedScriptProvider(propertyFileName){
				@Override
				public void init(String propertyFile) {
					super.init(propertyFile);
					scriptlets.add(CHECK_CALL);
					scriptlets.add(SPLITIP_CALL);					
				}
				
			}, new NoSecurityManager());

			compiledScriptFactory.executeCompiledScript(CompiledScriptFactory.GLOBAL);
		}
		return compiledScriptFactory;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void execute(Tuple tuple) {
		String val0 = tuple.getString(0);
		logger.debug("JavaScriptBolt Received tuple:"+tuple+" val0:"+val0);
		LinkedHashMap<String, Object> tupelMap =tupleToLinkedHashMap(tuple);
		String ip = (String) tupelMap.get("str");
		if(ip!=null){
//		Object retVal = getCompiledScriptFactory().executeCompiledScript(CHECK_CALL,"_ip",val0);
		Object retVal = getCompiledScriptFactory().executeCompiledScript(SPLITIP_CALL,"_ip",ip);
		logger.debug("JavaScriptBolt Result from Script:"+retVal);
		if(retVal instanceof Map){			
			Map m = (Map)retVal;
			String ipNo = (String)m.get("ipNo");
			String network = (String)m.get("network");
			String subnet = (String)m.get("subnet");
			if(network!=null && subnet!=null){
			_collector.emit(tuple, new Values(network , subnet));
			}
		}
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
		declarer.declare(new Fields("ipNo","network","subnet"));
	}
	
	public static LinkedHashMap<String, Object> tupleToLinkedHashMap(final Tuple t) {
		return StreamSupport.stream(t.getFields().spliterator(), false)
							.collect(Collectors.toMap(f -> f, f -> t.getValueByField(f), (m1, m2) -> m1, LinkedHashMap::new));
	}
	
}
