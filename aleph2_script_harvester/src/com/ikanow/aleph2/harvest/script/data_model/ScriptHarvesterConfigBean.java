package com.ikanow.aleph2.harvest.script.data_model;

import java.util.Optional;

public class ScriptHarvesterConfigBean {
	
	//jackson ctor
	protected ScriptHarvesterConfigBean(){}
	
	public static String WORKING_DIR = System.getProperty("java.io.tmpdir");
	
	public String working_dir() { return Optional.ofNullable(working_dir).orElse(WORKING_DIR); }
	
	private String working_dir;
}
