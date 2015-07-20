/******************************************************************************
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
 ******************************************************************************/
package com.ikanow.aleph2.example.flume_harvester.services;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeUtils;

/** Flume harvester entry points
 * @author Alex
 */
public class FlumeHarvestTechnology implements IHarvestTechnologyModule {

	@Override
	public void onInit(IHarvestContext context) {
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		
		//TODO: check if globals exist
		
		return false;
	}

	protected void startAgent(final DataBucketBean bucket, final FlumeBucketConfigBean config, final IHarvestContext context) throws IOException {
		
		// Is morphlines configured?
		final Optional<String> morphlines_file = Optional.ofNullable(config.morphlines_config())
				.flatMap(Lambdas.wrap_u(m_cfg -> {
					final File tmp_morph = File.createTempFile("aleph2_flume", ".config");
					final Optional<String> morph_cfg = FlumeUtils.createMorphlinesConfig(config);
					return morph_cfg.map(Lambdas.wrap_u(mcfg -> {
						FileUtils.writeStringToFile(tmp_morph, mcfg);	
						return tmp_morph.toString();
					}));
					
				}));		
		File tmp_flume = File.createTempFile("aleph2_flume", ".config");
		final String agent_name = FlumeUtils.getConfigName(bucket.full_name(), Optional.empty());
		final String flume_config = FlumeUtils.createFlumeConfig(agent_name, 1, config, 
													context.getHarvestContextSignature(Optional.empty(), Optional.empty()), 
													morphlines_file);
		FileUtils.writeStringToFile(tmp_flume, flume_config);
		
		//TODO copy tmp_flume to some bucket unique name in location
	}
	
	protected void stopAgent() {
		//TODO delete some bucket unique name in location
	}
	
	@Override
	public CompletableFuture<BasicMessageBean> onNewSource(
			DataBucketBean new_bucket, IHarvestContext context, boolean enabled) {
		
		
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onUpdatedSource(
			DataBucketBean old_bucket, DataBucketBean new_bucket,
			boolean is_enabled, Optional<BucketDiffBean> diff,
			IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onSuspend(
			DataBucketBean to_suspend, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onResume(
			DataBucketBean to_resume, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPurge(DataBucketBean to_purge,
			IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDelete(
			DataBucketBean to_delete, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onDecommission(
			DataBucketBean to_decommission, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onPeriodicPoll(
			DataBucketBean polled_bucket, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onHarvestComplete(
			DataBucketBean completed_bucket, IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<BasicMessageBean> onTestSource(
			DataBucketBean test_bucket, ProcessingTestSpecBean test_spec,
			IHarvestContext context) {
		// TODO Auto-generated method stub
		return null;
	}

}
