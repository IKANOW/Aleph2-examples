package com.ikanow.aleph2.harvest.script.services;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import scala.Tuple2;

import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.data_model.interfaces.data_import.IHarvestContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IUnderlyingService;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketStatusBean;
import com.ikanow.aleph2.data_model.objects.data_import.HarvestControlMetadataBean;
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
		final ScriptHarvestService harvester = new ScriptHarvestService();
		harvester.onInit(getFakeContext());
		
		final String tmp_dir = System.getProperty("java.io.tmpdir");
		//override the pid tracking dir
		ProcessUtils.PID_OUTPUT_DIR = tmp_dir + File.separator + "run" + File.separator;
		final String file_path = tmp_dir + File.separator + "test1";
		//have to put quotes around the path on windows systems		
		final CompletableFuture<BasicMessageBean> future = harvester.onTestSource(getTestbucket("/test/script1", "touch \"" + file_path + "\""), new ProcessingTestSpecBean(10L, 10L), null);
		final BasicMessageBean response = future.get();		
		assertTrue(response.message(), response.success());
		
		//test if file was created
		final File file = new File(file_path);
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
				// TODO Auto-generated method stub
				return null;
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
				// TODO Auto-generated method stub
				return null;
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

	private static DataBucketBean getTestbucket(final String full_name, final String script) {
		final LinkedHashMap<String, Object> config = new LinkedHashMap<String, Object>();		
		config.put("script", script);
		final List<HarvestControlMetadataBean> harvest_configs = new ArrayList<HarvestControlMetadataBean>();
		harvest_configs.add(new HarvestControlMetadataBean("harvester_1", true, null, new ArrayList<String>(), null, config));
		return BeanTemplateUtils.build(DataBucketBean.class)
				.with(DataBucketBean::full_name, full_name)
				.with(DataBucketBean::harvest_configs, harvest_configs)
				.done().get();
	}

}
