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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.TimeUtils;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashBucketConfigBean;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;
import com.ikanow.aleph2.harvest.logstash.services.LogstashHarvestService;

/** Utilities for manipulating logstash assets (config file code is separate)
 *  Mostly copied from V1
 * @author Alex
 */
public class LogstashUtils {

	private static final Logger _logger = LogManager.getLogger();
	
	/** Builds a process to execute
	 * @param global
	 * @param bucket_config
	 * @param logstash_config
	 * @param requested_docs
	 * @param bucket_path if this is present, will log output to /tmp/unique_sig
	 * @param context 
	 * @return
	 */
	public static ProcessBuilder buildLogstashTest(final LogstashHarvesterConfigBean global, final LogstashBucketConfigBean bucket_config, final String logstash_config, final long requested_docs, final Optional<String> bucket_path ) {
		
		ArrayList<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(global.binary_path(), "-e", logstash_config));
		if ( bucket_path.isPresent() ) {
			args.addAll(Arrays.asList("-l", System.getProperty("java.io.tmpdir") + File.separator + BucketUtils.getUniqueSignature(bucket_path.get(), Optional.empty())));
		}
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
	public static String getOutputTemplate(final String type, final DataBucketBean bucket, final IStorageService storage_service, final String hadoop_root_path, final IHarvestContext context) throws IOException {
		if (type.equals("hdfs")) {
			final String import_dir = hadoop_root_path + storage_service.getBucketRootPath() + IStorageService.TO_IMPORT_DATA_SUFFIX;
			final String temp_dir = hadoop_root_path + storage_service.getBucketRootPath() + IStorageService.TEMP_DATA_SUFFIX;
			final String output = IOUtils.toString(LogstashHarvestService.class.getClassLoader().getResourceAsStream("output_hdfs.ls"),Charsets.UTF_8)
									.replace("_XXX_TEMPPATH_XXX_", temp_dir)
									.replace("_XXX_FINALPATH_XXX_", import_dir)
									;
			return output;
		}
		else if (type.equals("elasticsearch")) {
			// Work out what the index naming is:
			//create the template
			context.getServiceContext().getSearchIndexService().get().getDataService().get().getWritableDataService(JsonNode.class, bucket, Optional.empty(), Optional.empty()).get();
			//replace out the elasticsearch-specific sub variables 
			final Optional<String> grouping = Optionals.of(() -> bucket.data_schema().temporal_schema().grouping_time_period());
			final String time_suffix = 
					grouping
						.<ChronoUnit>flatMap(g-> TimeUtils.getTimePeriod(g).validation(f->Optional.empty(), s->Optional.of(s)))
						.map(p->TimeUtils.getTimeBasedSuffix(p, Optional.empty()))
						.map(s->"_%{+" + s + "}")
						//.map(s->"_%{+" + s.replaceAll("y", "Y") + "}")
						.orElse("");			
				
			final String output = IOUtils.toString(LogstashHarvestService.class.getClassLoader().getResourceAsStream("output_elasticsearch.ls"),Charsets.UTF_8)
					.replace("_XXX_INDEX_XXX_", BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty()) + time_suffix);
			_logger.error(output);
			return output;			
		}
		else return "";
	}
}
