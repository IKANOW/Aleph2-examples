package com.ikanow.aleph2.storm.harvest_technology;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
import com.ikanow.aleph2.storm.samples.bolts.SampleOutputBolt;
import com.ikanow.aleph2.storm.samples.bolts.SampleOutputFileBolt;
import com.ikanow.aleph2.storm.samples.bolts.SampleOutputHDFSBolt;
import com.ikanow.aleph2.storm.samples.bolts.SampleProxyParserBolt;
import com.ikanow.aleph2.storm.samples.spouts.SampleFileLineReaderSpout;
import com.ikanow.aleph2.storm.topology.IStormHarvestTopology;

public class StormHarvestTechnologyTopologyUtil {	
	private static final Logger logger = LogManager.getLogger();
	
	public static StormTopology createTopology(@NonNull List<HarvestControlMetadataBean> harvest_configs, @NonNull String job_name, @NonNull IHarvestContext context, @NonNull DataBucketBean bucket_bean) throws Exception {
		HarvestControlMetadataBean harvest_config = harvest_configs.get(0); //dunno if we are suppose to expect more than 1
		StormHarvestTechnologyConfig storm_config = StormHarvestTechnologyConfigUtils.parseHarvestConfig(harvest_config);
		return createTopology(storm_config, job_name, context, bucket_bean);							
	}

	private static StormTopology createTopology(@NonNull StormHarvestTechnologyConfig storm_config, @NonNull String job_name, @NonNull IHarvestContext context, @NonNull DataBucketBean bucket_bean) throws Exception {
		if ( storm_config.getTopology_class() != null )
			return createTopology(storm_config.getTopology_class(), job_name, context, bucket_bean);
		else
			return createTopology(storm_config.getSource_type(), storm_config.getSource_url(), storm_config.getSource_parser(), storm_config.getSource_output(), job_name, context, bucket_bean);
	}

	@SuppressWarnings("unchecked")
	private static StormTopology createTopology(String topology_class_name, @NonNull String job_name, @NonNull IHarvestContext context, @NonNull DataBucketBean bucket_bean) throws ClassNotFoundException, InstantiationException, IllegalAccessException {		
		logger.info("Creating topology via class: " + topology_class_name);
		Class<IStormHarvestTopology> topology_clazz = (Class<IStormHarvestTopology>) Class.forName(topology_class_name);
		return topology_clazz.newInstance().getStormTopology(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()), job_name, bucket_bean);
	}

	private static StormTopology createTopology(@NonNull String source_type, @NonNull String source_url, @NonNull String source_parser, @NonNull String source_output, @NonNull String job_name, @NonNull IHarvestContext context, @NonNull DataBucketBean bucket_bean) throws Exception {
		logger.info(source_type + " " + source_url + " " + source_parser);
		
		StormTopology topology = null;
		TopologyBuilder builder = new TopologyBuilder();
		//add spout
		if ( source_type.equals("File") )
			builder.setSpout("spout", new SampleFileLineReaderSpout(source_url));
		else
			throw new Exception("Unknown source_type type: " + source_type);
		
		//set parser
		if ( source_parser.equals("ProxyParser"))
			builder.setBolt("parser", new SampleProxyParserBolt()).shuffleGrouping("spout");
		else
			throw new Exception("Unknown source_parser: " + source_parser);
		
		if ( source_output.equals("File") )
			builder.setBolt("output", new SampleOutputFileBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()), job_name)).shuffleGrouping("parser");
		else if ( source_output.equals("HDFS") )
			builder.setBolt("output", new SampleOutputHDFSBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()))).shuffleGrouping("parser");
		else
			builder.setBolt("output", new SampleOutputBolt(context.getHarvestContextSignature(Optional.of(bucket_bean), Optional.empty()))).shuffleGrouping("parser");		
		
		topology = builder.createTopology();	
		return topology;
	}
}
