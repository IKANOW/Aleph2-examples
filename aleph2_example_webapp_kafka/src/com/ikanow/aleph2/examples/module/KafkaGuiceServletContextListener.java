package com.ikanow.aleph2.examples.module;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import javax.servlet.ServletContextEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.examples.websocket.KafkaWebsocketResource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import fj.data.Either;

/**
 * Listener that sets up our guice injection.  Grabs the injector from aleph2 and puts it in a public field
 * so it can be used by the rest of this application (see ExampleApplication)
 * 
 * @author Burch
 *
 */
public class KafkaGuiceServletContextListener extends GuiceServletContextListener {
	private static final Logger _logger = LogManager.getLogger();
	public static Injector injector;
	private String config_path; 
	
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		config_path = servletContextEvent.getServletContext().getInitParameter("aleph2.config");
		super.contextInitialized(servletContextEvent);
	}
	
	@Override
	protected Injector getInjector() {
		//get the injector from the aleph2 framework		
		_logger.debug("Config path is set to: " + config_path);
		final File config_file = new File(config_path);
				
		try {
			File f = new File(".");
			_logger.debug("CURR PATH: " + f.getCanonicalPath());
			_logger.debug("full file path: " + config_file.getCanonicalPath());
			final Config config = ConfigFactory.parseFile(config_file);
			//I don't think I actually need to get the websocket, but init app doesn't seem to let me not load something up, I just want the injector
			//TODO we added a new way to start up an injector in ModuleUtils, need to try switching to that, we don't actaully need to get an instance of KafkaWebSocketResource here, we just need to do this to init the injector currently
			ModuleUtils.initializeApplication(Arrays.asList(new KafkaModule()), Optional.of(config), Either.left(KafkaWebsocketResource.class));
			//final KafkaWriteResource r2 = ModuleUtils.initializeApplication(Arrays.asList(new KafkaModule()), Optional.of(config), Either.left(KafkaWriteResource.class));
			injector = ModuleUtils.getAppInjector().get();
		} catch (Exception e) {
			_logger.error("error creating injector", e);
		}						
		
		return injector;
	}

}
