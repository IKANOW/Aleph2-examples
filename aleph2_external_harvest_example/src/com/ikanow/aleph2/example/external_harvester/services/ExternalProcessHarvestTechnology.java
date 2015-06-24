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

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.checkerframework.checker.nullness.qual.NonNull;

import scala.Tuple2;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.FutureUtils;
import com.ikanow.aleph2.example.external_harvester.utils.ProcessUtils;

public class ExternalProcessHarvestTechnology implements IHarvestTechnologyModule {

	//////////////////////////////////////////////////////////////////////////////////
	
	// FUNCTIONS THAT NEED TO BE IMPLEMENTED (JUNE RELEASE)
	
	@Override
	public void onInit(@NonNull IHarvestContext context) {
		//Nothing to do
	}

	@Override
	public boolean canRunOnThisNode(@NonNull DataBucketBean bucket, @NonNull IHarvestContext context) {
		return true;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onNewSource(
			@NonNull DataBucketBean new_bucket,
			@NonNull IHarvestContext context, boolean enabled) {
		try {
			if (enabled) {
				Tuple2<String, String> err_or_pid = ProcessUtils.launchProcess(new_bucket, context);
				if (null != err_or_pid._1()) {
					return CompletableFuture.completedFuture(
							getMessage(false, "onNewSource", "Bucket error: " + err_or_pid._1()));				
				}
				else {
					return CompletableFuture.completedFuture(
							getMessage(false, "onNewSource", "Bucket launched: " + err_or_pid._2()));								
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
	public @NonNull CompletableFuture<BasicMessageBean> onUpdatedSource(
			@NonNull DataBucketBean old_bucket,
			@NonNull DataBucketBean new_bucket, boolean is_enabled,
			Optional<BucketDiffBean> diff, @NonNull IHarvestContext context) {

		try {
			final Tuple2<String, Boolean> kill_result = ProcessUtils.killProcess(ProcessUtils.getPid(old_bucket));
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
	public @NonNull CompletableFuture<BasicMessageBean> onSuspend(
			@NonNull DataBucketBean to_suspend, @NonNull IHarvestContext context) {
		
		try {
			final Tuple2<String, Boolean> kill_result = ProcessUtils.killProcess(ProcessUtils.getPid(to_suspend));
			return CompletableFuture.completedFuture(
					getMessage(kill_result._2(), "onSuspend", "Bucket suspended: " + kill_result._1()));			
		}
		catch (Exception e) {
			return FutureUtils.returnError(e);
		}
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onResume(
			@NonNull DataBucketBean to_resume, @NonNull IHarvestContext context) {
		return onNewSource(to_resume, context, true);
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onDelete(
			@NonNull DataBucketBean to_delete, @NonNull IHarvestContext context) {
		return onSuspend(to_delete, context);
	}

	//////////////////////////////////////////////////////////////////////////////////

	// UTILS
	
	BasicMessageBean getMessage(final boolean success, final String source, final String message) {
		return new BasicMessageBean(new Date(), success, "ExternalProcessHarvestTechnology", source, 
				null, message, null);		
	}
	
	//////////////////////////////////////////////////////////////////////////////////

	// CURRENTLY UNUSED FUNCTIONS
	
	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onPurge(
			@NonNull DataBucketBean to_purge, @NonNull IHarvestContext context) {
		return null;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onPeriodicPoll(
			@NonNull DataBucketBean polled_bucket,
			@NonNull IHarvestContext context) {
		return null;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onHarvestComplete(
			@NonNull DataBucketBean completed_bucket,
			@NonNull IHarvestContext context) {
		return null;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onTestSource(
			@NonNull DataBucketBean test_bucket,
			@NonNull ProcessingTestSpecBean test_spec,
			@NonNull IHarvestContext context) {
		return null;
	}

}
