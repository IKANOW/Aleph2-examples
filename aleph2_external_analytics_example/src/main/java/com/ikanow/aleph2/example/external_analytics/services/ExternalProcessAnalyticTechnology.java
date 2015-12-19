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
package com.ikanow.aleph2.example.external_analytics.services;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadJobBean;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadTriggerBean.AnalyticThreadComplexTriggerBean;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.FutureUtils.ManagementFuture;

/** Analytic Technology Service Entry Point
 * @author Alex
 */
public class ExternalProcessAnalyticTechnology implements IAnalyticsTechnologyModule {

	@Override
	public void onInit(IAnalyticsContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onNewThread(
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context,
			boolean enabled) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedThread(
			DataBucketBean old_analytic_bucket,
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, boolean is_enabled,
			Optional<BucketDiffBean> diff, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDeleteThread(
			DataBucketBean to_delete_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ManagementFuture<Boolean> checkCustomTrigger(
			DataBucketBean analytic_bucket,
			AnalyticThreadComplexTriggerBean trigger, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onThreadExecute(
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			Collection<AnalyticThreadComplexTriggerBean> matching_triggers,
			IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onThreadComplete(
			DataBucketBean completed_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPurge(
			DataBucketBean purged_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onTestThread(
			DataBucketBean test_bucket, Collection<AnalyticThreadJobBean> jobs,
			ProcessingTestSpecBean test_spec, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> startAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_start, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> stopAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_stop, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> resumeAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_resume, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> suspendAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_suspend, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> startAnalyticJobTest(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_test,
			ProcessingTestSpecBean test_spec, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ManagementFuture<Boolean> checkAnalyticJobProgress(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_check, IAnalyticsContext context) {
		// TODO Auto-generated method stub
		return null;
	}

}
