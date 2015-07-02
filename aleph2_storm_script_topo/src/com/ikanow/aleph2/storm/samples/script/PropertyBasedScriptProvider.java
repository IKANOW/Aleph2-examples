package com.ikanow.aleph2.storm.samples.script;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertyBasedScriptProvider implements IScriptProvider {
	private static final Logger logger = LogManager.getLogger(PropertyBasedScriptProvider.class);

	protected Properties properties;
	protected List<String> scriptlets =  new ArrayList<String>(); 	
	protected String globalScript = null; 

	protected static String GLOBAL = "global";
	public PropertyBasedScriptProvider(String propertiesFileName){
		loadProperties(propertiesFileName);
		init();
	}
	

	protected void init(){
		for (Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();
			if (entry.getKey().toString().equalsIgnoreCase(GLOBAL)){
					globalScript = loadScriptFromResource((String)entry.getValue());
			} // if
			else{
				String scriptlet = loadScriptFromResource((String)entry.getValue());
				scriptlets.add(scriptlet);
			}
		} // for		
	}
	
	@Override
	public List<String> getScriptlets() {
		return scriptlets;
	}

	@Override
	public String getGlobalScript() {
		return globalScript;
	}
	
	protected void loadProperties(String propertiesFileName) {
			this.properties = new Properties();

			try {
				InputStream inStream = PropertyBasedScriptProvider.class.getResourceAsStream(propertiesFileName);
				if (inStream == null) {
					// second try
					inStream = new FileInputStream(propertiesFileName);
				}
				if (inStream != null) {
					properties.load(inStream);						
				}

			} catch (Throwable t) {
				logger.error("Caught exception loading properties:", t);
			}
	}

	protected static String loadScriptFromResource(String resourcePathToJSFile) {
		String script = null;
		if(resourcePathToJSFile!=null){
			try {
				InputStream inStream = PropertyBasedScriptProvider.class.getResourceAsStream(resourcePathToJSFile);
				if (inStream == null) {
					// second try
					inStream = new FileInputStream(resourcePathToJSFile);
				}
				if (inStream != null) {
					script = IOUtils.toString(inStream, "UTF-8");
				}

			} catch (Throwable t) {
				logger.error("Caught exception loading resource:", t);
			}
		}
		return script;
	}


}
