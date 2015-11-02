package com.ikanow.aleph2.examples.rest;

import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.distributed_services.services.ICoreDistributedServices;

/**
 * Example resource that allows writing messages to a kafka topic.  Useful when testing
 * the KafkaWebsocketResource to send test messages the socket can receive.
 * 
 * http://localhost:8080/aleph2_example_webapp_kafka/rest/kafka_write/mytopic/test_message
 * 
 * @author Burch
 *
 */
@Path("/rest/kafka_write")
public class KafkaWriteResource {
	private static final Logger _logger = LogManager.getLogger();
	protected final IServiceContext _context;
	protected final ICoreDistributedServices _core_dist;
	
	@Inject
	public KafkaWriteResource(IServiceContext context) {
		_context = context;
		_core_dist = _context.getService(ICoreDistributedServices.class, Optional.empty()).get();
	}
	
	@GET
	@Path("/write/{topic}/{message}")
	@Produces(MediaType.TEXT_PLAIN)
	public String streamKafkaTopic(@PathParam("topic") String topic, @PathParam("message") String message) {			
		_logger.debug("producing topic: " + topic + " message: " + message);
		if ( topic != null && !topic.isEmpty() && message != null && !message.isEmpty()) {
			_core_dist.produce(topic, message);
			return "success";
		} else {
			return "bad params";
		}
	}
}
