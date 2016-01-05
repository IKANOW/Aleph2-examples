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
package com.ikanow.aleph2.harvest.logstash.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ISubject;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.ProcessUtils;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashBucketConfigBean;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;
import com.ikanow.aleph2.harvest.logstash.utils.LogstashConfigUtils;
import com.ikanow.aleph2.harvest.logstash.utils.LogstashUtils;

import fj.data.Validation;

/** Logstash harvester interface
 * @author Alex
 */
public class LogstashHarvestService implements IHarvestTechnologyModule {
	private static final Logger _logger = LogManager.getLogger();
	protected final SetOnce<LogstashHarvesterConfigBean> _globals = new SetOnce<>();
	protected final SetOnce<IHarvestContext> _context = new SetOnce<>();
	protected final SetOnce<GlobalPropertiesBean> _global_propertes = new SetOnce<>();
	private static final String LOCAL_RUN_DIR_SUFFIX = "run" + File.separator;
	
	////////////////////////////////////////////////////////////////////////////////
	
	// SERVICE API	
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onInit(com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public void onInit(IHarvestContext context) {
		_logger.error("LOGSTASH: init");
		_globals.set(BeanTemplateUtils.from(Optional.ofNullable(context.getTechnologyLibraryConfig().library_config()).orElse(Collections.emptyMap()), LogstashHarvesterConfigBean.class).get());
		_context.set(context);
		_global_propertes.set(context.getServiceContext().getGlobalProperties());		
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#canRunOnThisNode(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		_logger.error("LOGSTASH: canRun");
		final File master = new File(_globals.get().master_config_dir()); 
		final File slave = new File(_globals.get().slave_config_dir()); 
		if (BucketUtils.isTestBucket(bucket)) {
			_logger.error("LOGSTASH: canRun test: " + new File(_globals.get().binary_path()).exists());
			return new File(_globals.get().binary_path()).exists();
		}
		else if (Optional.ofNullable(bucket.multi_node_enabled()).orElse(false)) { // multi node
			return master.exists() || slave.exists();
		}
		else { // single node, only care about master 
			return master.exists();
		}
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onNewSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext, boolean)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		
		if (enabled) {
			final LogstashBucketConfigBean config = 
					Optionals.ofNullable(new_bucket.harvest_configs()).stream().findFirst()														
						.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
					.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
						
			return CompletableFuture.completedFuture(startOrUpdateLogstash(new_bucket, config, _globals.get(), context));
		}
		else {		
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onNewSource", "Bucket {0} created but suspended", new_bucket.full_name()));
		}
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onUpdatedSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, boolean, java.util.Optional, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			boolean is_enabled, Optional<BucketDiffBean> diff,
			IHarvestContext context) {
				
		final LogstashBucketConfigBean config = Optionals.ofNullable(new_bucket.harvest_configs()).stream().findFirst()														
				.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
		
		// Handle test case - use process utils to delete
		if (BucketUtils.isTestBucket(new_bucket)) {
			_logger.error("UPDATE SOURCE: trying to stop a test bucket, resetting file pointer");
			resetFilePointer(new_bucket, config, _globals.get());
			
			//kill/log
			_logger.error("Sending stop process");
			final Tuple2<String, Boolean> kill_result = ProcessUtils.stopProcess(this.getClass().getSimpleName(), new_bucket, _global_propertes.get().local_root_dir() + LOCAL_RUN_DIR_SUFFIX, Optional.of(2));
			
			return CompletableFuture.completedFuture(
					ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "Bucket suspended: {0}", kill_result._1()));
		}
		else {
			if (diff.map(bdb -> bdb.diffs().isEmpty()).orElse(false)) { // if nothing's changed then do nothing
				//TODO: longer term could do better here, eg we don't care unless data_schema or harvest_configs have changed, right?				
				return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onUpdatedSource", "No change to bucket"));			
			}
			if (is_enabled) {
				return CompletableFuture.completedFuture(startOrUpdateLogstash(new_bucket, config, _globals.get(), context));
			}
			else { // Just stop
				//(this does nothing if the bucket isn't actually running)
				return CompletableFuture.completedFuture(stopLogstash(new_bucket, config, _globals.get()));
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onPurge(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
		
		final LogstashBucketConfigBean config = 
				Optionals.ofNullable(to_purge.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
				
		resetFilePointer(to_purge, config, _globals.get());
		
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onPurge", "(done)"));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onDelete(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		
		final LogstashBucketConfigBean config = 
				Optionals.ofNullable(to_delete.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
				
		resetFilePointer(to_delete, config, _globals.get());
		
		return onUpdatedSource(to_delete, to_delete, false, Optional.empty(), context);				
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onDecommission(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onDecommission", "NYI"));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onPeriodicPoll(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		_logger.error("LOGSTASH: on periodic poll was called");
		final LogstashBucketConfigBean config = Optionals.ofNullable(polled_bucket.harvest_configs()).stream().findFirst()														
				.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
		//check if the job is still running
		//if yes: report its running
		//if no: restart job
		if ( isConfigRunning(polled_bucket, config, _globals.get())) {
			_logger.error("LOGSTASH: on periodic poll was called, config was running, do nothing");
			return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onPeriodicPoll", "Config is currently running!"));
		} else {
			_logger.error("LOGSTASH: on periodic poll was called, config was NOT running, restarting config job");
			return CompletableFuture.completedFuture(startOrUpdateLogstash(polled_bucket, config, _globals.get(), context));
			
		}
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onHarvestComplete(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onHarvestComplete", "NYI"));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onTestSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		_logger.error("LOGSTASH: test was called");
		
		// Kill any previously running tests
		ProcessUtils.stopProcess(this.getClass().getSimpleName(), test_bucket, _global_propertes.get().local_root_dir() + LOCAL_RUN_DIR_SUFFIX, Optional.of(2));
		
		// Build the logstash config file
		
		final LogstashBucketConfigBean config = 
				Optionals.ofNullable(test_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());				
		
		final Validation<BasicMessageBean, String> ls_config = getLogstashFormattedConfig(test_bucket, config, _globals.get(), context); 
		if (ls_config.isFail()) {
			return CompletableFuture.completedFuture(ls_config.fail());
		}
		
		// Launch the binary in a separate process
		
		//final ProcessBuilder pb = LogstashUtils.buildLogstashTest(_globals.get(), config, ls_config.success(), Optional.ofNullable(test_spec.requested_num_objects()).orElse(10L), Optional.empty());
		final ProcessBuilder pb = LogstashUtils.buildLogstashTest(_globals.get(), config, ls_config.success(), Optional.ofNullable(test_spec.requested_num_objects()).orElse(10L), Optional.of(test_bucket.full_name()));
		
		_logger.error("LOGSTASH: process built was: " + pb.command().toString());
		final Tuple2<String, String> err_pid = ProcessUtils.launchProcess(pb, this.getClass().getSimpleName(), test_bucket, _global_propertes.get().local_root_dir() + LOCAL_RUN_DIR_SUFFIX, Optional.of(new Tuple2<Long, Integer>(test_spec.max_run_time_secs(), 2)));
	
		if (null != err_pid._1()) {
			return CompletableFuture.completedFuture(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "onTestSource", "Bucket error: " + err_pid._1()));				
		}
		else {
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onTestSource", "Bucket launched: " + err_pid._2()));											
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	
	// EXECUTIVE UTILS
	
	/** Runs logstash in test mode before doing anything else, to check its formatting (otherwise deploying the config can crash the entire thread)
	 * @param script
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected BasicMessageBean validateLogstashConfigBeforeRunning(final String script, final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		
		final ProcessBuilder pb = LogstashUtils.buildLogstashTest(_globals.get(), config, script, 0L, Optional.empty());		
		try {
			final Process px = pb.start();
			final StringWriter outputAndError = new StringWriter();
			final OutputCollectorService outAndErrorStream = new OutputCollectorService(px.getInputStream(), new PrintWriter(outputAndError));
			outAndErrorStream.start();
			if (!px.waitFor(60L, TimeUnit.SECONDS)) { // exited
				px.destroy();
			}
			outAndErrorStream.join();
			
			int ret_val = px.exitValue();
			
			return ErrorUtils.buildMessage(ret_val == 0, this.getClass().getSimpleName(), "validateLogstashConfigBeforeRunning", outputAndError.toString());
		}
		catch (Exception e) {
			return ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "validateLogstashConfigBeforeRunning", ErrorUtils.getLongForm("{0}", e));
		}
	}
	
	/** Starts logstash (or updates if already present), by create a new config file and restarting the logstash service
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 * @throws IOException
	 */
	protected BasicMessageBean startOrUpdateLogstash(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals, final IHarvestContext context) {
		try {
			final Validation<BasicMessageBean, String> ls_config = getLogstashFormattedConfig(bucket, config, globals, context);
			if (ls_config.isSuccess()) {
				final BasicMessageBean validation = validateLogstashConfigBeforeRunning(ls_config.success(), bucket, config, globals);
				if (validation.success()) {
					final String config_file_path = getConfigFilePath(bucket, config, globals);
					FileUtils.write(new File(config_file_path), ls_config.success());
					createLogstashRestartCommand(bucket, config, globals);
					
					return ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "startOrUpdateLogstash", "Launched {0} for bucket {1}", config_file_path, bucket.full_name());
				}
				else {
					return validation;
				}
			}		
			else return ls_config.fail();
		}
		catch (Exception e) {
			return ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "startOrUpdateLogstash", ErrorUtils.getLongForm("{0}", e));
		}
	}
	
	/** Stops this logstash source (don't use this if going to restart), by deleting the config file and restarting the logstash service
	 * @param bucket
	 * @param config
	 * @param globals
	 * @throws IOException
	 */
	protected BasicMessageBean stopLogstash(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		try {
			final String config_file_path = getConfigFilePath(bucket, config, globals);
			final File config_file = new File(config_file_path);
			if (config_file.exists()) {
				config_file.delete();
				createLogstashRestartCommand(bucket, config, globals);
			}	
			return ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "stopLogstash", "(stopped logstash)");
		}
		catch (Exception e) {
			return ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "stopLogstash", ErrorUtils.getLongForm("{0}", e));
		}
	}	
	
	/** Creates the logstash restart file, which the v1 version of logstash periodically checks to decide whether to restart
	 * @param bucket
	 * @param config
	 * @param globals
	 * @throws IOException
	 */
	protected void createLogstashRestartCommand(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) throws IOException {
		FileUtils.touch(new File(globals.restart_file()));
	}
	
	/**
	 * Returns back if the given logstash config is already running.
	 * 
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected boolean isConfigRunning(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		final String config_file_path = getConfigFilePath(bucket, config, globals);
		final File config_file = new File(config_file_path);
		return config_file.exists();
	}

	protected void resetFilePointer(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		final String since_db_path = getFilePointer(bucket, config, globals);
		try {
			new File(since_db_path).delete();
		}
		catch (Exception e) {} // don't care
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// CONFIGURATION BUILDING
	
	/** Builds a working logstash formatted config
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected Validation<BasicMessageBean, String> getLogstashFormattedConfig(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals, final IHarvestContext context) {
		try {
			boolean is_admin = isAdmin(Optional.ofNullable(bucket.owner_id()).orElse(""), _context.get());
	
			// Input and filter
			
			final StringBuffer errMessage = new StringBuffer();
			String logstashConfig = LogstashConfigUtils.validateLogstashInput(globals, bucket.full_name(), config.script(), errMessage, is_admin);
			if (null == logstashConfig) { // Validation error...
				return Validation.fail(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "getLogstashFormattedConfig", errMessage.toString()));
			}//TESTED
			
			logstashConfig = logstashConfig.replace("_XXX_DOTSINCEDB_XXX_", getFilePointer(bucket, config, globals));
			// Replacement for #LOGSTASH{host} - currently only replacement supported (+ #IKANOW{} in main code)
			try {
				logstashConfig = logstashConfig.replace("#LOGSTASH{host}", java.net.InetAddress.getLocalHost().getHostName());
			}
			catch (Exception e) {
				logstashConfig = logstashConfig.replace("#LOGSTASH{host}", "localhost.localdomain");
				
			}
			
			String outputConfig = 
					LogstashUtils.getOutputTemplate(config.output_override(), bucket, _context.get().getServiceContext().getStorageService(), _globals.get().hadoop_mount_root(), context, config, _global_propertes.get())
									.replace("_XXX_SOURCEKEY_XXX_", bucket.full_name())
									;
			// Output
			
			return Validation.success(logstashConfig + "\n" + outputConfig);
		}
		catch (Exception e) {
			return Validation.fail(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "getLogstashFormattedConfig", ErrorUtils.getLongForm("{0}", e)));
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// Lower Level Utils
	
	/** Returns whether the current user is an admin
	 * @param user_id
	 * @param context
	 * @return
	 */
	protected static boolean isAdmin(final String user_id, IHarvestContext context) {
		final ISubject system_user = context.getServiceContext().getSecurityService().loginAsSystem();
		try {
			context.getServiceContext().getSecurityService().runAs(system_user, Arrays.asList(user_id)); // (Switch to bucket owner user)
			return context.getServiceContext().getSecurityService().hasRole(system_user, "admin");
		}
		finally {
			context.getServiceContext().getSecurityService().releaseRunAs(system_user);
		}
	}
	
	/** Returns the location of the file pointer
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected static String getFilePointer(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		return globals.working_dir() + ".sincedb_" + BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty());
	}
	
	/** Returns the config path + the file name
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected static String getConfigFilePath(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		return getConfigPath(globals) + File.separator + BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty()) + LogstashHarvesterConfigBean.LOGSTASH_CONFIG_EXTENSION;
	}
	
	/** Returns the config path (checks master first, then returns slave as backstop) 
	 * @param globals
	 * @return
	 */
	protected static String getConfigPath(LogstashHarvesterConfigBean globals) {
		if ((new File(globals.master_config_dir())).exists()) return globals.master_config_dir();
		else return globals.slave_config_dir();
	}	
}
