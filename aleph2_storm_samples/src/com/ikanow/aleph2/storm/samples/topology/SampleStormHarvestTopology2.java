package com.ikanow.aleph2.storm.samples.topology;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;

import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.storm.samples.bolts.SampleOutputFileBolt;
import com.ikanow.aleph2.storm.samples.bolts.SampleProxyParserBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleFileLineReaderSpout;
import com.ikanow.aleph2.storm.topology.IStormHarvestTopology;

/**
 * An example of a topology that is exactly what you can create via the config methods.
 * e.g. you can instead send: 
 * 
 * @author Burch
 *
 */
public class SampleStormHarvestTopology2 implements IStormHarvestTopology {

	@Override
	public StormTopology getStormTopology(String harvest_context_signature, String job_name, DataBucketBean bucket_bean) {
		TopologyBuilder builder = new TopologyBuilder();		
		builder.setSpout("spout", new SampleFileLineReaderSpout("sample_log_files/proxy_small_sample.log"));
		builder.setBolt("parser", new SampleProxyParserBolt()).shuffleGrouping("spout");
		builder.setBolt("output", new SampleOutputFileBolt(harvest_context_signature, job_name)).shuffleGrouping("parser");
		return builder.createTopology();
	}

}
