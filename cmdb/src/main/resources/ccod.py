# -*- coding: utf-8 -*-

import os
import json
import logging
import sys
import subprocess
import re
import time
import cx_Oracle
import pymysql
import datetime
import yaml


reload(sys)
sys.setdefaultencoding('utf8')
LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logging.basicConfig(filename='my.log', level=logging.DEBUG, format=LOG_FORMAT)
gls_service_unit_table = 'GLS_SERVICE_UNIT'
platform_id_regex = "^[a-z]+(\\-?[0-9a-z]+)*$"
yaml_line_match_regex = "^\\s+%s:\\s+[^\\s]+$"
sub_pattern = "%s:\\s+[^\\s]+$"
sub_value = "%s: %s"


class Gls_DB(object):

    def __init__(self, user, pwd, port, db_name, ip, db_type='ORACLE'):
        conn_str = "%s/%s@%s:%d/%s" % (user, pwd, ip, port, db_name)
        logging.debug('conn_str=%s, dbType=%s' % (conn_str, db_type))
        if db_type == 'MYSQL':
            self.connect = pymysql.connect(host=ip, port=port, user=user, passwd=pwd, db=db_name, charset='utf8')
        elif db_type == 'ORACLE':
            self.connect = cx_Oracle.connect(conn_str)
        else:
            raise Exception('unsupported gls db type %s' % db_type)
        self.cursor = self.connect.cursor()

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
            self.cursor.executemany(sql, list_param)
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


def get_service_node_port(platform_id, service_name):
    exec_command = "kubectl get svc -n %s | grep -E '^%s\s+' | awk '{print $5}' | awk -F ':' '{print $2}' | awk -F '/' '{print $1}'" \
                   % (platform_id, service_name)
    exec_result = run_shell_command(exec_command, None)
    logging.debug('nodePort of %s is %s' % (service_name, exec_result))
    return int(exec_result)


def get_deployment_status(platform_id, deployment_name):
    exec_command = "kubectl -n %s get deployment %s | grep %s" % (platform_id, deployment_name, deployment_name)
    exec_result = run_shell_command(exec_command)
    return re.split(r'\s+', exec_result)[1]


def run_shell_command(command, cwd=None, accept_err=False):
    """
    执行一条脚本命令,并返回执行结果输出
    :param command:执行命令,可执行文件/脚本可以是相对cwd的相对路径
    :param cwd:执行命令的cwd
    :param accept_err: 是否接受错误输出，如果为True将错误结果输出，否则抛出异常
    :return:返回结果
    """
    logging.info("准备执行命令:%s" % command)
    if cwd:
        logging.info('cwd=%s' % cwd)
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd,
                                   close_fds=True)
    else:
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, close_fds=True)
    out, err = process.communicate()
    if process.stdin:
        process.stdin.close()
    if process.stdout:
        process.stdout.close()
    if process.stderr:
        process.stderr.close()
    if err:
        if accept_err:
            result = err
        else:
            raise Exception(err)
    else:
        result = out
    logging.info("命令执行结果\n:%s" % result)
    return result


def get_start_param(param_file):
    if os.path.exists(param_file):
        r_open = open(param_file, 'r')
        for line in r_open:
            return json.loads(line)
    else:
        logging.error("%s not exist" % param_file)
        return []


def deploy_platform(platform_id, cfgs, steps, is_base=False):
    now = datetime.datetime.now()
    db_cfg = cfgs['glsserver']['db']
    db_type = db_cfg['type']
    print("**** begin to create %s platform by script ****" % platform_id)
    logging.info("begin to create %s platform by script" % platform_id)
    for step in steps:
        file_path = step['filePath']
        file_name = re.sub('.*/', '', file_path)
        arr = file_name.split('.')[0].split('-')
        k8s_opt = arr[-1]
        k8s_type = arr[-2]
        obj_name = re.sub('-[^-]+-[^-]+$', '', file_name)
        print('%s %s %s with %s' % (k8s_opt, k8s_type, obj_name, file_path))
        command = "kubectl apply -f %s" % file_path
        print(command)
        exec_result = run_shell_command(command)
        print(exec_result)
        if step['timeout'] > 0:
            if k8s_type == 'deployment':
                time_usage = 0
                while time_usage <= step['timeout']:
                    deploy_status = get_deployment_status(platform_id, obj_name)
                    print('%s deployment status is %s at %d seconds' % (obj_name, deploy_status, time_usage))
                    if deploy_status == '1/1':
                        print('%s deployment change to ACTIVE with %d seconds usage' % (obj_name, time_usage))
                        break
                    elif time_usage >= step['timeout']:
                        print('%s deployment start timeout with %d seconds usage' % (obj_name, time_usage))
                        break
                    time.sleep(3)
                    time_usage += 3
            else:
                print('after exec %s, sleep %d seconds' % (command, step['timeout']))
                time.sleep(step['timeout'])
        if k8s_type == 'service' and re.match('^ucds\d*.*-out$', obj_name):
            port = get_service_node_port('base-%s' % platform_id, db_cfg['service']['value'])
            if db_type == 'ORACLE':
                db = Gls_DB(db_cfg['user']['value'], db_cfg['password']['value'], port, db_cfg['sid']['value'],
                            cfgs['host-ip'], db_type=db_type)
            else:
                db = Gls_DB(db_cfg['user']['value'], db_cfg['password']['value'], port, db_cfg['name']['value'],
                            cfgs['host-ip'], db_type=db_type)
            port = get_service_node_port(platform_id, obj_name)
            print('%s started, so update  ucds port in glsserver to %d' % (obj_name, port))
            ucds = re.sub('-out$', '', obj_name)
            if db_type == 'oralce':
                update_sql = """update "%s"."%s" set PARAM_UCDS_PORT='%d' where NAME='%s'""" % (db_cfg['name']['value'], gls_service_unit_table, port, ucds)
            else:
                update_sql = """UPDATE %s SET PARAM_UCDS_PORT = '%d' WHERE	NAME = '%s'""" % (gls_service_unit_table, port, ucds)
            db.update(update_sql)
            print('%s port has been updated to %d' % (ucds, port))
    time_usage = 0
    while not is_base and time_usage <= 120:
        print('wait frontend module to startup')
        time.sleep(3)
        time_usage += 3
    print('**** %s create finish, use time %d seconds ****' % (platform_id, (datetime.datetime.now() - now).seconds))
    logging.info('%s create finish, use time %d seconds' % (platform_id, (datetime.datetime.now() - now).seconds))


def save_image(images, save_dir):
    print(len(images))
    if not os.path.exists(save_dir):
        raise Exception('%s directory not exist' % save_dir)
    if not os.path.isdir(save_dir):
        raise Exception('%s not directory' % save_dir)
    for image in images:
        image_tag = re.sub('.*/', '', image)
        print('export %s image to %s/%s.tar' % (image, save_dir, image_tag))
        command = 'docker save -o %s/%s.tar %s' % (save_dir, image_tag, image)
        exec_result = run_shell_command(command)
        print(exec_result)


def clear_image(images):
    for image in images:
        repository = re.sub('\\:[^\\:]+$', '', image)
        tag = re.sub('^.*\\:', '', image)
        print('delete image %s from docker' % image)
        command = """docker images|grep "%s"|grep "%s"|awk '{print $3}'|xargs docker rmi""" % (repository, tag)
        exec_result = run_shell_command(command, accept_err=True)
        print(exec_result)


def load_image(images, save_dir):
    if not os.path.exists(save_dir):
        raise Exception('%s directory not exist' % save_dir)
    if not os.path.isdir(save_dir):
        raise Exception('%s not directory' % save_dir)
    for image in images:
        image_tag = re.sub('.*/', '', image)
        # if image_tag != 'im:bb6b0816':
        #     continue
        print('import %s image from %s/%s.tar' % (image, save_dir, image_tag))
        command = 'docker load -i %s/%s.tar' % (save_dir, image_tag)
        exec_result = run_shell_command(command)
        print(exec_result)


def sub_yaml_line(line, key, value):
    if re.match(yaml_line_match_regex % key, line):
        line = re.sub(sub_pattern % key, sub_value % (key, value), line)
    return line


def yaml_line_replace(line, kind, platform_id, nfs_ip, host_name, cfg_data, proto_platform_id):
    if not line:
        return line
    if kind == "PV":
        line = sub_yaml_line(line, 'server', nfs_ip)
    elif kind == 'DEPLOYMENT':
        line = sub_yaml_line(line, '\\- /tmp/init.sh', host_name)
    elif kind == 'SERVICE':
        if re.match(yaml_line_match_regex % 'externalName', line):
            arr = re.split("\\:\\s+", line)[1].split(".")
            arr[1] = 'base-%s' % platform_id
            line = sub_yaml_line(line, 'externalName', '.'.join(arr))
    elif kind == 'CONFIGMAP':
        line = re.sub('/%s"' % proto_platform_id, '/%s"' % platform_id, line)
        line = re.sub('"%s"' % proto_platform_id, '"%s"' % platform_id, line)
        line = re.sub(' %s"' % proto_platform_id, ' %s"' % platform_id, line)
        line = re.sub('\\-%s"' % proto_platform_id, '-%s"' % platform_id, line)
        for key in cfg_data.keys():
            if line.find("##[]%s##" % key) >= 0:
                line = line.replace("##[]%s##" % key, '%s' % cfg_data[key])
            line = line.replace("##[]%s##" % key, '%s' % cfg_data[key])
    line = re.sub('/%s/' % proto_platform_id, '/%s/' % platform_id, line)
    line = re.sub('/%s$' % proto_platform_id, '/%s' % platform_id, line)
    line = re.sub('/%s;' % proto_platform_id, '/%s;' % platform_id, line)
    line = re.sub(' %s$' % proto_platform_id, ' %s' % platform_id, line)
    line = re.sub('\\-%s$' % proto_platform_id, '-%s' % platform_id, line)
    line = re.sub(' %s\\-' % proto_platform_id, ' %s-' % platform_id, line)
    line = re.sub('\\.%s\\.' % src_platform_id, '.%s.' % platform_id, line)
    line = re.sub(' %s\\.' % src_platform_id, ' %s.' % platform_id, line)
    line = re.sub('\\.%s ' % src_platform_id, '.%s ' % platform_id, line)
    return line


def replace_yaml_file(file_path, save_path, kind, platform_id, nfs_ip, host_name, cfg_data, proto_platform_id):
    save_dir = re.sub('/[^/]+$', '', save_path)
    if not os.path.exists(save_dir):
        os.makedirs(save_dir)
    with open(file_path, 'r') as in_f:
        lines = in_f.readlines()
        with open(save_path, 'w') as out_f:
            for line in lines:
                line = yaml_line_replace(line, kind, platform_id, nfs_ip, host_name, cfg_data, proto_platform_id)
                out_f.write(line)


def get_deploy_cfgs():
    with open('config.yaml', 'r') as in_f:
        cfgs = yaml.load(in_f)
    return cfgs


def generate_deploy_script(deploy_params, deploy_cfgs, proto_platform_id):
    sub_path = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    cfg_data = deploy_params['configCenterData']
    platform_id = deploy_cfgs['platform-id']
    nfs_ip = deploy_cfgs['nfs-ip']
    host_name = deploy_cfgs['host-name']
    for step in deploy_params['baseExecSteps']:
        file_path = step['filePath']
        save_path = '%s/%s' % (sub_path, file_path)
        kind = step['kind']
        replace_yaml_file(file_path, save_path, kind, platform_id, nfs_ip, host_name, cfg_data, proto_platform_id)
        step['filePath'] = save_path
    for step in deploy_params['execSteps']:
        file_path = step['filePath']
        save_path = '%s/%s' % (sub_path, file_path)
        kind = step['kind']
        replace_yaml_file(file_path, save_path, kind, platform_id, nfs_ip, host_name, cfg_data, proto_platform_id)
        step['filePath'] = save_path


def show_help():
    print('error command input, for example:')
    print('python ccod.py create : auto create ccod platform')
    print('python ccod.py image -e /tmp/ccod/images : export all necessary images to /tmp/ccod/images directory')
    print('python ccod.py image -i /tmp/ccod/images : import all necessary images from /tmp/ccod/images directory')
    print('python ccod.py image -d : clear exist images in docker')


if __name__ == '__main__':
    exec_params = get_start_param("start_param.txt")
    exec_cfgs = get_deploy_cfgs()
    create_platform_id = exec_cfgs['platform-id']
    src_platform_id = exec_params['platformParams']['platformId']
    print(json.dumps(exec_cfgs))
    if exec_cfgs['image'] and exec_cfgs['image']['load']:
        load_images = True
        image_save_dir = exec_cfgs['image']['save-dir']
        if not os.path.exists(image_save_dir):
            raise Exception("image directory %s not exist" % image_save_dir)
        elif not os.path.isdir(image_save_dir):
            raise Exception("image directory %s not directory" % image_save_dir)
    else:
        load_images = False
    print('load_image=%s' % load_images)
    if exec_cfgs['depend-cloud'] and exec_cfgs['depend-cloud']['create']:
        create_cloud = True
        base_data = exec_cfgs['depend-cloud']['base-data-dir']
        if not os.path.exists(base_data):
            raise Exception("base data directory %s for init not exist" % base_data)
        elif not os.path.isdir(base_data):
            raise Exception("base data directory %s for init not directory" % base_data)
    else:
        create_cloud = False
    exec_params['configCenterData']['platform_id'] = create_platform_id
    exec_params['configCenterData']['domain_name'] = exec_cfgs['host-name']
    gls_db_cfgs = exec_cfgs['glsserver']['db']
    logging.debug('glsdb=%s' % json.dumps(gls_db_cfgs))
    for k in gls_db_cfgs.keys():
        if k == 'type':
            continue
        exec_params['configCenterData'][gls_db_cfgs[k]['key']] = gls_db_cfgs[k]['value']
    logging.info('configCenterData=%s' % json.dumps(exec_params['configCenterData']))
    print('create_base_cloud=%s' % create_cloud)
    generate_deploy_script(exec_params, exec_cfgs, src_platform_id)
    if load_images:
        print('load image from %s' % image_save_dir)
        load_image(exec_params['images'], image_save_dir)
    if create_cloud:
        work_dir = '/home/kubernetes/volume/%s/base-volume' % create_platform_id
        command = "rm -rf %s;mkdir %s -p;cp %s/. %s -R;cd %s;tar -xvzf *.gz" % (work_dir, work_dir, base_data, work_dir, work_dir)
        run_shell_command(command.replace("//", "/"))
        print('begin to deploy cloud apps for %s' % create_platform_id)
        deploy_platform('base-%s' % create_platform_id, exec_cfgs, exec_params['baseExecSteps'], is_base=True)
    print('begin to deploy ccod platform %s' % create_platform_id)
    deploy_platform(create_platform_id, exec_cfgs, exec_params['execSteps'])
