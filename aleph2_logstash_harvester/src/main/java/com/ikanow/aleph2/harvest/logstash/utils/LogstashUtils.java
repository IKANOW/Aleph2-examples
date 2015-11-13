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
package com.ikanow.aleph2.harvest.logstash.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashBucketConfigBean;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;

/** Utilities for manipulating logstash assets (config file code is separate)
 *  Mostly copied from V1
 * @author Alex
 */
public class LogstashUtils {

	/** Builds a process to execute
	 * @param global
	 * @param bucket_config
	 * @param logstash_config
	 * @param requested_docs
	 * @return
	 */
	public static ProcessBuilder buildLogstashTest(final LogstashHarvesterConfigBean global, final LogstashBucketConfigBean bucket_config, final String logstash_config, final long requested_docs) {
		
		ArrayList<String> args = new ArrayList<String>(4);
		args.addAll(Arrays.asList(global.binary_path(), "-e", logstash_config));
		if (0L == requested_docs) {
			args.add("-t"); // test mode, must faster
		}//TESTED
		
		if (bucket_config.debug_verbosity()) {
			args.add("--debug");
		}
		else {
			args.add("--verbose");					
		}
		ProcessBuilder logstashProcessBuilder = new ProcessBuilder(args);
		logstashProcessBuilder = logstashProcessBuilder.directory(new File(global.working_dir())).redirectErrorStream(true);
		logstashProcessBuilder.environment().put("JAVA_OPTS", "");
		
		return logstashProcessBuilder;
	}
	
	/**
	 * @param type
	 * @param bucket
	 * @return
	 * @throws IOException 
	 */
	public static String getOutputTemplate(final String type, final DataBucketBean bucket, final IStorageService storage_service) throws IOException {
		if (type.equals("hdfs")) {
			final String import_dir = storage_service.getBucketRootPath() + IStorageService.TO_IMPORT_DATA_SUFFIX;
			final String temp_dir = storage_service.getBucketRootPath() + IStorageService.TEMP_DATA_SUFFIX;
			final String output = Resources.toString(Resources.getResource("output_hdfs.ls"), Charsets.UTF_8)
									.replace("_XXX_TEMPPATH_XXX_", temp_dir)
									.replace("_XXX_FINALPATH_XXX_", import_dir)
									;
			return output;
		}
		else if (type.equals("elasticsearch")) {
			// Work out what the index naming is:
			
			//TODO: need to replace out the elasticsearch-specific sub variables 
			final String output = Resources.toString(Resources.getResource("output_elasticsearch.ls"), Charsets.UTF_8);
			return output;			
		}
		else return "";
	}
}
