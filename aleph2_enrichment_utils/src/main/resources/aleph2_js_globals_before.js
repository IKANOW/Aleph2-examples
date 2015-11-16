// Globals:
//_a2_global_context - com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext
// _a2_global_grouping_fields - java.util.List of java.lang.String
// _a2_global_previous_stage - java.lang.String
// _a2_global_next_stage - java.lang.String
// _a2_global_config - com.fasterxml.jackson.databind.node.ObjectNode
// _a2_global_mapper - com.fasterxml.jackson.databind.ObjectMapper
// _a2_global_bucket - com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean

// Global methods
function _a2_global_js_to_json(json) {
	if (json instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
		return json;
	}
	else if (json instanceof java.lang.String) {
		return _a2_global_mapper.readTree(json);		
	}
	else if (json instanceof String) {
		return _a2_global_mapper.readTree(String(json));				
	}
	else { //js object 
		var json_str = JSON.stringify(json);
		return _a2_global_mapper.readTree(String(json_str));						
	}	
	
}

// Callbacks:
function _a2_global_emit(json, grouping_field) { // output to next stage in pipeline
	var json_grouping_field = grouping_field
			? java.util.Optional.of(_a2_global_js_to_json(grouping_field))
			: java.util.Optional.empty();
			
	return _a2_global_context.emitMutableObject(0, _a2_global_js_to_json(json), java.util.Optional.empty(), json_grouping_field);
}
function _a2_global_emit_external(bucket_path, json) { // emit to the input of an external bucket (or the current bucket's output, though that's not really intended)
	var bucket = com.ikanow.aleph2.data_model.utils.BeanTemplateUtils.build(com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean.class)
						.with(String("full_name"), String(bucket_path))
					.done().get();
	return _a2_global_context.externalEmit(bucket, Packages.fj.data.Either.left(_a2_global_js_to_json(json)), java.util.Optional.empty());	
}

function _a2_global_to_json(jsonnode) {
	return JSON.parse(jsonnode.toString());
}

function _a2_global_list_to_js(jlist) {
	return Java.from(jlist);
}

function Aleph2Api() {
	this.context = _a2_global_context;
	this.grouping_fields = _a2_global_list_to_js(_a2_global_grouping_fields);
	this.previous_stage = _a2_global_previous_stage;
	this.next_stage = _a2_global_next_stage;
	this.config = _a2_global_to_json(_a2_global_config);
	this.bucket = _a2_global_bucket;
	this.emit = _a2_global_emit;
	this.externalEmit = _a2_global_emit_external;
	this.to_json = _a2_global_to_json;
	this.list_to_js = _a2_global_list_to_js;
}
var _a2 = new Aleph2Api();

