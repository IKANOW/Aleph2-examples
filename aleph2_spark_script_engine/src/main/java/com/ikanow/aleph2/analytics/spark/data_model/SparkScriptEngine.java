/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
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

package com.ikanow.aleph2.analytics.spark.data_model;

import java.util.Optional;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;

import scala.Tuple2;

import com.google.common.collect.Multimap;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;

/** A very very simple class to contain everything we want to pass from Java to Scala
 * @author Alex
 *
 */
public class SparkScriptEngine {

	public SparkScriptEngine(
			IAnalyticsContext aleph2_context,
			Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs,
			Optional<ProcessingTestSpecBean> test_spec,
			SparkConf spark_context, SparkTopologyConfigBean job_config) {
		super();
		this.aleph2_context = aleph2_context;
		this.inputs = inputs;
		this.test_spec = test_spec;
		this.spark_context = spark_context;
		this.job_config = job_config;
	}
	public IAnalyticsContext aleph2_context;
	public Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs;
	public Optional<ProcessingTestSpecBean> test_spec;
	public SparkConf spark_context;
	public SparkTopologyConfigBean job_config;
}
