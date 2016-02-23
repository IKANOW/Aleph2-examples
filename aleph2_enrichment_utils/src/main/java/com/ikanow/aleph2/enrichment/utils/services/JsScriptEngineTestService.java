/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.Charsets;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import scala.Tuple2;

import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule.ProcessingStage;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Tuples;

import fj.data.Either;

/** Very basic test harness for scripts
 *  TODO: allow specification of a bucket/job/enrichment engine
 *  TODO: allow other jobs to be used instead/as well, assuming they're on the classpath
 * @author Alex
 *
 */
public class JsScriptEngineTestService {
	private static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	/** Entry point
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("ARGS: <script-file> <input-file> <output-prefix> [{[len: <LEN>], [group: <GROUP>]}]");
		}
		
		// STEP 1: load script file
		
		final String user_script = Files.toString(new File(args[0]), Charsets.UTF_8);
		
		// STEP 2: get a stream for the JSON file
		
		final InputStream io_stream = new FileInputStream(new File(args[1]));
		
		// STEP 3: set up control if applicable
		
		Optional<JsonNode> json = Optional.of("").filter(__ -> args.length > 3).map(__ -> args[3]).map(Lambdas.wrap_u(j -> _mapper.readTree(j)));
		
		// STEP 4: set up the various objects
		
		final DataBucketBean bucket = Mockito.mock(DataBucketBean.class);
		
		final JsScriptEngineService service_under_test = new JsScriptEngineService();

		final LinkedList<ObjectNode> emitted = new LinkedList<>();
		final LinkedList<JsonNode> grouped = new LinkedList<>();
		final LinkedList<JsonNode> externally_emitted = new LinkedList<>();
		
		final IEnrichmentModuleContext context = Mockito.mock(IEnrichmentModuleContext.class, new Answer<Void>() {
		      @SuppressWarnings("unchecked")
			public Void answer(InvocationOnMock invocation) {
		    	  try {
			    	  Object[] args  = invocation.getArguments();
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
			    		  externally_emitted.add(((ObjectNode)out.left().value()).put("__a2_bucket", to.full_name()));			    		  
			    	  }
		    	  }
		    	  catch (Exception e) {
		    		  e.printStackTrace();
		    	  }
		          return null;
		      }});
		
		final EnrichmentControlMetadataBean control =
				BeanTemplateUtils.build(EnrichmentControlMetadataBean.class)
					.with(EnrichmentControlMetadataBean::config,
							new LinkedHashMap<String, Object>(ImmutableMap.<String, Object>builder()
								.put("script", user_script)
							.build())
							)
				.done().get();
		
		service_under_test.onStageInitialize(context, bucket, control, Tuples._2T(ProcessingStage.batch, ProcessingStage.grouping), Optional.empty());
				
		final BeJsonParser json_parser = new BeJsonParser();
		
		// Run the file through
		
		final Stream<Tuple2<Long, IBatchRecord>> json_stream = StreamUtils.takeUntil(
				Stream.generate(() -> json_parser.getNextRecord(io_stream)), 
				i -> null == i)
				.map(j -> Tuples._2T(0L, new BatchRecord(j)))
				;
	
		service_under_test.onObjectBatch(json_stream, json.map(j -> j.get("len")).map(j -> (int)j.asLong(0L)), json.map(j -> j.get("group")));

		System.out.println("RESULTS: ");
		System.out.println("emitted: " + emitted.size());
		System.out.println("grouped: " + grouped.size());
		System.out.println("externally emitted: " + externally_emitted.size());
		Files.write(emitted.stream().map(j -> j.toString()).collect(Collectors.joining(";")), new File(args[2] + "emit.json"), Charsets.UTF_8);
		Files.write(grouped.stream().map(j -> j.toString()).collect(Collectors.joining(";")), new File(args[2] + "group.json"), Charsets.UTF_8);
		Files.write(externally_emitted.stream().map(j -> j.toString()).collect(Collectors.joining(";")), new File(args[2] + "external_emit.json"), Charsets.UTF_8);
	}
	
	// UTILS:

	/** Parser for reading in JSON data
	 * @author Alex
	 */
	public static class BeJsonParser {
		private ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
		private JsonParser _parser = null;
		private JsonFactory _factory = null;
			
		public JsonNode getNextRecord(InputStream inStream) {
			try {
				if (null == _factory) {
					_factory = _mapper.getFactory();
				}
				if (null == _parser) {
					_parser = _factory.createParser(inStream);
				}
				JsonToken token = _parser.nextToken();
				while ((token != JsonToken.START_OBJECT) && (token != null)) {
					token = _parser.nextToken();
				}
				if (null == token) {
					_parser = null;
					return null; //EOF
				}
				JsonNode node = _parser.readValueAsTree();				
				return node;
				
			} catch (Exception e) {
				// (this can often happen as an EOF condition)s
				_parser = null;
				return null; //EOF
			}
		}

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
