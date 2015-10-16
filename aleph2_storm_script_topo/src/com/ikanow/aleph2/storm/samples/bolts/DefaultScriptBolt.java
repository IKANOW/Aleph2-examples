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
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.storm.samples.script.CompiledScriptFactory;
import com.ikanow.aleph2.storm.samples.script.IScriptProvider;
import com.ikanow.aleph2.storm.samples.script.ScriptSecurityManager;

public abstract class DefaultScriptBolt extends BaseRichBolt {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4568169861800172703L;

	private static final Logger logger = LogManager.getLogger(JavaScriptFolderBolt.class);

	/**
	 * 
	 */
	protected OutputCollector _collector;	
	protected transient CompiledScriptFactory compiledScriptFactory = null;

	protected IScriptProvider scriptProvider;
	
	protected String contextSignature = null;
	protected transient  IServiceContext serviceContext;
	protected transient IEnrichmentModuleContext _context;
	
	public DefaultScriptBolt(String contextSignature,IScriptProvider scriptProvider){
		this.contextSignature = contextSignature;
		this.scriptProvider = scriptProvider;		
	}
	
	protected CompiledScriptFactory getCompiledScriptFactory(){
		if(compiledScriptFactory == null){
			
			this.compiledScriptFactory = new CompiledScriptFactory(scriptProvider, new ScriptSecurityManager(serviceContext.getSecurityService()));
			compiledScriptFactory.executeCompiledScript(CompiledScriptFactory.GLOBAL);
		}
		return compiledScriptFactory;
	}
	

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;

		try {
			_context = ContextUtils.getEnrichmentContext(contextSignature);		
			this.serviceContext = _context.getServiceContext();
		}
		catch (Exception e) { // nothing to be done here?
			logger.error("Failed to get context", e);
		}
	}

	
	public static LinkedHashMap<String, Object> tupleToLinkedHashMap(final Tuple t) {
		return StreamSupport.stream(t.getFields().spliterator(), false)
							.collect(Collectors.toMap(f -> f, f -> t.getValueByField(f), (m1, m2) -> m1, LinkedHashMap::new));
	}


}
