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
package com.ikanow.aleph2.harvest.script.services;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.harvest.script.data_model.ScriptHarvesterBucketConfigBean;
import com.ikanow.aleph2.harvest.script.data_model.ScriptHarvesterConfigBean;
import com.ikanow.aleph2.harvest.script.utils.ScriptUtils;

public class ScriptHarvestService implements IHarvestTechnologyModule {
	
	private static final Logger _logger = LogManager.getLogger();
	protected final SetOnce<ScriptHarvesterConfigBean> _globals = new SetOnce<>();
	protected final SetOnce<IHarvestContext> _context = new SetOnce<>();
	protected final SetOnce<GlobalPropertiesBean> _global_propertes = new SetOnce<>();
	
	protected static final boolean DEBUG = false;
	
	@Override
	public void onInit(IHarvestContext context) {
		if (DEBUG) _logger.error("SCRIPT: init");
		_globals.set(BeanTemplateUtils.from(Optional.ofNullable(context.getTechnologyLibraryConfig().library_config()).orElse(Collections.emptyMap()), ScriptHarvesterConfigBean.class).get());
		_context.set(context);
		_global_propertes.set(context.getServiceContext().getGlobalProperties());
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		if (DEBUG) _logger.error("SCRIPT: canRun");
		//if config has a required_assets field set, check they exist on this box, otherwise we can run anywhere
		final ScriptHarvesterBucketConfigBean config = 
				Optionals.ofNullable(bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());	
		return config.required_assets().stream().allMatch(ra -> new File(ra).exists());
	}

	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		if (enabled) {
			//TODO loop over every harvest config and run for every one enabled rather than just the first
			final ScriptHarvesterBucketConfigBean config = 
					Optionals.ofNullable(new_bucket.harvest_configs()).stream().findFirst()														
						.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
					.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());	
						
			return CompletableFuture.completedFuture(ScriptUtils.startScriptProcess(new_bucket, context, _global_propertes.get().local_root_dir(), config, "onNewSource", _globals.get().working_dir(), Optional.empty(), Optional.empty()));
		}
		else {		
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onNewSource", "Bucket {0} created but suspended", new_bucket.full_name()));
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			boolean is_enabled, Optional<BucketDiffBean> diff,
			IHarvestContext context) {
		if (DEBUG) _logger.error("SCRIPT: onUPDATE, enabled: " + is_enabled);
		// stop any currently running pid for this job, if enabled start up again
		final ScriptHarvesterBucketConfigBean config = 
				Optionals.ofNullable(new_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());	
				
		if (diff.map(bdb -> bdb.diffs().isEmpty()).orElse(false)) { // if nothing's changed then do nothing
			//TODO: longer term could do better here, eg we don't care unless data_schema or harvest_configs have changed, right?				
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onUpdatedSource", "No change to bucket"));			
		}
		if (is_enabled) {
			return CompletableFuture.completedFuture(ScriptUtils.restartScriptProcess(new_bucket, context, _global_propertes.get().local_root_dir(), config, "onDelete", _globals.get().working_dir(), Optional.empty(), Optional.empty()));
		}
		else { // Just stop
			//(this does nothing if the bucket isn't actually running)
			return CompletableFuture.completedFuture(ScriptUtils.stopScriptProcess(new_bucket, config, "onDelete", _globals.get().working_dir(), _global_propertes.get().local_root_dir()));
		}		
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
		// TODO nothing to do, scripts shouldn't be keeping any local state? or do we want to pass
		//a special ENV var that tells a script to purge
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onPurge", "NYI - if you have a need to purge a script, let me know how to implement"));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		// kill the process if its running
		final ScriptHarvesterBucketConfigBean config = 
				Optionals.ofNullable(to_delete.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());	
		
		return CompletableFuture.completedFuture(ScriptUtils.stopScriptProcess(to_delete, config, "onDelete", _globals.get().working_dir(), _global_propertes.get().local_root_dir()));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		//run stop because the process is moving off this node
		return onUpdatedSource(to_decommission, to_decommission, false, Optional.empty(), context);
	//	return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onDecommission", "NYI"));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		if (DEBUG) _logger.error("SCRIPT: onPeriodicPoll was called");
		
		final boolean is_running =  ScriptUtils.isProcessRunning(polled_bucket, _global_propertes.get().local_root_dir());
		final ScriptHarvesterBucketConfigBean config = 
				Optionals.ofNullable(polled_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());	
		
		if (is_running || !config.watchdog_enabled()) {
			return CompletableFuture.completedFuture(ErrorUtils.buildSuccessMessage(this.getClass().getSimpleName(), "onPeriodicPoll", "is process still running: " + is_running));			
		}
		else { // isn't running AND watch dog enabled, so restart
			return CompletableFuture.completedFuture(ScriptUtils.restartScriptProcess(polled_bucket, context, _global_propertes.get().local_root_dir(), config, "onPeriodicPoll", _globals.get().working_dir(), Optional.empty(), Optional.empty()));
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		return CompletableFuture.completedFuture(ErrorUtils.buildMessage(true, this.getClass().getSimpleName(), "onHarvestComplete", "NYI"));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		if (DEBUG) _logger.error("SCRIPT: test was called");
				
		//TODO loop over every harvest config and run for every one enabled rather than just the first
		final ScriptHarvesterBucketConfigBean config = 
				Optionals.ofNullable(test_bucket.harvest_configs()).stream().findFirst()														
					.map(cfg -> BeanTemplateUtils.from(cfg.config(), ScriptHarvesterBucketConfigBean.class).get())
				.orElse(BeanTemplateUtils.build(ScriptHarvesterBucketConfigBean.class).done().get());		
		//kill any already running scripts
		ScriptUtils.stopScriptProcess(test_bucket, config, "onTestSource", _globals.get().working_dir(), _global_propertes.get().local_root_dir());
		
		return CompletableFuture.completedFuture(ScriptUtils.startScriptProcess(test_bucket, context, _global_propertes.get().local_root_dir(), config, "onTestSource", _globals.get().working_dir(), Optional.of(test_spec.requested_num_objects()), Optional.of(test_spec.max_run_time_secs())));
	}

}
