/*******************************************************************************
* Copyright 2015, The IKANOW Open Source Project.
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License, version 3,
* as published by the Free Software Foundation.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
******************************************************************************/
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
	
	private String script;
	private List<String> imports; 
	private Map<String, Object> config;	
}
