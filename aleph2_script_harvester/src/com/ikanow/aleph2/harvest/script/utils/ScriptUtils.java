package com.ikanow.aleph2.harvest.script.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.google.common.base.Charsets;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.harvest.script.data_model.ScriptHarvesterBucketConfigBean;
import com.ikanow.aleph2.harvest.script.services.ScriptHarvestService;

public class ScriptUtils {
	private static final Logger _logger = LogManager.getLogger();
	private static final String TMP_SCRIPT_FILE_PREFIX = "tmp_script_";
	private static final String ENV_TEST_NUM_OBJ = "ALEPH2_TEST_NUM_OBJECTS";
	private static final String ENV_TEST_MAX_RUNTIME_S = "ALEPH2_TEST_MAX_RUNTIME_S";
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
	 */
	public static ProcessBuilder createProcessBuilderForScriptFile(final String script_file_path, final String working_dir, Optional<Long> test_requested_num_objects, Optional<Long> test_max_runtime_s) {
		_logger.error("create pb for script file: " + script_file_path);
		ArrayList<String> args = new ArrayList<String>();
		args.add("sh");
		args.add(script_file_path);
		ProcessBuilder pb = new ProcessBuilder(args);
		pb = pb.directory(new File(working_dir)).redirectErrorStream(true);
		pb.environment().put("JAVA_OPTS", "");
		if ( test_requested_num_objects.isPresent())
			pb.environment().put(ENV_TEST_NUM_OBJ, test_requested_num_objects.get().toString());
		if ( test_max_runtime_s.isPresent())
			pb.environment().put(ENV_TEST_MAX_RUNTIME_S, test_max_runtime_s.get().toString());
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
	public static BasicMessageBean startScriptProcess(final DataBucketBean bucket, final ScriptHarvesterBucketConfigBean config, final String message, final String working_dir, 
			Optional<Long> requested_num_objects, Optional<Long> max_run_time_secs) {
		//TODO pass on user args in config.args to script call
		//TODO start script, record pid so we can kill later if need be
		//validate one of the 3 script fields was supplied
		if ( !config.script().isEmpty() || !config.local_script_url().isEmpty() || !config.resource_name().isEmpty()) {
			_logger.error("Running a script or script_file: " + config.script() + " or " + config.local_script_url() + " or " + config.resource_name());			
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
				}
			}
			//run the script file (or script we copied into one)
			final ProcessBuilder pb = ScriptUtils.createProcessBuilderForScriptFile(script_file_path, working_dir, requested_num_objects, max_run_time_secs);
			final Tuple2<String, String> err_pid = ProcessUtils.launchProcess(pb, bucket);
			
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
	public static BasicMessageBean stopScriptProcess(final DataBucketBean bucket, final ScriptHarvesterBucketConfigBean config, final String message, final String working_dir) {
		//TODO STOP PID if its still running (verify its the same process)
		
		//TODO KILL tmp script file we copied locally if need be?
		return ErrorUtils.buildErrorMessage(ScriptHarvestService.class.getSimpleName(), message, "not yet implemented");
	}

	/**
	 * Just a helper function that calls stopScriptProcess then startScriptProcess
	 * @param bucket
	 */
	public static BasicMessageBean restartScriptProcess(final DataBucketBean bucket, final ScriptHarvesterBucketConfigBean config, final String message, final String working_dir, 
			Optional<Long> requested_num_objects, Optional<Long> max_run_time_secs) {
		final BasicMessageBean stop_result = stopScriptProcess(bucket, config, message, working_dir);
		final BasicMessageBean start_result = startScriptProcess(bucket, config, message, working_dir, requested_num_objects, max_run_time_secs);
		//merge the results and return that bean
		final Map<String, String> details = stop_result.details();
		details.putAll(start_result.details());
		return new BasicMessageBean(new Date(), stop_result.success() && start_result.success(), ScriptHarvestService.class.getSimpleName(), "restartScriptProcess", 0, "STOP: " + stop_result.message() + " START: " + start_result.message(), details);
	}
}
