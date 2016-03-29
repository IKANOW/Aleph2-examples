/*******************************************************************************
 * Copyright 2015, The IKANOW Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ikanow.aleph2.security.module;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.ikanow.aleph2.security.interfaces.IRoleProvider;
import com.ikanow.aleph2.security.service.IkanowV2MongoRoleProvider;
import com.ikanow.aleph2.security.shiro.ActiveSessionCredentialMatcher;
import com.ikanow.aleph2.security.shiro.ActiveSessionRealm;
import com.ikanow.aleph2.security.shiro.MongoDbSessionDao;

import org.apache.shiro.session.mgt.eis.SessionDAO;

public class IkanowMocklLoginWebSecurityModule extends ShiroWebModule {
	private static final Logger logger = LogManager.getLogger(IkanowV2WebSecurityModule.class);

	
	public IkanowMocklLoginWebSecurityModule(ServletContext sc) {
        super(sc);
    }

	@Override
	protected void configureShiroWeb() {
		bindSessionDao();
		bindCredentialsMatcher();
		bindAuthProviders();
		bindRoleProviders();
    	bindRealms();
    	bindMisc();

    	addFilterChains();
	}
    

	protected void bindSessionDao()
	{
		bind(SessionDAO.class).to(MongoDbSessionDao.class).asEagerSingleton();
	
		//bind(SessionDAO.class).to(MySessionDao.class);
		//bind(SessionFactory.class).to(MySessionFactory.class);
		//bind(CacheManager.class).to(EhCacheManager.class);		
	}

	@SuppressWarnings("unchecked")
	public void addFilterChains(){
    	
    	/* samples
     	addFilterChain("/public/**", ANON);    	 
        addFilterChain("/stuff/allowed/**", AUTHC_BASIC, config(PERMS, "yes"));
        addFilterChain("/stuff/forbidden/**", AUTHC_BASIC, config(PERMS, "no"));
        addFilterChain("/**", AUTHC_BASIC);
        */
     	addFilterChain("/login.jsp", AUTHC);
     	addFilterChain("/logout", LOGOUT);
        addFilterChain("/rest/**", AUTHC, config(PERMS, "no"));
     	

	}
	
	
	
	protected void bindMisc() {
		// do not just bind the implementation class,e.g. IkanowV1DataModificationChecker. This somehow creates an error about EhCachemanager already created.
	//bind(IModificationChecker.class).to(IkanowV1DataModificationChecker.class).asEagerSingleton();
		//expose(IModificationChecker.class);
	}

	protected void bindRealms() {
		bindRealm().to(ActiveSessionRealm.class).asEagerSingleton();		
	}

	/** 
     * Place holder to overwrite. 
     */
    protected void bindAuthProviders(){
    	logger.debug("bindAuthProviders -placeholder, override in sub-modules");
    }
	

    protected void bindRoleProviders(){
		Multibinder<IRoleProvider> uriBinder = Multibinder.newSetBinder(binder(), IRoleProvider.class);
	    uriBinder.addBinding().to(IkanowV2MongoRoleProvider.class);
    }
	
	protected void bindCredentialsMatcher() {
 		bind(CredentialsMatcher.class).to(ActiveSessionCredentialMatcher.class);
	}

	@Override
	protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
		bind.to(DefaultWebSessionManager.class);
		bindConstant().annotatedWith(Names.named("shiro.globalSessionTimeout")).to(5000L);
		bind(Cookie.class).toInstance(new SimpleCookie("aleph2_session"));		
	}
}
