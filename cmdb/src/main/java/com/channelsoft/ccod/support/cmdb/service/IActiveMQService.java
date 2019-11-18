package com.channelsoft.ccod.support.cmdb.service;

import javax.jms.Connection;
import javax.jms.Session;

/**
 * @ClassName: IActiveMQService
 * @Author: lanhb
 * @Description: 定义activemq的服务接口
 * @Date: 2019/11/14 18:05
 * @Version: 1.0
 */
public interface IActiveMQService {
    /**
     * 向某个queue发送消息
     * @param connection actvieMQ连接
     * @param queueName queue名
     * @param textMessage 需要发送的消息
     * @throws Exception
     */
    void sendQueueMsg(Connection connection, String queueName, String textMessage) throws Exception;
    /**
     * 向某个topic发送消息
     * @param connection  activeMQ连接
     * @param topicName topic名
     * @param textMessage 需要发送的消息
     * @throws Exception
     */
    void sendTopicMsg(Connection connection, String topicName, String textMessage) throws Exception;

    /**
     * 从指定queue上接受消息
     * @param connection activeMQ连接
     * @param queueName queue名
     * @param timeout 接受消息超时时长
     * @return 接受到的消息
     * @throws Exception
     */
    String receiveTextMsgFromQueue(Connection connection, String queueName, long timeout) throws Exception;
}
