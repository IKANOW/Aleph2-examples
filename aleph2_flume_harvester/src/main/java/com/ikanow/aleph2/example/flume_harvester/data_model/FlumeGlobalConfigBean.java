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
package com.ikanow.aleph2.example.flume_harvester.data_model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Global configuration bean
 * @author Alex
 */
public class FlumeGlobalConfigBean {
	public static final String __DEFAULT_FLUME_CONFIG_PATH = "/etc/flume/conf";
	public static final String __DEFAULT_FLUME_SERVICE_PATH = "/usr/hdp/current/flume-server/bin/flume-ng";	
	public static final String __DEFAULT_FLUME_LIB_PATH = "/usr/hdp/current/flume-server/lib/";	
	public static final String __DEFAULT_MEMUSAGE = "100m";
	
	/** If not HDP then specifies the config directory for Flume
	 * @return
	 */
	public String flume_config_path() { return Optional.ofNullable(flume_config_path).orElse(__DEFAULT_FLUME_CONFIG_PATH); }
	/**If not HDP then specifies the flume binary to call
	 * @return
	 */
	public String flume_service_path() { return Optional.ofNullable(flume_service_path).orElse(__DEFAULT_FLUME_SERVICE_PATH); }
	/** IF not HDP then specifies the locations of all the 
	 * @return
	 */
	public String flume_lib_path() { return Optional.ofNullable(flume_lib_path).orElse(__DEFAULT_FLUME_LIB_PATH); }

	/** The memory usage of each agent, in "java" format (eg "100m" which is the default)
	 * @return
	 */
	public String default_memory_usage() { return Optional.ofNullable(default_memory_usage).orElse(__DEFAULT_MEMUSAGE); }
	/** A set of overrides that must all start -X or -D 
	 * @return
	 */
	public List<String> java_overrides() { return Optional.ofNullable(java_overrides).orElse(Collections.emptyList()); }
	
	/** For HDP - if false (default) then Flume is prevented from adding the contents of HDP's hadoop libs onto the classpath
	 * @return
	 */
	public boolean include_hadoop_classpath() { return Optional.ofNullable(include_hadoop_classpath).orElse(false); }
	/** For HDP - if false (default) then Flume is prevented from adding the contents of HDP's hbase libs onto the classpath
	 * @return
	 */
	public boolean include_hbase_classpath() { return Optional.ofNullable(include_hbase_classpath).orElse(false); }
	/** For HDP - if false (default) then Flume is prevented from adding the contents of HDP's hive libs onto the classpath
	 * @return
	 */
	public boolean include_hive_classpath() { return Optional.ofNullable(include_hive_classpath).orElse(false); }
	/** Workaround for HDP 2.3.4 issues - eg Flume uses an old version of ZK/kafka, this override will prepend all the Aleph2 libraries
	 * @return
	 */
	public boolean flume_classpath_override() { return Optional.ofNullable(flume_classpath_override).orElse(true); }
		
	private String flume_service_path;
	private String flume_lib_path;
	private String flume_config_path;
	
	private String default_memory_usage;
	private List<String> java_overrides;
	
	private Boolean include_hadoop_classpath;
	private Boolean include_hbase_classpath;
	private Boolean include_hive_classpath;
	private Boolean flume_classpath_override;
}
