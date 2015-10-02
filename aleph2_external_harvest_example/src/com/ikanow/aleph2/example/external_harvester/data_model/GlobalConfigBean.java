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
package com.ikanow.aleph2.example.external_harvester.data_model;

import java.util.Optional;

/** Stores global configuration for the harvester (just for demonstration)
 * @author Alex
 */
public class GlobalConfigBean {

	/** Whether to store the PIDs in the DB vs in the file
	 *  (Just used to demonstrate global configuration and per library state access)
	 * @return
	 */
	public boolean store_pids_in_db() { return Optional.ofNullable(store_pids_in_db).orElse(false); }
	public boolean restart_process_on_exit() { return Optional.ofNullable(restart_process_on_exit).orElse(false); }
	private Boolean store_pids_in_db;
	private Boolean restart_process_on_exit;
}
