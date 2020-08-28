package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.config.LocalDateSerializer;
import com.channelsoft.ccod.support.cmdb.config.LocalTimeSerializer;
import com.channelsoft.ccod.support.cmdb.constant.K8sStatus;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javax.annotation.PostConstruct;

/**
 * @ClassName: K8sApiServiceImpl
 * @Author: lanhb
 * @Description: IK8sApiService接口的实现类
 * @Date: 2020/5/27 18:02
 * @Version: 1.0
 */
@Service
public class K8sApiServiceImpl implements IK8sApiService {

    private final static Logger logger = LoggerFactory.getLogger(K8sApiServiceImpl.class);

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

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

    @Autowired
    INexusService nexusService;

    protected String testPlatformId = "jhkzx";

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    @PostConstruct
    public void init() throws Exception
    {
//        getTemplateJsonTest();
    }

    @Override
    public V1Pod readNamespacedPod(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read pod %s from %s with namespace %s", name, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Pod pod = apiInstance.readNamespacedPod(name, namespace, null, null, null);
        logger.debug(String.format("find pod %s from %s", gson.toJson(pod), k8sApiUrl));
        return pod;
    }

    @Override
    public List<V1Pod> listNamespacedPod(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list pods at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PodList list = apiInstance.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d pods %s at namespace %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    /**
     * 生成k8s api客户端
     * @param httpApi k8s api url
     * @param token k8s的认证token
     * @return 生成的k8s api客户端
     */
    private ApiClient getConnection(String httpApi, String token) {
        ApiClient client = new ClientBuilder().
                setBasePath(httpApi).setVerifyingSsl(false).
                setAuthentication(new AccessTokenAuthentication(token)).build();
        Configuration.setDefaultApiClient(client);
        return client;
    }

    @Override
    public List<V1Node> listNode(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all node from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NodeList list = apiInstance.listNode(null, null, null, null ,null, null, null,null, null);
        logger.debug(String.format("find %d node %s from %s", list.getItems().size(), gson.toJson(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Node readNode(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get node %s from %s", name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Node node = apiInstance.readNode(name, null, null, null);
        logger.info(String.format("find node %s from %s", gson.toJson(node), k8sApiUrl));
        return node;
    }

    @Override
    public List<V1ConfigMap> listNamespacedConfigMap(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list configMaps at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMapList list = apiInstance.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d configMap %s at %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1ConfigMap readNamespacedConfigMap(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read configMap %s from %s at namespace %s", name, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap configMap = apiInstance.readNamespacedConfigMap(name, namespace, null, null, null);
        logger.debug(String.format("find configMap %s from %s", gson.toJson(configMap), k8sApiUrl));
        return configMap;
    }

    @Override
    public V1ConfigMap createNamespacedConfigMap(String namespace, V1ConfigMap configMap, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to create configMap %s at %s from %s", gson.toJson(configMap), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap create = apiInstance.createNamespacedConfigMap(namespace, configMap, null, null, null);
        logger.info(String.format("configMap created success : %s", gson.toJson(create)));
        return create;
    }

    @Override
    public V1ConfigMap replaceNamespacedConfigMap(String name, String namespace, V1ConfigMap configMap, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to replace configMap %s to %s at %s from %s", name, gson.toJson(configMap), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap replace = apiInstance.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null);
        logger.info(String.format("configMap replace success : %s", gson.toJson(replace)));
        return replace;
    }

    @Override
    public boolean isNamespacedConfigMapExist(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("check configMap %s exist at %s from %s", name, namespace, k8sApiUrl));
        String fieldSelector = String.format("metadata.name=%s", name);
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMapList list = apiInstance.listNamespacedConfigMap(namespace, null, null, null, fieldSelector, null, null, null, null, null);
        boolean exist = list.getItems().size() == 0 ? false : true;
        logger.info(String.format("configMap %s at %s exist : %b", name, namespace, exist));
        return exist;
    }

    @Override
    public List<V1Endpoints> listNamespacedEndpoints(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all endpoints at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1EndpointsList list = apiInstance.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d endpoints %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Endpoints readNamespacedEndpoints(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read endpoints %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Endpoints endpoints = apiInstance.readNamespacedEndpoints(name, namespace, null, null, null);
        logger.info(String.format("find endpoints %s from %s", gson.toJson(endpoints), k8sApiUrl));
        return endpoints;
    }

    @Override
    public V1Endpoints createNamespacedEndpoints(String namespace, V1Endpoints endpoints, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create Endpoints %s at %s from %s", gson.toJson(endpoints), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Endpoints create = apiInstance.createNamespacedEndpoints(namespace, endpoints, null, null, null);
        logger.info(String.format("Endpoints %s created from %s", gson.toJson(create), k8sApiUrl));
        return create;
    }

    @Override
    public V1Endpoints replaceNamespacedEndpoints(String name, String namespace, V1Endpoints endpoints, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace Endpoints %s at %s to %s from %s", name, namespace, gson.toJson(endpoints), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Endpoints replace = apiInstance.replaceNamespacedEndpoints(name, namespace, endpoints, null, null, null);
        logger.info(String.format("Endpoints %s at %s replaced to %s", name, namespace, gson.toJson(replace)));
        return replace;
    }

    @Override
    public V1Status deleteNamespacedEndpoints(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete Endpoints %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deleteNamespacedEndpoints(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("Endpoints %s at %s deleted : %s", name, namespace, gson.toJson(status)));
        return status;
    }

    @Override
    public List<V1Secret> listNamespacedSecret(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list secret at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1SecretList list = apiInstance.listNamespacedSecret(namespace, null ,null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d secret %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Secret readNamespacedSecret(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read secret %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Secret secret = apiInstance.readNamespacedSecret(name, namespace, null, null, null);
        logger.info(String.format("find secret %s at %s from %s", gson.toJson(secret), namespace, k8sApiUrl));
        return secret;
    }

    @Override
    public V1Secret createNamespacedSecret(String namespace, V1Secret secret, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create Secret %s for %s from %s", gson.toJson(secret), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Secret create = apiInstance.createNamespacedSecret(namespace, secret, null, null, null);
        logger.info(String.format("secret %s been created for %s from %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public V1Secret replaceNamespacedSecret(String name, String namespace, V1Secret secret, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace Secret %s at %s to %s from %s", name, namespace, gson.toJson(secret), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Secret replace = apiInstance.replaceNamespacedSecret(name, namespace, secret, null, null, null);
        logger.info(String.format("Secret %s at %s has been replaced to %s from %s", name, namespace, gson.toJson(replace), k8sApiUrl));
        return replace;
    }

    @Override
    public V1Status deleteNamespacedSecret(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete Secret %s at %s from %s", name, name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deleteNamespacedSecret(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("from %s Secret %s at %s delete : %s", k8sApiUrl, name, namespace, gson.toJson(status)));
        return status;
    }

    @Override
    public List<V1Job> listNamespacedJob(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list job at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        BatchV1Api apiInstance = new BatchV1Api();
        V1JobList list = apiInstance.listNamespacedJob(namespace, null ,null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d jobs %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Job readNamespacedJob(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read job %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        BatchV1Api apiInstance = new BatchV1Api();
        V1Job job = apiInstance.readNamespacedJob(name, namespace, null, null, null);
        logger.info(String.format("find secret %s at %s from %s", gson.toJson(job), namespace, k8sApiUrl));
        return job;
    }

    @Override
    public V1Job createNamespacedJob(String namespace, V1Job job, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create job %s for %s from %s", gson.toJson(job), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        BatchV1Api apiInstance = new BatchV1Api();
        V1Job create = apiInstance.createNamespacedJob(namespace, job, null, null, null);
        logger.info(String.format("job %s been created for %s from %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public List<V1PersistentVolumeClaim> listNamespacedPersistentVolumeClaim(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list persistentVolumeClaim at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaimList list = apiInstance.listNamespacedPersistentVolumeClaim(namespace, null ,null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d persistentVolumeClaim %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1PersistentVolumeClaim readNamespacedPersistentVolumeClaim(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query persistentVolumeClaim $s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaim claim = apiInstance.readNamespacedPersistentVolumeClaim(name, namespace, null, null, null);
        logger.info(String.format("find persistentVolumeClaim %s at %s from %s", gson.toJson(claim), namespace, k8sApiUrl));
        return claim;
    }

    @Override
    public V1PersistentVolumeClaim createNamespacedPersistentVolumeClaim(String namespace, V1PersistentVolumeClaim persistentVolumeClaim, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create PersistentVolumeClaim %s for %s from %s", gson.toJson(persistentVolumeClaim), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaim create = apiInstance.createNamespacedPersistentVolumeClaim(namespace, persistentVolumeClaim, null, null, null);
        logger.info(String.format("PersistentVolumeClaim %s been created for %s from %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public V1PersistentVolumeClaim replaceNamespacedPersistentVolumeClaim(String name, String namespace, V1PersistentVolumeClaim persistentVolumeClaim, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace PersistentVolumeClaim %s at %s to %s from %s", name, namespace, gson.toJson(persistentVolumeClaim), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaim replace = apiInstance.replaceNamespacedPersistentVolumeClaim(name, namespace, persistentVolumeClaim, null, null, null);
        logger.info(String.format("PersistentVolumeClaim %s at %s has been replaced to %s from %s", name, namespace, gson.toJson(replace), k8sApiUrl));
        return replace;
    }

    @Override
    public V1Status deleteNamespacedPersistentVolumeClaim(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete PersistentVolumeClaim %s at %s from %s", name, name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deleteNamespacedSecret(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("from %s PersistentVolumeClaim %s at %s delete : %s", k8sApiUrl, name, namespace, gson.toJson(status)));
        return status;
    }

    @Override
    public List<V1PersistentVolume> listPersistentVolume(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list PersistentVolume from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeList list = apiInstance.listPersistentVolume(null, null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d PersistentVolume %s from %s", list.getItems().size(), gson.toJson(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1PersistentVolume readPersistentVolume(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read PersistentVolume %s  from %s", name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume volume = apiInstance.readPersistentVolume(name, null, null, null);
        logger.debug(String.format("find PersistentVolume %s from %s", gson.toJson(volume), k8sApiUrl));
        return volume;
    }

    @Override
    public V1PersistentVolume createPersistentVolume(V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create PersistentVolume %s from %s", gson.toJson(persistentVolume), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume create = apiInstance.createPersistentVolume(persistentVolume, null, null, null);
        logger.info(String.format("create PersistentVolume %s SUCCESS from %s", gson.toJson(create), k8sApiUrl));
        return create;
    }

    @Override
    public V1PersistentVolume replacePersistentVolume(String name, V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace PersistentVolume %s to %s from %s", name, gson.toJson(persistentVolume), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume replace = apiInstance.replacePersistentVolume(name, persistentVolume, null, null, null);
        logger.info(String.format("%s PersistentVolume has been replaced by %s from %s", name, gson.toJson(replace), k8sApiUrl));
        return replace;
    }

    @Override
    public V1Status deletePersistentVolume(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete PersistentVolume %s from %s", name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deletePersistentVolume(name, null, null, null, null, null, null);
        logger.info(String.format("PersistentVolume %s has been delete %s from %s", name, gson.toJson(status), k8sApiUrl));
        return status;
    }

    V1ConfigMap createConfigMapFromFile(String namespace, String configMapName, String fileSavePath, String k8sApiUrl, String authToken) throws ApiException, IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileSavePath)),
                "UTF-8"));
        String lineTxt = null;
        String context = "";
        while ((lineTxt = br.readLine()) != null)
        {
            context += lineTxt + "\n";
        }
        br.close();
        getConnection(k8sApiUrl, authToken);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(configMapName);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap body = new V1ConfigMap();
        return body;
    }

    @Override
    public V1ConfigMap createConfigMapFromNexus(String namespace, String configMapName, String k8sApiUrl, String authToken, List<NexusAssetInfo> cfgs, String nexusHostUrl, String nexusUser, String nexusPwd) throws ApiException, InterfaceCallException, IOException {
        V1ConfigMap body = getConfigMapFromNexus(namespace, configMapName, cfgs, nexusHostUrl, nexusUser, nexusPwd);
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap configMap = apiInstance.createNamespacedConfigMap(namespace, body, null, null,null);
        logger.info(String.format("configMap %s create SUCCESS : %s", configMapName, gson.toJson(configMap)));
        return configMap;
    }

    @Override
    public V1ConfigMap getConfigMapFromNexus(String namespace, String configMapName, List<NexusAssetInfo> cfgs, String nexusHostUrl, String nexusUser, String nexusPwd) throws InterfaceCallException, IOException {
        logger.debug(String.format("get configMap %s at %s from nexus %s", configMapName, namespace, gson.toJson(cfgs)));
        Map<String, String> dataMap = new HashMap<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = sf.format(now);
        String tmpSaveDir = String.format("%s/temp/cfgs/%s/%s", System.getProperty("user.dir"), configMapName, dateStr);
        File saveDir = new File(tmpSaveDir);
        if(!saveDir.exists())
        {
            saveDir.mkdirs();
        }
        for(NexusAssetInfo cfg : cfgs)
        {
            String fileSavePath = this.nexusService.downloadFile(nexusUser, nexusPwd, cfg.getDownloadUrl(), tmpSaveDir, cfg.getNexusAssetFileName());
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileSavePath)),
                    "UTF-8"));
            String lineTxt = null;
            String context = "";
            while ((lineTxt = br.readLine()) != null)
            {
                context += lineTxt + "\n";
            }
            br.close();
            dataMap.put(cfg.getNexusAssetFileName(), context);
        }
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(configMapName);
        V1ConfigMap body = new V1ConfigMap();
        body.setMetadata(meta);
        body.setData(dataMap);
        logger.info(String.format("get configMap %s at %s from nexus SUCCESS", configMapName, namespace));
        return body;
    }

    @Override
    public void deleteNamespacedConfigMap(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete configMap %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        apiInstance.deleteNamespacedConfigMap(name, namespace, null, null, null, null, null, null);
        logger.debug(String.format("delete success"));
    }

    @Override
    public List<V1Deployment> listNamespacedDeployment(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list deployment at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api appsV1Api = new AppsV1Api();
        V1DeploymentList list = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d deployment %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Deployment readNamespacedDeployment(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read deployment %s from %s at %s", name, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api appsV1Api = new AppsV1Api();
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(name, namespace, null, null, null);
        logger.debug(String.format("find deployment %s from %s", gson.toJson(deployment), k8sApiUrl));
        return deployment;
    }

    @Override
    public K8sStatus readNamespacedDeploymentStatus(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read deployment status %s from %s at %s", name, k8sApiUrl, namespace));
        V1Deployment deployment = readNamespacedDeployment(name, namespace, k8sApiUrl, authToken);
        K8sStatus status = K8sStatus.UPDATING;
        if(deployment.getStatus().getAvailableReplicas() == deployment.getStatus().getReplicas())
            status = K8sStatus.ACTIVE;
        logger.info(String.format("status of deployment %s is %s", name, status.name));
        return status;
    }

    @Override
    public V1Deployment createNamespacedDeployment(String namespace, V1Deployment deployment, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create deployment %s for %s at %s", gson.toJson(deployment), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Deployment create = apiInstance.createNamespacedDeployment(namespace, deployment, null, null, null);
        logger.info(String.format("deployment %s created for %s at %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public V1Status deleteNamespacedDeployment(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete Deployment %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Status status = apiInstance.deleteNamespacedDeployment(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("delete Deployment %s at %s from %s : %s", name, namespace, k8sApiUrl, gson.toJson(status)));
        return status;
    }

    @Override
    public boolean isNamespacedDeploymentExist(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("check deployment %s exist at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        String fieldSelector = String.format("metadata.name=%s", name);
        V1DeploymentList deploymentList = apiInstance.listNamespacedDeployment(namespace, null, null, null, fieldSelector, null, null, null, null, null);
        boolean exist = deploymentList.getItems().size() == 0 ? false : true;
        logger.info(String.format("deployment %s at %s exist : %b", name, namespace, exist));
        return exist;
    }

    @Override
    public V1Status deleteCollectionNamespacedDeployment(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete all Deployment at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Status status = apiInstance.deleteCollectionNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        logger.info(String.format("delete all Deployment at %s from %s : %s", namespace, k8sApiUrl, gson.toJson(status)));
        return status;
    }

    @Override
    public V1Deployment replaceNamespacedDeployment(String name, String namespace, V1Deployment deployment, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace Deployment %s at %s from %s to %s", name, namespace, k8sApiUrl, gson.toJson(deployment)));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Deployment replace = apiInstance.replaceNamespacedDeployment(name, namespace, deployment, null, null, null);
        logger.info(String.format("replace Deployment %s at %s from %s to %s SUCCESS", name, namespace, k8sApiUrl, gson.toJson(replace)));
        return replace;
    }

    @Override
    public List<V1Deployment> selectNamespacedDeployment(String namespace, Map<String, String> selector, String k8sApiUrl, String authToken) throws ApiException {
        String labelSelector = null;
        if(selector != null && selector.size() > 0)
            labelSelector = String.join(",", selector.keySet().stream().map(key->String.format("%s=%s", key, selector.get(key))).collect(Collectors.toList()));
        logger.debug(String.format("select deployment at %s from %s for labelSelector=%s", namespace, k8sApiUrl, labelSelector));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api appsV1Api = new AppsV1Api();
        List<V1Deployment> list = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, labelSelector, null, null, null, null).getItems();
        logger.info(String.format("select %d deployment at %s for labelSelector=%s", list.size(), namespace, labelSelector));
        return list;
    }

    @Override
    public V1Service readNamespacedService(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read service %s at %s from %s ", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Service service = apiInstance.readNamespacedService(name, namespace, null, null, null);
        logger.info(String.format("find service %s from %s ", gson.toJson(service), k8sApiUrl));
        return service;
    }

    @Override
    public List<V1Service> listNamespacedService(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list service at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ServiceList list = apiInstance.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
        logger.info(String.format("find %d service %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Service createNamespacedService(String namespace, V1Service service, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create service %s for %s at %s", gson.toJson(service), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Service create = apiInstance.createNamespacedService(namespace, service, null, null, null);
        logger.debug(String.format("service %s created for %s at %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public V1Status deleteNamespacedService(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete Service %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deleteNamespacedService(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("delete Service %s at %s from %s : %s", name, namespace, k8sApiUrl, gson.toJson(status)));
        return status;
    }

    @Override
    public V1Service replaceNamespacedService(String name, String namespace, V1Service service, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace Service %s at %s from %s to %s", name, namespace, k8sApiUrl, gson.toJson(service)));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Service replace = apiInstance.replaceNamespacedService(name, namespace, service, null, null, null);
        logger.info(String.format("replace Service %s at %s from %s to %s SUCCESS", name, namespace, k8sApiUrl, gson.toJson(replace)));
        return replace;
    }

    @Override
    public List<V1Service> selectNamespacedService(String namespace, Map<String, String> selector, String k8sApiUrl, String authToken) throws ApiException {
        String labelSelector = null;
        if(selector != null && selector.size() > 0)
            labelSelector = String.join(",", selector.keySet().stream().map(key->String.format("%s=%s", key, selector.get(key))).collect(Collectors.toList()));
        logger.debug(String.format("select service at %s from %s for labelSelector=%s", namespace, k8sApiUrl, labelSelector));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        List<V1Service> services = apiInstance.listNamespacedService(namespace, null, null, null, null, labelSelector, null, null, null, null).getItems();
        logger.info(String.format("select %d service at %s for selector %s", services.size(), namespace, gson.toJson(selector)));
        return services;
    }

    @Override
    public boolean isNamespacedServiceExist(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("check exist of service %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String fieldSelector = String.format("metadata.name=%s", name);
        V1ServiceList services = apiInstance.listNamespacedService(namespace, null, null, null, fieldSelector, null, null, null, null, null);
        boolean exist = services.getItems().size() == 0 ? false : true;
        logger.debug(String.format("service %s at %s exist : %b", name, namespace, exist));
        return exist;
    }

    @Override
    public boolean isServicePortChanged(String portKind, V1Service service, List<V1Service> oriServices) {
        if(portKind != "NodePort" && portKind != "ClusterIP")
            return true;
        if(portKind == "ClusterIP")
        {
            Map<Integer, IntOrString> portMap = service.getSpec().getPorts().stream()
                    .collect(Collectors.toMap(port->port.getPort(), v->v.getTargetPort()));
            Map<Integer, IntOrString> oriPortMap = oriServices.stream().flatMap(svc->svc.getSpec().getPorts().stream())
                    .collect(Collectors.toList()).stream().collect(Collectors.toMap(port->port.getPort(), v->v.getTargetPort()));
            if(portMap.size() != oriPortMap.size())
                return true;
            for(int port : portMap.keySet())
            {
                if(!portMap.containsKey(port) || !oriPortMap.get(port).equals(portMap.get(port)))
                    return true;
            }
            return false;
        }
        else
        {
            Map<Integer, Integer> portMap = service.getSpec().getPorts().stream()
                    .collect(Collectors.toMap(port->port.getPort(), v->v.getNodePort()));
            Map<Integer, Integer> oriPortMap = oriServices.stream().flatMap(svc->svc.getSpec().getPorts().stream())
                    .collect(Collectors.toList()).stream().collect(Collectors.toMap(port->port.getPort(), v->v.getNodePort()));
            if(portMap.size() != oriPortMap.size())
                return true;
            for(int port : portMap.keySet())
            {
                if(!oriPortMap.containsKey(port) || oriPortMap.get(port) != portMap.get(port))
                    return true;
            }
            return false;
        }
    }

    /**
     * 确定标签是否被选择器选择
     * @param selector 选择器
     * @param labels 标签
     * @return 选择结果
     */
    private boolean isSelected(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() < labels.size())
            return false;
        for(String key : selector.keySet())
        {
            if(!labels.containsKey(key) || !labels.get(key).equals(selector.get(key)))
                return false;
        }
        return true;
    }

    /**
     * 确定标签是和选择器匹配
     * @param selector 选择器
     * @param labels 标签
     * @return 选择结果
     */
    private boolean isMatch(Map<String, String> selector, Map<String, String> labels)
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

    @Override
    public V1Namespace readNamespace(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("read namespace %s from %s", name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Namespace ns = apiInstance.readNamespace(name, null, null, null);
        logger.debug(String.format("find namespace %s from %s", gson.toJson(ns), k8sApiUrl));
        return ns;
    }

    @Override
    public List<V1Namespace> listNamespace(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list all namespace from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NamespaceList list = apiInstance.listNamespace(null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d namespace %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Namespace createNamespace(V1Namespace namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create namespace %s at %s", gson.toJson(namespace), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Namespace create = apiInstance.createNamespace(namespace,null, null, null);
        logger.debug(String.format("namespace %s created at %s", gson.toJson(create), k8sApiUrl));
        return create;
    }

    @Override
    public V1Namespace createDefaultNamespace(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create default namespace %s at %s", name, k8sApiUrl));
        V1Namespace ns = new V1Namespace();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName(name);
        ns.setMetadata(meta);
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        ns = apiInstance.createNamespace(ns, null, null, null);
        logger.debug(String.format("namespace %s created at %s", gson.toJson(ns), k8sApiUrl));
        return ns;
    }

    @Override
    public V1Status deleteNamespace(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete namespace %s from %s", name, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Status status = apiInstance.deleteNamespace(name, null, null, null, null, null, null);
        logger.info(String.format("delete %s from %s : %s", name, k8sApiUrl, gson.toJson(status)));
        return status;
    }

    @Override
    public boolean isNamespaceExist(String name, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("check namespace %s exist at %s", name, k8sApiUrl));
        List<V1Namespace> namespaces = listNamespace(k8sApiUrl, authToken);
        boolean exist = false;
        for(V1Namespace namespace : namespaces)
        {
            if(namespace.getMetadata().getName().equals(name))
            {
                exist = true;
                break;
            }
        }
        logger.info(String.format("%s is exist : %b", name, exist));
        return exist;
    }

    @Override
    public List<ExtensionsV1beta1Ingress> listNamespacedIngress(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list all Ingress at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1IngressList list = apiInstance.listNamespacedIngress(namespace,null, null, null ,null, null, null, null,null, null);
        logger.info(String.format("find %d Ingress %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public ExtensionsV1beta1Ingress readNamespacedIngress(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query Ingress at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1Ingress ingress = apiInstance.readNamespacedIngress(name, namespace, null, null, null);
        logger.debug(String.format("find ingress %s at %s from %s", gson.toJson(ingress), namespace, k8sApiUrl));
        return ingress;
    }

    @Override
    public ExtensionsV1beta1Ingress createNamespacedIngress(String namespace, ExtensionsV1beta1Ingress ingress, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create ingress %s for %s at %s", gson.toJson(ingress), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1Ingress create = apiInstance.createNamespacedIngress(namespace, ingress, null, null, null);
        logger.debug(String.format("ingress %s created for %s at %s", gson.toJson(create), namespace, k8sApiUrl));
        return create;
    }

    @Override
    public V1Status deleteNamespacedIngress(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete Ingress %s at %s from %s", name, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        V1Status status = apiInstance.deleteNamespacedIngress(name, namespace, null, null, null, null, null, null);
        logger.info(String.format("delete Ingress %s at %s from %s : %s", name, namespace, k8sApiUrl, gson.toJson(status)));
        return status;
    }

    @Override
    public ExtensionsV1beta1Ingress replaceNamespacedIngress(String name, String namespace, ExtensionsV1beta1Ingress ingress, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("replace Ingress %s at %s from %s to %s", name, namespace, k8sApiUrl, gson.toJson(ingress)));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1Ingress replace = apiInstance.replaceNamespacedIngress(name, namespace, ingress, null, null, null);
        logger.info(String.format("replace Ingress %s at %s from %s to %s SUCCESS", name, namespace, k8sApiUrl, gson.toJson(replace)));
        return replace;
    }

    @Override
    public boolean isNamespacedIngressExist(String name, String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("check ingress %s at %s from %s exist", name, namespace, k8sApiUrl));
        String fieldSelector = String.format("metadata.name=%s", name);
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1IngressList list = apiInstance.listNamespacedIngress(namespace,null, null, null ,fieldSelector, null, null, null,null, null);
        boolean exist = list.getItems().size() == 0 ? false : true;
        logger.info(String.format("ingress %s at %s exist : %b", name, namespace, exist));
        return exist;
    }

    @Override
    public V1Secret generateNamespacedSSLCert(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create ssl cert for %s at %s", namespace, k8sApiUrl));
        V1Secret srcSecret = readNamespacedSecret("ssl", "kube-system", k8sApiUrl, authToken);
        srcSecret.getMetadata().setAnnotations(null);
        srcSecret.getMetadata().setCreationTimestamp(null);
        srcSecret.getMetadata().setNamespace(namespace);
        srcSecret.getMetadata().setSelfLink(null);
        srcSecret.getMetadata().setUid(null);
        srcSecret.getMetadata().setResourceVersion(null);
        logger.info(String.format("generate ssl cert %s for %s at %s", gson.toJson(srcSecret), namespace, k8sApiUrl));
        return srcSecret;
    }

    @Test
    public void k8sTest()
    {
        try
        {
            String path = "/root/Platform/bin";
            String logPath = path.replaceAll("/[^/]+$", "/logs");
            System.out.println(logPath);
//            deployReplaceTest();
//            namespaceCreateTest();
//            createDeploymentTest();
//            createPVTest();
//            createPVCTest();
//            createSvcTest();
//            streamTest();
//            createEndpointsTest();
//            getTemplateJsonTest();
            deploySelectTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void deploySelectTest() throws Exception
    {
        Map<String, String> selector = new HashMap<>();
        selector.put("domain-id", "public01");
        getConnection(this.testK8sApiUrl, this.testAuthToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1DeploymentList deploymentList = apiInstance.listNamespacedDeployment(testPlatformId, null, null, null, "", "domain-id=cloud01,cmsserver=cms1", null, null, null, null);
        System.out.println(gson.toJson(deploymentList.getItems()));
    }

    private void deployReplaceTest() throws Exception
    {
        getConnection(this.testK8sApiUrl, this.testAuthToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Deployment deployment = gson.fromJson("{\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"202005-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"cas-manage01\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/202005-test/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-INF/;jar uf cas.war WEB-INF/cas.properties;wget http://10.130.41.218:8081/repository/tmp/configText/202005-test/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf cas.war WEB-INF/web.xml;mv /opt/cas.war /war\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"init-cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg\",\"name\":\"cas-manager01-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/202005-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"name\":\"cas-manager01-volume\",\"configMap\":{\"name\":\"cas-manage01\",\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}]}}]}}}}", V1Deployment.class);
        V1Deployment replace = apiInstance.replaceNamespacedDeployment(deployment.getMetadata().getName(), deployment.getMetadata().getNamespace(), deployment, null, null, null);
        System.out.println(gson.toJson(replace));
    }

    private void namespaceCreateTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"k8s-platform-test\"},\"spec\":{\"finalizers\":[\"kubernetes\"]}}";
        V1Namespace ns = gson.fromJson(jsonStr, V1Namespace.class);
        ns = this.createNamespace(ns, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(gson.toJson(ns));
    }

    private void createDeploymentTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{\"deployment.kubernetes.io/revision\":\"1\"},\"creationTimestamp\":{\"iMillis\":1595668576000,\"iChronology\":{\"iBase\":{\"iBase\":{\"iBase\":{\"iMinDaysInFirstWeek\":4}},\"iParam\":{\"iZone\":{\"iTransitions\":[-9223372036854775808,-2177481943000,-933667200000,-922093200000,-908870400000,-888829200000,-881049600000,-767869200000,-745833600000,-733827600000,-716889600000,-699613200000,-683884800000,-670669200000,-652348800000,-650019600000,515527200000,527014800000,545162400000,558464400000,577216800000,589914000000,608666400000,621968400000,640116000000,653418000000,671565600000,684867600000],\"iWallOffsets\":[29143000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000],\"iStandardOffsets\":[29143000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000],\"iNameKeys\":[\"LMT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\"],\"iID\":\"Asia/Shanghai\"},\"iID\":\"Asia/Shanghai\"}}}},\"generation\":1,\"labels\":{\"app-name\":\"mysql\",\"job-id\":\"2591b591aa\",\"name\":\"mysql\",\"type\":\"THREE_PART_APP\",\"version\":\"5.7.29\"},\"name\":\"mysql\",\"namespace\":\"test07\",\"resourceVersion\":\"37321266\",\"selfLink\":\"/apis/apps/v1/namespaces/test07/deployments/mysql\",\"uid\":\"b84a3766-84e3-4b15-b8db-bc3c9b77d982\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql\"}},\"strategy\":{\"rollingUpdate\":{\"maxSurge\":\"25%\",\"maxUnavailable\":\"25%\"},\"type\":\"RollingUpdate\"},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"terminationMessagePath\":\"/dev/termination-log\",\"terminationMessagePolicy\":\"File\",\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"dnsPolicy\":\"ClusterFirst\",\"restartPolicy\":\"Always\",\"schedulerName\":\"default-scheduler\",\"securityContext\":{},\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-test07\"}}]}}},\"status\":{\"conditions\":[{\"lastTransitionTime\":{\"iMillis\":1595668576000,\"iChronology\":{\"iBase\":{\"iBase\":{\"iBase\":{\"iMinDaysInFirstWeek\":4}},\"iParam\":{\"iZone\":{\"iTransitions\":[-9223372036854775808,-2177481943000,-933667200000,-922093200000,-908870400000,-888829200000,-881049600000,-767869200000,-745833600000,-733827600000,-716889600000,-699613200000,-683884800000,-670669200000,-652348800000,-650019600000,515527200000,527014800000,545162400000,558464400000,577216800000,589914000000,608666400000,621968400000,640116000000,653418000000,671565600000,684867600000],\"iWallOffsets\":[29143000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000],\"iStandardOffsets\":[29143000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000],\"iNameKeys\":[\"LMT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\"],\"iID\":\"Asia/Shanghai\"},\"iID\":\"Asia/Shanghai\"}}}},\"lastUpdateTime\":{\"iMillis\":1595668576000,\"iChronology\":{\"iBase\":{\"iBase\":{\"iBase\":{\"iMinDaysInFirstWeek\":4}},\"iParam\":{\"iZone\":{\"iTransitions\":[-9223372036854775808,-2177481943000,-933667200000,-922093200000,-908870400000,-888829200000,-881049600000,-767869200000,-745833600000,-733827600000,-716889600000,-699613200000,-683884800000,-670669200000,-652348800000,-650019600000,515527200000,527014800000,545162400000,558464400000,577216800000,589914000000,608666400000,621968400000,640116000000,653418000000,671565600000,684867600000],\"iWallOffsets\":[29143000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000],\"iStandardOffsets\":[29143000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000],\"iNameKeys\":[\"LMT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\"],\"iID\":\"Asia/Shanghai\"},\"iID\":\"Asia/Shanghai\"}}}},\"message\":\"Deployment does not have minimum availability.\",\"reason\":\"MinimumReplicasUnavailable\",\"status\":\"False\",\"type\":\"Available\"},{\"lastTransitionTime\":{\"iMillis\":1595668576000,\"iChronology\":{\"iBase\":{\"iBase\":{\"iBase\":{\"iMinDaysInFirstWeek\":4}},\"iParam\":{\"iZone\":{\"iTransitions\":[-9223372036854775808,-2177481943000,-933667200000,-922093200000,-908870400000,-888829200000,-881049600000,-767869200000,-745833600000,-733827600000,-716889600000,-699613200000,-683884800000,-670669200000,-652348800000,-650019600000,515527200000,527014800000,545162400000,558464400000,577216800000,589914000000,608666400000,621968400000,640116000000,653418000000,671565600000,684867600000],\"iWallOffsets\":[29143000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000],\"iStandardOffsets\":[29143000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000],\"iNameKeys\":[\"LMT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\"],\"iID\":\"Asia/Shanghai\"},\"iID\":\"Asia/Shanghai\"}}}},\"lastUpdateTime\":{\"iMillis\":1595668576000,\"iChronology\":{\"iBase\":{\"iBase\":{\"iBase\":{\"iMinDaysInFirstWeek\":4}},\"iParam\":{\"iZone\":{\"iTransitions\":[-9223372036854775808,-2177481943000,-933667200000,-922093200000,-908870400000,-888829200000,-881049600000,-767869200000,-745833600000,-733827600000,-716889600000,-699613200000,-683884800000,-670669200000,-652348800000,-650019600000,515527200000,527014800000,545162400000,558464400000,577216800000,589914000000,608666400000,621968400000,640116000000,653418000000,671565600000,684867600000],\"iWallOffsets\":[29143000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000,32400000,28800000],\"iStandardOffsets\":[29143000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000,28800000],\"iNameKeys\":[\"LMT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\",\"CDT\",\"CST\"],\"iID\":\"Asia/Shanghai\"},\"iID\":\"Asia/Shanghai\"}}}},\"message\":\"ReplicaSet \\\"mysql-5798ff855c\\\" is progressing.\",\"reason\":\"ReplicaSetUpdated\",\"status\":\"True\",\"type\":\"Progressing\"}],\"observedGeneration\":1,\"replicas\":1,\"unavailableReplicas\":1,\"updatedReplicas\":1}}";
        Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                .registerTypeAdapter(LocalTime.class, new LocalTimeSerializer()).create();
        V1Deployment deployment = gson.fromJson(jsonStr, V1Deployment.class);
        List<V1Deployment> deployments = new ArrayList<>();
        for(int i = 0; i < 20; i++)
            deployments.add(deployment);
        Map<String, List<V1ObjectMeta>> metaMap = deployments.stream().map(dep -> dep.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            System.out.println(String.format("%s has %d", name, metaMap.get(name).size()));
        }
        String platformId = "test91";
//        deployment = this.createNamespacedDeployment(platformId, deployment, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(gson.toJson(deployment));
    }

    private String streamTest() throws Exception
    {
        String command = "cp /cfg/dds-cfg/dds_logger.cfg /binary-file/cfg/dds_logger.cfg";
        String[] arr = command.split("\\s+");
        System.out.println(arr.length);
        String platformId = "just-test";
        List<V1Deployment> deployments = this.listNamespacedDeployment(platformId, this.testK8sApiUrl, this.testAuthToken);
        String allName = String.join(",", deployments.stream().collect(Collectors.toMap(V1Deployment::getMetadata, Function.identity())).keySet().stream().collect(Collectors.toMap(V1ObjectMeta::getName, Function.identity())).keySet());
        System.out.println(allName);
        return allName;
    }

    private void createPVTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}";
        V1PersistentVolume pv = gson.fromJson(jsonStr, V1PersistentVolume.class);
        pv = createPersistentVolume(pv, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(gson.toJson(pv));
    }

    private void createPVCTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}";
        V1PersistentVolumeClaim pvc = gson.fromJson(jsonStr, V1PersistentVolumeClaim.class);
        pvc = createNamespacedPersistentVolumeClaim("k8s-test", pvc, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(pvc);
    }

    private void createSvcTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}}";
        V1Service svc = gson.fromJson(jsonStr, V1Service.class);
        String platformId = "k8s-test";
        svc = createNamespacedService(platformId, svc, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(gson.toJson(svc));
    }

    void createEndpointsTest() throws Exception
    {
        String name = "umg41";
        String namespace = "clone-test";
        V1Endpoints endpoints = new V1Endpoints();
        endpoints.setMetadata(new V1ObjectMeta());
        endpoints.getMetadata().setName(name);
        endpoints.getMetadata().setNamespace(namespace);
        V1EndpointSubset subset = new V1EndpointSubset();
        List<V1EndpointAddress> addressList = new ArrayList<>();
        V1EndpointAddress address = new V1EndpointAddress();
        address.setIp("10.130.41.41");
        addressList.add(address);
        V1EndpointPort port = new V1EndpointPort();
        port.setPort(120000);
        port.setProtocol("TCP");
        List<V1EndpointPort> ports = new ArrayList<>();
        ports.add(port);
        subset.addAddressesItem(address);
        endpoints.setSubsets(new ArrayList<>());
        endpoints.getSubsets().add(subset);
        getConnection(this.testK8sApiUrl, this.testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Endpoints ret = apiInstance.createNamespacedEndpoints(namespace, endpoints, null, null, null);
        System.out.println(ret);
    }

    private void getTemplateJsonTest() throws Exception
    {
        V1Namespace ns = readNamespace(testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(ns));
        V1Secret ssl = readNamespacedSecret("ssl", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(ssl));
        V1PersistentVolume pv = readPersistentVolume("base-volume-test-by-wyf", testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(pv));
        V1PersistentVolumeClaim pvc = readNamespacedPersistentVolumeClaim("base-volume-test-by-wyf", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(pvc));
        V1Job job = readNamespacedJob("platform-base-init", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(job));
        V1Deployment deploy = readNamespacedDeployment("oracle", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(deploy));
        deploy = readNamespacedDeployment("mysql", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(deploy));
        deploy = readNamespacedDeployment("glsserver-public01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(deploy));
        deploy = readNamespacedDeployment("dcms-manage01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(deploy));
        deploy = readNamespacedDeployment("cas-manage01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(deploy));
        V1Service service = readNamespacedService("oracle", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(service));
        service = readNamespacedService("mysql", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(service));
        service = readNamespacedService("glsserver-public01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(service));
        service = readNamespacedService("dcms-manage01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(service));
        service = readNamespacedService("cas-manage01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(service));
        ExtensionsV1beta1Ingress ingress = readNamespacedIngress("dcms-manage01", testPlatformId, testK8sApiUrl, testAuthToken);
        logger.error(templateParseGson.toJson(ingress));
    }
}
