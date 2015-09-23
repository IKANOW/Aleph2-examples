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
******************************************************************************/
package com.ikanow.aleph2.storm.harvest_technology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.yaml.snakeyaml.Yaml;

import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;

import com.google.common.collect.Sets;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.data_model.utils.PropertiesUtils;
import com.ikanow.aleph2.utils.JarBuilderUtil;
import com.ikanow.aleph2.utils.StormControllerUtil;

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
	private final static Set<String> dirs_to_ignore = Sets.newHashSet("org/slf4j", "org/apache/log4j");

	@SuppressWarnings("unchecked")
	@Override
	public void onInit(IHarvestContext context) {
		logger.info("initializing storm harvest technology");
		try {
			_globals = BeanTemplateUtils.from(PropertiesUtils.getSubConfig(ModuleUtils.getStaticConfig(), GlobalPropertiesBean.PROPERTIES_ROOT).orElse(null), GlobalPropertiesBean.class);
		} catch (IOException e) {
			logger.error(ErrorUtils.getLongForm("Couldn't set globals property bean in storm harvest tech onInit: {0}", e));			
		}
		logger.info("Loading storm config from: " + _globals.local_yarn_config_dir() + File.separator + "storm.yaml");
		Yaml yaml = new Yaml();
		InputStream input;
		Map<String, Object> object;
		try {
			input = new FileInputStream(new File(_globals.local_yarn_config_dir() + File.separator + "storm.yaml"));
			object = (Map<String, Object>) yaml.load(input);
		} catch (FileNotFoundException e) {
			logger.error(ErrorUtils.getLongForm("Error reading storm.yaml in storm harvest tech onInit: {0}", e));
			object = new HashMap<String, Object>();
		}
		
		if ( null == storm_controller ) {
			if ( object.containsKey(backtype.storm.Config.NIMBUS_HOST) ) {
				logger.info("starting in remote mode v5");
				logger.info(object.get(backtype.storm.Config.NIMBUS_HOST));
				//run in distributed mode
				storm_controller = StormControllerUtil.getRemoteStormController(
						(String)object.get(backtype.storm.Config.NIMBUS_HOST), 
						(int)object.get(backtype.storm.Config.NIMBUS_THRIFT_PORT), 
						(String)object.get(backtype.storm.Config.STORM_THRIFT_TRANSPORT_PLUGIN));
			} else {
				logger.info("starting in local mode");
				//run in local mode
				storm_controller = StormControllerUtil.getLocalStormController(); //debug mode	
			}
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
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
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
			stop_future.complete(new BasicMessageBean(new Date(), false, null, "updateSource", null, ErrorUtils.getLongForm("{0}", e), null));
			return stop_future;
		}
		return onNewSource(new_bucket, context, is_enabled);
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPurge(
			DataBucketBean to_purge, IHarvestContext context) {		
		//purge means that someone has dumped all the data from this harvest, nothing to do on
		//our end, just let the source keep running (e.g. like delete docs in the old harvester)
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		future.complete(new BasicMessageBean(new Date(), true, null, "onPurge", null, "Nothing to do for purge", null));
		return future;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		//TODO not sure what delete is suppose to do, stop this topology? I assume no
		//data is being stored in the harvest tech so nothing to delete? (see purge)
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		try {
			StormControllerUtil.stopJob(storm_controller, getJobName(to_delete));
		} catch (Exception e) {
			logger.info("Stop completing exceptionally", e);
			future.complete(new BasicMessageBean(new Date(), false, null, "onDelete", null, ErrorUtils.getLongForm("{0}", e), null));
			return future;
		}		
		logger.info("returning completed stop");
		future.complete(new BasicMessageBean(new Date(), true, null, "onDelete", null, null, null));
		return future;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket,
			IHarvestContext context) {
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		TopologyInfo top_info;
		try {
			 top_info = StormControllerUtil.getJobStats(storm_controller, getJobName(polled_bucket));
		} catch (Exception ex) {
			//set failure in completable future
			future.complete(new BasicMessageBean(new Date(), false, null, "onPeriodicPoll", null, ErrorUtils.getLongForm("{0}", ex), null));
			return future;
		}
		//TODO see if there is any info on this buckets harvest stats, can we
		//see how many documents have been sent via the spout or something?
		future.complete(new BasicMessageBean(new Date(), true, null, "onPeriodicPoll", null, top_info.toString(), null));
		return future;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket,
			IHarvestContext context) {		
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
	private static String getJobName(DataBucketBean data_bucket) {
		return replaceJobCharacters(JOB_NAME_PREFIX + data_bucket._id() + "_");
	}		
	
	private static String replaceJobCharacters(String job_name) {
		return job_name.replaceAll("\\.", "_").replaceAll("__+", "_").replace(";", "__");
	}
	
	//private final String job_name = "STORM_TEST_JOB_";	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	
	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket,
			ProcessingTestSpecBean test_spec,
			IHarvestContext context) {		
		//submit a job just like a new source		
		CompletableFuture<BasicMessageBean> future = onNewSource(test_bucket, context, true);
		
		//test_spec.requested_num_objects() //TODO I can't find a way to see how many objects have been harvested, so I don't have a good way to stop it
		//once X # of objects have been acked, maybe there is somewhere you can see total acks?
		//TODO find if there is a way to cancel after requested num objects harvested
		
		//set up a timer to cancel job after max runtime
		executor.schedule(new RunnableCancelTestJob(getJobName(test_bucket), future), test_spec.max_run_time_secs(), TimeUnit.SECONDS); //schedules a job to cancel the top in 5s
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
				StormControllerUtil.stopJob(storm_controller, job_name);
				//TODO fill in the complete response
				future.complete(new BasicMessageBean(new Date(), true, null, "onTest", null, "yes it was killed", null));
			} catch (Exception e) {
				future.complete(new BasicMessageBean(new Date(), false, null, "onTest", null, ErrorUtils.getLongForm("{0}", e), null));				
			}
			
		}
	}

	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		logger.info("received new source request, enabled: " + enabled);
		CompletableFuture<BasicMessageBean> future = new CompletableFuture<BasicMessageBean>();
		if ( enabled ) {
			
			//build out a topology for these config options
			String job_name = getJobName(new_bucket);
			StormTopology topology = null;
			try {
				topology = StormHarvestTechnologyTopologyUtil.createTopology(new_bucket.harvest_configs(), job_name, context, new_bucket);
			} catch (Exception e) {
				//set failure in completable future
				future.complete(new BasicMessageBean(new Date(), false, null, "onNewSource", null, ErrorUtils.getLongForm("{0}", e), null));
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
				JarBuilderUtil.mergeJars(jars_to_merge, input_jar_location, dirs_to_ignore);
				StormControllerUtil.startJob(storm_controller, job_name, input_jar_location, topology);
				
				//verify job was assigned some executors
				TopologyInfo info = StormControllerUtil.getJobStats(storm_controller, job_name);
				if ( info.get_executors_size() == 0 ) {
					//no executors were available for this job, stop the job, throw an error
					StormControllerUtil.stopJob(storm_controller, job_name);
					future.complete(new BasicMessageBean(new Date(), false, null, "onNewSource", null, "No executors were assigned to this job, typically this is because too many jobs are currently running, kill some other jobs and resubmit.", null));
					return future;					
				}
			} catch (Exception e) {
				//set failure in completable future
				future.complete(new BasicMessageBean(new Date(), false, null, "onNewSource", null, ErrorUtils.getLongForm("{0}", e), null));
				return future;
			}	
		}	
		
		//TODO return something useful
		future.complete(new BasicMessageBean(new Date(), true, null, "onNewSource", null, null, null));
		return future;
	}	
	
	public static void main(String[] args) {
		//fake main for eclipse build
		StormHarvestTechnologyModule mod = new StormHarvestTechnologyModule();
		mod.onInit(null);
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}
}
