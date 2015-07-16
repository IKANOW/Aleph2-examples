/******************************************************************************
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
 ******************************************************************************/
package com.ikanow.aleph2.example.external_harvester.services;

import java.net.InetAddress;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.interfaces.data_services.IManagementDbService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.data_model.utils.FutureUtils;
import com.ikanow.aleph2.data_model.utils.Tuples;
import com.ikanow.aleph2.example.external_harvester.utils.ProcessUtils;
import com.ikanow.aleph2.example.external_harvester.data_model.GlobalConfigBean;
import com.ikanow.aleph2.example.external_harvester.data_model.ProcessInfoBean;

import java.util.Arrays;

public class ExternalProcessHarvestTechnology implements IHarvestTechnologyModule {
	protected static Logger _logger = LogManager.getLogger(); 

	//////////////////////////////////////////////////////////////////////////////////
	
	// FUNCTIONS THAT NEED TO BE IMPLEMENTED (JUNE RELEASE)
	
	@Override
	public void onInit(IHarvestContext context) {
		//Nothing to do
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket, IHarvestContext context) {
		return true;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket,
			IHarvestContext context, boolean enabled) {
		try {
			final Tuple2<SharedLibraryBean, Optional<GlobalConfigBean>> global_config = getConfig(context);
			if (enabled) {
				Tuple2<String, String> err_or_pid = ProcessUtils.launchProcess(new_bucket, context);
				if (null != err_or_pid._1()) {
					return CompletableFuture.completedFuture(
							getMessage(false, "onNewSource", "Bucket error: " + err_or_pid._1()));				
				}
				else {
					// (log)
					global_config._2().ifPresent(g -> {
						if (g.store_pids_in_db()) updateProcessDatabase(err_or_pid._2(), new_bucket.full_name(), global_config._1(), context, true);
					});		
					
					return CompletableFuture.completedFuture(
							getMessage(true, "onNewSource", "Bucket launched: " + err_or_pid._2()));								
				}
			}
			else {
				return CompletableFuture.completedFuture(
						getMessage(true, "onNewSource", "Bucket suspended"));
			}
		}
		catch (Exception e) {
			return FutureUtils.returnError(e);
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket,
			DataBucketBean new_bucket, boolean is_enabled,
			Optional<BucketDiffBean> diff, IHarvestContext context) {

		try {
			final String pid_to_suspend = ProcessUtils.getPid(old_bucket);
			final Tuple2<SharedLibraryBean, Optional<GlobalConfigBean>> global_config = getConfig(context);
			
			//kill/log
			final Tuple2<String, Boolean> kill_result = ProcessUtils.killProcess(pid_to_suspend);
			global_config._2().ifPresent(g -> {
				if (g.store_pids_in_db()) updateProcessDatabase(pid_to_suspend, old_bucket.full_name(), global_config._1(), context, false);
			});
			
			if (!kill_result._2()) {
				return CompletableFuture.completedFuture(
						getMessage(false, "onUpdatedSource", "Bucket suspended: " + kill_result._1()));							
			}
			if (is_enabled) {
				return onNewSource(new_bucket, context, is_enabled);
			}
			else {
				return CompletableFuture.completedFuture(getMessage(true, "onUpdatedSource", "Bucket suspended: " + kill_result._1()));
			}
		}
		catch (Exception e) {
			return FutureUtils.returnError(e);
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		
		try {
			final String pid_to_suspend = ProcessUtils.getPid(to_suspend);
			final Tuple2<SharedLibraryBean, Optional<GlobalConfigBean>> global_config = getConfig(context);
			
			//kill/log
			final Tuple2<String, Boolean> kill_result = ProcessUtils.killProcess(pid_to_suspend);
			global_config._2().ifPresent(g -> {
				if (g.store_pids_in_db()) updateProcessDatabase(pid_to_suspend, to_suspend.full_name(), global_config._1(), context, false);
			});
			
			return CompletableFuture.completedFuture(
					getMessage(kill_result._2(), "onSuspend", "Bucket suspended: " + kill_result._1()));			
		}
		catch (Exception e) {
			return FutureUtils.returnError(e);
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onResume(
			DataBucketBean to_resume, IHarvestContext context) {
		return onNewSource(to_resume, context, true);
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		return onSuspend(to_delete, context);
	}

	//////////////////////////////////////////////////////////////////////////////////

	// UTILS
	
	protected BasicMessageBean getMessage(final boolean success, final String source, final String message) {
		return new BasicMessageBean(new Date(), success, "ExternalProcessHarvestTechnology", source, 
				null, message, null);		
	}
	
	public static Tuple2<SharedLibraryBean, Optional<GlobalConfigBean>> getConfig(final IHarvestContext context) {
		final SharedLibraryBean lib = context.getLibraryConfig();
		return Tuples._2T(lib, Optional.ofNullable(lib.library_config())
					.map(j -> BeanTemplateUtils.from(j, GlobalConfigBean.class).get()));
	}
	
	protected void updateProcessDatabase(final String pid, final String bucket_name, final SharedLibraryBean lib, final IHarvestContext context, boolean add_not_remove) {
		
		final IManagementDbService core_db = context.getService(IManagementDbService.class, IManagementDbService.CORE_MANAGEMENT_DB).get();
		
		final ICrudService<ProcessInfoBean> pid_crud = core_db.getPerLibraryState(ProcessInfoBean.class, lib, ProcessInfoBean.PID_COLLECTION_NAME);
		
		pid_crud.optimizeQuery(Arrays.asList("pid", "hostname"));
		pid_crud.optimizeQuery(Arrays.asList("bucket_name"));
		
		final String hostname = getHostname();
		
		if (add_not_remove) {
			final ProcessInfoBean pid_info = new ProcessInfoBean(pid, hostname, bucket_name);
			pid_crud.storeObject(pid_info, true).thenAccept(__ -> {
				_logger.info("Saved PID|host|bucket: " + pid + "|" + hostname + "|" + bucket_name);
			})
			.exceptionally(err -> {
				_logger.error("Failed to save PID|host|bucket: " + pid + "|" + hostname + "|" + bucket_name, err);
				return null;
			})
			;
		}
		else { // remove not add!
			pid_crud.deleteObjectBySpec(CrudUtils.allOf(ProcessInfoBean.class).when(ProcessInfoBean::pid, pid).when(ProcessInfoBean::hostname, hostname))
				.thenAccept(ret -> {
					if (ret) {
						_logger.info("Removed PID|host|bucket: " + pid + "|" + hostname + "|" + bucket_name);						
					}
					else {
						_logger.warn("Not present, ignored: PID|host|bucket: " + pid + "|" + hostname + "|" + bucket_name);						
					}
				})
				.exceptionally(err -> {
					_logger.error("Failed to remove PID|host|bucket: " + pid + "|" + hostname + "|" + bucket_name, err);
					return null;
				})
			;
		}		
	}
	
	private static String _hostname;
	/** Returns the hostname
	 * @return
	 */
	public static String getHostname() {
		// (just get the hostname once)
		if (null == _hostname) {
			try {
				_hostname = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				_hostname = "UNKNOWN";
			}
		}		
		return _hostname;
	}//TESTED		
	
	//////////////////////////////////////////////////////////////////////////////////

	// CURRENTLY UNUSED FUNCTIONS
	
	@Override
	public CompletableFuture<BasicMessageBean> onPurge(
			DataBucketBean to_purge, IHarvestContext context) {
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket,
			IHarvestContext context) {
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket,
			IHarvestContext context) {
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket,
			ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		return null;
	}

}
