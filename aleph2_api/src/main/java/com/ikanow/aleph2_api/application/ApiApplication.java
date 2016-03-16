package com.ikanow.aleph2_api.application;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ikanow.aleph2.web_utils.GuiceContextListener;
import com.ikanow.aleph2_api.rest.BasicResource;

/**
 * Application tells the servlet what all resources to server up (what endpoints can be hit)
 * 
 * @author Burch, jfreydank
 *
 */
public class ApiApplication extends Application {
	private static final Logger _logger = LogManager.getLogger();
	private Set<Object> singletons = new HashSet<Object>();
	
	public ApiApplication() {
		//TODO if there is a nice way of just adding everything in certain packages, I should use that rather than individual spec'ing every resource
		_logger.debug("Initializing ExampleApplication, creating resources");		
		singletons.add(GuiceContextListener.injector.getInstance(BasicResource.class));
	}
	
	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
