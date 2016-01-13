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
package com.ikanow.aleph2.harvest.script.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.ProcessUtils;
import com.ikanow.aleph2.harvest.script.data_model.ScriptHarvesterBucketConfigBean;
import com.ikanow.aleph2.harvest.script.services.ScriptHarvestService;

public class ScriptUtils {
	private static final Logger _logger = LogManager.getLogger();
	private static final String TMP_SCRIPT_FILE_PREFIX = "tmp_script_";
	private static final String ENV_TEST_NUM_OBJ = "A2_TEST_NUM_OBJECTS"; //number of test objects requested (onTest only)
	private static final String ENV_TEST_MAX_RUNTIME_S = "A2_TEST_MAX_RUNTIME_S"; //max seconds runtime (onTest only)
	private static final String ENV_LIBRARY_PATH = "A2_LIBRARY_PATH"; //path of cached library jar
	private static final String ENV_MODULE_PATH = "A2_MODULE_PATH"; //path of cached module jars
	private static final String ENV_CLASS_PATH = "A2_CLASS_PATH"; //path of lib + module jars
	private static final String ENV_BUCKET_HDFS_PATH = "A2_BUCKET_HDFS_PATH"; //path of bucket in hdfs
	private static final String ENV_BUCKET_PATH = "A2_BUCKET_PATH"; //subpath to bucket
	private static final String ENV_BUCKET_STR = "A2_BUCKET_STR"; //string of bucket json
	private static final String LOCAL_RUN_DIR_SUFFIX = "run" + File.separator;
	
	/**
	 * Copies the given script into a random output file in /tmp/
	 * Uses the test buckets full path and owner_id to create a unique path
	 * 
	 * @param script
	 * @param script_output_file
	 */
	public static String saveScriptToTempFile(final String script, final DataBucketBean bucket) throws IOException {		
		//TODO clean these up?
		final File script_output_file = new File(createTmpScriptFilePath(bucket));
		FileUtils.writeStringToFile(script_output_file, script);
		return script_output_file.getAbsolutePath();
	}
	
	/**
	 * Creates a processbuilder pointed at the given script path and adds the working dir and environment vars for you.
	 * Just runs a process that does "sh <script_file_path>"
	 * @param script_file_path
	 * @param working_dir
	 * @return
	 * @throws JsonProcessingException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static ProcessBuilder createProcessBuilderForScriptFile(final String script_file_path, final String working_dir, 
			final Optional<Long> test_requested_num_objects, final Optional<Long> test_max_runtime_s, 
			final Map<String, String> user_args, final IHarvestContext context, final DataBucketBean bucket, final String aleph_root_path) throws JsonProcessingException, InterruptedException, ExecutionException {
		_logger.error("create pb for script file: " + script_file_path);
		ArrayList<String> args = new ArrayList<String>();
		args.add("sh");
		args.add(script_file_path);
		final ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File(working_dir)).redirectErrorStream(true);
		pb.environment().put("JAVA_OPTS", "");
		if ( test_requested_num_objects.isPresent())
			pb.environment().put(ENV_TEST_NUM_OBJ, test_requested_num_objects.get().toString());
		if ( test_max_runtime_s.isPresent())
			pb.environment().put(ENV_TEST_MAX_RUNTIME_S, test_max_runtime_s.get().toString());
		//add in default env vars
		final String classpath = Stream.concat(
					context.getHarvestContextLibraries(Optional.empty()).stream(),
					context.getHarvestLibraries(Optional.of(bucket)).get().values().stream()
				).collect(Collectors.joining(":"));
		pb.environment().put(ENV_MODULE_PATH, context.getHarvestContextLibraries(Optional.empty()).stream().collect(Collectors.joining(":")));
		pb.environment().put(ENV_LIBRARY_PATH, context.getHarvestLibraries(Optional.of(bucket)).get().values().stream().collect(Collectors.joining(":")));
		pb.environment().put(ENV_CLASS_PATH, classpath);
		pb.environment().put(ENV_BUCKET_HDFS_PATH, aleph_root_path + LOCAL_RUN_DIR_SUFFIX + File.separator + bucket.full_name()); //TODO this isn't right, it should be something like /app/aleph2/<bucket.fullname>
		pb.environment().put(ENV_BUCKET_PATH, bucket.full_name()); 		
		pb.environment().put(ENV_BUCKET_STR, BeanTemplateUtils.toJson(bucket).toString());
		//add user args	as env vars
		user_args.forEach((k,val) -> pb.environment().put(k, val));
		return pb;
	}
	
	/**
	 * Creates a file path for a temporary local file that should be unique for this user/bucket, can copy scripts into this
	 * location.
	 * 
	 * @param bucket
	 * @return
	 */
	private static String createTmpScriptFilePath(DataBucketBean bucket) {
		return new StringBuilder()
			.append(System.getProperty("java.io.tmpdir"))
			.append(File.separator)
			.append(TMP_SCRIPT_FILE_PREFIX)
			.append(BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty()))
			.append("_")
			.append(bucket.owner_id())
			.append(".sh")
			.toString();
	}
	
	/**
	 * Starts a script process and stores the PID somewhere so we can kill it later if need be.
	 * 
	 * @param bucket
	 */
	public static BasicMessageBean startScriptProcess(final DataBucketBean bucket, final IHarvestContext context, final String aleph_root_path, final ScriptHarvesterBucketConfigBean config, 
			final String message, final String working_dir, 
			Optional<Long> requested_num_objects, Optional<Long> max_run_time_secs) {
		//TODO pass on user args in config.args to script call
		//TODO start script, record pid so we can kill later if need be
		//validate one of the 3 script fields was supplied
		if ( !config.script().isEmpty() || !config.local_script_url().isEmpty() || !config.resource_name().isEmpty()) {
			_logger.error("Running a script or script_file: " + config.script() + " or local: " + config.local_script_url() + " or resource: " + config.resource_name());			
			String script_file_path;
			//get the script file path from the 3 config options
			if ( !config.script().isEmpty() ) { 	
				//SCRIPT FIELD - copy to local file
				try {
					script_file_path = ScriptUtils.saveScriptToTempFile(config.script(), bucket);
				} catch (IOException e) {
					return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Could not create temporary file to load script into: " + e.getMessage());
				}
			} else if ( !config.local_script_url().isEmpty() ) {
				//SCRIPT FILE - point to it
				script_file_path = config.local_script_url();
			} else {
				//RESOURCE - copy to local file, point to it								
				try {
					final String resource = IOUtils.toString(ScriptHarvestService.class.getClassLoader().getResourceAsStream(config.resource_name()), Charsets.UTF_8).replaceAll("\r\n", "\n");
					script_file_path = ScriptUtils.saveScriptToTempFile(resource, bucket);
				} catch (IOException e) {
					return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Could not create temporary file to load script into: " + e.getMessage());
				} catch (NullPointerException e) {
					return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Could not find resource file: " + config.resource_name());
				}
			}
			//run the script file (or script we copied into one)
			ProcessBuilder pb;
			try {
				pb = ScriptUtils.createProcessBuilderForScriptFile(script_file_path, working_dir, requested_num_objects, max_run_time_secs, config.args(), context, bucket, aleph_root_path);
			} catch (JsonProcessingException | InterruptedException | ExecutionException e) {
				return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Could not create process to run: " + e.getMessage());
			}
			final Tuple2<String, String> err_pid = ProcessUtils.launchProcess(pb, ScriptHarvestService.class.getSimpleName(), bucket, aleph_root_path + LOCAL_RUN_DIR_SUFFIX, 
					max_run_time_secs.isPresent() ? Optional.of(new Tuple2<Long, Integer>(max_run_time_secs.get(), 9)) : Optional.empty());
			
			if (null != err_pid._1()) {
				return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Bucket error: " + err_pid._1());				
			}
			else {
				return ErrorUtils.buildSuccessMessage(ScriptHarvestService.class.getSimpleName(), message, "Bucket launched: " + err_pid._2());											
			}
		} else {
			return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "requires script or script_url to be specified in the harvester configs");
		}
	}
	
	/**
	 * Gets the pid if one exists for this bucket and tries to kill it.  What if this job finished and something else has been assigned this pid?
	 * 
	 * @param bucket
	 */
	public static BasicMessageBean stopScriptProcess(final DataBucketBean bucket, final ScriptHarvesterBucketConfigBean config, final String message, final String working_dir, final String aleph_root_path) {
		//STOP PID if its still running (verify its the same process)		
		final Tuple2<String, Boolean> err_pid = ProcessUtils.stopProcess(ScriptHarvestService.class.getSimpleName(), bucket, aleph_root_path + LOCAL_RUN_DIR_SUFFIX, Optional.empty());
		if ( !err_pid._2) {
			//failed to stop, try to cleanup script file and bail out
			cleanupTempScriptFile(bucket, message);
			return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "Error stopping script (can result in script continuing to run on server, need to manually kill perhaps): "+err_pid._1, Optional.empty());
		}
		
		// KILL tmp script file we copied locally if need be?		
		cleanupTempScriptFile(bucket, message);		
		return ErrorUtils.buildSuccessMessage(ScriptHarvestService.class.getSimpleName(), message, "Temporary script stopped and tmp files deleted successfully.");
	}
	
	private static BasicMessageBean cleanupTempScriptFile(final DataBucketBean bucket, final String message) {
		final String script_file_path = ScriptUtils.createTmpScriptFilePath(bucket);
		final File tmp_script_File = new File(script_file_path);
		if ( tmp_script_File.exists() )
			tmp_script_File.delete();
		return ErrorUtils.buildSuccessMessage(ScriptHarvestService.class.getSimpleName(), message, "Temporary script deleted successfully.");
	}

	/**
	 * Just a helper function that calls stopScriptProcess then startScriptProcess
	 * @param bucket
	 */
	public static BasicMessageBean restartScriptProcess(final DataBucketBean bucket, final IHarvestContext context, final String aleph_root_path, final ScriptHarvesterBucketConfigBean config, final String message, final String working_dir, 
			Optional<Long> requested_num_objects, Optional<Long> max_run_time_secs) {
		final BasicMessageBean stop_result = stopScriptProcess(bucket, config, message, working_dir, aleph_root_path);
		final BasicMessageBean start_result = startScriptProcess(bucket, context, aleph_root_path, config, message, working_dir, requested_num_objects, max_run_time_secs);
		//merge the results and return that bean
		final Map<String, String> details = new HashMap<String, String>();
		if ( stop_result.details() != null )
			details.putAll(stop_result.details());
		if ( start_result.details() != null)
			details.putAll(start_result.details());
		return new BasicMessageBean(new Date(), stop_result.success() && start_result.success(), ScriptHarvestService.class.getSimpleName(), "restartScriptProcess", 0, "STOP: " + stop_result.message() + " START: " + start_result.message(), details);
	}

	public static boolean isProcessRunning(final DataBucketBean bucket, final String aleph_root_path) {
		return ProcessUtils.isProcessRunning(ScriptHarvestService.class.getSimpleName(), bucket, aleph_root_path + LOCAL_RUN_DIR_SUFFIX);
	}
}
