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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;

import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.ikanow.aleph2.data_model.objects.shared.AuthorizationBean;
import com.ikanow.aleph2.security.interfaces.IAuthProvider;
import com.ikanow.aleph2.security.interfaces.IRoleProvider;
import com.ikanow.aleph2.security.service.MapAuthProvider;
import com.ikanow.aleph2.security.service.MapRoleProvider;
import com.ikanow.aleph2.security.shiro.IkanowV2Realm;
import com.ikanow.aleph2.security.shiro.MongoDbSessionDao;
import com.ikanow.aleph2.security.shiro.MongoWebSessionManager;

public class IkanowMockLoginWebSecurityModule extends ShiroWebModule {
	private static final Logger logger = LogManager.getLogger(IkanowV2WebSecurityModule.class);

	protected static Map<String, Set<String>> rolesMap = new HashMap<String, Set<String>>();
	protected static Map<String, Set<String>> permissionsMap = new HashMap<String, Set<String>>();
	protected static Map<String, AuthorizationBean> authMap = new HashMap<String, AuthorizationBean>();
	protected static MapRoleProvider roleProvider =  new MapRoleProvider(rolesMap, permissionsMap);
	protected static MapAuthProvider authProvider = new MapAuthProvider(authMap);

	static{
		AuthorizationBean ab1 = new AuthorizationBean("admin");
		ab1.setCredentials("admin123");
		authMap.put("admin",ab1);
		AuthorizationBean ab2 = new AuthorizationBean("user");
		ab2.setCredentials("user123");
		authMap.put("user",ab2);
		AuthorizationBean ab3 = new AuthorizationBean("testUser");
		ab3.setCredentials("testUser123");
		authMap.put("testUser",ab3);
		AuthorizationBean ab4 = new AuthorizationBean("system");
		ab4.setCredentials("system123");
		authMap.put("system",ab4);
		
		permissionsMap.put("admin", new HashSet<String>(Arrays.asList("*")));
		permissionsMap.put("user", new HashSet<String>(Arrays.asList("permission1","permission2","permission3","read:tmp:data:misc","package:*","permission:*","DataBucketBean:read:bucketId1","community:*:communityId1")));
		permissionsMap.put("testUser", new HashSet<String>(Arrays.asList("t1","t2","t3")));

		rolesMap.put("admin", new HashSet<String>(Arrays.asList("admin")));
		rolesMap.put("user", new HashSet<String>(Arrays.asList("user")));
		rolesMap.put("testUser", new HashSet<String>(Arrays.asList("testUser")));
	}

	public IkanowMockLoginWebSecurityModule(ServletContext sc) {
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
		bind(SessionDAO.class).to(MongoDbSessionDao.class);
	}

	@SuppressWarnings("unchecked")
	public void addFilterChains(){
		 bindConstant().annotatedWith(Names.named("shiro.loginUrl")).to("/login.jsp");
     	addFilterChain("/login.jsp", AUTHC);
     	addFilterChain("/logout", LOGOUT);
        addFilterChain("/rest/**", AUTHC);
	}
	
	
	
	protected void bindMisc() {
	}

	protected void bindRealms() {
		bindRealm().to(IkanowV2Realm.class).asEagerSingleton();		
	}

	/** 
     * Place holder to overwrite. 
     */
    protected void bindAuthProviders(){
    	logger.debug("bindAuthProviders -placeholder, override in sub-modules");
 		bind(IAuthProvider.class).toInstance(authProvider);
    }
	

    protected void bindRoleProviders(){
		Multibinder<IRoleProvider> uriBinder = Multibinder.newSetBinder(binder(), IRoleProvider.class);					
	    uriBinder.addBinding().toInstance(roleProvider);

    }
	
	protected void bindCredentialsMatcher() {
 		bind(CredentialsMatcher.class).to(SimpleCredentialsMatcher.class);
	}

	@Override
	protected void bindSessionManager(AnnotatedBindingBuilder<SessionManager> bind) {
		bindConstant().annotatedWith(Names.named("shiro.globalSessionTimeout")).to(5000L);
		bind(Cookie.class).toInstance(new SimpleCookie("aleph2_session"));		
		bind.to(MongoWebSessionManager.class).asEagerSingleton();
	}
}
