DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
  `app_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '应用id,主键数据库自动生成',
  `app_name` varchar(20) NOT NULL COMMENT '应用名',
  `app_alias` varchar(20) NOT NULL COMMENT '应用别名',
  `app_type` varchar(20) NOT NULL COMMENT '应用类型',
  `version` varchar(40) NOT NULL COMMENT '版本',
  `ccod_version` varchar(40) NOT NULL COMMENT '归属于哪个大版本的ccod',
  `base_path` varchar(100) NOT NULL COMMENT '应用的base路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_reason` varchar(255) DEFAULT NULL COMMENT '创建原因',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  `version_control` varchar(20) DEFAULT 'git' COMMENT '版本控制方式',
  `version_control_url` varchar(255) DEFAULT NULL COMMENT '同版本控制相关的url',
  PRIMARY KEY (`app_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `app_cfg_file`;
CREATE TABLE `app_cfg_file` (
  `cfg_file_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '应用配置文件id,主键数据库自动生成',
  `app_id` int(11) NOT NULL COMMENT '应用id,外键app表主键',
  `file_name` varchar(40) NOT NULL COMMENT '文件名',
  `ext` varchar(10) NOT NULL COMMENT '文件类型,例如binary,zip,war,ini,yml等',
  `deploy_path` varchar(128) NOT NULL COMMENT '文件存放路径,可以是相对app的base path的相对路径也可以是绝对路径',
  `nexus_repository` varchar(20) NOT NULL COMMENT '保存在nexus的仓库名',
  `nexus_directory` varchar(255) NOT NULL COMMENT '在nexus的保存路径',
  `nexus_asset_id` varchar(64) NOT NULL COMMENT '保存在nexus的asset id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `md5` varchar(40) NOT NULL COMMENT '该配置文件的md5',
  PRIMARY KEY (`cfg_file_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `app_install_package`;
CREATE TABLE `app_install_package` (
  `package_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '应用配置文件id,主键数据库自动生成',
  `app_id` int(11) NOT NULL COMMENT '应用id,外键app表主键',
  `file_name` varchar(40) NOT NULL COMMENT '安装包文件名',
  `ext` varchar(10) NOT NULL COMMENT '文件类型,例如binary,zip,war,ini,yml等',
  `deploy_path` varchar(128) NOT NULL COMMENT '安装包部署路径,可以是相对app的base path的相对路径也可以是绝对路径',
  `nexus_repository` varchar(20) NOT NULL COMMENT '保存在nexus的仓库名',
  `nexus_directory` varchar(255) NOT NULL COMMENT '在nexus的保存路径',
  `nexus_asset_id` varchar(64) NOT NULL COMMENT '保存在nexus的asset id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `md5` varchar(40) NOT NULL COMMENT '该安装包文件的md5',
  PRIMARY KEY (`package_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `platform`;
CREATE TABLE `platform` (
  `platform_id` varchar(40) NOT NULL COMMENT '平台id',
  `platform_name` varchar(40) NOT NULL COMMENT '平台名',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` tinyint(2) default 1 COMMENT '平台当前状态1、运行中,2、已撤销,3、停运中',
  `ccod_version` varchar(40) NOT NULL COMMENT '该平台采用的ccod版本',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`platform_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `domain`;
CREATE TABLE `domain` (
  `domain_id` varchar(40) NOT NULL COMMENT '域id',
  `domain_name` varchar(40) NOT NULL COMMENT '域名',
  `platform_id` varchar(40) NOT NULL COMMENT '平台id,外键platform表主键',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `status` tinyint(2) not null default 1 COMMENT '平台当前状态1、运行中,2、已撤销,3、停运中',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`domain_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `server`;
CREATE TABLE `server` (
	`server_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '服务器id,主键数据库自动生成',
  `host_name` varchar(40) NOT NULL COMMENT '主机名',
  `host_ip` varchar(40) NOT NULL COMMENT '主机ip',
  `server_type` tinyint(2) not null default 1 COMMENT '服务器类型,1、linux服务器,2、windows服务器',
  `platform_id` varchar(40) NOT NULL COMMENT '平台id,外键platform表主键',
  `domain_id` varchar(40) NOT NULL COMMENT '域id,外键domain表主键',
  `status` tinyint(2) not null default 1 COMMENT '平台当前状态1、运行中,2、已撤销,3、停运中',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`server_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `server_user`;
CREATE TABLE `server_user` (
    `user_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '服务器用户id,主键数据库自动生成',
  `user_name` varchar(40) NOT NULL COMMENT '用户名',
  `password` varchar(40) NOT NULL COMMENT '登录密码',
  `server_id` int(11) NOT NULL COMMENT '服务器id,外键server表主键',
  `login_method` tinyint(2) not null default 1 COMMENT '登录方式,1、ssh',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  `ssh_port` int(11) COMMENT '如果登录方式为ssh对应的ssh端口',
  PRIMARY KEY (`user_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `platform_app`;
CREATE TABLE `platform_app` (
	`platform_app_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '平台部署应用id,主键数据库自动生成',
	`app_id` int(11) NOT NULL COMMENT '应用id,外键app表主键',
	`platform_id` varchar(40) NOT NULL COMMENT '平台id,外键platform表主键',
  `domain_id` varchar(40) NOT NULL COMMENT '域id,外键domain表主键',
  `server_id` int(11) NOT NULL COMMENT '服务器id,外键server表主键',
  `runner_id` int(11) NOT NULL COMMENT '运行用户id,外键server_user表主键',
  `base_path` varchar(255) NOT NULL COMMENT '应用根路径',
  `deploy_time` datetime NOT NULL COMMENT '部署时间',
  PRIMARY KEY (`platform_app_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `platform_app_cfg_file`;
CREATE TABLE `platform_app_cfg_file` (
`cfg_file_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '平台应用配置文件id,主键数据库自动生成',
	`platform_app_id` int(11) NOT NULL COMMENT '平台部署应用id,外键platform_app表主键',
	  `file_name` varchar(40) NOT NULL COMMENT '文件名',
  `ext` varchar(10) NOT NULL COMMENT '文件类型,例如binary,zip,war,ini,yml等',
  `deploy_path` varchar(128) NOT NULL COMMENT '安装包部署路径,可以是相对app的base path的相对路径也可以是绝对路径',
  `nexus_repository` varchar(20) NOT NULL COMMENT '保存在nexus的仓库名',
  `nexus_directory` varchar(255) NOT NULL COMMENT '在nexus的保存路径',
  `nexus_asset_id` varchar(64) NOT NULL COMMENT '保存在nexus的asset id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `md5` varchar(40) NOT NULL COMMENT '该配置文件的md5',
  PRIMARY KEY (`cfg_file_id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `platform_app_attribute`;
CREATE TABLE `platform_app_attribute` (
`id` int(11) NOT NULL AUTO_INCREMENT COMMENT '平台应用属性id,主键数据库自动生成',
	`platform_app_id` int(11) NOT NULL COMMENT '平台部署应用id,外键platform_app表主键',
	  `key` varchar(40) NOT NULL COMMENT '属性key',
  `value` varchar(128) NOT NULL COMMENT '属性key对应的值',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

