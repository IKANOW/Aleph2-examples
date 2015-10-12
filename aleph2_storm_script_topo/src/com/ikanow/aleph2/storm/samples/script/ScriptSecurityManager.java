package com.ikanow.aleph2.storm.samples.script;

import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.ikanow.aleph2.data_model.interfaces.shared_services.ISecurityService;

public class ScriptSecurityManager extends NoSecurityManager {

	protected ISecurityService securityService;

	public ScriptSecurityManager(ISecurityService securityService){
		this.securityService = securityService;
		securityService.enableJvmSecurityManager(true);
	}
	@Override
	public CompiledScript compile(ScriptEngine scriptEngine, String script) throws ScriptException {
		// TODO check roles for compile here?
		return super.compile(scriptEngine, script);
	}

	@Override
	public void setSecureFlag(boolean b) {
		securityService.enableJvmSecurity(b);
	}

}
