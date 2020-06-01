package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public V1Namespace queryNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Namespace ns = apiInstance.readNamespace(namespace, null, null, null);
        logger.debug(String.format("find namespace %s[%s] from %s", namespace, JSONObject.toJSONString(ns), k8sApiUrl));
        return ns;
    }

    @Override
    public List<V1Namespace> queryAllNamespace(String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("list all namespace from %s", k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1NamespaceList namespaceList = apiInstance.listNamespace(null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d namespace from %s", namespaceList.getItems().size(), k8sApiUrl));
        return namespaceList.getItems();
    }

    @Override
    public V1Pod queryPod(String namespace, String podName, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to get pod %s from %s with namespace %s", podName, k8sApiUrl, namespace));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1Pod pod = apiInstance.readNamespacedPod(podName, namespace, null, null, null);
        logger.debug(String.format("find pod %s at namespace %s from %s", podName, namespace, k8sApiUrl));
        return pod;
    }

    @Override
    public List<V1Pod> queryAllPodAtNamespace(String namespace, String k8sApiUrl, String authToken) throws ApiException {
        logger.debug(String.format("begin to query all pods at namespace %s from %s", namespace, k8sApiUrl));
        getConnection(k8sApiUrl, authToken);
        CoreV1Api apiInstance = new CoreV1Api();
        V1PodList podList = apiInstance.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
        logger.debug(String.format("find %d pods with namespace %s from %s", podList.getItems().size(), namespace, k8sApiUrl));
        return podList.getItems();
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
}
