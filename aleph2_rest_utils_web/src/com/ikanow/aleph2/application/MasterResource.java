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

import java.io.InputStream;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.ikanow.aleph2.aleph2_rest_utils.FileDescriptor;
import com.ikanow.aleph2.aleph2_rest_utils.RestCrudFunctions;
import com.ikanow.aleph2.aleph2_rest_utils.RestCrudFunctions.FunctionType;
import com.ikanow.aleph2.aleph2_rest_utils.RestUtils;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;

@Path("/aleph2_api")
public class MasterResource {
	final IServiceContext service_context;
	final Logger _logger = LogManager.getLogger();
	
	public MasterResource(final IServiceContext service_context) {
		this.service_context = service_context;
	}
	
	
	////////////////////////////////////READ METHODS//////////////////////////////////////
	//read w/ query in body GET call (most services don't allow body but crud w3c does I think)
	@GET
	@Path("/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			@QueryParam("limit") Long limit,
			String query_json) {
		return get(service_name, read_write, identifier, null, buckets, limit, query_json);		
	}
	
	//read w/ query in body POST call (circumventing get with body issues)
	@POST
	@Path("/query/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuery(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			@QueryParam("limit") Long limit,
			String query_json) {
		return get(service_name, read_write, identifier, null, buckets, limit, query_json);
	}
	
	//read by id
	@GET
	@Path("/{service_name}/{read_write}/{identifier}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@PathParam("id") String id,
			@QueryParam("buckets") String buckets,
			@QueryParam("limit") Long limit) {
		return get(service_name, read_write, identifier, null, buckets, limit, null);
	}
	
	//helper function for all read requests
	private Response get(
			String service_name,
			String read_write,
			String identifier,
			String id,
			String buckets,
			Long limit,
			String query_json) {
		_logger.error("READ QUERY request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" id: " + id +
				" b: " + buckets +
				" q: " + query_json +
				" l: " + limit
				);
		return RestCrudFunctions.readFunction(service_context, FunctionType.QUERY, service_name, read_write, 
				identifier, Optional.ofNullable(buckets), 
				RestUtils.getOptional(query_json), Optional.ofNullable(id), 
				RestUtils.getOptional(limit));
	}
	
	//count by query (body of GET request)
	@GET
	@Path("/count/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCount(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			String query_json) {
		return postCount(service_name, read_write, identifier, buckets, query_json);		
	}
	
	//count by query (body of POST request)
	@POST
	@Path("/count/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCount(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			String query_json) {
		_logger.error("READ COUNT request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" b: " + buckets +
				" q: " + query_json
				);
		return RestCrudFunctions.readFunction(service_context, FunctionType.COUNT, service_name, read_write, identifier, Optional.ofNullable(buckets), RestUtils.getOptional(query_json), Optional.empty(), Optional.empty());
	}
	
	
	////////////////////////////////////CREATE METHODS//////////////////////////////////////
	//regular json post
	@POST
	@Path("/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			String json) {
		_logger.error("CREATE request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" b: " + buckets +
				" j: " + json
				);
		//if json sent in body, sent create request, otherwise return error
		return RestUtils.getOptional(json).map(j->RestCrudFunctions.createFunction(service_context, service_name, read_write, identifier, Optional.ofNullable(buckets), j))
		.orElse(Response.status(Status.BAD_REQUEST).entity("POST requires json in the body").build());
		
	}
	
	//file + (maybe) json post
	//this is specifically for 2 end points:
	//management_db | write | bucket file uploads (no json)
	//management_db | write | shared lib uploads (json)
	@POST
	@Path("/upload/{service_name}/{read_write}/{identifier}")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.TEXT_PLAIN)
	public Response post(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@FormDataParam("json") String json, 
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("buckets") String buckets) {
		//bail out if file was not set, entry is optional
		if ( fileDetail == null ) {
			return Response.status(Status.BAD_REQUEST).entity("POST UPLOAD requires a multipart form data item named 'file'").build();
		}
		_logger.error("CREATE request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" b: " + buckets +
				" j: " + json +
				" f: " + fileDetail.getFileName()
				);			
		return RestCrudFunctions.createFunction(service_context, service_name, read_write, identifier, Optional.ofNullable(buckets), new FileDescriptor(uploadedInputStream, fileDetail.getFileName()), RestUtils.getOptional(json));		
	}


	////////////////////////////////////UPDATE METHODS//////////////////////////////////////
	//regular update request
	@PUT
	@Path("/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response put(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			String json) {
		_logger.error("UPDATE request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" b: " + buckets +
				" j: " + json
				);
		return RestCrudFunctions.updateFunction(service_context, service_name, read_write, identifier, Optional.ofNullable(buckets), RestUtils.getOptional(json));
	}
	
	//file + (maybe) json post
	//this is specifically for 2 end points:
	//management_db | write | bucket file uploads (no json)
	//management_db | write | shared lib uploads (json)
	@PUT
	@Path("/upload/{service_name}/{read_write}/{identifier}")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	@Produces(MediaType.TEXT_PLAIN)
	public Response put(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@FormDataParam("json") String json, 
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("buckets") String buckets) {
		return Response.status(Status.BAD_REQUEST).entity("PUT UPLOAD is not yet implemented, perform a delete/create call instead for now.").build();
		//bail out if file was not set, entry is optional
//		if ( fileDetail == null ) {
//			return Response.status(Status.BAD_REQUEST).entity("POST UPLOAD requires a multipart form data item named 'file'").build();
//		}
//		_logger.error("UPDATE request: sn: " + service_name +
//				" rw: " + read_write +
//				" i: " + identifier + 
//				" b: " + buckets +
//				" j: " + json +
//				" f: " + fileDetail.getFileName()
//				);			
//		return RestCrudFunctions.updateFunction(service_context, service_name, read_write, identifier, Optional.ofNullable(buckets), new FileDescriptor(uploadedInputStream, fileDetail.getFileName()), RestUtils.getOptional(json));		
	}
	
	////////////////////////////////////DELETE METHODS//////////////////////////////////////
	//delete by query
	@DELETE
	@Path("/{service_name}/{read_write}/{identifier}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@QueryParam("buckets") String buckets,
			String query_json) {
		return delete(service_name, read_write, identifier, null, buckets, query_json);		
	}
	
	//delete by id
	@DELETE
	@Path("/{service_name}/{read_write}/{identifier}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteID(
			@PathParam("service_name") String service_name,
			@PathParam("read_write") String read_write,
			@PathParam("identifier") String identifier,
			@PathParam("id") String id,
			@QueryParam("buckets") String buckets) {
		return delete(service_name, read_write, identifier, id, buckets, null);
	}
	
	//delete helper function
	private Response delete(
			String service_name,
			String read_write,
			String identifier,
			String id,
			String buckets,
			String query_json) {
		_logger.error("DELETE QUERY request: sn: " + service_name +
				" rw: " + read_write +
				" i: " + identifier + 
				" id: " + id +
				" b: " + buckets +
				" q: " + query_json
				);
		return RestCrudFunctions.deleteFunction(service_context, service_name, read_write, identifier, Optional.ofNullable(buckets), RestUtils.getOptional(query_json), Optional.ofNullable(id));
	}
	
	
}
