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
******************************************************************************/
package com.ikanow.aleph2.storm_harvest_technology;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IUuidService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.data_model.utils.UuidUtils;
import com.ikanow.aleph2.storm.harvest_technology.StormHarvestTechnologyModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestStormHarvestTechnologyModule {

//	private IUuidService uuid_service = UuidUtils.get();
//	private Injector _app_injector;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Creates a data bucket bean using the simple harvester config to
	 * specify a few config params for a topology.
	 * 
	 * @return
	 */
//	private DataBucketBean createTestDataBucketBean() {		
//		List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
//		Map<String, Object> config = new HashMap<String, Object>();
//		config.put("source_type", "File");
//		config.put("source_url", "sample_log_files/proxy_small_sample.log");
//		config.put("source_parser", "ProxyParser");
//		config.put("source_output", "File");
//		harvest_configs.add(new HarvestControlMetadataBean("storm_harvest_config", true, null, config));
//		return BeanTemplateUtils.build(DataBucketBean.class)
//		.with("_id", uuid_service.getTimeBasedUuid())
//		.with("harvest_configs", harvest_configs)
//		.done().get();
//	}
//	
//	/**
//	 * Creates a data bucket bean using the alternate config pointing to a
//	 * file to get a custom topology from.
//	 * 
//	 * @return
//	 */
//	private DataBucketBean createTestDataBucketBeanTopology1() {		
//		List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
//		Map<String, Object> config = new HashMap<String, Object>();
//		config.put("IStormHarvestTopologyClass", SampleStormHarvestTopology1.class.getCanonicalName());
//		harvest_configs.add(new HarvestControlMetadataBean("storm_harvest_config", true, null, config));
//		return BeanTemplateUtils.build(DataBucketBean.class)
//		.with("_id", uuid_service.getTimeBasedUuid())
//		.with("harvest_configs", harvest_configs)
//		.done().get();
//	}
//	
//	/**
//	 * Creates a data bucket bean using the alternate config pointing to a
//	 * file to get a custom topology from that matches what you get from the simple config in createTestDataBucketBean
//	 * 
//	 * @return
//	 */
//	private DataBucketBean createTestDataBucketBeanTopology2() {		
//		List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
//		Map<String, Object> config = new HashMap<String, Object>();
//		config.put("IStormHarvestTopologyClass", SampleStormHarvestTopology2.class.getCanonicalName());
//		harvest_configs.add(new HarvestControlMetadataBean("storm_harvest_config", true, null, config));
//		return BeanTemplateUtils.build(DataBucketBean.class)
//		.with("_id", uuid_service.getTimeBasedUuid())
//		.with("harvest_configs", harvest_configs)
//		.done().get();
//	}
//	
//	private IHarvestTechnologyModule getHarvestTech() throws Exception {
//		final Config config = ConfigFactory.parseFile(new File("./example_config_files/harvest_local_test.properties"));
//		ModuleUtils.loadModulesFromConfig(config);
//		_app_injector = ModuleUtils.createInjector(Arrays.asList(), Optional.of(config));
//		//IServiceContext service_context = ModuleUtils.getService(IServiceContext.class, Optional.empty());
//		//IHarvestTechnologyModule harvest_tech = new StormHarvestTecnologyModule(service_context);
//		IHarvestTechnologyModule harvest_tech = new StormHarvestTechnologyModule();		
//		//IHarvestTechnologyModule harvest_tech = ModuleUtils.getService(IHarvestTechnologyModule.class, Optional.of("StormHarvestTechnologyModule"));
//		return harvest_tech;
//	}
//	
//	/**
//	 * Creates an instance of the harvest context
//	 * 
//	 * @return
//	 */
//	private IHarvestContext getHarvestContext() {
//		final HarvestContext test_context = _app_injector.getInstance(HarvestContext.class);
//		return test_context;
//	}	
//	
//	@Test
//	public void testTestSource() throws Exception {			
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();
//		ProcessingTestSpecBean test_spec = new ProcessingTestSpecBean(10L, 1L);
//		IHarvestContext context = getHarvestContext();
//		//the job is only suppose to run for 1s, so it should be done
//		CompletableFuture<BasicMessageBean> future = storm.onTestSource(bucket, test_spec, context);
//		BasicMessageBean result = future.get(1L, TimeUnit.SECONDS);
//		System.out.println(result.message());
//		assertTrue(result.success());
//	}	
//	
//	@Test
//	public void testNewSource() throws Exception {
//		IHarvestTechnologyModule storm = getHarvestTech();
//		IHarvestContext context = getHarvestContext();
//		storm.onInit(context);
//		DataBucketBean bucket = createTestDataBucketBeanTopology1();			
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());		
//		Thread.sleep(20000);
//	}
//	
//	@Test
//	public void testPollSource() throws Exception {		
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		Thread.sleep(500); //sleep a little bit to let the job start
//		CompletableFuture<BasicMessageBean> future_poll = storm.onPeriodicPoll(bucket, context);
//		//System.out.println("MESSAGE: " + future_poll.get().message());
//		BasicMessageBean result = future_poll.get(20L, TimeUnit.SECONDS);
//		assertTrue(result.success());				
//	}
//	
//	@Test
//	public void testStopSource() throws Exception {
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		//immediately try to kill it
//		CompletableFuture<BasicMessageBean> delete_poll = storm.onDelete(bucket, context);
//		BasicMessageBean result = delete_poll.get(20L, TimeUnit.SECONDS);
//		assertTrue(result.success());
//	}
//	
//	@Test
//	public void testPurgeSource() throws Exception {
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		//immediately try to kill it
//		CompletableFuture<BasicMessageBean> delete_poll = storm.onPurge(bucket, context);
//		BasicMessageBean result = delete_poll.get(20L, TimeUnit.SECONDS);
//		assertTrue(result.success());
//	}
//	
//	@Test
//	public void testCompleteSource() throws Exception {
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//				
//		Thread.sleep(10000); //wait a bit to finish
//		CompletableFuture<BasicMessageBean> delete_poll = storm.onHarvestComplete(bucket, context);
//		BasicMessageBean result = delete_poll.get(20L, TimeUnit.SECONDS);
//		assertTrue(result.success());
//	}
//	
//	@Test
//	public void testSuspendResumeSource() throws Exception {
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		//immediately try to suspend it
//		CompletableFuture<BasicMessageBean> delete_poll = storm.onSuspend(bucket, context);
//		BasicMessageBean result = delete_poll.get(20L, TimeUnit.SECONDS);
//		assertTrue(result.success());
//		
//		//wait a few second for it to die, and release control of directory
//		Thread.sleep(1000);
//		//try to resume it then
//		//TODO on resume I need to start up the bucket with a different id, so im going to have to 
//		//create some map to 
//		CompletableFuture<BasicMessageBean> future_resume = storm.onResume(bucket, context);
//		assertTrue(future_resume.get(20L, TimeUnit.SECONDS).success());
//	}
//	
//	@Test
//	public void testUpdateSource() throws Exception {
//		//kick off a source
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBean();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		//try to update source
//		DataBucketBean bucket_updated = createTestDataBucketBean();	
//		CompletableFuture<BasicMessageBean> future_updated = storm.onUpdatedSource(bucket, bucket_updated, true, Optional.empty(), context);
//		assertTrue(future_updated.get(20L, TimeUnit.SECONDS).success());
//	}
//	
//	@Test
//	public void testTopologyBuilder1() throws Exception {
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBeanTopology1();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		Thread.sleep(10000);
//		
//	}
//	
//	@Test
//	public void testTopologyBuilder2() throws Exception {
//		IHarvestTechnologyModule storm = getHarvestTech();
//		DataBucketBean bucket = createTestDataBucketBeanTopology2();		
//		IHarvestContext context = getHarvestContext();
//		CompletableFuture<BasicMessageBean> future = storm.onNewSource(bucket, context, true);		
//		assertTrue(future.get(20L, TimeUnit.SECONDS).success());
//		
//		Thread.sleep(10000);
//		
//	}
}
