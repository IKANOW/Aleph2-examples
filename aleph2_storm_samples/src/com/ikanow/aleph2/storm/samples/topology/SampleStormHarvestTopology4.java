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
package com.ikanow.aleph2.storm.samples.topology;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;

import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.storm.samples.bolts.SampleLineParserBolt;
import com.ikanow.aleph2.storm.samples.bolts.SampleOutputBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleWebReaderSpout;
import com.ikanow.aleph2.storm.topology.IStormHarvestTopology;

/**
 * Example of a custom topology that does not follow the simple config
 * format.
 * 
 * @author Burch
 *
 */
public class SampleStormHarvestTopology4 implements IStormHarvestTopology {

	@Override
	public StormTopology getStormTopology(String harvest_context_signature, String job_name, DataBucketBean bucket_bean) {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("web_spout", new SampleWebReaderSpout("https://wordpress.org/plugins/about/readme.txt"));
		builder.setBolt("line_bolt", new SampleLineParserBolt()).shuffleGrouping("web_spout");
		builder.setBolt("output_bolt", new SampleOutputBolt(harvest_context_signature)).shuffleGrouping("line_bolt");
		return builder.createTopology();
	}

}
