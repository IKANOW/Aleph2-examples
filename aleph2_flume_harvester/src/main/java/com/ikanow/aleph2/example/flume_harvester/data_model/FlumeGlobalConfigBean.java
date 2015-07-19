/******************************************************************************
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
package com.ikanow.aleph2.example.flume_harvester.data_model;

import java.util.Optional;

/** Global configuration bean
 * @author Alex
 */
public class FlumeGlobalConfigBean {
	public static final String __DEFAULT_FLUME_CONFIG_PATH = "/etc/flume/config";
	public static final String __DEFAULT_FLUME_SERVICE_PATH = "/usr/hdp/current/flume/bin/";	
	public static final String __DEFAULT_FLUME_PLUGIN_PATH = "TODO";	
	
	public String flume_config_path() { return Optional.ofNullable(flume_config_path).orElse(__DEFAULT_FLUME_CONFIG_PATH); }
	public String flume_service_path() { return Optional.ofNullable(flume_service_path).orElse(__DEFAULT_FLUME_SERVICE_PATH); }
	
	public String flume_plugin_path() { return Optional.ofNullable(flume_plugin_path).orElse(__DEFAULT_FLUME_PLUGIN_PATH); }
	public boolean add_self_to_flume_classpath() { return Optional.ofNullable(add_self_to_flume_classpath).orElse(false); }	

	private String flume_service_path;
	private String flume_config_path;
	private String flume_plugin_path;
	private Boolean add_self_to_flume_classpath;
}
