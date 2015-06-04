package com.ikanow.aleph2.storm_harvest_technology.jar_builder_dir;

import com.ikanow.aleph2.data_model.interfaces.shared_services.IUuidService;
import com.ikanow.aleph2.data_model.utils.UuidUtils;
import com.ikanow.aleph2.storm.harvest_technology.IStormController;
import com.ikanow.aleph2.storm.harvest_technology.StormControllerUtil;
import com.ikanow.aleph2.storm.samples.bolts.SampleWordParserBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleWebReaderSpout;

import backtype.storm.topology.TopologyBuilder;

public class RemoteTopologyJarSubmissionExample {

	public static void main(String[] args) throws Exception {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("web_spout", new SampleWebReaderSpout("https://wordpress.org/plugins/about/readme.txt"));
		builder.setBolt("word_bolt", new SampleWordParserBolt()).shuffleGrouping("web_spout");
		
		//builder.setSpout("proxy_spout", new SampleFileLineReaderSpout("sample_log_files/proxy_small_sample.log"));
		//builder.setBolt("proxy_parser", new SampleProxyParserBolt()).shuffleGrouping("proxy_spout");
		//builder.setBolt("proxy_output", new SampleOutputBolt()).shuffleGrouping("proxy_parser");		
		
		IUuidService uuid_service = UuidUtils.get();
		String inputJar = "C:/Users/Burch/Desktop/aleph2_harvest_technology_storm_example-0.0.1-SNAPSHOT-jar-with-dependencies.jar";
		IStormController storm_controller = StormControllerUtil.getRemoteStormController("localhost", 6627, "backtype.storm.security.auth.SimpleTransportPlugin");
		storm_controller.submitJob(uuid_service.getTimeBasedUuid(), inputJar, builder.createTopology());				
	}

}
