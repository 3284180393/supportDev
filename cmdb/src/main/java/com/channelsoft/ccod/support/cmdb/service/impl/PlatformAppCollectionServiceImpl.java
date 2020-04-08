package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppFileAttribute;
import com.channelsoft.ccod.support.cmdb.service.IActiveMQService;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Value("${windows}")
    private boolean isWindows;

    @Value("${cmdb.share-secret}")
    private String shareSecret;

    @Value("${cmdb.server-name}")
    private String serverName;

    private String instructionTopicFmt = "CMDB_TO_%s_INSTRUCTION";

    @Value("${cmdb.app-collect.start-data-collect-instruction}")
    private String startCollectDataInstruction;

    @Value("${cmdb.app-collect.start-file-transfer-instruction}")
    private String startAppFileTransferInstruction;

    private String reportCollectDataQueueFmt = "CLIENT_REPORT_COLLECT_DATA-%s";

    private String receiveFileQueueFmt = "FILE_REC_%s";

    @Value("${cmdb.app-collect.receipt-timeout}")
    private long receiptTimeout;

    @Value("${cmdb.app-collect.collect-data-timeout}")
    private long collectDataTimeout;

    @Value("${cmdb.app-collect.transfer-file-timeout}")
    private long transferFileTimeout;

    @Value("${cmdb.app-collect.app-file-attribute-key}")
    private String appFileAttributeKey;

    private String installPackage = "Install_Package";

    private String cfgFile = "CFG_FILE";

    @Value("${activemq.brokerUrl}")
    private String activeMqBrokeUrl;

    @Value("${activemq.receive-time-span}")
    private long activeMqReceiveTimeSpan;

    @Autowired
    IActiveMQService activeMQService;

    @Autowired
    IAppManagerService appManagerService;

    private Random random = new Random();

    private ConnectionFactory connectionFactory = null;

    private String cfgKeyFmt = "%s;%s;%s;%s;%s;%s";

    private String packageKeyFmt = "%s;%s;%s";

    private String tmpSaveDirFmt = "%s/downloads/%s";

    private String tmpSavePathFmt = "%s/%s";

    @PostConstruct
    void init() throws Exception
    {
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
//        connection.setClientID(this.serverName);
//        connection.start();
    }

    @Override
    public List<PlatformAppModuleVo> checkPlatformAppData(String platformId, String platformName,String domainName, String hostIp, String appName, String version) throws Exception {
        if(StringUtils.isBlank(platformId))
        {
            logger.error(String.format("checkPlatformAppData FAIL : platformId is blank"));
            throw new Exception(String.format("checkPlatformAppData FAIL : platformId is blank"));
        }
        if(StringUtils.isBlank(platformName))
        {
            logger.error(String.format("checkPlatformAppData FAIL : platformName is blank"));
            throw new Exception(String.format("checkPlatformAppData FAIL : platformName is blank"));
        }
        Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        params.put("platformName", platformName);
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
        logger.info(String.format("begin to collect %s platform app infos, params=%s", platformId, JSONObject.toJSONString(params)));
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID(this.serverName);
        connection.start();
        try {
            List<PlatformAppModuleVo> modules = collectPlatformAppData(platformId, params, connection);
            return modules;
        }
        finally {
            connection.close();
        }
    }

    @Override
    public List<PlatformAppModuleVo> collectPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception {
        if(StringUtils.isBlank(platformId))
        {
            logger.error(String.format("collectPlatformAppData FAIL : platformId is blank"));
            throw new Exception(String.format("collectPlatformAppData FAIL : platformId is blank"));
        }
        if(StringUtils.isBlank(platformName))
        {
            logger.error(String.format("checkPlatformAppData FAIL : platformName is blank"));
            throw new Exception(String.format("checkPlatformAppData FAIL : platformName is blank"));
        }
        Map<String, String> params = new HashMap<>();
        params.put("platformId", platformId);
        params.put("platformName", platformName);
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
        logger.info(String.format("begin to collect %s platform app infos, params=%s", platformId, JSONObject.toJSONString(params)));
        connectionFactory = new ActiveMQConnectionFactory(this.activeMqBrokeUrl);
        Connection connection = connectionFactory.createConnection();
        connection.setClientID(this.serverName);
        connection.start();
        try
        {
            List<PlatformAppModuleVo> modules = collectPlatformAppData(platformId, params, connection);
            modules = getPlatformAppInstallPackageAndCfg(platformId, modules, params, connection);
            return modules;
        }
        finally {
            connection.close();
        }

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
        logger.info(String.format("begin to collect %s platform app data : params=%s", platformId, JSONObject.toJSONString(params)));
        Date now = new Date();
        int timestamp = (int)(now.getTime() / 1000);
        int nonce = random.nextInt(1000000);
        String md5 = DigestUtils.md5DigestAsHex(String.format("%d:%d", timestamp, nonce).getBytes());
        String collectDataQueue = String.format(this.reportCollectDataQueueFmt, md5);
        params.put("collectDataQueue", collectDataQueue);
        ActiveMQInstructionVo instructionVo = new ActiveMQInstructionVo(this.startCollectDataInstruction, JSONObject.toJSONString(params),
                timestamp, nonce);
        String signature = instructionVo.generateSignature(this.shareSecret);
        logger.info(String.format("start platform app collect instruction msg is %s and signature=%s",
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
        JSONObject jsonObject = JSONObject.parseObject(resultVo.getData());
        if(jsonObject.containsKey("QUERY_APP_CFGS"))
        {
            List<CfgQueryParamVo> queryList = JSONArray.parseArray(jsonObject.getString("QUERY_APP_CFGS"), CfgQueryParamVo.class);
            String  recevieQueue = jsonObject.getString("QUEUE");
            List<Map<String, Object>> queryResultList = this.queryAppCfgs(queryList);
            activeMQService.sendQueueMsg(connection, recevieQueue, JSONArray.toJSONString(queryResultList));
            clientRet = activeMQService.receiveTextMsgFromQueue(connection, recevieQueue, this.collectDataTimeout);
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
            jsonObject = JSONObject.parseObject(resultVo.getData());
        }
        if(!jsonObject.containsKey("MODULES"))
        {
            logger.error(String.format("client return bad msg : can not get MODULES info"));
            throw new Exception(String.format("client return bad msg : can not get MODULES info"));
        }
        List<PlatformAppModuleVo> modules = JSONArray.parseArray(jsonObject.getString("MODULES"), PlatformAppModuleVo.class);
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
        logger.info(String.format("begin to transfer %s platform app file : params=%s", platformId, JSONObject.toJSONString(params)));
        Date now = new Date();
        int timestamp = (int)(now.getTime() / 1000);
        int nonce = random.nextInt(1000000);
        String md5 = DigestUtils.md5DigestAsHex(String.format("%d:%d", timestamp, nonce).getBytes());
        String recvFileQueue = String.format(this.receiveFileQueueFmt, md5);
        params.put("receiveFileQueue", recvFileQueue);
        ActiveMQInstructionVo instructionVo = new ActiveMQInstructionVo(this.startAppFileTransferInstruction, JSONObject.toJSONString(params),
                timestamp, nonce);
        String signature = instructionVo.generateSignature(this.shareSecret);
        logger.info(String.format("start platform app file transfer instruction msg is %s and signature=%s",
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
        modules = receiveFileFromQueue(connection, platformId, instructionVo, modules, recvFileQueue, this.transferFileTimeout);
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
     * @param connection activemq连接
     * @param platformId 需要传输文件的平台
     * @param receiveInstruction 接收指令
     * @param modules 指定的应用模块
     * @param queueName 接受文件的queue
     * @param timeout 超时时长
     * @return 接受到的安装包和配置文件
     * @throws Exception
     */
    private List<PlatformAppModuleVo> receiveFileFromQueue(
            Connection connection, String platformId, ActiveMQInstructionVo receiveInstruction,
            List<PlatformAppModuleVo> modules, String queueName, long timeout)
            throws JMSException, IOException
    {
        Map<String, List<DeployFileInfo>> cfgMap = new HashMap<>();
        Map<String, List<DeployFileInfo>> installPackageMap = new HashMap<>();
        for(PlatformAppModuleVo vo : modules)
        {
            if(vo.getCfgs() != null && vo.getCfgs().length > 0)
            {
                for(DeployFileInfo info : vo.getCfgs())
                {
                    String cfgKey = String.format(this.cfgKeyFmt, vo.getPlatformId(), vo.getDomainId(), vo.getHostIp(),
                            info.getBasePath(), info.getDeployPath(), info.getFileName());
                    if(!cfgMap.containsKey(cfgKey))
                    {
                        cfgMap.put(cfgKey, new ArrayList<>());
                    }
                    cfgMap.get(cfgKey).add(info);
                }
            }
            if(vo.getInstallPackage() != null)
            {
                String installPackageKey = String.format(this.packageKeyFmt, vo.getModuleName(), vo.getModuleAliasName(), vo.getVersion());
                if(!installPackageMap.containsKey(installPackageKey))
                {
                    installPackageMap.put(installPackageKey, new ArrayList<>());
                }
                installPackageMap.get(installPackageKey).add(vo.getInstallPackage());
            }
        }
        logger.info(String.format("prepare to receive %d install package and %d cfg file from brokerUrl=%s and queueName=%s",
                installPackageMap.size(), cfgMap.size(), this.activeMqBrokeUrl, queueName));
        Session session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
        // 创建 Destinatione
        Destination destination = session.createQueue(queueName);

        // 创建 Consumer
        MessageConsumer consumer = session.createConsumer(destination);
        long timeUsage;
        long startTime = System.currentTimeMillis();
        while (true)
        {
            timeUsage = System.currentTimeMillis() - startTime;
            if(timeUsage >= this.transferFileTimeout)
            {
                logger.error(String.format("receive file timeout : timeUsage=%d >= %d", timeUsage, this.transferFileTimeout));
                break;
            }
            Message message = consumer.receive(this.activeMqReceiveTimeSpan);
            if(message == null)
                continue;
            if(message instanceof BytesMessage)
            {
                BytesMessage bytesMessage = (BytesMessage) message;
                String attrStr;
                AppFileAttribute attr;
                try
                {
                    attrStr = bytesMessage.getStringProperty(this.appFileAttributeKey);
                    attr = JSONObject.parseObject(attrStr, AppFileAttribute.class);
                }
                catch (Exception ex)
                {
                    logger.error("parse app file attribue exception", ex);
                    continue;
                }
                if(!verifyFileAttribute(attr))
                {
                    logger.error(String.format("receive app file attribute[%s] is illegal", attrStr));
                    continue;
                }
                String fileType = attr.fileType;
                String pfId = attr.platformId;
                String domainId = attr.domainId;
                String hostIp = attr.hostIp;
                String appName = attr.appName;
                String appAlias = attr.appAlias;
                String version = attr.version;
                String basePath = attr.basePath;
                String deployPath = attr.deployPath;
                String fileName = attr.fileName;
                String fileMd5 = attr.md5;
                long fileSize = attr.fileSize;
                String ext = attr.ext;
                if(!this.installPackage.equals(fileType) && !this.cfgFile.equals(fileType))
                {
                    logger.error(String.format("unknown transfer file type %s", fileType));
                    continue;
                }
                String tmpSaveDir = String.format(this.tmpSaveDirFmt, System.getProperty("user.dir"), fileMd5);
                File saveDir = new File(tmpSaveDir);
                if(!saveDir.exists())
                {
                    saveDir.mkdirs();
                }
                String savePath = String.format(this.tmpSavePathFmt, tmpSaveDir, fileName);
                if(this.isWindows)
                {
                    savePath = savePath.replace("/", "\\");
                }
                FileOutputStream out = null;
                try
                {
                    out = new FileOutputStream(savePath);
                    fileSize = 0;
                    int len = 2048;
                    byte[] bytes = new byte[len];
                    while ((len=bytesMessage.readBytes(bytes))!=-1){
                        out.write(bytes,0,len);
                        fileSize += len;
                    }
                }
                catch (Exception ex)
                {
                    logger.error(String.format("handle transfered install package or cfg file exception ", ex));
                    if(out != null)
                    {
                        out.close();
                    }
                }
                String md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePath));
                if(!fileMd5.equals(md5))
                {
                    logger.error(String.format("receive [%s] file FAIL : md5 not equal, wanted=%s and receive=%s",
                            attrStr, fileMd5, md5));
                    continue;
                }
                logger.info(String.format("receive [%s] file SUCCESS and save to %s", attrStr, savePath));
                if(fileType.equals(this.installPackage))
                {
                    String pkgKey = String.format(this.packageKeyFmt, appName, appAlias, version);
                    if(installPackageMap.containsKey(pkgKey))
                    {
                        logger.info(String.format(String.format("appName=%s and appAlias=%s and version=%s app's install package is in list",
                                appName, appAlias, version)));
                        for(DeployFileInfo info : installPackageMap.get(pkgKey))
                        {
                            if(md5.equals(info.getFileMd5())) {
                                info.setLocalSavePath(savePath);
                                info.setFileSize(fileSize);
                                info.setExt(ext);
                            }
                            else
                            {
                                logger.error(String.format("received appName=%s and appAlias=%s and version=%s app's install package is not wanted install package : wanted file md5=%s and receive file md5=%s",
                                        appName, appAlias, version, info.getFileMd5(), md5));
                            }
                            installPackageMap.remove(pkgKey);
                        }
                    }
                    else
                    {
                        logger.error(String.format("appName=%s and appAlias=%s and version=%s app's install package is not in list",
                                appName, appAlias, version));
                    }
                }
                else
                {
                    String cfgKey = String.format(this.cfgKeyFmt, pfId, domainId, hostIp, basePath, deployPath, fileName);
                    if(cfgMap.containsKey(cfgKey))
                    {
                        logger.info(String.format(String.format("platformId=%s and domainId=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is in wanted list",
                                pfId, domainId, hostIp, basePath, deployPath, fileName)));
                        for(DeployFileInfo info : cfgMap.get(cfgKey))
                        {
                            if(md5.equals(info.getFileMd5())) {
                                info.setLocalSavePath(savePath);
                                info.setFileSize(fileSize);
                                info.setExt(ext);
                            }
                            else
                            {
                                logger.error(String.format(String.format("platformId=%s and domainId=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is not wanted cfg : receive file md5=%s and wanted file md5=%s",
                                        pfId, domainId, hostIp, basePath, deployPath, fileName, md5, info.getFileMd5())));
                            }
                        }
                        cfgMap.remove(cfgKey);
                    }
                    else
                    {
                        logger.error(String.format(String.format("platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s app's cfg is not at wanted list",
                                pfId, domainId, hostIp, basePath, deployPath, fileName)));
                    }
                }
//                if(cfgMap.size() == 0 && installPackageMap.size() == 0)
//                {
//                    logger.info(String.format("all wanted cfg and install package receive"));
//                    break;
//                }
            }
            else if(message instanceof TextMessage)
            {
                String text = ((TextMessage) message).getText();
                try
                {
                    InstructionResultVo resultVo = JSONObject.parseObject(text, InstructionResultVo.class);
                    boolean verifySucc = verifyInstructionResult(resultVo, receiveInstruction);
                    if(verifySucc)
                    {
                        if(resultVo.isSuccess() && "TRANSFER_FINISH".equals(resultVo.getData()))
                        {
                            logger.info(String.format("platformId=%s client notify file transfer finish", platformId));
                        }
                        else if(resultVo.isSuccess())
                        {
                            logger.error(String.format("receive instruction return Date=%s is unknown", resultVo.getData()));
                        }
                        else
                        {
                            logger.error(String.format("client return transfer file FAIL : %s", resultVo.getData()));
                        }
                        break;
                    }
                    else
                    {
                        logger.error(String.format("verify receive instruction result message[%s] FAIL  from queue=%s", text, queueName));
                    }
                }
                catch (Exception e)
                {
                    logger.error(String.format("receive illegal messge[%s] from queue=%s exception", text, queueName), e);
                }
            }
        }
        if(installPackageMap.size() > 0)
        {
            for(String instKey : installPackageMap.keySet())
            {
                String[] arr = instKey.split(";");
                logger.error(String.format("not successfully receive appName=%s and appAlias=%s and version=%s install package", arr[0], arr[1], arr[2]));
            }
        }
        if(cfgMap.size() > 0)
        {
            for(String cfgKey : cfgMap.keySet())
            {
                String[] arr = cfgKey.split(";");
                logger.error(String.format("not successfully receive platformId=%s and domainName=%s and hostIp=%s and basePath=%s and deployPath=%s and fileName=%s cfg file",
                        arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]));
            }
        }
        consumer.close();
        session.close();
        return modules;
    }

    private boolean verifyFileAttribute(AppFileAttribute attr)
    {
        if(!this.installPackage.equals(attr.fileType) && !this.cfgFile.equals(attr.fileType))
        {
            logger.error(String.format("unknown file type %s", attr.fileType));
            return false;
        }
        else if(StringUtils.isBlank(attr.platformId))
        {
            logger.error(String.format("%s is blank", "platformId"));
            return false;
        }
        else if(StringUtils.isBlank(attr.domainId))
        {
            logger.error(String.format("%s is blank", "domainId"));
            return false;
        }
        else if(StringUtils.isBlank(attr.hostIp))
        {
            logger.error(String.format("%s is blank", "hostIp"));
            return false;
        }
        else if(StringUtils.isBlank(attr.appName))
        {
            logger.error(String.format("%s is blank", "appName"));
            return false;
        }
        else if(StringUtils.isBlank(attr.appAlias))
        {
            logger.error(String.format("%s is blank", "appAlias"));
            return false;
        }
        else if(StringUtils.isBlank(attr.version))
        {
            logger.error(String.format("%s is blank", "version"));
            return false;
        }
        else if(StringUtils.isBlank(attr.basePath))
        {
            logger.error(String.format("%s is blank", "basePath"));
            return false;
        }
        else if(StringUtils.isBlank(attr.deployPath))
        {
            logger.error(String.format("%s is blank", "deployPath"));
            return false;
        }
        else if(StringUtils.isBlank(attr.fileName))
        {
            logger.error(String.format("%s is blank", "fileName"));
            return false;
        }
        else if(StringUtils.isBlank(attr.ext))
        {
            logger.error(String.format("%s is blank", "ext"));
            return false;
        }
        else if(StringUtils.isBlank(attr.md5))
        {
            logger.error(String.format("%s is blank", "md5"));
            return false;
        }
        return true;
    }

    private List<Map<String, Object>> queryAppCfgs(List<CfgQueryParamVo> queryList)
    {
        List<Map<String, Object>> retList = new ArrayList<>();
        for(CfgQueryParamVo paramVo : queryList)
        {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("appName", paramVo.getAppName());
            resultMap.put("version", paramVo.getVersion());
            resultMap.put("cfgFileName", paramVo.getCfgFileName());
            try
            {
                String context = this.appManagerService.getAppCfgText(paramVo.getAppName(), paramVo.getVersion(), paramVo.getCfgFileName());
                resultMap.put("result", true);
                resultMap.put("data", context);
                AppModuleVo moduleVo = this.appManagerService.queryAppByVersion(paramVo.getAppName(), paramVo.getVersion());
                Map<String, AppCfgFilePo> cfgMap = moduleVo.getCfgs().stream().collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
                resultMap.put("nexusPath", cfgMap.get(paramVo.getCfgFileName()).getNexusFileSavePath());
                resultMap.put("md5", cfgMap.get(paramVo).getMd5());
            }
            catch(Exception ex)
            {
                resultMap.put("result", false);
                resultMap.put("data", ex.getMessage());
            }
            retList.add(resultMap);
        }
        return retList;
    }

}
