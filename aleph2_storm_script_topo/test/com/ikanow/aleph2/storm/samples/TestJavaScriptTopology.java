/*******************************************************************************
* Copyright 2015, The IKANOW Open Source Project.
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License, version 3,
* as published by the Free Software Foundation.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
******************************************************************************/
package com.ikanow.aleph2.storm.samples;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_import.context.stream_enrichment.utils.ErrorUtils;
import com.ikanow.aleph2.data_import.services.StreamingEnrichmentContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.ISearchIndexService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.distributed_services.services.ICoreDistributedServices;
import com.ikanow.aleph2.distributed_services.utils.KafkaUtils;
import com.ikanow.aleph2.storm.samples.script.PropertyBasedScriptProvider;
import com.ikanow.aleph2.storm.samples.topology.JavaScriptTopology;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestJavaScriptTopology {

	LocalCluster _local_cluster;
	
	protected Injector _app_injector;

	private List<String> ips;
	
	 
	@Before
	public void injectModules() throws Exception {
		final Config config = ConfigFactory.parseFile(new File("./example_config_files/context_local_test.properties"));
		
		try {
			_app_injector = ModuleUtils.createInjector(Arrays.asList(), Optional.of(config));
			_local_cluster = new LocalCluster();
			this.ips = readIps();

		}
		catch (Exception e) {
			try {
				e.printStackTrace();
			}
			catch (Exception ee) {
				System.out.println(ErrorUtils.getLongForm("{0}", e));
			}
		}
	}
	
	@Test
	public void testJavaScriptTopology() throws InterruptedException, ExecutionException {
		// PHASE 1: GET AN IN-TECHNOLOGY CONTEXT
		// Bucket
		final DataBucketBean test_bucket = BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::_id, "test_js_topology")
				.with(DataBucketBean::modified, new Date())
				.with(DataBucketBean::full_name, "/test/javascript")
				.with("data_schema", BeanTemplateUtils.build(DataSchemaBean.class)
						.with("search_index_schema", BeanTemplateUtils.build(DataSchemaBean.SearchIndexSchemaBean.class)
								.done().get())
						.done().get())
				.done().get();

		// Context		
		final StreamingEnrichmentContext test_context = _app_injector.getInstance(StreamingEnrichmentContext.class);
		test_context.setBucket(test_bucket);
		test_context.setUserTopologyEntryPoint("com.ikanow.aleph2.storm.samples.topology.JavaScriptTopology");
		test_context.getEnrichmentContextSignature(Optional.empty(), Optional.empty());
		test_context.overrideSavedContext(); // (THIS IS NEEDED WHEN TESTING THE KAFKA SPOUT)
		
		//PHASE 2: CREATE TOPOLOGY AND SUBMit		
		final ICoreDistributedServices cds = test_context.getService(ICoreDistributedServices.class, Optional.empty()).get();
		final StormTopology topology = (StormTopology) new JavaScriptTopology()
											.getTopologyAndConfiguration(test_bucket, test_context)
											._1();
		
		final backtype.storm.Config config = new backtype.storm.Config();
		config.setDebug(true);
		_local_cluster.submitTopology("test_js_topology", config, topology);		
		Thread.sleep(3000L);
		
		//PHASE 3: CHECK INDEX
		final ISearchIndexService index_service = test_context.getService(ISearchIndexService.class, Optional.empty()).get();
		final ICrudService<JsonNode> crud_service = index_service.getCrudService(JsonNode.class, test_bucket).get();
		crud_service.deleteDatastore().get();
		Thread.sleep(1000L);
		assertEquals(0L, crud_service.countObjects().get().intValue());
		
		//PHASE4 : WRITE TO KAFKA
		int count = 0;
		for (String ip : ips) {
			String msg = "{\"ip\":\""+ip+"\"}";
			// "{\"test\":\"test1\"}"
			cds.produce(KafkaUtils.bucketPathToTopicName(test_bucket.full_name()), msg);
			if(count>100){ break;}
			count++;
		}
		Thread.sleep(90000L);
		
		assertEquals(1L, crud_service.countObjects().get().intValue());		
	}
	
	protected List<String> readIps() throws IOException{
		BufferedReader br  = null;
		List<String> ips =  new ArrayList<String>();
	    try {
	    	br = new BufferedReader(new FileReader("./example_config_files/zeus_badips.txt"));
	        String line = null;
	        while ((line = br.readLine()) != null) {
	        	if(!line.trim().startsWith("#")){
	        		ips.add(line);
	        	}	           
	        }
	    } finally {
	        br.close();
	    }
		return ips;	
	}
	
}
