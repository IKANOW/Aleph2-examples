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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import scala.Tuple2;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.base.BaseRichSpout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentStreamingTopology;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.JsonUtils;
import com.ikanow.aleph2.storm.samples.bolts.ReducerCounterBolt;
import com.ikanow.aleph2.storm.samples.bolts.JavaScriptBolt;
/**
 * An example of a topology that is using a javascript Bolt for enrichment.
 * 
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public class JavaScriptTopology implements IEnrichmentStreamingTopology {
	
	protected static ObjectMapper object_mapper = BeanTemplateUtils.configureMapper(Optional.empty());

	@Override
	public Tuple2<Object, Map<String, String>> getTopologyAndConfiguration(DataBucketBean bucket, IEnrichmentModuleContext context) {
		TopologyBuilder builder = new TopologyBuilder();		
		String contextSignature = context.getEnrichmentContextSignature(Optional.of(bucket), Optional.empty());

		final Collection<Tuple2<BaseRichSpout, String>>  entry_points = context.getTopologyEntryPoints(BaseRichSpout.class, Optional.of(bucket));				
		entry_points.forEach(spout_name -> builder.setSpout(spout_name._2(), spout_name._1()));
		entry_points.stream().reduce(
				builder.setBolt("scriptBolt", new JavaScriptBolt(contextSignature,"/com/ikanow/aleph2/storm/samples/script/js/scripts.properties")),
				(acc, v) -> acc.shuffleGrouping(v._2()),
				(acc1, acc2) -> acc1 // (not possible in practice)
				) ;		
		builder.setBolt("reducerCounter", new ReducerCounterBolt()).shuffleGrouping("scriptBolt");
		builder.setBolt("out", context.getTopologyStorageEndpoint(BaseRichBolt.class, Optional.of(bucket))).localOrShuffleGrouping("reducerCounter");
		return new Tuple2<Object, Map<String, String>>(builder.createTopology(), new HashMap<String, String>());
	}

	@Override
	public <O> JsonNode rebuildObject(O raw_outgoing_object, Function<O, LinkedHashMap<String, Object>> generic_outgoing_object_builder) {
		return JsonUtils.foldTuple(generic_outgoing_object_builder.apply(raw_outgoing_object), object_mapper, Optional.empty()); 
	}
	
	

}
