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
package com.ikanow.aleph2.storm.harvest_technology;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
import com.ikanow.aleph2.storm.topology.IStormHarvestTopology;

/**
 * Returns back a topology based on the harvest_configs input parameters.
 * See the StormHarvestTechnologyConfig for examples of what we we expect.
 * 
 * @author Burch
 *
 */
public class StormHarvestTechnologyTopologyUtil {	
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Returns back a topology based on whats found in harvest_configs
	 * 
	 * @param harvest_configs
	 * @param job_name
	 * @param context
	 * @param bucket_bean
	 * @return
	 * @throws Exception
	 */
	public static StormTopology createTopology(List<HarvestControlMetadataBean> harvest_configs, String job_name, IHarvestContext context, DataBucketBean bucket_bean) throws Exception {
		HarvestControlMetadataBean harvest_config = harvest_configs.get(0); //dunno if we are suppose to expect more than 1
		StormHarvestTechnologyConfig storm_config = StormHarvestTechnologyConfigUtils.parseHarvestConfig(harvest_config);
		return createTopology(storm_config, job_name, context, bucket_bean);							
	}

	/**
	 * Splits the topology creation between the dev specified top class or the parsed version.
	 * 
	 * @param storm_config
	 * @param job_name
	 * @param context
	 * @param bucket_bean
	 * @return
	 * @throws Exception
	 */
	private static StormTopology createTopology(StormHarvestTechnologyConfig storm_config, String job_name, IHarvestContext context, DataBucketBean bucket_bean) throws Exception {
		if ( storm_config.getTopology_class() != null )
			return createTopology(storm_config.getTopology_class(), job_name, context, bucket_bean);
		else
			return createTopology(storm_config.getSource_type(), storm_config.getSource_url(), storm_config.getSource_parser(), storm_config.getSource_output(), job_name, context, bucket_bean);
	}

	/**
	 * Creates a topology based on the class given.  expects the class to be of IStormHarvestTopology type and calls the getStormTopology to retrieve the topology.
	 * 
	 * @param topology_class_name
	 * @param job_name
	 * @param context
	 * @param bucket_bean
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	private static StormTopology createTopology(String topology_class_name, String job_name, IHarvestContext context, DataBucketBean bucket_bean) throws ClassNotFoundException, InstantiationException, IllegalAccessException {		
		logger.info("Creating topology via class: " + topology_class_name);
		Class<IStormHarvestTopology> topology_clazz = (Class<IStormHarvestTopology>) Class.forName(topology_class_name);
		return topology_clazz.newInstance().getStormTopology(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()), job_name, bucket_bean);
	}

	/**
	 * Creates a topology based on the parsed config params, this version has
	 * a preset spout/parser/output logic it just loops over to return a standard
	 * set of items and you specify where the input is coming from.
	 * 
	 * NOTE: This is all commented out because I pulled the sample bolts/spouts out
	 * into a separate project.
	 * 
	 * @param source_type
	 * @param source_url
	 * @param source_parser
	 * @param source_output
	 * @param job_name
	 * @param context
	 * @param bucket_bean
	 * @return
	 * @throws Exception
	 */
	private static StormTopology createTopology(String source_type, String source_url, String source_parser, String source_output, String job_name, IHarvestContext context, DataBucketBean bucket_bean) throws Exception {
		logger.info(source_type + " " + source_url + " " + source_parser);
		
		StormTopology topology = null;
		TopologyBuilder builder = new TopologyBuilder();
		//add spout
//		if ( source_type.equals("File") )
//			builder.setSpout("spout", new SampleFileLineReaderSpout(source_url));
//		else
//			throw new Exception("Unknown source_type type: " + source_type);
//		
//		//set parser
//		if ( source_parser.equals("ProxyParser"))
//			builder.setBolt("parser", new SampleProxyParserBolt()).shuffleGrouping("spout");
//		else
//			throw new Exception("Unknown source_parser: " + source_parser);
//		
//		if ( source_output.equals("File") )
//			builder.setBolt("output", new SampleOutputFileBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()), job_name)).shuffleGrouping("parser");
//		else if ( source_output.equals("HDFS") )
//			builder.setBolt("output", new SampleOutputHDFSBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()))).shuffleGrouping("parser");
//		else
//			builder.setBolt("output", new SampleOutputBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()))).shuffleGrouping("parser");		
//		
		topology = builder.createTopology();	
		return topology;
	}
}
