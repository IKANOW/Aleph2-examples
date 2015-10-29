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
