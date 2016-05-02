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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.aleph2_rest_utils.FileDescriptor;
import com.ikanow.aleph2.aleph2_rest_utils.RestUtils;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService.Cursor;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;

/**
 * @author Burch
 *
 */
@Path("/multipart")
public class FormResource {

	final IServiceContext service_context;
	final Logger _logger = LogManager.getLogger();
	/**
	 * 
	 * @param service_context
	 */
	public FormResource(final IServiceContext service_context) {
		this.service_context = service_context;
	}
	
	@GET
	@Path("/test")	
	@Produces(MediaType.TEXT_PLAIN)
	public String get(@QueryParam("bucket") String bucket_full_name) {		
		_logger.error("input: " + bucket_full_name);
		try {
			final DataBucketBean bucket = RestUtils.convertToBucketFullName(service_context, bucket_full_name);
			final ICrudService<FileDescriptor> crud_service = RestUtils.getBucketDataStore(this.service_context, bucket);			
			CompletableFuture<Cursor<FileDescriptor>> fut = crud_service.getObjectsBySpec(null);
			String res = fut.handle((ok, ex) -> {
				if ( ok != null ) {
					StringBuilder sb = new StringBuilder();
					ok.forEach(fd -> sb.append(fd.file_name()));
					return sb.toString();
				}
				else if ( ex != null) 
					return ErrorUtils.getLongForm("Exception lsing dir: {0}", ex);
				else
					return "something broke";
			}).get();
			
			return "hello bucket is: " + bucket_full_name + " result: " + res;
		} catch (Exception e) {
			return ErrorUtils.getLongForm("Error: {0}", e);
		}
	}

	@POST
	@Path("/test")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.TEXT_PLAIN)
	public String post(
			@FormDataParam("entry") String entry, 
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("bucket") String bucket_full_name) {		
		_logger.error("input: " + entry);
		try {
			final JsonNode json = RestUtils.convertStringToJson(entry);
			//save file to storage service somewhere
			final DataBucketBean bucket = RestUtils.convertToBucketFullName(service_context, bucket_full_name);
			final ICrudService<FileDescriptor> crud_service = RestUtils.getBucketDataStore(this.service_context, bucket);			
			CompletableFuture<Supplier<Object>> fut = crud_service.storeObject(new FileDescriptor(uploadedInputStream, fileDetail.getFileName()));
			String res = fut.handle((ok, ex) -> {
				if ( ok != null )
					return "ok";
				else if ( ex != null) 
					return ErrorUtils.getLongForm("Exception storing object: {0}",ex);
				else
					return "something broke";
			}).get();
			
			return "hello" + json.toString() + " bucket is: " + bucket_full_name + " file is: " + fileDetail.getSize() + fileDetail.getFileName() + " result: " + res;
		} catch (Exception e) {
			return ErrorUtils.getLongForm("Error: {0}", e);
		}
	}
	
	@DELETE
	@Path("/test")
	@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	@Produces(MediaType.TEXT_PLAIN)
	public String delete(
			String files_to_delete,
			@QueryParam("bucket") String bucket_full_name) {
		
		_logger.error("input: " + files_to_delete);
		try {
			final JsonNode json = RestUtils.convertStringToJson(files_to_delete);			
			final DataBucketBean bucket = RestUtils.convertToBucketFullName(service_context, bucket_full_name);
			final ICrudService<FileDescriptor> crud_service = RestUtils.getBucketDataStore(this.service_context, bucket);
			//delete files
			Collection<String> results = new ArrayList<String>();
			json.forEach(n->{
				_logger.error("deleting file: " + n.asText());
				try {
					results.add( crud_service.deleteObjectById(n.asText()).handle((ok, ex) -> {
						if ( ok != null )
							return "ok";
						else if ( ex != null) 
							return ErrorUtils.getLongForm("Exception deleting object: {0}",ex);
						else
							return "something broke";
							
					}).get());
				} catch (Exception e) {
					results.add( ErrorUtils.getLongForm("Exception deleting object: {0}",e));
				}
			});			
			
			return "hello" + json.toString() + " bucket is: " + bucket_full_name +  " result: " + results.toString();
		} catch (Exception e) {
			return ErrorUtils.getLongForm("Error: {0}", e);
		}
	}
}
