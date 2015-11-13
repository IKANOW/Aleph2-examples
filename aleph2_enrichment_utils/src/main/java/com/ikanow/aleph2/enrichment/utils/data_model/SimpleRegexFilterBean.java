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
import java.util.Optional;

/** Configuration for SimpleRegexFilterService
 * @author Alex
 */
public class SimpleRegexFilterBean implements Serializable {
	private static final long serialVersionUID = -5464523739742592609L;

	protected SimpleRegexFilterBean() {}
	
	/* Returns the list of elements to apply
	 * @return
	 */
	public List<RegexConfig> elements() { return Optional.ofNullable(elements).map(l -> Collections.unmodifiableList(l)).orElse(Collections.emptyList()); }	
	
	private List<RegexConfig> elements;
	
	/** The low level regex config element
	 * @author Alex
	 */
	public static class RegexConfig implements Serializable {
		private static final long serialVersionUID = 2427058584606592197L;

		protected RegexConfig() {}
		
		public boolean enabled() { return Optional.ofNullable(enabled).orElse(true); }
		public List<String> fields() { return Optional.ofNullable(fields).map(l -> Collections.unmodifiableList(l)).orElse(Collections.emptyList()); }	
		public String flags() { return Optional.ofNullable(flags).orElse(""); }
		public List<String> regexes() { return Optional.ofNullable(regexes).map(l -> Collections.unmodifiableList(l)).orElse(Collections.emptyList()); }			
		
		private Boolean enabled;
		private List<String> fields;
		private String flags;
		private List<String> regexes; 
	}
	
}
