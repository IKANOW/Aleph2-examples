package com.ikanow.aleph2.harvest.logstash.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import scala.Tuple2;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashBucketConfigBean;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;
import com.ikanow.aleph2.harvest.logstash.utils.LogstashUtils;
import com.ikanow.aleph2.harvest.logstash.utils.ProcessUtils;

import fj.data.Validation;

/** Logstash harvester interface
 * @author Alex
 */
public class LogstashHarvestService implements IHarvestTechnologyModule {
	protected final SetOnce<LogstashHarvesterConfigBean> _globals = new SetOnce<>();
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onInit(com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public void onInit(IHarvestContext context) {
		_globals.set(BeanTemplateUtils.from(Optional.ofNullable(context.getTechnologyLibraryConfig().library_config()).orElse(Collections.emptyMap()), LogstashHarvesterConfigBean.class).get());		
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#canRunOnThisNode(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		
		final File master = new File(_globals.get().master_config_dir()); 
		final File slave = new File(_globals.get().slave_config_dir()); 
		if (BucketUtils.isTestBucket(bucket)) {
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
						
			return CompletableFuture.completedFuture(startOrUpdateLogstash(new_bucket, config, _globals.get()));
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
				
		// Handle test case - use process utils to delete
		if (BucketUtils.isTestBucket(new_bucket)) {
					
			final String pid_to_suspend = ProcessUtils.getPid(new_bucket);
			
			//TODO: need to kill the ".sincedb" file
			
			//kill/log
			final Tuple2<String, Boolean> kill_result = ProcessUtils.killProcess(pid_to_suspend);
			
			return CompletableFuture.completedFuture(
					ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "Bucket suspended: {0}", kill_result._1()));
		}
		else {
			if (diff.map(bdb -> bdb.diffs().isEmpty()).orElse(false)) { // if nothing's changed then do nothing
				//TODO: longer term could do better here, eg we don't care unless data_schema or harvest_configs have changed, right?				
				return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onUpdatedSource", "No change to bucket"));			
			}
			final LogstashBucketConfigBean config = Optionals.ofNullable(new_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
					.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
			
			if (is_enabled) {
				return CompletableFuture.completedFuture(startOrUpdateLogstash(new_bucket, config, _globals.get()));
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
		
		// TODO Auto-generated method stub
		// Look for .sincedb files for this bucket and delete if present
		
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onPurge", "NYI"));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onDelete(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		
		// TODO Auto-generated method stub
		// Look for .sincedb files for this bucket and delete if present
		
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
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onPeriodicPoll", "NYI"));
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
		
		// Build the logstash config file
		
		final LogstashBucketConfigBean config = 
				Optionals.ofNullable(test_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), LogstashBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(LogstashBucketConfigBean.class).done().get());
		
		final Validation<BasicMessageBean, String> ls_config = getLogstashFormattedConfig(test_bucket, config, _globals.get()); 
		if (ls_config.isFail()) {
			return CompletableFuture.completedFuture(ls_config.fail());
		}
		
		// Launch the binary in a separate process
		
		final ProcessBuilder pb = LogstashUtils.buildLogstashTest(_globals.get(), config, ls_config.success(), Optional.ofNullable(test_spec.requested_num_objects()).orElse(10L));
		
		final Tuple2<String, String> err_pid = ProcessUtils.launchProcess(pb, test_bucket, context);
	
		if (null != err_pid._1()) {
			return CompletableFuture.completedFuture(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "onTestSource", "Bucket error: " + err_pid._1()));				
		}
		else {
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onTestSource", "Bucket launched: " + err_pid._2()));											
		}
	}

	////////////////////////////////////////////////////////////////////////////////
	
	// EXECUTIVE UTILS
	
	/** Runs logstash in test mode before doing anything else to check its formatting
	 * @param script
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected BasicMessageBean validateLogstashConfigBeforeRunning(final String script, final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		
		final ProcessBuilder pb = LogstashUtils.buildLogstashTest(_globals.get(), config, script, 0L);		
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
			
			return ErrorUtils.buildMessage(ret_val != 0, this.getClass().getSimpleName(), "validateLogstashConfigBeforeRunning", outputAndError.toString());
		}
		catch (Exception e) {
			return ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "validateLogstashConfigBeforeRunning", ErrorUtils.getLongForm("{0}", e));
		}
	}
	
	/** Starts logstash (or updates if already present)
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 * @throws IOException
	 */
	protected BasicMessageBean startOrUpdateLogstash(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		try {
			final Validation<BasicMessageBean, String> ls_config = getLogstashFormattedConfig(bucket, config, globals);
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
	
	/** Stops this logstash source (don't use this if going to restart)
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
	
	/** Builds a working logstash formatted config
	 * @param bucket
	 * @param config
	 * @param globals
	 * @return
	 */
	protected Validation<BasicMessageBean, String> getLogstashFormattedConfig(final DataBucketBean bucket, final LogstashBucketConfigBean config, final LogstashHarvesterConfigBean globals) {
		
		//TODO
		
		return null;
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

	////////////////////////////////////////////////////////////////////////////////
	
	// Lower Level Utils
	
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
