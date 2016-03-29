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
package com.ikanow.aleph2.remote.es_tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.data_services.IDocumentService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService.Cursor;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService.IReadOnlyCrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils.QueryComponent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/** Logs in as designated user and accesses security
 * @author Alex
 */
public class SimpleReadableCrudTest {

	@Inject
	public IServiceContext _service_context;
	
	public static void main(final String args[]) throws Exception {
		if (args.length < 1) {
			System.out.println("CLI: <host>");
			System.exit(-1);
		}
		
		final String temp_dir = System.getProperty("java.io.tmpdir") + File.separator;
		
		Config config = ConfigFactory.parseReader(new InputStreamReader(SimpleReadableCrudTest.class.getResourceAsStream("/test_es_remote.properties")))
				.withValue("globals.local_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_cached_jar_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.distributed_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_yarn_config_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("MongoDbManagementDbService.mongodb_connection", ConfigValueFactory.fromAnyRef(args[0] + ":27017"))
				.withValue("ElasticsearchCrudService.elasticsearch_connection", ConfigValueFactory.fromAnyRef(args[0] + ":9300"))
				;
		
		final SimpleReadableCrudTest app = new SimpleReadableCrudTest();
		
		final Injector app_injector = ModuleUtils.createTestInjector(Arrays.asList(), Optional.of(config));	
		app_injector.injectMembers(app);

		app.runTest();
		System.exit(0);
	}
	
	public void runTest() throws IOException {
		System.out.println("running test");
		
		final String bucket_str = Resources.toString(Resources.getResource("com/ikanow/aleph2/remote/es_tests/test_es_databucket.json"), Charsets.UTF_8);
						
		final DataBucketBean bucket = BeanTemplateUtils.from(bucket_str, DataBucketBean.class).get();
		
		
		final Optional<IReadOnlyCrudService<JsonNode>> maybe_read_crud = 
				_service_context.getService(IDocumentService.class, Optional.empty())
						.flatMap(ds -> ds.getDataService())
						.flatMap(ds -> ds.getReadableCrudService(JsonNode.class, Arrays.asList(bucket), Optional.empty()))
				;
				
		maybe_read_crud.ifPresent(read_crud -> System.out.println("*************** " + read_crud.countObjects().join().intValue()));
		
		QueryComponent<JsonNode> query = CrudUtils.allOf()
			.withAny("email_key.raw", Arrays.asList("dionne05@gmail.com", "dionne05@gmail.com", "dionnewalker2016@gmail.com", "diontaethompson10@gmil.com", "dionteb41@gmail.com",
					"diquangantt@ymail.com"))
					.limit(Integer.MAX_VALUE)
					;
				
		
		
		System.out.println(query.toString());
		
		final Cursor<JsonNode> q = maybe_read_crud.get().getObjectsBySpec(query).join();
		
		q.iterator().forEachRemaining(j -> System.out.println("??? " + j.toString()));
	}
}
