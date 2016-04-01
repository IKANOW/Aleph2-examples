package com.ikanow.aleph2.security.shiro;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

import com.google.inject.Inject;



public class MongoWebSessionManager extends DefaultWebSessionManager {
	protected static Logger logger = LogManager.getLogger(MongoWebSessionManager.class);
	
	public static String COOKIE_NAME = "ALEPH2_SESSION";
	@Inject
	public MongoWebSessionManager(MongoDbSessionDao mongoSessionDao){
		logger.debug("MongoWebSessionManager created.");
		setSessionDAO(mongoSessionDao);
        Cookie cookie = new SimpleCookie(COOKIE_NAME);
        cookie.setHttpOnly(true); //more secure, protects against XSS attacks
        cookie.setPath("/");
        long sessionTimeout = getGlobalSessionTimeout();
        if(sessionTimeout>0){
        	cookie.setMaxAge((int)(sessionTimeout/1000));
        }
        setSessionIdCookie(cookie);
        setSessionIdCookieEnabled(true);
	}

}
