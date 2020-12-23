package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.K8sTemplateMapper;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartServiceVo;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IK8sTemplateService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.utils.FileUtils;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName: K8sTemplateServiceImpl
 * @Author: lanhb
 * @Description: IK8sTemplateService接口实现类
 * @Date: 2020/8/6 9:55
 * @Version: 1.0
 */
@Service
public class K8sTemplateServiceImpl implements IK8sTemplateService {

    private final static Logger logger = LoggerFactory.getLogger(K8sTemplateServiceImpl.class);

    @Autowired
    IK8sApiService ik8sApiService;

    @Autowired
    IAppManagerService appManagerService;

    @Autowired
    INexusService nexusService;

    @Autowired
    K8sTemplateMapper k8sTemplateMapper;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${k8s.labels.app-name}")
    private String appNameLabel;

    @Value("${k8s.labels.app-type}")
    private String appTypeLabel;

    @Value("${k8s.labels.ccod-version}")
    private String ccodVersionLabel;

    @Value("${k8s.labels.platform-id}")
    private String platformIdLabel;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.labels.node-ip}")
    private String nodeIpLabel;

    @Value("${k8s.labels.app-version}")
    private String appVersionLabel;

    @Value("${k8s.labels.service-type}")
    private String serviceTypeLabel;

    @Value("${k8s.labels.platform-tag}")
    private String platformTagLabel;

    @Value("${k8s.labels.domain-tag}")
    private String domainTagLabel;

    @Value("${k8s.labels.app-tag}")
    private String appTagLabel;

    @Value("${k8s.deployment.defaultCfgMountPath}")
    private String defaultCfgMountPath;

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${ccod.service-port-regex}")
    private String portRegex;

    @Value("${ccod.health-check-at-regex}")
    private String healthCheckRegex;

    @Value("${ccod.start-cmd-regex}")
    private String startCmdRegex;

    @Value("${k8s.template-file-path}")
    private String templateSavePath;

    @Value("${k8s.test-three-part-service-save-path}")
    private String testThreePartServiceSavePath;

    @Value("${nexus.user}")
    private String nexusUserName;

    @Value("${nexus.password}")
    private String nexusPassword;

    @Value("${nexus.host-url}")
    private String nexusHostUrl;

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).disableHtmlEscaping().create();

    private Gson templateParseGson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            //过滤掉字段名包含"age"
            return f.getName().contains("creationTimestamp") || f.getName().contains("status") || f.getName().contains("resourceVersion") || f.getName().contains("selfLink") || f.getName().contains("uid")
                    || f.getName().contains("generation") || f.getName().contains("annotations") || f.getName().contains("strategy")
                    || f.getName().contains("terminationMessagePath") || f.getName().contains("terminationMessagePolicy")
                    || f.getName().contains("dnsPolicy") || f.getName().contains("securityContext") || f.getName().contains("schedulerName")
                    || f.getName().contains("restartPolicy") || f.getName().contains("clusterIP")
                    || f.getName().contains("sessionAffinity") || f.getName().contains("nodePort");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            //过滤掉 类名包含 Bean的类
            return clazz.getName().contains("Bean");
        }
    }).registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

    private final List<K8sObjectTemplatePo> objectTemplateList = new ArrayList<>();

    private final List<K8sThreePartServiceVo> testThreePartServices = new ArrayList<>();

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    private final static String forAllVersion = "ANY_VERSION";

    @PostConstruct
    void init() throws Exception
    {
//        List<K8sObjectTemplatePo> testList = generatePlatformObjectTemplate("test-by-wyf", "4.1", "ucds-cloud01", "cas-manage01", "dcms-manage01");
//        this.objectTemplateList.addAll(testList);
//        testList = generatePlatformObjectTemplate("jhkzx-1", "3.9", "ucds-cloud01", "cas-manage01", "dcmswebservice-manage01");
////        this.objectTemplateList.addAll(testList);
//        List<K8sObjectTemplatePo> list = parseTemplateFromFile(this.templateSavePath);
////        this.objectTemplateList.addAll(list);
//        list.forEach(t->{
//            K8sTemplatePo po = new K8sTemplatePo(t);
//            k8sTemplateMapper.insert(po);
//        });
//        List<K8sThreePartServiceVo> threeSvcs = getThreePartServices("test-by-wyf", testK8sApiUrl, testAuthToken);
//        logger.warn(this.templateParseGson.toJson(threeSvcs));
//        this.testThreePartServices.addAll(threeSvcs);
//        k8sTemplateMapper.select().forEach(t->{
//            String appType = t.getLabels().get(appTypeLabel);
//            if(appType != null && appType.equals(AppType.THREE_PART_APP.name)){
//                t.getObjectTemplate().setConfigMaps(new ArrayList<>());
//                k8sTemplateMapper.update(t);
//            }
//        });
        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
        templateList.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        List<K8sThreePartServiceVo> threeSvcs = parseTestThreePartServiceFromFile(this.testThreePartServiceSavePath);
        this.testThreePartServices.addAll(threeSvcs);
        try{
//            updateTemplate();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void updateTemplate() throws Exception
    {
//        for(K8sTemplatePo template : temps){
//            if(!template.getLabels().containsKey(appTypeLabel) || template.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name))
//                continue;
//            for(V1Deployment d : template.getObjectTemplate().getDeployments()){
//                for(V1Container c : d.getSpec().getTemplate().getSpec().getInitContainers()) {
//                    c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION));
//                }
//            }
//            k8sTemplateMapper.update(template);
//        }
        k8sTemplateMapper.select().forEach(t->{
            if (t.getLabels().containsKey(appTypeLabel) && t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP)) {

                K8sObjectTemplatePo o = t.getObjectTemplate();
                if(o.getDeployments() == null){
                    o.setDeployments(new ArrayList<>());
                }
                if(o.getEndpoints() == null){
                    o.setEndpoints(new ArrayList<>());
                }
                if(o.getConfigMaps() == null){
                    o.setConfigMaps(new ArrayList<>());
                }
                if(o.getStatefulSets() == null){
                    o.setStatefulSets(new ArrayList<>());
                }
                k8sTemplateMapper.update(t);
            }
        });
        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
        templateList.stream().filter(t->t.getLabels().containsKey(appTypeLabel) && t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)).forEach(t->{
            t.getLabels().put(appTagLabel, "standard");
            t.getObjectTemplate().getLabels().put(appTagLabel, "standard");
            k8sTemplateMapper.update(t);
        });
        templateList.stream().filter(t->t.getLabels().containsKey(appTypeLabel) && t.getObjectTemplate().getStatefulSets() == null).forEach(t->{
            t.getObjectTemplate().setStatefulSets(new ArrayList<>());
            k8sTemplateMapper.update(t);
        });


        String platformId = K8sObjectTemplatePo.PLATFORM_ID;
        String domainId = K8sObjectTemplatePo.DOMAIN_ID;
        String appName = K8sObjectTemplatePo.APP_NAME;
        String alias = K8sObjectTemplatePo.ALIAS;
        String hostUrl = K8sObjectTemplatePo.HOST_URL;
        String k8sHostIp = K8sObjectTemplatePo.K8S_HOST_IP;
        String nfsServerIp = K8sObjectTemplatePo.NFS_SERVER_IP;
        for(K8sTemplatePo template : templateList){
            Map<String, String> labels = template.getLabels();
            if(template.getLabels().containsKey(appTypeLabel)){
                resetAppTemplate(template.getObjectTemplate());
                k8sTemplateMapper.update(template);
            }
            String json = gson.toJson(template);

            json = json.replace("\"server\":\"10.130.41.218\"", String.format("\"server\":\"%s\"", nfsServerIp));

            json = json.replace("test-by-wyf.ccod.com", hostUrl);
            json = json.replace("jhkgs.ccod.com", hostUrl);
            json = json.replace("jhkzx-1.ccod.com", hostUrl);
            json = json.replace("10.130.41.218", k8sHostIp);

            json = json.replace("cas-manage01", String.format("%s-%s", alias, domainId));
            json = json.replace("\"cas\"", String.format("\"%s\"", alias));
            json = json.replace("\"cas-", String.format("\"%s-", alias));
            json = json.replace("manage01", domainId);

            json = json.replace("ucds", alias);
            json = json.replace("cloud01", domainId);

            json = json.replace("dcms", alias);
            json = json.replace("dcmswebservice", alias);
            json = json.replace("freeswitch-wgw", alias);

            json = json.replace("test-by-wyf", platformId);
            json = json.replace("someTest", platformId);
            json = json.replace("jhkzx-1", platformId);
            json = json.replace("test08", platformId);
            json = json.replace("test48", platformId);
            json = json.replace("k8s-test", platformId);

            json = json.replace("base-volume/db/oracle/sql", String.format("base-volume/db/%s/sql", K8sObjectTemplatePo.ALIAS));
            json = json.replace("base-volume/db/oracle/data", String.format("base-volume/db/%s/data", K8sObjectTemplatePo.ALIAS));
            json = json.replace("base-volume/db/mysql/sql", String.format("base-volume/db/%s/sql", K8sObjectTemplatePo.ALIAS));
            json = json.replace("base-volume/db/mysql/data", String.format("base-volume/db/%s/data", K8sObjectTemplatePo.ALIAS));

            json = json.replace("202005-test", K8sObjectTemplatePo.PLATFORM_ID);

            K8sTemplatePo po = gson.fromJson(json, K8sTemplatePo.class);
            po.setLabels(labels);
            po.getObjectTemplate().setLabels(labels);
            k8sTemplateMapper.update(po);
            logger.error(String.format("template=%s", gson.toJson(po)));
        }
        templateList = k8sTemplateMapper.select();
        for(K8sTemplatePo template : templateList){
            Map<String, String> labels = template.getLabels();
            if(labels.containsKey(appTypeLabel) && labels.get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
                String name = null;
                for(String key : labels.keySet()){
                    if(key.equals(appTypeLabel) || key.equals(ccodVersionLabel) || key.equals(appTagLabel))
                        continue;
                    name = labels.get(key);
                    break;
                }
                labels.put(name, forAllVersion);
                labels.remove(appNameLabel);
                template.setLabels(labels);
                template.getObjectTemplate().setLabels(labels);
                k8sTemplateMapper.update(template);
            }
        }
        List<K8sTemplatePo> temps = k8sTemplateMapper.select();
        for(K8sTemplatePo template : temps){
            if(template.getObjectTemplate().getPvList() != null){
                template.getObjectTemplate().setPvList(template.getObjectTemplate().getPvList().stream().map(v->{
                    v.getSpec().getClaimRef().setNamespace(String.format("base-%s", K8sObjectTemplatePo.PLATFORM_ID));
                    return v;
                }).collect(Collectors.toList()));
                k8sTemplateMapper.update(template);
                System.out.println("hello");
            }
            else if(template.getLabels().containsKey(appTypeLabel) && !template.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
                template.getObjectTemplate().setDeployments(template.getObjectTemplate().getDeployments().stream().map(d->{
                    d.getSpec().getTemplate().getSpec().getInitContainers().forEach(c->c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION)));
                    return d;
                }).collect(Collectors.toList()));
                System.out.println("hello");
                k8sTemplateMapper.update(template);
            }
            if(template.getObjectTemplate().getPvcList() != null){
                template.getObjectTemplate().setJobs(new ArrayList<>());
                template.getObjectTemplate().setPvcList(template.getObjectTemplate().getPvcList().stream().map(v->{
                    v.getMetadata().setNamespace(String.format("base-%s", K8sObjectTemplatePo.PLATFORM_ID));
                    return v;
                }).collect(Collectors.toList()));
                k8sTemplateMapper.update(template);
                System.out.println("hello");
            }
            else if(template.getLabels().containsKey(appTypeLabel) && template.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
                template.getObjectTemplate().setDeployments(template.getObjectTemplate().getDeployments().stream().map(d->{
                    d.getMetadata().setNamespace(String.format("base-%s", K8sObjectTemplatePo.PLATFORM_ID));
                    return d;
                }).collect(Collectors.toList()));
                if(template.getObjectTemplate().getServices() == null){
                    System.out.println("bye");
                }
                template.getObjectTemplate().setServices(template.getObjectTemplate().getServices().stream().map(d->{
                    d.getMetadata().setNamespace(String.format("base-%s", K8sObjectTemplatePo.PLATFORM_ID));
                    return d;
                }).collect(Collectors.toList()));
                template.getObjectTemplate().setEndpoints(template.getObjectTemplate().getEndpoints().stream().map(d->{
                    d.getMetadata().setNamespace(String.format("base-%s", K8sObjectTemplatePo.PLATFORM_ID));
                    return d;
                }).collect(Collectors.toList()));
                System.out.println("hello");
                k8sTemplateMapper.update(template);
            }
            if(template.getObjectTemplate().getPvList() != null){
                template.getObjectTemplate().setPvList(template.getObjectTemplate().getPvList().stream().map(v->{
                    v.getSpec().getNfs().setPath("/home/kubernetes/volume/${PLATFORMID}");
                    return v;
                }).collect(Collectors.toList()));
                k8sTemplateMapper.update(template);
                System.out.println("hello");
            }
            if(template.getLabels().containsKey(appTypeLabel) && !template.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
                template.getObjectTemplate().setDeployments(template.getObjectTemplate().getDeployments().stream().map(d->{
                    d.getSpec().getTemplate().getSpec().getInitContainers().forEach(c->c.setName(String.format(K8sObjectTemplatePo.ALIAS)));
                    return d;
                }).collect(Collectors.toList()));
                System.out.println("hello");
                k8sTemplateMapper.update(template);
            }
        }
        temps.forEach(t->logger.error(String.format("template=%s", gson.toJson(t))));
    }

//    private void updateTemplate() throws Exception
//    {
////        Map<String, String> labels = new HashMap<>();
////        labels.put(ccodVersionLabel, "4.8");
////        labels.put(appTypeLabel, AppType.THREE_PART_APP.name);
////        labels.put(appNameLabel, "umg");
////        String json = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"name\":\"umg\",\"namespace\":\"default\",\"labels\":{\"name\":\"umg\"}},\"spec\":{\"strategy\":{\"type\":\"Recreate\"},\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"umg\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"umg\"}},\"spec\":{\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"config\",\"configMap\":{\"name\":\"umg\"}},{\"name\":\"ccod-runtime\",\"hostPath\":{\"path\":\"/var/ccod-runtime/default/ssr\"}},{\"name\":\"core\",\"hostPath\":{\"path\":\"/var/ccod-runtime/default/core/ssr\"}}],\"containers\":[{\"name\":\"umg\",\"imagePullPolicy\":\"Always\",\"image\":\"ccod/ssr-umg:225-3543_225-22317\",\"ports\":[{\"containerPort\":12000},{\"containerPort\":11500}],\"volumeMounts\":[{\"mountPath\":\"/root/ATS/config\",\"name\":\"config\"},{\"mountPath\":\"/root/ATS/runlog\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":20,\"periodSeconds\":10,\"successThreshold\":1,\"tcpSocket\":{\"port\":12000},\"timeoutSeconds\":1},\"readinessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":20,\"periodSeconds\":10,\"successThreshold\":1,\"tcpSocket\":{\"port\":12000},\"timeoutSeconds\":1},\"resources\":{\"requests\":{\"memory\":\"1000Mi\",\"cpu\":\"1000m\"},\"limits\":{\"memory\":\"1000Mi\",\"cpu\":\"1000m\"}},\"workingDir\":\"/root/ATS/bin\",\"command\":[\"/bin/bash\",\"-c\"],\"args\":[\"./umgdaemon;\",\"tail -F /root/ATS/runlog/*;\"]}]}}}}";
////        V1Deployment deployment = gson.fromJson(json, V1Deployment.class);
////        json = "{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"umg\",\"namespace\":\"default\"},\"spec\":{\"ports\":[{\"name\":\"umg\",\"port\":11500,\"protocol\":\"TCP\",\"targetPort\":11500},{\"name\":\"ssr\",\"port\":12000,\"protocol\":\"TCP\",\"targetPort\":12000}],\"type\":\"ClusterIP\"}}";
////        V1Service service = gson.fromJson(json, V1Service.class);
////        json = "{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\",\"metadata\":{\"name\":\"umg\",\"namespace\":\"default\"},\"data\":{\"ATSEConfigData.cfg\":\"<?xml version=\\\"1.0\\\" encoding=\\\"GB2312\\\"?>\\n<ATSE>\\n  <AttributeSet MediaServerIP=\\\"\\\" AutoAnswer = \\\"true\\\" StatisticsLevel=\\\"3\\\" IsDebug=\\\"true\\\" LogLevel=\\\"DT\\\" RunLog=\\\"true\\\" UseDRWRClient=\\\"true\\\" WriteUserDefinedCallID=\\\"true\\\" AutoClear = \\\"5\\\" MaxUserIdleSeconds=\\\"\\\" SMSMediaServerIP=\\\"\\\" SupportFileOnUnix=\\\"true\\\">\\n    <SSR2 IP=\\\"127.0.0.1\\\" Port=\\\"12000\\\" SLEEName=\\\"Slee1_name\\\" SLEEPassword=\\\"Slee1_pwd\\\"/>\\n    <SNMPAgent TargetAddress=\\\"127.0.0.1\\\" ListenPort=\\\"10015\\\" TargetPort=\\\"10061\\\" NodeId=\\\"100\\\" NodeLocation=\\\"wuxz_notebook\\\" NodeName=\\\"slee100\\\"/>\\n    <SDR NodeCode=\\\"1001\\\" DeviceCode=\\\"001001\\\"/>\\n  </AttributeSet>\\n  <DNGroups>\\n    <ResGroup Desc=\\\"服务资源组1\\\" Type = \\\"outbound\\\">\\n      <DNList>\\n        <ComplexDN StartDN=\\\"3001\\\" EndDN=\\\"3001\\\"/>\\n      </DNList>\\n      <RunTime>\\n        <RunPeriod Begin=\\\"0\\\" End=\\\"23\\\">\\n          <Application Name=\\\"ATS服务\\\" XMLFile=\\\"b.usml\\\" IsInbound=\\\"false\\\" IsAutoStart=\\\"false\\\"/>\\n        </RunPeriod>\\n      </RunTime>\\n    </ResGroup>\\n    <ResGroup Desc=\\\"服务资源组1\\\" Type = \\\"inbound\\\">\\n      <DNList>\\n        <ComplexDN StartDN=\\\"5001\\\" EndDN=\\\"5001\\\"/>\\n      </DNList>\\n      <RunTime>\\n        <RunPeriod Begin=\\\"0\\\" End=\\\"23\\\">\\n          <Application Name=\\\"ATS服务\\\" XMLFile=\\\"inbound.usml\\\" IsInbound=\\\"true\\\" IsAutoStart=\\\"false\\\"/>\\n        </RunPeriod>\\n      </RunTime>\\n      </ResGroup>\\n        <ResGroup Desc=\\\"服务资源组1\\\" Type = \\\"App\\\">\\n          <DNList>\\n            <ComplexDN StartDN=\\\"7003\\\" EndDN=\\\"7003\\\"/>\\n          </DNList>\\n          <RunTime>\\n            <RunPeriod Begin=\\\"0\\\" End=\\\"23\\\">\\n              <Application Name=\\\"ATS服务\\\" XMLFile=\\\"a3.usml\\\" IsInbound=\\\"true\\\" IsAutoStart=\\\"false\\\"/>\\n            </RunPeriod>\\n          </RunTime>\\n    </ResGroup>\\n  </DNGroups>\\n  <Components>\\n    <Component ProgID=\\\"QNWriteFileCOM.clsWriteFile\\\"     ClassName=\\\"com.channelsoft.reusable.comobj.writefilecom.WriteFileCom\\\">    \\n      <Config>\\n        <Entry Key=\\\"WriteFilePath\\\" Value=\\\"\\\"/>\\n      </Config>\\n    </Component>\\n    <Component ProgID=\\\"QNDBCOM.clsQueryDB\\\" ClassName=\\\"com.channelsoft.reusable.comobj.dbcom.DBCom\\\"/>\\n    <Component ProgID=\\\"QuerySQL.CQueryDB\\\" ClassName=\\\"com.channelsoft.reusable.comobj.dbcom.DBCom\\\"/>\\n    <Component ProgID=\\\"QNNetCOM.clsHttp\\\" ClassName=\\\"com.channelsoft.reusable.comobj.httpcom.HttpCom\\\"/>\\n    <Component ProgID=\\\"PublicUnity.CHTTP\\\" ClassName=\\\"com.channelsoft.reusable.comobj.httpcom.HttpCom\\\"/>\\n    <Component ProgID=\\\"MSSOAP.SoapClient30\\\" ClassName=\\\"com.channelsoft.reusable.comobj.ws.SoapClientByWSIF\\\"/>\\n    <Component ProgID=\\\"QnINICOM.ini\\\" ClassName=\\\"com.channelsoft.reusable.comobj.inicom.IniCom\\\"/>\\n    <Component ProgID=\\\"MD5Com.MD5Entity\\\" ClassName=\\\"com.channelsoft.reusable.comobj.md5com.Md5Com\\\"/>            \\n    <Component ProgID=\\\"JMSCom.JMSEntity\\\" ClassName=\\\"com.channelsoft.reusable.comobj.jmscom.JmsCom\\\"/>    \\n    <Component ProgID=\\\"CCOD.AssociatedData\\\" ClassName=\\\"com.channelsoft.slee.callagent.ccod.servicedata.V2_ServiceData\\\"/>\\n    <Component ProgID=\\\"CONFNODE.ConfClient.1\\\" ClassName=\\\"com.channelsoft.reusable.comobj.confclient.ConfNode\\\"/>\\n    <Component ProgID=\\\"TConfCom.TConfClient.1\\\" ClassName=\\\"com.channelsoft.reusable.comobj.confclient.ConfNode\\\"/>\\n    <Component ProgID=\\\"QnFaxForAgent.clsOprTask\\\" ClassName=\\\"com.channelsoft.reusable.comobj.DCOMInvoker\\\">\\n      <Config>\\n        <Entry Key=\\\"User\\\" Value=\\\"\\\"/>\\n        <Entry Key=\\\"Password\\\" Value=\\\"\\\"/>\\n      </Config>\\n    </Component>\\n    <Component ProgID=\\\"TDP.IVRInterface\\\" ClassName=\\\"com.channelsoft.reusable.comobj.DCOMInvoker\\\">\\n      <Config>\\n        <Entry Key=\\\"User\\\" Value=\\\"\\\"/>\\n        <Entry Key=\\\"Password\\\" Value=\\\"\\\"/>\\n      </Config>\\n    </Component>    \\n    <Component ProgID=\\\"Microsoft.XMLDOM\\\" ClassName=\\\"com.channelsoft.reusable.comobj.xmldom.DomDocument\\\"/>    \\n    <Component ProgID=\\\"MSXML2.DOMDocument.4.0\\\" ClassName=\\\"com.channelsoft.reusable.comobj.xmldom.DomDocument\\\"/>    \\n    <Component ProgID=\\\"CASRouterClient\\\" ClassName=\\\"com.channelsoft.reusable.comobj.cas.RouterClient\\\">\\n      <Config>\\n        <Entry Key=\\\"CASServerIP\\\" Value=\\\"\\\"/>\\n        <Entry Key=\\\"CASServerPort\\\" Value=\\\"\\\"/>\\n        <Entry Key=\\\"ListenServerPort\\\" Value=\\\"\\\"/>\\n      </Config>\\n    </Component>\\n  </Components>\\n  <ServiceProviders>\\n    <ReasoningProvider>\\n      <ClassName>com.channelsoft.reusable.debugproxy.DebugAgent</ClassName>\\n      <Enabled>false</Enabled>\\n    </ReasoningProvider>\\n  </ServiceProviders>\\n</ATSE>  \",\"SSR.ini\":\"[System]\\nUMGIP=127.0.0.1\\nUMGPort=11500\\nRouteStratgy_1=ani\\nRouteStratgy_2=dnis\\nRouteStratgy_3=oriani\\nRouteStratgy_4=ani\\nRouteStratgy_5=mediatype\\nRouteStratgy_6=weight\\nRouteStratgy_7=average\\nClientCount=2\\nListenPort=12000\\nLog_path=../runlog\\nLog_level=debug\\n[Client1]\\nType=cms\\nName=zxclone-cloud01-cms1\\nPassword=zxclone-cloud01-cms1\\nAcceptCall=yes\\nweigth=1\\nmediaType=0\\nDNIS=0000000001\\n[Client2]\\nType=cms\\nName=zxclone-cloud01-cms1\\nPassword=zxclone-cloud01-cms1\\nAcceptCall=yes\\nweigth=1\\nmediaType=0\\nDNIS=0000000001\",\"UMG.ini\":\"[SYSTEM]\\nlog_path=../runlog/\\nlog_level=debug\\nHWType=freeswitch\\nVGCP_listen_port=11500\\nchannel_worker_number=16\\nDefaultCallingNumber=1000\\nAccept_Route_Play=yes\\nMultiProcess=No\\nVolume=0\\nMax_Time_Drop_Enable=yes #是否启用超时挂断\\nMax_Drop_Time=7200 #超时挂断事件（S）该事件最低为300s，配置低于300默认为300\\nU2UInfo_for_OriANI = yes\\nRedis_Location = redis-headless:27379,redis-headless:27379,redis-headless:27379\\n[CPRLIST]\\ntotal=0\\ntone_clamp=yes\\necho_cancel=yes\\nneed_record=yes\\n[FilePath_Transfer]\\norig_path=D:\\\\ChannelSoft\\\\CsCCP\\\\SoftSwitch\\\\\\ndest_path=/data/record/recording/\\norig_path_fax=D:\\\\ChannelSoft\\\\CsCCP\\\\SoftSwitch\\\\Document\\\\\\ndest_path_fax=./\\nnew_record_path = umg69\\n[PSR]\\nConnect_PSR = no\\nMasterEndpoint = PSRServer:tcp_-h_10.9.26.79_-p_11280\\nSlaveEndpoint = PSRServer:tcp_-h_10.9.26.79_-p_11481\\nEventServer = PSREventServer:tcp_-h_127.0.0.1_-p_14681\\nUMG4PSREndpoint = UMG:tcp_-h_10.9.43.75_-p_10072\\n[XoIP]\\nsip_server_address=10.9.43.75 #WGW地址\\nsip_server_port=6080 #WGW端口\\nRegister_Period=3000\\nRegister_Expires=3600\\nRegister_Num_Begin=1000\\nRegister_Count=20\\nRegister_PhoneNumInterval=2\\nrtp_media_coding0=g711a\\nrtp_media_coding1=g711mu\\n[ModNumber]\\nIsNeedModNumber=yes #yes/no \\nModules_Params = ../hlr/hlr.13x, ../hlr/hlr.14x, ../hlr/hlr.15x, ../hlr/hlr.17x, ../hlr/hlr.18x\\n#[TRUNK_GROUP$]\\n#sip_server_address=user #客户交本地注册用户\\n[AccessExtMoudle] #iceserver链接配置 回呼\\nuse=true\\nIce_server=127.0.0.1\\nIce_port = 8001\\nIce_timeout = 5000\\n[StreamPlay] #流式放音\\nsupport =no                                                              \\nuri=10.9.43.75:8000\\n[StreamRecord] #流式录音\\nsupport = no\\nuri=10.9.43.75:8000\\nuser=source\\npasswd=hackme\\n[TRUNK_GROUP]\\ntotal=7\\n#linkChannels=960,962,964,966,968,970,972,974,976,978,980,982,984,986,988\\n#linkChannels=1025,1026,1027,1028,1029,1030,1032,1034,1036,1038,1040,1042,1044,1046,1048,1050,1052,1054,1055\\n[TRUNK_GROUP1]\\ntac=s\\nstart_number=0\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\n#sip_server_address=\\n#sip_server_port=5060\\n#sip_server_address=user\\n#sip_server_port=5060\\nbillingcode=cd691\\nis_need_csr=no\\n#internal|external\\nfstrunck=internal\\n[TRUNK_GROUP2]\\ntac=t\\nstart_number=5096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.43.75 #WGW地址\\nsip_server_port=6080 #WGW端口\\nbillingcode=cd692\\nis_need_csr=yes\\n#internal|external\\nfstrunck=internal\\nModNumber_Rules=../config/mod_number/beijing.xml\\n[TRUNK_GROUP3]\\ntac=w\\nstart_number=6096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.43.75 #WGW地址\\nsip_server_port=6080 #WGW端口\\nbillingcode=cd692\\nis_need_csr=yes\\n#internal|external\\nfstrunck=internal\\n[TRUNK_GROUP4]\\ntac=z\\nstart_number=7096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.15.145 #联调外部厂商，可不配\\nsip_server_port=5060 #联调外部厂商，可不配\\n#sip_server_address=10.9.43.75\\n#sip_server_port=6090\\nbillingcode=cd692\\nis_need_csr=yes\\n#internal|external\\nfstrunck=internal-ylxt\\n[TRUNK_GROUP5]\\ntac=o\\nstart_number=8096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.58.178 #联调外部厂商，可不配\\nsip_server_port=5060 #联调外部厂商，可不配\\nbillingcode=cd693\\nis_need_csr=yes\\nfstrunck=gateway/zealcomm\\n[TRUNK_GROUP6]\\ntac=m\\nstart_number=9096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.43.75\\nsip_server_port=7080\\n#sip_server_address=user\\n#sip_server_port=5060\\nbillingcode=cd691\\nis_need_csr=no\\n#internal|external\\nfstrunck=internal\\n[TRUNK_GROUP7]\\ntac=n\\nstart_number=10096\\ntotal= 1000\\nprotocol= sip\\naccept_call=yes\\ntrunkdrection=both\\ntacStrategy=1\\nsip_server_address=10.9.0.243\\nsip_server_port=5060\\n#sip_server_address=user\\n#sip_server_port=5060\\nbillingcode=cd691\\nis_need_csr=no\\n#internal|external\\nfstrunck=internal\\n[CDR]\\nsupport_cdr=yes\\ndevice_code=010VG1\\n[IvrMonitor]\\nsupported=no\\nivrmonitor_port=4321\\n[CtiAdapter]\\nsupported=yes\\nEndPoint=QRServer:tcp_-h_10.9.26.79_-p_11480 #tsr地址\\nLocalPath=/data/shdujian/\\nRemoteUrl=/data/wuzj/\\nTimeOut=3000\\nKeepAlive=false\\nLogFile=../runlog/\\n[SpecialAniAddrNature]\\naddr_nature=3\\nANI=95511,95533\\n[CloudData]\\nsupport =no\\nNodeID = 23\\nUMG_IP = 10.9.43.75 #中信测试环境未部署云接入\\nUMG_Port = 12000\\nzookeeper_ip = 10.130.41.140\\nzookeeper_port =2181\\nUMGName4Zookeeper=umg_fs_141\\nZooKeeperPath=/umg_cloud/cluster\\ntrunk_num = 2\\n[CloudData_Trunk1]\\nTac = s\\nType = SIP\\nOperator = CMCC\\nLocalAreaCode = 10\\nAvailable = true\\nChannelThreshold = 0.85\\nTotalChannels = 1000\\nRecordAble=1\\nbillingcode=cd691\\n[CloudData_Trunk2]\\nTac = t\\nType = PSTN\\nOperator = CMCC\\nLocalAreaCode = 10\\nAvailable = true\\nChannelThreshold = 0.85\\nTotalChannels = 1000\\nRecordAble=1\\nbillingcode=cd692\\n[Monitor]\\nIsNeedMonitor = no # 是否启用运行监控消息输出\\nMonitorFilePath = ../cctrack/umg # 运行监控消息输出目录，需要手动建立\\nMonitorFileName = UMG_TEST69 # 运行监控消息输出文件名\\nModuleName = UMG_TEST69 # 运行监控消息输出服务名  \\n[UUINFO]\\nvalue=\\\"ANI:15210508909|ID:2255|CLIENTNO:1002|CTINO:1002|SYSTEM:253648\\\"\\n[SS7ANI4BJLT]\\nSetSS7AniType=1 #use:1  nouse:0\\n[GLS]\\nConnect_GLS = yes\\nServiceName = UMG-DEMO\\nGlsLocation = GLSServer:tcp_-h_10.9.26.79_-p_17020\\n[FrontCode]\\nsupport = yes\\nCode = 9999\\nANIFile = ../config/ANIFile.cfg\\n[DataBase]\\nUSE_ICE=0\\n[IceServer]\\nIce_server=10.130.41.35\\nIce_port=10000\\n[FSW]\\nInboundTrunkTac=s\\nFSWServerAddr=127.0.0.1 #SGW地址\\nFSWServerPort=8021 #SGW端口\\nrecordingpath=/data/record/recording/\\nrecordedpath=/data/record/recorded/\\nsplitfileext=.wav\\nmixfileext=.wav\\nneed_add_alert=true\\n[Anon]\\nEnable=yes\\nExcTimeout=5\\nConTimeout=5\\nAgentid=010\\nUrl=http://ccod1:8082/ccod/getSerialNumber\\nAppID=ccodprivate\\nAppPWD=7cd3f4920ac00fad603a8205309c6e4c\\nRule=TEL:99\",\"drwr_client.cfg\":\"#######################################################################\\n# 话单存储器配置文件\\n# 林维志，2005-12-12\\n# 青牛（北京）技术有限公司 版权所有\\n#######################################################################\\n# ===========================\\n# 下面指定话单文件相关参数\\n# ===========================\\n# 缓存文件名，最好为全路径名。\\ndrwr_cache_filename = $USBOSS_HOME/sdr/temp/DRWRClient.dat\\n# 需要排除的业务标识前缀。缺省为USE_Service，为空时表示不需要排除功能。\\ndrwr_except_service_id_prefix = USE_Service\\n# 是否写话单文件。取值范围：[0,1]，分别代表[否，是]\\ndrwr_write_to_file = 1\\n# 生成话单文件的最大时间间隔，单位：分钟\\ndrwr_max_interval_per_file = 10\\n# 每个话单文件中允许的最大行数\\ndrwr_max_rows_per_file = 10000\\n# 话单文件的存放路径，最好使用全路径名\\ndrwr_file_storage_path = $USBOSS_HOME/sdr/DRFiles/\\n# 是否使用完整的存放路径，取值范围：[0,1],分别代表：[否，是(会在drwr_file_storage_path后加device_code)]\\ndrwr_need_full_storage_path = 1\\n# 话单文件备份的路径。最好使用全路径名。为空时不备份\\n#drwr_file_backup_path = $USBOSS_HOME/sdr/DRFilesBak/\\n# SDR话单文件的格式。取值范围：[1,2]，分别代表[格式1（老版本格式），格式2(新版本格式)]\\ndrwr_sdr_format = 2\\n# CDR话单文件的格式。取值范围：[1,2]，分别代表[格式1（老版本格式），格式2(新版本格式)]\\ndrwr_tel_cdr_format = 1\\n# ===========================\\n# 下面指定服务器相关参数\\n# ===========================\\n# 是否实时发送到AAA服务器。取值范围：[0,1]，分别代表[否，是]\\ndrwr_send_to_server = 1\\n# 服务器的地址xxx.xxx.xxx.xxx\\ndrwr_server_ip = 134.96.71.108\\n# 服务器的端口\\ndrwr_server_port = 9091\\n# ===========================\\n# 下面指定日志参数\\n# ===========================\\n# 日志文件名，最好为全路径名，且不需要指定扩展名。若此参数为空则不写日志。\\ndrwr_log_filename = $USBOSS_HOME/sdr/Log/\\n# 日志级别。取值范围：[700,600,400,300,0]，分别代表[DEBUG,INFO,WARN,ERROR,FATAL]\\ndrwr_log_level = INFO\",\"umg_cdr.cfg\":\"###############################################################################\\n#\\n#    话单存储器配置文件\\n#    林维志，2005-12-12\\n#    北京青牛软件技术有限责任公司　版权所有\\n#\\n###############################################################################\\n###############################################################################\\n# 以下话单收发模块drwr_client的配置\\n###############################################################################\\n# 是否实时发送到AAA服务器。取值范围：[0,1]，分别代表[否，是]\\ndrwr_send_to_server = 0\\n# 话单接收服务器的地址xxx.xxx.xxx.xxx\\ndrwr_server_ip = 127.0.0.1\\n# 话单接收服务器的端口,\\ndrwr_server_port = 9092\\n# 缓存文件名，最好为全路径名。\\ndrwr_cache_filename = $USBOSS_HOME/cdr/temp/DRWRClientTest.dat\\n# 需要排除的业务标识前缀。缺省为USE_Service，为空时表示不需要排除功能。\\ndrwr_except_service_id_prefix = USE_Service\\n# 是否写话单文件。取值范围：[0,1]，分别代表[否，是]\\ndrwr_write_to_file = 1\\n# 生成话单文件的最大时间间隔，单位：分钟\\ndrwr_max_interval_per_file = 60\\n# 每个话单文件中允许的最大行数\\ndrwr_max_rows_per_file = 20000\\n# 话单文件的存放路径，最好使用全路径名\\ndrwr_file_storage_path = $USBOSS_HOME/cdr/DRFiles/\\n# 是否使用完整的存放路径，取值范围：[0,1],分别代表：[否，是(会在drwr_file_storage_path后加device_code)]\\ndrwr_need_full_storage_path = 0\\n# 话单文件备份的路径。最好使用全路径名。为空时不备份\\ndrwr_file_backup_path = $USBOSS_HOME/cdr/DRFilesBak/\\n# SDR话单文件的格式。取值范围：[1,2]，分别代表[格式1（老版本格式），格式2(新版本格式)]\\ndrwr_sdr_format = 2\\n# CDR话单文件的格式。取值范围：[1,2]，分别代表[格式1（老版本格式），格式2(新版本格式)]\\ndrwr_tel_cdr_format = 2\\n# 日志文件名，最好为全路径名\\ndrwr_log_filename = $USBOSS_HOME/cdr/log/drwr_client.log\\n# 日志级别。取值范围：[700,600,400,300,0]，分别代表[DEBUG,INFO,WARN,ERROR,FATAL]\\ndrwr_log_level = 700\\n###############################################################################\\n# 以下为实时话单接收器drwr_server的配置\\n###############################################################################\\n# drwr_server使用的本地的IP地址\\ndrwr_local_ip = 127.0.0.1\\n# drwr_server使用的本地的端口\\ndrwr_local_port = 9091\\n# drwr_server的线程池的大小\\ndrwr_thread_pool_size = 30\\n# 允许访问drwr_server的客户端的IP数量\\n#drwr_allow_ip_count = 0\\n# 允许访问drwr_server的客户端的IP\\n#drwr_allow_id_1 =\"}}";
////        V1ConfigMap configMap = gson.fromJson(json, V1ConfigMap.class);
////        updateAppK8sTemplate(labels, Arrays.asList(deployment), new ArrayList<>(), Arrays.asList(service), null, new ArrayList<>(), Arrays.asList(configMap));
////        k8sTemplateMapper.select().forEach(t->{
////            if (t.getLabels().containsKey(appTypeLabel) && t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP)) {
////
////                K8sObjectTemplatePo o = t.getObjectTemplate();
////                if(o.getDeployments() == null){
////                    o.setDeployments(new ArrayList<>());
////                }
////                if(o.getEndpoints() == null){
////                    o.setEndpoints(new ArrayList<>());
////                }
////                if(o.getConfigMaps() == null){
////                    o.setConfigMaps(new ArrayList<>());
////                }
////                if(o.getStatefulSets() == null){
////                    o.setStatefulSets(new ArrayList<>());
////                }
////                k8sTemplateMapper.update(t);
////            }
////        });
//        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
////        templateList.stream().filter(t->t.getLabels().containsKey(appTypeLabel) && t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)).forEach(t->{
////            t.getLabels().put(appTagLabel, "standard");
////            t.getObjectTemplate().getLabels().put(appTagLabel, "standard");
////            k8sTemplateMapper.update(t);
////        });
////        templateList.stream().filter(t->t.getLabels().containsKey(appTypeLabel) && t.getObjectTemplate().getStatefulSets() == null).forEach(t->{
////            t.getObjectTemplate().setStatefulSets(new ArrayList<>());
////            k8sTemplateMapper.update(t);
////        });
//
//
//        String platformId = K8sObjectTemplatePo.PLATFORM_ID;
//        String domainId = K8sObjectTemplatePo.DOMAIN_ID;
//        String appName = K8sObjectTemplatePo.APP_NAME;
//        String alias = K8sObjectTemplatePo.ALIAS;
//        String hostUrl = K8sObjectTemplatePo.HOST_URL;
//        String k8sHostIp = K8sObjectTemplatePo.K8S_HOST_IP;
//        String nfsServerIp = K8sObjectTemplatePo.NFS_SERVER_IP;
////        for(K8sTemplatePo template : templateList){
////            Map<String, String> labels = template.getLabels();
//////            if(template.getLabels().containsKey(appTypeLabel)){
//////                resetAppTemplate(template.getObjectTemplate());
////////                k8sTemplateMapper.update(template);
//////            }
//////            String json = gson.toJson(template);
//////
////////            json = json.replace("\"server\":\"10.130.41.218\"", String.format("\"server\":\"%s\"", nfsServerIp));
////////
////////            json = json.replace("test-by-wyf.ccod.com", hostUrl);
////////            json = json.replace("jhkgs.ccod.com", hostUrl);
////////            json = json.replace("jhkzx-1.ccod.com", hostUrl);
////////            json = json.replace("10.130.41.218", k8sHostIp);
////////
////////            json = json.replace("cas-manage01", String.format("%s-%s", alias, domainId));
////////            json = json.replace("\"cas\"", String.format("\"%s\"", alias));
////////            json = json.replace("\"cas-", String.format("\"%s-", alias));
////////            json = json.replace("manage01", domainId);
////////
////////            json = json.replace("ucds", alias);
////////            json = json.replace("cloud01", domainId);
////////
////////            json = json.replace("dcms", alias);
////////            json = json.replace("dcmswebservice", alias);
////////            json = json.replace("freeswitch-wgw", alias);
////////
////////            json = json.replace("test-by-wyf", platformId);
////////            json = json.replace("someTest", platformId);
////////            json = json.replace("jhkzx-1", platformId);
////////            json = json.replace("test08", platformId);
////////            json = json.replace("test48", platformId);
////////            json = json.replace("k8s-test", platformId);
////////
////////            json = json.replace("base-volume/db/oracle/sql", String.format("base-volume/db/%s/sql", K8sObjectTemplatePo.ALIAS));
////////            json = json.replace("base-volume/db/oracle/data", String.format("base-volume/db/%s/data", K8sObjectTemplatePo.ALIAS));
////////
////////            json = json.replace("base-volume/db/mysql/sql", String.format("base-volume/db/%s/sql", K8sObjectTemplatePo.ALIAS));
////////            json = json.replace("base-volume/db/mysql/data", String.format("base-volume/db/%s/data", K8sObjectTemplatePo.ALIAS));
//////
//////            json = json.replace("202005-test", K8sObjectTemplatePo.PLATFORM_ID);
//////
//////            K8sTemplatePo po = gson.fromJson(json, K8sTemplatePo.class);
//////            po.setLabels(labels);
//////            po.getObjectTemplate().setLabels(labels);
//////            k8sTemplateMapper.update(po);
//////            logger.error(String.format("template=%s", gson.toJson(po)));
//////            Map<String, String> labels = template.getLabels();
//////            if(labels.containsKey(appTypeLabel) && labels.get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
//////                String name = null;
//////                for(String key : labels.keySet()){
//////                    if(key.equals(appTypeLabel) || key.equals(ccodVersionLabel) || key.equals(appTagLabel))
//////                        continue;
//////                    name = labels.get(key);
//////                    break;
//////                }
//////                labels.put(name, forAllVersion);
//////                labels.remove(appNameLabel);
//////                template.setLabels(labels);
//////                template.getObjectTemplate().setLabels(labels);
//////                k8sTemplateMapper.update(template);
//////            }
////        }
//        List<K8sTemplatePo> updateList = templateList.stream().filter(t->t.getLabels().containsKey(appTypeLabel) && !t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name)).collect(Collectors.toList());
//        for(K8sTemplatePo template : updateList){
////                List<V1Container> inits = t.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().getInitContainers();
////                if(inits != null && inits.size() == 1){
////                    inits.get(0).setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION));
////                }
////                t.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().setInitContainers(inits);
////                t.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().getInitContainers().forEach(c->c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION)));
//            K8sTemplatePo po = gson.fromJson(gson.toJson(template), K8sTemplatePo.class);
//            List<V1Container> inits = po.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().getInitContainers();
//            for(V1Container c : inits){
//                c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION));
//            }
//            po.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().setInitContainers(new ArrayList<>());
//            po.getObjectTemplate().getDeployments().get(0).getSpec().getTemplate().getSpec().setInitContainers(inits);
//            k8sTemplateMapper.update(po);
//        }
////        templateList.forEach(t->logger.error(String.format("template=%s", gson.toJson(t))));
//    }

    @Override
    public List<K8sObjectTemplatePo> getK8sTemplates() {
        return this.objectTemplateList;
    }


    public K8sTemplatePo addNewAppK8sTemplate(Map<String, String> labels, List<V1Deployment> deployments, List<V1StatefulSet> statefulSets, List<V1Service> services, ExtensionsV1beta1Ingress ingress, List<V1Endpoints> endpoints, List<V1ConfigMap> configMaps) throws ParamException
    {
        logger.debug(String.format("create template for %s : deployments=%s, statefulSets=%s, services=%s, ingress=%s, endpoints=%s, configMaps=%s",
                gson.toJson(deployments), gson.toJson(statefulSets), gson.toJson(services), gson.toJson(ingress), gson.toJson(endpoints), gson.toJson(configMaps) ));
        if(!labels.containsKey(ccodVersionLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", ccodVersionLabel));
        }
        else if(!labels.containsKey(appTypeLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", appTypeLabel));
        }
        for(K8sObjectTemplatePo template : objectTemplateList){
            if(isEqual(template.getLabels(), labels)){
                throw new ParamException(String.format("template for %s has been defined", gson.toJson(labels)));
            }
        }
        K8sObjectTemplatePo po = new K8sObjectTemplatePo();
        po.setLabels(labels);
        po.setConfigMaps(configMaps);
        po.setDeployments(deployments);
        po.setStatefulSets(statefulSets);
        po.setServices(services);
        po.setIngress(ingress);
        po.setEndpoints(endpoints);
        K8sTemplatePo templatePo = new K8sTemplatePo(po);
        k8sTemplateMapper.insert(templatePo);
        logger.info("template for %s has been added : %s", gson.toJson(labels), gson.toJson(templatePo));
        return templatePo;
    }

    private void resetAppTemplate(K8sObjectTemplatePo template)
    {
        String appType = template.getLabels().get(appTypeLabel);
//        if(!template.getLabels().containsKey(appNameLabel) || !template.getLabels().get(appNameLabel).equals("umg"))
//            return;
        boolean isDomainApp = appType.equals(AppType.THREE_PART_APP.name) || appType.equals(AppType.OTHER.name) ? false : true;
        template.setStatefulSets(template.getStatefulSets().stream().map(s->{
            s.getMetadata().setLabels(new HashMap<>());
            s.getMetadata().getLabels().put(appTypeLabel, appType);
            s.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            s.getSpec().getSelector().setMatchLabels(new HashMap<>());
            s.getSpec().getSelector().getMatchLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            s.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
            s.getSpec().getTemplate().getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            if(isDomainApp){
                s.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getSpec().getSelector().getMatchLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getSpec().getTemplate().getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
            }
            s.getSpec().getTemplate().getSpec().setNodeSelector(new HashMap<>());
            return s;
        }).collect(Collectors.toList()));
        if(template.getConfigMaps() != null){
            template.setConfigMaps(template.getConfigMaps().stream().map(c->{
                c.getMetadata().setLabels(new HashMap<>());
                return c;
            }).collect(Collectors.toList()));
        }
        else{
            template.setConfigMaps(new ArrayList<>());
        }
        if(template.getEndpoints() != null){
            template.setEndpoints(template.getEndpoints().stream().map(e->{
                e.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
                e.getMetadata().setLabels(new HashMap<>());
                e.getSubsets().forEach(s->s.getAddresses().forEach(a->a.setIp("${UMGIP}")));
                return e;
            }).collect(Collectors.toList()));
;
        }
        else{
            template.setEndpoints(new ArrayList<>());
        }
        if(template.getServices() == null){
            template.setServices(new ArrayList<>());
        }
        template.setServices(template.getServices().stream().map(s->{
            s.getMetadata().setLabels(new HashMap<>());
            s.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            if(isDomainApp){
                s.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getMetadata().setName(String.format("%s-%s", K8sObjectTemplatePo.ALIAS, K8sObjectTemplatePo.DOMAIN_ID));
            }
            else{
                s.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
            }
            if(template.getDeployments() != null && template.getDeployments().size() > 0){
                s.getSpec().setSelector(new HashMap<>());
                s.getSpec().getSelector().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    s.getSpec().getSelector().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
            }
            return s;
        }).collect(Collectors.toList()));
        if(template.getDeployments() != null){
            template.setDeployments(template.getDeployments().stream().map(d->{
                d.getMetadata().setLabels(new HashMap<>());
                d.getMetadata().getLabels().put(appTypeLabel, appType);
                d.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    d.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
                if(!isDomainApp)
                    d.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
                else
                    d.getMetadata().setName(String.format("%s-%s", K8sObjectTemplatePo.ALIAS, K8sObjectTemplatePo.DOMAIN_ID));
                d.getSpec().getSelector().setMatchLabels(new HashMap<>());
                d.getSpec().getSelector().getMatchLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                d.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
                d.getSpec().getTemplate().getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    d.getSpec().getSelector().getMatchLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                    d.getSpec().getTemplate().getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
                if(isDomainApp && d.getSpec().getTemplate().getSpec().getInitContainers() != null){
                    List<V1Container> inits = d.getSpec().getTemplate().getSpec().getInitContainers().stream()
                            .map(c->{
                                c.setName(K8sObjectTemplatePo.ALIAS);
                                c.setCommand(new ArrayList<>());
                                c.setArgs(new ArrayList<>());
                                c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION));
                                return c;
                            }).collect(Collectors.toList());
                    d.getSpec().getTemplate().getSpec().setInitContainers(inits);
                }
                d.getSpec().getTemplate().getSpec().getContainers().get(0).setName(String.format("%s-runtime", K8sObjectTemplatePo.ALIAS));
                if(isDomainApp){
                    List<V1Container> runs = d.getSpec().getTemplate().getSpec().getContainers().stream()
                            .map(c->{
                                c.setCommand(new ArrayList<>());
                                c.setArgs(new ArrayList<>());
                                return c;
                            }).collect(Collectors.toList());
                    d.getSpec().getTemplate().getSpec().setContainers(runs);
                }
                return d;
            }).collect(Collectors.toList()));
        }
        else{
            template.setDeployments(new ArrayList<>());
        }
        if(template.getIngress() != null){
            template.getIngress().getMetadata().setLabels(new HashMap<>());
        }
    }

    public K8sTemplatePo updateAppK8sTemplate(Map<String, String> labels, List<V1Deployment> deployments, List<V1StatefulSet> statefulSets, List<V1Service> services, ExtensionsV1beta1Ingress ingress, List<V1Endpoints> endpoints, List<V1ConfigMap> configMaps) throws ParamException
    {
        logger.debug(String.format("updated template for %s : deployments=%s, statefulSets=%s, services=%s, ingress=%s, endpoints=%s, configMaps=%s",
                gson.toJson(labels), gson.toJson(deployments), gson.toJson(statefulSets), gson.toJson(services), gson.toJson(ingress), gson.toJson(endpoints), gson.toJson(configMaps) ));
        if(!labels.containsKey(ccodVersionLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", ccodVersionLabel));
        }
        else if(!labels.containsKey(appTypeLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", appTypeLabel));
        }
        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
        K8sTemplatePo po = null;
        for(K8sTemplatePo template : templateList){
            if(isEqual(template.getLabels(), labels)){
                po = template;
                break;
            }
        }
        if(po == null){
            throw new ParamException(String.format("can not find template to be updated for %s", gson.toJson(labels)));
        }
        po.setLabels(labels);
        po.getObjectTemplate().setConfigMaps(configMaps);
        po.getObjectTemplate().setDeployments(deployments);
        po.getObjectTemplate().setStatefulSets(statefulSets);
        po.getObjectTemplate().setServices(services);
        po.getObjectTemplate().setIngress(ingress);
        po.getObjectTemplate().setEndpoints(endpoints);
        k8sTemplateMapper.update(po);
        logger.info("template for %s has been updated : %s", gson.toJson(labels), gson.toJson(po));
        templateList = k8sTemplateMapper.select();
        objectTemplateList.clear();
        templateList.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        return po;
    }

    public K8sTemplatePo addNewAppK8sTemplateFromExistNamespace(Map<String, String> labels, Map<String, String> selector, String namespace, String k8sApiUrl, String k8sAuthToken)throws ApiException, ParamException{
        for(K8sObjectTemplatePo po : objectTemplateList){
            if(isEqual(po.getLabels(), labels)){
                throw new ParamException(String.format("k8s template for %s exist", gson.toJson(labels)));
            }
            List<V1Deployment> deployments = ik8sApiService.selectNamespacedDeployment(namespace, selector, k8sApiUrl, k8sAuthToken);
            List<V1Service> services = ik8sApiService.selectNamespacedService(namespace, selector, k8sApiUrl, k8sAuthToken);

        }
        return null;
    }


    @Override
    public ExtensionsV1beta1Ingress generateIngress(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException {
        String platformId = platform.getPlatformId();
        String alias = appBase.getAlias();
        String domainId = domain.getDomainId();
        String hostUrl = platform.getHostUrl();
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.INGRESS, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        ExtensionsV1beta1Ingress ingress = (ExtensionsV1beta1Ingress)selectObject;
        ingress.getMetadata().setNamespace(platformId);
        ingress.getMetadata().setName(String.format("%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).setHost(hostUrl);
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).setPath(String.format("/%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().setServiceName(String.format("%s-%s", alias, domainId));
        logger.info(String.format("generate ingress is %s",  gson.toJson(ingress)));
        return ingress;
    }

    @Override
    public V1Service generateCCODDomainAppService(AppUpdateOperationInfo appBase, ServicePortType portType, String portStr, DomainPo domain, PlatformPo platform) throws ParamException {
        AppType appType = appBase.getAppType();
        String alias = appBase.getAlias();
        String domainId = domain.getDomainId();
        String appName = appBase.getAppName();
        String platformId = platform.getPlatformId();
        logger.debug(String.format("generate service for %s(%s) : portType=%s and port=%s", alias, appName, portType.name, portStr));
        if(!portType.equals(ServicePortType.ClusterIP) && !portType.equals(ServicePortType.NodePort))
            throw new ParamException(String.format("can not handle service port type : %s", portType.name));
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Service service = ((List<V1Service>)selectObject).get(0);
        List<PortVo> portList = parsePort(portStr, portType, appType);
        String[] ports = portStr.split(",");
        String name = portType.equals(ServicePortType.NodePort) ? String.format("%s-%s-out", alias, domainId) : String.format("%s-%s", alias, domainId);
        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
        service.getMetadata().getLabels().put(this.appNameLabel, appName);
        service.getMetadata().getLabels().put(this.serviceTypeLabel, portType.equals(ServicePortType.NodePort) ? K8sServiceType.DOMAIN_OUT_SERVICE.name : K8sServiceType.DOMAIN_SERVICE.name);
        service.getMetadata().getLabels().put(appName, alias);
        service.getMetadata().setName(name);
        service.getMetadata().setNamespace(platformId);
        service.getSpec().setSelector(new HashMap<>());
        service.getSpec().getSelector().put(this.domainIdLabel, domainId);
        service.getSpec().getSelector().put(appName, alias);
        service.getSpec().setPorts(new ArrayList<>());
        service.getSpec().setType(portType.name);
        for(PortVo portVo : portList)
        {
            V1ServicePort svcPort = new V1ServicePort();
            svcPort.setPort(portVo.getPort());
            svcPort.setName(portVo.getPort() + "");
            svcPort.setProtocol(portVo.getProtocol());
            if(portType.equals(ServicePortType.NodePort))
                svcPort.setNodePort(portVo.getNodePort());
            else
                svcPort.setTargetPort(new IntOrString(portVo.getTargetPort()));
            service.getSpec().getPorts().add(svcPort);
        }
        logger.info(String.format("service for %s(%s), portType=%s and port=%s)", alias, appName, portType.name, portStr));
        return service;
    }

    @Override
    public V1Deployment generateCCODDomainAppDeployment(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException {
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs() == null ? new ArrayList<>() : domain.getCfgs();
        logger.debug(String.format("generate deployment for %s : domainId=%s", gson.toJson(appBase), domainId));
        String appName = appBase.getAppName();
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        String hostIp = appBase.getHostIp();
        boolean fixedIp = appBase.isFixedIp();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream()
                .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        appBase.setInstallPackage(module.getInstallPackage());
        AppType appType = appBase.getAppType() == null ? module.getAppType() : appBase.getAppType();
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Deployment deploy = ((List<V1Deployment>)selectObject).get(0);
        if(fixedIp){
            deploy.getSpec().getTemplate().getSpec().setNodeSelector(new HashMap<>());
            deploy.getSpec().getTemplate().getSpec().getNodeSelector().put(nodeIpLabel, hostIp);
        }
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(appBase.getBasePath(), appBase.getDeployPath());
        String platformId = platform.getPlatformId();
//        deploy.getMetadata().setNamespace(platformId);
//        deploy.getMetadata().setName(String.format("%s-%s", alias, domainId));
//        deploy.getMetadata().setLabels(new HashMap<>());
//        deploy.getMetadata().getLabels().put(this.appTypeLabel, appType.name);
//        deploy.getMetadata().getLabels().put(this.domainIdLabel, domainId);
//        deploy.getMetadata().getLabels().put(appName, alias);
//        deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
//        deploy.getSpec().getSelector().getMatchLabels().put(this.domainIdLabel, domainId);
//        deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
//        deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
//        deploy.getSpec().getTemplate().getMetadata().getLabels().put(this.domainIdLabel, domainId);
//        deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        List<V1Volume> volumes = generateVolumeForDeployment(deploy, appType, alias, platformId, domainId, appBase.getCfgs(), platform.getCfgs(), domainCfg);
        deploy.getSpec().getTemplate().getSpec().setVolumes(volumes);
        V1Container initContainer = deploy.getSpec().getTemplate().getSpec().getInitContainers().get(0);
//        logger.debug(String.format("set initContainer name : %s", alias));
//        initContainer.setName(alias);
        List<V1VolumeMount> mounts = generateInitContainerMount(initContainer, appType, alias, domainId, basePath, deployPath);
        initContainer.setVolumeMounts(mounts);
        String image = String.format("%s/ccod/%s:%s", this.nexusDockerUrl, appName.toLowerCase(), version.replaceAll("\\:", "-"));
//        logger.debug(String.format("modify image of init container to %s", image));
//        initContainer.setImage(image);
        V1Container runtimeContainer = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
//        logger.debug(String.format("set container name to %s-runtime", alias));
//        runtimeContainer.setName(String.format("%s-runtime", alias));
//        if(appType.equals(AppType.JAR) || appType.equals(AppType.NODEJS)){
//            runtimeContainer.setImage(image);
//        }
        mounts = generateRuntimeContainerMount(runtimeContainer, appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setVolumeMounts(mounts);
        logger.debug(String.format("generate init container commands"));
        String packageFileName = module.getInstallPackage().getFileName();
        List<String> commands = generateCmdForInitContainer(appBase, packageFileName, appBase.getCfgs(), domainId);
        initContainer.setCommand(commands);
        initContainer.setArgs(new ArrayList<>());
        logger.debug(String.format("generate runtime container command"));
        commands = generateCmdForRuntimeContainer(appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setCommand(commands);
        runtimeContainer.setArgs(new ArrayList<>());
        List<V1ContainerPort> containerPorts = generateContainerPortsForRuntimeContainer(appBase.getPorts(), appType);
        logger.debug(String.format("containerPorts of %s runtime container at %s is : %s", alias, domainId, gson.toJson(containerPorts)));
        runtimeContainer.setPorts(containerPorts);
        generateProbeForRuntimeContainer(runtimeContainer, alias, domainId, appType, appBase.getPorts(),
                appBase.getCheckAt(), appBase.getInitialDelaySeconds(), appBase.getPeriodSeconds());
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
        {
            logger.debug(String.format("modify deployment hostnames of hostAliases to %s", platform.getHostUrl()));
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).getHostnames().set(0, platform.getHostUrl());
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).setIp((String)platform.getParams().get(PlatformBase.k8sHostIpKey));
        }
        logger.info(String.format("generated deployment for %s : %s", gson.toJson(appBase), gson.toJson(deploy)));
        return deploy;
    }

    private List<PortVo> parsePort(String portStr, ServicePortType portType, AppType appType) throws ParamException
    {
        List<PortVo> portList = new ArrayList<>();
        String[] ports = portStr.split(",");
        for(String thePort : ports)
        {
            if(!thePort.matches(this.portRegex))
                throw new ParamException(String.format("%s is illegal port string", thePort));
            PortVo portVo = PortVo.parse(thePort, portType);
            if(portType.equals(ServicePortType.ClusterIP) && portVo.getTargetPort() == 0)
            {
                switch (appType)
                {
                    case BINARY_FILE:
                    case NODEJS:
                        portVo.setTargetPort(portVo.getPort());
                        break;
                    case TOMCAT_WEB_APP:
                    case RESIN_WEB_APP:
                    case JAR:
                        portVo.setTargetPort(8080);
                        break;
                    default:
                        throw new ParamException(String.format("can not handle port for appType=%s", appType.name));
                }
            }
            portList.add(portVo);
        }
        return portList;
    }

    private List<V1VolumeMount> generateRuntimeContainerMount(V1Container runtimeContainer, AppBase appBase, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        logger.debug(String.format("generate runtime container volume mount"));
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath());
        String alias = appBase.getAlias();
        AppType appType = appBase.getAppType();
        Map<String, V1VolumeMount> volumeMountMap = runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        String pkgPath = getAbsolutePath(basePath, deployPath).replaceAll("/$", "");
        String logPath = appType.equals(AppType.TOMCAT_WEB_APP) ? pkgPath.replaceAll("/[^/]+$", "/logs") : pkgPath.replaceAll("/[^/]+$", "/log");
        volumeMountMap.get("ccod-runtime").setMountPath(logPath);
        volumeMountMap.get("ccod-runtime").setSubPath(alias);
        logger.debug(String.format("modify volume mount ccod-runtime to %s", gson.toJson(volumeMountMap.get("ccod-runtime"))));
        if(domainCfg != null && domainCfg.size() > 0)
        {
            String mountName = String.format("%s-volume", domainId);
            logger.debug(String.format("add domain public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s", domainId));
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            String mountName = String.format("%s-volume", platformId);
            logger.debug(String.format("add platform public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s", platformId));
        }
        if(volumeMountMap.containsKey("war")){
            volumeMountMap.get("war").setMountPath(deployPath);
        }
        if(volumeMountMap.containsKey("binary-file")){
            volumeMountMap.get("binary-file").setMountPath(basePath);
        }
        if(appType.equals(AppType.NODEJS)){
            String mountName = String.format("%s-%s-volume", alias, domainId);
            logger.debug(String.format("add app config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s-%s", alias, domainId));
        }
        return new ArrayList<>(volumeMountMap.values());
    }

    private List<V1VolumeMount> generateInitContainerMount(V1Container initContainer, AppType appType, String alias, String domainId, String basePath, String deployPath) throws ParamException
    {
        if(appType.equals(AppType.NODEJS)){
            return new ArrayList<>();
        }
        logger.debug(String.format("generate init container volume mount"));
        Map<String, V1VolumeMount> volumeMountMap = initContainer.getVolumeMounts().stream()
                .collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        String mountName = String.format("%s-%s-volume", alias, domainId);
        if(!volumeMountMap.containsKey(mountName))
        {
            logger.debug(String.format("add mount %s to init container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(String.format("%s-%s-volume", alias, domainId));
            volumeMountMap.put(mountName, mount);
        }
        volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s-cfg", alias));
        return new ArrayList<>(volumeMountMap.values());
    }

    private List<V1Volume> generateVolumeForDeployment(V1Deployment deploy, AppType appType, String alias, String platformId, String domainId, List<AppFileNexusInfo> appCfgs, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg)
    {
        logger.debug(String.format("generate volumes for %s at %s", alias, domainId));
        Map<String, V1Volume> volumeMap = deploy.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        logger.debug(String.format("generate app configMap volume"));
        String configMapName = String.format("%s-%s", alias, domainId);
        V1Volume volume = generateConfigMapVolume(configMapName, appCfgs);
        volumeMap.put(configMapName, volume);
        if(domainCfg != null && domainCfg.size() > 0)
        {
            logger.debug(String.format("generate domain public configMap volume"));
            configMapName = domainId;
            volume = generateConfigMapVolume(configMapName, domainCfg);
            volumeMap.put(configMapName, volume);
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            logger.debug(String.format("generate platform public configMap volume"));
            configMapName = platformId;
            volume = generateConfigMapVolume(configMapName, platformCfg);
            volumeMap.put(configMapName, volume);
        }
//        volumeMap.get("ccod-runtime").getHostPath().setPath(String.format("/var/ccod-runtime/%s/%s", platformId, domainId));
//        volumeMap.get("ccod-runtime").getHostPath().setType("DirectoryOrCreate");
//        logger.debug(String.format("modify ccod-runtime volume to %s", gson.toJson(volumeMap.get("ccod-runtime"))));
//        if(volumeMap.containsKey("core"))
//        {
//            volumeMap.get("core").getHostPath().setPath(String.format("/home/kubernetes/%s/core", platformId));
//            volumeMap.get("core").getHostPath().setType("");
//            logger.debug(String.format("modify core volume to %s", gson.toJson(volumeMap.get("core"))));
//        }
        return new ArrayList<>(volumeMap.values());
    }

    private V1Volume generateConfigMapVolume(String configMapName, List<AppFileNexusInfo> cfgs)
    {
        V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
        source.setItems(new ArrayList<>());
        source.setName(configMapName);
        for (AppFileNexusInfo cfg : cfgs) {
            V1KeyToPath item = new V1KeyToPath();
            item.setKey(cfg.getFileName());
            item.setPath(cfg.getFileName());
            source.getItems().add(item);
        }
        V1Volume volume = new V1Volume();
        volume.setName(String.format("%s-volume", configMapName));
        volume.setConfigMap(source);
        return volume;
    }

    private List<String> generateCmdForRuntimeContainer(AppBase appBase,String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        String alias = appBase.getAlias();
        AppType appType = appBase.getAppType();
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        String basePath = appBase.getBasePath().trim().replaceAll("/$", "");
        if(appType.equals(AppType.JAR) && basePath.equals("/root")){
            throw new ParamException("base path of JAR can not be /root");
        }
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath()).replaceAll("/$", "");
        String execParam;
        switch (appType){
            case JAR:
                execParam = String.format("mkdir %s -p;mv /root/%s %s", deployPath, appBase.getInstallPackage().getFileName(), deployPath);
                break;
            default:
                execParam = "";
                break;
        }
        execParam = String.format("%s;cd %s", execParam, basePath);
        if(platformCfg != null && platformCfg.size() > 0)
        {
            String mountPath = String.format("/cfg/%s", platformId);
            Map<String, List<AppFileNexusInfo>> deployPathCfgMap = platformCfg.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
            for (String cfgDeployPath : deployPathCfgMap.keySet()) {
                String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
                execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                    execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                }
            }
        }
        if(domainCfg != null && domainCfg.size() > 0)
        {
            String mountPath = String.format("/cfg/%s", domainId);
            Map<String, List<AppFileNexusInfo>> deployPathCfgMap = domainCfg.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
            for (String cfgDeployPath : deployPathCfgMap.keySet()) {
                String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
                execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                    execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                }
            }
        }
        switch (appType){
            case NODEJS:
            {
                String mountPath = String.format("/cfg/%s-%s", alias, domainId);
                for(AppFileNexusInfo cfg : appBase.getCfgs()){
                    String cfgSavePath = String.format("%s/%s", cfg.getDeployPath(), cfg.getFileName()).replaceAll("//", "/");
                    execParam = String.format("%s;cd %s;cp %s/%s %s", execParam, basePath, mountPath, cfg.getFileName(), cfgSavePath);
                }
                break;
            }
            default:
                break;
        }
        String cwd;
        switch (appType){
            case BINARY_FILE:
            case JAR:
            case NODEJS:
                cwd = deployPath;
                break;
            default:
                cwd = basePath;
                break;
        }
        if(StringUtils.isNotBlank(appBase.getInitCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getInitCmd());
        switch (appType){
            case NODEJS:
                String deployDir = deployPath.replaceAll(".*/", "");
                String newDir = String.format("%s-%s", alias, domainId);
                execParam = String.format("%s;cd %s;mv %s %s", execParam, deployPath.replaceAll(String.format("/%s$", deployDir), ""), deployDir, newDir);
                basePath = basePath.replaceAll(String.format("/%s$", deployDir), "/" + newDir);
                cwd = deployPath.replaceAll(String.format("/%s$", deployDir), "/" + newDir);
                deployPath = cwd;
                break;
            default:
                break;
        }
        execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getStartCmd());
        if(StringUtils.isNotBlank(appBase.getLogOutputCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getLogOutputCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";"));
        logger.debug(String.format("command for %s at %s is : %s", alias, domainId, String.join(";", commands)));
        return commands;
    }

    private List<String> generateCmdForInitContainer(AppBase appBase, String packageFileName, List<AppFileNexusInfo> appCfgs, String domainId) throws ParamException
    {
        if(appBase.getAppType().equals(AppType.NODEJS)){
            return Arrays.asList(new String[]{"/bin/sh", "-c", ""});
        }
        String alias = appBase.getAlias();
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        AppType appType = appBase.getAppType();
        String appName = appBase.getAppName();
        String theName = packageFileName.replaceAll("\\.war$", "");
        String basePath = appType.equals(AppType.BINARY_FILE) || appType.equals(AppType.JAR) ? "/binary-file" : "/opt";
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath());
        String execParam = "";
        String mountPath = String.format("/cfg/%s-cfg", alias);
        switch (appType)
        {
            case BINARY_FILE:
                execParam = String.format("mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", deployPath, basePath, packageFileName, deployPath, packageFileName);
                break;
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("mkdir %s -p;cd %s;mv /opt/%s %s/%s", deployPath, deployPath, packageFileName, deployPath, packageFileName);
                break;
            case JAR:
            case NODEJS:
                break;
            default:
                throw new ParamException(String.format("error appType %s", appType.name));
        }
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = appCfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        for (String cfgDeployPath : deployPathCfgMap.keySet()) {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            switch (appType)
            {
                case BINARY_FILE:
                case JAR:
                case NODEJS:
                    execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                    break;
                case RESIN_WEB_APP:
                case TOMCAT_WEB_APP:
                    execParam = String.format("%s;mkdir %s -p", execParam, absolutePath.replaceAll(String.format("/%s/", theName), "/"));
                    break;
            }
            execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
            for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                switch (appType)
                {
                    case BINARY_FILE:
                    case JAR:
                        execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                        break;
                    case RESIN_WEB_APP:
                    case TOMCAT_WEB_APP:
                        absolutePath = absolutePath.replaceAll(String.format("/%s/", theName), "/");
                        execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                        absolutePath = absolutePath.replaceAll(String.format("^%s/", deployPath), "");
                        execParam = String.format("%s;jar uf %s %s/%s", execParam, packageFileName, absolutePath, cfg.getFileName());
                        break;
                    default:
                        break;
                }
//                execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
//                if (appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
//                    execParam = String.format("%s;jar uf %s %s/%s", execParam, module.getInstallPackage().getFileName(), absolutePath.replaceAll(String.format("^%s", deployPath), "").replaceAll("^/", "").replaceAll(String.format("^%s/", theName), ""), cfg.getFileName());
            }
        }
        switch (appType)
        {
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("%s;mv /%s/%s /war/%s-%s.war", execParam, deployPath, packageFileName, alias, domainId);
        }
        if(StringUtils.isNotBlank(appBase.getEnvLoadCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, basePath, appBase.getEnvLoadCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";").replaceAll("//", "/"));
        logger.debug(String.format("command of init container is %s", String.join(";", commands)));
        return commands;
    }

    private List<V1ContainerPort> generateContainerPortsForRuntimeContainer(String portStr, AppType appType) throws ParamException
    {
        List<V1ContainerPort> containerPorts = new ArrayList<>();
        List<PortVo> portList = parsePort(portStr, ServicePortType.ClusterIP, appType);
        for(PortVo portVo : portList)
        {
            V1ContainerPort containerPort = new V1ContainerPort();
            containerPort.setProtocol(portVo.getProtocol());
            containerPort.setContainerPort(portVo.getTargetPort());
            containerPorts.add(containerPort);
        }
        return containerPorts;
    }

    private void generateProbeForRuntimeContainer(
            V1Container runtimeContainer, String alias, String domainId, AppType appType, String portStr,
            String checkAt, int initialDelaySeconds, int periodSeconds) throws ParamException
    {
        V1HTTPGetAction get = null;
        V1TCPSocketAction tcp = null;
        V1ExecAction exec = null;
        List<PortVo> portList = parsePort(portStr, ServicePortType.ClusterIP, appType);
        if(StringUtils.isNotBlank(checkAt))
        {
            if(!checkAt.matches(this.healthCheckRegex))
                throw new ParamException(String.format("%s is not legal health check word", checkAt));
            String checkType = checkAt.replaceAll("^.+/", "");
            String check = checkAt.replaceAll(String.format("/%s$", checkType), "");
            String port = check.replaceAll("\\:.+$", "");
            if(checkType.equals("HTTP") || checkType.equals("HTTPS"))
            {
                get = new V1HTTPGetAction();
                get.setPort(new IntOrString(Integer.parseInt(port)));
                String subPath = check.replaceAll(String.format("^%s\\:?", port), "");
                String path = String.format("/%s-%s%s", alias, domainId, subPath).replaceAll("/$", "");
                get.setPath(path);
                get.setScheme(checkType);
            }
            else if(checkType.equals("TCP"))
            {
                tcp = new V1TCPSocketAction();
                tcp.setPort(new IntOrString(Integer.parseInt(port)));
            }
            else
            {
                exec = new V1ExecAction();
                exec.setCommand(Arrays.asList(new String[]{"/bin/sh", "-c", check}));
            }
        }
        else
        {
            int targetPort = portList.get(0).getTargetPort();
            switch (appType) {
                case BINARY_FILE:
                case NODEJS:
                    logger.debug(String.format("monitor port is %d/TCP", targetPort));
                    tcp = new V1TCPSocketAction();
                    tcp.setPort(new IntOrString(targetPort));
                    break;
                case TOMCAT_WEB_APP:
                case RESIN_WEB_APP:
                case JAR:
                    logger.debug(String.format("checked port is %d/HTTPGet", targetPort));
                    get = new V1HTTPGetAction();
                    get.setPort(new IntOrString(targetPort));
                    get.setPath(String.format("/%s-%s", alias, domainId));
                    get.setScheme("HTTP");
                    break;
                default:
                    throw new ParamException(String.format("can not handle probe for appType=%s", appType.name));
            }
        }
        runtimeContainer.getLivenessProbe().setTcpSocket(tcp);
        runtimeContainer.getLivenessProbe().setHttpGet(get);
        runtimeContainer.getLivenessProbe().setExec(exec);
        runtimeContainer.getReadinessProbe().setTcpSocket(tcp);
        runtimeContainer.getReadinessProbe().setHttpGet(get);
        runtimeContainer.getReadinessProbe().setExec(exec);
        if(initialDelaySeconds > 0){
            runtimeContainer.getLivenessProbe().setInitialDelaySeconds(initialDelaySeconds);
            runtimeContainer.getReadinessProbe().setInitialDelaySeconds(initialDelaySeconds);
        }
        if(periodSeconds > 0){
            runtimeContainer.getLivenessProbe().setPeriodSeconds(periodSeconds);
            runtimeContainer.getReadinessProbe().setPeriodSeconds(periodSeconds);
        }
        runtimeContainer.getReadinessProbe().setTcpSocket(tcp);
        runtimeContainer.getReadinessProbe().setHttpGet(get);
        runtimeContainer.getReadinessProbe().setExec(exec);
    }

    /**
     * 根据basePath获得相对路径代表的绝对路径
     *
     * @param basePath     basePath
     * @param relativePath 需要被转换成绝对路径的相对路径
     * @return 相对路径对应的绝对路径
     * @throws ParamException
     */
    private String getAbsolutePath(String basePath, String relativePath) throws ParamException {
        if (relativePath.matches("^/.*"))
            return relativePath;
        else if (relativePath.matches("^\\./.*"))
            return String.format("%s/%s", basePath, relativePath.replaceAll("^\\./", "")).replaceAll("//", "/");
        else if (relativePath.matches("^\\.\\./.*")) {
            int count = 1;
            String str = relativePath.replaceAll("^\\.\\./", "");
            while (str.matches("^\\.\\./.*")) {
                count++;
                str = str.replaceAll("^\\.\\./", "");
            }
            String[] arr = basePath.replaceAll("^/", "").replaceAll("/$", "").split("/");
            String abPath = "/";
            for (int i = 0; i < arr.length - count; i++)
                abPath = String.format("%s%s/", abPath, arr[i]);
            return String.format("%s%s", abPath, str);
        } else
            return String.format("%s/%s", basePath, relativePath).replaceAll("//", "/");
    }

    @Override
    public List<V1Deployment> generateThreeAppDeployment(CCODThreePartAppPo threePartAppPo, PlatformPo platform, boolean isBase) throws ParamException {
        String appName = threePartAppPo.getAppName();
        String version = threePartAppPo.getVersion();
        String alias = threePartAppPo.getAlias();
        String platformId = !isBase ? platform.getPlatformId() : String.format("base-%s", platform.getPlatformId());
        Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find deployment template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Deployment> deploys = (List<V1Deployment>)selectObject;
        for(V1Deployment deploy : deploys){
            deploy.getMetadata().setNamespace(platformId);
            deploy.getMetadata().setName(alias);
            deploy.getMetadata().setLabels(new HashMap<>());
            deploy.getMetadata().getLabels().put(appName, alias);
            deploy.getMetadata().getLabels().put(this.appVersionLabel, version);
            if(StringUtils.isBlank(version)){
                deploy.getMetadata().getLabels().remove(this.appVersionLabel);
            }
            deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
            deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
            deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
            deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
            List<V1Volume> volumes = deploy.getSpec().getTemplate().getSpec().getVolumes().stream()
                    .collect(Collectors.groupingBy(V1Volume::getName)).get(threePartAppPo.getVolume());
//            if(volumes == null){
//                throw new ParamException(String.format("not find %s volume from %s yaml", threePartAppPo.getVolume(), threePartAppPo.getAppName()));
//            }
//            else if(volumes.size() > 1){
//                throw new ParamException(String.format("%s volume from %s yaml multi defined", threePartAppPo.getVolume(), threePartAppPo.getAppName()));
//            }
            if(volumes != null && volumes.size() > 0){
                V1Volume volume = volumes.get(0);
                if(volume.getHostPath() != null){
                    volume.getHostPath().setPath(String.format("/home/kubernetes/volume/%s/%s", platform.getPlatformId(), threePartAppPo.getMountSubPath()).replaceAll("//", "/"));
                }
                else if(volume.getPersistentVolumeClaim() != null){
                    volume.getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platform.getPlatformId()));
                }
            }
        }
        if(appName.equals("oracle")){
            deploys.get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getArgs().set(0, String.format("/tmp/init.sh %s", platform.getHostUrl()));
        }
        logger.info(String.format("selected deployments %s", gson.toJson(deploys)));
        return deploys;
    }

    @Override
    public V1Namespace generateNamespace(String ccodVersion, String platformId, String platformTag) throws ParamException {
        logger.debug(String.format("begin to generate namespace for %s with ccodVersion=%s and platformTag=%s", platformId, ccodVersion, platformTag));
        Map<String, String> k8sMacroData = new HashMap<>();
        k8sMacroData.put(K8sObjectTemplatePo.PLATFORM_ID, platformId);
        Object selectObject = selectK8sObjectForPlatform(K8sKind.NAMESPACE, platformTag, ccodVersion, k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not select namespace template for ccodVersion=%s and platformTag=%s", ccodVersion, platformTag));
        }
        V1Namespace ns = (V1Namespace)selectObject;
//        ns.getMetadata().setNamespace(platformId);
//        ns.getMetadata().setName(platformId);
//        Map<String, String> labels = new HashMap<>();
//        labels.put(this.platformIdLabel, platformId);
//        labels.put(this.ccodVersionLabel, ccodVersion);
//        ns.getMetadata().setLabels(labels);
        logger.info(String.format("selected namespace is %s", gson.toJson(ns)));
        return ns;
    }

    @Override
    public List<V1PersistentVolume> generatePersistentVolume(PlatformPo platform, boolean isBase) throws ParamException {
        String platformId = platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        String nfsServerIp = StringUtils.isBlank(platform.getNfsServerIp()) ? (String)platform.getParams().get(PlatformBase.nfsServerIpKey) : platform.getNfsServerIp();
        Object selectObject = selectK8sObjectForPlatform(K8sKind.PV, platform.getTag(), ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1PersistentVolume> pvList = (List<V1PersistentVolume>)selectObject;
        for(V1PersistentVolume pv : pvList){
//            String name = String.format("base-volume-%s", platformId);
//            pv.getMetadata().setName(name);
//            pv.getSpec().getClaimRef().setNamespace(isBase ? String.format("base-%s", platformId) : platformId);
//            pv.getSpec().getClaimRef().setName(name);
//            pv.getSpec().getNfs().setPath(String.format("/home/kubernetes/volume/%s", platform.getPlatformId()));
//            pv.getSpec().getNfs().setServer(nfsServerIp);
//            pv.getSpec().setStorageClassName(name);
            logger.info(String.format("generate persistentVolume is %s", gson.toJson(pv)));
        }
        return pvList;
    }

    @Override
    public List<V1PersistentVolumeClaim> generatePersistentVolumeClaim(PlatformPo platform, boolean isBase) throws ParamException {
        String ccodVersion = platform.getCcodVersion();
        Object selectObject = selectK8sObjectForPlatform(K8sKind.PVC, platform.getTag(), ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1PersistentVolumeClaim> pvcList = (List<V1PersistentVolumeClaim>)selectObject;
        for(V1PersistentVolumeClaim pvc : pvcList){
//            String name = String.format("base-volume-%s", platform.getPlatformId());
//            pvc.getMetadata().setName(name);
//            pvc.getSpec().setStorageClassName(name);
//            pvc.getSpec().setVolumeName(name);
            pvc.getMetadata().setNamespace(isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId());
            logger.info(String.format("generate pvc is %s", gson.toJson(pvc)));
        }
        return pvcList;
    }

    private Object selectK8sObjectForPlatform(K8sKind kind, String platformTag, String ccodVersion, Map<String, String> k8sMacroData){
        logger.debug(String.format("begin to select platform k8s object %s with platformTag=%s and ccodVersion=%s and k8sMacroData=%s",
                kind.name, platformTag, ccodVersion, gson.toJson(k8sMacroData)));
        List<K8sObjectTemplatePo> templateList = objectTemplateList.stream().filter(t->isMatchForPlatform(t, platformTag, ccodVersion))
                .collect(Collectors.toList());
        for(K8sObjectTemplatePo template : templateList){
            if(template.getK8sObject(kind) != null){
                logger.debug(String.format("template %s with platformTag=%s and ccodVersion=%s been selected : %s", kind.name, platformTag, ccodVersion, gson.toJson(template.getK8sObject(kind))));;
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        logger.warn(String.format("can not select platform k8s object %s with platformTag=%s and ccodVersion=%s", kind.name, platformTag, ccodVersion));
        return null;
    }

    private Object selectK8sObjectForApp(K8sKind kind, String appName, String version, AppType appType, String appTag, String platformTag, String ccodVersion, Map<String, String> k8sMacroData){
        logger.info(String.format("begin to select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s and appTag=%s and platformTag=%s, k8sMacroData=%s",
                kind.name, ccodVersion, appType.name, appName, version, appTag, platformTag, gson.toJson(k8sMacroData)));
        if(appName.equals("umg")){
            System.out.println("ok");
        }
        boolean isDomainApp = appType.equals(AppType.THREE_PART_APP) || appType.equals(AppType.OTHER) ? false : true;
        List<K8sObjectTemplatePo> templateList = objectTemplateList.stream().filter(t->isMatchForApp(t, appType, appTag, ccodVersion))
                .collect(Collectors.toList());
        for(K8sObjectTemplatePo template : templateList){
            if (template.getK8sObject(kind) != null && isTemplateAppVersionMatch(template, appName, version, appType, appTag, platformTag, ccodVersion)) {
                logger.debug(String.format("%s template for %s with version matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        for(K8sObjectTemplatePo template : templateList){
            if(template.getK8sObject(kind) != null && isTemplateAppNameMatch(template, appName, appType, appTag, platformTag, ccodVersion)){
                logger.debug(String.format("%s template for %s with appName matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        if(isDomainApp){
            for(K8sObjectTemplatePo template : templateList){
                if(template.getK8sObject(kind) != null && isTemplateAppTypeMatch(template, appType, appTag, platformTag, ccodVersion)){
                    logger.debug(String.format("%s template for %s with appType matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                    return template.toMacroReplace(kind, k8sMacroData);
                }
            }
        }
        if(StringUtils.isNotBlank(platformTag)){
            platformTag = null;
            for(K8sObjectTemplatePo template : templateList){
                if (template.getK8sObject(kind) != null && isTemplateAppVersionMatch(template, appName, version, appType, appTag, platformTag, ccodVersion)) {
                    logger.debug(String.format("%s template for %s with version matched and platformTag=null: %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                    return template.toMacroReplace(kind, k8sMacroData);
                }
            }
            for(K8sObjectTemplatePo template : templateList){
                if(template.getK8sObject(kind) != null && isTemplateAppNameMatch(template, appName, appType, appTag, platformTag, ccodVersion)){
                    logger.debug(String.format("%s template for %s with appName matched and platformTag=null: %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                    return template.toMacroReplace(kind, k8sMacroData);
                }
            }
            if(isDomainApp){
                for(K8sObjectTemplatePo template : templateList){
                    if(template.getK8sObject(kind) != null && isTemplateAppTypeMatch(template, appType, appTag, platformTag, ccodVersion)){
                        logger.debug(String.format("%s template for %s with appType matched and platformTag=null: %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                        return template.toMacroReplace(kind, k8sMacroData);
                    }
                }
            }
        }
       logger.warn(String.format("can not select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s and appTag=%s and platformTag=%s",
               kind.name, ccodVersion, appType.name, appName, version, appTag, platformTag));
        return null;
    }

    private boolean isMatchForPlatform(K8sObjectTemplatePo template, String platformTag, String ccodVersion){
        if(!template.getLabels().containsKey(ccodVersionLabel) || !template.getLabels().get(ccodVersionLabel).equals(ccodVersion)){
            return false;
        }
        if(!isTagMatch(template.getLabels().get(platformTagLabel), platformTag)){
            return false;
        }
        for(String key : template.getLabels().keySet()){
            if(!key.equals(ccodVersionLabel) && !key.equals(platformTagLabel)){
                return false;
            }
        }
        return true;
    }

    private boolean isMatchForApp(K8sObjectTemplatePo template, AppType appType, String appTag, String ccodVersion){
        if(!template.getLabels().containsKey(appTypeLabel) || !template.getLabels().get(appTypeLabel).equals(appType.name))
            return false;
        if(!template.getLabels().containsKey(ccodVersionLabel) || !template.getLabels().get(ccodVersionLabel).equals(ccodVersion))
            return false;
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag))
            return false;
        return true;
    }


    private boolean isTemplateAppTypeMatch(K8sObjectTemplatePo template, AppType appType, String appTag, String platformTag, String ccodVersion){
        if(!template.getLabels().containsKey(ccodVersionLabel) || !template.getLabels().get(ccodVersionLabel).equals(ccodVersion))
            return false;
        if(!template.getLabels().containsKey(appTypeLabel) || !template.getLabels().get(appTypeLabel).equals(appType.name))
            return false;
        if(!isTagMatch(template.getLabels().get(platformTagLabel), platformTag))
            return false;
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag))
            return false;
        return true;
    }

    private boolean isTemplateAppNameMatch(K8sObjectTemplatePo template, String appName, AppType appType, String appTag, String platformTag, String ccodVersion){
        if(!isTemplateAppTypeMatch(template, appType, appTag, platformTag, ccodVersion))
            return false;
        if(!template.getLabels().containsKey(appName))
            return false;
        if(template.getLabels().get(appName).equals(forAllVersion))
            return true;
        return false;
    }

    private boolean isTemplateAppVersionMatch(K8sObjectTemplatePo template, String appName, String version, AppType appType, String appTag, String platformTag, String ccodVersion){
        if(!isTemplateAppTypeMatch(template, appType, appTag, platformTag, ccodVersion))
            return false;
        if(!template.getLabels().containsKey(appName))
            return false;
        List<String> versions = Arrays.asList(template.getLabels().get(appName).split(","));
        if(versions.contains(version))
            return true;
        return false;
    }

    private boolean isMatch(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() > labels.size())
            return false;
        if (!selector.containsKey(ccodVersionLabel)){
            return false;
        }
        if(!isTagMatch(selector.get(platformTagLabel), labels.get(platformTagLabel))){
            return false;
        }
        if(!isTagMatch(selector.get(domainIdLabel), labels.get(domainIdLabel))){
            return false;
        }
        if(!isTagMatch(selector.get(appTagLabel), labels.get(appTagLabel))){
            return false;
        }
        for(String key : selector.keySet())
        {
            if(key.equals(platformTagLabel) || key.equals(domainTagLabel) || key.equals(appTagLabel)){
                continue;
            }
            if(!labels.containsKey(key))
                return false;
        }
        return true;
    }

    private boolean isTagMatch(String srcTag, String dstTag){
        if(StringUtils.isBlank(srcTag) && StringUtils.isBlank(dstTag)){
            return true;
        }
        else if(StringUtils.isNotBlank(srcTag) && StringUtils.isNotBlank(dstTag)) {
            Set<String> src = Arrays.stream(srcTag.split(",")).map(t->t.trim()).collect(Collectors.toSet());
            Set<String> dst = Arrays.stream(dstTag.split(",")).map(t->t.trim()).collect(Collectors.toSet());
            if(src.size() != dst.size()){
                return false;
            }
            for(String tag : src){
                if(!dst.contains(tag)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isEqual(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() != labels.size())
            return false;
        for(String key : selector.keySet())
        {
            if(!labels.containsKey(key) || !labels.get(key).equals(selector.get(key)))
                return false;
        }
        return true;
    }

    private List<K8sObjectTemplatePo> parseTemplateFromFile(String savePath) throws IOException
    {
        List<K8sObjectTemplatePo> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = br.readLine();
        JsonParser jp = new JsonParser();
        JsonArray je = jp.parse(lineTxt).getAsJsonArray();
        for(JsonElement e : je){
            K8sObjectTemplatePo po = new K8sObjectTemplatePo();
            JsonObject jo = e.getAsJsonObject();
            JsonObject labels = jo.get("labels").getAsJsonObject();
            po.setLabels(new HashMap<>());
            for(String key : labels.keySet()){
                po.getLabels().put(key, labels.get(key).getAsString());
            }
//            po.setLabels(gson.fromJson(jo.get("labels").getAsString(), HashMap.class));
            if(jo.has("deployJson")){
                po.setDeployments(gson.fromJson(jo.get("deployJson").getAsString(), new TypeToken<List<V1Deployment>>() {
                }.getType()));
            }
            if(jo.has("serviceJson")){
                po.setServices(gson.fromJson(jo.get("serviceJson").getAsString(), new TypeToken<List<V1Service>>() {
                }.getType()));
            }
            if(jo.has("ingressJson")){
                po.setIngress(gson.fromJson(jo.get("ingressJson").getAsString(), ExtensionsV1beta1Ingress.class));
            }
            if(jo.has("endpointsJson")){
                po.setEndpoints(gson.fromJson(jo.get("endpointsJson").getAsString(), new TypeToken<List<V1Endpoints>>() {
                }.getType()));
            }
            if(jo.has("podJson")){
                po.setPods(gson.fromJson(jo.get("podJson").getAsString(), new TypeToken<List<V1Pod>>() {
                }.getType()));
            }
            if(jo.has("namespaceJson")){
                po.setNamespaces(gson.fromJson(jo.get("namespaceJson").getAsString(), V1Namespace.class));
            }
            if(jo.has("jobJson")){
                po.setJobs(Arrays.asList(gson.fromJson(jo.get("jobJson").getAsString(), V1Job.class)));
            }
            if(jo.has("secretJson")){
                po.setSecrets(Arrays.asList(gson.fromJson(jo.get("secretJson").getAsString(), V1Secret.class)));
            }
            if(jo.has("persistentVolumeJson")){
                po.setPvList(Arrays.asList(gson.fromJson(jo.get("persistentVolumeJson").getAsString(), V1PersistentVolume.class)));
            }
            if(jo.has("persistentVolumeClaimJson")){
                po.setPvcList(Arrays.asList(gson.fromJson(jo.get("persistentVolumeClaimJson").getAsString(), V1PersistentVolumeClaim.class)));
            }
            list.add(po);
        }
        return list;
    }

    private List<K8sThreePartServiceVo> parseTestThreePartServiceFromFile(String savePath) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = br.readLine();
        List<K8sThreePartServiceVo> list = this.gson.fromJson(lineTxt, new TypeToken<List<K8sThreePartServiceVo>>() {
        }.getType());
        return list;
    }

    @Override
    public List<V1Job> generatePlatformInitJob(PlatformPo platform, boolean isBase) throws ParamException {
        String platformId = isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        String baseDataNexusPath = (String)platform.getParams().get(PlatformBase.baseDataNexusPathKey);
        String platformBaseDataRepository = (String)platform.getParams().get(PlatformBase.baseDataNexusRepositoryKey);
        Object selectObject = selectK8sObjectForPlatform(K8sKind.JOB, platform.getTag(), ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1Job> jobs = (List<V1Job>) selectObject;
        for(V1Job job : jobs){
            String fileName = baseDataNexusPath.replaceAll("^.*/", "");
            job.getMetadata().setNamespace(platformId);
            String workDir = String.format("/root/data/base-volume");
            String arg = String.format("mkdir %s -p;cd %s;wget %s/repository/%s/%s;tar -xvzf %s",
                    workDir, workDir, nexusHostUrl, platformBaseDataRepository, baseDataNexusPath, fileName);
            job.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(Arrays.asList(arg));
            job.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()))
                    .get("data").getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platformId));
        }
        return jobs;
    }

    @Override
    public K8sCCODDomainAppVo getCCODDomainApp(String appName, String alias, String version, String domainId, String platformId, String k8sApiUrl, String k8sAuthToken) throws ParamException, ApiException {
        logger.debug(String.format("get %s[%s(%s)] at %s deploy detail from k8s", alias, appName, version, domainId));
        List<AppModuleVo> modules = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .filter(app->app.getAppName().equals(appName) && app.getVersion().equals(version)).collect(Collectors.toList());
        if(modules.size() == 0)
            throw new ParamException(String.format("%s(%s) has not register or not image", appName, version));
        AppModuleVo module = modules.get(0);
        AppType appType = module.getAppType();
        String name = String.format("%s-%s", alias, domainId);
        V1ConfigMap configMap = this.ik8sApiService.readNamespacedConfigMap(name, platformId, k8sApiUrl, k8sAuthToken);
        Map<String, String> selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.DEPLOYMENT);
        List<V1Deployment> deploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
        if(deploys.size() == 0)
            throw new ParamException(String.format("not select deployment for %s at %s from %s", gson.toJson(selector), platformId, k8sApiUrl));
        else if(deploys.size() > 1)
            throw new ParamException(String.format("select %d deployment for %s at %s from %s", deploys.size(), gson.toJson(selector), platformId, k8sApiUrl));
        selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.SERVICE);
        List<V1Service> services = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
        if(services.size() == 0)
            throw new ParamException(String.format("not select service for %s at %s", gson.toJson(selector), platformId));
        ExtensionsV1beta1Ingress ingress = null;
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
            ingress = this.ik8sApiService.readNamespacedIngress(String.format("%s-%s", alias, domainId), platformId, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo appVo = new K8sCCODDomainAppVo(alias, module, domainId, configMap, deploys.get(0), services, ingress);
        return appVo;
    }

    @Override
    public K8sCCODDomainAppVo generateNewCCODDomainApp(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, InterfaceCallException, IOException{
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String alias = appBase.getAlias();
        String tag = String.format("%s[%s(%s)]", alias, appName, version);
        String domainId = domain.getDomainId();
        logger.debug(String.format("generate k8s object for %s, domainId=%s", gson.toJson(appBase), domainId));
        AppModuleVo module = this.appManagerService.queryAppByVersion(appName, version, true);
        appBase.setInstallPackage(module.getInstallPackage());
        String ccodVersion = module.getCcodVersion();
        AppType appType = module.getAppType();
        String platformId = platform.getPlatformId();
        logger.debug(String.format("generate configMap for %s : cfg=%s", tag, gson.toJson(module.getCfgs())));
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, String.format("%s-%s", alias, domainId),
                appName, appBase.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        V1Deployment deploy = this.generateCCODDomainAppDeployment(appBase, domain, platform);
        List<V1Service> services = new ArrayList<>();
        V1Service service = this.generateCCODDomainAppService(appBase, ServicePortType.ClusterIP, appBase.getPorts(), domain, platform);
        services.add(service);
        if(StringUtils.isNotBlank(appBase.getNodePorts())) {
            service = this.generateCCODDomainAppService(appBase, ServicePortType.NodePort, appBase.getNodePorts(), domain, platform);
            services.add(service);
        }
        ExtensionsV1beta1Ingress ingress = null;
        if(appType.equals(AppType.TOMCAT_WEB_APP) || appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.JAR) || appType.equals(AppType.NODEJS))
            ingress = this.generateIngress(appBase, domain, platform);
        K8sCCODDomainAppVo app = new K8sCCODDomainAppVo(alias, module, domainId, configMap, deploy, services, ingress);
        return app;
    }

    @Override
    public List<K8sOperationInfo> getDeletePlatformAppSteps(String jobId, String appName, String alias, String version, String domainId, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException {
        logger.debug(String.format("generate delete %s[%s(%s)] at %s from k8s steps", alias, appName, version, domainId));
        K8sCCODDomainAppVo app = getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
        String name = String.format("%s-%s", alias, domainId);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step;
        if(app.getIngress() != null)
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, app.getIngress());
            steps.add(step);
        }
        for(V1Service service : app.getServices())
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE, name, K8sOperation.DELETE, service);
            steps.add(step);
        }
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT, name, K8sOperation.DELETE, app.getDeploy());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, name, K8sOperation.DELETE, app.getConfigMap());
        steps.add(step);
        logger.debug(String.format("delete %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateAddPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, boolean isNewPlatform) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs() == null ? new ArrayList<>() : domain.getCfgs();
        logger.debug(String.format("generate step of add %s to %s", gson.toJson(appBase), domainId));
        String appName = appBase.getAppName();
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        String name = String.format("%s-%s", alias, domainId);
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        String hostIp = appBase.getHostIp();
        boolean fixedIp = appBase.isFixedIp();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .filter(app->app.getAppName().equals(appName)&&app.getVersion().equals(version))
                .collect(Collectors.toList()).get(0);
        appBase.setInstallPackage(module.getInstallPackage());
        AppType appType = appBase.getAppType() == null ? module.getAppType() : appBase.getAppType();
        if(!isNewPlatform)
        {
            if(this.ik8sApiService.isNamespacedConfigMapExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("configMap %s exist at %s", name, platformId));
            Map<String, String> selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.DEPLOYMENT);
            if(this.ik8sApiService.isNamespacedDeploymentExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("deployment %s exist at %s", name, platformId));
            List<V1Deployment> deploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
            if(deploys.size() > 0)
                throw new ParamException(String.format("deployment for selector %s exist at %s", gson.toJson(selector), platformId));
            if(this.ik8sApiService.isNamespacedServiceExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("service %s exist at %s", name, platformId));
            if(this.ik8sApiService.isNamespacedServiceExist(String.format("%s-out", name), platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("service %s-out exist at %s", name, platformId));
            selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.SERVICE);
            List<V1Service> services = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
            if(services.size() > 0)
                throw new ParamException(String.format("service for selector %s exist at %s", gson.toJson(selector), platformId));
             if(this.ik8sApiService.isNamespacedIngressExist(name, platformId, k8sApiUrl, k8sAuthToken))
                 throw new ParamException(String.format("ingress %s exist at %s", name, platformId));
        }
        K8sCCODDomainAppVo app = generateNewCCODDomainApp(appBase, domain, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                app.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, app.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                app.getDeploy().getMetadata().getName(), K8sOperation.CREATE, app.getDeploy());
        if(module.isKernal() != null && module.isKernal()) {
            step.setKernal(true);
            step.setTimeout(module.getTimeout());
        }
        steps.add(step);
        for(V1Service service : app.getServices())
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                    service.getMetadata().getName(), K8sOperation.CREATE, service);
            steps.add(step);
        }
        if(app.getIngress() != null)
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                    app.getIngress().getMetadata().getName(), K8sOperation.CREATE, app.getIngress());
            steps.add(step);
        }
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs() == null ? new ArrayList<>() : domain.getCfgs();
        String hostIp = appBase.getHostIp();
        boolean fixedIp = appBase.isFixedIp();
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        logger.debug(String.format("generate update step for %s at %s", alias, domainId));
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        K8sCCODDomainAppVo oriApp= getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo updateApp = generateNewCCODDomainApp(appBase, domain, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step;
        for(V1Service service : updateApp.getServices())
        {
            String portKind = service.getSpec().getType();
            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
            if(isChanged)
            {
                for(V1Service svc : oriServices)
                {
                    step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                            svc.getMetadata().getName(), K8sOperation.DELETE, svc);
                    steps.add(step);
                }
            }
        }
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getDeploy());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getDeploy());
        steps.add(step);
        for(V1Service service : updateApp.getServices())
        {
            String portKind = service.getSpec().getType();
            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
            if(isChanged)
            {
                step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                        service.getMetadata().getName(), K8sOperation.CREATE, service);
                steps.add(step);
            }
        }
        logger.debug(String.format("update %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateDebugPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, int timeout) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(!this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s not exist at %s", platformId, k8sApiUrl));
        String name = String.format("%s-%s", alias, domainId);
        Map<String, String> selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.DEPLOYMENT);;
        List<V1Deployment> srcDeploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
        selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.SERVICE);
        List<V1Service> srcServices = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
        ExtensionsV1beta1Ingress srcIngress = null;
        if(this.ik8sApiService.isNamespacedIngressExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcIngress = this.ik8sApiService.readNamespacedIngress(name, platformId, k8sApiUrl, k8sAuthToken);
        V1ConfigMap srcCm = null;
        if(this.ik8sApiService.isNamespacedConfigMapExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcCm = this.ik8sApiService.readNamespacedConfigMap(name, platformId, k8sApiUrl, k8sAuthToken);
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(srcIngress != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, srcIngress));
        for(V1Service svc : srcServices)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE, svc.getMetadata().getName(), K8sOperation.DELETE, svc));
        for(V1Deployment deploy : srcDeploys)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT, deploy.getMetadata().getName(), K8sOperation.DELETE, deploy));
        if(srcCm != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, name, K8sOperation.DELETE, srcCm));
        List<K8sOperationInfo> addSteps = generateAddPlatformAppSteps(jobId, appBase, domain, platform, true);
        addSteps.forEach(v->{
            if (v.getKind().equals(K8sKind.DEPLOYMENT) && v.getOperation().equals(K8sOperation.CREATE)) {
                v.setKernal(true);
                v.setTimeout(timeout);
            }});
        steps.addAll(addSteps);
        return steps;
    }

    /**
     * 生成选择器用于选择k8s上的ccod域应用相关资源
     * @param appName 应用名
     * @param alias 应用别名
     * @param version 应用版本
     * @param appType 应用类型
     * @param domainId 域id
     * @param kind k8s资源类型
     * @return 生成的选择器
     */
    @Override
    public Map<String, String> getCCODDomainAppSelector(String appName, String alias, String version, AppType appType, String domainId, K8sKind kind)
    {
        Map<String, String> selector = new HashMap<>();
        selector.put(this.domainIdLabel, domainId);
        selector.put(appName, alias);
        return selector;
    }

    @Override
    public List<K8sOperationInfo> generatePlatformCreateSteps(
            String jobId, V1Job job, V1Namespace namespace, List<V1Secret> secrets,
            V1PersistentVolume pv, V1PersistentVolumeClaim pvc, List<CCODThreePartAppPo> threePartApps,
            List<K8sThreePartServiceVo> threePartServices, String nfsServerIp, PlatformPo platform) throws ApiException, ParamException, IOException, InterfaceCallException {
        String platformId = platform.getPlatformId();
        String platformName = platform.getPlatformName();
        String ccodVersion = platform.getCcodVersion();
        String hostUrl = platform.getHostUrl();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s has exist at %s", platformId, k8sApiUrl));
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(namespace == null)
            namespace = generateNamespace(ccodVersion, platformId, platform.getTag());
        if(!namespace.getMetadata().getName().equals(platformId))
            throw new ParamException(String.format("name of namespace should be %s not %s", platformId, namespace.getMetadata().getName()));
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, null, K8sKind.NAMESPACE, platformId, K8sOperation.CREATE, namespace);
        steps.add(step);
        if(secrets == null)
            secrets = new ArrayList<>();
        Map<String, List<V1ObjectMeta>> metaMap = secrets.stream().map(se->se.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("secret %s multi define", name));
        }
        if(!metaMap.containsKey("ssl"))
        {
            V1Secret sslCert = this.ik8sApiService.generateNamespacedSSLCert(platformId, k8sApiUrl, k8sAuthToken);
            secrets.add(sslCert);
        }
        for(V1Secret secret : secrets)
        {
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.SECRET, secret.getMetadata().getName(), K8sOperation.CREATE, secret);
            steps.add(step);
        }
//        if(pv == null)
//            pv = generatePersistentVolume(platform, nfsServerIp);
//        step = new K8sOperationInfo(jobId, platformId, null, K8sKind.PV, pv.getMetadata().getName(), K8sOperation.CREATE, pv);
//        steps.add(step);
//        if(pvc == null)
//            pvc = generatePersistentVolumeClaim(ccodVersion, platformId);
//        step = new K8sOperationInfo(jobId, platformId, null, K8sKind.PVC, pvc.getMetadata().getName(), K8sOperation.CREATE, pvc);
//        steps.add(step);
//        job = job == null ? generatePlatformInitJob(platform) : job;
//        if(job != null)
//        {
//            if(!job.getMetadata().getNamespace().equals(platformId))
//                throw new ParamException(String.format("namespace of job should be %s not %s", platformId, namespace.getMetadata().getNamespace()));
//            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.JOB, job.getMetadata().getName(), K8sOperation.CREATE, job);
//            steps.add(step);
//        }
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, platformId, platformId,
                platform.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(this.nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, null, K8sKind.CONFIGMAP,
                platformId, K8sOperation.CREATE, configMap);
        steps.add(k8sOpt);
        generateThreePartServices(String.format("base-%s", platformId), threePartApps, platform).forEach(s->{
            steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, s.getMetadata().getName(), K8sOperation.CREATE, s));
        });
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateBasePlatformCreateSteps(String jobId, V1Job job, V1Namespace namespace, List<V1Secret> secrets, V1PersistentVolume pv, V1PersistentVolumeClaim pvc, List<CCODThreePartAppPo> threePartApps, String nfsServerIp, String baseNamespaceId, PlatformPo platform) throws ApiException, ParamException, IOException, InterfaceCallException {
        String platformId = String.format("base-%s", platform.getPlatformId());
        String ccodVersion = platform.getCcodVersion();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s has exist at %s", platformId, k8sApiUrl));
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(namespace == null)
            namespace = generateNamespace(ccodVersion, platformId, platform.getTag());
        if(!namespace.getMetadata().getName().equals(platformId))
            throw new ParamException(String.format("name of namespace should be %s not %s", platformId, namespace.getMetadata().getName()));
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, null, K8sKind.NAMESPACE, platformId, K8sOperation.CREATE, namespace);
        steps.add(step);
        if(secrets == null)
            secrets = new ArrayList<>();
        Map<String, List<V1ObjectMeta>> metaMap = secrets.stream().map(se->se.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("secret %s multi define", name));
        }
        if(!metaMap.containsKey("ssl"))
        {
            V1Secret sslCert = this.ik8sApiService.generateNamespacedSSLCert(platformId, k8sApiUrl, k8sAuthToken);
            secrets.add(sslCert);
        }
        for(V1Secret secret : secrets)
        {
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.SECRET, secret.getMetadata().getName(), K8sOperation.CREATE, secret);
            steps.add(step);
        }
        generatePersistentVolume(platform, true).forEach(v->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.PV, v.getMetadata().getName(), K8sOperation.CREATE, v)));
        generatePersistentVolumeClaim(platform, true).forEach(v->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.PVC, v.getMetadata().getName(), K8sOperation.CREATE, v)));
//        job = job == null ? generatePlatformInitJob(platform, true) : job;
//        if(job != null)
//        {
//            if(!job.getMetadata().getNamespace().equals(platformId))
//                throw new ParamException(String.format("namespace of job should be %s not %s", platformId, namespace.getMetadata().getNamespace()));
//            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.JOB, job.getMetadata().getName(), K8sOperation.CREATE, job);
//            steps.add(step);
//        }
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, platformId, platformId,
                platform.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(this.nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, null, K8sKind.CONFIGMAP,
                platformId, K8sOperation.CREATE, configMap);
        steps.add(k8sOpt);
        for(CCODThreePartAppPo threePartAppPo : threePartApps){
            K8sThreePartAppVo vo = generateK8sThreePartApp(platform, threePartAppPo, true);
            vo.getConfigMaps().forEach(c->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.CONFIGMAP, c.getMetadata().getName(), K8sOperation.CREATE, c)));
            vo.getEndpoints().forEach(e->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.ENDPOINTS, e.getMetadata().getName(), K8sOperation.CREATE, e)));
            vo.getServices().forEach(s->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, s.getMetadata().getName(), K8sOperation.CREATE, s)));
            vo.getDeploys().forEach(d->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.DEPLOYMENT, d.getMetadata().getName(), K8sOperation.CREATE, d, threePartAppPo.getTimeout())));
            vo.getStatefulSets().forEach(s->steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.STATEFULSET, s.getMetadata().getName(), K8sOperation.CREATE, s)));
        }
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateDomainDeploySteps(
            String jobId, PlatformPo platformPo, DomainUpdatePlanInfo plan, List<PlatformAppDeployDetailVo> domainApps,
            boolean isNewPlatform, BizSetDefine setDefine, V1Deployment glsserver) throws ApiException, InterfaceCallException, IOException, ParamException
    {
        String domainId = plan.getDomainId();
        DomainPo domain = plan.getDomain(platformPo.getPlatformId());
        Comparator<AppBase> sort = appManagerService.getAppSort(setDefine);
        List<K8sOperationInfo> steps = new ArrayList<>();
        String platformId = platformPo.getPlatformId();
        String k8sApiUrl = platformPo.getK8sApiUrl();
        String k8sAuthToken = platformPo.getK8sAuthToken();
        Map<String, PlatformAppDeployDetailVo> aliasAppMap = domainApps.stream().collect(Collectors.toMap(o->o.getAlias(), v->v));
        plan.getApps().stream().filter(o->o.getOperation().equals(AppUpdateOperation.UPDATE))
                .forEach(o->o.fill(aliasAppMap.get(o.getAlias())));
        List<AppFileNexusInfo> domainCfg = plan.getPublicConfig();
        if(!isNewPlatform && domainCfg != null && domainCfg.size() > 0) {
            if(this.ik8sApiService.isNamespacedConfigMapExist(domainId, platformId, k8sApiUrl, k8sAuthToken)) {
                V1ConfigMap configMap = this.ik8sApiService.readNamespacedConfigMap(domainId, platformId, k8sApiUrl, k8sAuthToken);
                K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.DELETE, configMap);
                steps.add(optInfo);
            }
        }
        if(domainCfg != null && domainCfg.size() > 0) {
            V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, domainId, domainId,
                    domainCfg.stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                    nexusHostUrl, nexusUserName, nexusPassword);
            K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.CREATE, configMap);
            steps.add(optInfo);
        }
        List<AppUpdateOperationInfo> deleteList = plan.getApps().stream()
                .filter(opt->opt.getOperation().equals(AppUpdateOperation.DELETE)).sorted(sort.reversed())
                .collect(Collectors.toList());
        for(AppUpdateOperationInfo optInfo : deleteList) {
            List<K8sOperationInfo> deleteSteps = getDeletePlatformAppSteps(jobId, optInfo.getAppName(),
                    optInfo.getAlias(), optInfo.getVersion(), domainId, platformId, k8sApiUrl, k8sAuthToken);
            steps.addAll(deleteSteps);
        }
        List<AppUpdateOperationInfo> addAndUpdateList = plan.getApps().stream()
                .filter(opt->opt.getOperation().equals(AppUpdateOperation.ADD) || opt.getOperation().equals(AppUpdateOperation.UPDATE))
                .sorted(sort).collect(Collectors.toList());
        for(AppUpdateOperationInfo optInfo : addAndUpdateList) {
            List<K8sOperationInfo> optSteps = optInfo.getOperation().equals(AppUpdateOperation.ADD) ?
                    generateAddPlatformAppSteps(jobId, optInfo, domain, platformPo, isNewPlatform)
                    : generateUpdatePlatformAppSteps(jobId, optInfo, domain, platformPo);
            steps.addAll(optSteps);
            if(optInfo.getAppName().equals("UCDServer")){
                String glsDomId = glsserver.getMetadata().getLabels().get(domainIdLabel);
                glsserver = gson.fromJson(gson.toJson(glsserver), V1Deployment.class);
                glsserver.getMetadata().getLabels().put("restart-reason", String.format("%s-deployment-created", optInfo.getAlias()));
                K8sOperationInfo info = new K8sOperationInfo(jobId, platformId, glsDomId, K8sKind.DEPLOYMENT, glsserver.getMetadata().getName(), K8sOperation.REPLACE, glsserver);
                info.setTimeout(60);
                steps.add(info);
            }
        }
        steps.forEach(s->s.setKernal(false));
        logger.info(String.format("deploy %s %d apps need %d steps", domainId, plan.getApps().size(), steps.size()));
        return steps;
    }

    private List<String> getIntegrationDeploy(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException{
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getTag(), platform.getCcodVersion(), new HashMap<>());
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Deployment deploy = ((List<V1Deployment>)selectObject).get(0);
        return deploy.getSpec().getTemplate().getSpec().getContainers().stream().map(c->c.getName()).collect(Collectors.toList());
    }

    private List<V1Service> generateThreePartServices(String baseNamespace, List<CCODThreePartAppPo> threePartApps, PlatformPo platform) throws ParamException
    {
        List<V1Service> allServices = new ArrayList<>();
        for(CCODThreePartAppPo threePartAppPo : threePartApps){
            Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
            Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
            if(selectObject == null){
                throw new ParamException(String.format("can not find service template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                        threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
            }
            List<V1Service> services = (List<V1Service>)selectObject;
            services.forEach(s->{
                V1Service svc = new V1Service();
                svc.setApiVersion("v1");
                svc.setKind("Service");
                Map<String, String> labels = new HashMap<>();
                labels.put(appTypeLabel, AppType.THREE_PART_APP.name);
                labels.put(appNameLabel, threePartAppPo.getAppName());
                labels.put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
                V1ObjectMeta meta = new V1ObjectMeta();
                meta.setLabels(labels);
                meta.setNamespace(platform.getPlatformId());
                meta.setName(threePartAppPo.getAlias());
                svc.setMetadata(meta);
                V1ServiceSpec spec = new V1ServiceSpec();
                spec.setType("ExternalName");
                spec.setExternalName(String.format("%s.%s.svc.cluster.local", threePartAppPo.getAlias(), baseNamespace));
                svc.setSpec(spec);
                allServices.add(svc);
            });
        }
        return allServices;
    }

    private K8sThreePartAppVo generateK8sThreePartApp(PlatformPo platform, CCODThreePartAppPo threePartAppPo, boolean isBase) throws ParamException
    {
        Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find service template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Service> services = (List<V1Service>)selectObject;
        String platformId = isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId();
        services.forEach(s->{
            s.getMetadata().setNamespace(platformId);
            if(services.size() == 1)
                s.getMetadata().setName(threePartAppPo.getAlias());
            Map<String, String> selector = new HashMap<>();
            selector.put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
            s.getSpec().setSelector(selector);
        });
        List<V1Deployment> deployments = generateThreeAppDeployment(threePartAppPo, platform, isBase);
        selectObject = selectK8sObjectForApp(K8sKind.ENDPOINTS, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find endpoint template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Endpoints> endpoints = (List<V1Endpoints>)selectObject;
        endpoints.forEach(e->{
            e.getMetadata().setNamespace(platformId);
            e.getMetadata().setName(threePartAppPo.getAlias());
            e.setKind("Endpoints");
            e.setApiVersion("v1");
            List<V1EndpointAddress> addresses = new ArrayList<>();
            for(String ip : threePartAppPo.getCfgs().get("ip").split(";")){
                V1EndpointAddress address = gson.fromJson(gson.toJson(e.getSubsets().get(0).getAddresses().get(0)), V1EndpointAddress.class);
                address.setIp(ip);
                addresses.add(address);
            }
            e.getSubsets().get(0).setAddresses(addresses);
        });
        selectObject = selectK8sObjectForApp(K8sKind.CONFIGMAP, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find configMap template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1ConfigMap> configMaps = (List<V1ConfigMap>)selectObject;
        configMaps.forEach(c->c.getMetadata().setNamespace(platformId));
        selectObject = selectK8sObjectForApp(K8sKind.STATEFULSET, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find statefulSet template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1StatefulSet> statefulSets = (List<V1StatefulSet>)selectObject;
        K8sThreePartAppVo appVo = new K8sThreePartAppVo(threePartAppPo.getAppName(), threePartAppPo.getAlias(), threePartAppPo.getVersion(), deployments, statefulSets, services, endpoints, configMaps);
        return appVo;
    }

    @Override
    public List<K8sThreePartServiceVo> generateTestThreePartServices(String ccodVersion, String platformId) throws ApiException, ParamException {
        for(K8sThreePartServiceVo threePartService : this.testThreePartServices)
        {
            threePartService.getService().getMetadata().setLabels(new HashMap<>());
            threePartService.getService().getMetadata().setNamespace(platformId);
            threePartService.getService().getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_SERVICE.name);
            threePartService.getService().getMetadata().getLabels().put(this.appNameLabel, "umg");
            threePartService.getEndpoints().getMetadata().setLabels(new HashMap<>());
            threePartService.getEndpoints().getMetadata().setNamespace(platformId);
            threePartService.getEndpoints().getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_SERVICE.name);
            threePartService.getEndpoints().getMetadata().getLabels().put(this.appNameLabel, "umg");
        }
        return testThreePartServices;
    }

    private List<K8sThreePartServiceVo> getThreePartServices(String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException
    {
        List<K8sThreePartServiceVo> list = new ArrayList<>();
        String name = "umg41";
        V1Endpoints endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        V1Service service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        K8sThreePartServiceVo threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        name = "umg141";
        endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        name = "umg147";
        endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        return list;
    }

//    private Object selectK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version, K8sKind kind) throws ParamException
//    {
//        Map<String, String> params = new HashMap<>();
//        return selectK8sObjectTemplate(ccodVersion, appType, appName, version, params, kind);
//    }

//    private Object selectK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version, Map<String, String> params, K8sKind kind) throws ParamException
//    {
//        if(StringUtils.isBlank(ccodVersion))
//            throw new ParamException("ccodVersion can not be empty for select k8s object template");
//        else if(appType == null && StringUtils.isNotBlank(appName))
//            throw new ParamException(String.format("appType of %s can not be empty for select k8s object template", appName));
//        else if(StringUtils.isBlank(appName) && StringUtils.isNotBlank(version))
//            throw new ParamException(String.format("appName which has version %s can not be empty for select k8s object template", version));
//        if(kind == null)
//            throw new ParamException("kind of select k8s object can not be null");
//        String errMsg = String.format("can not select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s",
//                kind.name, ccodVersion, appType==null ? "":appType.name, appName, version);
//        Map<String, String> selector = new HashMap<>();
//        selector.put(this.ccodVersionLabel, ccodVersion);
//        if(appType != null)
//            selector.put(this.appTypeLabel, appType.name);
//        if(StringUtils.isNotBlank(appName))
//            selector.put(this.appNameLabel, appName);
//        if(StringUtils.isNotBlank(version))
//            selector.put(this.appVersionLabel, version);
//        if(params == null)
//            params = new HashMap<>();
//        for(String key : params.keySet()){
//            selector.put(key, params.get(key));
//        }
//        Object template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        if(StringUtils.isNotBlank(version))
//            selector.remove(this.appVersionLabel);
//        else if(StringUtils.isNotBlank(appName))
//            selector.remove(this.appNameLabel);
//        else
//            selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        if(StringUtils.isNotBlank(appName))
//            selector.remove(this.appNameLabel);
//        else
//            selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        throw new ParamException(errMsg);
//    }

//    private Object selectK8sObjectTemplate(Map<String, String> selector, K8sKind kind)
//    {
//        for(K8sObjectTemplatePo templatePo : this.objectTemplateList)
//        {
//            if(isMatch(selector, templatePo.getLabels()))
//            {
//                switch (kind)
//                {
//                    case ENDPOINTS:
//                        if(templatePo.getEndpoints() != null)
//                            return templatePo.getEndpoints();
//                        break;
//                    case INGRESS:
//                        if(templatePo.getIngress() != null)
//                            return templatePo.getIngress();
//                        break;
//                    case SERVICE:
//                        if(templatePo.getServices() != null)
//                            return templatePo.getServices();
//                        break;
//                    case DEPLOYMENT:
//                        if(templatePo.getDeployments() != null)
//                            return templatePo.getDeployments();
//                        break;
//                    case PVC:
//                        if(templatePo.getPvcList() != null)
//                            return templatePo.getPvcList();
//                        break;
//                    case PV:
//                        if(templatePo.getPvList() != null)
//                            return templatePo.getPvList();
//                        break;
//                    case NAMESPACE:
//                        if(templatePo.getNamespaces() != null)
//                            return templatePo.getNamespaces();
//                        break;
//                    case JOB:
//                        if(templatePo.getJobs() != null)
//                            return templatePo.getJobs();
//                        break;
//                    case SECRET:
//                        if(templatePo.getSecrets() != null)
//                            return templatePo.getSecrets();
//                        break;
//                    case CONFIGMAP:
//                        if(templatePo.getConfigMaps() != null)
//                            return templatePo.getConfigMaps();
//                    case STATEFULSET:
//                        if(templatePo.getStatefulSets() != null)
//                            return templatePo.getStatefulSets();
//                        break;
//                }
//            }
//        }
//        return null;
//    }

    private String getPortString(List<V1ServicePort> ports, String serviceType)
    {
        return ports.stream().map(p->serviceType.equals("ClusterIP")?String.format("%s/%s", p.getPort(), p.getProtocol()) : String.format("%s:%s/%s", p.getPort(), p.getNodePort(), p.getProtocol()))
                .collect(Collectors.joining(","));
    }

    private PlatformAppDeployDetailVo getAppDetailFromK8sObj(String alias, V1Deployment deploy, List<V1Service> services, V1ConfigMap configMap) throws IOException, InterfaceCallException, NexusException, ParamException
    {
        V1Container init = deploy.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(alias);
        AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
        String volume = module.getAppType().equals(AppType.BINARY_FILE) || module.getAppType().equals(AppType.JAR) ? "binary-file" : "war";
        V1Container runtime = deploy.getSpec().getTemplate().getSpec().getContainers().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v)).get(String.format("%s-runtime", alias));
        String basePath = module.getAppType().equals(AppType.NODEJS) ? runtime.getCommand().get(2).replaceAll(";.*", "").replaceAll("^\\s*cd\\s+", "")
                : runtime.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                .get(volume).getMountPath().replaceAll("/$", "");
        if(module.getAppType().equals(AppType.TOMCAT_WEB_APP) || module.getAppType().equals(AppType.RESIN_WEB_APP)){
            basePath = basePath.replaceAll("/webapps$", "");
        }
        PlatformAppDeployDetailVo detail = new PlatformAppDeployDetailVo();
        Map<String, String> labels = deploy.getSpec().getTemplate().getMetadata().getLabels();
        for(V1Service service : services){
            String portStr = getPortString(service.getSpec().getPorts(), service.getSpec().getType());
            if(service.getSpec().getType().equals("ClusterIP"))
                detail.setPorts(portStr);
            else
                detail.setNodePorts(portStr);
        }
        detail.setReplicas(deploy.getStatus().getReplicas());
        detail.setAvailableReplicas(deploy.getStatus().getAvailableReplicas());
        K8sStatus status = this.ik8sApiService.getStatusFromDeployment(deploy);
        detail.setStatus(status.name);
        detail.setInitCmd(module.getInitCmd());
        detail.setAssembleTag(deploy.getMetadata().getName());
        detail.setPlatformId(deploy.getMetadata().getNamespace());
        detail.setAlias(alias);
        detail.setBasePath(basePath);
        String domainId = labels.get(this.domainIdLabel);
        detail.setDomainId(domainId);
        List<String> cmd = runtime.getCommand();
        List<String> commands = Arrays.asList(cmd.get(2).replaceAll(";$", "").replaceAll("\\s*;\\s*", ";").split(";"));
        String cmdTag = "resin.sh";
        if(module.getAppType().equals(AppType.TOMCAT_WEB_APP))
            cmdTag = "startup.sh";
        else if(module.getAppType().equals(AppType.BINARY_FILE) || module.getAppType().equals(AppType.JAR))
            cmdTag = module.getInstallPackage().getFileName();
        else if(module.getAppType().equals(AppType.NODEJS))
            cmdTag = "nginx";
        String cmdRegex = String.format(".*(/|\\s+)?%s(\\s.+|$)", cmdTag);
        String startCmd = null;
        int index = 0;
        for(int i = 0; i < commands.size(); i++){
            String command = commands.get(i);
            if(command.matches(cmdRegex) && !command.matches("^(mv|cp|touch) .+")){
                startCmd = command;
                index = i;
            }
        }
        if(startCmd == null){
            throw new ParamException(String.format("can not parse startCmd from %s at container %s in deployment %s",
                    String.join(";", commands), runtime.getName(), deploy.getMetadata().getName()));
        }
        String initCmd = String.join(";", commands.subList(0, index));
        String logOutputCmd = String.join(";", commands.subList(index+1, commands.size()));
        detail.setStartCmd(startCmd);
        detail.setInitCmd(initCmd);
        detail.setLogOutputCmd(logOutputCmd);
        if(runtime.getLivenessProbe() != null){
            detail.setInitialDelaySeconds(runtime.getLivenessProbe().getInitialDelaySeconds());
            detail.setPeriodSeconds(runtime.getLivenessProbe().getPeriodSeconds());
            if(runtime.getLivenessProbe().getHttpGet() != null){
                detail.setCheckAt(String.format("%d/%s", runtime.getLivenessProbe().getHttpGet().getPort().getIntValue(), runtime.getLivenessProbe().getHttpGet().getScheme()));
            }
            else if(runtime.getLivenessProbe().getTcpSocket() != null){
                detail.setCheckAt(String.format("%d/TCP", runtime.getLivenessProbe().getTcpSocket().getPort().getIntValue()));
            }
            else if(runtime.getLivenessProbe().getExec() != null){
                detail.setCheckAt(String.format("%s/CMD", runtime.getLivenessProbe().getExec().getCommand()));
            }
        }
        detail.setEnvLoadCmd(init.getCommand().get(2));
        if(configMap != null){
            String mountPath = runtime.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, v->v.getMountPath())).get(volume);
            List<AppFileNexusInfo> cfgs = restoreConfigFileFromConfigMap(configMap, init.getCommand().get(2), detail.getBasePath(), volume, mountPath);
            detail.setCfgs(cfgs);
        }
        detail.fill(module);
        return detail;
    }

    private List<AppFileNexusInfo> restoreConfigFileFromConfigMap(V1ConfigMap configMap, String cfgCreateCmd, String basePath, String volume, String mountPath) throws IOException, InterfaceCallException, NexusException
    {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String saveDir = String.format("%s/cfgs/%s/%s", System.getProperty("user.dir"), configMap.getMetadata().getName(), sf.format(now));
        String repository = String.format("restoredFromK8s/%s/%s", configMap.getMetadata().getName(), sf.format(now));
        List<DeployFileInfo> fileList = new ArrayList<>();
        String[] cmds = cfgCreateCmd.replaceAll(String.format("(^|;)/%s", volume), mountPath)
                .replaceAll("//", "").replaceAll("^\\s+", "").replaceAll("\\s$", "")
                .replaceAll("\\s*;\\s*", ";").split(";");
        Map<String, String> cpMap = new HashMap<>();
        Arrays.stream(cmds).filter(s->s.matches("^cp\\s+.+")).forEach(s->{
            String[] arr = s.split("\\s+");
            String fileName = arr[1].replaceAll(".*/", "");
            String deployPath = arr[2].replaceAll(String.format("%s$", fileName), "").replaceAll("/$", "");
            if(deployPath.matches(String.format("^%s(/.*|$)", basePath))) {
                deployPath = "./" + deployPath.replaceAll(String.format("^%s", ""), "").replaceAll("^/", "");
            }
            else if(StringUtils.isBlank(deployPath)){
                deployPath = "./";
            }
            cpMap.put(fileName, deployPath);
        });
        for(String fileName : configMap.getData().keySet()){
            if(cpMap.containsKey(fileName)){
                String filePath = FileUtils.saveContextToFile(saveDir, fileName, configMap.getData().get(fileName), true);
                DeployFileInfo fileInfo = new DeployFileInfo(filePath);
                fileList.add(fileInfo);
            }
            else {
                logger.error(String.format("%s for %s not used", fileName, configMap.getMetadata().getName()));
            }

        }
        List<NexusAssetInfo> assets = nexusService.uploadRawComponent(nexusHostUrl, nexusUserName, nexusPassword, platformAppCfgRepository, repository, fileList.toArray(new DeployFileInfo[0]));
        return assets.stream().map(a->new AppFileNexusInfo(a, cpMap.get(a.getNexusAssetFileName()))).collect(Collectors.toList());
    }

    @Override
    public List<PlatformAppDeployDetailVo> getPlatformAppDetailFromK8s(PlatformPo platform, boolean isGetCfg) throws ApiException, ParamException, InterfaceCallException, NexusException, IOException
    {
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        List<V1Deployment> deployments = this.ik8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> services = this.ik8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, V1ConfigMap> configMapMap = isGetCfg ? this.ik8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken)
                .stream().collect(Collectors.toMap(s->s.getMetadata().getName(), v->v)) : new HashMap<>();
        List<PlatformAppDeployDetailVo> details = new ArrayList<>();
        for(V1Deployment deployment : deployments){
            boolean isDomainApp = isCCODDomainAppDeployment(deployment);
            logger.debug(String.format("deployment %s is ccod domain app deployment : %b", deployment.getMetadata().getName(), isDomainApp));
            if(!isDomainApp){
                continue;
            }
            String domainId = deployment.getMetadata().getLabels().get(this.domainIdLabel);
            for(V1Container init : deployment.getSpec().getTemplate().getSpec().getInitContainers()){
                V1ConfigMap configMap = configMapMap.get(String.format("%s-%s", init.getName(), domainId));
                if(configMap == null && isGetCfg){
                    logger.error(String.format("can not find configMap %s-%s", init.getName(), domainId));
                    continue;
                }
                List<V1Service> relativeSvcs = services.stream().filter(s->isMatch(s.getSpec().getSelector(), deployment.getSpec().getTemplate().getMetadata().getLabels()))
                        .collect(Collectors.toList());
                try{
                    PlatformAppDeployDetailVo detail = this.getAppDetailFromK8sObj(init.getName(), deployment, relativeSvcs, configMap);
                    details.add(detail);
                }
                catch (Exception ex){
                    logger.error(String.format("get %s deploy detail for %s exception", platform.getPlatformName(), platform.getPlatformId()), ex);
                }
            }

        }
        return details;
    }


    /**
     * 用来判断deployment上运行的是否是ccod域模块
     * @param deployment
     * @return
     */
    private boolean isCCODDomainAppDeployment(V1Deployment deployment)
    {
        System.out.println(deployment.getMetadata().getName());
        if(deployment.getMetadata().getName().equals("upload-api01"))
            System.out.println("haha");
        String deployName = deployment.getMetadata().getName();
        String errTag = String.format("so %s is not ccod domain app deployment", deployName);
        Map<String, String> labels = deployment.getMetadata().getLabels();
        if(labels == null || labels.size() == 0 || !labels.containsKey(this.domainIdLabel) || !labels.containsKey(this.appTypeLabel)){
            logger.warn(String.format("deployment labels not container %s or %s tag, %s", domainIdLabel, appTypeLabel, errTag));
            return false;
        }
        AppType appType = AppType.getEnum(labels.get(appTypeLabel));
        if(appType == null){
            logger.warn(String.format("appType %s is not been supported, %s", labels.get(appTypeLabel), errTag));
            return false;
        }
        switch (appType){
            case BINARY_FILE:
            case RESIN_WEB_APP:
            case TOMCAT_WEB_APP:
            case JAR:
            case NODEJS:
                break;
            default:
                logger.warn(String.format("appType %s is not supported by ccod domain app, %s", appType.name, errTag));
                return false;
        }
        String domainId = labels.get(this.domainIdLabel);
        List<V1Container> initList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getInitContainers();
        List<V1Container> runtimeList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getContainers();
        if(initList.size() != runtimeList.size()){
            logger.warn(String.format("%s init container count not equal runtime count, %s", deployName, errTag));
            return false;
        }
        if(deployment.getSpec().getTemplate().getSpec().getVolumes() == null || deployment.getSpec().getTemplate().getSpec().getVolumes().size() == 0){
            logger.warn(String.format("deployment %s has not any volume, %s", deployName, errTag));
            return false;
        }
        if(appType.equals(AppType.NODEJS)){
            return true;
        }
        Map<String, V1Volume> volumeMap = deployment.getSpec().getTemplate().getSpec().getVolumes().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v));
        try{
            Map<String, V1Container> runtimeMap = runtimeList.stream().collect(Collectors.toMap(c->c.getName(), v->v));
            for(V1Container init : initList){
                V1Container runtime = runtimeMap.get(String.format("%s-runtime", init.getName()));
                if(runtime == null){
                    logger.warn(String.format("can not find container %s-runtime at deployment %s, %s"
                            , runtime.getName(), deployName, errTag));
                    return false;
                }
                if(runtime.getCommand() == null || runtime.getCommand().size() != 3 || !runtime.getCommand().get(0).equals("/bin/sh") || !runtime.getCommand().get(1).equals("-c")){
                    logger.warn(String.format("deployment %s %s container command is not wanted, %s", deployName, runtime.getName(), errTag));
                    return false;
                }

                if(init.getCommand() == null || init.getCommand().size() != 3 || !init.getCommand().get(0).equals("/bin/sh") || !init.getCommand().get(1).equals("-c")){
                    logger.warn(String.format("deployment %s %s container command is not wanted, %s",
                            deployName, init.getName(), errTag));
                    return false;
                }
                if(init.getVolumeMounts() == null || init.getVolumeMounts().size() == 0){
                    logger.warn(String.format("deployment %s %s container has not any volumeMount, %s", deployName, init.getName(), errTag));
                    return false;
                }
                Map<String, V1VolumeMount> initMountMap = init.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                if(runtime.getVolumeMounts() == null || runtime.getVolumeMounts().size() == 0){
                    logger.warn(String.format("deployment %s %s container has not any volumeMount, %s", deployName, runtime.getName(), errTag));
                    return false;
                }
                Map<String, V1VolumeMount> runtimeMountMap = runtime.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
                if(!module.getAppType().equals(appType)){
                    logger.warn(String.format("appTye %s is not equal with registered %s, %s", appType.name, module.getAppType().name, errTag));
                    return false;
                }
                String volume = appType.equals(AppType.BINARY_FILE) || appType.equals(AppType.JAR) ? "binary-file" : "war";
                if(!volumeMap.containsKey(volume)){
                    logger.warn(String.format("deployment %s not find %s volume, %s", deployName, volume, errTag));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s volume, %s", deployName, init.getName(), volume, errTag));
                    return false;
                }
                else if(!runtimeMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s volume, %s", deployName, runtime.getName(), volume, errTag));
                    return false;
                }
                volume = String.format("%s-%s-volume", init.getName(), domainId);
                if(!volumeMap.containsKey(volume)){
                    logger.warn(String.format("deployment %s not find %s configMap volume, %s", deployName, volume, errTag));
                    return false;
                }
                else if(volumeMap.get(volume).getConfigMap() == null){
                    logger.warn(String.format("deployment %s %s volume is not configMap, %s", deployName, volume, errTag));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s configMap volume, %s", deployName, init.getName(), volume, errTag));
                    return false;
                }
            }
        }
        catch (Exception ex) {
            logger.error(String.format("parse deployment exception, %s", errTag), ex);
            return false;
        }
        return true;
    }

    @Override
    public PlatformAppDeployDetailVo getPlatformAppDetailFromK8s(PlatformPo platform, String domainId, String appName, String alias, boolean isGetCfg) {
        try{
            Map<String, String> selector = new HashMap<>();
            selector.put(this.domainIdLabel, domainId);
            selector.put(appName, alias);
            List<V1Deployment> deployments = this.ik8sApiService.selectNamespacedDeployment(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            if(deployments.size() == 0 || deployments.size() > 1){
                throw new ParamException(String.format("%s has find %d deployment for %s", platform.getPlatformId(), deployments.size(), gson.toJson(selector)));
            }
            V1Deployment deployment = deployments.get(0);
            if(!isCCODDomainAppDeployment(deployment)){
                throw new ParamException(String.format("deployment %s for %s is illegal ccod domain app deployment",
                        deployment.getMetadata().getName(), gson.toJson(selector)));
            }
            V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().stream()
                    .collect(Collectors.toMap(k->k.getName(), v->v)).get(alias);
            if(initContainer == null){
                throw new ParamException(String.format("can not find container for %s", gson.toJson(selector)));
            }
            V1ConfigMap configMap = isGetCfg ? ik8sApiService.readNamespacedConfigMap(String.format("%s-%s", alias, domainId), platform.getPlatformId(), platform.getK8sApiUrl(), platform.getK8sAuthToken()) : null;
            List<V1Service> services = this.ik8sApiService.selectNamespacedService(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            return getAppDetailFromK8sObj(alias, deployment, services, configMap);
        }
        catch (Exception ex){
            logger.debug(String.format("get detail for 5s(%s) at %s of %s exception", alias, appName, domainId, platform.getPlatformId()), ex);
            PlatformAppDeployDetailVo detail = new PlatformAppDeployDetailVo();
            detail.setPlatformId(platform.getPlatformId());
            detail.setDomainId(domainId);
            detail.setAppName(appName);
            detail.setAlias(alias);
            detail.setStartCmd("ERROR");
            return detail;
        }
    }
}
