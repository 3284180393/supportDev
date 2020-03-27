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
ccod_apps = """dcms##dcms##11110##dcms.war##war"""
make_image_base_path = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/test'
k8s_host_ip = platform_deploy_params['k8s_host_ip']
gls_db_type = platform_deploy_params['gls_db_type']
gls_db_user = platform_deploy_params['gls_db_user']
gls_db_pwd = platform_deploy_params['gls_db_pwd']
gls_db_sid = platform_deploy_params['gls_db_sid']
gls_db_svc_name = platform_deploy_params['gls_db_svc_name']
platform_id = platform_deploy_params['platform_id']
base_data_nexus_repository = platform_deploy_schema['baseDataNexusRepository']
base_data_zip_nexus_path = platform_deploy_schema['baseDataNexusPath']
base_data_unzip_dir = '/home/kubernetes/volume/%s' % platform_id
gls_stand_alias = 'glsserver'
gls_db_name = 'CCOD'
gls_service_unit_table = 'GLS_SERVICE_UNIT'
ucds_start_timeout = 30


class OracleUtils(object):

    def __init__(self, user, pwd, ip, port, sid):
        conn_str = "%s/%s@%s:%d/%s" % (user, pwd, ip, port, sid)
        logging.debug('oracle con_str : %s' % conn_str)
        try:
            self.connect = cx_Oracle.connect(conn_str)
            self.cursor = self.connect.cursor()
        except Exception as e:
            logging.error('create %s conn exception, 30s later try again : %s' % (conn_str, e.args[0]))
            time.sleep(30)
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


def __get_app_query_url(app_name, version):
    return '%s/api/apps/%s/%s' % (cmdb_host_url, app_name, version)


def __get_image_query_url(app_name, version):
    return  "%s/v2/%s/%s/tags/list" % (nexus_image_repository_url, app_name, version)


# def __get_app_cfg_download_uri(platform_id, domain_id, app_alias, file_name):
#     return '%s/repository/%s/configText/%s/%s_%s/%s' % (nexus_host_url, cfg_repository, platform_id, domain_id, app_alias, file_name)


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


def get_app_cfg_params_for_k8s(cfgs):
    cfg_params = ""
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


def get_add_app_helm_command(platform_id, domain_id, app_name, app_type, version, alias, cfgs, work_dir):
    version = re.sub('\\:', '-', version)
    app_work_path = 'cd %s;cd payaml/%s' % (work_dir, app_name)
    if app_type == 'CCOD_WEBAPPS_MODULE':
        if app_name == 'cas':
            app_work_path = 'cd %s;cd payaml/tomcat6-jre7/' % work_dir
        else:
            app_work_path = 'cd %s;cd payaml/resin-4.0.13_jre-1.6.0_21/' % work_dir
    cfg_params_for_k8s = get_app_cfg_params_for_k8s(cfgs)
    exec_command = '%s;/usr/local/bin/helm install --set module.vsersion=%s --set module.name=%s --set module.alias=%s-%s %s %s-%s . -n %s' % (
        app_work_path, version, app_name, alias, domain_id,  cfg_params_for_k8s, alias.lower(), domain_id.lower(), platform_id.lower()
    )
    logging.info('command for deploy %s/%s/%s/%s/%s is %s and app_work_dir is %s' % (
        platform_id, domain_id, app_name, alias, version, exec_command, app_work_path
    ))
    return exec_command


def get_del_app_helm_command(platform_id, domain_id, app_name, app_alias):
    exec_command = '/usr/local/bin/helm del %s-%s . -n %s' % (
        app_alias.lower(), domain_id.lower(), platform_id.lower()
    )
    logging.info('command for del %s/%s/%s/%s is %s' % (
        platform_id, domain_id, app_name, app_alias, exec_command))
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


def nexus_app_image_exist_query(app_name, version):
    query_url = __get_app_image_uri(app_name, version)
    a = HTTPBasicAuth(nexus_user, nexus_user_pwd)
    response = requests.get(url=query_url, auth=a)
    if response.status_code != 200:
        return False
    if "\"%s\"" % version in response.text:
        return True
    return False


def nexus_asset_query(asset_id):
    url = "%s/service/rest/v1/assets/%s" % (nexus_host_url, asset_id)
    a = HTTPBasicAuth(nexus_user, nexus_user_pwd)
    response = requests.get(url=url, auth=a)
    if response.status_code != 200:
        logging.error('query %s fail : error_code=%d' % (url, response.status_code))
        raise Exception('error code %d' % response.status_code)
    logging.debug(response.text)
    data = json.loads(response.text)
    return data


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


def download_file_from_nexus(file_download_url, save_path):
    file_download_url = re.sub(u'/$', u'', file_download_url)
    file_name = file_download_url.split('/')[-1]
    md5_l = hashlib.md5()
    logging.debug('download %s from %s and save as %s' % (file_name, file_download_url, save_path))
    try:
        req = urllib2.Request(file_download_url)
        basic_auth = 'Basic %s' % (base64.b64encode('%s:%s' % (nexus_user, nexus_user_pwd)))
        req.add_header("Authorization", basic_auth)
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


def generate_platform_base_data():
    logging.debug('begin to check data base dir %s' % base_data_unzip_dir)
    if os.path.exists(base_data_unzip_dir) and not os.path.isdir(base_data_unzip_dir):
        logging.error('%s exist and not directory' % base_data_unzip_dir)
        raise Exception('%s exist and not directory' % base_data_unzip_dir)
    elif os.path.exists(base_data_unzip_dir):
        logging.debug('%s not empty, clean it first', base_data_unzip_dir)
        del_file(base_data_unzip_dir)
    else:
        logging.debug('%s not exist, create it fist' % base_data_unzip_dir)
        os.makedirs(base_data_unzip_dir)
    file_name = base_data_zip_nexus_path.split('/')[-1]
    arr = file_name.split('.')
    if len(arr) == 1 or arr[-1] != 'zip':
        logging.error('%s is not a legal base data zip package name' % file_name)
        raise Exception('%s is not a legal base data zip package name' % file_name)
    download_url = '%s/repository/%s/%s' % (nexus_host_url, base_data_nexus_repository, base_data_zip_nexus_path)
    save_path = '%s/%s' % (base_data_unzip_dir, file_name)
    logging.debug('begin to download base data zip package from %s to %s' % (download_url, save_path))
    download_file_from_nexus(download_url, save_path)
    logging.debug('base data zip package download finish')
    logging.debug('begin to unzip base data zip package')
    exec_command = 'cd %s;unzip %s' % (base_data_unzip_dir, file_name)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    # exec_command = 'cd %s; rm -f %s' % (base_data_unzip_dir, file_name)
    # exec_result = __run_shell_command((exec_command, None))
    # print(exec_result)
    logging.debug('base data zip unzip finish')


def del_file(path):
    ls = os.listdir(path)
    for i in ls:
        c_path = os.path.join(path, i)
        if os.path.isdir(c_path):
            del_file(c_path)
        else:
            os.remove(c_path)


def download_install_package(app_name, app_alias, version, package_file_name, save_dir):
    package_download_url = '%s/repository/%s/%s/%s/%s/%s' % (nexus_host_url, app_repository, app_name, app_alias, version, package_file_name)
    save_path = '%s/%s' % (save_dir, package_file_name)
    download_file_from_nexus(package_download_url, save_path)
    return save_path


def generate_docker_file(app_package_file_name, save_dir, package_type='CCOD_KERNEL_MODULE'):
    save_file_path = '%s/%s' % (save_dir, 'Dockerfile')
    print(save_file_path)
    with open(save_file_path, 'w') as out_f:
        if package_type == 'CCOD_KERNEL_MODULE':
            out_f.write("FROM harbor.io:1180/ccod-base/centos-backend:0.4\n")
            # out_f.write("COPY lib/*.* /opt/\n")
        else:
            out_f.write("FROM nexus.io:5000/ccod-base/alpine-java:jdk8-slim\n")
        out_f.write("ADD %s /opt/%s\n" % (app_package_file_name, app_package_file_name))
        if package_type == 'CCOD_KERNEL_MODULE':
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


def __get_app_image_uri(app_name, version):
    image_uri = app_image_query_url % app_name
    return image_uri


def build_app_image(app_name, version, app_dir):
    # logging.debug('cp lib of %s to ./lib' % gcc_depend_lib_path)
    # exec_command = 'cd %s;mkdir lib;cp %s/*.* ./lib' % (app_dir, gcc_depend_lib_path)
    # exec_result = __run_shell_command(exec_command, None)
    # print(exec_result)
    version = re.sub(':', '-', version)
    image_tag = '%s:%s' % (app_name.lower(), version)
    image_uri = '%s/%s/%s' % (nexus_image_repository_url, image_repository, image_tag)
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
    logging.debug('confirm %s(%s) image exist at server', app_name, version)
    exec_command = "docker image ls| grep %s | grep %s | awk '{print $3}'" % (app_name.lower(), re.sub(':', '-', version))
    exec_result = __run_shell_command(exec_command, None)
    if exec_result:
        return True
    logging.debug('%s(%s) image not exist at server, confirm it has been built at nexus repository' % (app_name, version))
    confirm_result = nexus_app_image_exist_query(app_name, version)
    if not confirm_result:
        logging.info('%s(%s) not been built' % (app_name, version))
    return confirm_result


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
    return app_list


def check_platform_exist(platform_id):
    logging.debug('check platform %s existed' % platform_id)
    exec_command = "kubectl get namespace | awk '{print $1}' | grep -P \"^%s$\"" % platform_id
    exec_result = __run_shell_command(exec_command, None)
    if exec_result:
        logging.debug('platform %s exist' % platform_id)
        return True
    logging.debug('platform %s not exist' % platform_id)
    return False


def delete_exist_platform(platform_id):
    logging.debug('begin to delete platform %s' % platform_id)
    exec_command = 'kubectl delete namespace %s' % platform_id
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    exec_command = 'kubectl delete pv base-volume-%s' % platform_id
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    logging.debug('%s deleted' % platform_id)


def create_new_platform(work_dir):
    logging.debug('create platform %s' % platform_id)
    exec_command = 'kubectl create namespace %s' % platform_id
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    logging.debug('create base service for %s' % platform_id)
    print('create base service for %s' % platform_id)
    db_param = '--set oracle.ccod.name=oracle --set oracle.ccod.sid=xe --set runtime.network.domainName=ccod.io --set oracle.ccod.user=ccod --set oracle.ccod.passwd=ccod --set mysql.ccod.user=ucds  --set mysql.ccod.passwd=ucds'
    exec_command = 'cd %s;cd payaml/baseService;/usr/local/bin/helm install %s -n %s baseservice .' % (
        work_dir, db_param, platform_id)
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


def create_ccod_platform(platform_id, work_dir):
    if check_platform_exist(platform_id):
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
    db_param = '--set oracle.ccod.name=oracle --set oracle.ccod.sid=xe --set runtime.network.domainName=ccod.io --set oracle.ccod.user=ccod --set oracle.ccod.passwd=ccod --set mysql.ccod.user=ucds  --set mysql.ccod.passwd=ucds'
    exec_command = 'cd %s;cd payaml/baseService;/usr/local/bin/helm install %s -n %s baseservice .' % (work_dir, db_param, platform_id)
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


def get_md5_value(input_str):
    md5 = hashlib.md5()
    md5.update(input_str)
    md5_digest = md5.hexdigest()
    return md5_digest


def exec_platform_create_schema(schema):
    platform_id = platform_deploy_schema['platformId'].lower()
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
            version = deploy_app['targetVersion']
            alias = deploy_app['appAlias']
            app_module = query_app_module(app_name, version)
            app_type = app_module['appType']
            cfgs = deploy_app['cfgs']
            helm_command = get_add_app_helm_command(platform_id, domain_id, app_name, app_type, version, alias, cfgs, work_dir)
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


def check_app_install_package_and_cfg(app_module, cfgs):
    app_name = app_module['appName']
    version = app_module['version']
    logging.debug('begin to check %s[%s] install package' % (app_name, version))
    try:
        check_nexus_app_file(app_module['installPackage'])
    except Exception as e:
        logging.error('check install package of %s[%s] fail : %s' % (app_name, version, e.args[0]))
        raise Exception('check install package of %s[%s] fail : %s' % (app_name, version, e.args[0]))
    logging.debug('check %s[%s] install package success' % (app_name, version))
    logging.debug('begin to check %s[%s] cfg' % (app_name, version))
    cfg_dict = {}
    for cfg in app_module['cfgs']:
        cfg_dict[cfg['fileName']] = cfg
    if len(cfg_dict) != len(cfgs):
        logging.error('check cfg of %s[%s] fail : wanted %d cfg but %d' %(app_name, version, len(cfg_dict), len(cfgs)))
        raise Exception('check cfg of %s[%s] fail : wanted %d cfg but %d' %(app_name, version, len(cfg_dict), len(cfgs)))
    for cfg in cfgs:
        try:
            check_nexus_app_file(cfg)
            pass
        except Exception as e:
            logging.error('check cfg of %s[%s] fail : %s' % (app_name, version, e.args[0]))
            raise Exception('check cfg of %s[%s] fail : %s' % (app_name, version, e.args[0]))
    logging.debug('check %s[%s] cfg success' % (app_name, version))


def check_nexus_app_file(app_file):
    data = nexus_asset_query(app_file['nexusAssetId'])
    file_name = data['path'].split('/')[-1]
    if file_name != app_file['fileName']:
        logging.error('want file name is %s but nexus is %s'
                      % (app_file['fileName'], file_name))
        raise Exception('want file name is %s but nexus is %s'
                        % (app_file['fileName'], file_name))
    if app_file['md5'] != data['checksum']['md5']:
        logging.error('want %s md5 is %s but nexus is %s' % (file_name, app_file['md5'], data['checksum']['md5']))
        raise Exception('want %s md5 is %s but nexus is %s' % (file_name, app_file['md5'], data['checksum']['md5']))


def add_app_module_to_k8s(domain_id, app_module, alias, cfgs, work_dir):
    app_name = app_module['appName']
    version = app_module['version']
    check_app_install_package_and_cfg(app_module, cfgs)
    image_exist = app_image_exist_check(app_name, version)
    if not image_exist:
        app_dir = '%s/%s/%s' % (work_dir, app_name, version)
        if not os.path.exists(app_dir):
            exec_command = 'mkdir %s -p' % app_dir
            __run_shell_command(exec_command, None)
        generate_app_image(app_name, app_module['appAlias'], version,
                           app_module['installPackage']['fileName'], app_module['appType'], app_dir)
    opt = dict()
    opt['platform_id'] = platform_id
    opt['domain_id'] = domain_id
    opt['app_name'] = app_name
    opt['version'] = version
    opt['alias'] = alias
    opt['operation'] = 'ADD'
    opt['helm_command'] = get_add_app_helm_command(platform_id, domain_id, app_name, app_module['appType'], version, alias, cfgs, work_dir)
    return opt


def delete_app_module_from_k8s(platform_id, domain_id, app_name, version, alias):
    opt = dict()
    opt['platform_id'] = platform_id
    opt['domain_id'] = domain_id
    opt['app_name'] = app_name
    opt['version'] = version
    opt['alias'] = alias
    opt['operation'] = 'ADD'
    opt['helm_command'] = get_del_app_helm_command(platform_id, domain_id, app_name, alias)
    return opt


def generate_platform_deploy_operation(work_dir):
    del_list = list()
    add_list = list()
    for update_plan in platform_deploy_schema['domainUpdatePlanList']:
        domain_id = update_plan['domainId']
        for app_opt in update_plan['appUpdateOperationList']:
            op = app_opt['operation']
            app_name = app_opt['appName']
            alias = app_opt['appAlias']
            if op == 'ADD' or op == 'START':
                version = app_opt['targetVersion']
                app_module = query_app_module(app_name, version)
                if not app_module:
                    logging.error('version %s of %s not exist' % (version, app_name))
                    raise Exception('version %s of %s not exist' % (version, app_name))
                opt = add_app_module_to_k8s(domain_id, app_module, alias, app_opt['cfgs'], work_dir)
                opt['delay'] = app_opt['addDelay']
                add_list.append(opt)
            elif op == 'DELETE' or op == 'STOP':
                version = app_opt['originalVersion']
                opt = delete_app_module_from_k8s(platform_id, domain_id, app_name, version, alias, work_dir)
                opt['delay'] = app_opt['addDelay']
                del_list.append(opt)
            elif op == 'VERSION_UPDATE' or op == 'CFG_UPDATE':
                version = app_opt['originalVersion']
                if not app_module:
                    logging.error('version %s of %s not exist' % (version, app_name))
                    raise Exception('version %s of %s not exist' % (version, app_name))
                opt = delete_app_module_from_k8s(platform_id, domain_id, app_name, version, alias, work_dir)
                del_list.append(opt)
                version = app_opt['targetVersion']
                app_module = query_app_module(app_name, version)
                opt = add_app_module_to_k8s(domain_id, app_module, alias, app_opt['cfgs'], work_dir)
                add_list.append(opt)
    return del_list, add_list


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


def sort_app_opt(domain_plan, app_add_order):
    if not app_add_order or len(app_add_order) == 0:
        return
    dt = dict()
    for opt in domain_plan['appUpdateOperationList']:
        app_name = opt['appName']
        if app_name not in dt.keys():
            dt[app_name] = list()
        dt[app_name].append(opt)
    opt_list = list()
    for app_name in app_add_order:
        if app_name in dt.keys():
            opt_list.extend(dt[app_name])
            del opt[app_name]
    for opts in dt.values():
        opt_list.extend(opts)
    domain_plan['appUpdateOperationList'] = opt_list


def deploy_platform():
    platform_id = platform_deploy_schema['platformId'].lower()
    now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    work_dir = '/tmp/%s' % get_md5_value('%s%s' % (platform_id, now))
    delete_opt_list, add_opt_list = generate_platform_deploy_operation(work_dir)
    generate_platform_base_data()
    exec_command = 'mkdir %s -p' % work_dir
    __run_shell_command(exec_command, None)
    exec_command = 'cd %s;git clone %s' % (work_dir, k8s_deploy_git_url)
    __run_shell_command(exec_command, None)
    if platform_deploy_schema['taskType'] == 'CREATE':
        if check_platform_exist(platform_id):
            delete_exist_platform(platform_id)
        create_new_platform(work_dir)
    else:
        if not check_platform_exist(platform_id):
            logging.error('%s not existed %s fail' % (platform_id, platform_deploy_schema['taskType']))
            raise ('%s not existed %s fail' % (platform_id, platform_deploy_schema['taskType']))
    for opt in delete_opt_list:
        helm_command = opt['helm_command']
        print(helm_command)
        exec_result = __run_shell_command(helm_command, None)
        print(exec_result)
    for opt in add_opt_list:
        helm_command = opt['helm_command']
        print(helm_command)
        exec_result = __run_shell_command(helm_command, None)
        print(exec_result)
        if opt['delay'] > 0:
            time.sleep(opt['delay'])
        if opt['app_name'] == 'UCDServer':
            logging.debug('%s is UCDServer, so need update node port at glsserver' % opt['alias'])
            update_ucds_node_port(opt['alias'], opt['domain_id'])
            logging.debug('UCDServer node port has been update so restart glsserver first')
            restart_gls_server()
            time.sleep(30)
    for opt in add_opt_list:
        if opt['app_name'] == 'DDSServer':
            restart_app(opt['alias'], opt['domain_id'])
            if opt['delay'] > 0:
                time.sleep(opt['delay'])


def get_svc_node_port(service_name):
    exec_command = "kubectl get svc -n %s | grep %s | awk '{print $5}' | awk -F ':' '{print $2}' | awk -F '/' '{print $1}'" % (platform_id, service_name)
    exec_result = __run_shell_command(exec_command, None)
    node_port = None
    if exec_result and exec_result.isdigit():
        node_port = int(exec_result)
    logging.info('port of %s''s %s is %s' % (platform_id, service_name, node_port))
    return node_port


def update_ucds_node_port(ucds_alias, domain_id):
    db_port = get_svc_node_port(gls_db_svc_name)
    if not db_port or db_port == 0:
        logging.error('get glsserver oracle database port fail:svn_name=%s' % gls_db_svc_name)
        raise Exception('get glsserver oracle database port fail:svn_name=%s' % gls_db_svc_name)
    ucds_svc_name = '%s-%s-out' % (ucds_alias, domain_id)
    time_usage = 0
    ucds_node_port = None
    while not ucds_node_port and ucds_start_timeout > time_usage:
        time.sleep(3)
        time_usage += 3
        ucds_node_port = get_svc_node_port(ucds_svc_name)
    if not ucds_node_port:
        logging.error('%s start timeout : use time %d', ucds_svc_name, time_usage)
        raise Exception('%s start timeout : use time %d', ucds_svc_name, time_usage)
    logging.info('node port of %s is %d, use time %d' % (ucds_svc_name, ucds_node_port, time_usage))
    update_sql = """update "%s"."%s" set PARAM_UCDS_PORT='%d' where NAME='%s-%s'""" \
                 % (gls_db_name, gls_service_unit_table, ucds_node_port, ucds_alias, domain_id)
    logging.info('update ucds sql=%s' % update_sql)
    oracle = OracleUtils(gls_db_user, gls_db_pwd, k8s_host_ip, db_port, gls_db_sid)
    oracle.update(update_sql)


def restart_app(app_alias, domain_id):
    logging.debug('begin to restart %s at %s' % (app_alias, domain_id))
    exec_command = """kubectl get pod -n %s | grep -E '^%s-%s-' | awk '{print $1}'""" \
                   % (platform_id, app_alias, domain_id)
    exec_result = __run_shell_command(exec_command, None)
    if not exec_result:
        logging.error('not find %s at domain %s' % (app_alias, domain_id))
        raise Exception('not find %s at domain %s' % (app_alias, domain_id))
    if len(exec_result.split('\n')) > 1:
        logging.error('%s at domain %s not unique : %s' % (app_alias, domain_id, exec_result))
        raise Exception('%s at domain %s not unique : %s' % (app_alias, domain_id, exec_result))
    print(exec_result)
    exec_command = 'kubectl delete pod -n %s %s' % (platform_id, exec_result)
    print(exec_command)
    exec_result = __run_shell_command(exec_command, None)
    print(exec_result)
    logging.info('restart %s at %s success' % (app_alias, domain_id))


def restart_gls_server():
    exec_command = """kubectl get pod -n %s | grep -E '^%s-[a-z]+[0-9]+-' | awk '{print $1}'""" \
                   % (platform_id, gls_stand_alias)
    exec_result = __run_shell_command(exec_command)
    if not exec_result:
        logging.error('restart gls fail : can not find glsserver at platform %s' % platform_id)
        raise Exception('restart gls fail : can not find glsserver at platform %s' % platform_id)
    pod_name_list = exec_result.split('\n')
    logging.debug('%s has %d glsserver : %s' % (platform_id, len(pod_name_list), exec_result))
    for pod_name in pod_name_list:
        alias = pod_name.split('-')[0]
        domain_id = pod_name.split('-')[1]
        logging.debug('prepare to restart glsserver %s at domain %s' % (alias, domain_id))
        restart_app(alias, domain_id)


if __name__ == '__main__':
    try:
        logging.debug('now begin to deploy app')
        deploy_platform()
        logging.info('app deploy finish')
    except Exception as e:
        print('deploy app exception:%s' % e.args[0])
        logging.error('deploy app exception:%s' % e, exc_info=True)

