package com.ikanow.aleph2.storm.harvest_technology;

import org.checkerframework.checker.nullness.qual.NonNull;

import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;

public interface IStormController {
	void submitJob(@NonNull String job_name, @NonNull String input_jar_location, StormTopology topology) throws Exception;
	void stopJob(@NonNull String job_name) throws Exception;
	TopologyInfo getJobStats(@NonNull String job_name) throws Exception;
}
