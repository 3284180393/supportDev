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
import cx_Oracle
import base64


import sys
reload(sys)
sys.setdefaultencoding("utf-8")

LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logging.basicConfig(filename='my.log', level=logging.DEBUG, format=LOG_FORMAT)

gls_standard_alias = 'glsserver'
dcs_standard_alias = 'dcs'
gls_db_name = 'CCOD'
gls_service_unit_table = 'GLS_SERVICE_UNIT'
platform_deploy_params = """"""
app_repository = platform_deploy_params['app_repository']
image_repository = platform_deploy_params['image_repository']
nexus_host_url = platform_deploy_params['nexus_host_url']
nexus_user = platform_deploy_params['nexus_user']
nexus_user_pwd = platform_deploy_params['nexus_user_pwd']
nexus_image_repository_url = platform_deploy_params['nexus_image_repository_url']
cmdb_host_url = platform_deploy_params['cmdb_host_url']
upload_url = "%s/service/rest/v1/components?repository=%s" % (nexus_host_url, app_repository)
app_register_url = "%s/cmdb/api/apps" % cmdb_host_url
schema_update_url = '%s/cmdb/api/platformUpdateSchema' % cmdb_host_url
k8s_deploy_git_url = platform_deploy_params['k8s_deploy_git_url']
app_image_query_url = "http://%s/v2/%s/%%s/tags/list" % (nexus_image_repository_url, image_repository)
platform_deploy_schema = platform_deploy_params['update_schema']
platform_public_config = platform_deploy_params['publicConfig']
ccod_apps = """dcms##dcms##11110##dcms.war##war"""
make_image_base_path = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/test'
k8s_host_ip = platform_deploy_params['k8s_host_ip']
gls_db_type = platform_deploy_params['gls_db_type']
gls_db_user = platform_deploy_params['gls_db_user']
gls_db_pwd = platform_deploy_params['gls_db_pwd']
gls_db_sid = platform_deploy_params['gls_db_sid']
gls_db_svc_name = platform_deploy_params['gls_db_svc_name']
schema_platform_id = platform_deploy_params['platform_id']
platform_deploy_schema = platform_deploy_params['update_schema']
now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
base_data_nexus_repository = platform_deploy_schema['baseDataNexusRepository']
base_data_zip_nexus_path = platform_deploy_schema['baseDataNexusPath']
base_data_repository = platform_deploy_schema['baseDataNexusRepository']
base_data_path = platform_deploy_schema['baseDataNexusPath']
if gls_db_type != 'ORACLE':
    logging.error('current version only support ORACLE database')
    raise Exception('current version only support ORACLE database')
nexus_image_repository = platform_deploy_params['image_repository']


class ExecutableCommand():

    """
    用来执行一条命令的工具类
    """
    @staticmethod
    def timeout_shell_command(command, timeout, cwd=None):
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
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                       close_fds=True)
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

    @staticmethod
    def run_shell_command(command, cwd=None):
        """
        执行一条脚本命令,并返回执行结果输出
        :param command:执行命令,可执行文件/脚本可以是相对cwd的相对路径
        :param cwd:执行命令的cwd
        :return:返回结果
        """
        logging.info("准备执行命令:%s" % command)
        if cwd:
            logging.info('cwd=%s' % cwd)
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=cwd,
                                       close_fds=True)
        else:
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                       close_fds=True)
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

    @staticmethod
    def command_result_regex_match(command_result, regex, command_type='bash/shell'):
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

    @staticmethod
    def get_md5_value(input_str):
        md5 = hashlib.md5()
        md5.update(input_str)
        md5_digest = md5.hexdigest()
        return md5_digest

    @staticmethod
    def del_file(path):
        ls = os.listdir(path)
        for i in ls:
            c_path = os.path.join(path, i)
            if os.path.isdir(c_path):
                ExecutableCommand.del_file(c_path)
            else:
                os.remove(c_path)


class NexusRepository(object):
    """
    用来封装nexus操作的类
    """

    def __init__(self, nexus_host_url, nexus_user, nexus_user_pwd):
        """
        nexus初始化函数
        :param nexus_host_url: nexus的url
        :param nexus_user: nexus的登录用户
        :param nexus_user_pwd: nexus登录用户密码
        """
        self.nexus_host_url = nexus_host_url
        self.nexus_user = nexus_user
        self.nexus_user_pwd = nexus_user_pwd

    def query_asset(self, asset_id):
        """
        查询指定id的nexus asset
        :param asset_id: 需要查询的nexus asset的id
        :return: 查询结果的字典，如果返回http请求返回非200，则抛出错误码
        """
        url = "%s/service/rest/v1/assets/%s" % (self.nexus_host_url, asset_id)
        a = HTTPBasicAuth(self.nexus_user, self.nexus_user_pwd)
        response = requests.get(url=url, auth=a)
        if response.status_code != 200:
            logging.error('query %s fail : error_code=%d' % (self.nexus_host_url, response.status_code))
            raise Exception('http get return error code %d' % response.status_code)
        logging.debug(response.text)
        data = json.loads(response.text)
        return data

    def query_group(self, repository, group):
        """
        从nexus的指定仓库查询特定group的存储信息
        :param repository: 仓库名
        :param group: 需要查询的group
        :return: 查询结果,如果返回码非200，直接抛出错误返回码异常信息
        """
        url = "%s/service/rest/v1/search?repository=%s&group=%s" % (self.nexus_host_url, repository, group)
        a = HTTPBasicAuth(self.nexus_user, self.nexus_user_pwd)
        response = requests.get(url=url, auth=a)
        if response.status_code != 200:
            logging.error('query %s fail : error_code=%d' % (self.nexus_host_url, response.status_code))
            raise Exception('http get return error code %d' % response.status_code)
        logging.debug(json.dumps(response.text))
        print(response.text)
        data = json.loads(response.text)
        items = data['items']
        return items

    def download_file(self, repository, file_nexus_path, save_path):
        file_download_url = "%s/repository/%s/%s" % (self.nexus_host_url, repository, file_nexus_path)
        file_name = file_nexus_path.split('/')[-1]
        logging.debug('begin to download %s from %s and save to %s' % (file_name, file_download_url, save_path))
        req = urllib2.Request(file_download_url)
        basic_auth = 'Basic %s' % (base64.b64encode('%s:%s' % (nexus_user, nexus_user_pwd)))
        req.add_header("Authorization", basic_auth)
        f = urllib2.urlopen(req)
        data = f.read()
        with open(save_path, "w") as code:
            code.write(data)
        logging.debug('download %s from %s and save to %s success' % (file_name, file_download_url, save_path))

    def app_nexus_file_check(self, app_nexus_file):
        asset_id = app_nexus_file['nexusAssetId']
        file_name = app_nexus_file['fileName']
        repository = app_nexus_file['nexusRepository']
        md5 = app_nexus_file['md5']
        logging.error(json.dumps(app_nexus_file, ensure_ascii=False))
        if 'nexusPath' in app_nexus_file.keys():
            print("0" + app_nexus_file['nexusPath'])
            path = app_nexus_file['nexusPath']
        else:
            print("1:" + app_nexus_file['nexusFileSavePath'])
            path = app_nexus_file['nexusFileSavePath']
        print(path)
        path = re.sub('^/+', '', path)
        print(path)
        logging.debug('begin to check id=%s,name=%s,repository=%s,path=%s,md5=%s file' %
                      (asset_id, file_name, repository, path, md5))
        ret_msg = None
        if file_name != path.split('/')[-1]:
            return 'want file name %s not %s' % (file, path.split('/')[-1])
        else:
            asset_info = self.query_asset(asset_id)
            if not asset_info:
                ret_msg = 'id=%s[%s] not exist' % (app_nexus_file['nexusAssetId'], file_name)
            elif repository != asset_info['repository']:
                ret_msg = 'want %s repository is %s not %s' % (file_name, repository, asset_info['repository'])
            elif path != asset_info['path']:
                ret_msg = 'want %s nexus path is %s not %s' % (file_name, path, asset_info['path'])
            elif md5 != asset_info['checksum']['md5']:
                ret_msg = 'want %s md5 is %s not %s' % (file_name, md5, asset_info['checksum']['md5'])
        logging.debug('check result %s' % ret_msg)
        return ret_msg


class ImageRepository(object):
    """
    用来封装和应用image相关操作的类
    """

    def __init__(self, nexus_url, nexus_user, nexus_user_pwd, image_repository, app_nexus_repository):
        """
        应用镜像类的构造函数
        :param nexus_url: 镜像nexus仓库的url
        :param nexus_user: nexus的登录用户名
        :param nexus_user_pwd: nexus的登录密码
        :param image_repository: 镜像的仓库名
        :param app_nexus_repository: 存储应用包的nexus仓库
        """
        self.nexus_url = nexus_url
        self.nexus_user = nexus_user
        self.nexus_user_pwd = nexus_user_pwd
        self.image_repository = image_repository
        self.work_dir = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/buildings'
        self.app_nexus_repository = app_nexus_repository
        if os.path.exists(self.work_dir) and not os.path.isdir(self.work_dir):
            logging.error('%s exist and not directory' % self.work_dir)
            raise Exception('%s exist and not directory' % self.work_dir)
        elif not os.path.exists(self.work_dir):
            exec_command = 'mkdir %s -p' % self.work_dir
            ExecutableCommand.run_shell_command(exec_command, None)

    def exist_app_image_at_nexus(self, app_module):
        """
        检查nexus仓库是否有指定应用模块的镜像
        :param app_module: 应用模块
        :return: 存在返回TRUE,不存在返回FALSE,如果http请求返回非200和404则抛异常
        """
        app_name = app_module['appName'].lower()
        version = re.sub(':', '-', app_module['version'])
        query_url = "http://%s/v2/%s/%s/tags/list" % (self.nexus_url, self.image_repository, app_name)
        a = HTTPBasicAuth(nexus_user, nexus_user_pwd)
        logging.debug('begin to query %s' % query_url)
        response = requests.get(url=query_url, auth=a)
        status_code = response.status_code
        text = response.text
        logging.debug('status_code=%s : %s' % (status_code, text))
        if status_code == 404:
            logging.debug('query %s return 404, repository %s or image %s[%s] not exist'
                          % (query_url, image_repository, app_name, version))
            return False
        elif status_code != 200:
            logging.error('query %s return error code %d' % (query_url, status_code))
            raise Exception('query %s return error code %d' % (query_url, status_code))
        image_exist = False
        if "\"%s\"" % version in response.text:
            image_exist = True
        logging.debug('%s[%s] image exist at nexus : %s' % (app_name, version, image_exist))
        return image_exist

    def exist_app_image_at_server(self, app_module):
        """
        检查服务器上是否已经有指定应用模块的镜像
        :param app_module: 应用模块
        :return: 检查结果TRUE有，FALSE没有
        """
        app_name = app_module['appName']
        version = app_module['version']
        logging.debug('confirm %s(%s) image exist at server', app_name, version)
        exec_command = "docker image ls| grep %s | grep %s | awk '{print $3}'" \
                       % (app_name.lower(), re.sub(':', '-', version))
        image_exist = False
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        if exec_result:
            image_exist = True
        logging.debug('%s(%s) image exist at server : %s' %
                      (app_name, version, image_exist))
        return image_exist

    def generate_docker_file(self, app_module):
        """
        为指定的应用模块生成DockerFile
        :param app_module: 需要生成DockerFile的应用模块
        :return: 生成的DockerFile的存放绝对路径
        """
        package_type = app_module['appType']
        package_file_name = app_module['installPackage']['fileName']
        save_dir = self.__get_app_work_dir(app_module)
        save_file_path = '%s/%s' % (save_dir, 'Dockerfile')
        print(save_file_path)
        with open(save_file_path, 'w') as out_f:
            if package_type == 'CCOD_KERNEL_MODULE':
                out_f.write("FROM harbor.io:1180/ccod-base/centos-backend:0.4\n")
            else:
                out_f.write("FROM nexus.io:5000/ccod-base/alpine-java:jdk8-slim\n")
            out_f.write("ADD %s /opt/%s\n" % (package_file_name, package_file_name))
            if package_type == 'CCOD_KERNEL_MODULE':
                out_f.write('RUN chmod a+x /opt/%s\n' % package_file_name)
                out_f.write('CMD ["/bin/bash", "-c", "ldd /opt/%s;/opt/%s"]"]\n'
                            % (package_file_name, package_file_name))
        return save_file_path

    def build_image(self, app_module):
        self.generate_docker_file(app_module)
        app_name = app_module['appName']
        version = app_module['version']
        app_dir = self.__get_app_work_dir(app_module)
        image_tag = '%s:%s' % (app_name.lower(), re.sub(':', '-', version))
        image_uri = '%s/%s/%s' % (self.nexus_url, self.image_repository, image_tag)
        exec_command = 'cd %s;docker build -t %s .' % (app_dir, image_uri)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        exec_command = 'cd %s;docker push %s' % (app_dir, image_uri)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        return True

    def __get_app_work_dir(self, app_module):
        """
        获取应用模块的工作目录
        :param app_module: 应用模块
        :return: 应用模块的工作目录
        """
        app_name = app_module['appName']
        version = app_module['version']
        app_dir = '%s/%s/%s' % (self.work_dir, app_name, version)
        return app_dir

    def prepare_image(self, app_module):
        image_tag = '%s[%s]' % (app_module['appName'], app_module['version'])
        if self.exist_app_image_at_nexus(app_module):
            logging.debug('%s exist at nexus repository' % image_tag)
            return True
        logging.debug('%s image not exist at image repository, build it fist' % image_tag)
        success = self.build_image(app_module)
        logging.debug('build %s image : %s' % success)
        return success


class CMDB(object):
    """
    用来封装应用模块相关的操作
    """
    def __init__(self, cmdb_url):
        self.cmdb_url = cmdb_url

    def query(self, app_name, version):
        query_url = '%s/api/apps/%s/%s' % (self.cmdb_url, app_name, version)
        response = requests.get(query_url)
        status_code = response.status_code
        text = response.text
        logging.debug('query %s return status_coude %d : %s' % (query_url, status_code, text))
        if status_code != 200:
            logging.error('query %s return error code %d' % status_code)
            raise Exception('query %s return error code %d : %s' % (query_url, status_code, text))
        app_module = None
        ajax_result = json.loads(text)
        if ajax_result['success'] is True:
            app_module = ajax_result['rows']
        else:
            logging.error('query %s return error ： %s' % (query_url, ajax_result['msg']))
        return app_module


class AppInstance(object):
    def __init__(self, platform_id, domain_id, app_module, alias, operation, cfgs=None, timeout=0, src_revision='2',
                 target_revision='1', task_type='CREATE', public_config=None):
        self.operation = operation
        # if self.operation == 'ADD':
        #     self.operation = 'DELETE'
        self.platform_id = platform_id
        self.task_type = task_type
        self.domain_id = domain_id
        self.app_name = app_module['appName']
        if self.operation == 'ADD' or self.operation == 'UPDATE':
            self.version = app_module['version']
            self.install_package = app_module['installPackage']
            self.app_type = app_module['appType']
        else:
            self.version = None
            self.install_package = None
            self.app_type = None
        self.alias = alias
        self.service_name = '%s-%s' % (alias, domain_id)
        self.cfgs = cfgs
        self.deploy_time = None
        self.time_usage = 0
        self.timeout = timeout
        if not timeout:
            self.timeout = 30
        self.command_exec_result = None
        self.pod_name = None
        self.status = None
        self.pod_ready = None
        self.pod_restarts = None
        self.pod_age = None
        self.node_port = None
        self.current_revision = None
        self.src_revision = src_revision
        self.target_revision = target_revision
        self.update_status = None
        self.update_time = None
        self.log = None
        self.public_config = public_config
        self.__get_current_status()
        exec_command = None
        opt_tag = '%s %s[%s(%s)] at %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
        if self.operation == 'ADD':
            opt_tag = '%s %s[%s(%s)] to %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
            if not self.cfgs:
                logging.error('%s fail : cfgs is blank' % opt_tag)
                raise Exception('%s fail : cfgs is blank' % opt_tag)
            elif self.pod_name:
                logging.error('%s fail : %s exist at %s with pod_name %s' % (opt_tag, self.alias, self.domain_id, self.pod_name))
                raise Exception('%s fail : %s exist at %s with pod_name %s' % (opt_tag, self.alias, self.domain_id, self.pod_name))
            cfg_params = self.__get_app_cfg_params_for_k8s()
            exec_command = '/usr/local/bin/helm install --set module.vsersion=%s --set module.name=%s --set module.alias=%s-%s %s %s-%s . -n %s' % (
                re.sub(':', '-', self.version), self.app_name, self.alias, self.domain_id, cfg_params, self.alias, self.domain_id,
                self.platform_id
            )
        elif self.operation == 'UPDATE':
            opt_tag = '%s %s[%s(%s)] at %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
            if not cfgs:
                logging.error('%s fail : cfgs is blank' % opt_tag)
                raise Exception('%s fail : cfgs is blank' % opt_tag)
            if not self.pod_name:
                logging.error('%s fail : target app not exist' % opt_tag)
                raise Exception('%s fail : target app not exist' % opt_tag)
            cfg_params = self.__get_app_cfg_params_for_k8s()
            exec_command = '/usr/local/bin/helm upgrade --set module.vsersion=%s --set module.name=%s --set module.alias=%s-%s %s %s-%s . -n %s' % (
                re.sub(':', '-', self.version), self.app_name, self.alias, self.domain_id, cfg_params, self.alias, self.domain_id,
                self.platform_id
            )
        elif self.operation == 'DELETE':
            opt_tag = '%s %s[%s(%s)] from %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
            if not self.pod_name:
                logging.error('%s fail : target app not exist' % opt_tag)
                raise Exception('%s fail : target app not exist' % opt_tag)
            exec_command = '/usr/local/bin/helm delete -n %s %s-%s' % (platform_id, alias, domain_id)
        elif self.operation == 'ROLLBACK':
            opt_tag = '%s %s[%s(%s)] at %s from %s to %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id, self.current_revision, self.target_revision)
            if not self.pod_name:
                logging.error('%s fail : target app not exist' % opt_tag)
                raise Exception('%s fail : target app not exist' % opt_tag)
            elif not self.target_revision:
                logging.error('%s fail : target revision is blank' % opt_tag)
                raise Exception('%s fail : target revision is blank' % opt_tag)
            elif self.current_revision == self.target_revision:
                logging.error('%s fail : can not rollback to same revision %s' % (opt_tag, self.current_revision))
                raise Exception('%s fail : can not rollback to same revision %s' % (opt_tag, self.current_revision))
            last_update_status = PlatformManager.get_app_last_update_status(self.platform_id, self.domain_id, self.alias)
            if not last_update_status:
                logging.error('%s fail :can not get update status' % opt_tag)
                raise Exception('%s fail :can not get update status' % opt_tag)
            if last_update_status['revision'] == self.target_revision:
                logging.error('%s fail : can not rollback to same revision %s' % (opt_tag, self.target_revision))
                raise Exception('%s fail : can not rollback to same revision %s' % (opt_tag, self.target_revision))
            history = PlatformManager.get_app_update_history(self.platform_id, self.domain_id, self.alias)
            history_dict = dict()
            for update_status in history:
                history_dict[update_status['revision']] = update_status
            print(history_dict.keys())
            if self.target_revision not in history_dict.keys():
                logging.error('%s fail : %s is not a real revision' % (opt_tag, self.target_revision))
                raise Exception('%s fail : %s is not a real revision' % (opt_tag, self.target_revision))
        elif self.operation == 'RESTART':
            opt_tag = '%s %s[%s(%s)] at %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
            if not self.pod_name:
                logging.error('%s fail : target app not exist' % opt_tag)
                raise Exception('%s fail : target app not exist' % opt_tag)
            exec_command = 'kubectl delete pod -n %s %s' % (self.platform_id, self.pod_name)
        else:
            logging.error('%s fail : not support operation %s' % (opt_tag, operation))
            raise Exception('%s fail : not support operation %s' % (opt_tag, operation))
        self.exec_command = exec_command

    def __get_current_status(self):
        self.pod_name = None
        self.status = None
        self.pod_ready = None
        self.pod_restarts = None
        self.pod_age = None
        self.update_status = None
        self.update_revision = None
        self.update_time = None
        self.node_port = None
        pod_status = PlatformManager.get_app_pod_status(self.platform_id, self.domain_id, self.alias)
        if pod_status:
            self.pod_name = pod_status['name']
            self.status = pod_status['status']
            self.pod_ready = pod_status['ready']
            self.pod_restarts = pod_status['restarts']
            self.pod_age = pod_status['age']
            update_status = PlatformManager.get_app_last_update_status(self.platform_id, self.domain_id, self.alias)
            if update_status:
                self.current_revision = update_status['revision']
                self.update_status = update_status['status']
                self.update_time = update_status['time']
            service_name = '%s-%s' % (self.alias, self.domain_id)
            self.node_port = PlatformManager.get_service_node_port(self.platform_id, service_name)

    def __add(self, work_dir):
        opt_tag = '%s %s[%s(%s)] to %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
        if not self.cfgs:
            logging.error('%s fail : cfgs is blank' % opt_tag)
            raise Exception('%s fail : cfgs is blank' % opt_tag)
        elif self.pod_name:
            logging.error(
                '%s fail : %s exist at %s with pod_name %s' % (opt_tag, self.alias, self.domain_id, self.pod_name))
            raise Exception(
                '%s fail : %s exist at %s with pod_name %s' % (opt_tag, self.alias, self.domain_id, self.pod_name))
        start = datetime.datetime.now()
        print(self.exec_command)
        if self.app_type == 'CCOD_WEBAPPS_MODULE':
            if self.app_name == 'cas':
                cwd = '%s/payaml/tomcat6-jre7/' % work_dir
            else:
                cwd = '%s/payaml/resin-4.0.13_jre-1.6.0_21/' % work_dir
        else:
            cwd = '%s/payaml/%s' % (work_dir, self.app_name)
        exec_result = ExecutableCommand.run_shell_command(self.exec_command, cwd=cwd)
        self.command_exec_result = exec_result
        self.time_usage = (datetime.datetime.now() - start).seconds
        print(exec_result)
        success = PlatformManager.is_app_running(self.platform_id, self.domain_id, self.alias, start=start, timeout=self.timeout)
        logging.debug('%s %s' % (opt_tag, success))
        self.__get_current_status()
        return success

    def __update(self, work_dir):
        opt_tag = '%s %s[%s(%s)] to %s' % (self.operation, self.alias, self.app_name, self.version, self.domain_id)
        if not self.cfgs:
            logging.error('%s fail : cfgs is blank' % opt_tag)
            raise Exception('%s fail : cfgs is blank' % opt_tag)
        elif not self.pod_name:
            logging.error(
                '%s fail : %s at %s not exist' % (opt_tag, self.alias, self.domain_id))
            raise Exception(
                '%s fail : %s at %s not exist' % (opt_tag, self.alias, self.domain_id))
        start = datetime.datetime.now()
        print(self.exec_command)
        if self.app_type == 'CCOD_WEBAPPS_MODULE':
            if self.app_name == 'cas':
                cwd = '%s/payaml/tomcat6-jre7/' % work_dir
            else:
                cwd = '%s/payaml/resin-4.0.13_jre-1.6.0_21/' % work_dir
        else:
            cwd = '%s/payaml/%s' % (work_dir, self.app_name)
        exec_result = ExecutableCommand.run_shell_command(self.exec_command, cwd=cwd)
        self.command_exec_result = exec_result
        self.time_usage = (datetime.datetime.now() - start).seconds
        print(exec_result)
        success = PlatformManager.is_app_running(self.platform_id, self.domain_id, self.alias, start=start,
                                                 timeout=self.timeout)
        logging.debug('%s %s' % (opt_tag, success))
        self.__get_current_status()
        return success

    def __delete(self):
        success, self.exec_command = PlatformManager.delete_app(self.platform_id, self.domain_id, self.alias)
        return success

    def __rollback(self):
        success, exec_command = PlatformManager.rollback_app(self.platform_id, self.domain_id, self.alias, self.target_revision)
        self.exec_command = exec_command
        return success

    def __restart(self):
        opt_tag = '%s %s[%s] at %s' % (self.operation, self.alias, self.app_name, self.domain_id)
        logging.debug(opt_tag)
        success = PlatformManager.restart_app(self.platform_id, self.domain_id, self.alias, self.timeout)
        logging.debug('%s : %s' % (opt_tag, success))
        return success

    def restart_app(self, pod_name):
        logging.debug('begin to restart %s at %s' % (self.platform_id, pod_name))
        exec_command = """kubectl get pod -n %s | grep -E '^%s$' | awk '{print $1}'""" \
                       % (self.platform_id, pod_name)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        if not exec_result:
            logging.error('not find %s at %s' % (pod_name, self.platform_id))
            raise Exception('not find %s at %s' % (pod_name, self.platform_id))
        if len(exec_result.split('\n')) > 1:
            logging.error('%s in %s not unique' % (pod_name, self.platform_id))
            raise Exception('%s in %s not unique' % (pod_name, self.platform_id))
        print(exec_result)
        exec_command = 'kubectl delete pod -n %s %s' % (self.platform_id, pod_name)
        print(exec_command)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.info('restart %s in %s success' % (pod_name, self.platform_id))
        return True

    def __get_app_cfg_params_for_k8s(self):
        cfgs = self.cfgs
        cfg_params = ""
        if self.app_type == 'CCOD_WEBAPPS_MODULE' and self.app_name != 'cas':
            cfg_params = "--set publicConfig.[%s]=[%s] --set isPublicConfig=False --set publicConfig.local_datasource.xml=/root/resin-4.0.13/conf --set publicConfig.local_jvm.xml=/root/resin-4.0.13/conf" % (platform_public_config['fileName'], platform_public_config['deployPath'])
        elif (self.app_name == 'UCGateway' or self.app_name == 'AppGateWay' or self.app_name == 'DialEngine') and self.public_config:
            cfg_params = "--set publicConfig.[%s]=[%s] --set isPublicConfig=False --set config.sdcommon.ini=./Config" % (self.public_config['fileName'], self.public_config['deployPath'])
        for cfg in cfgs:
            cfg_deploy_path = re.sub('^.*WEB-INF/', 'WEB-INF/', cfg['deployPath'])
            cfg_deploy_path = re.sub('/$', '', cfg_deploy_path)
            cfg_file_name = re.sub('\\.', '\\\\\\.', cfg['fileName'])
            cfg_params = '%s --set config.%s=%s' % (cfg_params, cfg_file_name, cfg_deploy_path)
        cfg_repository = cfgs[0]['nexusRepository']
        str_info = re.compile('^/')
        cfg_directory = str_info.sub('', cfgs[0]['nexusPath'])
        str_info = re.compile('/[^/]+$')
        cfg_directory = str_info.sub('', cfg_directory)
        cfg_uri = '%s/repository/%s/%s' % (
            nexus_host_url, cfg_repository, cfg_directory)
        return '%s --set runtime.configPath=%s' % (cfg_params, cfg_uri)

    def execute(self, work_dir):
        self.__get_current_status()
        if self.operation == 'ADD':
            success = self.__add(work_dir)
        elif self.operation == 'DELETE':
            success = self.__delete()
        elif self.operation == 'UPDATE':
            success = self.__update(work_dir)
        elif self.operation == 'ROLLBACK':
            success = self.__rollback()
        else:
            success = self.__restart()
        return success


class PlatformManager(object):
    """
    用来封装平台管理相关功能的类
    核心功能:平台创建、更新和删除
    """

    @staticmethod
    def add_new_platform(platform_id, work_dir):
        logging.debug('add new namespace %s to k8s' % platform_id)
        if PlatformManager.is_platform_exist(platform_id):
            logging.error('add new namespace %s to k8s fail : %s exist' % (platform_id, platform_id))
            raise Exception('add new namespace %s to k8s fail : %s exist' % (platform_id, platform_id))
        exec_command = 'kubectl create namespace %s' % platform_id
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.debug('create base service for %s' % platform_id)
        print('create base service for %s' % platform_id)
        db_param = '--set oracle.ccod.name=oracle --set oracle.ccod.sid=xe --set runtime.network.domainName=ccod.io --set oracle.ccod.user=ccod --set oracle.ccod.passwd=ccod --set mysql.ccod.user=ucds  --set mysql.ccod.passwd=ucds --set isPublicConfig=False'
        exec_command = 'cd %s;cd payaml/baseService;/usr/local/bin/helm install %s -n %s baseservice .' % (
            work_dir, db_param, platform_id)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.debug('create cas cert for %s' % platform_id)
        print('create cas cert for %s' % platform_id)
        exec_command = """cd %s;kubectl get -n kube-system secret ssl -o yaml | grep -vE '(creationTimestamp|resourceVersion|selfLink|uid)'|sed "s/kube-system/%s/g" > ssl.yaml""" % (
            work_dir, platform_id
        )
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        exec_command = 'cd %s;kubectl apply -f ssl.yaml' % work_dir
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)

    @staticmethod
    def get_app_pod_status(platform_id, domain_id, app_alias):
        if not PlatformManager.is_platform_exist(platform_id):
            return None
        exec_command = "kubectl get pod -n %s | grep -E '^%s-%s-'" % (platform_id, app_alias, domain_id)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        pod_status = None
        if exec_result:
            if len(exec_result.split('\n')) > 1:
                logging.error('get pod name of %s at %s fail : %s-%s not unque \n : %s'
                              % (app_alias, domain_id, app_alias, domain_id, exec_result))
                raise Exception('get pod name of %s at %s fail : %s-%s not unque \n : %s'
                                % (app_alias, domain_id, app_alias, domain_id, exec_result))
            arr = re.split('\s+', exec_result)
            if len(arr) == 5:
                pod_status = dict()
                pod_status['name'] = arr[0]
                pod_status['ready'] = arr[1]
                pod_status['status'] = arr[2]
                pod_status['restarts'] = arr[3]
                pod_status['age'] = arr[4]
        logging.debug('pod status of %s''s %s is %s' % (domain_id, app_alias, pod_status))
        return pod_status

    @staticmethod
    def get_log(platform_id, domain_id, app_alias):
        pod_name = PlatformManager.get_app_pod_name(platform_id, domain_id, app_alias)
        if not pod_name:
            logging.error('get log of %s at %s fail : %s not exist' % (app_alias, domain_id, app_alias))
            raise Exception('get log of %s at %s fail : %s not exist' % (app_alias, domain_id, app_alias))
        exec_command = 'kubectl logs -n %s %s' % (platform_id, pod_name)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        return exec_result

    @staticmethod
    def get_app_last_update_status(platform_id, domain_id, app_alias):
        exec_command = "helm history -n %s %s-%s" % (platform_id, app_alias, domain_id)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        update_status = None
        if exec_result:
            lines = exec_result.split('\n')
            if re.match('^REVISION\s+UPDATED\s+STATUS\s+', lines[0]):
                arr = re.split('\s+', lines[-1])
                update_status = dict()
                update_status['revision'] = arr[0]
                update_status['time'] = '%s %s %s %s %s' % (arr[1], arr[2], arr[3], arr[4], arr[5])
                update_status['status'] = arr[6]
                regex = '(Install|Upgrade|Rollback)\s+.+$'
                matcher = re.search(regex, lines[-1])
                desc = None
                if matcher:
                    desc = matcher.group()
                update_status['description'] = desc
        logging.debug('update status of %s''s %s is %s' % (domain_id, app_alias, update_status))
        return update_status

    @staticmethod
    def rollback_app(platform_id, domain_id, app_alias, revision):
        opt_tag = 'ROLLBACK %s at %s to %s' % (app_alias, domain_id, revision)
        last_update_status = PlatformManager.get_app_last_update_status(platform_id, domain_id, app_alias)
        if not last_update_status:
            logging.error('%s fail :can not get update status' % opt_tag)
            raise Exception('%s fail :can not get update status' % opt_tag)
        if last_update_status['revision'] == revision:
            logging.error('%s fail : can not rollback to same revision %s' % (opt_tag, revision))
            raise Exception('%s fail : can not rollback to same revision %s' % (opt_tag, revision))
        history = PlatformManager.get_app_update_history(platform_id, domain_id, app_alias)
        history_dict = dict()
        for update_status in history:
            history_dict[update_status['revision']] = update_status
        if revision not in history_dict.keys():
            logging.error('%s fail : %s is not a real revision' % (opt_tag, revision))
            raise Exception('%s fail : %s is not a real revision' % (opt_tag, revision))
        exec_command = 'helm rollback %s-%s %s -n %s' % (app_alias, domain_id, revision, platform_id)
        ExecutableCommand.run_shell_command(exec_command)
        last_update_status = PlatformManager.get_app_last_update_status(platform_id, domain_id, app_alias)
        success = False
        if last_update_status and last_update_status['status'] == 'deployed' and last_update_status['description'] == 'Rollback to %s' % revision:
            success = True
        logging.error('%s : %s' % (opt_tag, success))
        return success, exec_command

    @staticmethod
    def get_app_update_history(platform_id, domain_id, app_alias):
        exec_command = "helm history -n %s %s-%s" % (platform_id, app_alias, domain_id)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        history = list()
        if exec_result and len(exec_result.split('\n')) >= 2:
            lines = exec_result.split('\n')[1:]
            for line in lines:
                arr = re.split('\s+', line)
                update_status = dict()
                update_status['revision'] = arr[0]
                update_status['time'] = '%s %s %s %s %s' % (arr[1], arr[2], arr[3], arr[4], arr[5])
                update_status['status'] = arr[6]
                regex = '(Install|Upgrade|Rollback)\s+.+$'
                matcher = re.search(regex, line)
                desc = None
                if matcher:
                    desc = matcher.group()
                update_status['description'] = desc
                history.append(update_status)
        return history

    @staticmethod
    def is_platform_exist(platform_id):
        logging.debug('check platform %s existed' % platform_id)
        exec_command = "kubectl get namespace | awk '{print $1}' | grep -P \"^%s$\"" % platform_id
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        exist = False
        if exec_result:
            exist = True
        logging.debug('platform %s exist : %s' % (platform_id, exist))
        return exist

    @staticmethod
    def is_app_running(platform_id, domain_id, app_alias, start=None, timeout=0):
        if not start:
            start = datetime.datetime.now()
        running = False
        while True:
            pod_status = PlatformManager.get_app_pod_status(platform_id, domain_id, app_alias)
            time_usage = (datetime.datetime.now() - start).seconds
            if pod_status and pod_status['status'] == 'Running':
                running = True
                break
            elif time_usage >= timeout:
                logging.debug('timeout')
                break
            time.sleep(1)
        logging.debug('%s at %s is running : %s, time_usage=%d' % (app_alias, domain_id, running, time_usage))
        return running

    @staticmethod
    def get_app_pod_name(platform_id, domain_id, app_alias):
        exec_command = "kubectl get pod -n %s | grep -E '^%s-%s-'" % (platform_id, app_alias, domain_id)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        pod_name = None
        if exec_result:
            if len(exec_result.split('\n')) > 1:
                logging.error('get pod name of %s at %s fail : %s-%s not unque %s\n : %s'
                              % (app_alias, domain_id, app_alias, domain_id, exec_result))
                raise Exception('get pod name of %s at %s fail : %s-%s not unque %s\n : %s'
                                % (app_alias, domain_id, app_alias, domain_id, exec_result))
            arr = re.split('\s+', exec_result)
            if len(arr) == 5:
                pod_name = arr[0]
        logging.debug('pod name of %s at %s is %s' % (app_alias, domain_id, pod_name))
        return pod_name

    @staticmethod
    def delete_platform(platform_id):
        logging.debug('begin to delete platform %s' % platform_id)
        if not PlatformManager.is_platform_exist(platform_id):
            logging.error('delete %s fail : platform %s not exist' % (platform_id, platform_id))
            raise Exception('delete %s fail : platform %s not exist' % (platform_id, platform_id))
        exec_command = 'kubectl delete namespace %s' % platform_id
        ret_command = exec_command
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        exec_command = 'kubectl delete pv base-volume-%s' % platform_id
        ret_command = '%s;%s' % (ret_command, exec_command)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.debug('namespace %s deleted' % platform_id)
        success = PlatformManager.is_platform_exist(platform_id)
        logging.debug('delete platform %s : %s' % (platform_id, not success))
        return not success, ret_command

    @staticmethod
    def delete_app(platform_id, domain_id, app_alias, timeout=10):
        opt_tag = 'DELETE %s from %s' % (app_alias, domain_id)
        logging.debug('begin to %s' % opt_tag)
        pod_name = PlatformManager.get_app_pod_name(platform_id,domain_id, app_alias)
        if not pod_name:
            logging.error('%s fail : app not exist' % opt_tag)
            raise Exception('%s fail : app not exist' % opt_tag)
        exec_command = 'helm delete %s-%s -n %s' % (app_alias, domain_id, platform_id)
        ExecutableCommand.run_shell_command(exec_command)
        start = datetime.datetime.now()
        while (datetime.datetime.now() - start).seconds <= timeout:
            time.sleep(1)
            pod_name = PlatformManager.get_app_pod_name(platform_id, domain_id, app_alias)
            if not pod_name:
                break
        logging.debug('%s : %s' % (opt_tag, not pod_name))
        return not pod_name, exec_command

    @staticmethod
    def restart_app(platform_id, domain_id, app_alias, timeout=0):
        logging.debug('begin to restart %s at %s' % (domain_id, app_alias))
        pod_status = PlatformManager.get_app_pod_status(platform_id, domain_id, app_alias)
        if not pod_status:
            logging.error('restart %s at %s fail : %s or %s not exist' % (app_alias, domain_id, domain_id, app_alias))
            raise Exception('restart %s at %s fail : %s or %s not exist' % (app_alias, domain_id, domain_id, app_alias))
        exec_command = 'kubectl delete pod -n %s %s' % (platform_id, pod_status['name'])
        start = datetime.datetime.now()
        print(exec_command)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.debug('restart %s at %s finish' % (app_alias, domain_id))
        success = PlatformManager.is_app_running(platform_id, domain_id, app_alias, start, timeout)
        logging.debug('restart %s at %s : %s' % (app_alias, domain_id, success))
        return success

    @staticmethod
    def restart_all(platform_id, app_standard_alias, timeout=0):
        exec_command = """kubectl get pod -n %s | grep -E '^%s-[a-z]+[0-9]+-' | awk '{print $1}'""" \
                       % (platform_id, app_standard_alias)
        exec_result = ExecutableCommand.run_shell_command(exec_command)
        if not exec_result:
            logging.error('restart all %s fail : can not find %s in platform %s'
                          % (app_standard_alias, app_standard_alias, platform_id))
            raise Exception('restart all %s fail : can not find %s in platform %s'
                            % (app_standard_alias, app_standard_alias, platform_id))
        pod_name_list = exec_result.split('\n')
        logging.debug('%s has %d %s : %s' % (platform_id, len(pod_name_list), app_standard_alias, exec_result))
        for pod_name in pod_name_list:
            app_alias = pod_name.split('-')[0]
            domain_id = pod_name.split('-')[1]
            success = PlatformManager.restart_app(platform_id, domain_id, app_alias, timeout)
            logging.debug('restart pod %s : %s' % (pod_name, success))
            if not success:
                logging.error('restart all %s fail : restart pod %s fail' % (app_standard_alias, pod_name))
                return False
        return True

    @staticmethod
    def get_service_node_port(platform_id, service_name):
        exec_command = "kubectl get svc -n %s | grep -E '^%s\s+' | awk '{print $5}' | awk -F ':' '{print $2}' | awk -F '/' '{print $1}'" \
                       % (platform_id, service_name)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        node_port = None
        if exec_result and exec_result.isdigit():
            node_port = int(exec_result)
        logging.debug('nodePort of %s''s %s is %s' % (platform_id, service_name, node_port))
        return node_port


class CCODPlatform(object):
    """
    用来封装平台管理相关功能的类
    核心功能:平台创建、更新和删除
    """

    def __init__(self, platform_id, cmdb, k8s_deploy_git_url, k8s_host_ip, nexus_repository, image_repository,
                 gls_db, base_data_nexus_repository, base_data_zip_nexus_path, gls_standard_alias='glsserver', dcs_standard_alias='dcs'):
        self.platform_id = platform_id
        self.cmdb = cmdb
        self.k8s_deploy_git_url = k8s_deploy_git_url
        self.k8s_host_ip = k8s_host_ip
        self.nexus_repository = nexus_repository
        self.image_repository = image_repository
        self.nexus_image_repository = nexus_image_repository
        self.gls_db = gls_db
        self.base_data_nexus_repository = base_data_nexus_repository
        self.base_data_zip_nexus_path = base_data_zip_nexus_path
        self.gls_standard_alias = gls_standard_alias
        self.dcs_standard_alias = dcs_standard_alias
        work_dir = '/tmp/%s' % ExecutableCommand.get_md5_value('%s%s' % (self.platform_id, datetime.datetime.now()))
        exec_command = 'mkdir %s -p' % work_dir
        ExecutableCommand.run_shell_command(exec_command)
        self.work_dir = work_dir
        exec_command = 'cd %s;git clone %s' % (work_dir, self.k8s_deploy_git_url)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)

    def generate_platform_base_data(self):
        base_data_unzip_dir = '/home/kubernetes/volume/%s' % self.platform_id
        logging.debug('begin to check data base dir %s' % base_data_unzip_dir)
        if os.path.exists(base_data_unzip_dir) and not os.path.isdir(base_data_unzip_dir):
            logging.error('%s exist and not directory' % base_data_unzip_dir)
            raise Exception('%s exist and not directory' % base_data_unzip_dir)
        elif os.path.exists(base_data_unzip_dir):
            logging.debug('%s not empty, clean it first', base_data_unzip_dir)
            ExecutableCommand.del_file(base_data_unzip_dir)
        else:
            logging.debug('%s not exist, create it fist' % base_data_unzip_dir)
            os.makedirs(base_data_unzip_dir)
        file_name = self.base_data_zip_nexus_path.split('/')[-1]
        arr = file_name.split('.')
        if len(arr) == 1 or arr[-1] != 'zip':
            logging.error('%s is not a legal base data zip package name' % file_name)
            raise Exception('%s is not a legal base data zip package name' % file_name)
        save_path = '%s/%s' % (base_data_unzip_dir, file_name)
        logging.debug('begin to download base data zip package %s' % save_path)
        self.nexus_repository.download_file(self.base_data_nexus_repository, self.base_data_zip_nexus_path, save_path)
        logging.debug('base data zip package download finish')
        logging.debug('begin to unzip base data zip package')
        exec_command = 'cd %s;unzip %s' % (base_data_unzip_dir, file_name)
        exec_result = ExecutableCommand.run_shell_command(exec_command, None)
        print(exec_result)
        logging.debug('base data zip unzip finish')

    def __update_ucds_data(self, ucds_alias, domain_id, ucds_port):
        update_sql = """update "%s"."%s" set PARAM_UCDS_PORT='%d' where NAME='%s-%s'""" \
                     % (gls_db_name, gls_service_unit_table, ucds_port, ucds_alias, domain_id)
        logging.info('update ucds sql=%s' % update_sql)
        self.gls_db.update(update_sql)
        logging.debug('success update %s at %s data in %s' % (ucds_alias, domain_id, self.gls_db.service_name))
        return True

    def __package_and_cfg_check(self, app_module, cfgs):
        inst_pkg = app_module['installPackage']
        check_result = self.nexus_repository.app_nexus_file_check(inst_pkg)
        if check_result:
            return check_result
        if not cfgs:
            src_dict = dict()
            dst_dict = dict()
            for cfg in app_module['cfgs']:
                src_dict[cfg['fileName']] = cfg
            for cfg in cfgs:
                dst_dict[cfg['fileName']] = cfg
            if len(src_dict) != len(dst_dict):
                return 'want %d cfgs but offer %d cfgs' % (len(src_dict), len(dst_dict))
            for file_name in src_dict.keys():
                if file_name not in dst_dict.keys():
                    return 'not find %s in cfgs' % file_name
            for cfg in cfgs:
                check_result = self.nexus_repository.app_nexus_file_check(cfg)
                if not check_result:
                    return check_result
            return None

    def create(self, domain_list, force=False):
        start = datetime.datetime.now()
        ret_list = list()
        if PlatformManager.is_platform_exist(self.platform_id):
            if not force:
                logging.error('create %s fail : %s exist' % (self.platform_id, self.platform_id))
                raise Exception('create %s fail : %s exist' % (self.platform_id, self.platform_id))
            PlatformManager.delete_platform(self.platform_id)
        deploy_apps = list()
        for domain in domain_list:
            domain_id = domain['domainId']
            domain_public_config = None
            if 'publicConfig' in domain.keys:
                domain_public_config = domain['publicConfig']
            if domain['updateType'] != 'ADD':
                logging.error('add domain %s fail : updateType must be ADD, not %s' % (domain_id, domain['updateType']))
                raise Exception('add domain %s fail : updateType must be ADD, not %s' % (domain_id, domain['updateType']))
            for app in domain['appUpdateOperationList']:
                operation = app['operation']
                app_name = app['appName']
                version = app['targetVersion']
                alias = app['appAlias']
                cfgs = app['cfgs']
                app_tag = 'ADD %s[%s(%s)] to %s ' % (alias, app_name, version, domain_id)
                if operation != 'ADD':
                    logging.error('%s : operation  must be ADD, not %s' % (app_tag, operation))
                    raise Exception('%s : operation  must be ADD, not %s' % (app_tag, operation))
                app_module = self.cmdb.query(app_name, version)
                if not app_module:
                    logging.error('%s fail : %s[%s] not exist' % (app_tag, app_name, version))
                    raise Exception('%s fail : %s[%s] not exist' % (app_tag, app_name, version))
                check_result = self.__package_and_cfg_check(app_module, cfgs)
                if check_result:
                    logging.error('%s fail : %s' % (app_tag, check_result))
                    raise Exception('%s fail : %s' % (app_tag, check_result))
                if not self.image_repository.prepare_image(app_module):
                    logging('%s fail : prepare image fail' % app_tag)
                    raise Exception('%s fail : prepare image fail' % app_tag)
                app_ins = AppInstance(self.platform_id, domain_id, app_module, alias, operation, app['cfgs'], app['addDelay'], public_config=domain_public_config)
                deploy_apps.append(app_ins)
        self.generate_platform_base_data()
        PlatformManager.add_new_platform(self.platform_id, self.work_dir)
        db_port = PlatformManager.get_service_node_port(self.platform_id, self.gls_db.service_name)
        if not db_port:
            logging.error('create %s fail : can not get node port of database %s' % (self.platform_id, self.gls_db.service_name))
            raise Exception('create %s fail : can not get node port of database %s' % (self.platform_id, self.gls_db.service_name))
        self.gls_db.conn(db_port)
        for deploy_app in deploy_apps:
            success = deploy_app.execute(self.work_dir)
            ret_list.append(deploy_app.__dict__)
            if not success:
                logging.error('create platform %s fail : ADD %s to %s fail'
                              % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                print('create platform %s fail : ADD %s to %s fail'
                      % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                return False, ret_list
            if deploy_app.app_name == 'UCDServer':
                ucds_port = PlatformManager.get_service_node_port(self.platform_id, '%s-%s-out' % (deploy_app.alias, deploy_app.domain_id))
                if not ucds_port:
                    logging.error('create %s fail : can not get node port of UCDServer %s at %s'
                                  % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                    print('create %s fail : can not get node port of UCDServer %s at %s'
                          % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                    return False, ret_list
                if not self.__update_ucds_data(deploy_app.alias, deploy_app.domain_id, ucds_port):
                    logging.error('create %s fail : update %s at %s data fail' %
                                  (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                    print('create %s fail : update %s at %s data fail' %
                          (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                    return False, ret_list
                if not PlatformManager.restart_all(self.platform_id, self.gls_standard_alias, 30):
                    logging.error('create platform %s fail : after add UCDServer, restart all glsserver fail' % self.platform_id)
                    print('create platform %s fail : after add UCDServer, restart all glsserver fail' % self.platform_id)
                    return False, ret_list
        if not PlatformManager.restart_all(self.platform_id, self.dcs_standard_alias, 20):
            logging.error('create platform %s fail : after add all apps, restart all dcs fail' % self.platform_id)
            print('create platform %s fail : after add all apps, restart all dcs fail' % self.platform_id)
            return False, ret_list
        logging.info('create %s success, use time %d seconds' % (self.platform_id, (datetime.datetime.now() - start).seconds))
        print('create %s success, use time %d seconds' % (self.platform_id, (datetime.datetime.now() - start).seconds))
        return True, ret_list

    def update(self, domain_list):
        start = datetime.datetime.now()
        if not PlatformManager.is_platform_exist(self.platform_id):
            logging.error('create %s fail : %s exist' % (self.platform_id, self.platform_id))
            raise Exception('create %s fail : %s exist' % (self.platform_id, self.platform_id))
        deploy_apps = list()
        ret_list = list()
        for domain in domain_list:
            domain_id = domain['domainId']
            for app in domain['appUpdateOperationList']:
                operation = app['operation']
                app_name = app['appName']
                alias = app['appAlias']
                if operation == 'ADD' or operation == 'UPDATE':
                    version = app['targetVersion']
                    app_tag = '%s %s[%s(%s)] at %s ' % (operation, alias, app_name, version, domain_id)
                    cfgs = app['cfgs']
                    app_module = self.cmdb.query(app_name, version)
                    if not app_module:
                        logging.error('%s fail : %s[%s] not exist' % (app_tag, app_name, version))
                        raise Exception('%s fail : %s[%s] not exist' % (self.platform_id, app_name, version))
                    check_result = self.__package_and_cfg_check(app_module, cfgs)
                    if check_result:
                        logging.error('%s fail : %s' % (app_tag, check_result))
                        raise Exception('%s fail : %s' % (app_tag, check_result))
                    if not self.image_repository.prepare_image(app_module):
                        logging('%s fail : prepare image fail' % app_tag)
                        raise Exception('%s fail : prepare image fail' % app_tag)
                else:
                    cfgs = None
                    app_module = dict()
                    app_module['appName'] = app_name
                app_ins = AppInstance(self.platform_id, domain_id, app_module, alias, operation, cfgs,
                                      app['addDelay'], src_revision='2', target_revision='1')
                deploy_apps.append(app_ins)
        db_port = PlatformManager.get_service_node_port(self.platform_id, self.gls_db.service_name)
        if not db_port:
            logging.error(
                'create %s fail : can not get node port of database %s' % (self.platform_id, self.gls_db.service_name))
            raise Exception(
                'create %s fail : can not get node port of database %s' % (self.platform_id, self.gls_db.service_name))
        self.gls_db.conn(db_port, timeout=0)
        for deploy_app in deploy_apps:
            succ = deploy_app.execute(self.work_dir)
            ret_list.append(deploy_app.__dict__)
            if not succ:
                logging.error('create platform %s fail : ADD %s to %s fail'
                              % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                print('create platform %s fail : ADD %s to %s fail'
                      % (self.platform_id, deploy_app.alias, deploy_app.domain_id))
                return ret_list, False
            if deploy_app.app_name == 'UCDServer' and ( deploy_app.operation == 'ADD' or deploy_app.operation == 'UPDATE'):
                ucds_port = PlatformManager.get_service_node_port(self.platform_id, '%s-%s-out' % (deploy_app.alias, deploy_app.domain_id))
                if not ucds_port:
                    logging.error('can not get node port of UCDServer %s at %s' % (deploy_app.alias, deploy_app.domain_id))
                    raise Exception('can not get node port of UCDServer %s at %s' % (deploy_app.alias, deploy_app.domain_id))
                if not self.__update_ucds_data(deploy_app.alias, deploy_app.domain_id, ucds_port):
                    logging.error('update %s at %s data fail' % (deploy_app.alias, deploy_app.domain_id))
                    raise Exception('update %s at %s data fail' % (deploy_app.alias, deploy_app.domain_id))
                if not PlatformManager.restart_all(self.platform_id, self.gls_standard_alias, 30):
                    logging.error('after %s UCDServer %s at %s, restart all glsserver fail'
                                  % (deploy_app.operation, deploy_app.alias, deploy_app.domain_id))
                    raise Exception('after %s UCDServer %s at %s, restart all glsserver fail'
                                    % (deploy_app.operation, deploy_app.alias, deploy_app.domain_id))
        if not PlatformManager.restart_all(self.platform_id, self.dcs_standard_alias, 20):
            logging.error('after update platform %s, restart all dcs fail' % self.platform_id)
            raise Exception('after update platform %s, restart all dcs fail' % self.platform_id)
        logging.info('update %s success,use time %d(s)' % (self.platform_id, (datetime.datetime.now() - start).seconds))
        print('update %s success,use time %d(s)' % (self.platform_id, (datetime.datetime.now() - start).seconds))
        return True, ret_list


class GLSOracleDB(object):

    def __init__(self, user, pwd, ip, sid='xe', service_name='oracle'):
        self.user = user
        self.pwd = pwd
        self.ip = ip
        self.sid = sid
        self.service_name = service_name
        self.port = None
        self.ora_conn = None
        self.db_type = 'ORACLE'

    def conn(self, port=1521, timeout=180):
        self.port = port
        start = datetime.datetime.now()
        while True:
            try:
                con = OracleUtils(self.user, self.pwd, self.ip, self.port, self.sid)
                time_usage = (datetime.datetime.now() - start).seconds
                self.ora_conn = con
                logging.debug('oracle conn success, use time %s(s)' % time_usage)
                break
            except Exception as e:
                time_usage = (datetime.datetime.now() - start).seconds
                logging.error('conn oracle fail, use time %d : %s' % (time_usage, e.args[0]))
                if time_usage < timeout:
                    time.sleep(10)
                else:
                    raise Exception('conn oracle timeout : %s' % e.args[0])

    """处理数据二维数组，转换为json数据返回"""

    def select(self, sql):
        return self.ora_conn.select(sql)

    def disconnect(self):
        self.ora_conn.disconnect()

    def insert(self, sql, list_param):
        self.ora_conn.insert(sql, list_param)

    def update(self, sql):
        self.ora_conn.update(sql)

    def delete(self, sql):
        self.ora_conn.delete(sql)


class OracleUtils(object):

    def __init__(self, user, pwd, ip, port, sid):
        conn_str = "%s/%s@%s:%d/%s" % (user, pwd, ip, port, sid)
        logging.debug('oracle con_str : %s' % conn_str)
        self.connect = cx_Oracle.connect(conn_str)
        self.cursor = self.connect.cursor()
        logging.debug('oracle conn create success')

    """处理数据二维数组，转换为json数据返回"""
    def select(self, sql):
        lst = []
        self.cursor.execute(sql)
        result = self.cursor.fetchall()
        col_name = self.cursor.description
        for row in result:
            dt = {}
            for col in range(len(col_name)):
                key = col_name[col][0]
                value = row[col]
                dt[key] = value
            lst.append(dt)
        js = json.dumps(lst, ensure_ascii=False, indent=2, separators=(',', ':'))
        return js

    def disconnect(self):
        self.cursor.close()
        self.connect.close()

    def insert(self, sql, list_param):
        try:
            self.cursor.executemany(sql,list_param)
            self.connect.commit()
            print("插入ok")
        except Exception as e:
            print(e)
        finally:
            self.disconnect()

    def update(self, sql):
        logging.debug('prepare to execute update sql : %s' % sql)
        try:
            self.cursor.execute(sql)
            self.connect.commit()
            logging.debug('update success')
        except Exception as e:
            logging.error('update fail : %s' % e.args[0])
            raise e
        finally:
            self.disconnect()

    def delete(self, sql):
        try:
            self.cursor.execute(sql)
            self.connect.commit()
            print("delete ok")
        except Exception as e:
            print(e)
        finally:
            self.disconnect()


def print_help():
    print('-c images_base_path to create images for global params ccod_apps, images_base_path should be empty directory')
    print('-r to deploy app defined by global param test_schema_json without checking image and cfgs of app')
    print('-vr to check image and cfgs of app defined by global param test_schema_json and then deploy')


if __name__ == '__main__':
    cmdb = CMDB(cmdb_host_url)
    nexus_repository = NexusRepository(nexus_host_url, nexus_user, nexus_user_pwd)
    image_repository = ImageRepository(nexus_image_repository_url, nexus_user, nexus_user_pwd, nexus_image_repository,
                                       nexus_repository)
    gls_db = GLSOracleDB(gls_db_user, gls_db_pwd, k8s_host_ip, service_name=gls_db_svc_name)

    ccod_platform = CCODPlatform(schema_platform_id, cmdb, k8s_deploy_git_url, k8s_host_ip, nexus_repository,
                                 image_repository, gls_db, base_data_repository, base_data_zip_nexus_path)
    domain_plan_list = platform_deploy_schema['domainUpdatePlanList']
    task_type = platform_deploy_schema['taskType']
    logging.debug('begin to %s %s' % (schema_platform_id, task_type))
    # task_type = 'UPDATE'
    try:
        if task_type == 'CREATE':
            succ, steps = ccod_platform.create(domain_plan_list, True)
        elif task_type == 'UPDATE':
            succ, steps = ccod_platform.update(domain_plan_list)
        else:
            logging.error('not support task type %s' % platform_deploy_schema['taskType'])
            raise Exception('not support task type %s' % platform_deploy_schema['taskType'])
        logging.info('%s %s %s :\n%s' % (task_type, schema_platform_id, succ, json.dumps(steps, ensure_ascii=False)))
    except Exception as e:
        print('deploy app exception:%s' % e.args[0])
        logging.error('deploy app exception:%s' % e, exc_info=True)

