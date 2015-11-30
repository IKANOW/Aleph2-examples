package com.ikanow.aleph2.web_utils;

import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;

public class DefaultInjectorContainer {

	
	private IServiceContext serviceContext;

	public IServiceContext getServiceContext() {
		return serviceContext;
	}

	public DefaultInjectorContainer(IServiceContext serviceContext){
		this.serviceContext = serviceContext;
	}
	
}
