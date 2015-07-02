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

import java.util.List;

/**
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public interface IScriptProvider {

	/**
	 * 
	 * @return Map<String,String> of signature,Scripts that will be added to the factory and precompiled
	 */
	public List<String> getScriptlets();
	/**
	 * 
	 * @return String, the global script that will be executed one and inits the global variables and functions .
	 * It can be executed by using CompiledScriptFactory.GLOBAL as the key 
	 */
	public String getGlobalScript();
}
