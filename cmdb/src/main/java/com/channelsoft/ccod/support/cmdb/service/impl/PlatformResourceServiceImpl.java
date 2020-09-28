package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.dao.PlatformResourceMapper;
import com.channelsoft.ccod.support.cmdb.service.IPlatformResourceService;
import com.channelsoft.ccod.support.cmdb.vo.PlatformResourceVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.Gson;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUpload;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.util.List;

/**
 * @ClassName: PlatformResourceServiceImpl
 * @Author: lanhb
 * @Description: 平台资源服务接口的实现类
 * @Date: 2019/11/27 15:50
 * @Version: 1.0
 */
@Service
public class PlatformResourceServiceImpl implements IPlatformResourceService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformResourceServiceImpl.class);

    private final static Gson gson = new Gson();

    @Autowired
    PlatformResourceMapper platformResourceMapper;

    @Override
    public PlatformResourceVo[] queryPlatformResources() throws Exception {
        logger.debug("begin to query resource of all platforms");
        List<PlatformResourceVo> list = this.platformResourceMapper.select();
        logger.info(String.format("query %d platforms resource", list.size()));
        return list.toArray(new PlatformResourceVo[0]);
    }

    @Override
    public PlatformResourceVo queryPlatformResource(String platformId, String domainId, String hostIp) throws Exception {
        logger.debug(String.format("begin to query platform resource : platformId=%s, domainId=%s, hostIp=%s",
                platformId, domainId, hostIp));
        PlatformResourceVo resourceVo = this.platformResourceMapper.selectByPlatform(platformId, domainId, hostIp);
        if(resourceVo == null)
        {
            logger.warn(String.format("not find platform resource with platformId=%s and domainId=%s and hostIp=%s",
                    platformId, domainId, hostIp));
        }
        else
        {
            logger.info(String.format("platformId=%s and domainId=%s and hostIp=%s platform resource been found",
                    platformId, domainId, hostIp));
        }
        return resourceVo;
    }

    private void someTest()  throws Exception
    {
        String json = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"labels\":{\"app-name\":\"mysql\",\"job-id\":\"73a8e02621\",\"name\":\"mysql\",\"type\":\"THREE_PART_APP\",\"version\":\"5.7.29\"},\"name\":\"mysql\",\"namespace\":\"test-by-wyf\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin=mysql_native_password\",\"--character-set-server=utf8mb4\",\"--collation-server=utf8mb4_unicode_ci\",\"--lower-case-table-names=1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-test-by-wyf\"}}]}}}}";
        JsonNode jsonNodeTree = new ObjectMapper().readTree(json);
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        System.out.println(jsonAsYaml);
        GitlabAPI api = GitlabAPI.connect("http://10.130.24.101/", "xHYTEP_Ms5MNaD_i59bz");
        GitlabProject project = api.getProject("supportDev", "ccod-app-shop");
        File file = new File("d:/tmp/schema.json");
        GitlabUpload upload = api.uploadFile(project, file);
//        api.createProject("ccod-app-shop");
        System.out.println("haha");
    }

    @Test
    public void doTest()
    {
        try
        {
            someTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
