package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import com.alibaba.fastjson.support.spring.annotation.FastJsonFilter;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        logger.debug(String.format("find pod %s from %s", JSONObject.toJSONString(pod), k8sApiUrl));
        String[] excludeProperties = {"tcpSocket", "httpGet"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        String jsnStr = JSONObject.toJSONString(pod, excludefilter);
        return JSONObject.parseObject(jsnStr, V1Pod.class);
    }

    @Override
    public List<V1Pod> queryAllPodAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all pods at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PodList list = apiInstance.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
        List<V1Pod> pods = list.getItems();
        String[] excludeProperties = {"tcpSocket", "httpGet"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        String jsnStr = JSONArray.toJSONString(pods, excludefilter);

//        logger.debug(String.format(jsonStr));
        logger.debug(String.format("find %d pods %s at namespace %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), namespace, k8sApiUrl));
        return JSONArray.parseArray(jsnStr, V1Pod.class);
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
        logger.debug(String.format("find service %s from %s", JSONObject.toJSONString(service), k8sApiUrl));
        return service;
    }

    @Override
    public List<V1Service> queryAllServiceAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all service at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1ServiceList list = apiInstance.listNamespacedService(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d service %s at %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), namespace, k8sApiUrl));
        return list.getItems();
    }

    @Override
    public List<V1Node> queryAllNode(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all node from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NodeList list = apiInstance.listNode(null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d node %s from %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), k8sApiUrl));
        return list.getItems();
    }

    @Override
    public V1Node queryNode(String nodeName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get node %s from %s", nodeName, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Node node = apiInstance.readNode(nodeName, null, null, null);
        logger.debug(String.format("find node %s from %s", JSONObject.toJSONString(node), k8sApiUrl));
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
    public List<ExtensionsV1beta1Deployment> queryAllDeploymentAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get all deployment from %s at namespace %s", k8sApiUrl, namespace));
        ApiClient client = getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
        ExtensionsV1beta1DeploymentList list = api.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d deployment %s from %s at %s", list.getItems().size(), JSONArray.toJSONString(list.getItems()), k8sApiUrl, namespace));
        return list.getItems();
    }

    @Override
    public ExtensionsV1beta1Deployment queryDeployment(String namespace, String deploymentName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get deployment %s from %s at namespace %s", deploymentName, k8sApiUrl, namespace));
        ApiClient client = getConnection(k8sApiUrl, authToken);
        ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
        ExtensionsV1beta1Deployment deployment = api.readNamespacedDeployment(deploymentName, namespace, null, null, null);
        logger.debug(String.format("find deployment %s from %s", JSONObject.toJSONString(deployment), k8sApiUrl));
        return deployment;
    }

    @Test
    public void cmTest()
    {
        try
        {
            jsonTest();
//            listAllConfigMap();
////            createConfigMapTest();
//            replaceConfigMapTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
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

}
