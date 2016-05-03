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
package com.ikanow.aleph2.application;

import java.util.concurrent.ExecutionException;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;

/**
 * @author Burch
 *
 */
public class ExampleApplication extends ResourceConfig {
	
	public ExampleApplication() {
		Injector injector;
		try {
			injector = ModuleUtils.getAppInjector().get();
			final IServiceContext service_context = injector.getInstance(IServiceContext.class);
			System.out.println("got service_context");
						
			
			//add a router to a resource
			
			//example of programmatically adding resources to our router
			//we can't fully do this because multipart (file upload) does not have
			//full support
			//GET query method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}", "GET", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.QUERY)));

			register(MultiPartFeature.class); //required for jersey mutlipart support (file upload)			
			register(new MasterResource(service_context));
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
	}
}
