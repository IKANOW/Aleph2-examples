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
package com.ikanow.aleph2.storm.samples.script;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;


/**
 * This class is used to create and execute pre-compiled javascript functions.
 * 
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public class CompiledScriptFactory implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3560458546056722085L;
	public static int GLOBAL = 0;
	protected static int COMBINED_PIPELINE = 1;
//	public static int UNSTRUCTURED_GLOBAL = 2;
	
	private static final Logger logger = Logger.getLogger(CompiledScriptFactory.class);

	protected static String JAVASCRIPT = "javascript";
	public static int STANDALONE_TYPE=0;
	public static int SCRIPT_TYPE=1;
	public static int FUNC_TYPE=2;

	
	private transient ScriptEngine engine = null;
	private transient ScriptContext scriptContext = null;
	protected HashMap<Integer,CompiledScriptEntry> compiledScriptMap = null;
	
	private IScriptSecurityManager securityManager;
	private IScriptProvider scriptProvider;
	
	/**
	 * @return the engine
	 */
	public ScriptEngine getEngine() {
		if(engine ==null){
			init();
		}
		return engine;
	}

	public CompiledScriptFactory(){
		
	}

	public CompiledScriptFactory(IScriptProvider scriptProvider,IScriptSecurityManager securityManager){		
		logger.debug("CompiledScriptFactoryContructor compiling scripts from scriptProvider:"+scriptProvider);
		this.securityManager = securityManager;
		this.scriptProvider = scriptProvider;
		init();
	}
	

	protected void init(){
		ScriptEngineManager manager = new ScriptEngineManager();
		this.engine = manager.getEngineByName("JavaScript");	
		this.scriptContext = engine.getContext();
		this.compiledScriptMap = new HashMap<Integer,CompiledScriptEntry>();
		List<String> scriptlets = scriptProvider.getScriptlets();
		for (String sciptlet : scriptlets) {
			addAndCompileScript(sciptlet,false);			
		}
		
		addAndCompileScript(scriptProvider.getGlobalScript(),GLOBAL,false);			
	}
	
	
	protected CompiledScript addAndCompileScript(String scriptlet,int key,boolean executeCacheCheckScript){
		CompiledScript cs = null;
		if(scriptlet!=null){
			CompiledScriptEntry ce = compiledScriptMap.get(key); 
			if(ce==null){
				int scriptType = STANDALONE_TYPE;
				StringBuffer script = new StringBuffer();
				if (scriptlet.toLowerCase().startsWith("$script") )
				{
					script.append(JavaScriptUtils.createDollarScriptFunctionAndCall(scriptlet));
					scriptType = SCRIPT_TYPE;
				}
				else  if(scriptlet.toLowerCase().startsWith("$func"))
				{
					script.append(JavaScriptUtils.getScript(scriptlet));
					scriptType = FUNC_TYPE;
				}
				else{
					script.append(scriptlet);
				}
				try {
					cs = securityManager.compile(getEngine(), script.toString());
					compiledScriptMap.put(key, new CompiledScriptEntry(cs,scriptType,executeCacheCheckScript));
					logger.debug("Added compiled script,mapsize="+compiledScriptMap.size()+",key="+key+" ,script:\n"+script);
					// debug
					//if (scriptlet.toLowerCase().startsWith("$script") )
					//{
						//executeCompiledScript(scriptlet);
					//}
					
				} catch (ScriptException e) {
					logger.error("Error compiling script:\n"+scriptlet,e);			
				}
			}else{
				cs= ce.getCompiledScript();
				logger.debug("addAndCompileScript script (key="+key+") already exists, skipping compilation\n"+scriptlet);
			}
		} // if !=null
		return cs;
	}

	protected CompiledScript addAndCompileScript(String scriptlet,boolean addCacheCheckScript){
		CompiledScript cs = null;
		if(scriptlet!=null){
			cs = addAndCompileScript(scriptlet,scriptlet.hashCode(),addCacheCheckScript);
		}
		return cs;
	}

	protected String lookupFunctionName(Map<Integer,String> genericFunctionNames,String script) {
		// just pass in hash so we don't keep the scripts
		String functionName = genericFunctionNames.get(script.hashCode());
		if (functionName != null) {
			return functionName;
		} else {
			logger.warn("generic function name not found for script:" + script + " ");
			return null;
		}
	}

	public ScriptContext getScriptContext() {
		if(scriptContext == null){
			init();
		}
		return scriptContext;
	}

	public Object executeCompiledScript(String scriptlet,Object... attributes) {
		Object retVal = null;		
		try{
			for (int i = 0; i < 2*(attributes.length/2); i+=2) {
				String attrName  = (String)attributes[i];
				Object attrValue  = attributes[i+1];
				if(attrName!=null){
					getScriptContext().setAttribute(attrName,attrValue,ScriptContext.ENGINE_SCOPE);
				}
			}
			//logger.debug("Factory document:"+getScriptContext().getAttribute("_doc")+",metadata"+getScriptContext().getAttribute("_metadata")+" for script:"+scriptlet);
			if(scriptlet!=null){
				int key = scriptlet.hashCode();
				CompiledScriptEntry ce = compiledScriptMap.get(key);				
				if(ce!=null){
					securityManager.setSecureFlag(true);			
					int cetype = ce.getScriptType();
					if((cetype==STANDALONE_TYPE) || (cetype==FUNC_TYPE)){
						retVal = ce.getCompiledScript().eval(getScriptContext());
					}else if(cetype==SCRIPT_TYPE){
						retVal = ce.getCompiledScript().eval(getScriptContext());
					}
				} // ce!=null
				else{
					// TODO maybe allow lazy compilation
					logger.error("Script was not compiled ,mapsize="+compiledScriptMap.size()+",key="+key+" ,script:"+scriptlet);
				}
			}
		} catch (Exception e) {
			logger.error("executeCompiledScript caught exception for script:\n"+scriptlet+" \n"+e.getMessage());
		}finally {
			securityManager.setSecureFlag(false);				
		}
		return retVal;
	}
	

	public Object executeCompiledScript(int key) {
		Object retVal = null;
		try{
			securityManager.setSecureFlag(true);				
			CompiledScriptEntry ce = compiledScriptMap.get(key);
			if(ce!=null){
				int cetype = ce.getScriptType();
				if((cetype==STANDALONE_TYPE) || (cetype==FUNC_TYPE)){
					retVal = ce.getCompiledScript().eval(scriptContext);
				}else if(cetype==SCRIPT_TYPE){
					// call invocable
					Invocable invocable = (Invocable)ce.getCompiledScript().getEngine();					
					retVal = invocable.invokeFunction(JavaScriptUtils.genericFunctionCall);
				}
			} // ce!=null
		} catch (Exception e) {
			logger.error("executeCompiledScript caught exception:",e);

		}finally {
			securityManager.setSecureFlag(false);				
		}
		return retVal;
	}

	/**
	 *  Internal class use to speedup check if script has a $SCRIPT, func or standalone functionality 
	 * @author jfreydank
	 *
	 */
	  class CompiledScriptEntry implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -5200918200005796573L;
		private CompiledScript compiledScript;
		private int scriptType = 0;
		private boolean executeCacheCheckScript = false;

		public CompiledScriptEntry(CompiledScript compiledScript,int scriptType,boolean executeCacheCheckScript){
			this.compiledScript= compiledScript;			
			this.scriptType = scriptType;
			this.executeCacheCheckScript = executeCacheCheckScript;
		}
		
		public CompiledScript getCompiledScript() {
			return compiledScript;
		}
		
		public int getScriptType() {
			return scriptType;
		}

		/**
		 * @return the executeCacheCheckScript
		 */
		public boolean isExecuteCacheCheckScript() {
			return executeCacheCheckScript;
		}	
	}

}
