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
package com.ikanow.aleph2.example.flume_harvester.data_model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Per bucket configuration
 * @author alex
 */
public class FlumeBucketConfigBean {

	/** The structured input configuration - overrides the sources and channels from flume_config if enabled
	 *  If any underlying objects are specified then flume_config (but not flume_config_str) is ignored
	 * @return
	 */
	public InputConfig input() { return input; }
	/** Strings in the flume config that start with this string (default $$$$) are substituted - currently supported is
	 *  signature (Aleph2 bucket signature), morphline (location of morphline file), hostname
	 * @return
	 */
	public String substitution_prefix() { return substitution_prefix; }
	/** A key/value map of strings, each one corresponds to a line in the generated flume config key=value
	 *  Note that keys with "."s (ie most of them) should have the "." replaced with ":" in the config
	 *  Note that if no sink is specified (this is recommended) then one is auto generated based on the bucket data schema/the output block
	 *  This is ignored if any input objects are specified
	 * @return
	 */
	public Map<String, String> flume_config() { return flume_config; }
	/** A set of key/values that overwrite the ones from flume_config when run in test mode. Should be strings, or {} to remove an entry
	 *  This is ignored if any input objects are specified
	 * @return
	 */
	public Map<String, Object> flume_config_test_overrides() { return flume_config_test_overrides; } // (use {} to delete a field from flume_config)
	/** A set of key/values (string/anything) that map to Morphlines key=value lines
	 * @return
	 */
	public Map<String, Object> morphlines_config() { return morphlines_config; }
	/** This allows users to specify a set of properties in standard flume format (to which flume_config/input is appended)
	 *  Its use is not recommended
	 * @return
	 */
	public String flume_config_str() { return flume_config_str; }
	/** This allows users to specify a set of Morphlines properties in standard Morphlines format (to which morphlines_config is appended)
	 * @return
	 */
	public String morphlines_config_str() { return morphlines_config_str; }
	/** Specifies a structured output configuration  - overrides the sinks from flume_config if enabled
	 * @return
	 */
	public OutputConfig output() { return output; }
	
	/** The structured input configuration - overrides the sources and channels from flume_config if enabled
	 * @author Alex
	 */
	public static class InputConfig {
		public static class SpoolDirConfig {
			/** Whether this input directory is currently enabled
			 * @return
			 */
			public boolean enabled() { return enabled == null ? true : enabled; }
			/** The path of the spool dir - note only one flume instance can read from a spool dir at a time
			 *  (because that flume instance will delete/rename the folder)
			 * @return
			 */
			public String path() { return path; }
			/** In test mode, files from this directory are copied into the (test) spool directory
			 * @return
			 */
			public String test_src_path() { return test_src_path; }
			/** Allows the tracker dir to be overwritten - else a bucket-unique directory is used
			 *  (recommend this is left at its default)
			 * @return
			 */
			public String tracker_dir() { return tracker_dir; }
			/** A regex (default ^$ ie no files) that is used to determine files to ignore
			 * @return
			 */
			public String ignore_pattern() { return ignore_pattern; }
			/** If true, the file is deleted once ingested; if false it has ".COMPLETED" appended
			 * @return 
			 */
			public boolean delete_on_ingest() { return Optional.ofNullable(delete_on_ingest).orElse(true); }
			
			private Boolean enabled;
			private String path;
			private String test_src_path;
			private String tracker_dir;
			private String ignore_pattern;
			private Boolean delete_on_ingest;
		}		
		public List<SpoolDirConfig> spool_dirs() { return Optional.ofNullable(spool_dirs).map(Collections::unmodifiableList).orElse(Collections.emptyList()); }
		private List<SpoolDirConfig> spool_dirs;
	}
	/** Specifies a structured output configuration  - overrides the sinks from flume_config if enabled
	 *  If neither JSON not CSV is specified, then it defaults to JSON with policy "event"
	 * @author Alex
	 */
	public static class OutputConfig {
		/** For JSON output
		 * @author Alex
		 */
		public static class JsonConfig {
			/** Whether the JSON config is enabled
			 * @return
			 */
			public boolean enabled() { return enabled == null ? true : enabled; }
			/** One of:
			 *  - body .. will take the body, try to convert it to JSON and just use that
			 *  - body_plus_headers ... as above but will then insert the header key/values into the resulting JSON object
			 *  - event_no_body ... only the headers, no body
			 *  - event ... the headers + the body as a string field with name specified by "include_body_with_name" (default "message")
			 *  
			 * @return
			 */
			public JsonPolicy json_policy() { return Optional.ofNullable(json_policy).orElse(JsonPolicy.body_plus_headers); }
			/** If the body is included as a string field, this is the field name (default "message")
			 * @return
			 */
			public String include_body_with_name() { return include_body_with_name; }
			
			private Boolean enabled;
			public enum JsonPolicy { body, body_plus_headers, event, event_no_body };
			private JsonPolicy json_policy;
			private String include_body_with_name;
		}
		/** For CSV output
		 * @author Alex
		 */
		public static class CsvConfig {
			/** Whether the CSV config is enabled
			 * @return
			 */
			public boolean enabled() { return enabled == null ? true : enabled; }
			/** The field separator (default ",") - must be 1 character
			 * @return
			 */
			public String separator() { return Optional.ofNullable(separator).orElse(","); }
			/** A list of header fields. To discard a field from the CSV, leave the header field blank. Also any fields beyond the last header field is discarded
			 * @return
			 */
			public List<String> header_fields() { return header_fields; }
			/** A regex that is applied line by line, anything matching is discarded
			 * @return
			 */
			public String ignore_regex() { return ignore_regex; }
			/** The escape char (must be 1 char ins size), defaults to \
			 * @return
			 */
			public String escape_char() { return Optional.ofNullable(escape_char).orElse("\\"); }
			/** The quote char (must be 1 char ins size), defaults to "
			 * @return
			 */
			public String quote_char() { return Optional.ofNullable(quote_char).orElse("\""); }
			/** A map of field name to type (allowed types: "int", "long", "boolean", "double", "hex", "date")
			 * @return
			 */
			public Map<String, String> non_string_types() { return Optional.ofNullable(non_string_types).map(Collections::unmodifiableMap).orElse(Collections.emptyMap()); }
			/** A map of types to lists of fieldnames (replaces non_string_types) if present
			 * @return
			 */
			public Map<String, List<String>> non_string_type_map() { return Optional.ofNullable(non_string_type_map).map(Collections::unmodifiableMap).orElse(Collections.emptyMap()); }
			
			private Boolean enabled;
			private String separator;
			private List<String> header_fields;
			private String ignore_regex;
			private String escape_char;
			private String quote_char;
			private Map<String, String> non_string_types; // "int", "long", "boolean", "double", "hex", "date"
			private Map<String, List<String>> non_string_type_map; // as above except "int": [ "field1", "field2", ... ]
		}
		/** For JSON output
		 * @return
		 */
		public JsonConfig json() { return json; }
		/** For CSV
		 * @return
		 */
		public CsvConfig csv() { return csv; }
		/** If specified then the ouput module will write directly out to the designated service instead of going into the enrichment stream
		 *  (TODO (ALEPH-10) batch related concepts not yet supported)
		 *  General format <service name>[.<non default service name>]
		 *  Currently supported: search_index_service, storage_service
		 *  If stream is specified then the data is _also_ sent to the enrichment stream
		 *  
		 * @return
		 */
		public Set<String> direct_output() { return direct_output; }
		/** If this field is specified then a field with this name will be appended if not already present, with the ingest time
		 *  (If not but the temporal schema is enabled, then the time_field from that -default @timestamp - us used instead)
		 * @return
		 */
		public String add_time_with_name() { return add_time_with_name; }
		
		private JsonConfig json;
		private CsvConfig csv;
		
		private Set<String> direct_output; // ("search_index_service[.<non_default_service>]", "storage_service", etc)
		private String add_time_with_name;
	}
	
	private InputConfig input;
	private OutputConfig output;
	private String substitution_prefix;
	private Map<String, String> flume_config;
	private Map<String, Object> flume_config_test_overrides;
	private Map<String, Object> morphlines_config;	
	private String flume_config_str;
	private String morphlines_config_str;
}
