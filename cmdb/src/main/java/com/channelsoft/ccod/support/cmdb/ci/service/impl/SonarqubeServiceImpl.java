package com.channelsoft.ccod.support.cmdb.ci.service.impl;

import com.channelsoft.ccod.support.cmdb.ci.service.ISonarqubeService;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import org.junit.Test;
import org.springframework.stereotype.Service;

/**
 * @ClassName: SonarqubeServiceImpl
 * @Author: lanhb
 * @Description: sonarqube接口实现类
 * @Date: 2020/9/27 20:11
 * @Version: 1.0
 */
@Service
public class SonarqubeServiceImpl implements ISonarqubeService {

    private String userName = "admin";

    private String password = "$G1mRhN{j.qBkH:j";

    private String hostUrl = "http://sonarqube.ci.com/";

    @Override
    public String getCheckResult(String jobName) throws InterfaceCallException {
        String url = String.format("%s/api/qualitygates/project_status?projectKey=%s", hostUrl, jobName);
        String conResult = HttpRequestTools.httpGetRequest(url, userName, password);
        return conResult;
    }

    private void someTest() throws Exception
    {
        String checkResult = getCheckResult("cmsserver");
        System.out.println(checkResult);
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
