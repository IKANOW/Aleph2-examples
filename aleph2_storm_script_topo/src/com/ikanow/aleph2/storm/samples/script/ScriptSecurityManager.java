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

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.ikanow.aleph2.data_model.interfaces.shared_services.ISecurityService;

public class ScriptSecurityManager extends NoSecurityManager {

	protected ISecurityService securityService;

	public ScriptSecurityManager(ISecurityService securityService){
		this.securityService = securityService;
		securityService.enableJvmSecurityManager(true);
	}
	@Override
	public CompiledScript compile(ScriptEngine scriptEngine, String script) throws ScriptException {
		// TODO check roles for compile here?
		return super.compile(scriptEngine, script);
	}

	@Override
	public void setSecureFlag(boolean b) {
		securityService.enableJvmSecurity(b);
	}

}
