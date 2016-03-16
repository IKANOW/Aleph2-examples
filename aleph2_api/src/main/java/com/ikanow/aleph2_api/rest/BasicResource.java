package com.ikanow.aleph2_api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Just a basic test class that should return "Hello World" when you navigate to:
 * http://localhost:8080/aleph2_api/rest/basic/hello
 * http://localhost:8080/aleph2_api/rest/basic/bye/burch
 * 
 * @author Burch
 *
 */
@Path("/rest/basic")
public class BasicResource {
	
	@GET
	@Path("hello")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getHelloWorld() {
		return Response.status(Status.OK).entity("hello world").build();
	}
	
	@GET
	@Path("bye/{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getByeWorld(@PathParam("name") String name) {
		return Response.status(Status.OK).entity("bye world, " + name).build();
	}
}
