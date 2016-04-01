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
package com.ikanow.aleph2.enrichment.utils.data_model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Config bean for the JS script engine
 * @author Alex
 */
public class JsScriptEngineBean implements Serializable {
	private static final long serialVersionUID = -3588059238532929148L;

	protected JsScriptEngineBean() {}

	/** Returns the user script, should include one of
	 *  handle_batch or handle_batch_java. Not needed if that function appears in one of the imports.
	 * @return the user script to execute
	 */
	public String script() { return Optional.ofNullable(script).orElse(""); }
		
	/** A list of resources (which can include the main handle_batch call)
	 * @return
	 */
	public List<String> imports() { return Optional.ofNullable(imports).map(Collections::unmodifiableList).orElse(Collections.emptyList()); }
	
	/** Misc script specific config
	 * @return
	 */
	public Map<String, Object> config() { return Optional.ofNullable(config).map(Collections::unmodifiableMap).orElse(Collections.emptyMap()); }

	/** If true (default: false) then errors during record processing will cause the entire module to error out
	 *  (falling back on whatever the underlying technology does in the case of errors)
	 * @return
	 */
	public Boolean exit_on_error() { return Optional.ofNullable(exit_on_error).orElse(false); }
	
	private String script;
	private List<String> imports; 
	private Map<String, Object> config;	
	private Boolean exit_on_error;
}
