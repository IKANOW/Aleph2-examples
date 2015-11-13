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
package com.ikanow.aleph2.storm.topology;

import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;

import backtype.storm.generated.StormTopology;

/**
 * Any dev that wants to create their own topology needs a class that implements
 * this interface.  And then point to that class in the harvest_config of their sources.
 * 
 * @author Burch
 *
 */
public interface IStormHarvestTopology {
	/**
	 * Return back the topology you want to run.
	 * 
	 * @return
	 */
	StormTopology getStormTopology(String harvest_context_signature, String job_name, DataBucketBean bucket_bean);
}
