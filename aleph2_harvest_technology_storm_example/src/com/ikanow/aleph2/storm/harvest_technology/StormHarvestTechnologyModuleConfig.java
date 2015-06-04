package com.ikanow.aleph2.storm.harvest_technology;

public class StormHarvestTechnologyModuleConfig {
	public String nimbus_host;
	public int nimbus_thrift_port = 0;
	public String thrift_transport_plugin = "backtype.storm.security.auth.SimpleTransportPlugin";
}
