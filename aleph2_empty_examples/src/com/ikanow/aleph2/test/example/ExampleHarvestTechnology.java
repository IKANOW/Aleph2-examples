package com.ikanow.aleph2.test.example;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;

public class ExampleHarvestTechnology implements IHarvestTechnologyModule {
	private static final Logger _logger = LogManager.getLogger();	

	@Override
	public void onInit(final IHarvestContext context) {
		_logger.info("onInit");		
	}
	
	
	public boolean canRunOnThisNode(DataBucketBean bucket, IHarvestContext context) {
		_logger.info("canRunOnThisNode");
		
		return true;
	}

	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {		
		_logger.info("onNewSource" + enabled);

		// Log some information about the request
		try {
			final String core_list = context.getHarvestContextLibraries(Optional.empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Harvest library paths: " + context.getHarvestLibraries(Optional.of(new_bucket)).get());
			_logger.info("Harvest signature: " + context.getHarvestContextSignature(Optional.of(new_bucket), Optional.empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onNewSource {0}", e));
		}
		
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
			DataBucketBean old_bucket, DataBucketBean new_bucket, boolean is_enabled,
			Optional<BucketDiffBean> diff,
			IHarvestContext context) {
		_logger.info("onUpdatedSource " + is_enabled);
		
		// Log some information about the request
		try {
			final String core_list = context.getHarvestContextLibraries(Optional.empty())
										.stream().collect(Collectors.joining(", "));
			_logger.info("Core library paths: " + core_list);
			_logger.info("Harvest library paths: " + context.getHarvestLibraries(Optional.of(new_bucket)).get());
			_logger.info("Harvest signature: " + context.getHarvestContextSignature(Optional.of(new_bucket), Optional.empty()));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onNewSource {0}", e));
		}
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						new_bucket.display_name(),
						"onUpdatedSource",
						null, // message code
						"called onUpdatedSource " + is_enabled,
						null // details
						));
	}

	public CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		_logger.info("onSuspend");
		
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
		_logger.info("onResume");
		
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
		_logger.info("onPurge");
		
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
		_logger.info("onDelete");
		
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
		_logger.debug("onPeriodicPoll");
		
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
		_logger.info("onHarvestComplete");
		
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
		_logger.info("onTestSource");
		
		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						test_bucket.display_name(),
						"onTestSource",
						null, // message code
						"called onTestSource: " + (test_spec == null ? "no test spec" : (test_spec.requested_num_objects() + " / " + test_spec.max_run_time_secs())),
						null // details
						));
	}


	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		_logger.info("onDecommission");

		return CompletableFuture.completedFuture(
				new BasicMessageBean(
						new Date(), // date
						true, // success
						to_decommission.display_name(),
						"onDecommission",
						null, // message code
						"called onDecommission",
						null // details
						));
	}
}
