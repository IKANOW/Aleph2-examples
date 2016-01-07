package com.ikanow.aleph2.harvest.script.data_model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScriptHarvesterBucketConfigBean {

	//jackson ctor
	protected ScriptHarvesterBucketConfigBean() {}
	
	public String script() { return Optional.ofNullable(script).orElse(""); }
	public String local_script_url() { return Optional.ofNullable(local_script_url).orElse(""); }
	public String resource_name() { return Optional.ofNullable(resource_name).orElse(""); }
	public Map<String, String> args() { return Optional.ofNullable(args).orElse(new HashMap<String,String>()); }
	public List<String> required_assets() { return Optional.ofNullable(required_assets).orElse(new ArrayList<String>());}
	
	private String script;
	private String local_script_url;
	private String resource_name;
	private Map<String, String> args;
	private List<String> required_assets;
}
