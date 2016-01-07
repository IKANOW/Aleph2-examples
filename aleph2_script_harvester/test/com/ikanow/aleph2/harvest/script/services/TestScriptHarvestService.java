package com.ikanow.aleph2.harvest.script.services;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.ikanow.aleph2.data_model.interfaces.shared_services.MockServiceContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketStatusBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataSchemaBean.StorageSchemaBean;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.objects.shared.GlobalPropertiesBean;
import com.ikanow.aleph2.data_model.objects.shared.ProcessingTestSpecBean;
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.objects.shared.AssetStateDirectoryBean.StateDirectoryType;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;

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
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		final String file_path = tmp_dir + File.separator + "test1";
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		//have to put quotes around the path on windows systems		
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script1", Optional.of("touch \"" + file_path + "\""), Optional.empty(), Optional.empty(), new HashMap<String, String>(), new ArrayList<String>()), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
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
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		final String file_path = tmp_dir + File.separator + "test2";
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		//have to put quotes around the path on windows systems		
		final Map<String, String> args = new HashMap<String, String>();
		args.put("arg1", "my_val");
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script1", Optional.of("touch \"" + file_path + "\"\necho \"$arg1\" >> \"" + file_path + "\""), Optional.empty(), Optional.empty(), args, new ArrayList<String>()), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
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
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		final String file_path = tmp_dir + File.separator + "test3";
		System.out.println("file: " + file_path);
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		
		//have to put quotes around the path on windows systems
		final String script = new StringBuilder().append("touch \"" + file_path + "\"\n")
				.append("for (( ; ; ))\n")
				.append("do\n")
				.append(" echo \"iteration\" >> \"" + file_path + "\"\n")
				.append(" sleep 1\n")
				.append("done\n")
				.toString();
		final DataBucketBean bucket = getTestbucket("/test/script1", Optional.of(script), Optional.empty(), Optional.empty(), new HashMap<String, String>(), new ArrayList<String>());
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
		
		//test periodicPoll still thinks its running
		assertTrue(harvester.onPeriodicPoll(bucket, getFakeContext()).get().success());
		
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
	
	@Test
	public void testRestartScript() throws InterruptedException, ExecutionException {
		//start up a long running script that:
		//checks if file exists
		//if so, creates a second file
		//if not creates file, spins forever (gets stuck here)		
		
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		final String file_path_1 = tmp_dir + File.separator + "test5_1";
		final String file_path_2 = tmp_dir + File.separator + "test5_2";
		final File file_1 = new File(file_path_1);
		final File file_2 = new File(file_path_2);
		try { file_1.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		try { file_2.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
				
		final String script = new StringBuilder()
				.append("if [ -f "+file_path_1 +" ]\n")
				.append("then").append("\n")
				.append(" touch " + file_path_2).append("\n")
				.append("else").append("\n")
				.append(" touch " + file_path_1).append("\n")
				.append(" while [ : ]").append("\n")
				.append(" do").append("\n")
				.append("  sleep 1").append("\n")
				.append(" done").append("\n")
				.append("fi").append("\n")
				.toString();
		final DataBucketBean bucket = getTestbucket("/test/script1", Optional.of(script), Optional.empty(), Optional.empty(), new HashMap<String, String>(), new ArrayList<String>());
		final CompletableFuture<BasicMessageBean> future = harvester.onNewSource(bucket, getFakeContext(), true);
		final BasicMessageBean response = future.get();		
		assertTrue(response.message(), response.success());
		
		//test if the first file was created
		final long curr_time_1 = System.currentTimeMillis();
		while ( System.currentTimeMillis() < curr_time_1 + 5000 ) {
			if ( file_1.exists() )
				break;
			Thread.sleep(300);
		}
		assertTrue(file_1.exists());
		
		//test periodicPoll still thinks its running
		assertTrue(harvester.onPeriodicPoll(bucket, getFakeContext()).get().success());
		
		//restart the source
		final CompletableFuture<BasicMessageBean> future_restart = harvester.onUpdatedSource(bucket, bucket, true, Optional.empty(), getFakeContext());
		final BasicMessageBean response_restart= future_restart.get();		
		assertTrue(response_restart.message(), response_restart.success());
		
		//test if the 2nd file was created
		final long curr_time_2 = System.currentTimeMillis();
		while ( System.currentTimeMillis() < curr_time_2 + 5000 ) {
			if ( file_2.exists() )
				break;
			Thread.sleep(300);
		}
		assertTrue(file_2.exists());
		
		
		//stop the source
		final CompletableFuture<BasicMessageBean> future_stop = harvester.onUpdatedSource(bucket, bucket, false, Optional.empty(), getFakeContext());
		final BasicMessageBean response_stop = future_stop.get();		
		assertTrue(response_stop.message(), response_stop.success());
		
		//cleanup
		file_1.delete();
		file_2.delete();
	}
	
	@Test
	public void testRunLocalFile() throws InterruptedException, ExecutionException, IOException {
		//save a file to /tmp/somescript.sh and send that as a bucket param, test it works
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		final String file_path = tmp_dir + File.separator + "test1";
		final File file = new File(file_path);
		try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		
		//put the script in a local file
		final String file_script_path = tmp_dir + File.separator + "test1.sh";
		final File file_script = new File(file_script_path);
		try { file_script.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test
		file_script.createNewFile();
		IOUtils.write("touch \"" + file_path + "\"",new FileOutputStream(file_script));
		
		//have to put quotes around the path on windows systems		
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script4", Optional.empty(), Optional.of(file_script_path), Optional.empty(), new HashMap<String, String>(), new ArrayList<String>()), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
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
		file_script.delete();
	}
	
	@Test
	public void testRunResource() throws InterruptedException, ExecutionException {
		//create a file in this package, send as a bucket param, see if it works (dunno if this one is possible)
		//save a file to /tmp/somescript.sh and send that as a bucket param, test it works
				final ScriptHarvestService harvester = new ScriptHarvestService();
				harvester.onInit(getFakeContext());
				
				final String tmp_dir = System.getProperty("java.io.tmpdir");
				final String file_path = tmp_dir + File.separator + "test2";
				final File file = new File(file_path);
				try { file.delete(); } catch (Exception e) {} //cleanup if the file exists from previous test								
				
				//have to put quotes around the path on windows systems		
				final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script4", Optional.empty(), Optional.empty(), Optional.of("resource_test.sh"), new HashMap<String, String>(), new ArrayList<String>()), new ProcessingTestSpecBean(10L, 10L), getFakeContext());
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
	public void testCanRunRequiredAssets() throws IOException {
		//create a source that requires some external assets
		//test canRun returns false because they don't exist
		//create assets
		//test canrun returns true
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");		
		final File asset1 = new File(tmp_dir + File.separator + "asset1");
		final File asset2 = new File(tmp_dir + File.separator + "asset1");
		asset1.delete();
		asset2.delete();
		assertFalse(asset1.exists());
		assertFalse(asset2.exists());
		
		final List<String> required_assets = Arrays.asList(asset1.getAbsolutePath(), asset2.getAbsolutePath());		
		final DataBucketBean test_bucket = getTestbucket("/test/script5", Optional.of("echo hi"), Optional.empty(), Optional.empty(), new HashMap<String, String>(), required_assets);
		assertFalse("should not have local files to run this", harvester.canRunOnThisNode(test_bucket, getFakeContext()));
		
		//create assets
		asset1.createNewFile();
		asset2.createNewFile();
		assertTrue(asset1.exists());
		assertTrue(asset2.exists());
		
		assertTrue("assets should now exist to run this", harvester.canRunOnThisNode(test_bucket, getFakeContext()));
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
				final MockServiceContext context = new MockServiceContext();
				context.addService(IStorageService.class, Optional.empty(), getFakeStorageService());
				context.addGlobals(BeanTemplateUtils.build(GlobalPropertiesBean.class)
								.with(GlobalPropertiesBean::local_root_dir, System.getProperty("java.io.tmpdir") + File.separator)
								.done().get());
				return context;
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
				return System.getProperty("java.io.tmpdir") + File.separator;
			}
			
			@Override
			public String getBucketRootPath() {
				return "/app/aleph2/data/";
			}
		};
	}

	private static DataBucketBean getTestbucket(final String full_name, final Optional<String> script, final Optional<String> local_script_file, Optional<String> resource_file, final Map<String, String> args, final List<String> required_assets) {
		final LinkedHashMap<String, Object> config = new LinkedHashMap<String, Object>();
		if ( script.isPresent())
			config.put("script", script.get());
		if ( local_script_file.isPresent())
			config.put("local_script_url", local_script_file.get());
		if ( resource_file.isPresent()) 
			config.put("resource_name", resource_file.get());
		config.put("args", args);
		config.put("required_assets", required_assets);
		final List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
		harvest_configs.add(new HarvestControlMetadataBean("harvester_1", true, null, new ArrayList<String>(), null, config));
		return BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::full_name, full_name)
				.with(DataBucketBean::harvest_configs, harvest_configs)
				.with(DataBucketBean::owner_id, "test_owner_id1234")
				.done().get();
	}

}
