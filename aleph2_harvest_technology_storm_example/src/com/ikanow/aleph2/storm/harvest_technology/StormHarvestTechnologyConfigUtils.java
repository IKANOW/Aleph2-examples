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

import java.util.Map;

import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;

/**
 * Helper class to parse the config
 * 
 * @author Burch
 *
 */
public class StormHarvestTechnologyConfigUtils {	
	/**
	 * Helper function to parse a HarvestControlMetadataBean.  Expects there to be a config object and it
	 * to contain either "IStormHarvestTopologyClass=something
	 * 
	 * or
	 * 
	 * source_type=something
	 * source_url=something
	 * source_parser=something
	 * 
	 * 
	 * @param harvest_config
	 * @return a StormHarvestTechnologyConfig object with the config fields pushed in
	 */
	public static StormHarvestTechnologyConfig parseHarvestConfig(HarvestControlMetadataBean harvest_config) {
		Map<String, Object> config = harvest_config.config();
		//TODO create some consts to reference in StormHarvestTechnologyConfig
		if ( config.containsKey("source_type") &&
				config.containsKey("source_url") &&
				config.containsKey("source_parser")) {
			return new StormHarvestTechnologyConfig((String)config.get("source_type"), (String)config.get("source_url"), (String)config.get("source_parser"), (String)config.get("source_output"));
		} else if ( config.containsKey("IStormHarvestTopologyClass")) {
			return new StormHarvestTechnologyConfig((String)config.get("IStormHarvestTopologyClass"));
		}
		else {
			return null;
		}		
	}
}
