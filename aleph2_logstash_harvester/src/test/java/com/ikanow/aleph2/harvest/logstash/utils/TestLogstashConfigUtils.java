package com.ikanow.aleph2.harvest.logstash.utils;
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


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.JsonUtils;
import com.ikanow.aleph2.harvest.logstash.data_model.LogstashHarvesterConfigBean;

public class TestLogstashConfigUtils {

	@Test
	public void test_logstashConfigUtils() throws IOException {
		LogstashHarvesterConfigBean globals = BeanTemplateUtils.build(LogstashHarvesterConfigBean.class).done().get(); //(all defaults)
		
		// 1) Errored sources - things that break the formatting
		StringBuffer errors = new StringBuffer();
		String testName;
		// 1.1) {} mismatch 1
		//a
		errors.setLength(0);		
		testName = "error_1_1a";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		//b
		errors.setLength(0);
		testName = "error_1_1b";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		//c
		errors.setLength(0);
		testName = "error_1_1c";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch (})")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.2) {} mismatch 2

		//a
		errors.setLength(0);
		testName = "error_1_2a";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().startsWith("{} Mismatch ({)")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}

		
		// 1.3) multiple input/filter blocks
		// 1.3a) input
		errors.setLength(0);
		testName = "error_1_3a";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Multiple input or filter blocks: input")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		// 1.3b) filter
		errors.setLength(0);
		testName = "error_1_3b";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Multiple input or filter blocks: filter")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.4) unrecognized blocks
		// a output - special case
		errors.setLength(0);
		testName = "error_1_4a";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed output blocks - these are appended automatically by the logstash harvester")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		// b
		errors.setLength(0);
		testName = "error_1_4b";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Unrecognized processing block: something_random")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 1.5) fields/sub-elements that are not permitted
		// a ... sincedb_path
		errors.setLength(0);
		testName = "error_1_5a";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sincedb_path in input.* block")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		// b ... filter as sub-path of input
		errors.setLength(0);
		testName = "error_1_5b";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sub-elements of input called 'filter' (1)")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		// c ... filter as sub-path of sub-element of input
		errors.setLength(0);
		testName = "error_1_5c";
		if (null != LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors)) {
			fail("**** FAIL " + testName);
		}
		else if (!errors.toString().equals("Not allowed sub-elements of input called 'filter' (2)")) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		
		// 2) Valid formatted source
		ObjectNode retVal;
		String output;
		String inputName; // (for re-using config files across text)
		//2.1)
		errors.setLength(0);
		testName = "success_2_1";
		if (null == (retVal = LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors))) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		else if (!retVal.toString().equals("{ \"input\" : { \"file\" : [ { \"path\" : { } , \"start_position\" : { } , \"type\" : { } , \"codec.multiline\" : { }}]} , \"filter\" : { \"csv\" : [ { \"columns\" : { }}] , \"drop\" : [ { }] , \"mutate\" : [ { \"convert\" : { }} , { \"add_fields\" : { }} , { \"rename\" : { }}] , \"date\" : [ { \"timezone\" : { } , \"match\" : { }}] , \"geoip\" : [ { \"source\" : { } , \"fields\" : { }}]}}".replace(" ", ""))) {
			fail("**** FAIL " + testName + ": " + retVal.toString());						
		}
		//System.out.println("(val="+retVal+")");
		
		// 2.2
		errors.setLength(0);
		testName = "success_2_2";
		if (null == (retVal = LogstashConfigUtils.parseLogstashConfig(getTestFile(testName), errors))) {
			fail("**** FAIL " + testName + ": " + errors.toString());			
		}
		if (null == JsonUtils.getProperty("filter.geoip.fields", retVal)) {
			fail("**** FAIL " + testName + ": " + retVal);						
		}
		//System.out.println(retVal);
		
		//2.3)	- check that the sincedb is added correctly, plus the sourceKey manipulation
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_2_3";
		inputName = "success_2_3";		
		if (null == (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, true))) {
			fail("**** FAIL " + testName + ": errored: " + errors);			
		}
		else {
			String outputToTest = output.replaceAll("[\r\n]", "").replaceAll("\\s+", " ");
			String testAgainst = "input { file { sincedb_path => \"_XXX_DOTSINCEDB_XXX_\" path => \"/root/odin-poc-data/proxy_logs/may_known_cnc.csv\" start_position => beginning type => \"proxy_logs\" codec => multiline { pattern => \"^%{YEAR}-%{MONTHNUM}-%{MONTHDAY}%{DATA:summary}\" negate => true what => \"previous\" } add_field => [ \"[@metadata][sourceKey]\", \"inputs_2_3\"] }}filter { if [@metadata][sourceKey] == \"inputs_2_3\" { if [type] == \"proxy_logs\" { csv { columns => [\"Device_Name\",\"SimpleDate\",\"Event_#Date\",\"Source_IP\",\"Source_Port\",\"Destination_IP\",\"Destination_Port\",\"Protocol\",\"Vendor_Alert\",\"MSS_Action\",\"Logging_Device_IP\",\"Application\",\"Bytes_Received\",\"Bytes_Sent\",\"Dest._Country\",\"Message\",\"Message_Type\",\"MSS_Log_Source_IP\",\"MSS_Log_Source_Type\",\"MSS_Log_Source_UUID\",\"network_protocol_id\",\"OS_Type\",\"PIX_Main-Code\",\"PIX_Sub-Code\",\"Port\",\"Product_ID\",\"Product\",\"Rule\",\"Rule_Identifier\",\"Sensor_Name\",\"Class\",\"Translate_Destination_IP\",\"Translate_Destination_Port\",\"Translate_Source_IP\"] } if [Device_Name] == \"Device Name\" { drop {} } mutate { convert => [ \"Bytes_Received\", \"integer\" ] convert => [ \"Bytes_Sent\", \"integer\" ] } date { timezone => \"Europe/London\" match => [ \"Event_Date\" , \"yyyy-MM-dd'T'HH:mm:ss\" ] } geoip { source => \"Destination_IP\" fields => [\"timezone\",\"location\",\"latitude\",\"longitude\"] } } mutate { update => [ \"[@metadata][sourceKey]\", \"inputs_2_3\"] } }}";
			testAgainst = testAgainst.replaceAll("[\n\r]", "\\\\n").replaceAll("\\s+", " ");
			assertEquals(testAgainst, outputToTest);
		}

		// 3) Valid formatted source, access to restricted types
		
		// 3.1) input 
		// a) restricted - admin
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_3_1a";
		inputName = "success_2_1";		
		if (null != (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, false))) {
			fail("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Security error, non-admin not allowed input type file, allowed options: ")) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// b) restricted - non admin
		// (USE success_2_1 for this)
		errors.setLength(0);
		testName = "inputs_3_1b";
		inputName = "success_2_1";		
		if (null == (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, true))) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// c) unrestricted - non admin
		errors.setLength(0);
		testName = "inputs_3_1c";
		inputName = "inputs_3_1c";		
		if (null == (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, true))) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(val="+output+")");
		
		// d) no input at all
		errors.setLength(0);
		testName = "inputs_3_1d";
		inputName = "inputs_3_1d";		
		if (null != (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, false))) {
			fail("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.")) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// 3.2) filter
		// a) restricted - admin
		errors.setLength(0);
		testName = "filters_3_2a";
		inputName = "filters_3_2a";		
		if (null != (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, false))) {
			fail("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Security error, non-admin not allowed filter type elasticsearch, allowed options: ")) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(err="+errors.toString()+")");
		
		// b) restricted - non admin
		// (USE filters_3_2a for this)
		errors.setLength(0);
		testName = "filters_3_2a";
		inputName = "filters_3_2a";		
		if (null == (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, true))) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		//System.out.println("(val="+output+")");
		
		// c) unrestricted - non admin
		// (implicitly tested via 3.1bc)
		
		// d) no filter at all
		errors.setLength(0);
		testName = "filters_3_2d";
		inputName = "filters_3_2d";		
		if (null != (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, false))) {
			fail("**** FAIL " + testName + ": Should have errored: " + output);			
		}
		else if (!errors.toString().startsWith("Invalid input format, should be 'input { INPUT_TYPE { ... } }' (only one INPUT_TYPE) and also contain a filter, no \"s around them.")) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		
		// e) filter w/ regex that has { } in it
		// (USE filters_3_2e for this)
		errors.setLength(0);
		testName = "filters_3_2e";
		inputName = "filters_3_2e";		
		if (null == (output = LogstashConfigUtils.validateLogstashInput(globals, testName, getTestFile(inputName), errors, true))) {
			fail("**** FAIL " + testName + ": " + errors.toString());				
		}
		
	}
	private static String getTestFile(String name) throws IOException {
		File testFile = new File("src/test/resources/config_building/logstashtest_" + name + ".txt");
		return FileUtils.readFileToString(testFile);
	}
}
