package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.utils.K8sUtils;
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
    public V1Namespace queryNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Namespace ns = apiInstance.readNamespace(namespace, null, null, null);
        logger.debug(String.format("find namespace %s from %s", JSONObject.toJSONString(ns), k8sApiUrl));
        return ns;
    }

    @Override
    public List<V1Namespace> queryAllNamespace(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list all namespace from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NamespaceList list = apiInstance.listNamespace(null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d namespace %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Pod queryPod(String namespace, String podName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get pod %s from %s with namespace %s", podName, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Pod pod = apiInstance.readNamespacedPod(podName, namespace, null, null, null);
        logger.debug(String.format("find pod %s from %s", gson.toJson(pod), k8sApiUrl));
        return pod;
    }

    @Override
    public List<V1Pod> queryAllPodAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all pods at namespace %s from %s", namespace, k8sApiUrl));
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
    public V1Service queryService(String namespace, String serviceName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get service %s from %s with namespace %s", serviceName, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Service service = apiInstance.readNamespacedService(serviceName, namespace, null, null, null);
        logger.debug(String.format("find service %s from %s success", serviceName, k8sApiUrl));
        return service;
    }

    @Override
    public List<V1Service> queryAllServiceAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all service at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ServiceList list = apiInstance.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d service at %s from %s", list.getItems().size(), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public List<V1Node> queryAllNode(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all node from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NodeList list = apiInstance.listNode(null, null, null, null ,null, null, null,null, null);
        logger.debug(String.format("find %d node %s from %s", list.getItems().size(), gson.toJson(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Node queryNode(String nodeName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get node %s from %s", nodeName, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Node node = apiInstance.readNode(nodeName, null, null, null);
        logger.debug(String.format("find node %s from %s", gson.toJson(node), k8sApiUrl));
        return node;
    }

    @Override
    public List<V1ConfigMap> queryAllConfigMapAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all configMap at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMapList list = apiInstance.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d configMap %s at %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1ConfigMap queryConfigMap(String namespace, String configMapName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get configMap %s from %s at namespace %s", configMapName, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ConfigMap configMap = apiInstance.readNamespacedConfigMap(configMapName, namespace, null, null, null);
        logger.debug(String.format("find configMap %s from %s", JSONObject.toJSONString(configMap), k8sApiUrl));
        return configMap;
    }

    @Override
    public List<V1Deployment> queryAllDeploymentAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get all deployment from %s at namespace %s", k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api appsV1Api = new AppsV1Api();
        V1DeploymentList list = appsV1Api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d deployment from %s at %s", list.getItems().size(), k8sApiUrl, namespace));
        return list.getItems();
    }

    @Override
    public V1Deployment queryDeployment(String namespace, String deploymentName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get deployment %s from %s at namespace %s", deploymentName, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        AppsV1Api appsV1Api = new AppsV1Api();
        V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace, null, null, null);
        logger.debug(String.format("find deployment %s from %s", deploymentName, k8sApiUrl));
        return deployment;
    }

    @Override
    public List<ExtensionsV1beta1Ingress> queryAllIngressAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all ingress from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1IngressList list = apiInstance.listNamespacedIngress(namespace,null, null, null ,null, null, null, null,null, null);
        logger.debug(String.format("find %d ingress %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public ExtensionsV1beta1Ingress queryIngress(String namespace, String ingressName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query ingress at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api apiInstance = new ExtensionsV1beta1Api();
        ExtensionsV1beta1Ingress ingress = apiInstance.readNamespacedIngress(ingressName, namespace, null, null, null);
        logger.debug(String.format("find ingress %s at %s from %s", gson.toJson(ingress), namespace, k8sApiUrl));
        return ingress;
    }

    @Override
    public List<V1Endpoints> queryAllEndpointsAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all endpoints at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1EndpointsList list = apiInstance.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d endpoints %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Endpoints queryEndpoints(String namespace, String endpointsName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query endpoints %s at %s from %s", endpointsName, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Endpoints endpoints = apiInstance.readNamespacedEndpoints(endpointsName, namespace, null, null, null);
        logger.debug(String.format("find endpoints %s at %s from %s", gson.toJson(endpoints), namespace, k8sApiUrl));
        return endpoints;
    }

    @Override
    public List<V1Secret> queryAllSecretAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all secret at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1SecretList list = apiInstance.listNamespacedSecret(namespace, null ,null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d secret %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Secret querySecret(String namespace, String secretName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query secret $s at %s from %s", secretName, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Secret secret = apiInstance.readNamespacedSecret(secretName, namespace, null, null, null);
        logger.debug(String.format("find secret %s at %s from %s", gson.toJson(secret), namespace, k8sApiUrl));
        return secret;
    }

    @Override
    public List<V1PersistentVolumeClaim> queryAllPersistentVolumeClaimAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all persistentVolumeClaim at %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaimList list = apiInstance.listNamespacedPersistentVolumeClaim(namespace, null ,null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d persistentVolumeClaim %s at %s from %s", list.getItems().size(), gson.toJson(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1PersistentVolumeClaim queryPersistentVolumeClaim(String namespace, String persistentVolumeClaimName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query persistentVolumeClaim $s at %s from %s", persistentVolumeClaimName, namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeClaim claim = apiInstance.readNamespacedPersistentVolumeClaim(persistentVolumeClaimName, namespace, null, null, null);
        logger.debug(String.format("find persistentVolumeClaim %s at %s from %s", gson.toJson(claim), namespace, k8sApiUrl));
        return claim;
    }

    @Override
    public List<V1PersistentVolume> queryAllPersistentVolume(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all PersistentVolume from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolumeList list = apiInstance.listPersistentVolume(null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d PersistentVolume %s from %s", list.getItems().size(), gson.toJson(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1PersistentVolume queryPersistentVolume(String persistentVolumeName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query PersistentVolume %s from %s", persistentVolumeName, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PersistentVolume volume = apiInstance.readPersistentVolume(persistentVolumeName, null, null, null);
        logger.debug(String.format("find PersistentVolume %s from %s", gson.toJson(volume), k8sApiUrl));
        return volume;
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

    @Test
    public void cmTest()
    {
        try
        {
//            pstClaimTest();
            pstVolumesTest();
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
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    protected void endpointTest() throws Exception
    {
        getConnection(testK8sApiUrl, testAuthToken);
        CoreV1Api apiInstance = new CoreV1Api();
        String namespace = "123456-wuph";
        V1EndpointsList list = apiInstance.listNamespacedEndpoints(namespace, null, null, null, null, null, null, null, null, null);
        System.out.println(gson.toJson(list.getItems()));
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

    private void deploymentTest() throws Exception
    {
        String namespace = "pahj";
        ApiClient client = getConnection(testK8sApiUrl, testAuthToken);
        ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
        ExtensionsV1beta1DeploymentList list = api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);
        String json = JSONArray.toJSONString(list.getItems());
        System.out.println(json);
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
