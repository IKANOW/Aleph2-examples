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
package com.ikanow.aleph2.examples.client.module;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Inject;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.examples.client.services.ExampleClientService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import fj.data.Either;

/** The guice entry point for the module
 *  This is slightly impure since I am using the module both as a guice container and do other start-up stuff
 *  I like doing it this way, but others don't.
 * @author Alex
 */
public class ExampleClientAppModule extends AbstractModule {
	
	final protected IServiceContext _service_context;
	
	/** User c'tor (this is only used to pass the module into guice) 
	 */
	private ExampleClientAppModule() {
		_service_context = null;
	}
	
	/** Guice c'tor - the actual module we'll use
	 */
	@Inject
	private ExampleClientAppModule(final IServiceContext service_context) {
		_service_context = service_context;
	}
	
	
	/** Entry point
	 * @param args - the first arg is the location of the config file
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("CLI: config_file");
			System.exit(-1);
		}
		System.out.println("Running with command line: " + Arrays.toString(args));
		final Config config = ConfigFactory.parseFile(new File(args[0]));
		
		final ExampleClientAppModule app = ModuleUtils.initializeApplication(Arrays.asList(new ExampleClientAppModule()), Optional.of(config), Either.left(ExampleClientAppModule.class));
		app.start();
		
	}

	/** Start the app
	 */
	public void start() {
		// NOTE: if I didn't want to use guice to build this application's innards I could have called
		// final ExampleClientAppModule app = ModuleUtils.initializeApplication(Collections.emptyList(), Optional.of(config), Either.left(ExampleClientAppModule.class));
		// from main...
		// removed the configure() function and the inheritance of AbstractModule
		// ...and then done this:
		// new ExampleClientService(_service_context);
		// I don't need to here because guice automatically creates a singleton of ExampleClientService and passes the service context into it
	}
	
	@Override
	protected void configure() {
		this.bind(ExampleClientService.class).in(Scopes.SINGLETON);
	}
}
