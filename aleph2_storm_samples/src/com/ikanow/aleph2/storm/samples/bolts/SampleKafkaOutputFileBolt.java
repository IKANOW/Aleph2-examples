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
package com.ikanow.aleph2.storm.samples.bolts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ikanow.aleph2.data_model.utils.UuidUtils;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SampleKafkaOutputFileBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;
	private String output_file_location = null; //"/tmp/kafka_storm_test_output"; 
	public static final Logger logger = LoggerFactory.getLogger(SampleKafkaOutputFileBolt.class);
	
	public SampleKafkaOutputFileBolt() {	
	}
	
	private void createTmpDir() {
		output_file_location = System.getProperty("java.io.tmpdir") + File.separator + "kafka_storm_test_output";
		File file = new File(output_file_location);
		if ( !file.exists() )		
			file.mkdir();			
	}
	
	private void createDir(String path) {
		File file = new File(path);
		if ( !file.exists() )		
			file.mkdir();
	}
	
	@Override
	public void execute(Tuple tuple) {
		logger.error("TUPLE ("+tuple.size()+"): " + tuple.getString(0));
		if ( output_file_location == null )
			createTmpDir();
		if ( tuple.size() == 3 ) {
			String keyA = tuple.getString(0);
			String keyB = tuple.getString(1);
			String message = tuple.getString(2);
			
			try {
				createDir(output_file_location);
				logger.error("WRITE to output: " + output_file_location + File.separator + UuidUtils.get().getTimeBasedUuid() + ".txt");
				File file = new File(output_file_location + File.separator + UuidUtils.get().getTimeBasedUuid() + ".txt");
				if ( !file.exists() )
					file.createNewFile();
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter writer = new BufferedWriter(fw);
				writer.write("keyA: " + keyA + " keyB: " + keyB + " message: " + message + "\n");
				writer.close();
			} catch (IOException e) {
				logger.error("error writing to file", e);
			}	
		} else {
			logger.error("tuple size was not 3");
		}
		
		//always ack the tuple to acknowledge we've processed it, otherwise a fail message will be reported back
		//to the spout
		_collector.ack(tuple);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("keyA","keyB","message"));
	}

}
