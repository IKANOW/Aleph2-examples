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

package com.ikanow.aleph2.analytics.spark.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.ikanow.aleph2.analytics.spark.assets.SparkScalaInterpreterTopology;
import com.ikanow.aleph2.core.shared.utils.LiveInjector;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.data_model.utils.Tuples;

import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.tools.nsc.Global;
import scala.tools.nsc.Settings;
import scala.tools.nsc.Global.Run;
import scala.tools.nsc.reporters.StoreReporter;

/** Service for compiling scala
 * @author Alex
 *
 */
public class SparkCompilerService {

	final SetOnce<URLClassLoader> _cl = new SetOnce<>();
	
	/** Compiles a scala class
	 * @param script
	 * @param clazz_name
	 * @return
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public Tuple2<ClassLoader, Object> buildClass(final String script, final String clazz_name, final Optional<IBucketLogger> logger) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		final String relative_dir =  "./script_classpath/";
		new File(relative_dir).mkdirs();
		
		final File source_file = new File(relative_dir + clazz_name + ".scala") ;
		FileUtils.writeStringToFile(source_file, script);
		
		final String this_path = LiveInjector.findPathJar(this.getClass(), "./dummy.jar");
		final File f = new File(this_path);
		final File fp = new File(f.getParent());
		final String classpath = Arrays.stream(fp.listFiles()).map(ff -> ff.toString()).filter(ff -> ff.endsWith(".jar")).collect(Collectors.joining(":"));
		
		// Need this to make the URL classloader be used, not the system classloader (which doesn't know about Aleph2..)
		
		final Settings s = new Settings();
		s.classpath().value_$eq(System.getProperty("java.class.path") +  classpath);
		s.usejavacp().scala$tools$nsc$settings$AbsSettings$AbsSetting$$internalSetting_$eq(true);
		final StoreReporter reporter = new StoreReporter();
		final Global g = new Global(s, reporter);
		final Run r = g.new Run();
		r.compile(JavaConverters.asScalaBufferConverter(Arrays.<String>asList(source_file.toString())).asScala().toList());

		if (reporter.hasErrors() || reporter.hasWarnings()) {
			final String errors = "Compiler: Errors/warnings (**to get to user script line substract 22**): " + JavaConverters.asJavaSetConverter(reporter.infos()).asJava().stream().map(info -> info.toString()).collect(Collectors.joining(" ;; "));
			logger.ifPresent(l -> l.log(reporter.hasErrors() ? Level.ERROR : Level.DEBUG, false, () -> errors, () -> "SparkScalaInterpreterTopology", () -> "compile"));

			//ERROR:
			if (reporter.hasErrors()) {
				System.err.println(errors);
			}
		}
		// Move any class files (eg including sub-classes)
		Arrays.stream(fp.listFiles()).filter(ff -> ff.toString().endsWith(".class")).forEach(Lambdas.wrap_consumer_u(ff -> {
			FileUtils.moveFile(ff, new File(relative_dir + ff.getName()));
		}));

		// Create a JAR file...
		
		  Manifest manifest = new Manifest();
		  manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		final JarOutputStream target = new JarOutputStream(new FileOutputStream("./script_classpath.jar"), manifest);
		Arrays.stream(new File(relative_dir).listFiles()).forEach(Lambdas.wrap_consumer_i(ff -> {
			JarEntry entry = new JarEntry(ff.getName());
			target.putNextEntry(entry);
			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(ff))) {
			    byte[] buffer = new byte[1024];
			    while (true)
			    {
			      int count = in.read(buffer);
			      if (count == -1)
			        break;
			      target.write(buffer, 0, count);
			    }
			    target.closeEntry();			 				
			}
		}));
		target.close();

		final String created = "Created = " + new File("./script_classpath.jar").toURI().toURL() + " ... " + Arrays.stream(new File(relative_dir).listFiles()).map(ff -> ff.toString()).collect(Collectors.joining(";"));
		logger.ifPresent(l -> l.log(Level.DEBUG, false, () -> created, () -> "SparkScalaInterpreterTopology", () -> "compile"));
		
		final Tuple2<ClassLoader, Object> o = Lambdas.get(Lambdas.wrap_u(() -> {
			_cl.set(new java.net.URLClassLoader(Arrays.asList(new File("./script_classpath.jar").toURI().toURL()).toArray(new URL[0]), Thread.currentThread().getContextClassLoader()));
			Object o_ = _cl.get().loadClass("ScriptRunner").newInstance();
			return Tuples._2T(_cl.get(), o_);
		}));
		return o;
	}
	
	public static void main(String[] args) throws Exception {
		
		final String scala_script = "";
		final String wrapper_script = IOUtils.toString(SparkScalaInterpreterTopology.class.getClassLoader().getResourceAsStream("ScriptRunner.scala"), "UTF-8");
		final String to_compile = wrapper_script.replace("USER_SCRIPT", scala_script);
		
		final SparkCompilerService scs = new SparkCompilerService();
		final Tuple2<ClassLoader, Object> t2 = scs.buildClass(to_compile, "ScriptRunner", Optional.empty());
		System.out.println(t2._2().getClass().getClassLoader());
		
		//OK so java -cp "jar1;jar2;.." works here, BUT java -cp "./;jar1;./jar2" *doesn't*
		
		java.net.URLClassLoader cl = ((java.net.URLClassLoader)t2._1());
		System.out.println(t2._2().getClass().getClassLoader());
		
		cl.loadClass("ScriptRunner$$anon$1");
	}
	
}
