/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
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
package com.ikanow.aleph2_web_sso.utils;

public class Aleph2WebSsoConfig {

	public static Aleph2WebSsoConfig _instance = null;
	private String emailOid = "urn:oid:0.9.2342.19200300.100.1.3";
	private String uidOid = "urn:oid:0.9.2342.19200300.100.1.1";
	private String firstNameOid = "urn:oid:2.5.4.42";
	private String lastnameOid = "urn:oid:2.5.4.4";
	private String fullNameOid = "urn:oid:2.16.840.1.113730.3.1.241";
	private String phoneOid = " urn:oid:2.5.4.20";

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
	private boolean useProfile=true;
	private boolean useAttributes=true;


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

	public String getEmailOid() {
		return emailOid;
	}

	public void setEmailOid(String emailOid) {
		this.emailOid = emailOid;
	}

	public String getUidOid() {
		return uidOid;
	}

	public void setUidOid(String uidOid) {
		this.uidOid = uidOid;
	}

	public String getFirstNameOid() {
		return firstNameOid;
	}

	public void setFirstNameOid(String firstNameOid) {
		this.firstNameOid = firstNameOid;
	}

	public String getLastnameOid() {
		return lastnameOid;
	}

	public void setLastnameOid(String lastnameOid) {
		this.lastnameOid = lastnameOid;
	}

	public String getFullNameOid() {
		return fullNameOid;
	}

	public void setFullNameOid(String fullNameOid) {
		this.fullNameOid = fullNameOid;
	}


	public boolean isUseAttributes() {
		return useAttributes;
	}

	public void setUseAttributes(boolean useAttributes) {
		this.useAttributes = useAttributes;
	}

	public String getPhoneOid() {
		return phoneOid;
	}

	public void setPhoneOid(String phoneOid) {
		this.phoneOid = phoneOid;
	}
	
	
	
	
}
