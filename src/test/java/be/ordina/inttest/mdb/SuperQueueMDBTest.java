package be.ordina.inttest.mdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SuperQueueMDBTest {
    private static final String MSG_BODY = "PING";

	private static final int CONSUMER_WAIT_MAX = 5000;

	@Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/queue/SuperQueue")
    private Queue queue;

    @Resource(mappedName = "java:/topic/SuperTopic")
    private Topic topic;

	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class)
				.addClass(SuperQueueMDB.class)
				.addAsManifestResource(
						new File("./src/main/webapp/WEB-INF/hornetq-jms.xml"));
	}

	protected void sleepUtilTrue(AtomicBoolean status, long maxWait) {
		long currWait=0;
		while (!status.get() && currWait < maxWait) {
			try {
				Thread.sleep(100);
				currWait+=100;
			} catch(final InterruptedException ignored) {
				/* Ignored */
			}
		}
	}

	@Test
	public void testTextMessagesArePublishedToTopic() throws Exception {
		Connection connection=null;
		Session session=null;
		MessageProducer producer=null;

		TopicConnection topicConnection=null;
		TopicSession topicSession=null;
		TopicSubscriber subscriber=null;
		final AtomicBoolean gotMessage=new AtomicBoolean(false);


		try {
			connection = connectionFactory.createConnection();
			topicConnection = ((TopicConnectionFactory) connectionFactory).createTopicConnection();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

	        producer = session.createProducer(queue);
	        subscriber = topicSession.createSubscriber(topic);
	        subscriber.setMessageListener(new MessageListener() {
				@Override
				public void onMessage(Message message) {
					final TextMessage tmsg=(TextMessage) message;
					String body=null;
					try {
						body = tmsg.getText();
						System.out.println("Got topic message:"+body);
					} catch (final JMSException e) {
						e.printStackTrace();
					}
					assertEquals(MSG_BODY, body);
					gotMessage.set(true);
				}
			});

	        final TextMessage message = session.createTextMessage();
	        message.setJMSReplyTo(queue);
	        message.setText(MSG_BODY);

	        topicConnection.start();
	        assertFalse("Got topic message", gotMessage.get());
	        producer.send(message);
	        // Sleep until we got topic message or 5sec
	        sleepUtilTrue(gotMessage, 5000);
	        assertTrue("Got topic message", gotMessage.get());

		} finally {
			if (producer!=null)
				producer.close();
			if (session!=null)
				session.close();
			if (connection!=null)
				connection.close();

			if (subscriber!=null)
				subscriber.close();
			if (topicSession!=null)
				topicSession.close();
			if (topicConnection!=null)
				topicConnection.close();

		}
	}
}
