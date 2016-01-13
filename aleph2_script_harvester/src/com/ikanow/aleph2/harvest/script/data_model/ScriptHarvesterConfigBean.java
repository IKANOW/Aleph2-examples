/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
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
package com.ikanow.aleph2.harvest.script.data_model;

import java.util.Optional;

public class ScriptHarvesterConfigBean {
	
	//jackson ctor
	protected ScriptHarvesterConfigBean(){}
	
	public static String WORKING_DIR = System.getProperty("java.io.tmpdir");
	
	public String working_dir() { return Optional.ofNullable(working_dir).orElse(WORKING_DIR); }
	
	private String working_dir;
}
