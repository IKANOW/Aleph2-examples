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
	var m = new HashMap()
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
