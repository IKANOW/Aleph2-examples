package com.ikanow.aleph2.web_login;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;

import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.ikanow.aleph2.security.module.CoreSecurityModule;


public class WebEnvironment extends IniWebEnvironment{

	private static final Logger logger = LogManager.getLogger();


	protected Collection<Realm> realms = null;

    protected void configure() {
    	
		try {
			Injector injector = ModuleUtils.getAppInjector().get();			
			this.realms = injector.getInstance(CoreSecurityModule.realmCollectionKey());
			//logger.debug("Realm:"+realms);
		} catch (Throwable e) {
			logger.error("Caught exception getting app injector:",e);
		}

    	super.configure();
        
    }

	@Override
	protected WebSecurityManager createWebSecurityManager() {
		WebSecurityManager webSecurityManager = super.createWebSecurityManager();
		if(webSecurityManager instanceof DefaultWebSecurityManager){
			DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager)webSecurityManager;
			securityManager.setRealms(realms);
		}
		return webSecurityManager;
	}

	
	
	
}
