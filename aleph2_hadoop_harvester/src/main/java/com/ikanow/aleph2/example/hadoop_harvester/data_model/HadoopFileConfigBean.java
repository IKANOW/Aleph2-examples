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
package com.ikanow.aleph2.example.hadoop_harvester.data_model;

import java.util.List;
import java.util.Map;

import java.util.Collections;

/** Generic configuration for file configurations
 * @author Alex
 */
public class HadoopFileConfigBean {

	// Generic properties
	
	public static class FileConfigBean {

		//TOOD
		public List<String> uri_list;
		public String include_regex;
		public String exclude_regex;
		public Boolean delete_once_parsed;
		public String rename_once_parsed;
		public String parser_name;
		public Long max_depth;

		public List<String> auth_tokens;
	}
	
	public List<FileConfigBean> file_list() { return file_list; }
	private List<FileConfigBean> file_list;
	
	// Default parsers:
	
	public CsvConfigBean csv() { return csv; }
	private CsvConfigBean csv;
	
	// Additional parsers
	
	public Map<String, CsvConfigBean> csv_parsers() { return null != csv_parsers ? Collections.unmodifiableMap(csv_parsers) : Collections.emptyMap(); } 
	private Map<String, CsvConfigBean> csv_parsers;
	
	// Type specific properties
	
	public static class CsvConfigBean {
		
		public String candidate_header_regex() { return candidate_header_regex; }
		public String ignore_field_regex() { return ignore_field_regex; }
		public String header_fields_override() { return header_fields_override; }
		public String field_separator() { return field_separator; }
		public String escape_character() { return escape_character; }
		public String quote_character() { return quote_character; }
		
		protected String candidate_header_regex;
		protected String ignore_field_regex;
		protected String header_fields_override;
		protected String field_separator;
		protected String escape_character;
		protected String quote_character;
	};
}
