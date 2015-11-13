function handle_batch_java(json_stream, len, grouping_key) {
	
	// Check the imported underscore works
	
	var odds = _.filter([1, 2, 3, 4, 5, 6], function (num) {
	    return num % 2 == 1;
	});	
	
	// Now run the actual code
	
	var it = json_stream.iterator();
	while (it.hasNext()) {
		var json = it.next();
		if (null != len) {
			json.put("len", len);
		}
		_a2.emit(json);
		var json_str = json.toString();
		_a2.emit(json_str);
		var json_js = JSON.parse(json_str);
		if (null != grouping_key) {
			json_js.grouping_key = _a2.to_json(grouping_key);
		}
		_a2.emit(json_js);
		_a2.emit({'prev': _a2.previous_stage, 'next': _a2.next_stage, 'groups': _a2.grouping_fields, 'config': _a2.config });
	}
}
