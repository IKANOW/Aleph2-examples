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
package com.ikanow.aleph2.example.flume_harvester.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;




import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




import scala.Tuple2;




import com.codepoetics.protonpack.StreamUtils;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.FutureUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Tuples;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeGlobalConfigBean;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean.InputConfig.SpoolDirConfig;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeLaunchUtils;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeUtils;




import java.util.Collections;

/** Flume harvester entry points
 * @author Alex
 */
public class FlumeHarvestTechnology implements IHarvestTechnologyModule {
	protected Logger _logger = LogManager.getLogger();
	protected FlumeGlobalConfigBean _globals;
	
	///////////////////////////////////////////////////
	
	// C'TOR
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onInit(com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public void onInit(IHarvestContext context) {
		_globals = BeanTemplateUtils.from(Optional.ofNullable(context.getTechnologyLibraryConfig().library_config()).orElse(Collections.emptyMap()), FlumeGlobalConfigBean.class).get();		
	}

	///////////////////////////////////////////////////
	
	// UTILS
	
	/** Creates a temp file from a prefix + unique id
	 * @param prefix
	 * @param unique_filename
	 * @return
	 */
	protected static File createTempFile(final String prefix, final String unique_filename) {
		return new File(System.getProperty("java.io.tmpdir") + File.separator + prefix + unique_filename);
	}
	
	/** Starts/restarts a flume agent by creating a per-bucket config for that element
	 *  TODO (ALEPH-10): NOTE: currently only one config is allowed per agent - I didn't realize you needed one jvm per agent ...
	 *  the only way of having multiple configs per JVM is to generate multiple sources and sinks, ie merge all the config elements into 1
	 *  I'll leave it like this for the moment, in case the plan later is to spawn multiple processes from the one harvester
	 *  (which would have the advantage of not having to alter unaffected agents, which might be tricky to calculate of course) 
	 * @param agent_num
	 * @param bucket
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	protected Tuple2<String, File> updateAgentConfig(final int agent_num, final DataBucketBean bucket, final FlumeBucketConfigBean config, final IHarvestContext context,
			final boolean test_mode
			) 
			throws IOException
	{
		//TODO (ALEPH-10): unit test for this 
		
		final String agent_name = FlumeUtils.getConfigName(bucket.full_name(), Optional.of(Integer.toString(agent_num)));
		
		// Is morphlines configured?
		final Optional<String> morphlines_file = Optional.ofNullable(config.morphlines_config())
				.flatMap(Lambdas.wrap_u(m_cfg -> {
					final File tmp_morph = createTempFile("aleph2_morph_", agent_name);
					final Optional<String> morph_cfg = FlumeUtils.createMorphlinesConfig(config);
					return morph_cfg.map(Lambdas.wrap_u(mcfg -> {
						FileUtils.writeStringToFile(tmp_morph, mcfg);	
						return tmp_morph.toString();
					}));
					
				}));		
		final File tmp_flume = createTempFile("aleph2_flume_", agent_name);
		final String flume_config = FlumeUtils.createFlumeConfig(agent_name, bucket, config, 
													context.getHarvestContextSignature(Optional.of(bucket), FlumeLaunchUtils.getContextLibraries(Optional.of(config))), 
													morphlines_file, test_mode);
		FileUtils.writeStringToFile(tmp_flume, flume_config);
		
		return Tuples._2T(agent_name, tmp_flume);
	}
		
	/** Stops any agents associated with this bucket
	 *  NOTE: currently only one config is allowed per agent
	 * @param bucket
	 */
	protected int removeAgentConfigs(final DataBucketBean bucket, final int from) {
		//TODO (ALEPH-10): unit test for this 
		
		int stopped = 0;
		for (int i = from; i < 256; ++i) {
			
			final String agent_name = FlumeUtils.getConfigName(bucket.full_name(), Optional.of(Integer.toString(i)));
			
			//(delete morphline if it exists)
			final File f_morph = createTempFile("aleph2_morph_", agent_name);
			if (f_morph.exists()) f_morph.delete();
			
			final File f = createTempFile("aleph2_flume_", agent_name);
			if (f.exists()) {
				stopped += f.delete() ? 1 : 0;
			}
			else {
				break; //all done
			}
		}
		return stopped;
	}
	
	///////////////////////////////////////////////////
	
	// INTERFACE - MAIN FUNCTIONALITY
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#canRunOnThisNode(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		
		if (!(new File(_globals.flume_config_path()).canRead())) {
			_logger.info("No directory: " + _globals.flume_config_path() + ", or not readable");
			return false;
		}
		if (!(new File(_globals.flume_service_path()).canExecute())) {
			_logger.info("No file: " + _globals.flume_service_path() + ", or not executable");
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onNewSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext, boolean)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		
		return onNewSource(new_bucket, context, enabled, false);
	}

	/** Worker for starting a flume job or test
	 * @param new_bucket - the bucket to start/test
	 * @param context - the harvest context
	 * @param enabled - whether the job is enabled
	 * @param test_mode - whether the job is being run as a test
	 * @return
	 */
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled, boolean test_mode) {
		//TODO (ALEPH-10): unit test for this 
		
		try {
			// Create an agent per config element:
			
			if (enabled) {
				final List<FlumeBucketConfigBean> agents = FlumeUtils.getAgents(new_bucket);
				
				@SuppressWarnings("unused")
				final int stopped = removeAgentConfigs(new_bucket, 1);
				
				final Tuple2<String, Boolean> delete_result = FlumeLaunchUtils.killProcess(FlumeLaunchUtils.getPid(new_bucket)); 
					//(safe side, always kill - should fail harmlessly if px already dead....)
				
				final List<Tuple2<String, File>> agent_paths = 
						StreamUtils.zip(agents.stream(), Stream.iterate(1, i -> i+1), (a, b) -> Tuples._2T(a, b))
									.map(Lambdas.wrap_u(agent_index -> updateAgentConfig(agent_index._2(), new_bucket, agent_index._1(), context, test_mode)))
									.collect(Collectors.toList())
									;
				
				final List<Tuple2<String, String>> err_pids = agent_paths.stream()
																.map(agent_path -> FlumeLaunchUtils.launchProcess(new_bucket, _globals, agent_path, context))
																.collect(Collectors.toList());

				if (err_pids.isEmpty()) {
					return CompletableFuture.completedFuture(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "onNewSource", "Found no valid Flume configs " + delete_result._1()));									
				}
				else {
					final Tuple2<String, String> err_pid = err_pids.get(0);
					if (null != err_pid._1()) {
						return CompletableFuture.completedFuture(ErrorUtils.buildErrorMessage(this.getClass().getSimpleName(), "onNewSource", "Bucket error: " + err_pid._1() + " " + delete_result._1()));				
					}
					else {
						return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onNewSource", "Bucket launched: " + err_pid._2() + " " + delete_result._1()));								
						
					}
				}
			}
			else {
				return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onNewSource", "Created in suspended mode"));								
			}
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onNewSource: Unknown error {0}", e));
			return FutureUtils.returnError(e);
		}
	}

	/**
	 * @param to_suspend
	 * @param context
	 * @return
	 */
	protected CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		try {
			int stopped = removeAgentConfigs(to_suspend, 1);
			FlumeLaunchUtils.killProcess(FlumeLaunchUtils.getPid(to_suspend));			
			
			if (BucketUtils.isTestBucket(to_suspend)) {
				//(clean up any generated files now that the test is over)
				FlumeUtils.getAgents(to_suspend).stream().forEach(agent -> 
					FlumeUtils.deleteGeneratedDirs(to_suspend, agent, true));
			}			
			return CompletableFuture.completedFuture(new BasicMessageBean(
					new Date(), true, "onSuspend", "onSuspend", null, "Stopped " + stopped + " agents", null));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onSuspend: Unknown error {0}", e));
			return FutureUtils.returnError(e);
		}
	}	
	
	///////////////////////////////////////////////////
	
	// INTERFACE - JUST CALLS MAIN FUNCTIONALITY
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onUpdatedSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, boolean, java.util.Optional, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			boolean is_enabled, Optional<BucketDiffBean> diff,
			IHarvestContext context) {
		
		CompletableFuture<BasicMessageBean> reply = Lambdas.get(() -> {
			if (is_enabled) {
				return onNewSource(new_bucket, context, is_enabled);
			}
			else {
				return onSuspend(new_bucket, context);
			}
		});
		
		return reply;
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onPurge(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
				
		//(clean up any generated files)
		FlumeUtils.getAgents(to_purge).stream().forEach(agent -> 
			FlumeUtils.deleteGeneratedDirs(to_purge, agent, BucketUtils.isTestBucket(to_purge)));
		
		return CompletableFuture.completedFuture(new BasicMessageBean(
				new Date(), true, "onPurge", "onPurge", null, "No action taken", null));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onDelete(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		
		//(clean up any generated files)
		FlumeUtils.getAgents(to_delete).stream().forEach(agent -> 
			FlumeUtils.deleteGeneratedDirs(to_delete, agent, BucketUtils.isTestBucket(to_delete)));
				
		return onSuspend(to_delete, context);
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onDecommission(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		
		//(will also purge the tracker directory, ie reset knowledge of files)
		FlumeUtils.getAgents(to_decommission).stream().forEach(agent -> 
			FlumeUtils.deleteGeneratedDirs(to_decommission, agent, BucketUtils.isTestBucket(to_decommission)));
		
		return onSuspend(to_decommission, context);
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onPeriodicPoll(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		
		// (If we're here then we're not suspended by definition)
		
		// Check if it's running
		final Optional<String> maybe_pid = Optional.ofNullable(FlumeLaunchUtils.getPid(polled_bucket));
		final boolean is_running = maybe_pid.map(Lambdas.wrap_u(pid -> FlumeLaunchUtils.isRunning(pid))).orElse(false);
		if (!is_running) { // restart it if not
			return onNewSource(polled_bucket, context, true);
		}
		else {
			return CompletableFuture.completedFuture(new BasicMessageBean(
					new Date(), true, "onPeriodicPoll", "onPeriodicPoll", null, "No action taken", null));
		}
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onHarvestComplete(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		return CompletableFuture.completedFuture(new BasicMessageBean(
				new Date(), true, "onHarvestComplete", "onHarvestComplete", null, "No action taken", null));
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule#onTestSource(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean, com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext)
	 */
	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		try {
			// First try stopping any existing running service:
			this.onSuspend(test_bucket, context);
			
			// Then clean up from the previous test just in case:
			FlumeUtils.getAgents(test_bucket).stream().forEach(agent -> {
				final Collection<SpoolDirConfig> spool_dirs = FlumeUtils.getSpoolDirs(agent);
				FlumeUtils.deleteGeneratedDirs(test_bucket, spool_dirs, true);
				spool_dirs.stream()
					.forEach(Lambdas.wrap_consumer_u(v -> {
						//TODO (ALEPH-10): handle HDFS for the overriden src path (so can go fetch
						//TODO (ALEPH-10): security for this setting...
						final File dest_path = new File(v.path() + "/" + BucketUtils.getUniqueSignature(test_bucket.full_name(), Optional.empty()));
						FileUtils.forceMkdir(dest_path);						
						final File src_path = new File(Optional.ofNullable(v.test_src_path()).orElse(v.path()));
						if (src_path.exists()) {
							FileUtils.listFiles(src_path, null, false).forEach(Lambdas.wrap_consumer_u(file -> {
								Files.createLink(new File(dest_path.toString() + "/" + file.getName()).toPath(), file.toPath());
							}));
						}
						//(else just nothing to read on this node)
					}));
			});		
			
			// Now start a normal source except in test mode
			final CompletableFuture<BasicMessageBean> reply = onNewSource(test_bucket, context, true, true);
			
			return reply;
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onTestSource: Unknown error {0}", e));
			return FutureUtils.returnError(e);
		}
	}

}
