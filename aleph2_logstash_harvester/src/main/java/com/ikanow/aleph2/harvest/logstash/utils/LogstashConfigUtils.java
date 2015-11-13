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
package com.ikanow.aleph2.harvest.logstash.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;

/** Utilities for building logstash config files 
 * @author Alex
 */
public class LogstashConfigUtils {

	private static HashSet<String> _allowedInputs = new HashSet<String>();	
	private static HashSet<String> _allowedFilters = new HashSet<String>();	
	private static HashSet<String> _allowedOutputs = new HashSet<String>();	
	
	private static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	private static Pattern _validationRegexInputReplace = Pattern.compile("^([^a-z]*input[\\s\\n\\r]*\\{[\\s\\n\\r]*([a-z0-9_.-]+)[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);
	//private static Pattern _validationRegexOutput = Pattern.compile("[^a-z]output[\\s\\n\\r]*\\{", Pattern.CASE_INSENSITIVE); // (<-REPLACED BY JSONIFIED LOGIC, RETAINED UNLESS A NEED FOR IT COMES BACK)
	private static Pattern _validationRegexNoSourceKey = Pattern.compile("[^a-z0-9_]sourceKey[^a-z0-9_]", Pattern.CASE_INSENSITIVE);
	private static Pattern _validationRegexAppendFields = Pattern.compile("\\}[\\s\\n\\r]*\\}[^a-z{}\"']*(filter[\\s\\n\\r]*\\{)", Pattern.CASE_INSENSITIVE);

	//TODO (INF-2533): some of the not allowed types should be allowed but only with certain param settings (eg client not server - or even more sophisticated stuff)
	//eg would be good to allow elasticsearch but override indices

	public static String validateLogstashInput(LogstashHarvesterConfigBean globals, String sourceKey, String config, StringBuffer errorMessage, boolean isAdmin) {
		
		_allowedInputs.addAll(Arrays.asList(globals.non_admin_inputs().toLowerCase().split("\\s*,\\s*")));
		_allowedFilters.addAll(Arrays.asList(globals.non_admin_filters().toLowerCase().split("\\s*,\\s*")));
		_allowedOutputs.addAll(Arrays.asList(globals.non_admin_outputs().toLowerCase().split("\\s*,\\s*")));
		
		// Configuration validation, phase 1
		
		errorMessage.append("Validation error:");
		ObjectNode jsonifiedConfig = parseLogstashConfig(config, errorMessage);
		if (null == jsonifiedConfig) {
			return null;
		}
		errorMessage.setLength(0);
		
		// Configuration validation, phase 2 - very basic checks on the structure of the object

		Object input =  jsonifiedConfig.get("input");
		if ((null == input) || !(input instanceof ObjectNode)) { // Does input exist?
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (0)");
			return null;			
		}//TESTED (3_1d)
		else { // Check there's only one input type and (unless admin) it's one of the allowed types
			ObjectNode inputDbo = (ObjectNode)input;
			if (1 != inputDbo.size()) {
				errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (1)");
				return null;
			}//TESTED
			if (!isAdmin) {
				for (String key: (Iterable<String>) () -> inputDbo.fieldNames()) {
					if (!_allowedInputs.contains(key.toLowerCase())) {
						errorMessage.append("Security error, non-admin not allowed input type " + key + ", allowed options: " + _allowedInputs.toString());
						return null;
					}//TESTED
				}
			}//TESTED (3_1abc)
		}
		Object filter =  jsonifiedConfig.get("filter");
		if ((null == filter) || !(filter instanceof ObjectNode)) { // Does filter exist?
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (2)");
			return null;			
		}//TESTED (3_2d)
		else { // Check there's only one input type and (unless admin) it's one of the allowed types
			if (!isAdmin) {
				ObjectNode filterDbo = (ObjectNode)filter;
				for (String key: (Iterable<String>) () -> filterDbo.fieldNames()) {
					if (!_allowedFilters.contains(key.toLowerCase())) {
						errorMessage.append("Security error, non-admin not allowed filter type " + key + ", allowed options: " + _allowedFilters.toString());
						return null;
					}//TESTED
				}
			}//TESTED (3_2abc)
		}		
		
		//TODO: same for output
		
		// Configuration validation, phase 3
		
		Matcher m =  null;
		m =  _validationRegexInputReplace.matcher(config);
		if (!m.find()) {
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (3)");
			return null;
		}//TESTED (see above)
		else { // If admin check on allowed types
			String inputType = m.group(2).toLowerCase();
			
			// If it's a file-based plugin then replace sincedb_path (check that it's not used during the JSON-ification):
			if (inputType.equalsIgnoreCase("file") || inputType.equalsIgnoreCase("s3")) {
				config = _validationRegexInputReplace.matcher(config).replaceFirst("$1\n      sincedb_path => \"_XXX_DOTSINCEDB_XXX_\"\n");
			}//TESTED

		}//TESTED
		
		m =  _validationRegexNoSourceKey.matcher(config);
			// (this won't help malicious changes to source key, but will let people know they're not supposed to)
		if (m.find()) {
			errorMessage.append("Not allowed to reference sourceKey - this is automatically appended by the logstash harvester");
			return null;
		}//TESTED		
		
		// OK now need to append the sourceKey at each stage of the pipeline to really really ensure that nobody sets sourceKey to be different 
		
		m = _validationRegexAppendFields.matcher(config);
		StringBuffer newConfig = new StringBuffer();
		if (m.find()) {
			m.appendReplacement(newConfig, "add_field => [ \"sourceKey\", \""+sourceKey+"\"] \n\n" + m.group() + " \n if [sourceKey] == \""+sourceKey+"\" { \n\n ");
		}
		else {
			errorMessage.append("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them. (4)");
			return null;			
		}
		m.appendTail(newConfig);
		config = newConfig.toString();
		config = config.replaceAll("}[^}]*$", ""); // (remove the last })
		config += "\n\n mutate { update => [ \"sourceKey\", \""+sourceKey+"\"] } \n}\n}\n"; // double check the sourceKey hasn't been overwritten and close the if from above
		//TESTED (syntactically correct and does overwrite sourceKey everywhere - success_2_2)
		
		return config;
	}//TESTED
	
	/////////////////////////////////////////////////////////
	
	// The different cases here are:
	// 1) (?:[a-z0-9_]+)\\s*(=>)?\\s*([a-z0-9_]+)?\\s*\\{) 
	//		"input/filter {" - top level block
	//		"fieldname => type {" - (complex field, definitely allowed .. eg input.file.code)
	//		"fieldname => {" - (object field, not sure this is allowed)
	//		"fieldname {" - (object field, not sure this is allowed)
	// 2) (?:[a-z0-9_]+)\\s*=>\\s*[\\[a-z0-9])
	//		"fieldname => value" - sub-field or array of sub-fields
	// 3) (?:if\\s*[^{]+\\s*\\{)
	//		if statement
	// 4) }
	//		closing block
	public static Pattern _navigateLogstash = Pattern.compile("(?:([a-z0-9_]+)\\s*(=>)?\\s*([a-z0-9_]+)?\\s*\\{)|(?:([a-z0-9_]+)\\s*=>\\s*[\\[a-z0-9])|(?:if\\s*[^{]+\\s*\\{)|}", Pattern.CASE_INSENSITIVE);
	
	public static ObjectNode parseLogstashConfig(String configFile, StringBuffer error) {
		
		ObjectNode tree = _mapper.createObjectNode();

		// Stage 0: remove escaped "s and 's (for the purpose of the validation):
		// (prevents tricksies with escaped "s and then #s)
		// (http://stackoverflow.com/questions/5082398/regex-to-replace-single-backslashes-excluding-those-followed-by-certain-chars)
		configFile = configFile.replaceAll("(?<!\\\\)(?:((\\\\\\\\)*)\\\\)[\"']", "X");
		//TESTED (by hand - using last 2 fields of success_2_1)
		
		// Stage 1: remove #s, and anything in quotes (for the purpose of the validation)
		configFile = configFile.replaceAll("(?m)(?:([\"'])(?:(?!\\1).)*\\1)", "VALUE").replaceAll("(?m)(?:#.*$)", "");
		//TESTED (2_1 - including with a # inside the ""s - Event_Date -> Event_#Date)
		//TESTED (2_2 - various combinations of "s nested inside 's) ... yes that is a negative lookahead up there - yikes!
		
		// Stage 2: get a nested list of objects
		int depth = 0;
		int ifdepth = -1;
		Stack<Integer> ifStack = new Stack<Integer>();
		ObjectNode inputOrFilter = null;
		Matcher m = _navigateLogstash.matcher(configFile);
		// State:
		String currTopLevelBlockName = null;
		String currSecondLevelBlockName = null;
		ObjectNode currSecondLevelBlock = null;
		while (m.find()) {
			boolean simpleField = false;

			//DEBUG
			//System.out.println("--DEPTH="+depth + " GROUP=" + m.group() + " IFS" + Arrays.toString(ifStack.toArray()));
			//System.out.println("STATES: " + currTopLevelBlockName + " AND " + currSecondLevelBlockName);
			
			if (m.group().equals("}")) {
				
				if (ifdepth == depth) { // closing an if statement
					ifStack.pop();
					if (ifStack.isEmpty()) {
						ifdepth = -1;
					}
					else {
						ifdepth = ifStack.peek();
					}
				}//TESTED (1_1bc, 2_1)
				else { // closing a processing block
					
					depth--;
					if (depth < 0) { // {} Mismatch
						error.append("{} Mismatch (})");
						return null;
					}//TESTED (1_1abc)
				}
			}
			else { // new attribute!
				
				String typeName = m.group(1);
				if (null == typeName) { // it's an if statement or a string value
					typeName = m.group(4);
					if (null != typeName) {
						simpleField = true;
					}
				}		
				else if (typeName.equalsIgnoreCase("else")) { // It's an if statement..
					typeName = null;
				}
				if (null == typeName) { // if statement after all
					// Just keep track of ifs so we can ignore them
					ifStack.push(depth);
					ifdepth = depth;
					// (don't increment depth)
				}//TESTED (1_1bc, 2_1)
				else { // processing block
					String subTypeName = m.group(3);
					if (null != subTypeName) { // eg codec.multiline
						typeName = typeName + "." + subTypeName;
					}//TESTED (2_1, 2_3)
					
					if (depth == 0) { // has to be one of input/output/filter)
						String topLevelType = typeName.toLowerCase();
						if (topLevelType.equalsIgnoreCase("input") || topLevelType.equalsIgnoreCase("filter")) {
							if (tree.has(topLevelType)) {
								error.append("Multiple input or filter blocks: " + topLevelType);
								return null;
							}//TESTED (1_3ab)
							else {
								inputOrFilter = _mapper.createObjectNode();
								tree.put(topLevelType, inputOrFilter);
								
								// Store state:
								currTopLevelBlockName = topLevelType;
							}//TESTED (*)
						}
						else {
							if (topLevelType.equalsIgnoreCase("output")) {
								error.append("Not allowed output blocks - these are appended automatically by the logstash harvester");
							}
							else {
								error.append("Unrecognized processing block: " + topLevelType);
							}
							return null;
						}//TESTED (1_4a)
					}
					else if ((depth == 1) && (null != inputOrFilter)) { // processing blocks
						String subElType = typeName.toLowerCase();
						
						// Some validation: can't include a type called "filter" anywhere
						if ((null != currTopLevelBlockName) && currTopLevelBlockName.equals("input")) {
							if (subElType.equals("filter") || subElType.endsWith(".filter")) {
								error.append("Not allowed sub-elements of input called 'filter' (1)");
								return null;
							}
						}//TESTED (1_5b)
						
						ArrayNode subElements = (ArrayNode) inputOrFilter.get(subElType);
						if (null == subElements) {
							subElements = _mapper.createArrayNode();
							inputOrFilter.put(subElType, subElements);
						}
						ObjectNode newEl = _mapper.createObjectNode();
						subElements.add(newEl);
						
						// Store state:
						currSecondLevelBlockName = subElType;
						currSecondLevelBlock = newEl;
					}//TESTED (*)
					else if (depth == 2) { // attributes of processing blocks
						// we'll just store the field names for these and do any simple validation that was too complicated for the regexes
						String subSubElType = typeName.toLowerCase();
						
						// Validation:
						if (null != currTopLevelBlockName) {
							// 1] sincedb path
							if (currTopLevelBlockName.equals("input") && (null != currSecondLevelBlockName)) {
								// (don't care what the second level block name is - no sincedb allowed)
								if (subSubElType.equalsIgnoreCase("sincedb_path")) {
									error.append("Not allowed sincedb_path in input.* block");
									return null;
								}//TESTED (1_5a)
								// 2] no sub-(-sub etc)-elements of input called filter
								if (subSubElType.equals("filter") || subSubElType.endsWith(".filter")) {
									error.append("Not allowed sub-elements of input called 'filter' (2)");
									return null;									
								}//TESTED (1_5c)
							}				
						}
						
						// Store in map:
						if (null != currSecondLevelBlock) {
							currSecondLevelBlock.put(subSubElType, _mapper.createObjectNode());
						}
					}
					// (won't go any deeper than this)
					if (!simpleField) {
						depth++;
					}
				}
				
			}
		}
		if (0 != depth) {
			error.append("{} Mismatch ({)");
			return null;
		}//TESTED (1_2a)
		
		return tree;
	}//TESTED (1.1-3,2.1)

}
