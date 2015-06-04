package com.ikanow.aleph2.storm.harvest_technology;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.UuidUtils;
import com.ikanow.aleph2.utils.JarBuilderUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * IHarvestTechnologyModule is just for going from raw data to json.
 * 
 * All the start/stop/pause/etc are related to controlling a source, not
 * controlling the technology(storm) e.g. I don't need to keep starting/stopping
 * storm
 * 
 * @author Burch
 *
 */
public class StormHarvestTechnologyModule implements IHarvestTechnologyModule {

	//https://storm.apache.org/documentation/Running-topologies-on-a-production-cluster.html
	private static final String JOB_NAME_PREFIX = "storm_harvest_technology_job_";	
	private static final Logger logger = LogManager.getLogger();
	private static IStormController storm_controller;
	protected GlobalPropertiesBean _globals;
	private final String NIMBUS_HOST = "nimbus_host";
	private final String NIMBUS_THRIFT_PORT = "nimbus_thrift_port";
	private final String THRIST_TRANSPORT_PLUGIN = "thrift_transport_plugin";

	@Override
	public void onInit(IHarvestContext context) {
		logger.info("initializing storm harvest technology");
		//TODO get config from context or YARN CONFIG
		
		Map<String, Object> config_map = new HashMap<String, Object>();
		config_map.put("StormHarvestTechnologyModule." + NIMBUS_HOST, "utility-dev-db-hadoop.rr.ikanow.com");
		//config_map.put("StormHarvestTechnologyModule." + NIMBUS_HOST, "localhost");
		config_map.put("StormHarvestTechnologyModule." + NIMBUS_THRIFT_PORT, 6627);
		config_map.put("StormHarvestTechnologyModule." + THRIST_TRANSPORT_PLUGIN, "backtype.storm.security.auth.SimpleTransportPlugin");
		Config config = ConfigFactory.parseMap(config_map);
		config = config.getConfig("StormHarvestTechnologyModule");		
		if ( config.hasPath(NIMBUS_HOST) ) {
			logger.info("starting in remote mode v5");
			logger.info(config.getString(NIMBUS_HOST));
			//run in distributed mode
			storm_controller = StormControllerUtil.getRemoteStormController(config.getString(NIMBUS_HOST), config.getInt(NIMBUS_THRIFT_PORT), config.getString(THRIST_TRANSPORT_PLUGIN));
		} else {
			logger.info("starting in local mode");
			//run in local mode
			storm_controller = StormControllerUtil.getLocalStormController(); //debug mode	
		}
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		//3A - this checks that the node you are running can control the external harvester ... 
		//less important for Storm since it's distributed anyway, but eg you could check that Globals.shared_yarn_config exists?
		return storm_controller != null;
	}
	
	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			boolean is_enabled, Optional<BucketDiffBean> diff,
			IHarvestContext context) {
		logger.info("received update source request");
		CompletableFuture<BasicMessageBean> stop_future = onDelete(old_bucket, context);
		try {
			logger.info("waiting for stop to complete");
			stop_future.get(10L, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			//set failure in completable future
			logger.info("stop failed, returning that ",e);
			stop_future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", e), null));
			return stop_future;
		}
		return onNewSource(new_bucket, context, is_enabled);
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onSuspend(
			@NonNull DataBucketBean to_suspend, @NonNull IHarvestContext context) {
		//I don't think storm has a pause, so you'll have to dump the
		//job to get it to stop the spout
		return onDelete(to_suspend, context);
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onResume(
			@NonNull DataBucketBean to_resume, @NonNull IHarvestContext context) {
		//if storm doesn't have a pause, then we will just have to resend
		return onNewSource(to_resume, context, true);		
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onPurge(
			@NonNull DataBucketBean to_purge, @NonNull IHarvestContext context) {		
		//purge means that someone has dumped all the data from this harvest, nothing to do on
		//our end, just let the source keep running (e.g. like delete docs in the old harvester)
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		future.complete(new BasicMessageBean(new Date(), true, null, null, null, "Nothing to do for purge", null));
		return future;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onDelete(
			@NonNull DataBucketBean to_delete, @NonNull IHarvestContext context) {
		//TODO not sure what delete is suppose to do, stop this topology? I assume no
		//data is being stored in the harvest tech so nothing to delete? (see purge)
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		try {
			storm_controller.stopJob(getJobName(to_delete));
		} catch (Exception e) {
			logger.info("Stop completing exceptionally", e);
			future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", e), null));
			return future;
		}		
		logger.info("returning completed stop");
		future.complete(new BasicMessageBean(new Date(), true, null, null, null, null, null));
		return future;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onPeriodicPoll(
			@NonNull DataBucketBean polled_bucket,
			@NonNull IHarvestContext context) {
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		TopologyInfo top_info;
		try {
			 top_info = storm_controller.getJobStats(getJobName(polled_bucket));
		} catch (Exception ex) {
			//set failure in completable future
			future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", ex), null));
			return future;
		}
		//TODO see if there is any info on this buckets harvest stats, can we
		//see how many documents have been sent via the spout or something?
		future.complete(new BasicMessageBean(new Date(), true, null, null, null, top_info.toString(), null));
		return future;
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onHarvestComplete(
			@NonNull DataBucketBean completed_bucket,
			@NonNull IHarvestContext context) {		
		//i guess this tell us when we are done, so kill off the topology
		return onDelete(completed_bucket, context);		
	}
	
	/**
	 * Returns back the "simple" version of a job name, this needs to be
	 * used as a prefix to find the real job name.
	 * 
	 * @param data_bucket
	 * @return
	 */
	private static String getJobName(@NonNull DataBucketBean data_bucket) {
		return (JOB_NAME_PREFIX + data_bucket._id() + "_").replaceAll("\\.", "_");
	}
	
	/**
	 * Returns back a unique version of a job name, this is used when submitting
	 * a new job.
	 * 
	 * @param data_bucket
	 * @return
	 */
	private static String createJobName(@NonNull DataBucketBean data_bucket) {
		return (JOB_NAME_PREFIX + data_bucket._id() + "_" + UuidUtils.get().getTimeBasedUuid()).replaceAll("\\.", "_");
	}		
	
	//private final String job_name = "STORM_TEST_JOB_";	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	
	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onTestSource(
			@NonNull DataBucketBean test_bucket,
			@NonNull ProcessingTestSpecBean test_spec,
			@NonNull IHarvestContext context) {		
		//submit a job just like a new source
		//TODO we should set the job up w/o the output to harvest context option, if 
		//we let users submit full topologies though we can't guarantee that I think
		//we need to use a derived id for the jobname because this job could be running
		//and we want to push test
		CompletableFuture<BasicMessageBean> future = onNewSource(test_bucket, context, true);
		//test_spec.requested_num_objects() //TODO I can't find a way to see how many objects have been harvested, so I don't have a good way to stop it
		//once X # of objects have been acked, maybe theres somewhere you can see total acks?
		
		//set up a timer to cancel job after max runtime
		executor.schedule(new RunnableCancelTestJob(createJobName(test_bucket), future), test_spec.max_run_time_secs(), TimeUnit.SECONDS); //schedules a job to cancel the top in 5s
		return future;
	}
	
	/**
	 * Runnable class that kills a test job when run is called, returns
	 * the result of the test job in the future.
	 * 
	 * @author Burch
	 *
	 */
	public class RunnableCancelTestJob implements Runnable {
		private final String job_name;
		private final CompletableFuture<BasicMessageBean> future;
		public RunnableCancelTestJob(String job_name, CompletableFuture<BasicMessageBean> future) {
			this.job_name = job_name;
			this.future = future;
		}
		
		@Override
		public void run() {
			logger.info("killing job: " + job_name);
			try {
				storm_controller.stopJob(job_name);
				//TODO fill in the complete response
				future.complete(new BasicMessageBean(new Date(), true, null, null, null, "yes it was killed", null));
			} catch (Exception e) {
				future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", e), null));				
			}
			
		}
	}

	@Override
	public @NonNull CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		logger.info("received new source request, enabled: " + enabled);
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		if ( enabled ) {
			
			//build out a topology for these config options
			String job_name = createJobName(new_bucket);
			StormTopology topology = null;
			try {
				topology = StormHarvestTechnologyTopologyUtil.createTopology(new_bucket.harvest_configs(), job_name, context, new_bucket);
			} catch (Exception e) {
				//set failure in completable future
				future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", e), null));
				return future;
			}
			
			try {				
				//step1 create a megajar from:
				//context.getHarvestLibraries(Optional.of(new_bucket));
				//and whatever jars i need to read raw data, parse that data, output to context.stream();					
				//step2 send this jar + topology to storm so it starts	
				logger.debug("creating jar to submit");
				final String input_jar_location = System.getProperty("java.io.tmpdir") + File.separator + job_name + ".jar";
				List<String> jars_to_merge = new ArrayList<String>();
				jars_to_merge.addAll( context.getHarvestContextLibraries(Optional.empty()) );
				//filter the harvester out of the harvest libraries
				Map<String, String> harvest_libraries = context.getHarvestLibraries(Optional.of(new_bucket)).get();
				//kick the harvest library out of our jar (it contains storm.jar which we can't send to storm)
				List<String> harvest_library_paths = harvest_libraries.keySet().stream().filter(name -> !name.contains(new_bucket.harvest_technology_name_or_id())).map(name -> harvest_libraries.get(name)).collect(Collectors.toList());
				jars_to_merge.addAll(harvest_library_paths);
				JarBuilderUtil.mergeJars(jars_to_merge, input_jar_location);
				storm_controller.submitJob(job_name, input_jar_location, topology);
			} catch (Exception e) {
				//set failure in completable future
				future.complete(new BasicMessageBean(new Date(), false, null, null, null, ErrorUtils.getLongForm("{0}", e), null));
				return future;
			}	
		}	
		
		//TODO return something useful
		future.complete(new BasicMessageBean(new Date(), true, null, null, null, null, null));
		return future;
	}	
	
	public static void main(String[] args) {
		//fake main for eclipse build
		StormHarvestTechnologyModule mod = new StormHarvestTechnologyModule();
		mod.onInit(null);
	}
}
