package com.ikanow.aleph2.examples.websocket;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IServiceContext;
import com.ikanow.aleph2.distributed_services.services.ICoreDistributedServices;
import com.ikanow.aleph2.examples.module.KafkaConfigurator;

/**
 * Open up the CDS and stream a topic in
 * 
 * @author Burch
 *
 */
//http://localhost:8080/aleph2_example_webapp_kafka/TestKafkaWebsocket.html
//OR
//KafkaClient
@ServerEndpoint(value="/rest/websocket/kafka/{topic}", configurator=KafkaConfigurator.class)
public class KafkaWebsocketResource {
	private static final Logger _logger = LogManager.getLogger();
	protected final IServiceContext _context;
	protected final ICoreDistributedServices _core_dist;
	
	@Inject
	public KafkaWebsocketResource(IServiceContext context) {
		_context = context;
		_core_dist = _context.getService(ICoreDistributedServices.class, Optional.empty()).get();
	}
	
	@OnOpen
	public void onOpen(Session session, @PathParam("topic") String topic) {
		_logger.debug(session.getId() + " has opened a connection");
		try {
			session.getBasicRemote().sendText("Connection Established");
			//start a thread to send messages to this connection constantly
			new Thread(new KafkaOutput(session, topic, _core_dist)).start();
			//new Thread(new KafkaTopicWriter(topic, _core_dist)).start();
		} catch (IOException ex) {
			_logger.error("error connecting session", ex);
		}
		_logger.debug("done opening a connection");
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
	
	@GET
	@Path("{message}")
	public String streamKafkaTopic(@PathParam("topic") String topic, @PathParam("message") String message) {
		_logger.debug("producing topic: " + topic + " message: " + message);
		_core_dist.produce(topic, message);
		return "success";
	}
	
	/**
	 * Grabs an iterator for the given topic and continuously tries to read from it.  Sends any
	 * messages to the session.
	 * 
	 * @author Burch
	 *
	 */
	public class KafkaOutput implements Runnable {
		private final Session session;
		private final String topic;
		private final ICoreDistributedServices _core_dist;
		public KafkaOutput(Session session, String topic, ICoreDistributedServices _core_dist) {
			this.session = session;
			this.topic = topic;
			this._core_dist = _core_dist;
		}
		
		@Override
		public void run() {
			//send a message to the session every 100ms until it dies
			_logger.debug("getting iterator for topic: " + topic);
			Iterator<String> consumer = _core_dist.consumeAs(topic, Optional.of("websocket_" + session.getId()));			
			try {
				_logger.debug("starting looping on iter");
				while ( consumer.hasNext() ) {		
					_logger.debug("iter had another message");
					session.getBasicRemote().sendText(consumer.next());
					Thread.sleep(100);
				}
				_logger.debug("done looping on iter, I don't think this can be reached (iter blocks while waiting for next entry");
			} catch (IOException | InterruptedException e) {
				_logger.error("error sending message", e);
			}
			_logger.debug("Thread done, closing kafka output");
		}
	}
	
	/**
	 * Writes messages constantly to the given topic.  Can be turned on in the
	 * onOpen call if you want some sample data (and don't want to use the KafkaWriteResource call)
	 * 
	 * @author Burch
	 *
	 */
	public class KafkaTopicWriter implements Runnable {
		private final String topic;
		private final ICoreDistributedServices _core_dist;
		private long num_messages = 0;
		public KafkaTopicWriter(String topic, ICoreDistributedServices _core_dist) {
			this.topic = topic;
			this._core_dist = _core_dist;
		}
		
		@Override
		public void run() {
			//send a message to the kafka queue every 100ms until it dies
			_logger.debug("writing messages to topic: " + topic);			
			try {
				while (true) {
					_core_dist.produce(topic, "message number: " + num_messages++);
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				_logger.error("error writing to topic: " + topic, e);
			}
			_logger.debug("Thread done, closing kafka writer");
		}
	}
}
