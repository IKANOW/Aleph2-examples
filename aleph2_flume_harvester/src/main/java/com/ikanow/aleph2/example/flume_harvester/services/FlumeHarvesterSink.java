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

import java.util.Optional;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;

import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.utils.ContextUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.utils.FlumeUtils;

/** Harvest sink
 * @author alex
 */
public class FlumeHarvesterSink extends AbstractSink implements Configurable {

	IHarvestContext _context;
	FlumeBucketConfigBean _config;
	
	/* (non-Javadoc)
	 * @see org.apache.flume.conf.Configurable#configure(org.apache.flume.Context)
	 */
	@Override
	public void configure(Context flume_context) {
		_context = Lambdas.wrap_u(() ->
					ContextUtils.getHarvestContext(
							FlumeUtils.decodeSignature(flume_context.getString("context_signature", ""))))
					.get();
		
		//TODO get _config from bucket...
	}

	/* (non-Javadoc)
	 * @see org.apache.flume.Sink#process()
	 */
	@Override
	public Status process() throws EventDeliveryException {
		Status status = null;

		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		txn.begin();
		try {
			// This try clause includes whatever Channel operations you want to do

			Event event = ch.take();

			Optional<JsonNode> maybe_json_event = getEventJson(event, _config);
			maybe_json_event.ifPresent(json_event -> 
				_context.sendObjectToStreamingPipeline(Optional.empty(), json_event));

			txn.commit();
			status = Status.READY;
		} catch (Throwable t) {
			txn.rollback();

			// Log exception, handle individual exceptions as needed

			status = Status.BACKOFF;

			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error)t;
			}
		} finally {
			txn.close();
		}
		return status;
	}

	protected Optional<JsonNode> getEventJson(final Event e, final FlumeBucketConfigBean config) {
		return null;
	}

}
