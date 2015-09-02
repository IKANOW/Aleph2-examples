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
package com.ikanow.aleph2.examples.client.services;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.data_services.IManagementDbService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.shared.AssetStateDirectoryBean;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.examples.client.data_model.ExampleBean;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/** Example service illustrating use of the CRUD service
 * @author Alex
 */
public class TestExampleClientService {

	@Inject
	protected IServiceContext _context;
	
	protected IManagementDbService _core_mgmt_db;
	
	@Before
	public void setup() throws Exception {
		final String temp_dir = System.getProperty("java.io.tmpdir") + File.separator;
		
		// OK we're going to use guice, it was too painful doing this by hand...				
		final Config config = ConfigFactory.parseReader(new InputStreamReader(this.getClass().getResourceAsStream("/example_test.properties")))
							.withValue("globals.local_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
							.withValue("globals.local_cached_jar_dir", ConfigValueFactory.fromAnyRef(temp_dir))
							.withValue("globals.distributed_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
							.withValue("globals.local_yarn_config_dir", ConfigValueFactory.fromAnyRef(temp_dir));
		
		final Injector app_injector = ModuleUtils.createTestInjector(Arrays.asList(), Optional.of(config));	
		app_injector.injectMembers(this);
		
		_core_mgmt_db = _context.getCoreManagementDbService();
	}
	
	@Test
	public void test_doSomething() throws InterruptedException, ExecutionException {
		//cleaning up our test dbs, to ensure they are empty
		_core_mgmt_db.getPerLibraryState(ExampleBean.class, ExampleClientService.CRUD_LOCATION, Optional.of("test1")).deleteDatastore().get();
		_core_mgmt_db.getPerLibraryState(JsonNode.class, ExampleClientService.CRUD_LOCATION, Optional.of("test2")).deleteDatastore().get();
		
		// Get the directory and check it's empy
		final ICrudService<AssetStateDirectoryBean> collections = _core_mgmt_db.getPerLibraryState(AssetStateDirectoryBean.class, ExampleClientService.CRUD_LOCATION, Optional.empty());
		assertEquals("Starting state cleared", 0, collections.countObjects().get().intValue());
		
		// (non guice way of creating the test service)		
		final ExampleClientService test_service = new ExampleClientService(_context);

		// Here's an example of using generic access to a CRUD service
		//2 examples of crud pointing to test1
		final ICrudService<ExampleBean> bean_crud1 =  _core_mgmt_db.getPerLibraryState(ExampleBean.class, ExampleClientService.CRUD_LOCATION, Optional.of("test1"));				
		final ICrudService<JsonNode> json_crud1a =  _core_mgmt_db.getPerLibraryState(ExampleBean.class, ExampleClientService.CRUD_LOCATION, Optional.of("test1")).getRawService();
		
		//crud pointing to test2
		final ICrudService<JsonNode> json_crud2 =  _core_mgmt_db.getPerLibraryState(JsonNode.class, ExampleClientService.CRUD_LOCATION, Optional.of("test2"));
		
		//we now have 2 collections, even though we have 3 crud services
		assertEquals("Added a collection", 2, collections.countObjects().get().intValue());
		
		//push something in test1
		test_service.doSomething("test1", "test_id_1", "value1").get();					
		assertEquals("Added an object to collection", 1, bean_crud1.countObjects().get().intValue());
		assertEquals("Added an object to collection", 1, json_crud1a.countObjects().get().intValue());
		assertEquals("Other collection is still empty", 0, json_crud2.countObjects().get().intValue());	
		
		//push something in test2
		test_service.doSomething("test2", "test_id_2", "value2").get();
		assertEquals("Added an object to collection", 1, json_crud2.countObjects().get().intValue());		
		assertEquals(Arrays.asList("test1", "test2"), test_service.enumerate_collections_sync());
	}	
}
