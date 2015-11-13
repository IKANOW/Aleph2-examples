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
package com.ikanow.aleph2.examples.client.services;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
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
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.data_model.utils.UuidUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils.BeanUpdateComponent;
import com.ikanow.aleph2.data_model.utils.CrudUtils.QueryComponent;
import com.ikanow.aleph2.examples.client.data_model.ExampleBean;
import com.ikanow.aleph2.examples.client.data_model.ExampleComplexBean;
import com.ikanow.aleph2.examples.client.data_model.ExampleSubBean;
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
	
	@Test
	public void CRUDTest() throws InterruptedException, ExecutionException {
		//this example runs through the crud operations on a more complex bean object
		//first clear out the collection to make sure we are starting from scratch
		final SharedLibraryBean CRUD_LOCATION = BeanTemplateUtils.build(SharedLibraryBean.class).with(SharedLibraryBean::path_name, "my_db").done().get();
		_core_mgmt_db.getPerLibraryState(ExampleComplexBean.class, CRUD_LOCATION, Optional.of("test1")).deleteDatastore().get();
	
		//STEP 1: create the crud service so we can access our collection
		final ICrudService<ExampleComplexBean> bean_crud = _core_mgmt_db.getPerLibraryState(ExampleComplexBean.class, CRUD_LOCATION, Optional.of("test1"));
		assertEquals("Collection shoudl be empty", 0, bean_crud.countObjects().get().intValue());
		
		//STEP 2: create and insert a bean into collection
		//create sub object
		final ExampleSubBean sub_bean = BeanTemplateUtils.build(ExampleSubBean.class)
				.with(ExampleSubBean::field1, "sub field 1")
				.with(ExampleSubBean::field2, new Date())
				.with(ExampleSubBean::field3, System.currentTimeMillis())
				.done().get();
		//create bean
		final String id = UuidUtils.get().getRandomUuid();
		final ExampleComplexBean bean = BeanTemplateUtils.build(ExampleComplexBean.class)
				.with(ExampleComplexBean::_id, id)
				.with(ExampleComplexBean::value, "some value")
				.with(ExampleComplexBean::sub_object, sub_bean)
				.done().get();
		//insert
		bean_crud.storeObject(bean).get();
		
		//STEP 3: find our bean in collection
		//search via id
		assertEquals("Retrieved bean should have this value", bean_crud.getObjectById(id).get().get().value(), "some value");
		//search via query		
		QueryComponent<ExampleComplexBean> query = CrudUtils.allOf(ExampleComplexBean.class)
				.when(ExampleComplexBean::value, "some value");			
		assertEquals("Retrieved bean should have this value", bean_crud.getObjectBySpec(query).get().get().value(), "some value");
		
		//STEP 4: update our bean
		BeanUpdateComponent<ExampleComplexBean> update = CrudUtils.update(ExampleComplexBean.class)
				.set(ExampleComplexBean::value, "some new value");
		assertTrue(bean_crud.updateObjectById(id, update).get());
		
		//STEP 5: retrieve updated bean
		QueryComponent<ExampleComplexBean> query1 = CrudUtils.allOf(ExampleComplexBean.class)
				.when(ExampleComplexBean::value, "some new value");
		assertEquals("Retrieved bean should have this value", bean_crud.getObjectBySpec(query1).get().get().value(), "some new value");
		
		//STEP 6: delete bean
		bean_crud.deleteObjectById(id).get();
		assertEquals("Collection should be empty now", bean_crud.countObjects().get().longValue(), 0L);		
	}
}
