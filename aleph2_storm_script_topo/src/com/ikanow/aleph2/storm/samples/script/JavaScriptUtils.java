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
package com.ikanow.aleph2.storm.samples.script;

/**
 * @author Joern Freydank jfreydank@ikanow.com
 *
 */
public class JavaScriptUtils {

	// genericFunctionCall - all functions passed in via $SCRIPT get the following name
	public static String genericFunctionCall = "getValue";

	/**
	 * getScript
	 * Extracts JavaScript code from $SCRIPT() and wraps in getVal function
	 * @param script - $SCRIPT()
	 * @return String - function getVal()
	 */
	public static String getScript(String script)
	{
		// The start and stop index use to substring the script
		int start = script.indexOf("(");
		int end = script.lastIndexOf(")");
		
		try {
			if (script.toLowerCase().startsWith("$script"))
			{
				// Remove $SCRIPT() wrapper and then wrap script in 'function getValue() { xxxxxx }'
				return "function " + genericFunctionCall + "() { " + script.substring(start + 1, end) + " }";
			}
			else
			{
				// Simply remove $FUNC() wrapper
				return script.substring(start + 1, end);
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Malformed script: " + script);
		}
	}
	

	/**
	 * getScript
	 * Extracts JavaScript code from $SCRIPT() and wraps in getVal function
	 * @param script - $SCRIPT()
	 * @return String - function getVal()
	 */
	public static String createDollarScriptFunctionAndCall(String scriptlet)
	{
		// The start and stop index use to substring the script
		int start = scriptlet.indexOf("(");
		int end = scriptlet.lastIndexOf(")");
		
		try {
			if (scriptlet.toLowerCase().startsWith("$script"))
			{
				// Remove $SCRIPT() wrapper and then wrap script in 'function getValue() { xxxxxx }'
				return "function " + genericFunctionCall + "() { " + scriptlet.substring(start + 1, end) + " };"+genericFunctionCall+"();";
			}		
		}
		catch (Exception e) {
			throw new RuntimeException("Malformed script: " + scriptlet);
		}
		return "";
	}


}
