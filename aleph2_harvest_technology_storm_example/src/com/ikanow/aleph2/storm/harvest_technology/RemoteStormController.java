package com.ikanow.aleph2.storm.harvest_technology;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift7.TException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.simple.JSONValue;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.Nimbus.Client;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.generated.TopologySummary;
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Utils;

public class RemoteStormController implements IStormController  {
	private static final Logger logger = LogManager.getLogger();
	private Map<String, Object> remote_config = null;
	private Client client;
	
	public RemoteStormController(@NonNull String nimbus_host, @NonNull int nimbus_thrift_port, @NonNull String storm_thrift_transport_plugin) {
		remote_config = new HashMap<String, Object>();
		remote_config.put(Config.NIMBUS_HOST, nimbus_host);
		remote_config.put(Config.NIMBUS_THRIFT_PORT, nimbus_thrift_port);
		remote_config.put(Config.STORM_THRIFT_TRANSPORT_PLUGIN, storm_thrift_transport_plugin);	
		remote_config.put(Config.STORM_META_SERIALIZATION_DELEGATE, "todo");
		logger.info("Connecting to remote storm: " + remote_config.toString() );
		client = NimbusClient.getConfiguredClient(remote_config).getClient();
	}

	@Override
	public void submitJob(String job_name, String input_jar_location,
			StormTopology topology) throws Exception {
		logger.info("Submitting job: " + job_name + " jar: " + input_jar_location);
		Map storm_conf = Utils.readStormConfig(); //TODO i don't think this config stuff is necessary
		storm_conf.putAll(remote_config);
		logger.info("submitting jar");
		String remote_jar_location = StormSubmitter.submitJar(storm_conf, input_jar_location);
		String json_conf = JSONValue.toJSONString(storm_conf);
		//Client client = NimbusClient.getConfiguredClient(remote_config).getClient();
		logger.info("submitting topology");
		client.submitTopology(job_name, remote_jar_location, json_conf, topology);
	}

	@Override
	public void stopJob(String job_name) {
		logger.info("Stopping job: " + job_name);
		try {
			client.killTopology(getJobTopologySummaryFromJobPrefix(job_name).get_name());
		} catch (Exception ex) {
			//let die for now, usually happens when top doesn't exist
			logger.info("Error stopping job: " + job_name, ex);
		}
	}

	@Override
	public TopologyInfo getJobStats(String job_name) throws Exception {
		logger.info("Looking for stats for job: " + job_name);		
		String job_id = getJobTopologySummaryFromJobPrefix(job_name).get_id();
		logger.info("Looking for stats with id: " + job_id);
		if ( job_id != null )
			return client.getTopologyInfo(job_id);		
		return null;
	}
	
	private TopologySummary getJobTopologySummaryFromJobPrefix(@NonNull String job_prefix) throws TException {
		ClusterSummary cluster_summary = client.getClusterInfo();
		Iterator<TopologySummary> iter = cluster_summary.get_topologies_iterator();
		 while ( iter.hasNext() ) {
			 TopologySummary summary = iter.next();
			 System.out.println(summary.get_name() + summary.get_id() + summary.get_status());				 
			 if ( summary.get_name().startsWith(job_prefix));
			 	return summary;
		 }	
		 return null;
	}
}
