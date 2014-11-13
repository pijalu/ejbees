package be.ordina.inttest.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * A simple servlet 3 as client that sends several messages to a queue or a topic.
 * </p>
 * 
 * <p>
 * The servlet is registered and mapped to /SuperServletClient using the {@linkplain WebServlet
 * @HttpServlet}.
 * </p>
 * 
 * 
 */
@WebServlet("/SuperServletClient")
public class SuperServletClient extends HttpServlet {
    private static final long serialVersionUID = -8314035702649252239L;
    private static AtomicInteger counter=new AtomicInteger(1);

    private static final int MSG_COUNT = 5;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/queue/SuperQueue")
    private Queue queue;

    @Resource(mappedName = "java:/topic/SuperTopic")
    private Topic topic;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        Connection connection = null;
        out.write("<h1>Quickstart: This example demonstrates the use of <strong>JMS 1.1</strong> and <strong>EJB 3.1 Message-Driven Bean</strong> in Red Hat JBoss Enterprise Application Platform 6.</h1>");
        try {
            Destination destination;
            if (req.getParameterMap().keySet().contains("topic")) {
                destination = topic;
            } else {
                destination = queue;
            }
            out.write("<p>Sending messages to <em>" + destination + "</em></p>");
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = session.createProducer(destination);
            connection.start();
            out.write("<h2>Following messages will be send to the destination:</h2>");
            TextMessage message = session.createTextMessage();
            for (int i = 0; i < MSG_COUNT; i++) {
            	int val=counter.getAndAdd(1);
                message.setText("This is message " + val);
                messageProducer.send(message);
                out.write("Message (" + val + "): " + message.getText() + "</br>");
            }
            out.write("<p><i>Go to your JBoss EAP server console or log to see the result of messages processing</i></p>");
            out.write("<a href='SuperServletClient'>Send on queue</a></br><a href='SuperServletClient?topic'>Send on topic</a>");
        } catch (JMSException e) {
            e.printStackTrace();
            out.write("<h2>A problem occurred during the delivery of this message</h2>");
            out.write("</br>");
            out.write("<p><i>Go your the JBoss EAP server console or log to see the error stack trace</i></p>");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                out.close();
            }
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

}
