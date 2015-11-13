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
package com.ikanow.aleph2.examples.client.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;




import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




import java.util.function.Supplier;




import com.google.inject.Inject;
import com.ikanow.aleph2.data_model.interfaces.data_services.IManagementDbService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.ICrudService;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.data_model.objects.shared.AssetStateDirectoryBean;
import com.ikanow.aleph2.data_model.objects.shared.SharedLibraryBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils.UpdateComponent;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.CrudUtils.QueryComponent;
import com.ikanow.aleph2.data_model.utils.Tuples;
import com.ikanow.aleph2.examples.client.data_model.ExampleBean;

/** Example service illustrating use of the CRUD service
 * @author Alex
 */
public class ExampleClientService {
	static final Logger _logger = LogManager.getLogger(ExampleClientService.class);
	
	protected final IServiceContext _context;
	protected final IManagementDbService _core_mgmt_db;
	
	public static final String COLLECTION_ROOT = "/example/app/";
	public static final SharedLibraryBean CRUD_LOCATION = BeanTemplateUtils.build(SharedLibraryBean.class).with(SharedLibraryBean::path_name, COLLECTION_ROOT).done().get();
	
	/** User/guice constructor
	 * @param context
	 */
	@Inject
	public ExampleClientService(IServiceContext context) {
		_context = context;
		
		_core_mgmt_db = _context.getCoreManagementDbService();
		// One annoyance that we're working on is that you can't access some of the services' methods in the constructor, so you have to do something like this
		// ModuleUtils.getAppInjector().thenAccept(__ -> _bucket_crud_service = _core_mgmt_db.readOnlyVersion().getBucketCrudService())
	}
	
	/** Illustrate use of the CRUD service
	 * @param collection_name
	 * @param _id
	 * @param new_value
	 */
	public CompletableFuture<Void> doSomething(String collection_name, String _id, String new_value) {
		
		// 1) Get the collection		
		
		final ICrudService<ExampleBean> crud_service = _core_mgmt_db.getPerLibraryState(ExampleBean.class, CRUD_LOCATION, Optional.of(collection_name));
		
		// 2) Example: see if the object already exists:
		
		return crud_service.getObjectById(_id).thenAccept(opt_return_val -> {
			//(or to do synchronously ie blocking you'd just do "blah = crud_service.getObjectById().get()"
			
			if (opt_return_val.isPresent()) { // we'll overwrite it

				final ExampleBean saved = opt_return_val.get();
				
				// Example of modifying an object, even though it's not really needed:
				
				final ExampleBean update = BeanTemplateUtils.clone(saved).with(ExampleBean::value, new_value).done();

				// Since we're going to (artificially) order the object, let's make sure we've optimized the query first
				final String value_field_name = BeanTemplateUtils.from(ExampleBean.class).field(ExampleBean::value);
				crud_service.optimizeQuery(Arrays.asList(value_field_name));
				
				final QueryComponent<ExampleBean> get_this_object = 
						CrudUtils.allOf(ExampleBean.class).when(ExampleBean::_id, _id)
						.orderBy(Tuples._2T(value_field_name, 1))
						.limit(1)
						;
				
				final UpdateComponent<ExampleBean> update_this_object =
						CrudUtils.update(ExampleBean.class).set(ExampleBean::value, update.value());
				
				final CompletableFuture<Boolean> ret_val = crud_service.updateObjectBySpec(get_this_object, Optional.of(false), update_this_object);
				
				try {
					final boolean b = ret_val.get();
					_logger.info(ErrorUtils.get("Updated object {0}: {1} to {2} success={3}", _id, saved.value(), update.value(), b));
				}
				catch (Exception e) {
					_logger.error(ErrorUtils.getLongForm("UPDATE: Collection = {1} id = {2} error = {0}", e.getCause(), collection_name, _id));					
				}
				
			}
			else { // create it
				
				// (or you could just use a constructor!)
				final ExampleBean to_save = BeanTemplateUtils.build(ExampleBean.class)
																.with(ExampleBean::_id, _id)
																.with(ExampleBean::value, new_value)
															.done().get();
				
				final CompletableFuture<Supplier<Object>> future = crud_service.storeObject(to_save);
				try {
					future.get();
					_logger.info(ErrorUtils.get("Created object {0}: {1}", _id, to_save.value()));
				}
				catch (Exception e) {
					_logger.error(ErrorUtils.getLongForm("CREATE: Collection = {1} id = {2} error = {0}", e.getCause(), collection_name, _id));					
				}
			}			
		});
	}
	
	/** Make a list of all the collections used
	 * @return
	 */
	public CompletableFuture<List<String>> enumerate_collections_async() {
		
		// 1) Get the collection directory		
		
		final ICrudService<AssetStateDirectoryBean> crud_service = _core_mgmt_db.getPerLibraryState(AssetStateDirectoryBean.class, CRUD_LOCATION, Optional.empty());
		
		final QueryComponent<AssetStateDirectoryBean> all_query = CrudUtils.allOf(AssetStateDirectoryBean.class);
		
		return crud_service.getObjectsBySpec(all_query).thenApply(cursor -> {
			return StreamSupport
					.stream(cursor.spliterator(), false)
					.map(dir_entry -> dir_entry.collection_name())
					.collect(Collectors.toList())
					;
		});
	}
	
	/** Make a list of all the collections used
	 * @return
	 */
	public List<String> enumerate_collections_sync() {
		try {
			return enumerate_collections_async().get();
		}
		catch (Exception e) { 
			_logger.error(ErrorUtils.getLongForm("{0}", e.getCause()));
			return Collections.emptyList(); // (or whatever)
		}
	}
}
