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
package com.ikanow.aleph2.storm.samples.script;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Diasbled SecurityManager, use for testing purposes.
 * . 
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public class NoSecurityManager implements IScriptSecurityManager{

	public CompiledScript compile(final ScriptEngine scriptEngine, final String script) throws ScriptException
	{			
			//Security OFF
			Compilable compilingEngine = (Compilable)scriptEngine;
			return compilingEngine.compile(script);
	}

	@Override
	public void setSecureFlag(boolean b) {
		// do nothing on purpose		
	}

}
