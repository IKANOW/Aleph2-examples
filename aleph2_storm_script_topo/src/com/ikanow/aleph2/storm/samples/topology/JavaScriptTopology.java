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
package com.ikanow.aleph2.storm.samples.topology;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import scala.Tuple2;
import backtype.storm.topology.TopologyBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentStreamingTopology;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.JsonUtils;
import com.ikanow.aleph2.storm.samples.bolts.IndexerBolt;
import com.ikanow.aleph2.storm.samples.bolts.JavaScriptBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleFileLineReaderSpout;
/**
 * An example of a topology that is using a javascript Bolt for enrichment.
 * 
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public class JavaScriptTopology implements IEnrichmentStreamingTopology {
	
	protected static ObjectMapper object_mapper = BeanTemplateUtils.configureMapper(Optional.empty());

	@Override
	public Tuple2<Object, Map<String, String>> getTopologyAndConfiguration(DataBucketBean bean, IEnrichmentModuleContext context) {
		TopologyBuilder builder = new TopologyBuilder();		
		builder.setSpout("spout", new SampleFileLineReaderSpout("sample_log_files/proxy_small_sample.log"));
		builder.setBolt("scriptBolt", new JavaScriptBolt("/com/ikanow/aleph2/storm/samples/script/js/scripts.properties")).shuffleGrouping("spout");
		builder.setBolt("indexer", new IndexerBolt()).shuffleGrouping("scriptBolt");
		return new Tuple2<Object, Map<String, String>>(builder.createTopology(), new HashMap<String, String>());
	}

	@Override
	public <O> JsonNode rebuildObject(O raw_outgoing_object, Function<O, LinkedHashMap<String, Object>> generic_outgoing_object_builder) {
		return JsonUtils.foldTuple(generic_outgoing_object_builder.apply(raw_outgoing_object), object_mapper, Optional.empty()); 
	}
	
	

}
