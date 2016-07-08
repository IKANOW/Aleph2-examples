package org.ldaptive;

import java.nio.charset.Charset;

public class SimpleCredential extends Credential {
	
	/** UTF-8 character set. */ 
	private static final Charset UTF8CHARSET = Charset.forName("UTF-8"); 
	
	/** Credential stored as String **/
	String bindPassword;

	public SimpleCredential(){
		super("");
	}

	public SimpleCredential(String password) {
		super(password);
		bindPassword=password;
	}
	
	public byte[] getBytes()
	{
		return bindPassword.getBytes(UTF8CHARSET);
	}

	public String getString() 
	{ 
		return bindPassword;
	}
	
	public void setBindPassword(String bindPassword) {
		this.bindPassword = bindPassword;
	}
}
