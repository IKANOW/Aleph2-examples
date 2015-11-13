/*******************************************************************************
 * Copyright 2015, The IKANOW Open Source Project.
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
package com.ikanow.aleph2.storm.harvest_technology;

/**
 * A wrapper for the example config you can submit in the databucketbean.
 * 
 * This wrapper accepts 2 versions, a preconfigured type/url/parser/output that
 * will build out a topology based on your inputs.
 * 
 * or
 * 
 * The fully customizable topology_class that allows the dev to submit whatever topology they like.
 * 
 * @author Burch
 *
 */
public class StormHarvestTechnologyConfig {
	//either this debug version I started with that builds a topology from my premade pieces
	private String source_type;
	private String source_url;
	private String source_parser;
	private String source_output;
	
	//or provide your own topology
	private String topology_class;
	
	public StormHarvestTechnologyConfig( String source_type, String source_url, String source_parser, String source_output) {
		this.source_type = source_type;
		this.source_url = source_url;
		this.source_parser = source_parser;
		this.source_output = source_output;
	}
	
	public StormHarvestTechnologyConfig(String topology_class) {
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
