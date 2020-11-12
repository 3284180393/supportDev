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


reload(sys)
sys.setdefaultencoding('utf8')
LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logging.basicConfig(filename='my.log', level=logging.DEBUG, format=LOG_FORMAT)
gls_service_unit_table = 'GLS_SERVICE_UNIT'


def create_db_connection(host, port, user, pwd, db_name, db_type='oracle'):
    dbconfig = {'host': host,
                port: port,
                'user': user,
                'password': pwd,
                'database': db_name, }
    if db_type == 'mysql':
        conn = pymysql.connector.connect(**dbconfig)
    else:
        raise Exception('unsupported db type %s' % db_type)
    return conn


class Gls_DB(object):

    def __init__(self, user, pwd, ip, port, db_name, db_type='ORACLE'):
        if db_type == 'MYSQL':
            dbconfig = {'host': ip,
                        'port': port,
                        'user': user,
                        'password': pwd,
                        'database': db_name, }

            self.connect = pymysql.connect(host=ip, port=port, user=user, passwd=pwd, db=db_name, charset='utf8')
        elif db_type == 'ORACLE':
            conn_str = "%s/%s@%s:%d/%s" % (user, pwd, ip, port, db_name)
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
    print('nodePort of %s is %s' % (service_name, exec_result))
    node_port = None
    if exec_result and exec_result.isdigit():
        node_port = int(exec_result)
    logging.debug('nodePort of %s''s %s is %s' % (platform_id, service_name, node_port))
    return node_port


def run_shell_command(command, cwd=None):
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


def get_start_param(param_file):
    if os.path.exists(param_file):
        r_open = open(param_file, 'r')
        for line in r_open:
            return json.loads(line)
    else:
        logging.error("%s not exist" % param_file)
        return []


def deploy_platform(deploy_params):
    params = deploy_params['platformParams']
    db_type = params['glsDBType']
    for step in deploy_params['execSteps']:
        command = "kubectl apply -f %s" % step['filePath']
        print(command)
        exec_result = run_shell_command(command)
        print(exec_result)
        if step['timeout'] > 0:
            print('after exec %s, sleep %d seconds' % (command, step['timeout']))
            time.sleep(step['timeout'])
        #       if re.match('^[^/]+/ucds\d*/ucds\d*-.+?-deployment.yml$', step['filePath']):
        if step['filePath'] == 'cloud01/ucds/ucds-cloud01-out-service.yaml':
            # ucds = re.sub('-deployment.yml', '', step['filePath'].split('/')[2])
            ucds = 'ucds-cloud01-out'
            port = get_service_node_port(params['platformId'], params['glsDBService'])
            print('%s started, so update  ucds port in glsserver to %d' % (ucds, port))
            db = Gls_DB(params['glsDBUser'], params['glsDBPwd'], params['k8sHostIp'], port, params['glsDBSid'], db_type)
            if db_type == 'ORACLE':
                update_sql = """update "%s"."%s" set PARAM_UCDS_PORT='%d' where NAME='%s' % (params['glsDBName'], gls_service_unit_table, port, ucds)"""
            else:
                update_sql = """UPDATE %s SET PARAM_UCDS_PORT = '%d' WHERE	NAME = '%s' % (gls_service_unit_table, port, ucds)"""
            db.update(update_sql)
            print('%s port has been updated to %d' % (ucds, port))


if __name__ == '__main__':
    exec_params = get_start_param("start_param.txt")
    print('deploy platform need %d steps' % len(exec_params))
    deploy_platform(exec_params)