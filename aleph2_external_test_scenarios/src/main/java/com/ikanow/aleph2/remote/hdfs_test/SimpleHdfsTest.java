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
package com.ikanow.aleph2.remote.hdfs_test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.apache.hadoop.security.AccessControlException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.utils.ModuleUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SimpleHdfsTest {

	@Inject
	public IServiceContext _service_context;
	
	public static void main(final String args[]) throws Exception {
		if (args.length < 0) {
			System.out.println("CLI:");
			System.exit(-1);
		}
		
		final String temp_dir = System.getProperty("java.io.tmpdir") + File.separator;
		
		Config config = ConfigFactory.parseReader(new InputStreamReader(SimpleHdfsTest.class.getResourceAsStream("/test_hdfs_remote.properties")))
				.withValue("globals.local_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_cached_jar_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				//.withValue("globals.distributed_root_dir", ConfigValueFactory.fromAnyRef(temp_dir))
				.withValue("globals.local_yarn_config_dir", ConfigValueFactory.fromAnyRef(temp_dir + "/yarn-config"))
				;
		
		final SimpleHdfsTest app = new SimpleHdfsTest();
		
		final Injector app_injector = ModuleUtils.createTestInjector(Arrays.asList(), Optional.of(config));	
		app_injector.injectMembers(app);
		
		app.runTest();
		System.exit(0);
	}
	
	public void runTest() throws AccessControlException, FileNotFoundException, UnsupportedFileSystemException, IllegalArgumentException, IOException {
		final String temp_dir = System.getProperty("java.io.tmpdir") + File.separator;
		
		final IStorageService storage = _service_context.getStorageService();
		
		final FileContext fc = (FileContext) storage.getUnderlyingPlatformDriver(FileContext.class, Optional.empty()).get();

		final FileContext lfc = (FileContext) storage.getUnderlyingPlatformDriver(FileContext.class, IStorageService.LOCAL_FS).get();
		
		System.out.println("FILES IN BUCKET ROOT");
		
		final RemoteIterator<LocatedFileStatus> it = fc.util().listFiles(new Path(storage.getBucketRootPath()), true);
		boolean first = true;
		while (it.hasNext()) {
			final LocatedFileStatus lfs = it.next();
			if (first) {
				first = false;
				lfc.util().copy(lfs.getPath(), lfc.makeQualified(new Path(temp_dir + "ALEX.txt")));
			}
			System.out.println(lfs);
		}
		
		System.out.println("FILES/DIRS IN BUCKET ROOT");
		
		Stream<FileStatus> dirstream = Arrays.stream(fc.util().listStatus(new Path(storage.getBucketRootPath())));
		
		dirstream.forEach(fs -> System.out.println(fs));
	}
}
