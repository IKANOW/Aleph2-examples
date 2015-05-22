package com.ikanow.aleph2.test.example;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;

public class ExampleHarvestTechnology implements IHarvestTechnologyModule {

	public boolean canRunOnThisNode(DataBucketBean bucket) {
		return true;
	}

	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_bucket.display_name(),
						"onNewSource",
						null, // message code
						"called onNewSource: " + enabled,
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_bucket.display_name(),
						"onUpdatedSource",
						null, // message code
						"called onUpdatedSource",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_suspend.display_name(),
						"onSuspend",
						null, // message code
						"called onSuspend",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onResume(
			DataBucketBean to_resume, IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_resume.display_name(),
						"onResume",
						null, // message code
						"called onResume",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_purge.display_name(),
						"onPurge",
						null, // message code
						"called onPurge",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_delete.display_name(),
						"onDelete",
						null, // message code
						"called onDelete",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						polled_bucket.display_name(),
						"onPeriodicPoll",
						null, // message code
						"called onPeriodicPoll",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						completed_bucket.display_name(),
						"onHarvestComplete",
						null, // message code
						"called onHarvestComplete",
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						test_bucket.display_name(),
						"onTestSource",
						null, // message code
						"called onTestSource: " + test_spec == null ? "no test spec" : (test_spec.requested_num_objects() + " / " + test_spec.max_run_time_secs()),
						null // details
						));
	}
}
