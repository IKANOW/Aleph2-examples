package com.ikanow.aleph2.examples.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Example client that connects to the kafka websocket, spits out any messages it receives on the topic "test_topic"
 * 
 * @author Burch
 *
 */
@ClientEndpoint
public class KafkaClient {
	public static void main(String[] args) {
		try {
			// open websocket
            final KafkaClient clientEndPoint = new KafkaClient(new URI("ws://localhost:8080/aleph2_example_webapp_kafka/rest/websocket/kafka/test_topic"));

            // add listener
            clientEndPoint.addMessageHandler(new KafkaClient.MessageHandler() {
                public void handleMessage(String message) {
                    System.out.println(message);
                }
            });

            //just sit on a socket and listen for messages
            while ( true ) {
            	//do nothing, wait for messages to come in
            }
            
            // send message to websocket
//            clientEndPoint.sendMessage("{'event':'addChannel','channel':'ok_btccny_ticker'}");
		} catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
	}
	
	Session userSession = null;
	private MessageHandler messageHandler;
	
	public KafkaClient(URI websocket_uri) {
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, websocket_uri);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@OnOpen
	public void onOpen(Session userSession) {
		System.out.println("opening websocket");
        this.userSession = userSession;
	}
	
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		System.out.println("closing websocket");
        this.userSession = null;
	}
	
	@OnMessage
	public void onMessage(String message) {
		if ( this.messageHandler != null ) {
			this.messageHandler.handleMessage(message);
		}
	}
	
	public void addMessageHandler(MessageHandler msgHandler) {
		this.messageHandler = msgHandler;
	}
	
	public void sendMessage(String message) {
		this.userSession.getAsyncRemote().sendText(message);
	}
	
	public static interface MessageHandler {
		public void handleMessage(String message);
	}
}
