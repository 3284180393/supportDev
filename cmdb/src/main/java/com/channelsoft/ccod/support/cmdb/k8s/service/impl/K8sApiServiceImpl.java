package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;

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

    @Autowired
    INexusService nexusService;

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

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
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap configMap = apiInstance.createNamespacedConfigMap(namespace, body, null, null,null);
        return configMap;
    }

    @Override
    public void deleteConfigMapByName(String namespace, String configMapName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("delete configMap %s at %s from %s", configMapName, namespace,k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        apiInstance.deleteNamespacedConfigMap(configMapName, namespace, null, null, null, null, null, null);
        logger.debug(String.format("delete success"));
    }

    @Override
    public V1Deployment createNamespacedDeployment(String namespace, V1Deployment deployment, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create deployment %s for %s at %s", gson.toJson(deployment), namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Deployment create = apiInstance.createNamespacedDeployment(namespace, deployment, null, null, null);
        logger.debug(String.format("deployment %s created for %s at %s", gson.toJson(create), namespace, k8sApiUrl));
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

    @Test
    public void k8sTest()
    {
        try
        {
//            deployReplaceTest();
//            namespaceCreateTest();
            createDeploymentTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
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
        String jsonStr = "{\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"k8s-platform-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"cas-manage01\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/202005-test/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-INF/;jar uf cas.war WEB-INF/cas.properties;wget http://10.130.41.218:8081/repository/tmp/configText/202005-test/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf cas.war WEB-INF/web.xml;mv /opt/cas.war /war;cd /opt;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//cas.properties;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/home/portal/tomcat/webapps/cas/WEB-INF/\",\"name\":\"cas-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"202005-test-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/202005-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas-manage01\"},\"name\":\"cas-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"202005-test\"},\"name\":\"202005-test-volume\"}]}}}}";
        V1Deployment deployment = gson.fromJson(jsonStr, V1Deployment.class);
        String platformId = "k8s-platform-test";
        deployment = this.createNamespacedDeployment(platformId, deployment, this.testK8sApiUrl, this.testAuthToken);
        System.out.println(gson.toJson(deployment));
    }

}
