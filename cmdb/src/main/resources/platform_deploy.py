# -*- coding: utf-8 -*-

import re
import hashlib
import logging
import urllib2
import datetime
import time
import subprocess
import os
import json
import requests
from requests.auth import HTTPBasicAuth


import sys
reload(sys)
sys.setdefaultencoding("utf-8")

LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logging.basicConfig(filename='my.log', level=logging.DEBUG, format=LOG_FORMAT)
request_header = {"Authorization": "Basic YWRtaW46MTIzNDU2"}
app_properties_file_name = "appProp.properties"
ccod_version = "ccod4.4"
app_repository = 'ccod_modules'
image_repository = 'ccod'
nexus_host_url = 'http://10.130.41.216:8081'
nexus_user = 'admin'
nexus_user_pwd = '123456'
cfg_repository = 'tmp'
docker_image_repository_uri = 'nexus.io:5000'
nexus_download_url = '%s/%s/%s/%s'
upload_url = "http://10.130.41.216:8081/service/rest/v1/components?repository=%s" % app_repository
app_register_url = "http://10.130.76.78:8086/cmdb/api/apps"
schema_update_url = 'http://10.130.76.78:8086/cmdb/api/platformUpdateSchema'
cmdb_host_url = 'http://10.130.41.63:8086'
k8s_deploy_git_url = 'http://10.130.24.101/wuph/payaml.git'
# gcc_depend_lib_path = '/root/2020_tool/lib'
# ccod_apps = """DialEngine##DialEngine##20a0ff3d24ae5d2c45523ab5c7e0da7b86db4c18##DialEngine##binary"""
# ccod_apps = """AppGateWay##AppGateWay##3b651073c03e1e3fedf73f25a1565c602b8e4040##AppGateWay##binary
# UCGateway##UCGateway##2df8a399b4c50cc9602c11c9cbfae23d07f134dc##UCGateway##binary"""
# ccod_apps = """glsServer##glsServer##7b699a4aece10ef28dce83ab36e4d79213ec4f69##Glsserver##binary
# RMServer##rmserver##26461:26692##RMServer##binary"""
# ccod_apps = """cas##cas##10973##cas.war##war"""
ccod_apps = """dcms##dcms##11110##dcms.war##war"""
# ccod_apps = """DialEngine##DialEngine##20a0ff3d24ae5d2c45523ab5c7e0da7b86db4c18##DialEngine##binary
# AppGateWay##AppGateWay##3b651073c03e1e3fedf73f25a1565c602b8e4040##AppGateWay##binary
# UCGateway##UCGateway##2df8a399b4c50cc9602c11c9cbfae23d07f134dc##UCGateway##binary
# glsServer##glsServer##7b699a4aece10ef28dce83ab36e4d79213ec4f69##Glsserver##binary
# RMServer##rmserver##26461:26692##RMServer##binary
# cas##cas##10973##cas.war##war
# glsServer##glsServer##ece10ef28dce83ab36e4d79213ec4f69##Glsserver##binary
# LicenseServer##license##5214##LicenseServer##binary
# configserver##configserver##aca2af60caa0fb9f4af57f37f869dafc90472525##configserver##binary
# gls##gls##10309##gls.war##war
# dcms##dcms##11110##dcms.war##war
# dcmsWebservice##dcmsWebservice##20503##dcmsWebservice.war##war
# dcmsRecord##dcmsRecord##21763##dcmsRecord.war##war
# dcmsStaticsReport##dcmsStatics##20537##dcmsStatics.war##war
# dcmsStaticsReport##dcmsStaticsReport##20528##dcmsStaticsReport.war##war
# safetyMonitor##safetyMonitor##20383##safetyMonitor.war##war
# dcmssg##dcmsSG##20070##dcmsSG.war##war
# customWebservice##customWebservice##19553##customWebservice.war##war
# dcmsx##dcmsx##master_8efabf4##dcmsx.war##war
# slee##slee##3.1.5.0##slee.jar##binary
# UCGateway##UCGateway##b4c50cc9602c11c9cbfae23d07f134dc##UCGateway##binary
# AppGateWay##AppGateWay##c03e1e3fedf73f25a1565c602b8e4040##AppGateWay##binary
# DialEngine##DialEngine##24ae5d2c45523ab5c7e0da7b86db4c18##DialEngine##binary
# cmsserver##cms##4c303e2a4b97a047f63eb01b247303c9306fbda5##cmsserver##binary
# UCDServer##ucds##deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e##UCDServer##binary
# ucxserver##ucx##1fef2157ea07c483979b424c758192bd709e6c2a##ucxserver##binary
# DDSServer##dds##150:18722##DDSServer##binary
# dcs##dcs##155:21974##dcs##binary
# StatSchedule##ss##154:21104##StatSchedule##binary
# EAService##eas##216:11502##EAService##binary
# dcproxy##dcproxy##195:21857##dcproxy##binary
# daengine##daengine##179:20744##daengine##binary"""
make_image_base_path = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/test'
test_schema_json = """{"domainUpdatePlanList":[{"domainName":"运营门户01","domainId":"ops01","setId":"supportPortal","bkSetName":"运营门户","appUpdateOperationList":[{"platformAppId":0,"operation":"ADD","appName":"gls","appAlias":"gls","originalVersion":"","targetVersion":"10309","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"gls","cfgs":[{"fileName":"Param-Config.xml","ext":"xml","fileSize":0,"md5":"0d4c565c8a683c7f33204f92de20e489","deployPath":"./gls/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"gls/gls/10309","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ5ZjQ3NzA2NWIwOTA3ODdm"}]}],"updateType":"ADD","status":"CREATE","createTime":null,"updateTime":null,"executeTime":null,"comment":"new domain"},{"domainName":"公共组件01","domainId":"public01","setId":"publicModules","bkSetName":"公共组件","appUpdateOperationList":[{"platformAppId":0,"operation":"ADD","appName":"LicenseServer","appAlias":"LicenseServer","originalVersion":"","targetVersion":"5214","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"LicenseServer","cfgs":[{"fileName":"Config.ini","ext":"ini","fileSize":0,"md5":"1797e46c56de0b00e11255d61d5630e8","deployPath":"./bin/license/","nexusRepository":"ccod_modules","nexusPath":"/LicenseServer/license/5214","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM4NWZiNzUwN2U2Y2I5MTNl"}]},{"platformAppId":0,"operation":"ADD","appName":"configserver","appAlias":"configserver","originalVersion":"","targetVersion":"aca2af60caa0fb9f4af57f37f869dafc90472525","hostIp":"10.130.41.218","basePath":"/home/cfs/Platform/","appRunner":"configserver","cfgs":[{"fileName":"ccs_config.cfg","ext":"cfg","fileSize":0,"md5":"844cbcf66f9d16f7d376067831d67cfd","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"configserver/configserver/aca2af60caa0fb9f4af57f37f869dafc90472525","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM5YmQ1ZDdmMGY4Y2U4M2M3"},{"fileName":"ccs_logger.cfg","ext":"cfg","fileSize":0,"md5":"197075eb110327da19bfc2a31f24b302","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"configserver/configserver/aca2af60caa0fb9f4af57f37f869dafc90472525","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQyMTFmNGY1Y2E4OGY0ZGYx"}]},{"platformAppId":0,"operation":"ADD","appName":"glsServer","appAlias":"glsServer","originalVersion":"","targetVersion":"7b699a4aece10ef28dce83ab36e4d79213ec4f69","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"glsServer","cfgs":[{"fileName":"gls_config.cfg","ext":"cfg","fileSize":0,"md5":"fff65661bc6b88f7c21910146432044b","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"glsServer/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRmYTZkNGEwOWE1YWUwNjQ0"},{"fileName":"gls_logger.cfg","ext":"cfg","fileSize":0,"md5":"7b8e1879eab906cba05dabf3f6e0bc37","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"glsServer/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjYwY2U1OGU2MGUyOWUxYjlh"}]}],"updateType":"ADD","status":"CREATE","createTime":null,"updateTime":null,"executeTime":null,"comment":"new domain"},{"domainName":"管理门户01","domainId":"manage01","setId":"managerPortal","bkSetName":"管理门户","appUpdateOperationList":[{"platformAppId":0,"operation":"ADD","appName":"customWebservice","appAlias":"customWebservice","originalVersion":"","targetVersion":"19553","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"customWebservice","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"4708f827e04c5f785930696d7c81e23e","deployPath":"./customWebservice/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"customWebservice/customWebservice/19553","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZkZjVmZjhmOTMwODUzMGQ1"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"74e822b75eb8a90e5c0b0f0eec00df38","deployPath":"./customWebservice/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"customWebservice/customWebservice/19553","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE4MzdlOWUyOWE2MmM2YjZl"}]},{"platformAppId":0,"operation":"ADD","appName":"dcms","appAlias":"dcms","originalVersion":"","targetVersion":"11110","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcms","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"748cbedd71488664433cb2bb59f7b3c7","deployPath":"./dcms/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcms/dcms/11110","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNmNDliMTA1NWZjM2E3NmRi"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"039961b0aff865b1fb563a2823d28ae1","deployPath":"./dcms/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcms/dcms/11110","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRjZmE0YWQ2NDJlNTA3MTVj"},{"fileName":"Param-Config.xml","ext":"xml","fileSize":0,"md5":"1d54648884d965951101abade31564fd","deployPath":"./dcms/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcms/dcms/11110","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZkNGUwNTUzZWI3YmExYmQ0"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmsRecord","appAlias":"dcmsRecord","originalVersion":"","targetVersion":"21763","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmsRecord","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"5e292ede1aa89f7255848fc3eb0b98e9","deployPath":"./dcmsRecord/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcmsRecord/dcmsRecord/21763","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWExZjFmNmJmMjI1ZDdkMDZk"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"4e2f8f01783d5a59ba2d665f3342630d","deployPath":"./dcmsRecord/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsRecord/dcmsRecord/21763","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhMmI2MDgwODljMDM3Yzk3"},{"fileName":"applicationContext.xml","ext":"xml","fileSize":0,"md5":"2167da546f02041f985e59bc7abb5b88","deployPath":"./dcmsRecord/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsRecord/dcmsRecord/21763","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ0YjUwZDNmNjIzMDk3NzMz"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmssg","appAlias":"dcmssg","originalVersion":"","targetVersion":"20070","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmssg","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"c9c2d995e436f9e3ce20bea9f58675f3","deployPath":"./dcmsSG/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcmssg/dcmsSG/20070","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRiOTVjMTcyMjRjYmNjZTk0"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"06c1c1a72c35280b61e8c0005101aced","deployPath":"./dcmsSG/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmssg/dcmsSG/20070","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZmODEyOGZmYzlhYWFlYTIz"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmsStaticsReport","appAlias":"dcmsStatics","originalVersion":"","targetVersion":"20537","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmsStatics","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"1bd3d0faf77ef7e72ae3dc853eb2a9f5","deployPath":"./dcmsStatics/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcmsStaticsReport/dcmsStatics/20537","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE2MTVlNTE3NDdkMmU5YjAz"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"7adfc663082fa8a5a45792d9beda3f90","deployPath":"./dcmsStatics/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsStaticsReport/dcmsStatics/20537","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM1NzYyZWU3OGJiNjZlODhk"},{"fileName":"applicationContext.xml","ext":"xml","fileSize":0,"md5":"9e6f0f413ce17c98aa20c960cf3eae0c","deployPath":"./dcmsStatics/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsStaticsReport/dcmsStatics/20537","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRhMTVkMGY1ZjVlYWM4NGJh"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmsWebservice","appAlias":"dcmsWebservice","originalVersion":"","targetVersion":"20503","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmsWebservice","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"dae594913326ed68249ae37d8dae94d4","deployPath":"./dcmsWebservice/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcmsWebservice/dcmsWebservice/20503","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQyZTEyNzI2ZjFjM2U0OTgz"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"7beb9ba371f97d22dbd1fed55c10bc78","deployPath":"./dcmsWebservice/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsWebservice/dcmsWebservice/20503","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZiZmNlZTExZDI1ZWRmMjA0"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmsx","appAlias":"dcmsx","originalVersion":"","targetVersion":"master_8efabf4","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmsx","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"6540a11bd5c91033c3adf062275154ca","deployPath":"./dcmsx/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"dcmsx/dcmsx/master_8efabf4","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhZjVjYzdmMGI4ZjJkNTky"},{"fileName":"application.properties","ext":"properties","fileSize":0,"md5":"a16001e657e776c6d0a5d3076cfad13d","deployPath":"./dcmsx/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"dcmsx/dcmsx/master_8efabf4","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRkNDdhZjY5ZTg5NDAwYWI3"}]},{"platformAppId":0,"operation":"ADD","appName":"safetyMonitor","appAlias":"safetyMonitor","originalVersion":"","targetVersion":"20383","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"safetyMonitor","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"0129c9dab847d5fc0f50f437d66f06c2","deployPath":"./safetyMonitor/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"safetyMonitor/safetyMonitor/20383","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE4ZGFkNjBlNDc2Mzc2N2Yy"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"b9f401a56d80cd92c2840c7965b9c3f6","deployPath":"./safetyMonitor/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"safetyMonitor/safetyMonitor/20383","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM5ODc1MzAyZjBhODc5MzM4"},{"fileName":"applicationContext.xml","ext":"xml","fileSize":0,"md5":"493795bd1d8b35dde443e9dd732da30e","deployPath":"./safetyMonitor/WEB-INF/classes/","nexusRepository":"ccod_modules","nexusPath":"safetyMonitor/safetyMonitor/20383","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRjZmE0ZTIxNzkzNTllODky"}]},{"platformAppId":0,"operation":"ADD","appName":"dcmsStaticsReport","appAlias":"dcmsStaticsReport","originalVersion":"","targetVersion":"20528","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/resin-4.0.13/webapps/","appRunner":"dcmsStaticsReport","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"4673b1b939e9567a8e6a6a4ef6da4993","deployPath":"./dcmsStaticsReport/WEB-INF/","nexusRepository":"some_test","nexusPath":"dcmsStaticsReport/20528","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ2MDM0ZTU3MTA1N2RlM2Qx"},{"fileName":"config.properties","ext":"properties","fileSize":0,"md5":"34fb9d13c742306b2141f3a1bc79aaa2","deployPath":"./dcmsStaticsReport/WEB-INF/classes/","nexusRepository":"some_test","nexusPath":"dcmsStaticsReport/20528","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZlMTE1MTliMjc0MDE4NGE5"},{"fileName":"applicationContext.xml","ext":"xml","fileSize":0,"md5":"3d58aeb1b72748800e78c136b0232c4c","deployPath":"./dcmsStaticsReport/WEB-INF/classes/","nexusRepository":"some_test","nexusPath":"dcmsStaticsReport/20528","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE0MjlkMWRiMGUzMDlkMTJh"}]},{"platformAppId":0,"operation":"ADD","appName":"cas","appAlias":"cas","originalVersion":"","targetVersion":"10973","hostIp":"10.130.41.218","basePath":"/home/portal/tomcat/webapps/","appRunner":"cas","cfgs":[{"fileName":"web.xml","ext":"xml","fileSize":0,"md5":"06c29dce651ed51e092276533559853a","deployPath":"./cas/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"cas/cas/10973","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFjOTg5M2E5MzI0YjFlODRj"},{"fileName":"cas.properties","ext":"properties","fileSize":0,"md5":"c74190420467285db96a1e7a46a26573","deployPath":"./cas/WEB-INF/","nexusRepository":"ccod_modules","nexusPath":"cas/cas/10973","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMwMTJlOWVmZGUwMTNjMjlk"}]}],"updateType":"ADD","status":"CREATE","createTime":null,"updateTime":null,"executeTime":null,"comment":"new domain"},{"domainName":"域服务01","domainId":"cloud01","setId":"domainService","bkSetName":"域服务","appUpdateOperationList":[{"platformAppId":0,"operation":"ADD","appName":"UCDServer","appAlias":"ucds","originalVersion":"","targetVersion":"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"ucds","cfgs":[{"fileName":"DRWRClient.cfg","ext":"cfg","fileSize":0,"md5":"1954c2c1f488406f383cdf5a235ab868","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM3NWY1YmY5Yjg1OTgwYTY0"},{"fileName":"ucds_logger.cfg","ext":"cfg","fileSize":0,"md5":"ec57329ddcec302e0cc90bdbb8232a3c","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMjM2NmQ2OTRkZDRjNjAy"},{"fileName":"ucds_config.cfg","ext":"cfg","fileSize":0,"md5":"c78bfdf874c8c8a1ae6c55ac2e952306","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjY0MWFhYmE4NGQ0NDZkNjFk"}]},{"platformAppId":0,"operation":"ADD","appName":"dcs","appAlias":"dcs","originalVersion":"","targetVersion":"155:21974","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"dcs","cfgs":[{"fileName":"DCServer.cfg","ext":"cfg","fileSize":0,"md5":"63d5267c83a84a236f7e9e6f10ab8720","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/dcs/dcs/155:21974","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNlZGU3MjU2ZTc2N2QxYjY3"},{"fileName":"dc_log4cpp.cfg","ext":"cfg","fileSize":0,"md5":"138877a50a0f85a397ddbcf6be62095b","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/dcs/dcs/155:21974","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQwY2M1Mzc0MTM4ZGVkZmYz"}]},{"platformAppId":0,"operation":"ADD","appName":"cmsserver","appAlias":"cms1","originalVersion":"","targetVersion":"4c303e2a4b97a047f63eb01b247303c9306fbda5","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform/","appRunner":"cms1","cfgs":[{"fileName":"config.cms2","ext":"cms2","fileSize":0,"md5":"ebc8435fcea1515c2d73eaa8b46ccf39","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMGRmM2FkMDUyZmI0MmNl"},{"fileName":"beijing.xml","ext":"xml","fileSize":0,"md5":"4168695ceba63dd24d53d46fa65cffb1","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFiNzEyOTMxNjdlMmQwMTIw"},{"fileName":"cms_log4cpp.cfg","ext":"cfg","fileSize":0,"md5":"b16210d40a7ef123eef0296393df37b8","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMyMmU0NzE3OGU4MDU1NWZi"}]},{"platformAppId":0,"operation":"ADD","appName":"cmsserver","appAlias":"cms2","originalVersion":"","targetVersion":"4c303e2a4b97a047f63eb01b247303c9306fbda5","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform/","appRunner":"cms2","cfgs":[{"fileName":"config.cms2","ext":"cms2","fileSize":0,"md5":"ebc8435fcea1515c2d73eaa8b46ccf39","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMGRmM2FkMDUyZmI0MmNl"},{"fileName":"beijing.xml","ext":"xml","fileSize":0,"md5":"4168695ceba63dd24d53d46fa65cffb1","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFiNzEyOTMxNjdlMmQwMTIw"},{"fileName":"cms_log4cpp.cfg","ext":"cfg","fileSize":0,"md5":"b16210d40a7ef123eef0296393df37b8","deployPath":"./etc/","nexusRepository":"ccod_modules","nexusPath":"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMyMmU0NzE3OGU4MDU1NWZi"}]},{"platformAppId":0,"operation":"ADD","appName":"ucxserver","appAlias":"ucx","originalVersion":"","targetVersion":"1fef2157ea07c483979b424c758192bd709e6c2a","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform/","appRunner":"ucx","cfgs":[{"fileName":"config.ucx","ext":"ucx","fileSize":0,"md5":"6c2aca996f3e1e6fad277cafffd1ebf7","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"ucxserver/ucx/1fef2157ea07c483979b424c758192bd709e6c2a","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhYTUxY2IxOGM3ZGI5ZTRh"}]},{"platformAppId":0,"operation":"ADD","appName":"DDSServer","appAlias":"dds","originalVersion":"","targetVersion":"150:18722","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"dds","cfgs":[{"fileName":"dds_config.cfg","ext":"cfg","fileSize":0,"md5":"38e4194d03e10f5ce7fbf364fc5678b9","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/DDSServer/dds/150:18722","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzZGUyZjhjYmUzZWE1N2Uw"},{"fileName":"dds_logger.cfg","ext":"cfg","fileSize":0,"md5":"fe3c70d26b3827d44473b06f46af0970","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/DDSServer/dds/150:18722","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNmMDQwZTBiMWM4NTBlMmVh"}]},{"platformAppId":0,"operation":"ADD","appName":"StatSchedule","appAlias":"ss","originalVersion":"","targetVersion":"154:21104","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"ss","cfgs":[{"fileName":"ss_config.cfg","ext":"cfg","fileSize":0,"md5":"9c3476beac9ee275fa06a91497f58cd7","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/StatSchedule/ss/154:21104","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQxZGY1ZjgzYzJlZGI5ZGU5"}]},{"platformAppId":0,"operation":"ADD","appName":"dcproxy","appAlias":"dcproxy","originalVersion":"","targetVersion":"195:21857","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"dcproxy","cfgs":[{"fileName":"dcp_config.cfg","ext":"cfg","fileSize":0,"md5":"3fd2a067221bbc974cd05997fe46fe6b","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/dcproxy/dcproxy/195:21857","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ2ZDM1YmRiYjljOTI0OWY5"},{"fileName":"dcp_logger.cfg","ext":"cfg","fileSize":0,"md5":"08dbf42e8c02425e3a11b9cef38a9a7c","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/dcproxy/dcproxy/195:21857","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjY0YTg1MDkwMThkMmJhYmQ0"}]},{"platformAppId":0,"operation":"ADD","appName":"daengine","appAlias":"daengine","originalVersion":"","targetVersion":"179:20744","hostIp":"10.130.41.218","basePath":"/home/ccodrunner/Platform","appRunner":"daengine","cfgs":[{"fileName":"dae.cfg","ext":"cfg","fileSize":0,"md5":"b52196dbae7fa53481ec937dacfa7e2a","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/daengine/daengine/179:20744","nexusAssetId":"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZmNTc5Yjg3ZDQwNmIyOGRi"},{"fileName":"dae_config.cfg","ext":"cfg","fileSize":0,"md5":"04544c8572c42b176d501461168dacf4","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/daengine/daengine/179:20744","nexusAssetId":"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE2NTc0YWZhMGZiMzNhN2Y2"},{"fileName":"dae_log4cpp.cfg","ext":"cfg","fileSize":0,"md5":"0d5b6405de9af28401f7d494888eed8f","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/daengine/daengine/179:20744","nexusAssetId":"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM3YjVjMjVjYzgxODhhOTcw"},{"fileName":"dae_logger.cfg","ext":"cfg","fileSize":0,"md5":"ac2fde58b18a5ab1ee66d911982a326c","deployPath":"./cfg/","nexusRepository":"ccod_modules","nexusPath":"/daengine/daengine/179:20744","nexusAssetId":"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQzMjg1NjAwMzEzNTI4ODE4"}]}],"updateType":"ADD","status":"CREATE","createTime":null,"updateTime":null,"executeTime":null,"comment":"new domain"}],"platformId":"pahjgsrqhcs","platformName":"平安环境公司容器化测试","bkBizId":29,"bkCloudId":0,"ccodVersion":"ccod4.1","taskType":"CREATE","status":"CREATE","createTime":null,"updateTime":null,"executeTime":null,"deadline":null,"title":"pahjgsrqhcs平台使用模板完成规划","comment":"pahjgsrqhcs平台使用模板完成规划"}"""


def __get_app_query_url(app_name, version):
    return '%s/cmdb/api/apps/%s/%s' % (cmdb_host_url, app_name, version)


def __get_app_cfg_download_uri(platform_id, domain_id, app_alias, file_name):
    return '%s/repository/%s/configText/%s/%s_%s/%s' % (nexus_host_url, cfg_repository, platform_id, domain_id, app_alias, file_name)


def query_app_module(app_name, version):
    query_url = __get_app_query_url(app_name, version)
    response = requests.get(query_url)
    app_module = None
    logging.debug('query %s return : %s' % (query_url, response.text))
    ajax_result = json.loads(response.text)
    if ajax_result['success'] is True:
        app_module = ajax_result['rows']
    else:
        logging.error('query %s return error ： %s' % (query_url, ajax_result['msg']))
    return app_module


def get_app_cfg_params_for_k8s(platform_id, domain_id, app_module, app_alias):
    cfg_params = ""
    for cfg in app_module['cfgs']:
        cfg_deploy_path = re.sub('^.*WEB-INF/', 'WEB-INF/', cfg['deployPath'])
        cfg_deploy_path = re.sub('/$', '', cfg_deploy_path)
        cfg_file_name = re.sub('\\.', '\\\\\\.', cfg['fileName'])
        cfg_params = '%s --set config.%s=%s' % (cfg_params, cfg_file_name, cfg_deploy_path)
    cfg_uri = '%s/repository/%s/configText/%s/%s_%s' % (
        nexus_host_url, cfg_repository, platform_id, domain_id, app_alias)
    return '%s --set runtime.configPath=%s' % (cfg_params, cfg_uri)


def get_app_helm_command(platform_id, domain_id, app_module, app_alias, work_dir):
    version = re.sub('\\:', '-', app_module['version'])
    app_name = app_module['appName']
    app_type = app_module['appType']
    app_work_path = 'cd %s;cd payaml/%s' % (work_dir, app_name)
    if app_type == 'CCOD_WEBAPPS_MODULE':
        if app_name == 'cas':
            app_work_path = 'cd %s;cd payaml/tomcat6-jre7/' % work_dir
        else:
            app_work_path = 'cd %s;cd payaml/resin-4.0.13_jre-1.6.0_21/' % work_dir
    cfg_params_for_k8s = get_app_cfg_params_for_k8s(platform_id, domain_id, app_module, app_alias)
    exec_command = '%s;helm install --set module.vsersion=%s --set module.name=%s --set module.alias=%s-%s %s %s-%s . -n %s' % (
        app_work_path, version, app_name, app_alias, domain_id,  cfg_params_for_k8s, app_alias.lower(), domain_id.lower(), platform_id.lower()
    )
    logging.info('command for deploy %s/%s/%s/%s/%s is %s and app_work_dir is %s' % (
        platform_id, domain_id, app_name, app_alias, version, exec_command, app_work_path
    ))
    return exec_command


def __command_result_regex_match(command_result, regex, command_type='bash/shell'):
    """
    解析命令输出结果是否满足某个正则表达式，所有的命令结果在解析前需要删掉空行,命令输出结果的最后一个\n也要被去掉,
    bash/shell类型采用多行模式进行匹配,oracle/sql则需要把所有结果拼成一行进行匹配，特别注意的是在拼成一行之前需要把每行前面的空格去掉
    :param command_result:
    :param regex:
    :param command_type:
    :return:
    """
    input_str = re.compile('\s+\n').sub('\n', command_result)
    input_str = re.compile('\n{2,}').sub('\n', input_str)
    input_str = re.compile('\n$').sub('', input_str)
    input_str = re.compile('^\n').sub('', input_str)
    if command_type == 'bash/shell':
        if re.match(regex, input_str, flags=re.DOTALL):
            is_match = True
        else:
            is_match = False
    elif command_type == 'oracle/sql':
        input_str = re.compile('^\s+').sub('', input_str)
        input_str = re.compile('\n').sub('', input_str)
        if re.match(regex, input_str):
            is_match = True
        else:
            is_match = False
    else:
        logging.error('目前还不支持' + command_type + '类型命令结果解析')
        is_match = False
    return is_match


def __timeout_shell_command(command, timeout, cwd=None):
    """
    执行一条shell命令并返回命令结果,如果超过timeout还没有返回将返回None
    :param command:需要执行的命令可以是相对cwd的相对路径
    :param timeout:执行命令的超时时长
    :param cwd:执行命令的cwd
    :return:命令执行结果,如果超过timeout还没有返回将返回None
    """
    logging.info("准备执行命令:%s,超时时长:%s" % (command, timeout))
    start = datetime.datetime.now()
    if cwd:
        logging.info('cwd=%s' % cwd)
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=cwd,
                               close_fds=True)
    else:
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, close_fds=True)
    while process.poll() is None:
        time.sleep(0.1)
        now = datetime.datetime.now()
        if (now - start).seconds >= timeout:
            return None
    out = process.communicate()[0]
    if process.stdin:
        process.stdin.close()
    if process.stdout:
        process.stdout.close()
    if process.stderr:
        process.stderr.close()
    result = out.decode().encode('utf-8')
    str_info = re.compile("(\n)*$")
    result = str_info.sub('', result)
    logging.info("命令执行结果:%s" % result)
    return result


def __run_shell_command(command, cwd=None):
    """
    执行一条脚本命令,并返回执行结果输出
    :param command:执行命令,可执行文件/脚本可以是相对cwd的相对路径
    :param command:执行命令的cwd
    :return:返回结果
    """
    logging.info("准备执行命令:%s" % command)
    if cwd:
        logging.info('cwd=%s' % cwd)
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=cwd,
                               close_fds=True)
    else:
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, close_fds=True)
    out = process.communicate()[0]
    if process.stdin:
        process.stdin.close()
    if process.stdout:
        process.stdout.close()
    if process.stderr:
        process.stderr.close()
    result = out.decode().encode('utf-8')
    str_info = re.compile("(\n)*$")
    result = str_info.sub('', result)
    logging.info("命令执行结果\n:%s" % result)
    return result


def nexus_group_query(repository, group):
    url = "%s/service/rest/v1/search?repository=%s&group=%s" % (nexus_host_url, repository, group)
    # url = "%s/service/rest/v1/search?repository=%s&group=%s" % (nexus_host_url, 'ccod_modules', '/dcproxy/dcproxy/195:21857')
    try:
        a = HTTPBasicAuth(nexus_user, nexus_user_pwd)
        response = requests.get(url=url, auth=a)
        data = json.loads(response.text)
        logging.info(json.dumps(data, ensure_ascii=False))
        print(json.dumps(data, ensure_ascii=False))
        items = data['items']
    except Exception as e:
        logging.error('query %s exception' % url, exc_info=True)
        raise e
    else:
        pass
    return items


def check_app_and_cfg_file(platform_id, domain_id, app_name, app_alias, version):
    check_result = ""
    app_module = query_app_module(app_name, version)
    if not app_module:
        check_result = '%s with version %s not found' % (app_name, version)
    else:
        group = '/configText/%s/%s_%s' % (platform_id, domain_id, app_alias)
        items = nexus_group_query(cfg_repository, group)
        upload_cfgs = dict()
        for item in items:
            upload_file_name = item['name'].split('/')[-1]
            upload_cfgs[upload_file_name] = item
        for cfg in app_module['cfgs']:
            if cfg['fileName'] not in upload_cfgs.keys():
                check_result = '%s%s,' % (check_result, cfg['fileName'])
        if check_result:
            check_result = re.sub(',$', '', check_result)
            check_result = 'cfg %s of %s/%s/%s not found' % (check_result, platform_id, domain_id, app_alias)
        else:
            check_result = None
    return check_result


def download_file_from_nexus(file_download_url, save_path):
    file_download_url = re.sub(u'/$', u'', file_download_url)
    file_name = file_download_url.split('/')[-1]
    md5_l = hashlib.md5()
    logging.debug('download %s from %s and save as %s' % (file_name, file_download_url, save_path))
    try:
        req = urllib2.Request(file_download_url)
        req.add_header("Authorization", "Basic YWRtaW46MTIzNDU2")
        f = urllib2.urlopen(req)
        data = f.read()
        with open(save_path, "w") as code:
            code.write(data)
        md5_l.update(data)
        md5_ret = md5_l.hexdigest()
        print(md5_ret)
    except Exception as e:
        logging.error('download %s from %s fail' % (file_name, file_download_url), exc_info=True)
        raise e
    else:
        pass


def download_install_package(app_name, app_alias, version, package_file_name, save_dir):
    package_download_url = '%s/repository/%s/%s/%s/%s/%s' % (nexus_host_url, app_repository, app_name, app_alias, version, package_file_name)
    save_path = '%s/%s' % (save_dir, package_file_name)
    download_file_from_nexus(package_download_url, save_path)
    return save_path


def generate_docker_file(app_package_file_name, save_dir, package_type='binary'):
    save_file_path = '%s/%s' % (save_dir, 'Dockerfile')
    print(save_file_path)
    with open(save_file_path, 'w') as out_f:
        if package_type == 'binary':
            out_f.write("FROM harbor.io:1180/ccod-base/centos-backend:0.4\n")
            # out_f.write("COPY lib/*.* /opt/\n")
        else:
            out_f.write("FROM nexus.io:5000/ccod-base/alpine-java:jdk8-slim\n")
        out_f.write("ADD %s /opt/%s\n" % (app_package_file_name, app_package_file_name))
        if package_type == 'binary':
            out_f.write('RUN chmod a+x /opt/%s\n' % app_package_file_name)
            out_f.write('CMD ["/bin/bash", "-c", "ldd /opt/%s;/opt/%s"]"]\n' % (app_package_file_name, app_package_file_name))


def remove_generate_image(app_name, version):
    exec_command = "docker image ls| grep %s | grep %s | awk '{print $3}'" % (app_name.lower(), version)
    exec_result = __run_shell_command(exec_command, None)
    if exec_result:
        image_id = exec_result
        print('image_id=%s$$$$$' % image_id)
        exec_command = "docker ps -a | grep %s | awk '{print $1}'" % image_id
        exec_result = __run_shell_command(exec_command, None)
        if exec_result:
            container_ids = exec_result.split('\n')
            for container_id in container_ids:
                exec_command = 'docker stop %s' % container_id
                __run_shell_command(exec_command, None)
                exec_command = 'docker rm %s' % container_id
                __run_shell_command(exec_command, None)
        exec_command = 'docker rmi %s' % image_id
        __run_shell_command(exec_command, None)


def get_app_image_uri(app_name, version):
    version = re.sub('\\:', '\\-', version)
    image_tag = '%s:%s' % (app_name.lower(), version)
    image_uri = '%s/%s/%s' % (docker_image_repository_uri, image_repository, image_tag)
    return image_uri


def build_app_image(app_name, version, app_dir):
    # logging.debug('cp lib of %s to ./lib' % gcc_depend_lib_path)
    # exec_command = 'cd %s;mkdir lib;cp %s/*.* ./lib' % (app_dir, gcc_depend_lib_path)
    # exec_result = __run_shell_command(exec_command, None)
    # print(exec_result)
    version = re.sub('\\:', '\\-', version)
    image_tag = '%s:%s' % (app_name.lower(), version)
    image_uri = '%s/%s/%s' % (docker_image_repository_uri, image_repository, image_tag)
    exec_command = 'cd %s;docker build -t %s .' % (app_dir, image_uri)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    exec_command = 'cd %s;docker push %s' % (app_dir, image_uri)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    return image_uri


def test_image(image_uri):
    exec_command = 'docker pull %s' % image_uri
    logging.debug('pull %s from nexus repository' % image_uri)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    exec_command = 'docker run -it --rm %s' % image_uri
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)


def app_image_exist_check(app_name, version):
    remove_generate_image(app_name, version)
    app_image_uri = get_app_image_uri(app_name, version)
    exec_command = 'docker pull %s' % app_image_uri
    exec_result = __run_shell_command(exec_command, None)
    if __command_result_regex_match(exec_result, '.*Error response.*'):
        logging.error('pull %s fail : %s' % (app_image_uri, exec_result))
        return False
    else:
        remove_generate_image(app_name, version)
        return True


def generate_app_image(app_name, app_alias, version, package_file_name, package_type, app_dir):
    download_install_package(app_name, app_alias, version, package_file_name, app_dir)
    generate_docker_file(package_file_name, app_dir, package_type)
    image_uri = build_app_image(app_name, version, app_dir)
    return image_uri


def preprocess_ccod_app(base_path):
    ret_list = list()
    app_list = ccod_apps.split('\n')
    for app in app_list:
        if not app:
            continue
        app_prop = dict()
        app_prop['app_name'] = app.split('##')[0]
        app_prop['app_alias'] = app.split('##')[1]
        app_prop['version'] = app.split('##')[2]
        app_prop['package_file_name'] = app.split('##')[3]
        app_prop['package_type'] = app.split('##')[4]
        app_prop['app_dir'] = '%s/%s' % (base_path, app_prop['app_name'])
        if not os.path.exists(app_prop['app_dir']):
            os.makedirs(app_prop['app_dir'])
        ret_list.append(app_prop)
    return ret_list


def generate_image_for_ccod_apps(base_path):
    app_list = preprocess_ccod_app(base_path)
    for ccod_app in app_list:
        remove_generate_image(ccod_app['app_name'], ccod_app['version'])
    for ccod_app in app_list:
        logging.debug('****************\nbegin generate image for %s version %s\n************'
                      %(ccod_app['app_name'], ccod_app['version']))
        create_succ = False
        try:
            image_uri = generate_app_image(ccod_app['app_name'], ccod_app['app_alias'], ccod_app['version'],
                                       ccod_app['package_file_name'], ccod_app['package_type'], ccod_app['app_dir'])
            ccod_app['image_uri'] = image_uri
            create_succ = True
        except Exception as e:
            logging.error('create image for %s version %s exception' % (ccod_app['app_name'], ccod_app['version']), exc_info=True)
        # if create_succ and ccod_app['package_type'] == 'binary':
        #     try:
        #         test_image(image_uri)
        #     except Exception as e:
        #         logging.error('test image for %s version %s exception, image_uri=%s' % (ccod_app['app_name'], ccod_app['version'], image_uri), exc_info=True)
        #     else:
        #         pass
        # remove_generate_image(ccod_app['app_name'], ccod_app['version'])
    return app_list


def create_ccod_platform(platform_id, work_dir):
    logging.debug('check platform %s created' % platform_id)
    exec_command = "kubectl get namespace | awk '{print $1}' | grep -P \"^%s$\"" % platform_id
    exec_result = __run_shell_command(exec_command, None)
    if exec_result:
        logging.debug('%s exist, so delete %s and recreate ' % (platform_id, platform_id))
        exec_command = 'kubectl delete namespace %s' % platform_id
        exec_result = __run_shell_command(exec_command, None)
        print(exec_result)
        exec_command = 'kubectl delete pv base-volume-%s' % platform_id
        exec_result = __run_shell_command(exec_command, None)
        print(exec_result)
    logging.debug('create platform %s' % platform_id)
    exec_command = 'kubectl create namespace %s' % platform_id
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    logging.debug('create base service for %s' % platform_id)
    print('create base service for %s' % platform_id)
    db_param = '--set oracle.ccod.ip=10.130.41.12 --set oracle.ccod.port=1521 --set oracle.ccod.sid=ccdev --set oracle.ccod.user=ccod --set oracle.ccod.passwd=ccod --set mysql.ccod.ip=10.130.41.12 --set mysql.ccod.port=3306 --set mysql.ccod.user=ucds  --set mysql.ccod.passwd=ucds'
    # exec_command = 'cd %s;cd payaml/baseService;helm install %s -n %s baseservice .' % (work_dir, db_param, platform_id)
    exec_command = 'cd %s;cd payaml/baseService;helm install -n %s baseservice .' % (work_dir, platform_id)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    logging.debug('create cas cert for %s' % platform_id)
    print('create cas cert for %s' % platform_id)
    exec_command = """cd %s;kubectl get -n kube-system secret ssl -o yaml | grep -vE '(creationTimestamp|resourceVersion|selfLink|uid)'|sed "s/kube-system/%s/g" > ssl.yaml""" % (
        work_dir, platform_id
    )
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    exec_command = 'cd %s;kubectl apply -f ssl.yaml' % work_dir
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)


def check_platform_create_schema(schema):
    platform_id = schema['platformId'].lower()
    check_result = ""
    for domain_plan in schema['domainUpdatePlanList']:
        domain_id = domain_plan['domainId']
        for deploy_app in domain_plan['appUpdateOperationList']:
            app_name = deploy_app['appName']
            app_alias = deploy_app['appAlias']
            version = deploy_app['targetVersion']
            if not app_image_exist_check(app_name, version):
                check_result = '%simage for %s version %s not exist;' % (check_result, app_name, version)
            app_check_result = check_app_and_cfg_file(platform_id, domain_id, app_name, app_alias, version)
            if app_check_result is not None:
                check_result = '%s%s;\n' % (check_result, app_check_result)
    if check_result:
        logging.error('schema check fail : %s' % check_result)
    else:
        logging.debug('schema check success')
        check_result = None
    return check_result


def get_md5_value(input_str):
    md5 = hashlib.md5()
    md5.update(input_str)
    md5_digest = md5.hexdigest()
    return md5_digest


def exec_platform_create_schema(schema):
    platform_id = schema['platformId'].lower()
    now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    work_dir = '/tmp/%s' % get_md5_value('%s%s' % (platform_id, now))
    exec_command = 'mkdir %s -p' % work_dir
    __run_shell_command(exec_command, None)
    exec_command = 'cd %s;git clone %s' % (work_dir, k8s_deploy_git_url)
    __run_shell_command(exec_command, None)
    create_ccod_platform(platform_id, work_dir)
    for domain_plan in schema['domainUpdatePlanList']:
        domain_id = domain_plan['domainId']
        for deploy_app in domain_plan['appUpdateOperationList']:
            app_name = deploy_app['appName']
            app_alias = deploy_app['appAlias']
            version = deploy_app['targetVersion']
            app_module = query_app_module(app_name, version)
            helm_command = get_app_helm_command(platform_id, domain_id, app_module, app_alias, work_dir)
            print(helm_command)
            exec_result = __run_shell_command(helm_command, None)
            # exec_result = 'deploy %s in k8s success' % app_alias
            print(exec_result)
            if app_name == 'glsServer':
                print('%s start, so sleep 60s' % app_name)
                time.sleep(30)
                print('sleep end')
            elif app_name == 'DDSServer' or app_name == 'UCDServer' or app_name == ' dcs' or app_name == 'cmsserver' or app_name == 'ucxserver' or app_name == 'daengine':
                print('%s start, so sleep 20s' % app_name)
                time.sleep(20)
                print('sleep end')


def sync_platform_create_result(plan_create_schema):
    plan_create_schema['status'] = 'SUCCESS'
    # plan_create_schema = {"platformId": "pahjgsrqhcs"}
    # del plan_create_schema['domainUpdatePlanList']
    # del plan_create_schema['executeTime']
    # del plan_create_schema['updateTime']
    # del plan_create_schema['deadline']
    # del plan_create_schema['createTime']
    # del plan_create_schema['ccodVersion']
    # del plan_create_schema['platformName']
    # del plan_create_schema['comment']
    # del plan_create_schema['title']
    data = json.dumps(plan_create_schema, ensure_ascii=False)
    # data = {"platformId": "pahjgsrqhcs"}
    print(data)
    logging.info('sync_platform_create_result_url=%s and data=%s' % (schema_update_url, data))
    logging.info('schema=%s' % data)
    headers = {'Content-Type': 'application/json'}
    # headers = {'Content-Type': 'application/x-www-form-urlencoded'}
    # headers = {}
    # rep = requests.post(schema_update_url, data=data, headers=headers)
    rep = requests.post(schema_update_url, json=plan_create_schema, headers=headers)
    # rep = requests.post(schema_update_url, data=data)
    logging.info('update api return : %s' % rep.text)
    print(rep.text)
    return rep.text


def print_help():
    print('-c images_base_path to create images for global params ccod_apps, images_base_path should be empty directory')
    print('-r to deploy app defined by global param test_schema_json without checking image and cfgs of app')
    print('-vr to check image and cfgs of app defined by global param test_schema_json and then deploy')


def parse_schema_info(schema):
    app_sort_list = ['dds', 'ucds', 'dcs', 'cms1', 'cms2', 'ucx', 'daengine']
    for domain_plan in schema['domainUpdatePlanList']:
        domain_id = domain_plan['domainId']
        if domain_plan['setId'] == 'domainService':
            sort_service_domain(domain_plan, app_sort_list)
        for opt in domain_plan['appUpdateOperationList']:
            logging.info(json.dumps(opt, ensure_ascii=False))
            logging.info('%s/%s/%s/%s' % (opt['appName'], opt['appAlias'], opt['targetVersion'], domain_id))
            print('%s/%s/%s/%s' % (opt['appName'], opt['appAlias'], opt['targetVersion'], domain_id))
        print(json.dumps(schema, ensure_ascii=False))
    logging.info(json.dumps(schema, ensure_ascii=False))


def sort_service_domain(domain_plan, sort_list):
    dt = dict()
    for opt in domain_plan['appUpdateOperationList']:
        app_alias = opt['appAlias']
        dt[app_alias] = opt
    opt_list = list()
    for app_alias in sort_list:
        opt_list.append(dt[app_alias])
        del dt[app_alias]
    # for opt in dt.values():
    #     opt_list.append(opt)
    opt_list.extend(dt.values())
    domain_plan['appUpdateOperationList'] = opt_list


if __name__ == '__main__':
    # download_url = 'http://10.130.41.216:8081/repository/CCOD/CCOD/MONITOR_MODULE/ivr/3.0.0.0/ivr.zip'
    # download_file_from_nexus(download_url, 'D:\\temp\\ivr111.zip')
    # test_app_name = 'UCDServer'
    # test_app_alias = 'ucds'
    # test_version = 'deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e'
    # test_save_dir = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/test/UCDServer'
    # test_package_file_name = 'UCDServer'
    # test_package_type = 'binary'
    # # app_save_path = download_install_package(app_name, app_alias, version, save_dir)
    # # print(app_save_path)
    # # generate_docker_file(app_name, save_dir, is_binary=True)
    # test_image_uri = generate_app_image(test_app_name, test_app_alias, test_version, test_package_file_name, test_package_type, test_save_dir)
    # test_image(test_image_uri)
    # remove_generate_image(test_app_name, test_version)
    # print(test_image_uri)
    # all_ccod_apps = preprocess_ccod_app('D:/temp')
    # logging.debug(json.dumps(all_ccod_apps, ensure_ascii=False))
    # print(json.dumps(all_ccod_apps, ensure_ascii=False))
    # for app_p in all_ccod_apps:
    #     if app_p['package_type'] == 'war':
    #         if app_p['package_file_name'] != '%s.war' % app_p['app_alias']:
    #             logging.error('%s maybe error' % app_p['app_name'])
    #     else:
    #         if app_p['package_file_name'] != app_p['app_name']:
    #             logging.error('%s maybe error' % app_p['app_name'])
    # image_base_path = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/images'
    # generate_image_list = generate_image_for_ccod_apps(image_base_path)
    # logging.info('generateImages=%s' % json.dumps(generate_image_list, ensure_ascii=False))
    # test_app_module = query_app_module('gls', '10309')
    # print(json.dumps(test_app_module, ensure_ascii=False))
    # test_cfg_params = get_app_cfg_params_for_k8s('shpa', 'domain1', 'gls', 'gls', '10309')
    # print(test_cfg_params)
    # test_assets = nexus_group_query('ccod_modules', '/dcs/dcs/10952:27216')
    # print(json.dumps(test_assets, ensure_ascii=False))
    # test_check_result = check_app_and_cfg_file('shpa', 'domain01', 'dcproxy', 'dcproxy', '195:21857')
    # if test_check_result:
    #     print('app check file : %s' % test_check_result)
    # else:
    #     print('app and cfg file ok')
    # test_schema = json.loads(test_schema_json)
    # test_schema_check_result = check_platform_create_schema(test_schema)
    # print(test_schema_check_result)
    # test_schema_check_result = None
    # if not test_schema_check_result:
    #     exec_platform_create_schema(test_schema)
    # parse_schema_info(test_schema)
    if len(sys.argv) == 3 and sys.argv[1] == '-c':
        image_base_path = sys.argv[2]
        generate_image_list = generate_image_for_ccod_apps(image_base_path)
        logging.info('generateImages=%s' % json.dumps(generate_image_list, ensure_ascii=False))
    elif len(sys.argv) == 2 and sys.argv[1] == '-r':
        test_schema = json.loads(test_schema_json)
        exec_platform_create_schema(test_schema)
    elif len(sys.argv) == 2 and sys.argv[1] == '-vr':
        test_schema = json.loads(test_schema_json)
        test_schema_check_result = check_platform_create_schema(test_schema)
        print(test_schema_check_result)
        if not test_schema_check_result:
            exec_platform_create_schema(test_schema)
    elif len(sys.argv) == 2 and sys.argv[1] == '-p':
        test_schema = json.loads(test_schema_json)
        parse_schema_info(test_schema)
    elif len(sys.argv) == 2 and sys.argv[1] == '-s':
        test_schema = json.loads(test_schema_json)
        sync_platform_create_result(test_schema)
    else:
        print_help()
