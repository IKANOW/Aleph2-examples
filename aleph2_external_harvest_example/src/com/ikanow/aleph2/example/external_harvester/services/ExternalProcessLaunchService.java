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
package com.ikanow.aleph2.example.external_harvester.services;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.IManagementDbService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IManagementCrudService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean.MasterEnrichmentType;
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.example.external_harvester.data_model.GlobalConfigBean;
import com.ikanow.aleph2.example.external_harvester.data_model.ProcessInfoBean;

public class ExternalProcessLaunchService {
	final static Logger _logger = LogManager.getLogger();

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, JsonProcessingException, IOException, InterruptedException, ExecutionException {
	
		// Get the context (unused here)
		
		final IHarvestContext context = ContextUtils.getHarvestContext(args[0]);

		_logger.info("Launched context, eg bucket status = : " + BeanTemplateUtils.toJson(context.getBucketStatus(Optional.empty()).get()));
		_logger.info("Retrieved bucket from CON: " + BeanTemplateUtils.toJson(context.getBucket().get()));
		
		// Get the bucket (unused here)
		
		final ObjectMapper mapper = BeanTemplateUtils.configureMapper(Optional.empty());
		final DataBucketBean bucket = BeanTemplateUtils.from(mapper.readTree(args[1].getBytes()), DataBucketBean.class).get();

		_logger.info("Retrieved bucket from CLI: " + BeanTemplateUtils.toJson(bucket));
		
		// Check that joins the cluster if I request the data bucket store
		//context.getService(IManagementDbService.class, Optional.of("core_management_db")).get().getDataBucketStore();
		//(But not if it's in read only mode)
		final IManagementCrudService<DataBucketBean> bucket_service = context.getService(IManagementDbService.class, IManagementDbService.CORE_MANAGEMENT_DB).get().readOnlyVersion().getDataBucketStore();
		_logger.info("Getting Management DB and reading number of buckets = " + bucket_service.countObjects().get().intValue());
		
		// Demonstration of accessing (read only) library state information:
		
		final Tuple2<SharedLibraryBean, Optional<GlobalConfigBean>> lib_config = ExternalProcessHarvestTechnology.getConfig(context);
		_logger.info("Retrieved library configuration: " + lib_config._2().map(g -> BeanTemplateUtils.toJson(g).toString()).orElse("(no config)"));
		
		final IManagementDbService core_db = context.getService(IManagementDbService.class, IManagementDbService.CORE_MANAGEMENT_DB).get();		
		final ICrudService<ProcessInfoBean> pid_crud = core_db.getPerLibraryState(ProcessInfoBean.class, lib_config._1(), ProcessInfoBean.PID_COLLECTION_NAME);

		lib_config._2().ifPresent(gc -> {
			if (gc.store_pids_in_db())
				pid_crud.getObjectsBySpec(CrudUtils.allOf(ProcessInfoBean.class).when(ProcessInfoBean::bucket_name, bucket.full_name()))
							.thenAccept(cursor -> {
								String pids = StreamSupport.stream(cursor.spliterator(), false).map(c -> c._id()).collect(Collectors.joining(","));
								_logger.info("Pids/hostnames for this bucket: " + pids);
							})
							.exceptionally(err -> {
								_logger.error("Failed to get bucket pids", err);
								return null;
							});
		});
		
		
		// Just run for 10 minutes as an experiment
		for (int i = 0; i < 60; ++i) {
			// Example of promoting data to next stage
			if ((MasterEnrichmentType.streaming == bucket.master_enrichment_type())
					|| (MasterEnrichmentType.streaming_and_batch == bucket.master_enrichment_type())) {
				// Send an object to kafka
				final JsonNode json = mapper.createObjectNode().put("@timestamp", new Date().getTime()).put("test_str", "test" + i).put("test_int", i);
				_logger.info("Sending object to kafka: " + json);
				context.sendObjectToStreamingPipeline(Optional.empty(), json);
			}			
			_logger.info("(sleeping: " + i + ")");
			try { Thread.sleep(10L*1000L); } catch (Exception e) {}
		}
	}
}
