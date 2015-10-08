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
package com.ikanow.aleph2.remote.security_tests;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.shared.AuthorizationBean;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/** Logs in as designated user and accesses security
 * @author Alex
 */
public class SimpleSecurityTest {

	@Inject
	public IServiceContext _service_context;
	
	public static void main(final String args[]) throws Exception {
		if (args.length < 2) {
			System.out.println("CLI: <host> <owner_id>");
			System.exit(-1);
		}
		
		final String temp_dir = System.getProperty("java.io.tmpdir") + File.separator;
		
		Config config = ConfigFactory.parseReader(new InputStreamReader(SimpleSecurityTest.class.getResourceAsStream("/test_security_service_v1_remote.properties")))
				.withValue("globals.local_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_cached_jar_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.distributed_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_yarn_config_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("MongoDbManagementDbService.mongodb_connection", ConfigValueFactory.fromAnyRef(args[0] + ":27017"))
				;
		
		final SimpleSecurityTest app = new SimpleSecurityTest();
		
		final Injector app_injector = ModuleUtils.createTestInjector(Arrays.asList(), Optional.of(config));	
		app_injector.injectMembers(app);
		
		app.runTest(args[1]);
	}
	
	public void runTest(final String owner_id) {
		final CompletableFuture<Long> count = _service_context.getCoreManagementDbService().getSharedLibraryStore().readOnlyVersion().secured(_service_context, new AuthorizationBean(owner_id)).countObjects();
		
		System.out.println("Shared lib count: " + count.join().intValue());
	}
}
