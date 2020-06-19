package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.utils.K8sUtils;
import io.kubernetes.client.custom.V1Patch;
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
import io.kubernetes.client.util.labels.LabelSelector;
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

    private final static Gson gson = new Gson();

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
        logger.debug(String.format("find deployment %s from %s", name, k8sApiUrl));
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

    @Autowired
    public V1PersistentVolume createPersistentVolume(V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("create PersistentVolume %s from %s", gson.toJson(persistentVolume), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume create = apiInstance.createPersistentVolume(persistentVolume, null, null, null);
        logger.info(String.format("create PersistentVolume %s SUCCESS from %s", gson.toJson(create), k8sApiUrl));
        return create;
    }

    @Autowired
    public V1PersistentVolume replacePersistentVolume(String name, V1PersistentVolume persistentVolume, String k8sApiUrl, String authToken) throws ApiException
    {
        logger.debug(String.format("replace PersistentVolume %s to %s from %s", name, gson.toJson(persistentVolume), k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume replace = apiInstance.replacePersistentVolume(name, persistentVolume, null, null, null);
        logger.info(String.format("%s PersistentVolume has been replaced by %s from %s", name, gson.toJson(replace), k8sApiUrl));
        return replace;
    }

    @Autowired
    public V1Status deletePersistentVolume(String name, String k8sApiUrl, String authToken) throws ApiException
    {
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
    public void cmTest()
    {
        try
        {
            createIngressTest();
//            createServiceTest();
//            createDeploymentTest();
//            podTest();
//            deploymentTest();
//            podTest();
//            pstClaimTest();
//            pstVolumesTest();
//            secretTest();
//            endpointTest();
//            ingressTest();
//            deploymentTest1();
//            nodeTest();
//            serviceTest();
//            jsonTest();
//            listAllConfigMap();
////            createConfigMapTest();
//            replaceConfigMapTest();
            createNamespaceTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void createIngressTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"annotations\":{\"kubectl.kubernetes.io/last-applied-configuration\":\"{\\\"apiVersion\\\":\\\"extensions/v1beta1\\\",\\\"kind\\\":\\\"Ingress\\\",\\\"metadata\\\":{\\\"annotations\\\":{},\\\"name\\\":\\\"cas-manage01\\\",\\\"namespace\\\":\\\"202005-test\\\"},\\\"spec\\\":{\\\"rules\\\":[{\\\"host\\\":\\\"202005-test.ccod.com\\\",\\\"http\\\":{\\\"paths\\\":[{\\\"backend\\\":{\\\"serviceName\\\":\\\"cas-manage01\\\",\\\"servicePort\\\":80},\\\"path\\\":\\\"/cas\\\"}]}}]}}\\n\"},\"name\":\"cas-manage01\",\"namespace\":\"202005-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cass\"}]}}]}}";
        ExtensionsV1beta1Ingress ingress = gson.fromJson(jsonStr, ExtensionsV1beta1Ingress.class);
        getConnection(testK8sApiUrl, testAuthToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        String namespace = "202005-test";
        ExtensionsV1beta1Ingress create = apiInstance.createNamespacedIngress(namespace, ingress, null, null, null);
        System.out.println(gson.toJson(create));

    }

    private void createNamespaceTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "123456-wuph";
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"just-test\"},\"spec\":{\"finalizers\":[\"kubernetes\"]}}";
        V1Namespace ns = gson.fromJson(jsonStr, V1Namespace.class);
        V1Namespace create = apiInstance.createNamespace(ns, null, null, null);
        System.out.println(gson.toJson(create));
    }

    protected void endpointTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "123456-wuph";
        V1EndpointsList list = apiInstance.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null);
        System.out.println(gson.toJson(list.getItems()));
    }

    private void createServiceTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"cms1-cloud01\"},\"name\":\"cms1-cloud01\"},\"spec\":{\"ports\":[{\"name\":\"cms1-cloud01-17119\",\"port\":17119,\"protocol\":\"TCP\",\"targetPort\":17119},{\"name\":\"cms1-cloud01-11520\",\"port\":11520,\"protocol\":\"TCP\",\"targetPort\":11520}],\"selector\":{\"name\":\"cms1-cloud01\"},\"type\":\"ClusterIP\"}}";
        V1Service service = gson.fromJson(jsonStr, V1Service.class);
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "202005-test";
        V1Service create = apiInstance.createNamespacedService(namespace, service, null, null, null);
        System.out.println(gson.toJson(create));
    }

    protected void ingressTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1IngressList list = apiInstance.listIngressForAllNamespaces(null, null, null ,null, null, null, null,null, null);
        Gson gson = new Gson();
        System.out.println(gson.toJson(list));


    }

    protected void listAllConfigMap() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "202005-test"; // String | object name and auth scope, such as for teams and projects
//        String namespace = "kube-system";
        V1ConfigMapList cmList = apiInstance.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null);
        List<V1ConfigMap> items = cmList.getItems();
        String json = JSONArray.toJSONString(items);
        System.out.println(String.format("%s has find %d configMap : %s", namespace, items.size(), json));
    }

    protected void createConfigMapTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "202005-test"; // String | object name and auth scope, such as for teams and projects
        V1ConfigMap body = new V1ConfigMap(); // V1ConfigMap |
        Map<String, String> data = new HashMap<>();
        data.put("special.how", "very");
        data.put("special.type", "charm");
        body.setData(data);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName("special-config");
        body.setMetadata(meta);
//        body.setApiVersion("v1");
//        body.setKind("ConfigMap");
//        String pretty = "true"; // String | If 'true', then the output is pretty printed.
//        String dryRun = "All"; // String | When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
        String fieldManager = "lhb"; // String | fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
        try {
            V1ConfigMap result = apiInstance.createNamespacedConfigMap(namespace, body, null, null, null);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CoreV1Api#createNamespacedConfigMap");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }

    protected void replaceConfigMapTest()
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "202005-test"; // String | object name and auth scope, such as for teams and projects
        V1ConfigMap body = new V1ConfigMap(); // V1ConfigMap |
        Map<String, String> data = new HashMap<>();
        data.put("special.how", "kick");
        data.put("special.type", "man");
        body.setData(data);
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.setName("special-config");
        body.setMetadata(meta);
        String pretty = "true"; // String | If 'true', then the output is pretty printed.
        String dryRun = "All"; // String | When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
        String fieldManager = "lhb"; // String |
        // is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint..
        try {
            V1ConfigMap result = apiInstance.replaceNamespacedConfigMap("special-config", namespace, body, null, null, null);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CoreV1Api#replaceNamespacedConfigMap");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }

    private void nodeTest() throws Exception
    {
        String namespace = "kube-system";
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NodeList nodeList = apiInstance.listNode(null, null, null, null ,null, null, null,null, null);
        String jsonStr = JSONArray.toJSONString(nodeList.getItems());
        System.out.println(jsonStr);
    }

    private void podTest() throws Exception
    {
        String namespace = "202005-test";
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PodList podList = apiInstance.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
//        int replicas = 9;
//        String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/spec/replicas\", \"value\": " + replicas + " }]";
//        JSONArray jsonArray = JSONArray.parseArray(jsonPatchStr);
//        for(int i = 0; i < jsonArray.size(); i++)
//        {
//            JSONObject object = jsonArray.getJSONObject(i);
//            System.out.println(JSONObject.toJSONString(object));
//        }
//        System.out.println(gson.toJson(podList.getItems()));
        for(V1Pod pod : podList.getItems())
        {
            String alias = pod.getMetadata().getName().split("\\-")[0];
            Map<String, String> labelMap = new HashMap<>();
            labelMap.put("name", "daengine-cloud01");
            labelMap.put("alias", alias);
            String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/metadata/labels\", \"value\": " + JSONObject.toJSONString(labelMap) + " }]";
//            String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/metadata/labels\", \"value\": " + "name=pid" + " }]";
            System.out.println(jsonPatchStr);
            V1Patch body = new V1Patch(jsonPatchStr);
            V1Pod newPod = apiInstance.patchNamespacedPod(pod.getMetadata().getName(), namespace, body, null, null, null, true);
            System.out.print(newPod);
        }
    }

    private void serviceTest() throws Exception
    {
        String namespace = "kube-system";
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ServiceList list = apiInstance.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
        for(int i = 0; i < list.getItems().size(); i++) {
            V1Service service = list.getItems().get(i);
            JSONObject jsonObject = K8sUtils.transferV1ServiceToJSONObject(service);
            String jsonTestStr = JSONObject.toJSONString(jsonObject);
            System.out.println(jsonTestStr);
            V1Service svc = K8sUtils.transferJsonObjectToV1Service(jsonObject);
            System.out.println(svc);
//            try {
//                JSONObject.toJSONString(service);
//            }
//            catch (Exception ex)
//            {
//                ex.printStackTrace();
//                try
//                {
//                    JSONObject.toJSONString(service.getMetadata());
//                }
//                catch (Exception ex1)
//                {
//                    ex1.printStackTrace();
//                }
//                try
//                {
//                    JSONObject.toJSONString(service.getSpec());
//                }
//                catch (Exception ex1)
//                {
//                    ex1.printStackTrace();
//                }
//                try
//                {
//                    JSONObject.toJSONString(service.getStatus());
//                }
//                catch (Exception ex1)
//                {
//                    ex1.printStackTrace();
//                }
//
//                V1ServiceSpec spc = service.getSpec();
//                try
//                {
//                    JSONObject.toJSONString(spc.getPorts());
//                }
//                catch (Exception ex1)
//                {
//                    ex1.printStackTrace();
//                }
//                try
//                {
//                    JSONObject.toJSONString(spc.getSessionAffinityConfig());
//                }
//                catch (Exception ex1)
//                {
//                    ex1.printStackTrace();
//                }
//                List<V1ServicePort> ports = spc.getPorts();
//                for(int j = 0; j < ports.size(); j++)
//                {
//                    V1ServicePort port = ports.get(j);
//                    try
//                    {
//                        String[] excludeProperties = {"strValue"};
//                        PropertyPreFilters filters = new PropertyPreFilters();
//                        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
//                        excludefilter.addExcludes(excludeProperties);
//                        IntOrString tp = port.getTargetPort();
//                        JSONObject.toJSONString(tp, excludefilter);
//                    }
//                    catch (Exception ex1)
//                    {
//                        ex1.printStackTrace();
//                    }
//
//                }
        }
    }

    private void createDeploymentTest() throws Exception
    {
        String jsonStr = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{\"deployment.kubernetes.io/revision\":\"1\"},\"labels\":{\"name\":\"cms1-cloud01\"},\"name\":\"cms1-cloud01\",\"namespace\":\"202005-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"cms1-cloud01\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"creationTimestamp\":null,\"labels\":{\"name\":\"cms1-cloud01\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/cloud01_cms1/beijing.xml -P ./etc -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg -P ./etc -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/cloud01_cms1/config.cms2 -P ./etc -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/ccod/mod_number.so -P /root/Platform/bin -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/ccod/hlr.gz -P /root/Platform/etc -N; cd /root/Platform/etc;tar -xzf hlr.gz; cd /root/Platform/bin;/root/Platform/bin/cmsserver --config.main=../etc/config.cms2;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17119},\"timeoutSeconds\":1},\"name\":\"cms1-cloud01\",\"ports\":[{\"containerPort\":17119,\"protocol\":\"TCP\"},{\"containerPort\":11520,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"terminationMessagePath\":\"/dev/termination-log\",\"terminationMessagePolicy\":\"File\",\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"dnsPolicy\":\"ClusterFirst\",\"initContainers\":[{\"args\":[\"cp /opt/cmsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/cmsserver:4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"init-cmsserver\",\"resources\":{},\"terminationMessagePath\":\"/dev/termination-log\",\"terminationMessagePolicy\":\"File\",\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"workingDir\":\"/opt\"}],\"restartPolicy\":\"Always\",\"schedulerName\":\"default-scheduler\",\"securityContext\":{},\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/202005-test/cms1-cloud01/cms1-cloud01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"}]}}}}";
        V1Deployment deployment = gson.fromJson(jsonStr, V1Deployment.class);
        String namespace = "202005-test";
        getConnection(testK8sApiUrl, testAuthToken);
        AppsV1Api apiInstance = new AppsV1Api();
        V1Deployment create = apiInstance.createNamespacedDeployment(namespace, deployment, null, null, null);
        System.out.println(gson.toJson(create));

    }

    private void deploymentTest() throws Exception
    {
        String namespace = "202005-test";
        getConnection(testK8sApiUrl, testAuthToken);
        AppsV1Api apiInstance = new AppsV1Api();
//        CoreV1Api apiInstance = new CoreV1Api();
        V1DeploymentList list = apiInstance.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);
        for(V1Deployment deployment : list.getItems())
        {
            if(!deployment.getMetadata().getName().equals("daengine-cloud01"))
                continue;
            String name = deployment.getMetadata().getName();
//            String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/spec/replicas\", \"value\": " + 3 + " }]";
            Map<String, String> labelMap = new HashMap<>();
            labelMap.put("name", "daengine-cloud01");
            labelMap.put("alias", "daengine");
            String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/spec/template/metadata/labels\", \"value\": " + JSONObject.toJSONString(labelMap) + " }]";
//            String jsonPatchStr = "[{\"op\":\"replace\",\"path\":\"/metadata/labels\", \"value\": " + "name=just-test" + " }]";
            V1Patch body = new V1Patch(jsonPatchStr);
            try {
                V1Deployment result1 = apiInstance.patchNamespacedDeployment(name, namespace, body, null, null, null, null);
                System.out.println(result1);
            } catch (ApiException e) {
                e.printStackTrace();
                logger.error("k8s副本更新失败！");
            }
        }
    }

    private void deploymentTest1() throws Exception
    {
        Gson gson = new Gson();
        String namespace = "123456-wuph";
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ServiceList svcList = apiInstance.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
        System.out.println(gson.toJson(svcList.getItems()));
        AppsV1Api appsV1Api = new AppsV1Api();
        V1DeploymentList list = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);

        String someStr = gson.toJson(list.getItems());
        JSONArray array = JSONArray.parseArray(someStr);
        System.out.println(array.size());
        for(int i = 0; i < list.getItems().size(); i++)
        {
            V1Deployment deployment = list.getItems().get(i);
            String nextStr = gson.toJson(deployment);
            System.out.println(nextStr);
//            JSONObject jsonObject = K8sUtils.transferV1DeploymentToJSONObject(deployment);
//            System.out.println(JSONObject.toJSONString(jsonObject));
//            System.out.println(String.format("\n%d@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@%d", i, i));
        }
    }

    private void jsonTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        //打印所有的pod
        V1PodList list = apiInstance.listPodForAllNamespaces(null,null,null,null,null,null,null,
                null,null);

        for (int i = 0; i < list.getItems().size(); i++) {
            V1Pod item = list.getItems().get(i);
            String[] excludeProperties = {"tcpSocket", "httpGet"};
            PropertyPreFilters filters = new PropertyPreFilters();
            PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
            excludefilter.addExcludes(excludeProperties);
            String testStr = JSONObject.toJSONString(item, excludefilter);
            System.out.println(testStr);
//            for(V1Container container : item.getSpec().getContainers()) {
//
////                container.setEnvFrom(null);
//                V1Probe probe = container.getReadinessProbe();
//
//                if(probe != null)
//                {
////                    probe.setHttpGet(null);
//                    probe.setTcpSocket(null);
////                    probe.setExec(null);
//                }
//                probe = container.getLivenessProbe();
//                if(probe != null)
//                {
////                    probe.setHttpGet(null);
//                    probe.setTcpSocket(null);
////                    probe.setExec(null);
//                }
////                container.setReadinessProbe(null);
////                container.setLifecycle(null);
////                container.setVolumeDevices(null);
////                container.setSecurityContext(null);
////                container.setLivenessProbe(null);
//            }
            try
            {
//                String str = JSONObject.toJSONString(item, SerializerFeature.WriteDateUseDateFormat);
//                System.out.println(str);
            }
            catch (Exception ex)
            {
                V1PodSpec spec = item.getSpec();
                List<V1Container> containers = spec.getContainers();
                for(V1Container container :  containers)
                {
                    container.setLivenessProbe(null);
                    JSONObject.toJSONString(container);
                }
//                JSONArray.toJSONString(spec.getContainers());
                JSONArray.toJSONString(spec.getInitContainers());
                JSONObject.toJSONString(spec.getSecurityContext());
                try
                {
                    JSONObject.toJSONString(item.getSpec());
                }
                catch (Exception ex1)
                {
                    ex1.printStackTrace();
                }
                try
                {
                    JSONObject.toJSONString(item.getStatus());
                }
                catch (Exception ex1)
                {
                    ex1.printStackTrace();
                }
                try
                {
                    JSONObject.toJSONString(item.getMetadata());
                }
                catch (Exception ex1)
                {
                    ex1.printStackTrace();
                }
            }
//            System.out.println(item);
        }
    }

    private void secretTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        Gson gson = new Gson();
        String namespace = "123456-wuph";
        V1SecretList secretList = apiInstance.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null);
        System.out.println(gson.toJson(secretList.getItems()));
    }

    private void pstClaimTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        Gson gson = new Gson();
        String namespace = "123456-wuph";
        V1PersistentVolumeClaimList list = apiInstance.listNamespacedPersistentVolumeClaim(namespace, null, null, null, null ,null, null, null, null, null);
        System.out.println(gson.toJson(list.getItems()));
    }

    private void pstVolumesTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        Gson gson = new Gson();
        String namespace = "123456-wuph";
        V1PersistentVolumeList list = apiInstance.listPersistentVolume(null, null, null, null, null, null, null, null, null);
        System.out.println(gson.toJson(list.getItems()));
        V1PersistentVolume volume = apiInstance.readPersistentVolume("base-volume-123456-wuph", null, null, null);
        System.out.println(gson.toJson(volume));
    }
}
