package com.ikanow.aleph2.storm.samples.bolts;

import java.util.Map;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.utils.ContextUtils;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SampleOutputHDFSBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;
	private String harvest_signature;
	
	public SampleOutputHDFSBolt(String harvest_signature) {
		this.harvest_signature = harvest_signature;
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(Tuple tuple) {
		Map<String, Object> parsed_entry = (Map<String, Object>) tuple.getValue(0);
		if ( parsed_entry != null) {
			//instead of emiting the tuple, we save it to HDFS
			//haven't wrote how to connect to HDFS yet
			//TODO output to HDFS
			//https://github.com/IKANOW/Aleph2-contrib/blob/master/aleph2_management_db_service_mongodb/src/com/ikanow/aleph2/management_db/mongodb/services/IkanowV1SyncService_LibraryJars.java#L418
		}		
		
		//always ack the tuple to acknowledge we've processed it, otherwise a fail message will be reported back
		//to the spout
		_collector.ack(tuple);
	}

	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("proxy_parsed"));
	}

}
