package com.ikanow.aleph2.storm.topology;

import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;

import backtype.storm.generated.StormTopology;

public interface IStormHarvestTopology {
	/**
	 * Return back the topology you want to run.
	 * 
	 * @return
	 */
	StormTopology getStormTopology(String harvest_context_signature, String job_name, DataBucketBean bucket_bean);
}
