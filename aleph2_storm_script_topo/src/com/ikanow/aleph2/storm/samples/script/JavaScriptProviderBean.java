package com.ikanow.aleph2.storm.samples.script;

import java.io.Serializable;
import java.util.List;

public class JavaScriptProviderBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2804240910738839906L;
	List<String> scriptlets = null;
	
	public List<String> getScriptlets() {
		return scriptlets;
	}
	public void setScriptlets(List<String> scriptlets) {
		this.scriptlets = scriptlets;
	}
	public String getGlobalScript() {
		return globalScript;
	}
	public void setGlobalScript(String globalScript) {
		this.globalScript = globalScript;
	}
	public String getUserScript() {
		return userScript;
	}
	public void setUserScript(String userScript) {
		this.userScript = userScript;
	}
	String globalScript = null;
	String userScript = null;
	
}
