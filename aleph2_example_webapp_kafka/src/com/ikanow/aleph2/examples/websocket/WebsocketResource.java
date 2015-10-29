package com.ikanow.aleph2.examples.websocket;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example websocket that just continuously writes message to the socket
 * until the connection is closed.
 * 
 * @author Burch
 *
 */
//http://localhost:8080/aleph2_example_webapp_kafka/TestWebsocket.html
//OR
//WebsocketClient
@ServerEndpoint("/rest/websocket")
public class WebsocketResource {
	private static final Logger _logger = LogManager.getLogger();
	
	@OnOpen
	public void onOpen(Session session) {
		_logger.debug(session.getId() + " has opened a connection");
		try {
			session.getBasicRemote().sendText("Connection Established");
			//start a thread to send messages to this connection constantly
			new Thread(new ConstantOutput(session)).start();
		} catch (IOException ex) {
			_logger.error("error connecting session", ex);
		}
	}
	
	@OnMessage
	public void onMessage(String message, Session session) {
		_logger.debug("Message from " + session.getId() + ": " + message);
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException ex) {
			_logger.error("error sending message", ex);
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		_logger.debug("Session " + session.getId() + " has ended");
	}
	
	@OnError
	public void onError(Throwable t) {
		_logger.error("Error during connection: ", t);
	}
	
	public class ConstantOutput implements Runnable {
		private final Session session;
		private long num_messages = 0;
		public ConstantOutput(Session session) {
			this.session = session;
		}
		
		@Override
		public void run() {
			//send a message to the session every 100ms until it dies
			try {
				while ( session.isOpen() ) {
					session.getBasicRemote().sendText("Message number: " + num_messages++);
					Thread.sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				_logger.error("error sending message", e);
			}
		}
	}
}
