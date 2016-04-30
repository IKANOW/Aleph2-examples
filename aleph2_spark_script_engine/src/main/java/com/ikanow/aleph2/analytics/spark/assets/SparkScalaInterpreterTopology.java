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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.repl.SparkCommandLine;
import org.apache.spark.repl.SparkILoop;
import org.apache.spark.repl.SparkIMain;

import scala.Tuple2;









import scala.collection.JavaConverters;

import com.google.common.collect.Multimap;
import com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine;
import com.ikanow.aleph2.analytics.spark.data_model.SparkTopologyConfigBean;
import com.ikanow.aleph2.analytics.spark.utils.SparkTechnologyUtils;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.core.shared.utils.LiveInjector;

/** Very simple spark topology, runs the compiled script
 * /app/aleph2/library/spark_script.jar
 * @author Alex
 */
public class SparkScalaInterpreterTopology {

	// Params:
	
	//(not needed)
	//final static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		try {			
			final Tuple2<IAnalyticsContext, Optional<ProcessingTestSpecBean>> aleph2_tuple = SparkTechnologyUtils.initializeAleph2(args);
			final IAnalyticsContext context = aleph2_tuple._1();
			final Optional<ProcessingTestSpecBean> test_spec = aleph2_tuple._2();

			// Optional: make really really sure it exists after the specified timeout
			SparkTechnologyUtils.registerTestTimeout(test_spec, () -> {
				System.exit(0);
			});
			
			
			final SparkTopologyConfigBean job_config = BeanTemplateUtils.from(context.getJob().map(job -> job.config()).orElse(Collections.emptyMap()), SparkTopologyConfigBean.class).get();
			
			final String scala_script = Optional.ofNullable(job_config.script()).orElse("");
			
			final String wrapper_script = IOUtils.toString(SparkScalaInterpreterTopology.class.getClassLoader().getResourceAsStream("SparkWrapper.scala"), "UTF-8");

			test_spec.ifPresent(spec -> System.out.println("OPTIONS: test_spec = " + BeanTemplateUtils.toJson(spec).toString()));

			// MAIN PROCESSING
			
			//INFO:
			System.out.println("Starting SparkScalaInterpreterTopology");
			
			final SparkILoop spark_shell = new SparkILoop();
			
			//TODO: have to call process to finish everything...
			
			//spark_shell.settings_$eq(new SparkCommandLine(JavaConverters.asScalaBufferConverter(Arrays.<String>asList()).asScala().toList()).settings());
			//spark_shell.createInterpreter();
			//final SparkIMain interpreter = spark_shell.intp();	
			// (can't call this or command...)
			//spark_shell.initializeSpark();
			//final SparkContext spark_context = spark_shell.createSparkContext();
			
			final File temp = File.createTempFile("temp-file-name", ".tmp"); 
			
			FileUtils.writeStringToFile(temp, "jsc = new org.apache.spark.api.java.JavaSparkContext(sc)\n", false);
			FileUtils.writeStringToFile(temp, "aleph2_tuple = com.ikanow.aleph2.analytics.spark.utils.SparkTechnologyUtils.initializeAleph2(args)\n", true);
			FileUtils.writeStringToFile(temp, "exit()\n", true);
			
			//spark_shell.settings_$eq(new SparkCommandLine(JavaConverters.asScalaBufferConverter(Arrays.<String>asList("-i", temp.toString())).asScala().toList()).settings());			
			
			spark_shell.process(Arrays.<String>asList("-i", temp.toString()).toArray(new String[0]));
			//TODO: this runs into a bunch of issues:
			//NPE x2
			//at org.apache.spark.repl.SparkIMain$ReadEvalPrint.call(SparkIMain.scala:1065)
			//...
			//at org.apache.spark.repl.SparkILoop.initializeSpark(SparkILoop.scala:64)			
			
			//<console>:10: error: not found: value sqlContext
			// TODO this might be because i'm not including the SQL stuff!?
			//import sqlContext.implicits._
			
			//TODO: this looks like it might just be a syntax error in how i'm calling the code
//			Loading /raidarray/hadoop/yarn/local/usercache/tomcat/appcache/application_1460755414739_2534/container_e29_1460755414739_2534_01_000001/tmp/temp-file-name1920982671167695709.tmp...
//			<console>:16: error: not found: value jsc
//			val $ires3 = jsc
//			             ^
//			<console>:13: error: not found: value jsc
//			       jsc = new org.apache.spark.api.java.JavaSparkContext(sc)aleph2_tuple = com.ikanow.aleph2.analytics.spark.utils.SparkTechnologyUtils.initializeAleph2(args)exit()
//			       ^			
			
			//TODO: try to do stuff?
			
			//DEBUG
			//final boolean test_mode = test_spec.isPresent(); // (serializable thing i can pass into the map)
			
//			try (final JavaSparkContext jsc = new JavaSparkContext(spark_context)) {
//	
//				final Multimap<String, JavaPairRDD<Object, Tuple2<Long, IBatchRecord>>> inputs = SparkTechnologyUtils.buildBatchSparkInputs(context, test_spec, jsc, Collections.emptySet());
//				
//				//TODO
//				final SparkScriptEngine script_engine_bridge = new SparkScriptEngine(context, inputs, test_spec, null, job_config);
//				
//				/**/
//				System.out.println("__a2");
//				interpreter.bind(
//						"__a2",
//						"com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine",
//						script_engine_bridge,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//				
//				
//
//				
//				/**/
//				//???
//				interpreter.initializeSynchronous();
//				
//				/**/
//				//OK this just inserts an import at the top of the method
//				//interpreter.addImports(JavaConverters.asScalaBufferConverter(Arrays.<String>asList("com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine")).asScala().toList());
//				
//				final String this_path = LiveInjector.findPathJar(script_engine_bridge.getClass());
//				
//				/**/
//				System.out.println("??1 " + this_path);
//				
//				final File this_url = new File(this_path);				
//
//				/**/
//				System.out.println("??2 " + this_url.toURL());				
//				
//				try {
//					System.out.println(JavaConverters.asScalaBufferConverter(Arrays.<URL>asList(this_url.toURL())).asScala().toList().toString());
//					interpreter.addUrlsToClassPath(JavaConverters.asScalaBufferConverter(Arrays.<URL>asList(this_url.toURL())).asScala().toList());					
//				}
//				catch (Exception e) {
//					System.out.println("add classpath fail " +  ErrorUtils.getLongForm("{0}", e));	
//				}
//				
//				
//				
//				/**/
////				System.out.println("??3 " + interpreter.				
//
////				interpreter.addUrlsToClassPath(JavaConverters.asScalaBufferConverter(Arrays.<URL>asList(this_url.toURL())).asScala().toList());
//	
//				/**/
//				System.out.println("?? = " + interpreter.settings().toString());
//				/**/
//				System.out.println("??a = " + interpreter.parentClassLoader());
//				/**/
//				System.out.println("??b = " + interpreter.classLoader());
//				
//				interpreter.reset();				
//				
//				/**/
//				System.out.println("??a = " + interpreter.parentClassLoader());
//				/**/
//				System.out.println("??b = " + interpreter.classLoader());
//
//				try {
//					System.out.println("class1 = " + interpreter.classLoader().findClass("com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine"));
//				}
//				catch (Exception e) {
//					System.out.println("NO CLASS FOUND1" + e.getMessage());
//				}
//				/**/
//				try {
//					System.out.println("class2 = " + this_url.getClass().getClassLoader().loadClass("com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine"));
//				}
//				catch (Exception e) {
//					System.out.println("NO CLASS FOUND2: " + ErrorUtils.getLongForm("{0}", e));
//				}
//				try {
//					System.out.println("class1 = " + interpreter.classLoader().findClass("com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext"));
//				}
//				catch (Exception e) {
//					System.out.println("NO CLASS FOUND1" + e.getMessage());
//				}
//				/**/
//				try {
//					System.out.println("class2 = " + this_url.getClass().getClassLoader().loadClass("com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext"));
//				}
//				catch (Exception e) {
//					System.out.println("NO CLASS FOUND2: " + " ? " + this_url.getClass().getClassLoader() + " > " + ErrorUtils.getLongForm("{0}", e));
//				}
//				
//				/**/
//				System.out.println("__a2");
//				interpreter.bind(
//						"__a2",
//						"com.ikanow.aleph2.analytics.spark.data_model.SparkScriptEngine",
//						script_engine_bridge,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//				
//				// Don't need any of this, just pass it across
//				/**/
//				System.out.println("inputs");
//				interpreter.bind(
//						"inputs",
//						"com.google.common.collect.Multimap[String, org.apache.spark.api.java.JavaPairRDD[Object, Tuple2[Long, com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord]]]",
//						inputs,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//				
//				/**/
//				System.out.println("aleph2_context");
//				interpreter.bind(
//						"aleph2_context",
//						"com.ikanow.aleph2.data_model.interfaces.data_analytics.IAnalyticsContext",
//						context,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//				
//				/**/
//				System.out.println("spark_context");
//				interpreter.bind(
//						"spark_context",
//						"org.apache.spark.SparkConf",
//						spark_context,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//
//				/**/
//				System.out.println("job_config");
//				interpreter.bind(
//						"job_config",
//						"com.ikanow.aleph2.analytics.spark.data_model.SparkTopologyConfigBean",
//						job_config,
//						scala.collection.JavaConversions.asScalaBuffer(Arrays.<String>asList()).toList()
//						);
//				
//				/**/
//				System.out.println("?? " +
//				interpreter
//						);
//				
//				/**/
//				System.out.println("COMPILING\n" + wrapper_script.replace("USER_SCRIPT", scala_script));
//				
////				interpreter.
//				interpreter.compileString(wrapper_script.replace("USER_SCRIPT", scala_script)); // script must be in the form
//				
//				/**/
//				System.out.println("COMPILED");
//				
//				interpreter.interpret("ScriptRunner.runScript(__a2)");
//				
//				/**/
//				System.out.println("RUNNING");
//				
//				//interpreter.compile(scala_script); // (don't work since has to be multi line)
//				//interpreter.compileString("object Test { def main( args:Array[String] ): Unit = println(inputs.entries().iterator().next().getValue().count()) }");
//				/**/
//				interpreter.interpret("println(inputs.entries().iterator().next().getValue().count())");
//
//				jsc.stop();
//				
//				//INFO:
//				System.out.println("Finished interpreter");
//			}
		}
		catch (Throwable t) {
			System.out.println(ErrorUtils.getLongForm("ERROR: {0}", t));
		}
	}
}
