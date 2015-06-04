package com.ikanow.aleph2.storm.samples.bolts;

import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SampleProxyAlertBolt extends BaseRichBolt {

	private static final long serialVersionUID = 2023732900302096606L;
	private OutputCollector _collector;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Tuple tuple) {
		Map<String, Object> parsed_entry = (Map<String, Object>) tuple.getValue(0);
		if ( parsed_entry != null) {
			if ( parsed_entry.get(SampleProxyLogEntry.CS_CATEGORIES).equals("News/Media"))
				_collector.emit(tuple, new Values(parsed_entry));			
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
