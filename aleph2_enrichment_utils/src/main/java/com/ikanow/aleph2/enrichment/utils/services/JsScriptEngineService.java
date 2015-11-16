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
package com.ikanow.aleph2.enrichment.utils.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import scala.Tuple2;

import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.enrichment.utils.data_model.JsScriptEngineBean;

/** Very simple JS engine (no security or native-JS interface yet)
 * @author Alex
 */
public class JsScriptEngineService implements IEnrichmentBatchModule {
	final static protected Logger _logger = LogManager.getLogger();

	final protected SetOnce<IEnrichmentModuleContext> _context = new SetOnce<>();
	final protected SetOnce<JsScriptEngineBean> _config = new SetOnce<>();
	final protected SetOnce<Boolean> java_api = new SetOnce<>(); // (whether we're using the java api or the JS api)	
	final protected SetOnce<ScriptEngine> _engine = new SetOnce<>();
	final protected SetOnce<ScriptContext> _script_context = new SetOnce<>();
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onStageInitialize(com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext, com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean, boolean)
	 */
	@Override
	public void onStageInitialize(IEnrichmentModuleContext context,
			DataBucketBean bucket, EnrichmentControlMetadataBean control,
			final Tuple2<ProcessingStage, ProcessingStage> previous_next, final Optional<List<String>> grouping_fields)
	{
		// This is currently fixed:
		java_api.set(true);
		
		final JsScriptEngineBean config_bean = BeanTemplateUtils.from(Optional.ofNullable(control.config()).orElse(Collections.emptyMap()), JsScriptEngineBean.class).get();
		_config.trySet(config_bean);
		_context.trySet(context);
		
		// Initialize script engine:
		ScriptEngineManager manager = new ScriptEngineManager();
		_engine.trySet(manager.getEngineByName("JavaScript"));
		_script_context.trySet(_engine.get().getContext()); // (actually not needed since we're compiling things)

		// Load globals:
		_engine.get().put("_a2_global_context", _context.get());
		_engine.get().put("_a2_global_grouping_fields", grouping_fields.orElse(Collections.emptyList()));
		_engine.get().put("_a2_global_previous_stage", previous_next._1().toString());
		_engine.get().put("_a2_global_next_stage", previous_next._2().toString());
		_engine.get().put("_a2_global_bucket", bucket);
		_engine.get().put("_a2_global_config", BeanTemplateUtils.configureMapper(Optional.empty()).convertValue(config_bean.config(), JsonNode.class));
		_engine.get().put("_a2_global_mapper", BeanTemplateUtils.configureMapper(Optional.empty()));
		
		// Load the resources:
		Stream.concat(config_bean.imports().stream(), Stream.of("aleph2_js_globals_before.js", "", "aleph2_js_globals_after.js"))
				.flatMap(Lambdas.flatWrap_i(import_path -> {
					try {
						if (import_path.equals("")) { // also import the user script just before here
							return config_bean.script();
						}
						else return IOUtils.toString(JsScriptEngineService.class.getClassLoader().getResourceAsStream(import_path), "UTF-8");
					}
					catch (Exception e) {
						_logger.error(ErrorUtils.getLongForm("onStageInitialize: {0}", e));		
						throw e; // ignored
					}
				}))
		.forEach(Lambdas.wrap_consumer_i(script -> {
			try {
				_engine.get().eval(script);
			}
			catch (Exception e) {
				_logger.error(ErrorUtils.getLongForm("onStageInitialize: {0}", e));		
				throw e; // ignored
			}
		}));
		;
	}
	
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onObjectBatch(java.util.stream.Stream, java.util.Optional, java.util.Optional)
	 */
	@Override
	public void onObjectBatch(Stream<Tuple2<Long, IBatchRecord>> batch,
			Optional<Integer> batch_size, Optional<JsonNode> grouping_key) {		
		try {
			((Invocable)_engine.get()).invokeFunction("aleph2_global_handle_batch", 
					batch.map(x -> x._2().getJson()), 
					batch_size.orElse(null), 
					grouping_key.orElse(null));
		}
		catch (Exception e) {
			_logger.error(ErrorUtils.getLongForm("onObjectBatch: {0}", e));
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onStageComplete(boolean)
	 */
	@Override
	public void onStageComplete(boolean is_original) {
		//(nothing to do)
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#cloneForNewGrouping()
	 */
	@Override
	public IEnrichmentBatchModule cloneForNewGrouping() {
		final JsScriptEngineService clone = new JsScriptEngineService();
		clone._context.set(this._context.get());
		clone._config.set(this._config.get());
		
		clone.java_api.set(this.java_api.get());
		clone._engine.set(this._engine.get());
		clone._script_context.set(this._script_context.get());

		return clone;
	}	
}
