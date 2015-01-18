package be.ordina.inttest.mdb;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * <p>
 * A simple Message Driven Bean that asynchronously receives and processes the messages that are sent to the queue.
 * </p>
 *
 */
@MessageDriven(name = "SuperQueueMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/SuperQueue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })

public class SuperQueueMDB implements MessageListener {
    private final static Logger LOGGER = Logger.getLogger(SuperQueueMDB.class.toString());

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/topic/SuperTopic")
    private Topic topic;

    /**
     * @see MessageListener#onMessage(Message)
     */
    @Override
	public void onMessage(Message rcvMessage) {
        TextMessage msg = null;
        try {
            if (rcvMessage instanceof TextMessage) {
                msg = (TextMessage) rcvMessage;
                LOGGER.info("Received Message from queue: " + msg.getText());
               	publish(msg);
            } else {
                LOGGER.warning("Message of wrong type: " + rcvMessage.getClass().getName());
            }
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

	private void publish(Message msg) throws JMSException {
		Connection connection=null;
		Session session=null;
		MessageProducer producer=null;

		try {
			connection =  connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = session.createProducer(topic);
			producer.send(msg);
			//LOGGER.info("Replied to message !");
		} finally {
			if (producer!=null)
				producer.close();
			if (session!=null)
				session.close();
			if (connection!=null)
				connection.close();
		}
	}
}
