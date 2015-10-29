package com.ikanow.aleph2.examples.application;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ikanow.aleph2.examples.module.KafkaGuiceServletContextListener;
import com.ikanow.aleph2.examples.rest.BasicResource;
import com.ikanow.aleph2.examples.rest.KafkaWriteResource;
import com.ikanow.aleph2.examples.rest.StreamResource;
import com.ikanow.aleph2.examples.websocket.KafkaWebsocketResource;
import com.ikanow.aleph2.examples.websocket.WebsocketResource;

/**
 * Application tells the servlet what all resources to server up (what endpoints can be hit)
 * 
 * @author Burch
 *
 */
public class ExampleApplication extends Application {
	private static final Logger _logger = LogManager.getLogger();
	private Set<Object> singletons = new HashSet<Object>();
	
	public ExampleApplication() {
		//TODO if there is a nice way of just adding everything in certain packages, I should use that rather than individual spec'ing every resource
		_logger.debug("Initializing ExampleApplication, creating resources");		
		singletons.add(KafkaGuiceServletContextListener.injector.getInstance(BasicResource.class));
		singletons.add(KafkaGuiceServletContextListener.injector.getInstance(StreamResource.class));
		singletons.add(KafkaGuiceServletContextListener.injector.getInstance(KafkaWriteResource.class));
		singletons.add(KafkaGuiceServletContextListener.injector.getInstance(WebsocketResource.class));
		singletons.add(KafkaGuiceServletContextListener.injector.getInstance(KafkaWebsocketResource.class));
	}
	
	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
