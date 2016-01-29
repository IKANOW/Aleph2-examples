package com.ikanow.aleph2.web_utils;

import javax.servlet.ServletContext;

import com.google.inject.AbstractModule;

public class DefaultWebModule extends AbstractModule {

	
	protected ServletContext servletContext;

	public ServletContext getServletContext() {
		return servletContext;
	}

	public DefaultWebModule(ServletContext sc) {
		this.servletContext = sc;
    }

	@Override
	protected void configure() {
		// TODO Auto-generated method stub

	}

	
}
