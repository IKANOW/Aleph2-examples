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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.ikanow.aleph2.aleph2_rest_utils.MasterResource;
import com.ikanow.aleph2.aleph2_rest_utils.QueryComponentBean;
import com.ikanow.aleph2.aleph2_rest_utils.QueryComponentBeanUtils;
import com.ikanow.aleph2.aleph2_rest_utils.RestCrudFunctions;
import com.ikanow.aleph2.aleph2_rest_utils.RestCrudFunctions.FunctionType;
import com.ikanow.aleph2.aleph2_rest_utils.RestUtils;
import com.ikanow.aleph2.data_model.interfaces.data_services.IManagementDbService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService.Cursor;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean.SearchIndexSchemaBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils.QueryComponent;
import com.ikanow.aleph2.data_model.utils.CrudUtils.SingleQueryComponent;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;

import fj.data.Either;

/**
 * @author Burch
 *
 */
public class ExampleApplication extends ResourceConfig {
	private static Logger _logger = LogManager.getLogger();
	
	public ExampleApplication() {
		Injector injector;
		try {
			injector = ModuleUtils.getAppInjector().get();
			final IServiceContext service_context = injector.getInstance(IServiceContext.class);
			System.out.println("got service_context");
						
			
			//add a router to a resource
			
			///////Read Methods/////////////////////////////////////////////////////
			//GET query method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}", "GET", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.QUERY)));
//						
//			//GET id method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}/{id}", "GET", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.QUERY)));
//			
//			//GET alternative POST method (for query in body)
//			registerResources(RestUtils.buildResource("aleph2_api/query/{service_name}/{read_write}/{identifier}", "POST", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.QUERY)));
//			
//			//GET count method
//			registerResources(RestUtils.buildResource("aleph2_api/count/{service_name}/{read_write}/{identifier}", "GET", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.COUNT)));
//			//GET alternative POST count method (for query in body)
//			registerResources(RestUtils.buildResource("aleph2_api/count/{service_name}/{read_write}/{identifier}", "POST", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getReadFunction(service_context, FunctionType.COUNT)));
//			
//			////////////////Create Methods///////////////////////////////////////////////////////
//			//POST object method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}", "POST", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getCreateFunction(service_context)));
//			
//			///////Update Methods//////////////////////////////////////////////////////////
//			//PUT query, update method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}", "PUT", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getUpdateFunction(service_context)));
//			
//			/////////Delete Methods////////////////////////////////////////////////////////////////
//			//DELETE query method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}", "DELETE", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getDeleteFunction(service_context)));	 
//			
//			//DELETE id method
//			registerResources(RestUtils.buildResource("aleph2_api/{service_name}/{read_write}/{identifier}/{id}", "DELETE", Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), Arrays.asList(MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getDeleteFunction(service_context)));
//			
			
			
			
			
//			//testing multipart
//			registerResources(RestUtils.buildResource("aleph2_api/test", "POST", Arrays.asList(MediaType.MULTIPART_FORM_DATA), Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), service_context, 
//					RestCrudFunctions.getMultipartCreateFunction(service_context)));
//			
			register(MultiPartFeature.class);
//			register(new FormResource(service_context));
			
			register(new MasterResource(service_context));
			
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static ICrudService<JsonNode> getCrudService(final IServiceContext service_context) {
		//create some bucket to read from for test
		final DataBucketBean bucket = BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::full_name, "/bucket/burch/storm_test_demo3")
				.with(DataBucketBean::owner_id, "54f86d8de4b03d27d1ea0d7b")
				.with(DataBucketBean::data_schema, BeanTemplateUtils.build(DataSchemaBean.class)
						.with(DataSchemaBean::search_index_schema, BeanTemplateUtils.build(SearchIndexSchemaBean.class).done().get())
						.done().get())
				.done().get();
		//return a crud service pointed to that bucket (aka search index service)
		return service_context.getSearchIndexService().get().getDataService().get().getReadableCrudService(JsonNode.class, Arrays.asList(bucket), Optional.empty()).get();
//		return service_context.getSearchIndexService().get().getDataService().get().getWritableDataService(JsonNode.class, bucket, Optional.empty(), Optional.empty()).get().getCrudService().get();
	}
}
