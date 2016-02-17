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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScriptHarvesterBucketConfigBean {

	//jackson ctor
	protected ScriptHarvesterBucketConfigBean() {}
	
	public String script() { return Optional.ofNullable(script).orElse(""); }
	public String local_script_url() { return Optional.ofNullable(local_script_url).orElse(""); }
	public String resource_name() { return Optional.ofNullable(resource_name).orElse(""); }
	public Map<String, String> args() { return Optional.ofNullable(args).orElse(new HashMap<String,String>()); }
	public List<String> required_assets() { return Optional.ofNullable(required_assets).orElse(new ArrayList<String>());}
	public boolean watchdog_enabled() { return Optional.ofNullable(watchdog_enabled).orElse(true); }
	
	private String script;
	private String local_script_url;
	private String resource_name;
	private Map<String, String> args;
	private List<String> required_assets;
	private Boolean watchdog_enabled;
}
