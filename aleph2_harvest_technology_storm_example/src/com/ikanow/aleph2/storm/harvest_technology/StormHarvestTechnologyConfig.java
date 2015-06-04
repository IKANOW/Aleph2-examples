package com.ikanow.aleph2.storm.harvest_technology;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StormHarvestTechnologyConfig {
	//either this debug version I started with that builds a topology from my premade pieces
	private String source_type;
	private String source_url;
	private String source_parser;
	private String source_output;
	
	//or provide your own topology
	private String topology_class;
	
	public StormHarvestTechnologyConfig(@NonNull String source_type, @NonNull String source_url, @NonNull String source_parser, @Nullable String source_output) {
		this.source_type = source_type;
		this.source_url = source_url;
		this.source_parser = source_parser;
		this.source_output = source_output;
	}
	
	public StormHarvestTechnologyConfig(@NonNull String topology_class) {
		this.topology_class = topology_class;
	}
	
	public String getSource_type() {
		return source_type;
	}
	
	public String getSource_url() {
		return source_url;
	}
	
	public String getSource_parser() {
		return source_parser;
	}
	
	public String getTopology_class() {
		return topology_class;
	}

	public String getSource_output() {
		return source_output;
	}
}
