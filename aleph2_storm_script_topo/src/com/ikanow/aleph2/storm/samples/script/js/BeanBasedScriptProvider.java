package com.ikanow.aleph2.storm.samples.script.js;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.ikanow.aleph2.storm.samples.script.IScriptProvider;
import com.ikanow.aleph2.storm.samples.script.JavaScriptProviderBean;

public class BeanBasedScriptProvider implements IScriptProvider,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8831305243222180167L;
	private JavaScriptProviderBean providerBean;

	private List<String> scriptlets = new ArrayList<String>();
	
	public BeanBasedScriptProvider(JavaScriptProviderBean providerBean){
		this.providerBean = providerBean;
		if(providerBean.getScriptlets()!=null){
			scriptlets.addAll(providerBean.getScriptlets());
		}
	}
	
	@Override
	public List<String> getScriptlets() {
		
		return scriptlets;
	}

	@Override
	public String getGlobalScript() {
		return providerBean.getGlobalScript();
	}

}
