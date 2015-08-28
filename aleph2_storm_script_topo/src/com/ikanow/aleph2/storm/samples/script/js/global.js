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
//TODO put this somewhere else
//var globalText = "GlobalText";
//
//function printState(){
//		print("GlobalScript");
//}
//
//
//function check(){
//
//	var ipRange ="192";
//	return (_ip.substring(0, ipRange.length) === ipRange);
//}
//
//function splitIP(){
//	var ipObj = eval('(' + _ip + ')');
//	//print("IPOBj:"+ipObj["ip"]);
//	var HashMap = Java.type("java.util.HashMap")
//	var m = new HashMap();
//	var ipNo = ipObj["ip"]; 
//	var pos = ipNo.lastIndexOf(".");
//	if(pos>-1){
//		m.put("ipNo",ipNo);
//		m.put("network",ipNo.substring(0, pos));
//		m.put("subnet",ipNo.substring(pos+1));
//	}
//	print("Script M "+ipNo+":"+m);
//	return m;
//}
//
//// functions for Mapper and Folder, topology2
//function map(jsonIn){
//	
//	var jsonObObj = eval('(' + jsonIn + ')');
//	var HashMap = Java.type("java.util.HashMap")
//	var m = new HashMap();
//
//	// custom code
//	var ipNo = jsonObObj["ip"]; 
//	var pos = ipNo.lastIndexOf(".");
//	if(pos>-1){
//		m.put("mapKey",ipNo);
//
//		var mapValue = {};		
//		mapValue["ip"]= ipNo;
//		var network = ipNo.substring(0, pos);
//		mapValue["network"]= network;
//		mapValue["subnet"] = ipNo.substring(pos+1);		
//
//		// put in network as mapkey
//		m.put("mapKey",network);
//		m.put("mapValueJson",JSON.stringify(mapValue));
//	}
//	print("Script M "+ipNo+":"+m);
//	return m;
//}
//
////user specific update function
//function update(mapKey,state){
//	// count for now
//	
//	var count = state
//
//	if (count == null){
//		count = 0;
//	}
//	count++;
//	return count;
//}
//
//function fold(mapKey,mapValueJson){
//	var mapValueObj = eval('(' + mapValueJson + ')');
//	print("fold mapKey="+mapKey+" , mapValueJson="+mapValueJson);
//	var state = _map.get(mapKey);
//	var newState = update(mapKey, state);
//	store(mapKey, newState);
//	
//}
// user specific function returns object to be emitted or null
//function checkEmit(mapKey,state) {
//	// TODO modfify threshold
//	var threshold = 2;
//
//	var count = state;
//	
//	if(count >= threshold){
//		print("checkEmit mapKey="+mapKey+" , count="+count+",threshold="+threshold);
//		var countObj = {};
//		countObj["mapKey"]=mapKey;
//		countObj["mapValue"]=mapValueJson;
//		countObj["count"]=count;
//		
//		emit(JSON.stringify(countObj));
//	}	
//	return null;
//	
//}
//

// USER TO IMPLEMENT ARE:

//function map(jsonIn)
//function fold(mapKey,mapValueJson)
//function checkEmit(mapKey,state)

//init map
var HashMap = Java.type("java.util.HashMap")
var _map = new HashMap();

function reset(mapKey) {
	_map.put(mapKey, 0); 
}

function store(key,state) {
	_map.put(key, state);	
}

function allEntries() {
	return _map; 
}

function emit(objToEmit){
	com.ikanow.aleph2.storm.samples.bolts.JavaScriptFolderBolt.emit(_collector,_tuple, objToEmit);
}
