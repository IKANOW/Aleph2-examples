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

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;
















import com.google.common.collect.Multimap;
import com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine;
import com.ikanow.aleph2.analytics.spark.data_model.SparkTopologyConfigBean;
import com.ikanow.aleph2.analytics.spark.services.SparkCompilerService;
import com.ikanow.aleph2.analytics.spark.utils.SparkTechnologyUtils;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.SetOnce;

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
			
			
			final SparkTopologyConfigBean job_config = BeanTemplateUtils.from(context.getJob().map(job -> job.config()).orElse(Collections.emptyMap()), SparkTopologyConfigBean.class).get();			
			final String scala_script = Optional.ofNullable(job_config.script()).orElse("");

			final String wrapper_script = IOUtils.toString(SparkScalaInterpreterTopology.class.getClassLoader().getResourceAsStream("ScriptRunner.scala"), "UTF-8");
			final String to_compile = wrapper_script.replace("USER_SCRIPT", scala_script);
			final SparkCompilerService scs = new SparkCompilerService();
			final Tuple2<ClassLoader, Object> o = scs.buildClass(to_compile, "ScriptRunner");			
			
			Thread.currentThread().setContextClassLoader(o._1());

			test_spec.ifPresent(spec -> System.out.println("OPTIONS: test_spec = " + BeanTemplateUtils.toJson(spec).toString()));

			SparkConf spark_context = new SparkConf().setAppName("SparkPassthroughTopology");
			
			// MAIN PROCESSING
			
			//INFO:
			System.out.println("Starting SparkScalaInterpreterTopology");
			
			logger.optional().ifPresent(l -> {
				l.inefficientLog(Level.INFO, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
						"main", "Starting SparkScalaInterpreterTopology.{0}", Optionals.of(() -> context.getJob().get().name()).orElse("no_name")));
			});
			
			final Method m = o._2().getClass().getMethod("runScript", SparkScriptEngine.class);			
			
			//DEBUG
			//final boolean test_mode = test_spec.isPresent(); // (serializable thing i can pass into the map)

			try (final JavaSparkContext jsc = new JavaSparkContext(spark_context)) {
				final Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs = SparkTechnologyUtils.buildBatchSparkInputs(context, test_spec, jsc, Collections.emptySet());
				
				final SparkScriptEngine script_engine_bridge = new SparkScriptEngine(context, inputs, test_spec, spark_context, job_config);

				m.invoke(o._2(), script_engine_bridge);

				jsc.stop();
				
				logger.optional().ifPresent(l -> {
					l.inefficientLog(Level.INFO, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
							"main", "Stopping SparkScalaInterpreterTopology.{0}", Optionals.of(() -> context.getJob().get().name()).orElse("no_name")));
				});
				
				//INFO:
				System.out.println("Finished interpreter");
			}
		}
		catch (Throwable t) {
			logger.optional().ifPresent(l -> {
				l.inefficientLog(Level.ERROR, ErrorUtils.buildSuccessMessage("SparkScalaInterpreterTopology", 
						"main", ErrorUtils.getLongForm("Error executing SparkScalaInterpreterTopology.unknown: {0}", t)));
			});
			
			System.out.println(ErrorUtils.getLongForm("ERROR: {0}", t));
			System.exit(-1);
		}
	}
}
