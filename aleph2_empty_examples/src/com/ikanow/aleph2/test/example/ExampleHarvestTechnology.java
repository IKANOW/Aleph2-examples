package com.ikanow.aleph2.test.example;

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
			DataBucketBean new_bucket, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onResume(
			DataBucketBean to_resume, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		return null;
	}

	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		return null;
	}
}
