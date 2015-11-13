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
