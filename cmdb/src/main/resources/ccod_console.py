# -*- coding: utf-8 -*-

import sys
import os
# import logging as logger
from common.log import logger
import datetime
import re
import subprocess
import json
import signal
import functools
import paramiko
import time
import urllib
from paramiko.ssh_exception import AuthenticationException
from ccod_app import models as app_models


class TimeoutError(Exception):
    pass


reload(sys)
sys.setdefaultencoding('utf8')
# LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
# logger.basicConfig(filename='my.log', level=logger.DEBUG, format=LOG_FORMAT)

remote_dir = '/var/tmp/app_manager'
script_24_support_version = '^Python 2\.4\.3$'
script_26_support_version = '^Python 2\.6\.6$'
script_3_support_version = '||'
exec_command_timeout = 20
ssh_connect_timeout = 20
oracle_normal_switch_flows = [('check_primary_oracle_dg_status', '检查primary oracle的dg状态'), ('check_standby_oracle_dg_status', '检查standby oracle的dg状态'), ('confirm_ip_available', '检查用来切换的ip是否可用'), ('put_switch_script_to_remote_server', '创建并上传primary oracle数据库dg切换相关脚本'), ('put_switch_script_to_remote_server', '创建并上传standby oracle数据库dg切换相关 脚本'), ('to_standby', '对主库执行to primary操作'), ('to_primary', '对备库执行to standby操作'), ('change_server_ip', '修改主库ip'), ('change_server_ip', '修改备库ip'), ('change_server_ip', '修改主库ip'), ('mount_standby_oracle', '启动备库'), ('startup_primary_oracle', '启动主库'), ('sync_primary_standby_oracle', '主备同步'), ('table_space_check', '检查表空间')]
win_sys = False
executor_script_name = 'client_switch.py'
agent_script_name = 'switch_agent.py'
port_regex = "^\\d+(\\:\\d+)?/(TCP|UDP)(,\\d+(\\:\\d+)?/(TCP|UDP))*$"
check_at_regex = "^.+/CMD$|^\\d+/TCP$|^\\d+/(HTTP|HTTPS)(\\:.+)?$"
deploy_params = {}


def timeout(seconds, error_message='Timeout Error: the cmd 30s have not finished.'):
    def decorated(func):
        result = ""

        def _handle_timeout(signum, frame):
            global result
            result = error_message
            raise TimeoutError(error_message)

        def wrapper(*args, **kwargs):
            global result
            signal.signal(signal.SIGALRM, _handle_timeout)
            signal.alarm(seconds)

            try:
                result = func(*args, **kwargs)
            finally:
                signal.alarm(0)
                return result
            return result
        return functools.wraps(func)(wrapper)
    return decorated


class ExecutableCommand:
    """
    用来定义一条命令执行
    """
    """
    执行命令时如果有超时要求,用来定义超时时长
    """
    command_exec_timeout = 0

    def __init__(self):
        """
        初始化命令执行类
        """
        pass

    @staticmethod
    def get_init_ret_value(command, command_type, key, desc):
        """
        初始化命令执行返回结果
        :param command: 执行命令
        :param command_type: 命令类型
        :param key: 返回关键字
        :param desc: 命令描述
        :return: 初始化的命令执行结果
        """
        ret = dict()
        ret['start_time'] = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        ret['command'] = command
        ret['desc'] = desc
        ret['key'] = key
        ret['exec_success'] = False
        ret['command_type'] = command_type
        ret[key] = 'UNKNOWN'
        ret['output'] = 'command not execute'
        ret['end_time'] = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        return ret

    @staticmethod
    @timeout(ssh_connect_timeout, "Timeout Error:fail to create connection in " + str(ssh_connect_timeout) + " seconds")
    def create_ssh_connect(host_ip, ssh_port, user, password):
        """
        创建ssh连接
        :param host_ip:目标机器的host ip
        :param ssh_port: ssh端口
        :param user: 登录用户
        :param password: 登录密码
        :return:ret和ssh_client，ret表示创建ssh命令执行结果,如果创建成功ssh_client为创建的ssh客户端,否则ssh_client为None
        """
        logger.info('创建ssh连接host_ip=%s, ssh_port=%d, user=%s, password=%s' % (host_ip, ssh_port, user, password))
        command = 'create ssh connect for ' + user + '@' + host_ip
        key = 'CREATE_SSH_CONNECT'
        desc = '为%s创建在%s的ssh连接' % (user, host_ip)
        ret = ExecutableCommand.get_init_ret_value(command, 'local/runtime', key, desc)
        ssh_client = None
        try:
            ssh_client = paramiko.SSHClient()
            ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            ssh_client.connect(host_ip, ssh_port, user, password)
        except TimeoutError, te:
            logger.error('为%s创建在%s的ssh连接超时:%s' % (command, desc, te))
            ret[key] = 'TIMEOUT'
            ret['output'] = '%s' % te
        except AuthenticationException, ae:
            logger.error('%s登录%s认证失败:%s' % (user, host_ip, ae))
            ret['output'] = str(ae)
            ret[key] = 'AUTH_FAIL'
        except Exception, e:
            logger.error('%s登录%s失败:%s' % (user, host_ip, e), exc_info=True)
            ret['output'] = str(e)
            ret[key] = 'CREATE_CONN_FAIL'
        else:
            ret['output'] = "create ssh connect for " + user + ":SUCC"
            ret[key] = 'CREATE_CONN_SUCCESS'
            ret['exec_success'] = True
            ret['output'] = '连接创建成功'
        ret['end_time'] = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        logger.info('ssh连接创建结果:' + ret[key])
        return ret, ssh_client

    @staticmethod
    def ping_test(host_ip, ping_success, windows):
        """
        ping一个ip检查该ip是否可以ping通
        :param host_ip: 需要被ping的ip
        :param ping_success: 如果ping_success为True则ping通返回成功,否则ping不通返回成功
        :param windows: 如果windows为True表示是在windows下执行ping命令,否则缺省认为是在linux下执行ping命令
        :return: ping测试结果
        """
        logger.info('准备ping ip %s,ping_success=%s,windows=%s' % (host_ip, ping_success, windows))
        if windows is True:
            command = 'ping %s' % host_ip
        else:
            command = 'ping %s -c 4' % host_ip
        key = 'PING'
        desc = '%s是否在线' % host_ip
        ret = ExecutableCommand.get_init_ret_value(command, 'local/runtime', key, desc)
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        out = process.communicate()[0]
        if process.stdin:
            process.stdin.close()
        if process.stdout:
            process.stdout.close()
        if process.stderr:
            process.stderr.close()
        try:
            process.kill()
        except OSError:
            pass
        if windows is True:
            command_result = out.decode('gbk')
        else:
            command_result = out.decode()
        str_info = re.compile("(\n)*$")
        command_result = str_info.sub('', command_result)
        ret['end_time'] = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        ret['output'] = command_result
        ret[key] = 'PING_FAIL'
        if windows is True:
            if re.match(".*TTL=.*", command_result, re.DOTALL):
                ret[key] = 'PING_SUCC'
        else:
            if re.match(".*(ttl=.*time=).*", command_result, re.DOTALL):
                ret[key] = 'PING_SUCC'
        if ping_success is True and ret[key] == 'PING_SUCC':
            ret['exec_success'] = True
        elif ping_success is not True and ret[key] == 'PING_FAIL':
            ret['exec_success'] = True
        logger.info('ping %s, result:%s, exec_success=%s' % (host_ip, ret[key], ret['exec_success']))
        return ret

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
            logger.error('目前还不支持' + command_type + '类型命令结果解析')
            is_match = False
        return is_match

    @staticmethod
    @timeout(command_exec_timeout, error_message='in %s(seconds) without command return' % exec_command_timeout)
    def timeout_ssh_exec_command(conn, command, key, result_regex_map, accept_result, desc, command_type, exec_timeout):
        """
        通过ssh在linux服务器上执行一条命令，命令执行结果会匹配result_regex_map中的正则，
        如果能够匹配某个正则那么返回的key等于正则表达式对应的关键字否则返回失败
        装饰器控制函数决定了执行命令的超时时长
        :param conn: ssh连接会话
        :param command: 需要执行的命令
        :param key: 执行结果对应的key
        :param result_regex_map: 配置命令执行结果的正则表达式
        :param accept_result: 可接受结果，如果执行结果输出匹配result_regex_map并且满足accept_result则认为命令执行成功
        :param desc: 命令描述
        :param command_type: 命令类型，目前支持bash/shell和oracle/sql
        :param exec_timeout: 命令执行超时时长
        :return: {'output', 'command', 'key', key, 'desc', 'output', 'start_time', 'end_time'}
        """
        ExecutableCommand.command_exec_timeout = exec_timeout
        start = datetime.datetime.now()
        logger.info('即将执行%s:%s,key=%s,result_regex_map=%s,accept_result=%s,command_type=%s,timeout=%s' % (desc, command, key, result_regex_map, accept_result, command_type, timeout))
        ret = ExecutableCommand.get_init_ret_value(command, command_type, key, desc)
        try:
            ret = ExecutableCommand.__exec_command_timeout(conn, command, key, result_regex_map, accept_result, desc, command_type)
        except TimeoutError, te:
            logger.error('执行%s(%s)超时:%s' % (command, desc, te))
            ret[key] = 'TIMEOUT'
            ret['output'] = '%s' % te
        except Exception, e:
            logger.error('执行%s(%s)异常:%s' % (command, desc, e))
            ret[key] = 'EXCEPTION'
            ret['output'] = '%s' % e
        now = datetime.datetime.now()
        ret['end_time'] = now.strftime('%Y-%m-%d %H:%M:%S')
        ret['time_usage'] = (now - start).seconds
        logger.info('%s(%s)执行结果:%s=%s,用时%s(秒)' % (command, desc, key, ret[key], ret['time_usage']))
        logger.info('ret=%s' % json.dumps(ret, ensure_ascii=False))
        return ret

    @staticmethod
    def ssh_exec_command(conn, command, key, result_regex_map, accept_result, desc, command_type):
        """
        通过ssh在linux服务器上执行一条命令，命令执行结果会匹配result_regex_map中的正则，
        如果能够匹配某个正则那么返回的key等于正则表达式对应的关键字否则返回失败
        :param conn: ssh连接会话
        :param command: 需要执行的命令
        :param key: 执行结果对应的key
        :param result_regex_map: 配置命令执行结果的正则表达式
        :param accept_result: 可接受结果，如果执行结果输出匹配result_regex_map并且满足accept_result则认为命令执行成功
        :param desc: 命令描述
        :param command_type: 脚本类型目前只支持bash/shell和oracle/sql两类
        :return: {'output', 'command', 'key', key, 'desc', 'output', 'start_time', 'end_time'}
        """
        start = datetime.datetime.now()
        ret = ExecutableCommand.get_init_ret_value(command, command_type, key, desc)
        try:
            stdin, stdout, stderr = conn.exec_command('. ./.bash_profile;' + command)
            exec_result = stdout.read()
            err = stderr.read()
            if err:
                exec_result = err
            ret['output'] = exec_result
            logger.info('%s execute output:%s' % (command, ret['output']))
            for k, v in result_regex_map.iteritems():
                if ExecutableCommand.command_result_regex_match(exec_result, v, command_type):
                    logger.info("命令输出结果满足正则表达式:%s,%s将返回%s" % (v, key, k))
                    ret[key] = k
                    break
            if re.match(accept_result, ret[key]):
                logger.info('%s=%s,满足accept_result=%s要求,命令执行成功' % (key, ret[key], accept_result))
                ret['exec_success'] = True
        except Exception, e:
            logger.error('执行%s(%s)异常:%s' % (command, desc, e))
            ret[key] = 'EXCEPTION'
            ret['output'] = '%s' % e
        now = datetime.datetime.now()
        ret['end_time'] = now.strftime('%Y-%m-%d %H:%M:%S')
        ret['time_usage'] = (now - start).seconds
        logger.info('%s(%s)执行结果:%s=%s,用时%s(秒)' % (command, desc, key, ret[key], ret['time_usage']))
        logger.info('ret=%s' % json.dumps(ret, ensure_ascii=False))
        return ret

    @staticmethod
    def put_file_to_remote_server(host_ip, ssh_port, user, password, src_file_path, dst_file_path):
        """
        将一个文件上传到远端服务器上去
        :param conn: 连接远端的ssh连接
        :param src_file_path: 需要上传的文件路径
        :param dst_file_path: 需要上传的文件路径
        :return: 上传结果，如果上传成功，切换脚本在服务器的存放绝对路径将以switch_script_path为key添加到ora_cfg配置中
        """
        key = 'PUT_FILE_TO_REMOTE_SERVER'
        start = datetime.datetime.now()
        put_ret = ExecutableCommand.get_init_ret_value('put %s to %s@%s' % (src_file_path, dst_file_path, host_ip), 'local/runtime', key, '将%s上传到%s@%s' % (src_file_path, dst_file_path, host_ip))
        if not os.path.exists(src_file_path):
            put_ret['output'] = '需要上传的文件%s不存在' % src_file_path
            put_ret[key] = 'FILE_NOT_EXIST'
            logger.error(put_ret['output'])
            return put_ret
        create_ret, conn = ExecutableCommand.create_ssh_connect(host_ip, ssh_port, user, password)
        if create_ret['exec_success'] is not True:
            logger.error('上传文件失败,创建ssh连接失败:%s' % create_ret[create_ret['key']])
            put_ret[key] = 'CREATE_SSH_FAIL'
            now = datetime.datetime.now()
            put_ret['time_usage'] = (now - start).seconds
            return put_ret
        str_info = re.compile('/[^/]+$')
        save_dir = str_info.sub('', dst_file_path)
        logger.info('准备创建%s目录' % save_dir)
        conn.exec_command('mkdir -p %s' % save_dir)
        conn.close()
        create_ret, conn = ExecutableCommand.create_ssh_connect(host_ip, ssh_port, user, password)
        if create_ret['exec_success'] is not True:
            logger.error('上传文件失败,创建ssh连接失败:%s' % create_ret[create_ret['key']])
            put_ret[key] = 'CREATE_SSH_FAIL'
            now = datetime.datetime.now()
            put_ret['time_usage'] = (now - start).seconds
            return put_ret
        logger.info('准备将%s上传到%s@%s' % (src_file_path, dst_file_path, host_ip))
        try:
            sftp = conn.open_sftp()
            sftp.put(src_file_path, dst_file_path)
            put_ret['exec_success'] = True
            put_ret['output'] = '将%s上传到%s@%s成功' % (src_file_path, dst_file_path, host_ip)
            put_ret[key] = 'PUT_FILE_SUCCESS'
        except Exception, e:
            print(e)
            logger.error('上传文件失败:%s' % e, exc_info=True)
            put_ret[key] = 'PUT_FILE_FAIL'
            put_ret['out_put'] = e.args[0]
        now = datetime.datetime.now()
        put_ret['time_usage'] = (now - start).seconds
        logger.info('将%s上传到%s@%s:%s,用时%s(秒)' % (src_file_path, dst_file_path, host_ip, put_ret[key], put_ret['time_usage']))
        return put_ret


class CCODPlatform:
    """
    用来封装和ccod平台相关功能
    """
    def __init__(self, param_file):
        if not os.path.exists(param_file) or os.path.isfile(param_file):
            logger.error("param file %s not exist" % param_file)
            raise Exception("param file %s not exist" % param_file)
        r_open = open(param_file, 'r')
        for line in r_open:
            self.params = json.loads(line)


class DomainAppManager:
    """
    用来封装同应用管理相关功能
    """
    def __init__(self, app_cfg, domain_id, work_dir):
        self.name = app_cfg['appName']
        self.alias = app_cfg['alias']
        self.app_type = app_cfg['appType']
        self.init_cmd = app_cfg['initCmd']
        self.start_cmd = app_cfg['startCmd']
        self.log_cmd = app_cfg['logOutCmd']
        if self.app_type != 'NODEJS':
            self.home = '/home/ccodrunner/%s' % self.alias
            if self.app_type == 'BINARY_FILE' or self.app_type == 'JAR':
                self.base_path = self.home
            elif self.app_type == 'RESIN_WEB_APP':
                self.base_path = '%s/resin' % self.home
            elif self.app_type == 'TOMCAT_WEB_APP':
                self.base_path = '%s/tomcat' % self.home
        else:
            self.home = '/usr/share/nginx/html'
            self.base_path = '%s/%s' % (self.home, self.name)
        self.port = app_cfg['ports']
        self.check_at = app_cfg['checkAt']
        self.runtime = app_cfg['runtime']
        self.timeout = app_cfg['timeout']
        self.cfgs = app_cfg['cfgs']
        self.package = app_cfg['installPackage']
        self.domain_id = domain_id
        self.work_dir = work_dir

    def deploy(self):
        self.__init_runtime()
        if self.init_cmd:
            SystemUtils.run_shell_command(self.init_cmd)
        SystemUtils.run_shell_command(self.start_cmd)
        if self.log_cmd:
            SystemUtils.run_shell_command(self.log_cmd)

    def __init_runtime(self):
        timestamp = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
        if self.app_type == 'BINARY_FILE':
            self.__init_binary_file_runtime()
        elif self.app_type == 'RESIN_WEB_APP' or self.app_type == 'TOMCAT_WEB_APP':
            self.__init_container_runtime(timestamp)
        elif self.app_type == 'JAR':
            self.__init_jar_runtime()
        elif self.app_type == 'NODEJS':
            self.__init_nodejs_runtime()
        else:
            raise Exception('unsupport app type %s' % self.app_type)

    def __init_container_runtime(self, timestamp):
        """
        初始化容器的运行环境
        :param timestamp: 时间戳
        :return:
        """
        work_dir = self.work_dir
        if 'jdk' in self.runtime.keys():
            jdk = self.runtime['jdk']
        else:
            jdk = 'jdk8'
        if jdk not in deploy_params.keys():
            raise Exception('jdk %s not configured' % jdk)
        if self.app_type == 'RESIN_WEB_APP':
            container = 'resin'
        elif self.app_type == 'TOMCAT_WEB_APP':
            container = 'tomcat'
        else:
            raise Exception('not container for %s' % self.app_type)
        if container in self.runtime.keys():
            con_ver = self.runtime[container]
        elif self.app_type == 'RESIN_WEB_APP':
            con_ver = 'resin4'
        else:
            con_ver = 'tomcat6'
        if con_ver not in deploy_params.keys():
            raise Exception('container version %s not configured' % con_ver)
        war_path = self.__update_war_cfg(work_dir, timestamp)
        SystemUtils.add_user(self.alias, self.home)
        jdk_env_cmd = SystemUtils.modify_user_jdk(jdk)
        cmd = 'su - %s;%s;cp %s/%s ./%s -R;cp %s ./resin/webapps/%s-%s.war' \
              % (self.alias, jdk_env_cmd, deploy_params[con_ver], container, container, war_path, self.alias, self.domain_id)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = '%s;%s' % (cmd, SystemUtils.modify_user_env(self.runtime['env']))
        SystemUtils.run_shell_command(cmd)

    def __init_jar_runtime(self):
        SystemUtils.add_user(self.alias, self.home)
        cmd = 'su - %s' % self.alias
        cmd = '%s;mkdir %s -p;cp %s/%s/package/%s %s/%s' % (cmd, self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        cmd = '%s;%s' % (cmd, SystemUtils.modify_user_jdk(deploy_params['jdk8']))
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = '%s;%s' % (cmd, SystemUtils.modify_user_env(self.runtime['env']))
        SystemUtils.run_shell_command(cmd)

    def __init_binary_file_runtime(self):
        SystemUtils.add_user(self.alias, self.home)
        cmd = 'su - %s' % self.alias
        cmd = '%s;mkdir %s -p;cp %s/%s/package/%s %s/%s' % (cmd, self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = '%s;%s' % (cmd, SystemUtils.modify_user_env(self.runtime['env']))
        SystemUtils.run_shell_command(cmd)

    def __init_nodejs_runtime(self):
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s' % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = '%s;%s' % (cmd, SystemUtils.modify_user_env(self.runtime['env']))
        SystemUtils.run_shell_command(cmd)

    def __update_war_cfg(self, work_dir, timestamp):
        war_name = self.package['fileName']
        tmp_dir = '%s/%s/%s' % (work_dir, timestamp, self.alias)
        cmd = 'mkdir %s -p;cd %s,cp %s/%s/package/%s ./%s' % (tmp_dir, tmp_dir, work_dir, self.alias, war_name, war_name)
        for cfg in self.cfgs:
            cfg_path = '%s' % (re.sub('.*/WEB-INF/', 'WEB-INF/', cfg['deployPath']))
            cfg_path = re.sub('/$', '', cfg_path)
            cfg_name = cfg['fileName']
            cmd = '%s;mkdir %s;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg_path, work_dir, self.alias, cfg_name, cfg_path, cfg_name)
            cmd = '%s;jar uf %s %s/%s' % (cmd, war_name, cfg_path, cfg_name)
        SystemUtils.run_shell_command(cmd)
        return '%s/%s' % (dir, war_name)


class SystemUtils:
    def __init__(self):
        pass

    @staticmethod
    def run_shell_command(command, cwd=None, accept_err=False):
        """
        执行一条脚本命令,并返回执行结果输出
        :param command:执行命令,可执行文件/脚本可以是相对cwd的相对路径
        :param cwd:执行命令的cwd
        :param accept_err: 是否接受错误输出，如果为True将错误结果输出，否则抛出异常
        :return:返回结果
        """
        logger.info("准备执行命令:%s" % command)
        if cwd:
            logger.info('cwd=%s' % cwd)
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd,
                                       close_fds=True)
        else:
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       close_fds=True)
        out, err = process.communicate()
        if process.stdin:
            process.stdin.close()
        if process.stdout:
            process.stdout.close()
        if process.stderr:
            process.stderr.close()
        if err:
            if accept_err:
                exec_result = err
            else:
                raise Exception(err)
        else:
            exec_result = out
        logger.info("命令执行结果\n:%s" % exec_result)
        return exec_result

    @staticmethod
    def add_user(name, home):
        cmd = """cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|awk -F":" '{ print $1 }'|grep -E '^%s$'""" % name
        exec_result = SystemUtils.run_shell_command(cmd)
        if exec_result:
            logger.debug('%s user exist, not be created' % name)
        else:
            cmd = "adduser %s -d %s" % (name, home)
            SystemUtils.run_shell_command(cmd)
            logger.debug('user %s been created, home=%s' % (name, home))

    @staticmethod
    def modify_user_jdk(jdk):
        cmd = "echo 'JAVA_HOME=%s' >> ~/.bashrc" % jdk
        cmd = "%s;echo 'PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc" % cmd
        cmd = "%s;echo 'CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar' >> ~/.bashrc" % cmd
        cmd = "%s;echo 'export JAVA_HOME' >> ~/.bashrc" % cmd
        cmd = "%s;echo 'export PATH' >> ~/.bashrc" % cmd
        cmd = "%s;echo 'export CLASSPATH' >> ~/.bashrc" % cmd
        cmd = "%s;source ~/.bashrc" % cmd
        return cmd

    @staticmethod
    def modify_user_env(env):
        cmd = ''
        for k in env.keys():
            cmd = "%s;echo '%s=%s' >> ~/.bashrc" % (cmd, k, env[k])
            cmd = "%s;export %s" % (cmd, k)
        cmd = "%s;source ~/.bashrc" % cmd
        return re.sub('^;', '', cmd)


if __name__ == '__main__':
    pass



