package com.ikanow.aleph2.storm.samples.bolts;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SampleWordParserBolt extends BaseRichBolt {
	
	private static final long serialVersionUID = -754177901046983751L;
	private OutputCollector _collector;	
	private Pattern pattern = Pattern.compile("\\w+\\s*", Pattern.CASE_INSENSITIVE); //basic regex, doesn't consider non letter characters
	
	@Override
	public void execute(Tuple tuple) {
		String line = tuple.getString(0);
		Matcher matcher = pattern.matcher(line);
		while ( matcher.find() ) {
			_collector.emit(tuple, new Values( matcher.group(0).trim()));
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
		declarer.declare(new Fields("word"));
	}
}
