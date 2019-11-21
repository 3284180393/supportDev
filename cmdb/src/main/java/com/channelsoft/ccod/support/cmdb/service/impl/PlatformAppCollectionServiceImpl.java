package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.service.IActiveMQService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

/**
 * @ClassName: PlatformAppCollectionServiceImpl
 * @Author: lanhb
 * @Description: 平台app收集实现类
 * @Date: 2019/11/15 14:28
 * @Version: 1.0
 */
@Service
public class PlatformAppCollectionServiceImpl implements IPlatformAppCollectService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformAppCollectionServiceImpl.class);

    @Value("${cmdb.share_secret}")
    private String shareSecret;

    @Value("${cmdb.server_name}")
    private String serverName;

    private String instructionTopicFmt = "CMDB_TO_%s_INSTRUCTION";

    @Value("${cmdb.app_collect.start_data_collect_instruction}")
    private String startCollectDataInstruction;

    @Value("${cmdb.app_collect.start_file_transfer_instruction}")
    private String startAppFileTransferInstruction;

    @Value("${cmdb.app_collect.client_receipt_topic}")
    private String receiptTopic;

    private String reportCollectDataQueueFmt = "CLIENT_REPORT_COLLECT_DATA_%s";

    private String receiveFileQueueFmt = "FILE_REC_%s";

    @Value("${cmdb.app_collect.receipt_timeout}")
    private long receiptTimeout;

    @Value("${cmdb.app_collect.collect_data_timeout}")
    private long collectDataTimeout;

    @Value("${cmdb.app_collect.transfer_file_timeout}")
    private long transferFileTimeout;

    @Value("${cmdb.app_collect.file_type}")
    private String transferFileType;

    @Value("${cmdb.app_collect.app_name}")
    private String transferAppName;

    @Value("${cmdb.app_collect.app_alias}")
    private String transferAppAlias;

    @Value("${cmdb.app_collect.app_version}")
    private String transferAppVersion;

    @Value("${cmdb.app_collect.platformId}")
    private String transferPlatformId;

    @Value("${cmdb.app_collect.domainName}")
    private String transferDomainName;

    @Value("${cmdb.app_collect.host_ip}")
    private String transferHostIp;

    @Value("${cmdb.app_collect.base_path}")
    private String transferBasePath;

    @Value("${cmdb.app_collect.deploy_path}")
    private String transferDeployPath;

    @Value("${cmdb.app_collect.file_name}")
    private String transferFileName;

    @Value("${cmdb.app_collect.file_size}")
    private String transferFileSize;

    @Value("${cmdb.app_collect.file_md5}")
    private String transferFileMd5;

    @Value("${cmdb.app_collect.install_package}")
    private String transferInstallPackage;

    @Value("${cmdb.app_collect.cfg}")
    private String transferCfg;

    @Value("${activemq.brokerUrl}")
    private String activeMqBrokeUrl;

    @Value("${activemq.receive_time_span}")
    private long activeMqReceiveTimeSpan;

    @Autowired
    IActiveMQService activeMQService;

    private Random random = new Random();

    private ConnectionFactory connectionFactory = null;

    private String cfgKeyFmt = "%s;%s;%s;%s;%s;%s";

    private String packageKeyFmt = "%s;%s;%s";

    private String tmpSaveDirFmt = "%s/downloads/%s";

    private String tmpSavePathFmt = "%s/%s";

    @Override
    public List<PlatformAppModuleVo> checkPlatformAppData(String platformId, String domainName, String hostIp, String appName, String version) throws Exception {
        if(StringUtils.isBlank(platformId))
        {
            logger.error(String.format("checkPlatformAppData FAIL : platformId is blank"));
            throw new Exception(String.format("checkPlatformAppData FAIL : platformId is blank"));
        }
        Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        if(StringUtils.isNotBlank(domainName))
        {
            params.put("domainName", domainName);
        }
        if(StringUtils.isNotBlank(hostIp))
        {
            params.put("hostIp", hostIp);
        }
        if(StringUtils.isNotBlank(appName))
        {
            params.put("appName", appName);
        }
        if(StringUtils.isNotBlank(version))
        {
            params.put("version", version);
        }
        logger.debug(String.format("begin to collect %s platform app infos, params=%s", platformId, JSONObject.toJSONString(params)));
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID(this.serverName);
        connection.start();
        List<PlatformAppModuleVo> modules = collectPlatformAppData(platformId, params, connection);
        return modules;
    }

    @PostConstruct
    void init() throws Exception
    {
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
//        connection.setClientID(this.serverName);
//        connection.start();
    }


    @Override
    public List<PlatformAppModuleVo> collectPlatformAppData(String platformId, String domainName, String hostIp, String appName, String version) throws Exception {
        if(StringUtils.isBlank(platformId))
        {
            logger.error(String.format("collectPlatformAppData FAIL : platformId is blank"));
            throw new Exception(String.format("collectPlatformAppData FAIL : platformId is blank"));
        }
        Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        if(StringUtils.isNotBlank(domainName))
        {
            params.put("domainName", domainName);
        }
        if(StringUtils.isNotBlank(hostIp))
        {
            params.put("hostIp", hostIp);
        }
        if(StringUtils.isNotBlank(appName))
        {
            params.put("appName", appName);
        }
        if(StringUtils.isNotBlank(version))
        {
            params.put("version", version);
        }
        logger.debug(String.format("begin to collect %s platform app infos, params=%s", platformId, JSONObject.toJSONString(params)));
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID(this.serverName);
        connection.start();
        List<PlatformAppModuleVo> modules = collectPlatformAppData(platformId, params, connection);
        params = new HashMap<>();
        params.put("platformId", platformId);
        modules = getPlatformAppInstallPackageAndCfg(platformId, modules, params, connection);
        return modules;
    }


    /**
     * 收集平台模块数据
     * @param platformId 平台id
     * @param params 相关参数,如果需要指定域名、host ip或是模块名可以在params中添加,其中platformId为必须
     * @param connection 到activeMQ的连接
     * @return 收集到的指定条件的模块信息
     * @throws Exception
     */
    private List<PlatformAppModuleVo> collectPlatformAppData(String platformId, Map<String, String> params, Connection connection) throws Exception
    {
        logger.debug(String.format("begin to collect %s platform app data : params=%s", platformId, JSONObject.toJSONString(params)));
        Date now = new Date();
        int timestamp = (int)(now.getTime() / 1000);
        int nonce = random.nextInt(1000000);
        String md5 = DigestUtils.md5DigestAsHex(String.format("%d:%d", timestamp, nonce).getBytes());
        String collectDataQueue = String.format(this.reportCollectDataQueueFmt, md5);
        params.put("collectDataQueue", collectDataQueue);
        ActiveMQInstructionVo instructionVo = new ActiveMQInstructionVo(this.startCollectDataInstruction, JSONObject.toJSONString(params),
                timestamp, nonce);
        String signature = instructionVo.generateSignature(this.shareSecret);
        boolean verify = instructionVo.verifySignature(this.shareSecret);
        System.out.println("##########self verfify=" + verify + "#####################");
        String jsonStr = JSONObject.toJSONString(instructionVo);
        ActiveMQInstructionVo instrVo = JSONObject.parseObject(jsonStr, ActiveMQInstructionVo.class);
        verify = instrVo.verifySignature(this.shareSecret);
        System.out.println("&&&&&&&&&&&&&&&self verfify=" + verify + "&&&&&&&&&&&&&&&&&&&&");
        logger.debug(String.format("start platform app collect instruction msg is %s and signature=%s",
                JSONObject.toJSONString(instructionVo), signature));
        String instructionTopic = String.format(this.instructionTopicFmt, platformId);
        this.activeMQService.sendTopicMsg(connection, instructionTopic, JSONObject.toJSONString(instructionVo));
        String clientRet = activeMQService.receiveTextMsgFromQueue(connection, collectDataQueue, this.receiptTimeout);
        InstructionResultVo resultVo = JSONObject.parseObject(clientRet, InstructionResultVo.class);
        boolean verifySucc = verifyInstructionResult(resultVo, instructionVo);
        if(!verifySucc)
        {
            String errMsg = String.format("verify receipt message from %s at %s FAIL", collectDataQueue, this.activeMqBrokeUrl);
            logger.error(errMsg);
            throw new Exception(errMsg);
        }
        if(!resultVo.isSuccess())
        {
            logger.error(String.format("client return execute instruction=%s FAIL : %s", instructionVo.getInstruction(), resultVo.getData()));
            throw new Exception(String.format("%s", resultVo.getData()));
        }
        clientRet = activeMQService.receiveTextMsgFromQueue(connection, collectDataQueue, this.collectDataTimeout);
        resultVo = JSONObject.parseObject(clientRet, InstructionResultVo.class);
        verifySucc = verifyInstructionResult(resultVo, instructionVo);
        if(!verifySucc)
        {
            String errMsg = String.format("verify %s app collect message from %s at %s FAIL",
                    platformId, collectDataQueue, this.activeMqBrokeUrl);
            logger.error(errMsg);
            throw new Exception(errMsg);
        }
        if(!resultVo.isSuccess())
        {
            logger.error(String.format("client return collect %s app data FAIL : %s", platformId, resultVo.getData()));
            throw new Exception(String.format("%s", resultVo.getData()));
        }
        List<PlatformAppModuleVo> modules = JSONArray.parseArray(resultVo.getData(), PlatformAppModuleVo.class);
        logger.info(String.format("%s report %d app info", platformId, modules.size()));
        return modules;
    }


    /**
     * get指定条件的模块的安装包和配置文件
     * @param platformId 平台id
     * @param modules 指定的模块
     * @param params 相关参数,如果需要指定域名、host ip或是模块名可以在params中添加,其中platformId为必须
     * @param connection 指定的activeMQ连接
     * @return 指定条件的模块的安装包和配置文件
     * @throws Exception
     */
    private List<PlatformAppModuleVo> getPlatformAppInstallPackageAndCfg(String platformId, List<PlatformAppModuleVo> modules, Map<String, String> params, Connection connection) throws Exception
    {
        logger.debug(String.format("begin to transfer %s platform app file : params=%s", platformId, JSONObject.toJSONString(params)));
        Date now = new Date();
        int timestamp = (int)(now.getTime() / 1000);
        int nonce = random.nextInt(1000000);
        String md5 = DigestUtils.md5DigestAsHex(String.format("%d:%d", timestamp, nonce).getBytes());
        String recvFileQueue = String.format(this.receiveFileQueueFmt, md5);
        params.put("receiveFileQueue", recvFileQueue);
        ActiveMQInstructionVo instructionVo = new ActiveMQInstructionVo(this.startAppFileTransferInstruction, JSONObject.toJSONString(params),
                timestamp, nonce);
        String signature = instructionVo.generateSignature(this.shareSecret);
        logger.debug(String.format("start platform app file transfer instruction msg is %s and signature=%s",
                JSONObject.toJSONString(instructionVo), signature));
        String instructionTopic = String.format(this.instructionTopicFmt, platformId);
        this.activeMQService.sendTopicMsg(connection, instructionTopic, JSONObject.toJSONString(instructionVo));
        String clientRet = activeMQService.receiveTextMsgFromQueue(connection, recvFileQueue, this.receiptTimeout);
        InstructionResultVo resultVo = JSONObject.parseObject(clientRet, InstructionResultVo.class);
        boolean verifySucc = verifyInstructionResult(resultVo, instructionVo);
        if(!verifySucc)
        {
            String errMsg = String.format("verify file transfer receipt message from %s at %s FAIL",
                    recvFileQueue, this.activeMqBrokeUrl);
            logger.error(errMsg);
            throw new Exception(errMsg);
        }
        if(!resultVo.isSuccess())
        {
            logger.error(String.format("client return execute file transfer instruction=%s FAIL : %s",
                    instructionVo.getInstruction(), resultVo.getData()));
            throw new Exception(String.format("%s", resultVo.getData()));
        }
        modules = receiveFileFromQueue(connection, platformId, modules, recvFileQueue, this.transferFileTimeout);
        return modules;
    }

    /**
     * 验证activeMQ的指令以及对应的客户端返回结果是否匹配
     * @param resultVo 客户端返回结果
     * @param instructionVo 相关指令
     * @return 是否验证通过
     */
    private boolean verifyInstructionResult(InstructionResultVo resultVo, ActiveMQInstructionVo instructionVo)
    {
        String receiveSig = resultVo.getSignature();
        if(!resultVo.verifySignature(this.shareSecret))
        {
            logger.error(String.format("verify result signature FAIL, wanted=%s and receive=%s",
                    resultVo.generateSignature(this.shareSecret), receiveSig));
            return false;
        }
        if(!instructionVo.getInstruction().equals(resultVo.getInstruction()))
        {
            logger.error(String.format("verify instruction FAIL : wanted=%s and receive=%s",
                    instructionVo.getInstruction(), resultVo.getInstruction()));
            return false;
        }
        if(instructionVo.getTimestamp() != resultVo.getTimestamp())
        {
            logger.error(String.format("verify timestamp FAIL : wanted=%d and receive=%d",
                    instructionVo.getTimestamp(), resultVo.getTimestamp()));
            return false;
        }
        if(instructionVo.getNonce() != resultVo.getNonce())
        {
            logger.error(String.format("verify nonce FAIL : wanted=%s and receive=%s",
                    instructionVo.getNonce(), resultVo.getNonce()));
            return false;
        }
        return true;
    }


    /**
     * 从指定队列接受应用的安装包和配置文件
     * @param modules 指定的应用模块
     * @param queueName 接受文件的queue
     * @param timeout 超时时长
     * @return 接受到的安装包和配置文件
     * @throws Exception
     */
    private List<PlatformAppModuleVo> receiveFileFromQueue(Connection connection, String platformId, List<PlatformAppModuleVo> modules, String queueName, long timeout) throws Exception
    {
        Map<String, List<DeployFileInfo>> cfgMap = new HashMap<>();
        Map<String, List<DeployFileInfo>> installPackageMap = new HashMap<>();
        for(PlatformAppModuleVo vo : modules)
        {
            for(DeployFileInfo info : vo.getCfgs())
            {
                String cfgKey = String.format(this.cfgKeyFmt, vo.getPlatformId(), vo.getDomainName(), vo.getHostIp(),
                        info.getBasePath(), info.getDeployPath(), info.getFileName());
                if(!cfgMap.containsKey(cfgKey))
                {
                    cfgMap.put(cfgKey, new ArrayList<>());
                }
                cfgMap.get(cfgKey).add(info);
            }
            String installPackageKey = String.format(this.packageKeyFmt, vo.getModuleName(), vo.getModuleAliasName(), vo.getVersion());
            if(!installPackageMap.containsKey(installPackageKey))
            {
                installPackageMap.put(installPackageKey, new ArrayList<>());
            }
            installPackageMap.get(installPackageKey).add(vo.getInstallPackage());
        }
        logger.info(String.format("prepare to receive %d install package and %d cfg file from brokerUrl=%s and queueName=%s",
                installPackageMap.size(), cfgMap.size(), this.activeMqBrokeUrl, queueName));
        Session session = connection.createSession(Boolean.TRUE, Session.AUTO_ACKNOWLEDGE);
        // 创建 Destinatione
        Destination destination = session.createQueue(queueName);

        // 创建 Consumer
        MessageConsumer consumer = session.createConsumer(destination);
        long timeUsage = 0;
        boolean isSucc = false;
        do
        {
            Message message = consumer.receive(this.activeMqReceiveTimeSpan);
            if(message == null)
                continue;
            if(message instanceof BytesMessage)
            {
                BytesMessage bytesMessage = (BytesMessage) message;
                FileOutputStream out = null;
                try
                {
                    String fileType = message.getStringProperty(this.transferFileType);
                    String pfId = message.getStringProperty(this.transferPlatformId);
                    String domainName = message.getStringProperty(this.transferDomainName);
                    String hostIp = message.getStringProperty(this.transferHostIp);
                    String appName = message.getStringProperty(this.transferAppName);
                    String appAlias = message.getStringProperty(this.transferAppAlias);
                    String version = message.getStringProperty(this.transferAppVersion);
                    String basePath = message.getStringProperty(this.transferBasePath);
                    String deployPath = message.getStringProperty(this.transferDeployPath);
                    String fileName = message.getStringProperty(this.transferFileName);
                    String fileMd5 = message.getStringProperty(this.transferFileMd5);
                    long fileSize = message.getLongProperty(this.transferFileSize);
                    if(!this.transferInstallPackage.equals(fileType) && !this.transferCfg.equals(fileType))
                    {
                        logger.error(String.format("unknown transfer file type %s", fileType));
                        throw new Exception(String.format("unknown transfer file type %s", fileType));
                    }
                    String tmpSaveDir = String.format(this.tmpSaveDirFmt, System.getProperty("user.dir"), fileMd5);
                    File saveDir = new File(tmpSaveDir);
                    if(!saveDir.exists())
                    {
                        saveDir.mkdirs();
                    }
                    String savePath = String.format(this.tmpSavePathFmt, tmpSaveDir, fileName);
                    out = new FileOutputStream(savePath);
                    fileSize = 0;
                    int len = 2048;
                    byte[] bytes = new byte[len];
                    while ((len=bytesMessage.readBytes(bytes))!=-1){
                        out.write(bytes,0,len);
                        fileSize += len;
                    }
                    String md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePath));
                    if(fileMd5.equals(md5))
                    {
                        String errMsg = String.format("platformId=%s and domainName=%s and hostIp=%s and appName=%s and " +
                                        "appAlias=%s and version=%s and fileType=%s and basePath=%s and deployPath=%s " +
                                        "and fileName=%s and fileSize=%d transfer FAIL : srcMd5=%s and dstMd5=%s, not equal",
                                platformId, domainName, hostIp, appName, appAlias, version, fileType, basePath, deployPath,
                                fileName, fileSize, fileMd5, md5);
                        logger.error(errMsg);
                        throw new Exception(errMsg);
                    }
                    logger.info(String.format("platformId=%s and domainName=%s and hostIp=%s and appName=%s and " +
                                    "appAlias=%s and version=%s and fileType=%s and basePath=%s and deployPath=%s " +
                                    "and fileName=%s and fileSize=%d transfer SUCCESS : md5=%s",
                            pfId, domainName, hostIp, appName, appAlias, version, fileType, basePath, deployPath,
                            fileName, fileSize, md5));
                    logger.info(String.format("platformId=%s and domainName=%s and hostIp=%s and appName=%s and " +
                                    "appAlias=%s and version=%s and fileType=%s and basePath=%s and deployPath=%s " +
                                    "and fileName=%s and fileSize=%d save SUCCESS : savePath=%s",
                            pfId, domainName, hostIp, appName, appAlias, version, fileType, basePath, deployPath,
                            fileName, fileSize, savePath));
                    if(fileType.equals(this.transferInstallPackage))
                    {
                        String pkgKey = String.format(this.packageKeyFmt, appName, appAlias, version);
                        if(installPackageMap.containsKey(pkgKey))
                        {
                            logger.debug(String.format(String.format("appName=%s and appAlias=%s and version=%s app's install package is at wanted list",
                                    appName, appAlias, version)));
                            for(DeployFileInfo info : installPackageMap.get(pkgKey))
                            {
                                if(md5.equals(info.getFileMd5())) {
                                    info.setLocalSavePath(savePath);
                                    info.setFileSize(fileSize);
                                }
                                else
                                {
                                    logger.error(String.format("received appName=%s and appAlias=%s and version=%s app's install package is not wanted install package : wanted file md5=%s, receive file md5=%s",
                                            appName, appAlias, version, info.getFileMd5(), md5));
                                }
                            }
                            installPackageMap.remove(pkgKey);
                        }
                        else
                        {
                            logger.error(String.format("appName=%s and appAlias=%s and version=%s app's install package is not at wanted list",
                                    appName, appAlias, version));
                        }
                    }
                    else
                    {
                        String cfgKey = String.format(this.cfgKeyFmt, platformId, domainName, hostIp, basePath, deployPath, fileName);
                        if(cfgMap.containsKey(cfgKey))
                        {
                            logger.debug(String.format(String.format("platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is at wanted list",
                                    pfId, domainName, hostIp, basePath, deployPath, fileName)));
                            for(DeployFileInfo info : cfgMap.get(cfgKey))
                            {
                                if(md5.equals(info.getFileMd5())) {
                                    info.setLocalSavePath(savePath);
                                    info.setFileSize(fileSize);
                                }
                                else
                                {
                                    logger.error(String.format(String.format("platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is not wanted cfg : receive file md5=%s and wanted file md5=%s",
                                            pfId, domainName, hostIp, basePath, deployPath, fileName, md5, info.getFileMd5())));
                                }
                            }
                            cfgMap.remove(cfgKey);
                        }
                        else
                        {
                            logger.error(String.format(String.format("platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is not at wanted list",
                                    pfId, domainName, hostIp, basePath, deployPath, fileName)));
                        }
                    }
                }
                catch (Exception ex)
                {
                    logger.error(String.format("handle transfered install package or cfg file exception ", ex));
                    try
                    {
                        if(out != null)
                        {
                            out.close();
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error("close FileOutputStream Exception", e);
                    }
                }
                if(cfgMap.size() == 0 && installPackageMap.size() == 0)
                {
                    logger.info(String.format("all wanted cfg and install package receive"));
                    isSucc = true;
                    break;
                }
            }
            else if(message instanceof TextMessage)
            {
                String text = ((TextMessage) message).getText();
                try
                {
                    InstructionResultVo resultVo = JSONObject.parseObject(text, InstructionResultVo.class);
                    boolean verifySucc = resultVo.verifySignature(this.shareSecret);
                    if(verifySucc)
                    {
                        if(resultVo.isSuccess() && resultVo.getInstruction().equals(this.startCollectDataInstruction) && "TRANSFER_FINISH".equals(resultVo.getData()))
                        {
                            logger.info(String.format("platformId=%s client notify file transfer finish", platformId));
                            break;
                        }
                        else
                        {
                            logger.error(String.format("receive not wanted messge[%s] from queue=%s", text, queueName));
                        }
                    }
                    else
                    {
                        logger.error(String.format("receive verify signature fail messge[%s] from queue=%s", text, queueName));
                    }
                }
                catch (Exception e)
                {
                    logger.error(String.format("receive illegal messge[%s] from queue=%s", text, queueName));
                }
            }
        }
        while (timeUsage < this.transferFileTimeout);
        if(!isSucc)
        {
            logger.error(String.format("receive install package and cfg timeout : timeUsage=%d(min)", timeout/60/60));
            if(installPackageMap.size() > 0)
            {
                for(String instKey : installPackageMap.keySet())
                {
                    String[] arr = instKey.split(";");
                    logger.error(String.format("not successfully receive appName=%s and appAlias=%s and version=%s install package"),
                            arr[0], arr[1], arr[2]);
                }
            }
            if(cfgMap.size() > 0)
            {
                for(String cfgKey : cfgMap.keySet())
                {
                    String[] arr = cfgKey.split(";");
                    logger.error(String.format("not successfully receive platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s cfg file"),
                            arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
                }
            }
        }
        consumer.close();
        session.close();
        return modules;
    }
}
