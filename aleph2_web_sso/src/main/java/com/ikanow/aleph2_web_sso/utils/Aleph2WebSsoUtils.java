package com.ikanow.aleph2_web_sso.utils;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.saml.profile.SAML2Profile;

public class Aleph2WebSsoUtils {

	private static final Logger logger = LogManager.getLogger(Aleph2WebSsoUtils.class);

	public static String EMAIL_OID = "urn:oid:0.9.2342.19200300.100.1.3";
	public static String UID_OID = "urn:oid:0.9.2342.19200300.100.1.1";
	public static String FIRST_NAME_OID = "urn:oid:2.5.4.42";
	public static String LAST_NAME_OID = "urn:oid:2.5.4.4";
	public static String FULL_NAME_OID = "urn:oid:2.16.840.1.113730.3.1.241";
	public static String PHONE_OID = "urn:oid:2.16.840.1.113730.3.1.241";
			
	@SuppressWarnings("rawtypes")
	public static String extractAttribute(SAML2Profile sp, String attributeOid){
		String attribute = null;
		try {
			Object rawAttribute = sp.getAttribute(attributeOid);
			if(rawAttribute instanceof ArrayList){
				ArrayList listAttributes = (ArrayList)rawAttribute;
				if(listAttributes.size()>0){
					attribute = (String)listAttributes.get(0);
				}
			}			
		} catch (Exception e) {
			logger.error("extractAttribute caught exception extracting "+attributeOid,e);
		}
		return attribute;
	}
	
}
