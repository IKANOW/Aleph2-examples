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
package com.ikanow.aleph2.analytics.spark.assets;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;



























import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine;
import com.ikanow.aleph2.analytics.spark.data_model.SparkTopologyConfigBean;
import com.ikanow.aleph2.analytics.spark.services.SparkCompilerService;
import com.ikanow.aleph2.analytics.spark.utils.SparkTechnologyUtils;
import com.ikanow.aleph2.core.shared.utils.LiveInjector;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean.MasterEnrichmentType;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.SetOnce;

import fj.data.Either;

/** Very simple spark topology, runs the compiled script
 * /app/aleph2/library/spark_script.jar
 * @author Alex
 */
public class SparkScalaInterpreterTopology {
	
	// Params:
	
	//(not needed)
	//final static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		final SetOnce<IBucketLogger> logger = new SetOnce<>();
		
		try {			
			final Tuple2<IAnalyticsContext, Optional<ProcessingTestSpecBean>> aleph2_tuple = SparkTechnologyUtils.initializeAleph2(args);
			final IAnalyticsContext context = aleph2_tuple._1();
			final Optional<ProcessingTestSpecBean> test_spec = aleph2_tuple._2();

			logger.set(context.getLogger(context.getBucket()));
			
			// Optional: make really really sure it exists after the specified timeout
			SparkTechnologyUtils.registerTestTimeout(test_spec, () -> {
				System.exit(0);
			});
			
			//INFO:
			System.out.println("Starting SparkScalaInterpreterTopology logging=" + logger.optional().isPresent());
			
			logger.optional().ifPresent(l -> {
				l.inefficientLog(Level.INFO, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
						"main", "Starting SparkScalaInterpreterTopology.{0}", Optionals.of(() -> context.getJob().get().name()).orElse("no_name")));
			});
			
			final SparkTopologyConfigBean job_config = BeanTemplateUtils.from(context.getJob().map(job -> job.config()).orElse(Collections.emptyMap()), SparkTopologyConfigBean.class).get();			
			final String scala_script = Optional.ofNullable(job_config.script()).orElse("");

			final String wrapper_script = IOUtils.toString(SparkScalaInterpreterTopology.class.getClassLoader().getResourceAsStream("ScriptRunner.scala"), "UTF-8");
			final String to_compile = wrapper_script.replace("USER_SCRIPT", scala_script);
			final SparkCompilerService scs = new SparkCompilerService();
			final Tuple2<ClassLoader, Object> o = scs.buildClass(to_compile, "ScriptRunner", logger.optional());			
			
			Thread.currentThread().setContextClassLoader(o._1());

			test_spec.ifPresent(spec -> System.out.println("OPTIONS: test_spec = " + BeanTemplateUtils.toJson(spec).toString()));

			SparkConf spark_context = new SparkConf().setAppName("SparkPassthroughTopology");
			
			final long streaming_batch_interval = (long) spark_context.getInt(SparkTopologyConfigBean.STREAMING_BATCH_INTERVAL, 10);
			
			// MAIN PROCESSING
			
			final Method m = o._2().getClass().getMethod("runScript", SparkScriptEngine.class);			
			
			//DEBUG
			//final boolean test_mode = test_spec.isPresent(); // (serializable thing i can pass into the map)

			boolean is_streaming = context.getJob().map(j -> j.analytic_type()).map(t -> MasterEnrichmentType.streaming == t).orElse(false);
			final Either<JavaSparkContext, JavaStreamingContext> jsc = Lambdas.get(() -> {		
				return is_streaming 
						? Either.<JavaSparkContext, JavaStreamingContext>right(new JavaStreamingContext(spark_context, Durations.seconds(streaming_batch_interval))) 
						: Either.<JavaSparkContext, JavaStreamingContext>left(new JavaSparkContext(spark_context))
						;
			});			
			try {
				final JavaSparkContext jsc_batch = jsc.either(l->l, r->r.sparkContext());
				
				final Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs = 
						SparkTechnologyUtils.buildBatchSparkInputs(context, test_spec, jsc_batch, Collections.emptySet());
				
				final Multimap<String, JavaPairDStream<String, Tuple2<Long, IBatchRecord>>> streaming_inputs =
						jsc.<Multimap<String, JavaPairDStream<String, Tuple2<Long, IBatchRecord>>>>either(
								l -> HashMultimap.<String, JavaPairDStream<String, Tuple2<Long, IBatchRecord>>>create()
								, 
								r -> SparkTechnologyUtils.buildStreamingSparkInputs(context, test_spec, r, Collections.emptySet()));
				
				final SparkScriptEngine script_engine_bridge = new SparkScriptEngine(context, inputs, streaming_inputs, test_spec, jsc_batch, jsc.either(l->null, r->r), job_config);

				// Add driver and generated JARs to path:
				jsc_batch.addJar(LiveInjector.findPathJar(o._2().getClass()));
				
				m.invoke(o._2(), script_engine_bridge);

				jsc.either(l -> { l.stop(); return null; }, r -> { r.stop(); return null; });
				
				logger.optional().ifPresent(l -> {
					l.inefficientLog(Level.INFO, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
							"main", "Stopping SparkScalaInterpreterTopology.{0}", Optionals.of(() -> context.getJob().get().name()).orElse("no_name")));
				});
				
				//INFO:
				System.out.println("Finished interpreter");
			}
			finally {
				jsc.either(l -> { l.close(); return null; }, r -> { r.close(); return null; });
			}
			logger.optional().ifPresent(Lambdas.wrap_consumer_u(l -> l.flush().get(10, TimeUnit.SECONDS)));
		}
		catch (Throwable t) {
			logger.optional().ifPresent(l -> {
				l.inefficientLog(Level.ERROR, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
						"main", ErrorUtils.getLongForm("Error executing SparkScalaInterpreterTopology.unknown: {0}", t)));
			});
			
			System.out.println(ErrorUtils.getLongForm("ERROR: {0}", t));
			logger.optional().ifPresent(Lambdas.wrap_consumer_u(l -> l.flush().get(10, TimeUnit.SECONDS)));
			System.exit(-1);
		}
	}
}
