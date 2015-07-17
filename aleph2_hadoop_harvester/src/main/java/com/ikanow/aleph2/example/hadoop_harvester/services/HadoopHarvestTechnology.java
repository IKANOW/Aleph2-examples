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
package com.ikanow.aleph2.example.hadoop_harvester.services;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestTechnologyModule;
import com.ikanow.aleph2.data_model.objects.data_import.BucketDiffBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;

/** Harvest technology for launching jobs that use Hadoop to generate JSON 
 * @author Alex
 */
public class HadoopHarvestTechnology implements IHarvestTechnologyModule {

	@Override
	public void onInit(IHarvestContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canRunOnThisNode(DataBucketBean bucket,
			IHarvestContext context) {
		// TODO Auto-generated method stub
		return false;
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
