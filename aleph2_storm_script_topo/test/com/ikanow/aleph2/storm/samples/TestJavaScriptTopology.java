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
package com.ikanow.aleph2.storm.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import backtype.storm.LocalCluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.ikanow.aleph2.analytics.storm.services.MockStormTestingService;
import com.ikanow.aleph2.data_import.context.stream_enrichment.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.interfaces.data_services.ISearchIndexService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IDataWriteService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadBean;
import com.ikanow.aleph2.data_model.objects.data_analytics.AnalyticThreadJobBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.distributed_services.services.ICoreDistributedServices;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestJavaScriptTopology {

	static final Logger _logger = LogManager.getLogger(); 

	LocalCluster _local_cluster;
	
	protected Injector _app_injector;

	private List<String> ips;
	
	@Inject
	protected IServiceContext _service_context;
	 
	@Before
	public void injectModules() throws Exception {
		final Config config = ConfigFactory.parseFile(new File("./example_config_files/context_local_test.properties"));
		
		try {
			_app_injector = ModuleUtils.createTestInjector(Arrays.asList(), Optional.of(config));
			_local_cluster = new LocalCluster();
			this.ips = readIps();

			_app_injector.injectMembers(this);
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
		final AnalyticThreadJobBean.AnalyticThreadJobInputBean analytic_input = 
				BeanTemplateUtils.build(AnalyticThreadJobBean.AnalyticThreadJobInputBean.class)
					.with(AnalyticThreadJobBean.AnalyticThreadJobInputBean::data_service, "stream")
					.with(AnalyticThreadJobBean.AnalyticThreadJobInputBean::resource_name_or_id, "")
				.done().get();
		
		final AnalyticThreadJobBean.AnalyticThreadJobOutputBean analytic_output =
				BeanTemplateUtils.build(AnalyticThreadJobBean.AnalyticThreadJobOutputBean.class)
					.with(AnalyticThreadJobBean.AnalyticThreadJobOutputBean::is_transient, false)
				.done().get();
		
		final AnalyticThreadJobBean analytic_job1 = BeanTemplateUtils.build(AnalyticThreadJobBean.class)
				.with(AnalyticThreadJobBean::name, "analytic_job1")
				.with(AnalyticThreadJobBean::inputs, Arrays.asList(analytic_input))
				.with(AnalyticThreadJobBean::output, analytic_output)
                 .with(AnalyticThreadJobBean::entry_point, "com.ikanow.aleph2.storm.samples.topology.JavaScriptTopology2")
                 .done().get();		
		
		final AnalyticThreadBean analytic_thread = 	BeanTemplateUtils.build(AnalyticThreadBean.class)
				.with(AnalyticThreadBean::jobs, Arrays.asList(analytic_job1))
				.done().get();		
		
		final DataBucketBean test_bucket = BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::_id, "test_js_topology")
				.with(DataBucketBean::modified, new Date())
				.with(DataBucketBean::full_name, "/test/javascript")
				.with(DataBucketBean::analytic_thread, analytic_thread)
				.with("data_schema", BeanTemplateUtils.build(DataSchemaBean.class)
						.with("search_index_schema", BeanTemplateUtils.build(DataSchemaBean.SearchIndexSchemaBean.class)
								.done().get())
						.done().get())
				.done().get();

		//////////////////////////////////////////////////////
		// PHASE 2: SPECIFICALLY FOR THIS TEST
		//(Also: register a listener on the output to generate a secondary queue)
		final ICoreDistributedServices cds = _service_context.getService(ICoreDistributedServices.class, Optional.empty()).get();
/*		final AnalyticsContext analytic_context = new AnalyticsContext(_service_context);
		analytic_context.getAnalyticsContextSignature(Optional.empty(), Optional.empty());
		analytic_context.overrideSavedContext(); // (THIS + PREV LINE ARE NEEDED WHEN TO AVOID CREATING 2 ModuleUtils INSTANCES WHICH BREAKS EVERYTHING)
		
		final StreamingEnrichmentContextService test_context = new StreamingEnrichmentContextService(analytic_context);
		test_context.setBucket(test_bucket);
		test_context.setUserTopology(new com.ikanow.aleph2.storm.samples.topology.JavaScriptTopology());
		test_context.setJob(analytic_job1);		
	*/
		
		final BasicMessageBean res = new MockStormTestingService(_service_context).testAnalyticModule(test_bucket).get();
		if(!res.success()){
			_logger.error(res.message());
		}
		assertTrue("Storm starts", res.success());
		
		_logger.info("******** Submitted storm cluster: " + res.message());
		Thread.sleep(5000L);
		

		//////////////////////////////////////////////////////
		//PHASE 4: CHECK INDEX
		final ISearchIndexService index_service = _service_context.getService(ISearchIndexService.class, Optional.empty()).get();
		final ICrudService<JsonNode> crud_service = 
				index_service.getDataService()
					.flatMap(s -> s.getWritableDataService(JsonNode.class, test_bucket, Optional.empty(), Optional.empty()))
					.flatMap(IDataWriteService::getCrudService)
					.get();
		crud_service.deleteDatastore().get();
		_logger.info("******** Cleansed existing datastore");
		Thread.sleep(2000L);
		assertEquals(0L, crud_service.countObjects().get().intValue());

		//PHASE4 : WRITE TO KAFKA
		// TODO here's my example code replace with
		//context.sendObjectToStreamingPipeline(Optional.empty(), json);
		int count = 0;
		for (String ip : ips) {
			String msg = "{\"ip\":\""+ip+"\"}";
			// "{\"test\":\"test1\"}"
			cds.produce(cds.generateTopicName(test_bucket.full_name(), Optional.empty()), msg);
			if(count>100){ break;}
			count++;
		}
		Thread.sleep(9000L);
		
		assertTrue(crud_service.countObjects().get().intValue()>0);		
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
	        if (null != br) br.close();
	    }
		return ips;	
	}
	
}
