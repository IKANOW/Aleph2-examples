package com.ikanow.aleph2.examples.rest;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is an example of using jax-rs StreamingOutput, which is different
 * from websockets, it seems to allow sending larger amounts of data (quickly) but
 * it blocks until the entire output is ready still.
 * 
 * @author Burch
 *
 */
@Path("/rest/stream")
public class StreamResource {
	private final ObjectMapper objectMapper;
	
	public StreamResource() {
		objectMapper = new ObjectMapper();
	}
	
	//http://localhost:8080/aleph2_example_webapp_kafka/rest/stream/hello
	@GET
	@Path("hello")
	@Produces(MediaType.TEXT_PLAIN)
	public String getHelloWorld() {
		return "Hello World";
	}
	
	//http://localhost:8080/aleph2_example_webapp_kafka/rest/stream/topic/{kafka_topic}
	@GET
	@Path("topic/{kafka_topic}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response streamKafkaTopic(@PathParam("kafka_topic") String kafka_topic) {
		
		StreamingOutput stream = new StreamingOutput() {
			
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				@SuppressWarnings("deprecation")
				JsonGenerator jg = objectMapper.getJsonFactory().createJsonGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
								
				int i = 0;
				while ( i < 1000 ) {
					i++;
					jg.writeStartObject();
					jg.writeFieldName("test");
					jg.writeNumber(i);
					jg.writeEndObject();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				jg.writeEndArray();
				jg.flush();
				jg.close();
			}
		};
		return Response.ok().entity(stream).build();
		
	}
}
