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
package com.ikanow.aleph2.storm.samples.script.js;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ikanow.aleph2.storm.samples.script.IScriptProvider;
import com.ikanow.aleph2.storm.samples.script.JavaScriptProviderBean;

public class BeanBasedScriptProvider implements IScriptProvider,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8831305243222180167L;
	private JavaScriptProviderBean providerBean;

	private List<String> scriptlets = new ArrayList<String>();
	
	public BeanBasedScriptProvider(JavaScriptProviderBean providerBean){
		this.providerBean = providerBean;
		if(providerBean.getScriptlets()!=null){
			scriptlets.addAll(providerBean.getScriptlets());
		}
	}
	
	@Override
	public List<String> getScriptlets() {
		
		return scriptlets;
	}

	@Override
	public String getGlobalScript() {
		return providerBean.getGlobalScript() + "\n" + Optional.ofNullable(providerBean.getUserScript()).orElse("");
	}

}
