package com.ikanow.aleph2.web_login;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;

import com.google.inject.Inject;

public class WebEnvironment extends IniWebEnvironment{

    protected void configure() {

        this.objects.clear();

        WebSecurityManager securityManager = createWebSecurityManager();
        setWebSecurityManager(securityManager);

        FilterChainResolver resolver = createFilterChainResolver();
        if (resolver != null) {
            setFilterChainResolver(resolver);
        }
    }

	@Override
	protected WebSecurityManager createWebSecurityManager() {
		WebSecurityManager webSecurityManager = super.createWebSecurityManager();
		if(webSecurityManager instanceof DefaultWebSecurityManager){
			DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager)webSecurityManager;
			Collection<Realm> realms = getRealms();
			securityManager.setRealms(realms);
		}
		return webSecurityManager;
	}

	//TODO get from injector
	private Collection<Realm> getRealms() {
		Collection<Realm> realms = new ArrayList();
		return realms;
	}

}
