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
package com.ikanow.aleph2.example.flume_harvester.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;

public class TestFlumeUtils {
	
	///////////////////////////////////////////////

	// (Morphlines)

	@Test
	public void test_morphlines() {
		
	}
	
	///////////////////////////////////////////////
	
	// (Main flume config)
	
	@Test
	public void test_mainFlumeConfig() throws IOException {
	
		final String json_test1 = Resources.toString(Resources.getResource("TestFlumeUtils_flume1.json"), Charsets.UTF_8);
		final String json_test_results_1 = Resources.toString(Resources.getResource("TestFlumeUtils_flume1_results.properties"), Charsets.UTF_8).replaceAll("[\r\n]+", "\n").replaceAll("(?m)^\\s*#.*(?:\r?\n)?", "");
		final String json_test2 = Resources.toString(Resources.getResource("TestFlumeUtils_flume2.json"), Charsets.UTF_8);
		final String json_test_results_2 = Resources.toString(Resources.getResource("TestFlumeUtils_flume2_results.properties"), Charsets.UTF_8).replaceAll("[\r\n]+", "\n").replaceAll("(?m)^\\s*#.*(?:\r?\n)?", "");
		
		final DataBucketBean bucket = BeanTemplateUtils.build(DataBucketBean.class).with(DataBucketBean::_id, "test1").with(DataBucketBean::full_name, "/test/1").done().get();
		final FlumeBucketConfigBean bucket_config_1 = BeanTemplateUtils.from(json_test1, FlumeBucketConfigBean.class).get();
		assertEquals(json_test_results_1, FlumeUtils.createFlumeConfig("test_ext_c1651d4c69ed_1", bucket, bucket_config_1, "test_signature", Optional.empty(), false)
				.replace(HostInformationUtils.getHostname(), "HOSTNAME").replaceAll("[\r\n]+", "\n"));
		
		final FlumeBucketConfigBean bucket_config_2 = BeanTemplateUtils.from(json_test2, FlumeBucketConfigBean.class).get();
		assertEquals(json_test_results_2, FlumeUtils.createFlumeConfig("test_ext_c1651d4c69ed_2", bucket, bucket_config_2, "test_signature", Optional.of("/test/morphline/path"), false)
				.replaceAll("[\r\n]+", "\n"));
	}
	
	///////////////////////////////////////////////
	
	// (Main flume config name)
	
	@Test
	public void test_configName() {
		
		final String path1 = "/test+extra/";
		final String path2 = "/test+extra/4354____42";
		final String path3 = "test+extra/4354____42/some/more/COMPONENTS";
		
		assertEquals("test_ext_c1651d4c69ed", FlumeUtils.getConfigName(path1, Optional.empty()));
		assertEquals("test_ext_test_12_b540d8622174", FlumeUtils.getConfigName(path1, Optional.of("test+;12345")));
		assertEquals("test_ext_4354__bb8a6a382d7b", FlumeUtils.getConfigName(path2, Optional.empty()));
		assertEquals("test_ext_4354_t_3b7ae2550a2e", FlumeUtils.getConfigName(path2, Optional.of("t")));
		assertEquals("test_ext_more_componen_ec9cbb79741c", FlumeUtils.getConfigName(path3, Optional.empty()));
		assertEquals("test_ext_more_componen_xx__e1f3feb12442", FlumeUtils.getConfigName(path3, Optional.of("XX__________")));
	}
	
}
