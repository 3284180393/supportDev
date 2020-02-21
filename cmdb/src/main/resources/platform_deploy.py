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
platform_deploy_params = """"""
app_repository = platform_deploy_params['app_repository']
image_repository = platform_deploy_params['image_repository']
nexus_host_url = platform_deploy_params['nexus_host_url']
nexus_user = platform_deploy_params['nexus_user']
nexus_user_pwd = platform_deploy_params['nexus_user_pwd']
cfg_repository = platform_deploy_params['cfg_repository']
nexus_image_repository_url = platform_deploy_params['nexus_image_repository_url']
cmdb_host_url = platform_deploy_params['cmdb_host_url']
upload_url = "%s/service/rest/v1/components?repository=%s" % (nexus_host_url, app_repository)
app_register_url = "%s/cmdb/api/apps" % cmdb_host_url
schema_update_url = '%s/cmdb/api/platformUpdateSchema' % cmdb_host_url
k8s_deploy_git_url = platform_deploy_params['k8s_deploy_git_url']
app_image_query_url = "%s/v2/%%s/%%s/tags/list" % nexus_image_repository_url
platform_deploy_schema = platform_deploy_params['update_schema']
app_deploy_order = platform_deploy_params['app_deploy_order']
ccod_apps = """dcms##dcms##11110##dcms.war##war"""
make_image_base_path = '/root/project/gitlab-ccod/devops/imago/ccod-2.0/test'


def __get_app_query_url(app_name, version):
    return '%s/api/apps/%s/%s' % (cmdb_host_url, app_name, version)


def __get_image_query_url(app_name, version):
    return  "%s/v2/%s/%s/tags/list" % (nexus_image_repository_url, app_name, version)


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


def get_app_cfg_params_for_k8s(platform_id, domain_id, alias, cfgs):
    cfg_params = ""
    for cfg in cfgs:
        cfg_deploy_path = re.sub('^.*WEB-INF/', 'WEB-INF/', cfg['deployPath'])
        cfg_deploy_path = re.sub('/$', '', cfg_deploy_path)
        cfg_file_name = re.sub('\\.', '\\\\\\.', cfg['fileName'])
        cfg_params = '%s --set config.%s=%s' % (cfg_params, cfg_file_name, cfg_deploy_path)
    cfg_uri = '%s/repository/%s/configText/%s/%s_%s' % (
        nexus_host_url, cfg_repository, platform_id, domain_id, alias)
    return '%s --set runtime.configPath=%s' % (cfg_params, cfg_uri)


def get_add_app_helm_command(platform_id, domain_id, app_name, app_type, version, alias, cfgs, work_dir):
    version = re.sub('\\:', '-', version)
    app_work_path = 'cd %s;cd payaml/%s' % (work_dir, app_name)
    if app_type == 'CCOD_WEBAPPS_MODULE':
        if app_name == 'cas':
            app_work_path = 'cd %s;cd payaml/tomcat6-jre7/' % work_dir
        else:
            app_work_path = 'cd %s;cd payaml/resin-4.0.13_jre-1.6.0_21/' % work_dir
    cfg_params_for_k8s = get_app_cfg_params_for_k8s(platform_id, domain_id, alias, cfgs)
    exec_command = '%s;helm install --set module.vsersion=%s --set module.name=%s --set module.alias=%s-%s %s %s-%s . -n %s' % (
        app_work_path, version, app_name, alias, domain_id,  cfg_params_for_k8s, alias.lower(), domain_id.lower(), platform_id.lower()
    )
    logging.info('command for deploy %s/%s/%s/%s/%s is %s and app_work_dir is %s' % (
        platform_id, domain_id, app_name, alias, version, exec_command, app_work_path
    ))
    return exec_command


def get_del_app_helm_command(platform_id, domain_id, app_name, app_alias):
    exec_command = 'helm del %s-%s . -n %s' % (
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


def __get_app_image_uri(app_name, version):
    version = re.sub('\\:', '\\-', version)
    image_tag = '%s:%s' % (app_name.lower(), version)
    image_uri = '%s/%s/%s' % (nexus_image_repository_url, image_repository, image_tag)
    return image_uri


def build_app_image(app_name, version, app_dir):
    # logging.debug('cp lib of %s to ./lib' % gcc_depend_lib_path)
    # exec_command = 'cd %s;mkdir lib;cp %s/*.* ./lib' % (app_dir, gcc_depend_lib_path)
    # exec_result = __run_shell_command(exec_command, None)
    # print(exec_result)
    version = re.sub('\\:', '\\-', version)
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
    exec_command = "docker image ls| grep %s | grep %s | awk '{print $3}'" % (app_name.lower(), re.sub('\\:', '\\-', version))
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
                check_result = '%s image for %s version %s not exist;' % (check_result, app_name, version)
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
            version = deploy_app['targetVersion']
            alias = deploy_app['appAlias']
            app_module = query_app_module(app_name, version)
            app_type = app_module['appType']
            cfgs = app_module['cfgs']
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


def add_app_module_to_k8s(platform_id, domain_id, app_module, alias, work_dir):
    app_name = app_module['appName']
    version = app_module['version']
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
    opt['helm_command'] = get_add_app_helm_command(platform_id, domain_id, app_name, app_module['appType'], version, alias, app_module['cfgs'], work_dir)
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
    platform_id = platform_deploy_schema['platformId']
    for update_plan in platform_deploy_schema['domainUpdatePlanList']:
        domain_id = update_plan['domainId']
        for app_opt in update_plan['appUpdateOperationList']:
            op = app_opt['operation']
            app_name = app_opt['appName']
            alias = app_opt['appAlias']
            if op == 'ADD' or op == 'START':
                version = app_opt['targetVersion']
                app_module = query_app_module(app_name, version)
                opt = add_app_module_to_k8s(platform_id, domain_id, app_module, alias, work_dir)
                add_list.append(opt)
            elif op == 'DELETE' or op == 'STOP':
                version = app_opt['originalVersion']
                opt = delete_app_module_from_k8s(platform_id, domain_id, app_name, version, alias, work_dir)
                del_list.append(opt)
            elif op == 'VERSION_UPDATE' or op == 'CFG_UPDATE':
                version = app_opt['originalVersion']
                opt = delete_app_module_from_k8s(platform_id, domain_id, app_name, version, alias, work_dir)
                del_list.append(opt)
                version = app_opt['targetVersion']
                app_module = query_app_module(app_name, version)
                opt = add_app_module_to_k8s(platform_id, domain_id, app_module, alias, work_dir)
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


def deloy_platform():
    for plan in platform_deploy_schema['domainUpdatePlanList']:
        sort_app_opt(plan, app_deploy_order)
    delete_opt_list, add_opt_list = generate_platform_deploy_operation(make_image_base_path)
    for opt in delete_opt_list:
        helm_command = opt['helm_command']
        print(helm_command)
        # exec_result = __run_shell_command(helm_command, None)
        # print(exec_result)
    for opt in add_opt_list:
        helm_command = opt['helm_command']
        print(helm_command)
        # exec_result = __run_shell_command(helm_command, None)
        # print(exec_result)
        app_name = opt['app_name']
        if app_name == 'glsServer':
            print('%s start, so sleep 60s' % app_name)
            time.sleep(30)
            print('sleep end')
        elif app_name == 'DDSServer' or app_name == 'UCDServer' or app_name == ' dcs' or app_name == 'cmsserver' or app_name == 'ucxserver' or app_name == 'daengine':
            print('%s start, so sleep 20s' % app_name)
            time.sleep(20)
            print('sleep end')


if __name__ == '__main__':
    logging.debug('now begin to deploy app')
    deloy_platform()
    logging.info('app deploy finish')
