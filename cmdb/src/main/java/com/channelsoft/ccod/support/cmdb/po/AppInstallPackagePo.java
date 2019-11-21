package com.channelsoft.ccod.support.cmdb.po;

import java.util.Date;

/**
 * @ClassName: AppInstallPackagePo
 * @Author: lanhb
 * @Description: 用来定义应用安装包的类
 * @Date: 2019/11/21 13:31
 * @Version: 1.0
 */
public class AppInstallPackagePo {
    private int packageId; //发布包id,数据库唯一生成主键

    private int appId; //该发布包对应的是哪个应用的id,外键app表的appId

    private String fileName; //安装包文件名

    private String fileType; //安装包类型，例如zip,tar,war,binary等,由FileType枚举预定义

    private String deployPath; //文件存放路径,可以是相对app的base path的相对路径,

    private String nexusRepository; //保存在nexus的仓库名

    private String nexusAssetId; //在nexus中的assetId

    private String nexusDownloadUrl; //在nexus中的download url

    private Date createTime; //该文件在nexus的创建时间

    private String md5; //该安装包的md5特征值
}
