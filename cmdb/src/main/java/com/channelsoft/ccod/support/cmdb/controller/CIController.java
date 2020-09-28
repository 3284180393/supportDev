package com.channelsoft.ccod.support.cmdb.controller;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.ci.service.ICIService;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: CIController
 * @Author: lanhb
 * @Description: 用来定义ci相关http接口
 * @Date: 2020/9/27 20:59
 * @Version: 1.0
 */
@RestController
@RequestMapping("/cmdb/api/ci")
public class CIController {

    @Autowired
    ICIService ciService;

    @RequestMapping(value = "/builds/{jobName}", method = RequestMethod.GET)
    public AjaxResultPo queryBuild(@PathVariable String jobName)
    {
        AjaxResultPo resultPo;
        try {
            BuildDetailPo detail = ciService.getJobBuildResult(jobName);
            resultPo = new AjaxResultPo(true, "query build result success", 1, detail);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/builds/{jobName}", method = RequestMethod.POST)
    public AjaxResultPo createBuild(@PathVariable String jobName)
    {
        AjaxResultPo resultPo;
        try {
            ciService.createBuild(jobName);
            resultPo = new AjaxResultPo(true, "build create success");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }
}
