var globalText = "GlobalText";

function printState(){
		print("GlobalScript");
}


function check(){

	var ipRange ="192";
	return (_ip.substring(0, ipRange.length) === ipRange);
}

function splitIP(){
	var ipObj = eval('(' + _ip + ')');
	//print("IPOBj:"+ipObj["ip"]);
	var HashMap = Java.type("java.util.HashMap")
	var m = new HashMap();
	var ipNo = ipObj["ip"]; 
	var pos = ipNo.lastIndexOf(".");
	if(pos>-1){
		m.put("ipNo",ipNo);
		m.put("network",ipNo.substring(0, pos));
		m.put("subnet",ipNo.substring(pos+1));
	}
	print("Script M "+ipNo+":"+m);
	return m;
}

// functions for Mapper and folder
function map(jsonIn){
	
	var jsonObObj = eval('(' + jsonIn + ')');
	var HashMap = Java.type("java.util.HashMap")
	var m = new HashMap();

	// custom code
	var ipNo = jsonObObj["ip"]; 
	var pos = ipNo.lastIndexOf(".");
	if(pos>-1){
		m.put("mapKey",ipNo);

		var mapValue = {};		
		mapValue["ip"]= ipNo;
		var network = ipNo.substring(0, pos);
		mapValue["network"]= network;
		mapValue["subnet"] = ipNo.substring(pos+1);		

		// put in network as mapkey
		m.put("mapKey",network);
		m.put("mapValueJson",JSON.stringify(mapValue));
	}
	print("Script M "+ipNo+":"+m);
	return m;
}

//init map
var HashMap = Java.type("java.util.HashMap")
var _map = new HashMap();
var threshold = 1;

function fold(mapKey,mapValueJson){
	var mapValueObj = eval('(' + mapValueJson + ')');
	print("fold mapKey="+mapKey+" , mapValueJson="+mapValueJson);
	
	update(mapKey,mapValueJson);
}

function update(mapKey,mapValueJson){
	// count for now
	var count = _map.get(mapKey);
	print("update mapKey="+mapKey+" , count="+count);

	if (count == null){
		count = 0;
	}
	count++;
	_map.put(mapKey, count);	
}

function checkEmit(mapKey,mapValueJson) {	
	var count = _map.get(mapKey);
	
	print("checkEmit mapKey="+mapKey+" , count="+count+",threshold="+threshold);
	if(count >= threshold){
		var countObj = {};
		countObj["mapKey"]=mapKey;
		countObj["mapValue"]=mapValueJson;
		countObj["count"]=count;
		return JSON.stringify(countObj);
	}	
	return null;
	
}

function store(key,state) {
	_map.put(key, state);	
}

function allEntries() {
	return _map; 
}

