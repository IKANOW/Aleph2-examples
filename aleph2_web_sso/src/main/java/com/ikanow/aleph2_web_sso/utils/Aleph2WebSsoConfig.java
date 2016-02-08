package com.ikanow.aleph2_web_sso.utils;

public class Aleph2WebSsoConfig {

	public static Aleph2WebSsoConfig _instance = null;
	
	public static Aleph2WebSsoConfig getInstance() {
		if(_instance == null){
			_instance = new Aleph2WebSsoConfig();
		}
		return _instance;
	}
	
	private static void setInstance(Aleph2WebSsoConfig instance){
		_instance = instance;
	}
	public Aleph2WebSsoConfig(){
		Aleph2WebSsoConfig.setInstance(this);
	}
	
	private String logoutUrl;

	private boolean createUser=false;
	private boolean useProfile=false;

	public boolean isUseProfile() {
		return useProfile;
	}

	public void setUseProfile(boolean useProfile) {
		this.useProfile = useProfile;
	}

	public String getLogoutUrl() {
		return logoutUrl;
	}

	public void setLogoutUrl(String logoutUrl) {
		this.logoutUrl = logoutUrl;
	}

	public boolean isCreateUser() {
		return createUser;
	}

	public void setCreateUser(boolean createUser) {
		this.createUser = createUser;
	}
	
	
	
	
}
