package com.ikanow.aleph2.storm.samples.bolts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IUuidService;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.data_model.utils.UuidUtils;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SampleOutputFileBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;
	private String harvest_signature;
	private String output_file_location = "test_output_folder/";
	private String job_name = "";
	
	public SampleOutputFileBolt(String harvest_signature, String job_name) {
		this.harvest_signature = harvest_signature;
		File file = new File(output_file_location);
		if ( !file.exists() )		
			file.mkdir();		
		this.job_name = job_name;
		file = new File(output_file_location + job_name);
		if ( !file.exists() )		
			file.mkdir();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(Tuple tuple) {
		Map<String, Object> parsed_entry = (Map<String, Object>) tuple.getValue(0);
		if ( parsed_entry != null) {
			//instead of emiting the tuple, we save it to HDFS
			//haven't wrote how to connect to HDFS yet so outputting to local fs?
			try {
				File file = new File(output_file_location + job_name + "/" + UuidUtils.get().getTimeBasedUuid() + ".txt");
				if ( !file.exists() )
					file.createNewFile();
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter writer = new BufferedWriter(fw);
				writer.write(parsed_entry + "\n");
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
