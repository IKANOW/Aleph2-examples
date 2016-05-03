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

import java.io.Serializable;
import java.util.Optional;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;

import fj.data.Either;
import fj.data.Validation;

/** A very very simple class to contain everything we want to pass from Java to Scala
 * @author Alex
 *
 */
public class SparkScriptEngine implements Serializable {
	private static final long serialVersionUID = -1661759140972059024L;
	final protected static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	public SparkScriptEngine(
			final IAnalyticsContext aleph2_context,
			final Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs,
			final Optional<ProcessingTestSpecBean> test_spec,
			final JavaSparkContext java_spark_context,
			final SparkTopologyConfigBean job_config) {
		super();
		this.aleph2_context = aleph2_context;
		this.inputs = inputs;
		this.test_spec = test_spec;
		this.spark_context = java_spark_context.sc();
		this.java_spark_context = java_spark_context;
		this.job_config = job_config;
	}
	public final IAnalyticsContext aleph2_context;
	public final transient Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs;
	public final transient Optional<ProcessingTestSpecBean> test_spec;
	public final transient SparkContext spark_context;
	public final transient SparkTopologyConfigBean job_config;
	public final transient JavaSparkContext java_spark_context;
	public final transient ObjectMapper mapper = _mapper;
	/** Returns the union of all the provided inputs
	 * @return
	 */
	public JavaPairRDD<Object, Tuple2<Long, IBatchRecord>> allInputs() {
		return inputs.values().stream().reduce((acc1, acc2) -> acc1.union(acc2)).orElse(JavaPairRDD.fromJavaRDD(java_spark_context.emptyRDD()));
	}
	
	/** Emits the object to the bucket's data services
	 * @param json - the JSON object to output
	 * @return
	 */
	public Validation<BasicMessageBean, JsonNode> emit(final JsonNode json) {
		return aleph2_context.emitObject(Optional.empty(), aleph2_context.getJob().get(), Either.left(json), Optional.empty());
	}
	
	/** Emits the object to the another bucket
	 * @param bucket_path - the bucket to whch to emit
	 * @param json - the JSON object to output
	 * @return
	 */
	public Validation<BasicMessageBean, JsonNode> externalEmit(final String bucket_path, final JsonNode json) {
		return aleph2_context.emitObject(Optional.of(BeanTemplateUtils.build(DataBucketBean.class)
					.with(DataBucketBean::full_name, bucket_path)
				.done().get())
				,
				aleph2_context.getJob().get(), Either.left(json), Optional.empty());
	}
	
}
