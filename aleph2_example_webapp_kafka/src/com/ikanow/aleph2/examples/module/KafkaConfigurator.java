package com.ikanow.aleph2.examples.module;

import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * Used by KafkaWebsocketResource to get an instance of itself?
 * 
 * @author Burch
 *
 */
public class KafkaConfigurator extends Configurator {
	
	@Override
	public <T> T getEndpointInstance(Class<T> clazz)
			throws InstantiationException {
		return KafkaGuiceServletContextListener.injector.getInstance(clazz);
	}
}
