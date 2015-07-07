var globalText = "GlobalText";

function printState(){
		print("GlobalScript");
}


function check(){

	var ipRange ="192";
	return (_ip.substring(0, ipRange.length) === ipRange);
}

function splitIP(){
	var HashMap = Java.type("java.util.HashMap")
	var m = new HashMap()
	var pos = _ip.lastIndexOf(".");
	if(pos>-1){
		m.put("network",_ip.substring(0, pos));
		m.put("subnet",_ip.substring(pos+1));
	}
	print("Script M "+_ip+":"+m);
	return m;
}
