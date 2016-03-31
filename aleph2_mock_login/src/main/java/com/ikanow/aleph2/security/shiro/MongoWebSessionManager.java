package com.ikanow.aleph2.security.shiro;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

import com.google.inject.Inject;



public class MongoWebSessionManager extends DefaultWebSessionManager {
	protected static Logger logger = LogManager.getLogger(MongoWebSessionManager.class);

	@Inject
	public MongoWebSessionManager(MongoDbSessionDao mongoSessionDao){
		super();
		logger.debug("MongoWebSessionManager created.");
		setSessionDAO(mongoSessionDao);
	}

}
