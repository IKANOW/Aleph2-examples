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
package com.ikanow.aleph2.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ikanow.aleph2.storm.harvest_technology.IStormController;
import com.ikanow.aleph2.storm.harvest_technology.LocalStormController;
import com.ikanow.aleph2.storm.harvest_technology.RemoteStormController;

import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;

public class StormControllerUtil {
	private static final Logger _logger = LogManager.getLogger();
	private static final long MAX_RETRIES = 30;
	
	public static IStormController getLocalStormController() {
		return new LocalStormController();
	}
	
	public static IStormController getRemoteStormController(String nimbus_host, int nimbus_thrift_port, String storm_thrift_transport_plugin) {
		return new RemoteStormController(nimbus_host, nimbus_thrift_port, storm_thrift_transport_plugin);
	}
	
	public static void startJob(IStormController storm_controller, String job_name, String input_jar_location, StormTopology topology) throws Exception {
		long retries = 0;
		while ( retries < MAX_RETRIES ) {				
			try {
				_logger.debug("Trying to submit job, try: " + retries + " of " + MAX_RETRIES);
				storm_controller.submitJob(job_name, input_jar_location, topology);
				retries = MAX_RETRIES; //if we got here, we didn't throw an exception, so we completed successfully
			} catch ( Exception ex) {
				if ( ex instanceof AlreadyAliveException ) {
					retries++;
					Thread.sleep(1000); //sleep 1s, was seeing about 2s of sleep required before job successfully submitted on restart
				} else {
					retries = MAX_RETRIES; //we threw some other exception, bail out
					throw ex;
				}
			}
		}
	}
	
	public static void stopJob(IStormController storm_controller, String job_name) throws Exception {
		storm_controller.stopJob(job_name);
	}
	
	public static void restartJob(IStormController storm_controller, String job_name, String input_jar_location, StormTopology topology) throws Exception {
		stopJob(storm_controller, job_name);
		waitForJobToDie(storm_controller, job_name, 15L);
		startJob(storm_controller, job_name, input_jar_location, topology);
	}
	
	public static TopologyInfo getJobStats(IStormController storm_controller, String job_name) throws Exception {
		return storm_controller.getJobStats(job_name);
	}
	
	/**
	 * Continually checks if job has died, returns true if it has, or throws an exception if
	 * timeout occurs (seconds_to_wait elaspses).
	 * 
	 * @param storm_controller
	 * @param bucket
	 * @param l
	 * @return
	 * @throws Exception 
	 */
	private static void waitForJobToDie(
			IStormController storm_controller, String job_name, long seconds_to_wait) throws Exception {
		long start_time = System.currentTimeMillis();
		long num_tries = 0;
		long expire_time = System.currentTimeMillis() + (seconds_to_wait*1000);
		while ( System.currentTimeMillis() < expire_time ) {
			TopologyInfo info = null;
			try {
				info = getJobStats(storm_controller, job_name);
			} catch (Exception ex) {}
			if ( null == info ) {				
				_logger.debug("JOB_STATUS: no longer exists, assuming that job is dead and gone, spent: " + (System.currentTimeMillis()-start_time) + "ms waiting");				
				return;
			}
			num_tries++;
			_logger.debug("Waiting for job status to go away, try number: " + num_tries);
			Thread.sleep(2000); //wait 2s between checks, in tests it was taking 8s to clear
		}		
	}
}
