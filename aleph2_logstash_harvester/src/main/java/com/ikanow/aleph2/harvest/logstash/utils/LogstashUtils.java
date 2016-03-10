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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.Patterns;
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
	private static final String OUTPUT_FILE_SYNTAX = "ls_input_%{+yyyy.MM.dd.hh}_%{[@metadata][thread_id]}.json"; // (new file every minute unless flushed first)
	private static final String TEST_SEGMENT_PERIOD_OVERRIDE = "10";
	private static final Integer DEFAULT_FLUSH_INTERVAL = 300;
	private static final String HDFS_NAMENODE_HTTP_ADDRESS = "dfs.namenode.http-address."; //dev.nn1 dev.nn2 etc
	private static final String HDFS_NAMESERVICES = "dfs.nameservices";
	
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
		
		final String log_file = System.getProperty("java.io.tmpdir") + File.separator + BucketUtils.getUniqueSignature(bucket_path.orElse("DNE"), Optional.empty());
		try { //(delete log file if it exists)
			new File(log_file).delete();
		}
		catch (Exception e) {}
		
		ArrayList<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(global.binary_path(), "-e", logstash_config));
		if ( bucket_path.isPresent() ) {
			args.addAll(Arrays.asList("-l", log_file));
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
	public static String getOutputTemplate(final String type, final DataBucketBean bucket, final IStorageService storage_service, final String hadoop_root_path, final IHarvestContext context, final LogstashBucketConfigBean config, final GlobalPropertiesBean globals) throws IOException {
		if (type.equals("hdfs")) {
			//if test bucket, override segment_time to be 10s instead of 60s (or allow user to spec in config block)
//			final String import_dir = hadoop_root_path + storage_service.getBucketRootPath() + bucket.full_name() + IStorageService.TO_IMPORT_DATA_SUFFIX + OUTPUT_FILE_SYNTAX;
//			final String temp_dir = hadoop_root_path + storage_service.getBucketRootPath() + bucket.full_name() + IStorageService.TEMP_DATA_SUFFIX + OUTPUT_FILE_SYNTAX;
			final String import_dir = (storage_service.getBucketRootPath() + bucket.full_name() + IStorageService.TO_IMPORT_DATA_SUFFIX + OUTPUT_FILE_SYNTAX).replaceAll("//", "/");
//			final String temp_dir = storage_service.getBucketRootPath() + bucket.full_name() + IStorageService.TEMP_DATA_SUFFIX + OUTPUT_FILE_SYNTAX;
			final List<String> hdfs_server_url = getHDFSServerURL(globals);
			final String output = IOUtils.toString(LogstashHarvestService.class.getClassLoader().getResourceAsStream("output_hdfs.ls"),Charsets.UTF_8)
//									.replace("_XXX_TEMPORARY_PATH_XXX_", temp_dir)
									.replace("_XXX_PATH_XXX_", import_dir)
									.replace("_XXX_HOST1_XXX_", hdfs_server_url.get(0).substring(0, hdfs_server_url.get(0).indexOf(":")))
									.replace("_XXX_PORT1_XXX_", hdfs_server_url.get(0).substring(hdfs_server_url.get(0).indexOf(":")+1))
									.replace("_XXX_HOST2_XXX_", hdfs_server_url.get(1).substring(0, hdfs_server_url.get(1).indexOf(":")))
									.replace("_XXX_PORT2_XXX_", hdfs_server_url.get(1).substring(hdfs_server_url.get(1).indexOf(":")+1))
									.replace("_XXX_USER_XXX_", "tomcat") //TODO this should be a field in the HDFS config (see xxx_server_xxx)
									.replace("_XXX_IDLE_FLUSH_TIME_XXX_", BucketUtils.isTestBucket(bucket) ? TEST_SEGMENT_PERIOD_OVERRIDE : Optional.ofNullable(config.write_settings_override().batch_flush_interval()).orElse(DEFAULT_FLUSH_INTERVAL).toString())									
									.replace("_XXX_FLUSH_SIZE_XXX_", Optional.ofNullable(config.write_settings_override().batch_max_objects()).orElse(LogstashBucketConfigBean.DEFAULT_MAX_OBJECTS).toString())
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
						.<ChronoUnit>flatMap(g-> TimeUtils.getTimePeriod(g).<Optional<ChronoUnit>>validation(f->Optional.empty(), s->Optional.of(s)))
						.map(p->TimeUtils.getTimeBasedSuffix(p, Optional.empty()))
						.map(s->"_%{+" + s + "}")
						//.map(s->"_%{+" + s.replaceAll("y", "Y") + "}")
						.orElse("");			
				
			final String output = IOUtils.toString(LogstashHarvestService.class.getClassLoader().getResourceAsStream("output_elasticsearch.ls"),Charsets.UTF_8)
					.replace("_XXX_INDEX_XXX_", BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty()) + time_suffix);
			return output;			
		}
		else return "";
	}

	private static List<String> getHDFSServerURL(final GlobalPropertiesBean globals) {
		final Configuration config = getConfiguration(globals);
		//first get the dfs.nameservices
		final String dfs_name = config.get(HDFS_NAMESERVICES);
		return Arrays.asList(config.get(HDFS_NAMENODE_HTTP_ADDRESS + dfs_name + ".nn1"), config.get(HDFS_NAMENODE_HTTP_ADDRESS + dfs_name + ".nn2"));
	}
	
	/** 
	 * Retrieves the system configuration
	 *  (with code to handle possible internal concurrency bug in Configuration)
	 *  (tried putting a static synchronization around Configuration as an alternative)
	 * @return
	 */
	protected static Configuration getConfiguration(final GlobalPropertiesBean globals){		
		for (int i = 0; i < 60; ++i) {
			try { 
				return getConfiguration(globals, i);
			}
			catch (java.util.ConcurrentModificationException e) {
				final long to_sleep = Patterns.match(i).<Long>andReturn()
						.when(ii -> ii < 15, __ -> 100L)
						.when(ii -> ii < 30, __ -> 250L)
						.when(ii -> ii < 45, __ -> 500L)
						.otherwise(__ -> 1000L)
						+ (new Date().getTime() % 100L) // (add random component)
						;
				
				try { Thread.sleep(to_sleep); } catch (Exception ee) {}
				if (59 == i) throw e;
			}
		}
		return null;
	}
	protected static Configuration getConfiguration(final GlobalPropertiesBean globals, final int attempt){
		synchronized (Configuration.class) {
			Configuration config = new Configuration(false);
			
			if (new File(globals.local_yarn_config_dir()).exists()) {
				config.addResource(new Path(globals.local_yarn_config_dir() +"/yarn-site.xml"));
				config.addResource(new Path(globals.local_yarn_config_dir() +"/core-site.xml"));
				config.addResource(new Path(globals.local_yarn_config_dir() +"/hdfs-site.xml"));
			}
			else {
				final String alternative = System.getenv("HADOOP_CONF_DIR");
	
				_logger.warn("Aleph2 yarn-config dir not found, try alternative: " + alternative);
				// (another alternative would be HADOOP_HOME + "/conf")
				
				if ((null != alternative) && new File(alternative).exists()) {
					config.addResource(new Path(alternative +"/yarn-site.xml"));
					config.addResource(new Path(alternative +"/core-site.xml"));
					config.addResource(new Path(alternative +"/hdfs-site.xml"));				
				}
				else  // last ditch - will work for local testing but never from anything remote
					config.addResource("default_fs.xml");						
			}
			if (attempt > 10) { // (try sleeping here)
				final long to_sleep = 500L + (new Date().getTime() % 100L); // (add random component)
				try { Thread.sleep(to_sleep); } catch (Exception e) {}
			}
			
			// These are not added by Hortonworks, so add them manually
			config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");									
			config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");									
			config.set("fs.AbstractFileSystem.hdfs.impl", "org.apache.hadoop.fs.Hdfs");
			config.set("fs.AbstractFileSystem.file.impl", "org.apache.hadoop.fs.local.LocalFs");
			return config;
		}		
	}
}
