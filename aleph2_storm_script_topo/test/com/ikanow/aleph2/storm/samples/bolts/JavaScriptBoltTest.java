package com.ikanow.aleph2.storm.samples.bolts;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ikanow.aleph2.storm.samples.script.CompiledScriptFactory;

import backtype.storm.generated.GlobalStreamId;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.MessageId;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.TupleImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JavaScriptBoltTest {
	
	private static final Logger logger = LogManager.getLogger(CompiledScriptFactory.class);

	JavaScriptBolt javaScriptBolt = null;
	
	@Before
	public void setup(){
		javaScriptBolt = new JavaScriptBolt("com/ikanow/aleph2/storm/samples/js/script.properties");
	}

	@Test
	public void testBolt(){
		javaScriptBolt.execute(null);
	}
	
/*	public static LinkedHashMap<String, Object> tupleToLinkedHashMap(final Tuple t) {
		return StreamSupport.stream(t.getFields().spliterator(), false)
							.collect(Collectors.toMap(f -> f, f -> t.getValueByField(f), (m1, m2) -> m1, LinkedHashMap::new));
	}
	*/
}
