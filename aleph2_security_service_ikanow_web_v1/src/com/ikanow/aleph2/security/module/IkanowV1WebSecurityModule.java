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

import com.google.inject.multibindings.Multibinder;
import com.ikanow.aleph2.security.interfaces.IRoleProvider;
import com.ikanow.aleph2.security.service.AccountStatusCredentialsMatcher;
import com.ikanow.aleph2.security.service.IModificationChecker;
import com.ikanow.aleph2.security.service.IkanowV1AdminRoleProvider;
import com.ikanow.aleph2.security.service.IkanowV1DataGroupRoleProvider;
import com.ikanow.aleph2.security.service.IkanowV1DataModificationChecker;
import com.ikanow.aleph2.security.service.IkanowV1Realm;
import com.ikanow.aleph2.security.service.IkanowV1UserGroupRoleProvider;

public class IkanowV1WebSecurityModule extends ShiroWebModule {
	private static final Logger logger = LogManager.getLogger(IkanowV1WebSecurityModule.class);

	
	public IkanowV1WebSecurityModule(ServletContext sc) {
        super(sc);
    }

	@Override
	protected void configureShiroWeb() {
		bindCredentialsMatcher();
		bindAuthProviders();
		bindRoleProviders();
    	bindRealms();
    	bindCacheManager();
    	bindMisc();

    	addFilterChains();
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
     	

	}
	
	
	
	protected void bindMisc() {
		// do not just bind the implementation class,e.g. IkanowV1DataModificationChecker. This somehow creates an error about EhCachemanager already created.
		bind(IModificationChecker.class).to(IkanowV1DataModificationChecker.class).asEagerSingleton();
		expose(IModificationChecker.class);
	}

	protected void bindRealms() {
		bindRealm().to(IkanowV1Realm.class).asEagerSingleton();		
	}

	/** 
     * Place holder to overwrite. 
     */
    protected void bindAuthProviders(){
    	logger.debug("bindAuthProviders -placeholder, override in sub-modules");
    }
	

    protected void bindRoleProviders(){
		Multibinder<IRoleProvider> uriBinder = Multibinder.newSetBinder(binder(), IRoleProvider.class);
	    uriBinder.addBinding().to(IkanowV1AdminRoleProvider.class);
	    uriBinder.addBinding().to(IkanowV1UserGroupRoleProvider.class);
	    uriBinder.addBinding().to(IkanowV1DataGroupRoleProvider.class);
    }
	
	protected void bindCredentialsMatcher() {
 		bind(CredentialsMatcher.class).to(AccountStatusCredentialsMatcher.class);
	}

    protected void bindCacheManager() {
    	//CoreEhCacheManager.getInstance().bindCacheManager(binder());
    	//bind(CacheManager.class).toInstance(CoreEhCacheManager.getCacheManager());		
    	//expose(CacheManager.class);

	}

}
