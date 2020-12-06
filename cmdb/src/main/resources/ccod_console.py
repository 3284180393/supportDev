# -*- coding: utf-8 -*-

import sys
import os
import logging as logger
import datetime
import time
import re
import subprocess
import json
import signal
import functools
import yaml


class TimeoutError(Exception):
    pass


reload(sys)
sys.setdefaultencoding('utf8')
LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logger.basicConfig(filename='my.log', level=logger.DEBUG, format=LOG_FORMAT)

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
deploy_param_file = 'deploy_param.txt'
deploy_config_file = 'config.yaml'


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


class CCODPlatform:
    """
    用来封装和ccod平台相关功能
    """
    def __init__(self, param_file, cfg_file):
        work_dir = os.getcwd()
        param_file_path = '%s/%s' % (work_dir, param_file)
        if not os.path.exists(param_file) or not os.path.isfile(param_file_path):
            logger.error("param file %s not exist" % param_file_path)
            raise Exception("param file %s not exist" % param_file_path)
        r_open = open(param_file_path, 'r')
        for line in r_open:
            params = json.loads(line)
        params['systemType'] = 'suse'
        self.params = params
        self.system_type = params['systemType']
        apps = []
        for app_cfg in params['deploySteps']:
            app = DomainAppManager(app_cfg, app_cfg['domainId'], work_dir)
            apps.append(app)
        self.apps = apps
        cfg_file_path = '%s/%s' % (work_dir, cfg_file)
        if not os.path.exists(cfg_file_path) or not os.path.isfile(cfg_file_path):
            logger.error("cfg file %s not exist" % cfg_file_path)
            raise Exception("cfg file %s not exist" % cfg_file_path)
        with open(cfg_file_path, 'r') as in_f:
            cfgs = yaml.load(in_f)
        self.config = cfgs

    def test(self):
        for app_cfg in self.apps:
            print('name=%s and alias=%s' % (app_cfg.name, app_cfg.alias))

    def create(self):
        if 'hosts' in self.params and len(self.params['hosts']) > 0:
            for ip in self.params['hosts'].keys():
                cmd = "echo '%s %s' >> /etc/hosts" % (ip, self.params['hosts'][ip])
                SystemUtils.run_shell_command(cmd)
        for app_cfg in self.apps:
            app_cfg.deploy(self.config)


class DomainAppManager:
    """
    用来封装同应用管理相关功能
    """
    def __init__(self, app_cfg, domain_id, work_dir):
        self.name = app_cfg['appName']
        self.alias = app_cfg['alias']
        self.app_type = app_cfg['appType']
        self.package = app_cfg['installPackage']
        if 'initCmd' in app_cfg.keys():
            self.init_cmd = app_cfg['initCmd']
        else:
            self.init_cmd = None
        self.init_cmd = None
        self.start_cmd = app_cfg['startCmd']
        if 'logOutCmd' in app_cfg.keys():
            self.log_cmd = app_cfg['logOutCmd']
        else:
            self.log_cmd = None
        if self.app_type != 'NODEJS':
            self.home = '/home/ccodrunner/%s' % self.alias
            if self.app_type == 'BINARY_FILE' or self.app_type == 'JAR':
                self.start_cmd = 'cd bin;chmod 777 %s;%s' % (self.package['fileName'], app_cfg['startCmd'])
                self.base_path = self.home
            elif self.app_type == 'RESIN_WEB_APP':
                self.start_cmd = 'cd resin;%s' % app_cfg['startCmd']
                self.base_path = '%s/resin' % self.home
                # self.start_cmd = './bin/resin.sh start'
            elif self.app_type == 'TOMCAT_WEB_APP':
                self.start_cmd = 'cd tomcat;%s' % app_cfg['startCmd']
                self.base_path = '%s/tomcat' % self.home
                # self.start_cmd = './bin/startup.sh'
        else:
            self.start_cmd = app_cfg['startCmd']
            self.home = '/usr/share/nginx/html'
            self.base_path = '%s/%s' % (self.home, self.name)
        self.port = app_cfg['ports']
        if 'checkAt' in app_cfg.keys():
            self.check_at = app_cfg['checkAt']
        else:
            self.check_at = None
        self.runtime = app_cfg['runtime']
        if 'timeout' in app_cfg.keys():
            self.timeout = app_cfg['timeout']
        else:
            self.timeout = None
        self.cfgs = app_cfg['cfgs']
        self.domain_id = domain_id
        self.work_dir = work_dir

    def deploy(self, deploy_cfg):
        print('begin to deploy %s(%s)' % (self.alias, self.name))
        try:
            self.__init_runtime(deploy_cfg)
            # if self.init_cmd:
            #     SystemUtils.run_shell_command(self.init_cmd)
            if self.app_type == 'NODEJS':
                SystemUtils.run_shell_command(self.start_cmd, exit_time=5)
            else:
                SystemUtils.run_shell_command(self.start_cmd, self.alias, exit_time=5)
            # if self.log_cmd:
            #     SystemUtils.run_shell_command(self.log_cmd)
        except Exception, e:
            print('部署%s失败:%s' % (self.alias, e))
            logger.error('部署%s异常:%s' % (self.alias, e), exc_info=True)

    def __init_runtime(self, deploy_cfg):
        timestamp = datetime.datetime.now().strftime('%Y%m%d%H%M%S')
        if self.app_type == 'BINARY_FILE':
            self.__init_binary_file_runtime()
        elif self.app_type == 'RESIN_WEB_APP' or self.app_type == 'TOMCAT_WEB_APP':
            self.__init_container_runtime(timestamp, deploy_cfg)
        elif self.app_type == 'JAR':
            self.__init_jar_runtime(deploy_cfg)
        elif self.app_type == 'NODEJS':
            self.__init_nodejs_runtime()
        else:
            raise Exception('unsupport app type %s' % self.app_type)

    def __init_container_runtime(self, timestamp, deploy_cfg):
        """
        初始化容器的运行环境
        :param timestamp: 时间戳
        :return:
        """
        work_dir = self.work_dir
        if 'jdkVersion' in self.runtime.keys():
            jdk = self.runtime['jdkVersion']['version']
        else:
            jdk = 'jdk8'
        if jdk not in deploy_cfg.keys():
            raise Exception('jdk %s not configured' % jdk)
        if self.app_type == 'RESIN_WEB_APP':
            container = 'resin'
        elif self.app_type == 'TOMCAT_WEB_APP':
            container = 'tomcat'
        else:
            raise Exception('not container for %s' % self.app_type)
        if container in self.runtime.keys():
            con_ver = self.runtime[container]['version']
        elif self.app_type == 'RESIN_WEB_APP':
            con_ver = 'resin4'
        else:
            con_ver = 'tomcat6'
        if con_ver not in deploy_cfg.keys():
            raise Exception('container version %s not configured' % con_ver)
        war_path = self.__update_war_cfg(work_dir, timestamp,deploy_cfg)
        SystemUtils.add_user(self.alias, self.home, delete=True)
        cmd = SystemUtils.modify_user_jdk(deploy_cfg[jdk])
        SystemUtils.run_shell_command(cmd, user=self.alias)
        cmd = 'cp %s ./ -R' % deploy_cfg[con_ver]
        SystemUtils.run_shell_command(cmd, user=self.alias)
        cmd = 'cp %s ./%s/webapps/%s-%s.war' % (war_path, container, self.alias, self.domain_id)
        SystemUtils.run_shell_command(cmd, user=self.alias)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd, self.alias)
        if container == 'resin':
            cfg_file = 'resin/conf/resin.xml'
        else:
            cfg_file = 'tomcat/conf/server.xml'
        cmd = """sed -i \\"s/port=\\\\\\"8080\\\\\\"/port=\\\\\\"%s\\\\\\"/g\\" %s""" % (re.sub('/.*', '', self.port), cfg_file)
        SystemUtils.run_shell_command(cmd, user=self.alias)

    def __init_jar_runtime(self, deploy_cfg):
        SystemUtils.add_user(self.alias, self.home, delete=True)
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s' % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        SystemUtils.run_shell_command(re.sub('//', '/', cmd), user=self.alias)
        cmd = SystemUtils.modify_user_jdk(deploy_cfg['jdk8'])
        SystemUtils.run_shell_command(cmd, user=self.alias)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd, user=self.alias)

    def __init_binary_file_runtime(self):
        SystemUtils.add_user(self.alias, self.home, delete=True)
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s' % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        SystemUtils.run_shell_command(re.sub('//', '/', cmd), user=self.alias)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd, user=self.alias)

    def __init_nodejs_runtime(self):
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s' % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        SystemUtils.run_shell_command(re.sub('//', '/', cmd))
        cmd = 'cd %s;tar -xvzf %s' % (self.package['deployPath'], self.package['fileName'])
        SystemUtils.run_shell_command(cmd)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd)

    def __update_war_cfg(self, work_dir, timestamp, deploy_cfg):
        jdk = self.runtime['jdkVersion']['version']
        war_name = self.package['fileName']
        tmp_dir = '%s/%s/%s' % (work_dir, timestamp, self.alias)
        cmd = 'mkdir %s -p;cd %s;cp %s/%s/package/%s ./%s' % (tmp_dir, tmp_dir, work_dir, self.alias, war_name, war_name)
        for cfg in self.cfgs:
            cfg_path = '%s' % (re.sub('.*/WEB-INF/', 'WEB-INF/', cfg['deployPath']))
            cfg_path = re.sub('/$', '', cfg_path)
            cfg_name = cfg['fileName']
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg_path, work_dir, self.alias, cfg_name, cfg_path, cfg_name)
            cmd = '%s;%s/bin/jar uf %s %s/%s' % (cmd, deploy_cfg[jdk], war_name, cfg_path, cfg_name)
        cmd = re.sub('//', '/', cmd)
        SystemUtils.run_shell_command(cmd)
        return '%s/%s' % (tmp_dir, war_name)


class SystemUtils:
    def __init__(self, system_type='centos'):
        self.type = system_type

    @staticmethod
    def run_shell_command(command, user=None, cwd=None, accept_err=False, exit_time=None, timeout=None):
        """
        执行一条脚本命令,并返回执行结果输出
        :param command:执行命令,可执行文件/脚本可以是相对cwd的相对路径
        :param user: 如果需要切换用户执行命令
        :param cwd:执行命令的cwd
        :param accept_err: 是否接受错误输出，如果为True将错误结果输出，否则抛出异常
        :param exit_time: 如果命令在指定时间没有能结束，将正常退出，例如启动一个应用
        :param timeout: 如果指定时间内未能返回将抛出执行超时的异常
        :return:返回结果
        """
        if user:
            command = """su - %s -c \"%s\"""" % (user, command)
        logger.info("准备执行命令:%s" % command)
        if cwd:
            logger.info('cwd=%s' % cwd)
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd,
                                       close_fds=True)
        else:
            process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       close_fds=True)
        t_beginning = time.time()
        while True:
            if process.poll() is not None:
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
            seconds_passed = time.time() - t_beginning
            if exit_time and seconds_passed > exit_time:
                logger.info("命令%s %s秒内无错误返回，执行正常" % (command, seconds_passed))
                return process.stdin.readlines()
            if timeout and seconds_passed > timeout:
                logger.info("命令%s秒内无返回，执行超时" % seconds_passed)
                raise Exception('exec timeout: exec %s %s seconds without return' % (command, seconds_passed))
            time.sleep(0.1)

    @staticmethod
    def add_user(name, home, delete=False):
        """
        向系统添加新的用户
        :param name: 用户名
        :param home: 用户的home
        :param delete: 如果该用户已经存在是删除后新建还是保留
        :return:
        """
        if name == 'root':
            raise Exception('can not create user root')
        if home == '/':
            raise Exception('home of %s can not be /' % name)
        cmd = """cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|awk -F":" '{ print $1 }'|grep -E '^%s$'""" % name
        exec_result = SystemUtils.run_shell_command(cmd)
        if exec_result:
            if not delete:
                logger.debug('%s user exist, not create' % name)
                return
            else:
                logger.debug('%s user exist, delete it first' % name)
                cmd = """ps -u %s | grep -E "\\d+" |awk -F" " '{ print $1 }'""" % name
                exec_result = SystemUtils.run_shell_command(cmd)
                if exec_result:
                    logger.debug('some process still running at user %s, kill them fisrt' % name)
                    cmd = "kill -9 %s" % re.sub("\\n", " ", exec_result)
                    SystemUtils.run_shell_command(cmd)
                cmd = 'userdel -r %s;rm -rf %s' % (name, home)
                SystemUtils.run_shell_command(cmd, accept_err=True)
        cmd = "useradd %s -d %s -m" % (name, home)
        SystemUtils.run_shell_command(cmd)
        logger.debug('user %s been created, home=%s' % (name, home))


    @staticmethod
    def modify_user_jdk(jdk, system_type='suse'):
        if system_type == 'suse':
            res_file = '.profile'
        else:
            res_file = '.bashrc'
        cmd = "echo 'JAVA_HOME=%s' >> %s" % (jdk, res_file)
        cmd = "%s;echo 'PATH=$JAVA_HOME/bin:$PATH' >> %s" % (cmd, res_file)
        cmd = "%s;echo 'CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar' >> %s" % (cmd, res_file)
        cmd = "%s;echo 'export JAVA_HOME' >> %s" % (cmd, res_file)
        cmd = "%s;echo 'export PATH' >> %s" % (cmd, res_file)
        cmd = "%s;echo 'export CLASSPATH' >> %s" % (cmd, res_file)
        cmd = "%s;source %s" % (cmd, res_file)
        return cmd

    def modify_user_env(self, env):
        if self.type == 'suse':
            res_file = '.cshrc'
        else:
            res_file = '.bashrc'
        cmd = ''
        for k in env.keys():
            cmd = "%s;echo '%s=%s' >> %s" % (cmd, k, env[k], res_file)
            cmd = "%s;export %s >> %s" % (cmd, k, res_file)
        cmd = "%s;source %s" % (cmd, res_file)
        return re.sub('^;', '', cmd)


if __name__ == '__main__':
    platform = CCODPlatform(deploy_param_file, deploy_config_file)
    platform.test()
    platform.create()

