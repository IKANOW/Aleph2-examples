package com.ikanow.aleph2.storm.samples.spouts;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import jline.internal.InputStreamReader;

import org.checkerframework.checker.nullness.qual.NonNull;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * Tries to open a local file and emits tuples per line
 * of the file.
 * 
 * @author Burch
 *
 */
public class SampleWebReaderSpout extends BaseRichSpout {

	private static final long serialVersionUID = 2777840612433168585L;
	private SpoutOutputCollector _collector;
	private BufferedReader reader;
	private String file_location;

	public SampleWebReaderSpout(@NonNull String source_url) {
		file_location = source_url;
	}

	@Override
	public void nextTuple() {
		//send lines in the log file until we run out
		if ( reader != null ) {
			try {
				String line = reader.readLine();
				if ( line == null )
					reader = null;
				else
					_collector.emit(new Values(line));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		_collector = collector;
		
		//open web file sample
		try {
			InputStream is = new URL(file_location).openStream();
			reader = new BufferedReader(new InputStreamReader(is));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("log_entry"));
	}

}
