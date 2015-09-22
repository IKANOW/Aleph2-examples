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
package com.ikanow.aleph2.test.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsTechnologyModule;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IUnderlyingService;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadJobBean;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadTriggerBean.AnalyticThreadComplexTriggerBean;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.FutureUtils;
import com.ikanow.aleph2.data_model.utils.FutureUtils.ManagementFuture;

/** An empty analytics technology example
 * @author alex
 */
public class ExampleAnalyticsTechnology implements IAnalyticsTechnologyModule {
	protected static final Logger _logger = LogManager.getLogger();

	@Override
	public void onInit(IAnalyticsContext context) {
		_logger.info("onInit");		
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		
		_logger.info("canRunOnThisNode");		
		return true;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onNewThread(
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context,
			boolean enabled) {
		_logger.info("onNewThread: " + enabled);

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(new_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(new_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onNewSource {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_analytic_bucket.display_name(),
						"onNewThread",
						null, // message code
						"called onNewThread: " + enabled,
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedThread(
			DataBucketBean old_analytic_bucket,
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, boolean is_enabled,
			Optional<BucketDiffBean> diff, IAnalyticsContext context) {
		_logger.info("onUpdatedThread: " + is_enabled);

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(new_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(new_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onUpdatedThread {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_analytic_bucket.display_name(),
						"onUpdatedThread",
						null, // message code
						"called onUpdatedThread: " + is_enabled,
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDeleteThread(
			DataBucketBean to_delete_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		_logger.info("onDeleteThread");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(to_delete_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(to_delete_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onDeleteThread {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_delete_analytic_bucket.display_name(),
						"onDeleteThread",
						null, // message code
						"called onDeleteThread",
						null // details
						));
	}

	@Override
	public ManagementFuture<Boolean> checkCustomTrigger(
			DataBucketBean analytic_bucket,
			AnalyticThreadComplexTriggerBean trigger, IAnalyticsContext context) {
		_logger.info("checkCustomTrigger");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("checkCustomTrigger {0}", e));
		}
		
		return FutureUtils.<Boolean>createManagementFuture(
				CompletableFuture.<Boolean>completedFuture(true),
				CompletableFuture.<Collection<BasicMessageBean>>completedFuture(
					Arrays.asList(
						new BasicMessageBean(
								new Date(), // date
								true, // success
								analytic_bucket.display_name(),
								"checkCustomTrigger",
								null, // message code
								"called checkCustomTrigger",
								null // details
								))));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onThreadExecute(
			DataBucketBean new_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			Collection<AnalyticThreadComplexTriggerBean> matching_triggers,
			IAnalyticsContext context) {
		_logger.info("onThreadExecute");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(new_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(new_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onThreadExecute {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_analytic_bucket.display_name(),
						"onThreadExecute",
						null, // message code
						"called onThreadExecute",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onThreadComplete(
			DataBucketBean completed_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		_logger.info("onThreadComplete");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(completed_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(completed_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onThreadComplete {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						completed_analytic_bucket.display_name(),
						"onThreadComplete",
						null, // message code
						"called onThreadComplete",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPurge(
			DataBucketBean purged_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		_logger.info("onPurge");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(purged_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(purged_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onPurge {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						purged_analytic_bucket.display_name(),
						"onPurge",
						null, // message code
						"called onPurge",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs, IAnalyticsContext context) {
		_logger.info("onPeriodicPoll");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(polled_analytic_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(polled_analytic_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onPeriodicPoll {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						polled_analytic_bucket.display_name(),
						"onPeriodicPoll",
						null, // message code
						"called onPeriodicPoll",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> onTestThread(
			DataBucketBean test_bucket, Collection<AnalyticThreadJobBean> jobs,
			ProcessingTestSpecBean test_spec, IAnalyticsContext context) {
		_logger.info("onTestThread");

		// Log some information about the request
		try {
			final String core_list = context.getAnalyticsContextLibraries(Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Analytics library paths: " + context.getAnalyticsLibraries(Optional.of(test_bucket), jobs).get());
			_logger.info("Analytics signature: " + context.getAnalyticsContextSignature(Optional.of(test_bucket), Optional.<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>>empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onTestThread {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						test_bucket.display_name(),
						"onTestThread",
						null, // message code
						"called onTestThread",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> startAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_start, IAnalyticsContext context) {
		_logger.info("startAnalyticJob");
	
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						analytic_bucket.display_name(),
						"startAnalyticJob",
						null, // message code
						"called startAnalyticJob",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> stopAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_stop, IAnalyticsContext context) {
		_logger.info("stopAnalyticJob");
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						analytic_bucket.display_name(),
						"stopAnalyticJob",
						null, // message code
						"called stopAnalyticJob",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> resumeAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_resume, IAnalyticsContext context) {
		_logger.info("resumeAnalyticJob");
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						analytic_bucket.display_name(),
						"resumeAnalyticJob",
						null, // message code
						"called resumeAnalyticJob",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> suspendAnalyticJob(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_suspend, IAnalyticsContext context) {
		_logger.info("suspendAnalyticJob");
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						analytic_bucket.display_name(),
						"suspendAnalyticJob",
						null, // message code
						"called suspendAnalyticJob",
						null // details
						));
	}

	@Override
	public CompletableFuture<BasicMessageBean> startAnalyticJobTest(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_test,
			ProcessingTestSpecBean test_spec, IAnalyticsContext context) {
		_logger.info("startAnalyticJobTest");
		
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						analytic_bucket.display_name(),
						"startAnalyticJobTest",
						null, // message code
						"called startAnalyticJobTest",
						null // details
						));
	}

	@Override
	public ManagementFuture<Boolean> checkAnalyticJobProgress(
			DataBucketBean analytic_bucket,
			Collection<AnalyticThreadJobBean> jobs,
			AnalyticThreadJobBean job_to_check, IAnalyticsContext context) {
		_logger.info("checkAnalyticJobProgress");

		return FutureUtils.<Boolean>createManagementFuture(
				CompletableFuture.<Boolean>completedFuture(true),
				CompletableFuture.<Collection<BasicMessageBean>>completedFuture(
						Arrays.asList(
						new BasicMessageBean(
								new Date(), // date
								true, // success
								analytic_bucket.display_name(),
								"resumeAnalyticJob",
								null, // message code
								"called resumeAnalyticJob",
								null // details
								))) );
	}	

}
