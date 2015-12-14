package  com.ikanow.aleph2.web_utils;


import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import fj.data.Either;

/**
 * Listener that sets up our guice injection.  Grabs the injector from aleph2 and puts it in a public field
 * so it can be used by the rest of this application (see ExampleApplication)
 * 
 * @author Joern, derived from cburch
 *
 */
public class GuiceContextListener extends GuiceServletContextListener {
	
	public static String CONTEXT_PARAM_ALEPH2_CONFIG = "aleph2.config"; 
	public static String CONTEXT_PARAM_MODULE_CLASS = "aleph2.module_class"; 
	public static String CONTEXT_PARAM_APPLICATION_CLASS = "aleph2.application_class"; 
	private static final Logger logger = LogManager.getLogger();
	public static Injector injector;
	private String config_path; 
	private String moduleClassName; 
	private String applicationClassName;
	protected ServletContext servletContext; 
	
	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		this.servletContext  = servletContextEvent.getServletContext();
		config_path = servletContextEvent.getServletContext().getInitParameter(CONTEXT_PARAM_ALEPH2_CONFIG);
		this.moduleClassName = servletContextEvent.getServletContext().getInitParameter(CONTEXT_PARAM_MODULE_CLASS);
		this.applicationClassName = servletContextEvent.getServletContext().getInitParameter(CONTEXT_PARAM_APPLICATION_CLASS);
		super.contextInitialized(servletContextEvent);
	}
	
	@Override
	protected Injector getInjector() {
		//get the injector from the aleph2 framework		
		logger.debug("Config path is set to: " + config_path);
		final File config_file = new File(config_path);
				
		try {
			File f = new File(".");
			logger.debug("CURR PATH: " + f.getCanonicalPath());
			logger.debug("full file path: " + config_file.getCanonicalPath());
			Config config = null;
			// check if file exists otherwise try to load from classpath
			if(config_file.exists()){
				config = ConfigFactory.parseFile(config_file);	
			}
			else{
				config = ConfigFactory.parseResources(config_path);
			}
			this.getClass();
			// TODO check if instance of web module, then take different constructor
		   Class<?> moduleClass = Class.forName(moduleClassName!=null?moduleClassName:DefaultWebModule.class.getName()); 
		   Constructor<?> constructor = null;
		   try {
			   constructor = moduleClass.getConstructor(ServletContext.class);			
			} catch (Exception e) {
				logger.error("error getting constructor", e);
			}
		   
		   Module module = (constructor!=null) ? ((Module)constructor.newInstance(servletContext)): ((Module) moduleClass.newInstance());
			
			final Class<?> applicationClass =  applicationClassName!=null?Class.forName(applicationClassName):DefaultInjectorContainer.class;			
			ModuleUtils.initializeApplication(Arrays.asList(module), Optional.of(config), Either.left(applicationClass));
			
			injector = ModuleUtils.getAppInjector().get();
		} catch (Exception e) {
			logger.error("error creating injector", e);
		}						
		
		return injector;
	}

}