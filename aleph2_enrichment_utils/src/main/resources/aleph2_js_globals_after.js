// Globals:
//_a2_global_context - com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext
// _a2_global_grouping_fields - java.util.List of java.lang.String
// _a2_global_previous_stage - java.lang.String
// _a2_global_next_stage - java.lang.String
// _a2_global_config - com.fasterxml.jackson.databind.node.ObjectNode

// System, already declared
//function emit(json)

// User:
// handle_batch_java(json_stream, len, grouping_key)
// handle_batch - TBD

// Provided:
function aleph2_global_handle_batch(batch_record, batch_json, batch_size, grouping_key) {
	if (typeof handle_batch_java === "function") {
		handle_batch_java(batch_json.get(), batch_size, grouping_key);
	}
	else if (typeof handle_batch_java_record === "function") {
		handle_batch_java_record(batch_record.get(), batch_size, grouping_key);		
	}
	else {
		throw "Must implement one of handle_batch_java, handle_batch_java_record";
	}
}