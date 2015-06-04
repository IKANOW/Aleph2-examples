package com.ikanow.aleph2.storm.samples.topology;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;

import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.storm.samples.bolts.SampleWordParserBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleWebReaderSpout;
import com.ikanow.aleph2.storm.topology.IStormHarvestTopology;

/**
 * Example of a custom topology that does not follow the simple config
 * format.
 * 
 * @author Burch
 *
 */
public class SampleStormHarvestTopology1 implements IStormHarvestTopology {

	@Override
	public StormTopology getStormTopology(String harvest_context_signature, String job_name, DataBucketBean bucket_bean) {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("web_spout", new SampleWebReaderSpout("https://wordpress.org/plugins/about/readme.txt"));
		builder.setBolt("word_bolt", new SampleWordParserBolt()).shuffleGrouping("web_spout");
		return builder.createTopology();
	}

}
