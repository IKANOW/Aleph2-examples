package com.ikanow.aleph2.examples.module;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.AbstractModule;

/**
 * Module class to handle any bindings we want to make in our local app.
 * 
 * @author Burch
 *
 */
public class KafkaModule extends AbstractModule {	
	private static final Logger _logger = LogManager.getLogger();
	
	@Override
	protected void configure() {
		_logger.debug("configuring guice in KafkaModule");
		//add any additional bindings we want for our app here
	}
}
