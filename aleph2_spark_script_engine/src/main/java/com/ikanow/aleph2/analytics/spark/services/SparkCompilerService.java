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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.ikanow.aleph2.analytics.spark.assets.SparkScalaInterpreterTopology;
import com.ikanow.aleph2.core.shared.utils.LiveInjector;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Tuples;

import scala.Tuple2;
import scala.collection.JavaConverters;
import scala.tools.nsc.Global;
import scala.tools.nsc.Settings;
import scala.tools.nsc.Global.Run;

/** Service for compiling scala
 * @author Alex
 *
 */
public class SparkCompilerService {

	/** Compiles a scala class
	 * @param script
	 * @param clazz_name
	 * @return
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public Tuple2<ClassLoader, Object> buildClass(final String script, final String clazz_name) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		final String relative_dir =  "./script_classpath/";
		new File(relative_dir).mkdirs();
		
		final File source_file = new File(relative_dir + clazz_name + ".scala") ;
		FileUtils.writeStringToFile(source_file, script);
		
		final File f = new File(LiveInjector.findPathJar(this.getClass()));
		final File fp = new File(f.getParent());
		final String classpath = Arrays.stream(fp.listFiles()).map(ff -> ff.toString()).filter(ff -> ff.endsWith(".jar")).collect(Collectors.joining(":"));
		
		// Need this to make the URL classloader be used, not the system classloader (which doesn't know about Aleph2..)
		
		final Settings s = new Settings();
		s.classpath().value_$eq(System.getProperty("java.class.path") +  classpath);
		
		s.usejavacp().scala$tools$nsc$settings$AbsSettings$AbsSetting$$internalSetting_$eq(true);
		final Global g = new Global(s);
		final Run r = g.new Run();
		r.compile(JavaConverters.asScalaBufferConverter(Arrays.<String>asList(source_file.toString())).asScala().toList());
		
		FileUtils.moveFile(new File(clazz_name + ".class"), new File(relative_dir + clazz_name + ".class"));
		
		final Tuple2<ClassLoader, Object> o = Lambdas.get(Lambdas.wrap_u(() -> {
			try (URLClassLoader cl = new java.net.URLClassLoader(Arrays.asList(new File(relative_dir).toURI().toURL()).toArray(new URL[0]), Thread.currentThread().getContextClassLoader())) {
				Object o_ = cl.loadClass("ScriptRunner").newInstance();
				return Tuples._2T(cl, o_);
			}
		}));
		return o;
	}
	
	public static void main(String[] args) throws Exception {
		
		final String scala_script = "";
		final String wrapper_script = IOUtils.toString(SparkScalaInterpreterTopology.class.getClassLoader().getResourceAsStream("ScriptRunner.scala"), "UTF-8");
		final String to_compile = wrapper_script.replace("USER_SCRIPT", scala_script);
		
		final SparkCompilerService scs = new SparkCompilerService();
		final Tuple2<ClassLoader, Object> t2 = scs.buildClass(to_compile, "ScriptRunner");
		System.out.println(t2._2().getClass().getClassLoader());
	}
	
}
