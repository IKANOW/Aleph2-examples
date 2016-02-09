package com.ikanow.aleph2_web_sso.utils;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.saml.profile.SAML2Profile;

public class Aleph2WebSsoUtils {

	private static final Logger logger = LogManager.getLogger(Aleph2WebSsoUtils.class);

			
	@SuppressWarnings("rawtypes")
	public static String extractAttribute(SAML2Profile sp, String attributeOid){
		String attribute = null;
		if(sp!=null){
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
		}
		return attribute;
	}
	
}
