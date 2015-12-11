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

/** Global harvester configuration for Logstsash
 *  (currently nothing to configure)
 * @author Alex
 */
public class LogstashHarvesterConfigBean {
	// From v1:
	public static String LOGSTASH_DIRECTORY = "/opt/logstash-infinite/";
	public static String LOGSTASH_WD = "/opt/logstash-infinite/logstash/";
	public static String LOGSTASH_CONFIG = "/opt/logstash-infinite/logstash.conf.d/";
	public static String LOGSTASH_CONFIG_DISTRIBUTED = "/opt/logstash-infinite/dist.logstash.conf.d/";
	public static String LOGSTASH_BINARY = "/opt/logstash-infinite/logstash/bin/logstash";
	
	public static String LOGSTASH_RESTART_FILE = "/opt/logstash-infinite/RESTART_LOGSTASH";
	public static String LOGSTASH_CONFIG_EXTENSION = ".v2.conf";
	
	public static String NON_ADMIN_INPUTS = "collectd,drupal_dblog,gelf,gemfire,imap,irc,lumberjack,s3,snmptrap,sqs,syslog,twitter,udp,xmpp,zenoss";
	public static String NON_ADMIN_FILTERS = "advisor,alter,anonymize,checksum,cidr,cipher,clone,collate,csv,date,dns,drop,elapsed,extractnumbers,fingerprint,geoip,gelfify,grep,grok,grokdiscovery,l18n,json,json_encode,kv,metaevent,metrics,multiline,mutate,noop,prune,punct,railsparallelrequest,range,sleep,split,sumnumbers,syslog_pri,throttle,translate,unique,urldecode,useragent,uuid,wms,wmts,xml";
	public static String NON_ADMIN_OUTPUTS = ""; //(non for now, non-admins will have to use the overrides)
	
	public static String DEFAULT_MOUNT_ROOT = "/opt/hadoop-fileshare/app/aleph2";
	
	/** Jackson c'tor
	 */
	protected LogstashHarvesterConfigBean() {}
	
	public String base_dir() { return Optional.ofNullable(base_dir).orElse(LOGSTASH_DIRECTORY); }
	public String working_dir() { return Optional.ofNullable(working_dir).orElse(LOGSTASH_WD); }
	public String master_config_dir() { return Optional.ofNullable(master_config_dir).orElse(LOGSTASH_CONFIG); }
	public String slave_config_dir() { return Optional.ofNullable(slave_config_dir).orElse(LOGSTASH_CONFIG_DISTRIBUTED); }
	public String binary_path() { return Optional.ofNullable(binary_path).orElse(LOGSTASH_BINARY); }
	public String restart_file() { return Optional.ofNullable(restart_file).orElse(LOGSTASH_RESTART_FILE); }

	public String non_admin_inputs() { return Optional.ofNullable(non_admin_inputs).orElse(NON_ADMIN_INPUTS); }
	public String non_admin_filters() { return Optional.ofNullable(non_admin_filters).orElse(NON_ADMIN_FILTERS); }
	public String non_admin_outputs() { return Optional.ofNullable(non_admin_outputs).orElse(NON_ADMIN_OUTPUTS); }
	
	public String hadoop_mount_root() { return Optional.ofNullable(hadoop_mount_root).orElse(DEFAULT_MOUNT_ROOT); }
	
	private String base_dir;
	private String working_dir;
	private String master_config_dir;
	private String slave_config_dir;
	private String binary_path;	
	private String restart_file;
	private String hadoop_mount_root;
	
	private String non_admin_inputs;
	private String non_admin_filters;
	private String non_admin_outputs;
}
