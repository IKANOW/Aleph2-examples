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
package com.ikanow.aleph2.enrichment.utils.services;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import scala.Tuple2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule.ProcessingStage;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.Tuples;

import fj.data.Either;

public class TestJsScriptEngineService {

	@Test
	public void test_end2end() throws IOException {
		final ObjectMapper mapper = BeanTemplateUtils.configureMapper(Optional.empty());
		
		final String user_script = Resources.toString(Resources.getResource("js_engine_test.js"), Charsets.UTF_8);
		
		final JsScriptEngineService service_under_test = new JsScriptEngineService();
		
		final DataBucketBean bucket = Mockito.mock(DataBucketBean.class);
		//final IEnrichmentModuleContext context = Mockito.mock(IEnrichmentModuleContext.class);
		
		final LinkedList<ObjectNode> emitted = new LinkedList<>();
		final LinkedList<JsonNode> grouped = new LinkedList<>();
		final LinkedList<JsonNode> externally_emitted = new LinkedList<>();
		
		final IEnrichmentModuleContext context = Mockito.mock(IEnrichmentModuleContext.class, new Answer<Void>() {
		      @SuppressWarnings("unchecked")
			public Void answer(InvocationOnMock invocation) {
		    	  try {
			    	  Object[] args  = invocation.getArguments();
			    	  assertTrue("Unexpected call to context object during test: " + invocation.getMethod().getName(), invocation.getMethod().getName().equals("emitMutableObject") ||
			    			  invocation.getMethod().getName().equals("externalEmit")
			    			  );
			    	  if (invocation.getMethod().getName().equals("emitMutableObject")) {
			    		  final Optional<JsonNode> grouping = (Optional<JsonNode>) args[3];
			    		  if (grouping.isPresent()) {
			    			  grouped.add(grouping.get());
			    		  }
				    	  emitted.add((ObjectNode) args[1]);
			    	  }
			    	  else if (invocation.getMethod().getName().equals("externalEmit")) {
			    		  final DataBucketBean to = (DataBucketBean) args[0];
			    		  final Either<JsonNode, Map<String, Object>> out = (Either<JsonNode, Map<String, Object>>)args[1];
			    		  externally_emitted.add(((ObjectNode)out.left().value()).put("bucket", to.full_name()));			    		  
			    	  }
		    	  }
		    	  catch (Exception e) {
		    		  fail(e.getMessage());
		    	  }
		          return null;
		      }});
		
		final EnrichmentControlMetadataBean control =
				BeanTemplateUtils.build(EnrichmentControlMetadataBean.class)
					.with(EnrichmentControlMetadataBean::config,
							new LinkedHashMap<String, Object>(ImmutableMap.<String, Object>builder()
								.put("script", user_script)
								.put("config", ImmutableMap.<String, Object>builder().put("test", "config").build())
								.put("imports", Arrays.asList("underscore-min.js"))
							.build())
							)
				.done().get();
		
		service_under_test.onStageInitialize(context, bucket, control, Tuples._2T(ProcessingStage.batch, ProcessingStage.grouping), Optional.of(Arrays.asList("test1", "test2")));
		
		final List<Tuple2<Long, IBatchRecord>> batch = 
				Arrays.asList(
				new BatchRecord(mapper.readTree("{\"test\":\"1\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"2\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"3\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"4\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"5\"}"))
				)
				.stream()
				.<Tuple2<Long, IBatchRecord>>map(br -> Tuples._2T(0L, br))
				.collect(Collectors.toList())
				;
		
		service_under_test.onObjectBatch(batch.stream(), Optional.of(5), Optional.of(mapper.readTree("{\"key\":\"static\"}")));
		assertEquals(20, emitted.size());
		emitted.stream().forEach(on -> {
			if (on.has("len")) assertEquals(5, on.get("len").asInt());
			else if (on.has("grouping_key")) assertEquals("{\"key\":\"static\"}", on.get("grouping_key").toString());
			else if (on.has("prev")) {
				assertEquals("batch", on.get("prev").asText());
				assertEquals("grouping", on.get("next").asText());
				assertEquals("{\"test\":\"config\"}", on.get("config").toString());
				assertEquals(2, on.get("groups").size());
				//DEBUG
				//System.out.println(on.toString());
			}
			else {
				fail("missing field" + on.toString());
			}
		});
		
		assertEquals(5, grouped.size());
		assertTrue(grouped.stream().map(j -> j.toString()).allMatch(s -> s.equals("{\"key\":\"static\"}")));
		assertEquals(5, externally_emitted.size());
		
		// Finally, check cloning
		
		final IEnrichmentBatchModule service_under_test_2 = service_under_test.cloneForNewGrouping();
		
		final List<Tuple2<Long, IBatchRecord>> batch2 = 
				Arrays.asList(
				new BatchRecord(mapper.readTree("{\"test\":\"1\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"2\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"3\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"4\"}")),
				new BatchRecord(mapper.readTree("{\"test\":\"5\"}"))
				)
				.stream()
				.<Tuple2<Long, IBatchRecord>>map(br -> Tuples._2T(0L, br))
				.collect(Collectors.toList())
				;
		
		emitted.clear();
		assertEquals(0, emitted.size());
		service_under_test_2.onObjectBatch(batch2.stream(), Optional.empty(), Optional.empty());
		assertEquals(20, emitted.size());
		emitted.stream().forEach(on -> {
			//DEBUG
			//System.out.println(on.toString());
			
			assertFalse("Wrong format: " + on.toString(), on.has("len"));
			assertFalse("Wrong format: " + on.toString(), on.has("grouping_key"));			
			if (on.has("prev")) {
				assertEquals("batch", on.get("prev").asText());
				assertEquals("grouping", on.get("next").asText());
				assertEquals("{\"test\":\"config\"}", on.get("config").toString());
				assertEquals(2, on.get("groups").size());
			}
		});
		
	}

	/** IBatchRecord implementation
	 * @author Alex
	 */
	public static class BatchRecord implements IBatchRecord {
		final protected JsonNode json;
		public BatchRecord(JsonNode json) {
			this.json = json;
		}
		@Override
		public JsonNode getJson() {
			return json;
		}
		@Override
		public Optional<ByteArrayOutputStream> getContent() {
			return Optional.empty();
		}		
	}
	
}
