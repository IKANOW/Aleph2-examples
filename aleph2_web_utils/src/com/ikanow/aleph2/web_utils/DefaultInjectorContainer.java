package com.ikanow.aleph2.web_utils;

import com.google.inject.Inject;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;

public class DefaultInjectorContainer {

	
	@Inject
	private IServiceContext serviceContext;

	public void setServiceContext(IServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	public IServiceContext getServiceContext() {
		return serviceContext;
	}

	public DefaultInjectorContainer(){
		
	}
	
}
