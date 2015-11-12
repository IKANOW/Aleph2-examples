// Globals:
//_a2_global_context - com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext
// _a2_global_grouping_fields - java.util.List of java.lang.String
// _a2_global_previous_stage - java.lang.String
// _a2_global_next_stage - java.lang.String
// _a2_global_config - com.fasterxml.jackson.databind.node.ObjectNode
// _a2_global_mapper - com.fasterxml.jackson.databind.ObjectMapper

// Global methods

// Callbacks:
function _a2_global_emit(json) {
	// 2 cases: a) ObjectNode, b) JSON
	
	if (json instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
		_a2_global_context.emitMutableObject(0, json, java.util.Optional.empty(), java.util.Optional.empty());
	}
	else if (json instanceof java.lang.String) {
		_a2_global_context.emitMutableObject(0, _a2_global_mapper.readTree(json), java.util.Optional.empty(), java.util.Optional.empty());		
	}
	else if (json instanceof String) {
		_a2_global_context.emitMutableObject(0, _a2_global_mapper.readTree(String(json)), java.util.Optional.empty(), java.util.Optional.empty());				
	}
	else { //js object 
		var json_str = JSON.stringify(json);
		_a2_global_context.emitMutableObject(0, _a2_global_mapper.readTree(String(json_str)), java.util.Optional.empty(), java.util.Optional.empty());						
	}	
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
	this.emit = _a2_global_emit;
	this.to_json = _a2_global_to_json;
	this.list_to_js = _a2_global_list_to_js;
}
var _a2 = new Aleph2Api();

