package com.ikanow.aleph2.storm.harvest_technology;

import org.checkerframework.checker.nullness.qual.NonNull;

public class StormControllerUtil {
	public static IStormController getLocalStormController() {
		return new LocalStormController();
	}
	
	public static IStormController getRemoteStormController(@NonNull String nimbus_host, @NonNull int nimbus_thrift_port, @NonNull String storm_thrift_transport_plugin) {
		return new RemoteStormController(nimbus_host, nimbus_thrift_port, storm_thrift_transport_plugin);
	}
}
