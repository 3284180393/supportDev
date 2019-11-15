package com.channelsoft.ccod.support.cmdb.service;

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
     * @param queueName queue名
     * @param textMessage 需要发送的消息
     * @throws Exception
     */
    void sendQueueMsg(String queueName, String textMessage) throws Exception;
    /**
     * 向某个topic发送消息
     * @param textMessage topic名
     * @param textMessage 需要发送的消息
     * @throws Exception
     */
    void sendTopicMsg(String topicName, String textMessage) throws Exception;
}
