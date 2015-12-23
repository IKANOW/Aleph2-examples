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
package com.ikanow.aleph2.example.external_analytics.data_model;

import java.util.Optional;

/** Stores process information for each bucket/host (just for demonstration)
 * @author Alex
 */
public class ProcessInfoBean {

	public static final Optional<String> PID_COLLECTION_NAME = Optional.of("pids");
	
	protected ProcessInfoBean() {}
	
	public ProcessInfoBean(final String pid, final String hostname, final String bucketname) {
		_id = hostname + ":" + pid;
		this.pid = pid;
		this.hostname = hostname;
		this.bucket_name = bucketname;
	}
	
	/** The generated _id
	 * @return
	 */
	public String _id() { return _id; }
	
	/** The pid
	 * @return
	 */
	public String pid() { return pid; }

	/** The hostname (for multi node buckets)
	 * @return
	 */
	public String hostname() { return hostname; }
	
	/** The bucket.full_name (informational)
	 * @return
	 */
	public String bucket_name() { return bucket_name; }
	
	private String _id;
	private String pid;
	private String hostname;
	private String bucket_name;
}
