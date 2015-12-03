package com.ikanow.aleph2.harvest.script.services;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import scala.Tuple2;

import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.data_services.IStorageService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IUnderlyingService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketStatusBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean.StorageSchemaBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.objects.shared.AssetStateDirectoryBean.StateDirectoryType;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.harvest.script.utils.ProcessUtils;

import fj.data.Either;

public class TestScriptHarvestService {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testOnTest() throws IOException, InterruptedException, ExecutionException {		
		final ScriptHarvestService harvester = new ScriptHarvestService(getFakeStorageService());
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		//override the pid tracking dir
//		ProcessUtils.PID_OUTPUT_DIR = tmp_dir + File.separator + "run" + File.separator;
		final String file_path = tmp_dir + File.separator + "test1";
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		//have to put quotes around the path on windows systems		
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script1", "touch \"" + file_path + "\"", new HashMap<String, String>()), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
		final BasicMessageBean response = future.get();		
		assertTrue(response.message(), response.success());
		
		//test if file was created
		final long curr_time = System.currentTimeMillis();
		while ( System.currentTimeMillis() < curr_time + 5000 ) {
			if ( file.exists() )
				break;
			Thread.sleep(300);
		}
		assertTrue(file.exists());
		
		//cleanup
		file.delete();
	}
	
	@Test
	public void testUserArgs() throws IOException, InterruptedException, ExecutionException, URISyntaxException {		
		final ScriptHarvestService harvester = new ScriptHarvestService(getFakeStorageService());
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		//override the pid tracking dir
//		ProcessUtils.PID_OUTPUT_DIR = tmp_dir + File.separator + "run" + File.separator;
		final String file_path = tmp_dir + File.separator + "test2";
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		//have to put quotes around the path on windows systems		
		final Map<String, String> args = new HashMap<String, String>();
		args.put("arg1", "my_val");
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script1", "touch \"" + file_path + "\"\r\necho \"$arg1\" >> \"" + file_path + "\"", args), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
		final BasicMessageBean response = future.get();		
		assertTrue(response.message(), response.success());
		
		//test if file was created		
		final long curr_time = System.currentTimeMillis();
		while ( System.currentTimeMillis() < curr_time + 5000 ) {
			if ( file.exists() )
				break;
			Thread.sleep(300);
		}
		assertTrue(file.exists());
		//check it has our arg written to it
		final String file_str = IOUtils.toString(new FileInputStream(file), "UTF-8").trim();
		System.out.println(file_str);
		assertTrue(file_str.equals(args.get("arg1")));
		//cleanup
		file.delete();
	}
	
	@Test
	public void testStopScript() throws InterruptedException, ExecutionException {
		final ScriptHarvestService harvester = new ScriptHarvestService(getFakeStorageService());
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		//override the pid tracking dir
//		ProcessUtils.PID_OUTPUT_DIR = tmp_dir + File.separator + "run" + File.separator;
		final String file_path = tmp_dir + File.separator + "test3";
		System.out.println("file: " + file_path);
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		
		//have to put quotes around the path on windows systems
		final String script = new StringBuilder().append("touch \"" + file_path + "\"\r")
				.append("for (( ; ; ))\r")
				.append("do\r")
				.append(" echo \"iteration\" >> \"" + file_path + "\"\r")
				.append(" sleep 1\r")
				.append("done\r")
				.toString();
		final DataBucketBean bucket = getTestbucket("/test/script1", script, new HashMap<String, String>());
		final CompletableFuture<BasicMessageBean> future = harvester.onNewSource(bucket, getFakeContext(), true);
		final BasicMessageBean response = future.get();		
		assertTrue(response.message(), response.success());
		
		//test if file was created
		final long curr_time = System.currentTimeMillis();
		while ( System.currentTimeMillis() < curr_time + 5000 ) {
			if ( file.exists() )
				break;
			Thread.sleep(300);
		}
		assertTrue(file.exists());
		
		//stop the source
		final CompletableFuture<BasicMessageBean> future_stop = harvester.onUpdatedSource(bucket, bucket, false, Optional.empty(), getFakeContext());
		final BasicMessageBean response_stop = future_stop.get();		
		assertTrue(response_stop.message(), response_stop.success());
		
		//test file stopped growing or something
		long last_mod = file.lastModified();
		Thread.sleep(1500);
		assertEquals(file.lastModified(), last_mod);
		
		//cleanup
		file.delete();
		
	}
	
	private static IHarvestContext getFakeContext() {		
		return new IHarvestContext() {
			
			@Override
			public <T> Optional<T> getUnderlyingPlatformDriver(Class<T> driver_class,
					Optional<String> driver_options) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Collection<Object> getUnderlyingArtefacts() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void sendObjectToStreamingPipeline(Optional<DataBucketBean> bucket,
					Either<JsonNode, Map<String, Object>> object) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void logStatusForBucketOwner(Optional<DataBucketBean> bucket,
					BasicMessageBean message) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void logStatusForBucketOwner(Optional<DataBucketBean> bucket,
					BasicMessageBean message, boolean roll_up_duplicates) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void initializeNewContext(String signature) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public String getTempOutputLocation(Optional<DataBucketBean> bucket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public SharedLibraryBean getTechnologyLibraryConfig() {
//				_globals.set(BeanTemplateUtils.from(Optional.ofNullable(context.getTechnologyLibraryConfig().library_config()).orElse(Collections.emptyMap()), ScriptHarvesterConfigBean.class).get());
//				Map<String, Object> library_config = new HashMap<String, Object>();
//				library_config.put("", "")
				return new SharedLibraryBean(null, null, null, null, null, null, null, null, null, null, null);
			}
			
			@Override
			public IServiceContext getServiceContext() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public <S> Optional<ICrudService<S>> getLibraryObjectStore(Class<S> clazz,
					String name_or_id, Optional<String> collection) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Map<String, SharedLibraryBean> getLibraryConfigs() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public CompletableFuture<Map<String, String>> getHarvestLibraries(
					Optional<DataBucketBean> bucket) {
				return CompletableFuture.completedFuture(new HashMap<String,String>());
			}
			
			@Override
			public String getHarvestContextSignature(
					Optional<DataBucketBean> bucket,
					Optional<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>> services) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public List<String> getHarvestContextLibraries(
					Optional<Set<Tuple2<Class<? extends IUnderlyingService>, Optional<String>>>> services) {
				return new ArrayList<String>();
			}
			
			@Override
			public <S> ICrudService<S> getGlobalHarvestTechnologyObjectStore(
					Class<S> clazz, Optional<String> collection) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getFinalOutputLocation(Optional<DataBucketBean> bucket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public CompletableFuture<DataBucketStatusBean> getBucketStatus(
					Optional<DataBucketBean> bucket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public <S> ICrudService<S> getBucketObjectStore(Class<S> clazz,
					Optional<DataBucketBean> bucket, Optional<String> collection,
					Optional<StateDirectoryType> type) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Optional<DataBucketBean> getBucket() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public CompletableFuture<?> flushBatchOutput(Optional<DataBucketBean> bucket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void emitObject(Optional<DataBucketBean> bucket,
					Either<JsonNode, Map<String, Object>> object) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void emergencyQuarantineBucket(Optional<DataBucketBean> bucket,
					String quarantine_duration) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void emergencyDisableBucket(Optional<DataBucketBean> bucket) {
				// TODO Auto-generated method stub
				
			}
		};
	}
	
	private static IStorageService getFakeStorageService() {
		return new IStorageService() {
			
			@Override
			public <T> Optional<T> getUnderlyingPlatformDriver(Class<T> driver_class,
					Optional<String> driver_options) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Collection<Object> getUnderlyingArtefacts() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Tuple2<String, List<BasicMessageBean>> validateSchema(
					StorageSchemaBean schema, DataBucketBean bucket) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getRootPath() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getBucketRootPath() {
				return "/app/aleph2/data/";
			}
		};
	}

	private static DataBucketBean getTestbucket(final String full_name, final String script, final Map<String, String> args) {
		final LinkedHashMap<String, Object> config = new LinkedHashMap<String, Object>();		
		config.put("script", script);
		config.put("args", args);
		final List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
		harvest_configs.add(new HarvestControlMetadataBean("harvester_1", true, null, new ArrayList<String>(), null, config));
		return BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::full_name, full_name)
				.with(DataBucketBean::harvest_configs, harvest_configs)
				.done().get();
	}

}
