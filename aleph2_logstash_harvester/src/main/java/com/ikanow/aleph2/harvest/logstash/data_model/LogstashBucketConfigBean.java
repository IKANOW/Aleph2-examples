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
package com.ikanow.aleph2.harvest.logstash.data_model;

import java.util.Optional;

import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean.WriteSettings;

/** Per bucket configuration for Logstsash
 * @author Alex
 */
public class LogstashBucketConfigBean {
	public static final Integer DEFAULT_MAX_OBJECTS = 32768;
	
	/** Jackson c'tor
	 */
	protected LogstashBucketConfigBean() {}

	/** The logstash config script
	 * @return
	 */
	public String script() { return Optional.ofNullable(script).orElse(""); }

	/** Whether to go "full verbose" (currently doesn't do anything since the logs aren't returned)
	 * @return
	 */
	public boolean debug_verbosity() { return Optional.ofNullable(debug_verbosity).orElse(false); }
	
	/** Whether to allow the user to specify their own default, or whether to use one of the pre-existing:
	 *  "hdfs" (default), or "elasticsearch"
	 * @return
	 */
	public String output_override() { return Optional.ofNullable(output_override).orElse("hdfs"); }			
	
	public WriteSettings write_settings_override() { return Optional.ofNullable(write_settings_override).orElse(new WriteSettings(DEFAULT_MAX_OBJECTS, 0L, 300, 0)); }
	
	private String script;
	private String output_override;
	private Boolean debug_verbosity;
	private WriteSettings write_settings_override;
}
