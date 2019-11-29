package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.service.IActiveMQService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.jms.*;

/**
 * @ClassName: ActiveMQServiceImpl
 * @Author: lanhb
 * @Description: IActiveMQService接口的实现类
 * @Date: 2019/11/18 15:03
 * @Version: 1.0
 */
@Service
public class ActiveMQServiceImpl implements IActiveMQService {

    private final static Logger logger = LoggerFactory.getLogger(ActiveMQServiceImpl.class);

    @Override
    public void sendQueueMsg(Connection connection, String queueName, String textMessage) throws Exception {
        logger.info("to queue " + queueName + " send message:[" + textMessage + "]");
        Session session = null;
        try
        {
            session = connection.createSession(Boolean.TRUE,
                    Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            TextMessage message = session.createTextMessage(textMessage);
            producer.send(message);
            session.commit();
            producer.close();
            session.close();
        }
        catch (Exception e)
        {
            // TODO: handle exception
            logger.error("to queue " + queueName + " send message[" + textMessage + "] FAIL:", e);
            throw new Exception("to queue " + queueName + "send message[" + textMessage + "] FAIL:", e);
        }
        finally
        {
            if(session != null)
            {
                session.close();
            }
        }
    }

    @Override
    public void sendTopicMsg(Connection connection, String topicName, String textMessage) throws Exception {
        logger.info("to topic " + topicName + " send message:[" + textMessage + "]");
        Session session = null;
        try
        {
            session = connection.createSession(Boolean.TRUE,
                    Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createTopic(topicName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            TextMessage message = session.createTextMessage(textMessage);
            producer.send(message);
            session.commit();
            producer.close();
            session.close();
        }
        catch (Exception e)
        {
            // TODO: handle exception
            logger.error("to topic " + topicName + " send message[" + textMessage + "] FAIL:", e);
            throw new Exception("to topic " + topicName + "send message[" + textMessage + "] FAIL:", e);
        }
        finally
        {
            if(session != null)
            {
                session.close();
            }
        }
    }

    @Override
    public String receiveTextMsgFromQueue(Connection connection, String queueName, long timeout) throws Exception {
        logger.debug(String.format("begin to receive text msg from queue=%s with timeout=%d", queueName, timeout));
        Session session = connection.createSession(Boolean.FALSE,
                Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(destination);
        String textMessage = null;
        boolean isSucc = false;
        long timeUsage = 0;
        do {
            Message message = consumer.receive(100);
            if(message != null)
            {
                if(message instanceof TextMessage)
                {
                    textMessage = ((TextMessage) message).getText();
                    isSucc = true;
                }
                break;
            }
            timeUsage += 100;
        }
        while (timeUsage < timeout);
        consumer.close();
        session.close();
        if(isSucc)
        {
            logger.info(String.format("success receive msg=[%s] from queue=%s", textMessage, queueName));
        }
        else if(timeUsage >= timeout)
        {
            logger.error(String.format("receive msg from queue=%s timeout, timeUsage=%s", queueName, timeUsage));
            throw new Exception(String.format("receive msg from queue=%s timeout, timeUsage=%s", queueName, timeUsage));
        }
        else
        {
            logger.error(String.format("receive msg from queue=%s FAIL", queueName));
            throw new Exception(String.format("receive msg from queue=%s FAIL", queueName));
        }
        return textMessage;
    }
}
