package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleCfgVo;
import com.channelsoft.ccod.support.cmdb.vo.CcodPlatformAppVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

import org.apache.activemq.BlobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @ClassName: PlatformAppCollectionServiceImpl
 * @Author: lanhb
 * @Description: 平台app收集实现类
 * @Date: 2019/11/15 14:28
 * @Version: 1.0
 */
public class PlatformAppCollectionServiceImpl implements IPlatformAppCollectService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformAppCollectionServiceImpl.class);

    @Value("${cmdb.share_secrete}")
    private String shareSecret;

    @Value("${cmdb.server_name}")
    private String serverName;

    @Value("${cmdb.app_collect.instruction_topic}")
    private String instructionTopic;

    @Value("${cmdb.app_collect.start_instruction}")
    private String startInstruction;

    @Value("${cmdb.app_collect.client_receipt_topic}")
    private String receiptTopic;

    @Value("${cmdb.app_collect.client_report_collect_data_queue}")
    private String reportCollectDataQueue;

    @Value("${cmdb.app_collect.receive_file_queue}")
    private String receiveFileQueue;

    @Value("${activemq.brokerUrl}")
    private String activeMqBrokeUrl;

    private Random random = new Random();

    private String instructionFmt = "{\"serverName\": \"%s\", \"instruction\": \"%s\", \"params\": %s, \"timestamp\": %d, \"nonce\": %d, \"signature\": \"%s\"}";

    private ConnectionFactory connectionFactory = null;

    private Connection connection = null;




    @PostConstruct
    void init() throws Exception
    {
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
        connection.setClientID(this.serverName);
        connection.start();
    }


    @Override
    public CcodPlatformAppVo collectPlatformApp(String platformId) throws Exception {
        logger.debug(String.format("begin to collect %s platform app infos", platformId));
        return null;
    }

    private void notifyPlatformStartCollect(String platformId) throws Exception
    {
        Date now = new Date();
        int timestamp = (int)(now.getTime() / 1000);
        Map<String, String> params = new HashMap<>();
        String instMsg = generateInstructionMsg(startInstruction, params, timestamp);
        logger.debug(String.format("start platform app collect instruction msg is %s", instMsg));
    }

    private String generateInstructionMsg(String instruction, Map<String, String> params, int timestamp)
    {
        int nonce = random.nextInt(1000000);
        String paramsStr = JSONObject.toJSONString(params);
        String plainTxt = this.serverName + instruction + paramsStr + timestamp + nonce + this.shareSecret;
        String signature = DigestUtils.md5DigestAsHex(plainTxt.getBytes());
        String instMsg = String.format(this.instructionFmt, this.serverName, instruction, paramsStr, timestamp, nonce, signature);
        return instMsg;
    }

    private void receiveFileFromQueue(PlatformAppModuleVo[] modules, Session session, String queueName, long timeout) throws Exception
    {
        Map<String, AppModuleCfgVo> cfgMap = new HashMap<>();
        Map<String, List<PlatformAppModuleVo>> installPackageMap = new HashMap<>();
        String cfgKeyFmt = "%s-%s-%s-%s-%s";
        String packageKeyFmt = "%s-%s-%s";
        for(PlatformAppModuleVo vo : modules)
        {
            for(AppModuleCfgVo cfgVo : vo.getCfgs())
            {
                String cfgKey = String.format(cfgKeyFmt, vo.getPlatformId(), vo.getDomainName(), vo.getHostIp(),
                        cfgVo.getFilePath(), cfgVo.getFileName());
                cfgMap.put(cfgKey, cfgVo);
            }
            String installPackageKey = String.format(packageKeyFmt, vo.getModuleName(), vo.getModuleAliasName(), vo.getVersion());
            if(!installPackageMap.containsKey(installPackageKey))
            {
                installPackageMap.put(installPackageKey, new ArrayList<>());
            }
            installPackageMap.get(installPackageKey).add(vo);
        }
        // 创建 Destinatione
        Destination destination = session.createQueue(queueName);

        // 创建 Consumer
        MessageConsumer consumer = session.createConsumer(destination);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof BlobMessage) {
                    BlobMessage blobMessage = (BlobMessage) message;
                    try
                    {
                        String fileName = blobMessage.getStringProperty("FILE.NAME");
                        String fileType = blobMessage.getStringProperty("FILE.TYPE");
                        logger.debug("receive filename=%s, length=%s, fileType=%s",
                                fileName, blobMessage.getLongProperty("FILE.SIZE"), fileType);

                    }
                    catch (Exception ex)
                    {

                    }

                }
            }
        });
    }
}
