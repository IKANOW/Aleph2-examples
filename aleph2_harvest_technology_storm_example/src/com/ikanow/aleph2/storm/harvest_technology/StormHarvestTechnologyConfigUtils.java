package com.ikanow.aleph2.storm.harvest_technology;

import java.util.Map;

import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;

public class StormHarvestTechnologyConfigUtils {
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
