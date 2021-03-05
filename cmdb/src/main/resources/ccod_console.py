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
import telnetlib
import nginx
from redis.sentinel import Sentinel


class TimeoutError(Exception):
    pass


reload(sys)
sys.setdefaultencoding('utf8')
LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"
logger.basicConfig(filename='my.log', level=logger.DEBUG, format=LOG_FORMAT)
# define a Handler which writes INFO messages or higher to the sys.stderr
#
console = logger.StreamHandler()
console.setLevel(logger.DEBUG)
# set a format which is simpler for console use
#设置格式
formatter = logger.Formatter('%(name)-2s: %(levelname)-2s %(message)s')
# tell the handler to use this format
#告诉handler使用这个格式
console.setFormatter(formatter)
# add the handler to the root logger
#为root logger添加handler
logger.getLogger('').addHandler(console)


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
        self.params = params
        if 'systemType' in params.keys():
            self.system_type = params['systemType']
        else:
            self.system_type = 'centos'
        self.system_type = 'centos'
        apps = []
        for app_cfg in params['deploySteps']:
            app = DomainAppManager(app_cfg, app_cfg['domainId'], work_dir)
            apps.append(app)
        self.apps = apps
        if 'routes' not in params.keys():
            raise Exception('routes not defined in deploy params')
        self.routes = params['routes']
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
        work_dir = os.getcwd()
        if 'domainPublicCfgs' in self.params.keys():
            self.__handle_public_cfgs(self.params['domainPublicCfgs'], False, work_dir)
        if 'platformPublicCfgs' in self.params.keys():
            self.__handle_public_cfgs(self.params['platformPublicCfgs'], True, work_dir)
        for app_cfg in self.apps:
            app_cfg.deploy(self.config)
        # self.config_nginx()

    def __handle_public_cfgs(self, public_cfgs, is_platform, work_dir):
        cmd = ''
        tag = 'domain'
        if is_platform:
            tag = 'platform'
        for cfg in public_cfgs:
            if '/tomcat/' in cfg['deployPath']:
                for key in self.config.keys():
                    if 'tomcat' in key:
                        cmd = '%s;cp %s/%s/%s %s/conf' % (cmd, work_dir, tag, cfg['fileName'], self.config[key])
            elif '/resin/' in cfg['deployPath']:
                for key in self.config.keys():
                    if 'resin' in key:
                        cmd = '%s;cp %s/%s/%s %s/conf' % (cmd, work_dir, tag, cfg['fileName'], self.config[key])
            else:
                cmd = '%s;mkdir %s -p;cp %s/%s/%s %s/%s' % (cmd, cfg['deployPath'], work_dir, tag, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        if cmd:
            SystemUtils.run_shell_command(re.sub('^;', '', cmd))

    def redeploy_apps(self, apps=None):
        app_list = self.get_apps(apps)
        for app_cfg in app_list:
            app_cfg.deploy(self.config)

    def restart_apps(self, apps=None):
        """
        重启指定应用模块，如果没有指定将重启所有的模块
        :param apps: 指定重启的应用模块
        :return: 重启后的状态
        """
        app_list = self.get_apps(apps)
        for app_cfg in app_list:
            app_cfg.restart()

    def restart_app(self, alias, init_cmd=None, start_cmd=None):
        app_cfg = self.get_app_cfg(alias)
        if not app_cfg:
            raise Exception('%s not exist' % alias)
        app_cfg.restart(init_cmd=init_cmd, start_cmd=start_cmd)

    def get_app_cfg(self, alias):
        for app_cfg in self.apps:
            if app_cfg.alias == alias:
                return app_cfg
        return None

    def list_status(self, apps=None):
        """
        列出指定模块的状态，如果未指定模块将列出所有
        :param apps: 指定的应用模块
        :return: 指定模块的运行状态
        """
        result_list = []
        if not apps:
            app_list = self.apps
        else:
            app_list = []
            for app in apps:
                app_cfg = self.get_app_cfg(app)
                if not app_cfg:
                    result_list.append((False, '%s not exit' % app))
                else:
                    app_list.append(app_cfg)
        for app_cfg in app_list:
            status, desc = app_cfg.check_status()
            result_list.append((status, desc))
        return result_list

    def config_nginx(self):
        if not self.routes:
            logger.error("not find any route info in deploy params")
            return
        nginx_cfg = self.config['nginx']
        http_listen = nginx_cfg['listen']['http']
        https_listen = nginx_cfg['listen']['https']
        conf_path = nginx_cfg['conf']
        print(conf_path)
        c = nginx.loadf(conf_path)
        http_server = None
        https_server = None
        for server in c.servers:
            for key in server.keys:
                if key.name == 'listen' and key.value == '%s' % http_listen:
                    http_server = server
                elif key.name == 'listen' and key.value == '%s' % https_listen:
                    https_server = server
        for route in self.routes:
            location = self.__get_location(route)
            self.__add_location(location, http_server, http_listen)
            self.__add_location(location, https_server, https_listen)
        nginx.dumpf(c, conf_path)
        logger.debug('reload nginx')
        SystemUtils.run_shell_command('nginx -s reload')
        return c

    def __get_location(self, route):
        location = nginx.Location(
            '%s' % route['path'],
            nginx.Key('proxy_pass', route['proxy_pass'])
        )
        headers = ['Host $host', 'X-Real-IP $remote_addr', 'X-Forwarded-For $proxy_add_x_forwarded_for']
        for header in headers:
            location.add(nginx.Key('proxy_set_header', header))
        return location

    def __add_location(self, location, server, listen):
        if location and server:
            for loc in server.locations:
                if loc.value == location.value:
                    logger.debug('%s exist at %s listen, remove it' % (loc.value, listen))
                    server.remove(loc)
                    break
            logger.debug('%s add to %s listen' % (loc.value, listen))
            server.add(location)

    def get_apps(self, apps=None):
        if not apps:
            return self.apps
        app_list = []
        for alias in apps:
            app_cfg = self.get_app_cfg(alias)
            if not app_cfg:
                raise Exception('%s not exist' % alias)
            app_list.append(app_cfg)
        return app_list


class DomainAppManager:
    """
    用来封装同应用管理相关功能
    """
    def __init__(self, app_cfg, domain_id, work_dir):
        self.name = app_cfg['appName']
        self.alias = app_cfg['alias']
        self.app_type = app_cfg['appType']
        self.package = app_cfg['installPackage']
        self.version = app_cfg['version']
        self.ip = app_cfg['hostIp']
        if 'initCmd' in app_cfg.keys():
            self.init_cmd = app_cfg['initCmd']
        else:
            self.init_cmd = None
        if self.init_cmd and 'wget ' in self.init_cmd:
            self.init_cmd = None
        # self.init_cmd = None
        self.start_cmd = app_cfg['startCmd']
        if 'logOutCmd' in app_cfg.keys():
            self.log_cmd = app_cfg['logOutCmd']
        else:
            self.log_cmd = None
        if self.app_type != 'NODEJS':
            self.home = '/home/ccodrunner/%s' % self.alias
            if self.app_type == 'BINARY_FILE':
                self.start_cmd = 'cd bin;%s' % app_cfg['startCmd']
            elif self.app_type == 'JAR':
                self.start_cmd = app_cfg['startCmd']
            elif self.app_type == 'RESIN_WEB_APP':
                self.start_cmd = 'cd resin;%s' % app_cfg['startCmd']
            elif self.app_type == 'TOMCAT_WEB_APP':
                self.start_cmd = 'cd tomcat;%s' % app_cfg['startCmd']
        else:
            self.start_cmd = app_cfg['startCmd']
            self.home = '/usr/share/nginx/html'
        self.start_cmd = app_cfg['startCmd']
        self.port = app_cfg['ports']
        if 'checkAt' in app_cfg.keys():
            self.check_at = app_cfg['checkAt']
        else:
            self.check_at = None
        if 'runtime' in app_cfg.keys():
            self.runtime = app_cfg['runtime']
        else:
            self.runtime = {}
        if 'timeout' in app_cfg.keys():
            self.timeout = app_cfg['timeout']
        else:
            self.timeout = None
        self.cfgs = app_cfg['cfgs']
        self.domain_id = domain_id
        self.work_dir = work_dir

    def deploy(self, deploy_cfg):
        logger.debug('begin to deploy %s(%s)' % (self.alias, self.name))
        if self.app_type != 'NODEJS':
            user = self.alias
        else:
            user = None
        try:
            self.init_runtime(deploy_cfg)
            if self.init_cmd and 'keytool' not in self.init_cmd:
                SystemUtils.run_shell_command(self.init_cmd, user=user)
            self.start()
        except Exception, e:
            print('部署%s失败:%s' % (self.alias, e))
            logger.error('部署%s异常:%s' % (self.alias, e), exc_info=True)

    def start(self, timeout=30):
        t_beginning = time.time()
        user = self.alias
        if self.app_type == 'BINARY_FILE':
            start_cmd = 'cd %s;%s' % (self.package['deployPath'], self.start_cmd)
        elif self.app_type == 'JAR':
            port = re.sub(':.*', '', self.port)
            port = re.sub('/.*', '', port)
            start_cmd = 'cd %s;%s --server.port %s' % (self.package['deployPath'], self.start_cmd, port)
        elif self.app_type == 'RESIN_WEB_APP':
            start_cmd = 'cd resin;%s' % self.start_cmd
        elif self.app_type == 'TOMCAT_WEB_APP':
            start_cmd = 'cd tomcat;%s' % self.start_cmd
        elif self.app_type == 'NODEJS':
            start_cmd = self.start_cmd
            user = None
        else:
            raise Exception('app_type %s can not be started' % self.app_type)
        SystemUtils.exec_shell_command(start_cmd, user=user, exit_time=4)
        if not timeout:
            logger.info('%s(%s) start success and not check status' % (self.alias, self.name))
            return True
        started = False
        while True:
            seconds_passed = time.time() - t_beginning
            if self.check_status():
                started = True
                break
            logger.debug('%s(%s) not started at %d' % (self.alias, self.name, seconds_passed))
            if seconds_passed >= timeout:
                logger.error('%s(%s) start timeout with timeUsage=%d' % (self.alias, self.name, seconds_passed))
                started = False
                break
            time.sleep(3)
        print('\n\n\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%')
        logger.info('%s(%s) start %s with timeUsage=%d' % (self.alias, self.name, started, seconds_passed))
        print('%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n\n')
        return started

    def restart(self, init_cmd=None, start_cmd=None):
        SystemUtils.kill_process(self.alias)
        if self.app_type != 'NODEJS':
            user = self.alias
        else:
            user = None
        if init_cmd:
            SystemUtils.run_shell_command(init_cmd, user=user)
        elif self.init_cmd:
            SystemUtils.run_shell_command(self.init_cmd, user=user)
        if start_cmd:
            SystemUtils.run_shell_command(start_cmd, user=user, exit_time=5)
        else:
            SystemUtils.run_shell_command(self.start_cmd, user=user, exit_time=5)

    def init_runtime(self, deploy_cfg):
        if self.app_type != 'NODEJS':
            SystemUtils.add_user(self.alias, self.home, delete=True)
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
            raise Exception('unsupported app type %s' % self.app_type)

    def check_status(self):
        started = False
        for port_str in self.port.split(','):
            port = re.sub(':.*', '', port_str)
            port = re.sub('/.*', '', port)
            if self.app_type == 'BINARY_FILE' and port == '80':
                started = True
                break
            if SystemUtils.scan_port('127.0.0.1', int(port)):
                started = True
                break
        return started

    # def check_status(self):
    #     info = '%s[%s(%s)]' % (self.alias, self.name, self.version)
    #     if self.app_type == 'NODEJS':
    #         return False, '%s : UNKNOWN' % info
    #     pids = SystemUtils.list_process(self.alias)
    #     if not pids:
    #         return False, '%s: STOP' % info
    #     check_port = None
    #     check_cmd = None
    #     if not self.check_at:
    #         check_port = int(re.sub('/.*', '', self.port))
    #     else:
    #         if re.match('.*/CMD', self.check_at):
    #             check_cmd = re.sub('/CMD$', '', self.check_at)
    #         else:
    #             check_port = int(re.sub('/[^/].+$', '', self.check_at))
    #     if check_port:
    #         scan_result = SystemUtils.scan_port('127.0.0.1', check_port)
    #         if scan_result:
    #             return True, '%s: port %d is open' % (info, check_port)
    #         else:
    #             return False, '%s: port %d is not open' % (info, check_port)
    #     try:
    #         SystemUtils.run_shell_command(check_cmd, user=self.alias)
    #         return True, '%s: OK' % info
    #     except Exception as e:
    #         return False, '%s: ERROR' % info

    def get_nginx_location(self, nginx_cfg):
        if self.app_type == 'JAR' or self.app_type == 'RESIN_WEB_APP' or self.app_type == 'TOMCAT_WEB_APP':
            location = nginx.Location(
                '=/%s-%s' % (self.alias, self.domain_id),
                nginx.Key('proxy_pass', 'http://%s:%s/%s-%s' % (self.ip, re.sub('/.*', '', self.port), self.alias, self.domain_id))
            )
            for header in nginx_cfg['proxy-set-header']:
                location.add(nginx.Key('proxy_set_header', header))
            return location
        elif self.app_type != 'NODEJS':
            location = nginx.Location(
                '=/%s-%s' % (self.alias, self.domain_id),
                nginx.Key('root', self.home),
                nginx.Key('index', 'index.html index.htm')
            )
            return location
        else:
            return None

    def __str_replace(self, src_str, dst_str, cfg_file_path, is_str=True):
        if is_str:
            cmd = """sed -i \\"s/%s/%s/g\\" %s""" % (src_str, dst_str, cfg_file_path)
        SystemUtils.run_shell_command(cmd, user=self.alias)

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
        cmd = SystemUtils.modify_user_jdk(deploy_cfg[jdk])
        SystemUtils.run_shell_command(cmd, user=self.alias)
        cmd = 'cp %s ./ -R' % deploy_cfg[con_ver]
        SystemUtils.run_shell_command(cmd, user=self.alias)
        cmd = 'cp %s ./%s/webapps/%s-%s.war' % (war_path, container, self.alias, self.domain_id)
        SystemUtils.run_shell_command(cmd, user=self.alias)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd, self.alias)
        http_port = int(re.sub('/.*', '', self.port))
        if container == 'resin':
            self.__str_replace('8080', '%d' % http_port, 'resin/conf/resin.xml')
            self.__str_replace('6800', '%d' % (http_port-8080+6800), 'resin/conf/resin.xml')
            self.__str_replace('6800', '%d' % (http_port-8080+6800), 'resin/conf/local_server.xml')
            self.__str_replace('6600', '%d' % (http_port-8080+6600), 'resin/conf/local_server.xml')
            self.__str_replace('9999', '%d' % (http_port-8080+9999), 'resin/conf/local_jvm.xml')
        else:
            self.__str_replace('8080', '%d' % http_port, 'tomcat/conf/server.xml')
            self.__str_replace('8443', '%d' % (http_port-8080+8443), 'tomcat/conf/server.xml')
            self.__str_replace('8009', '%d' % (http_port-8080+8009), 'tomcat/conf/server.xml')
            self.__str_replace('8005', '%d' % (http_port-8080+8005), 'tomcat/conf/server.xml')

    def __init_jar_runtime(self, deploy_cfg):
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s;chmod 777 %s/%s' \
              % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'], self.package['deployPath'], self.package['fileName'])
        for cfg in self.cfgs:
            cmd = '%s;mkdir %s -p;cp %s/%s/cfg/%s %s/%s' % (cmd, cfg['deployPath'], self.work_dir, self.alias, cfg['fileName'], cfg['deployPath'], cfg['fileName'])
        SystemUtils.run_shell_command(re.sub('//', '/', cmd), user=self.alias)
        cmd = SystemUtils.modify_user_jdk(deploy_cfg['jdk8'])
        SystemUtils.run_shell_command(cmd, user=self.alias)
        if 'env' in self.runtime.keys() and len(self.runtime['env']) > 0:
            cmd = SystemUtils.modify_user_env(self.runtime['env'])
            SystemUtils.run_shell_command(cmd, user=self.alias)

    def __init_binary_file_runtime(self):
        cmd = 'mkdir %s -p;cp %s/%s/package/%s %s/%s;chmod 777 %s/%s' \
              % (self.package['deployPath'], self.work_dir, self.alias, self.package['fileName'], self.package['deployPath'], self.package['fileName'], self.package['deployPath'], self.package['fileName'])
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
    def user_exist(user):
        exec_command = """cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|awk -F":" '{ print $1 }'|grep -E '^%s$'"""
        exec_result = SystemUtils.run_shell_command(exec_command)
        if exec_result:
            exist = True
        else:
            exist = False
        logger.debug('%s exist:%s' %(user, exist))
        return exist

    @staticmethod
    def exec_shell_command(command, user=None, cwd=None, accept_err=False, exit_time=None, timeout=None):
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
        process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=cwd,
                                   close_fds=True)
        t_beginning = time.time()
        while True:
            if process.poll() is not None:
                # exec_result = process.stdout.read()
                if process.stdin:
                    process.stdin.close()
                if process.stdout:
                    process.stdout.close()
                if process.stderr:
                    process.stderr.close()
                logger.info("命令执行结果\n:%s" % "success")
                return 'success'
            seconds_passed = time.time() - t_beginning
            if exit_time and seconds_passed > exit_time:
                logger.info("命令%s %s秒内无错误返回，执行正常" % (command, seconds_passed))
                # return process.stdin.readlines()
                return "running"
            if timeout and seconds_passed > timeout:
                logger.info("命令%s秒内无返回，执行超时" % seconds_passed)
                raise Exception('exec timeout: exec %s %s seconds without return' % (command, seconds_passed))
            time.sleep(0.1)

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
                return "running"
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
        if not name:
            raise Exception('user to be created can not be None')
        elif name == 'root':
            raise Exception('can not create user root')
        elif not home:
            raise Exception('home of %s can not be None')
        elif home == '/':
            raise Exception('home of %s can not be /' % name)
        cmd = """cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|awk -F":" '{ print $1 }'|grep -E '^%s$'""" % name
        exec_result = SystemUtils.run_shell_command(cmd)
        if exec_result:
            if not delete:
                logger.debug('%s user exist, not create' % name)
                return
            else:
                logger.debug('%s user exist, delete it first' % name)
                cmd = 'pkill -9 -u %s' % name
                SystemUtils.run_shell_command(cmd)
                cmd = 'userdel -r %s;rm -rf %s' % (name, home)
                SystemUtils.run_shell_command(cmd, accept_err=True)
        cmd = "useradd %s -d %s -m" % (name, home)
        SystemUtils.run_shell_command(cmd)
        logger.debug('user %s been created, home=%s' % (name, home))

    @staticmethod
    def kill_process(user):
        """
        如果用户下面有进程正在运行，kill掉该用户下所有进程
        :param user:
        :return:
        """
        if not user:
            raise Exception('user of been kill process can not be blank')
        elif user == 'root':
            raise Exception('can not kill process for root')
        cmd = """pkill -9 -u %s""" % user
        exec_result = SystemUtils.run_shell_command(cmd)
        if exec_result:
            logger.debug('pid %s for %s still running, kill them' % (re.sub('\\n', ' ', exec_result), user))
            cmd = 'kill -9 %s' % re.sub('\\n', ' ', exec_result)
            SystemUtils.run_shell_command(cmd, user=user)
        else:
            logger.debug('%s has not any process running' % user)

    @staticmethod
    def list_process(user):
        if not user:
            raise Exception('user can not be None')
        cmd = """ps -u %s |awk -F" " '{ print $1 }' | grep -v 'PID'""" % user
        exec_result = SystemUtils.run_shell_command(cmd)
        if not exec_result:
            logger.debug('%s has not any process running' % user)
            return None
        else:
            logger.debug('%s has pid %s running' % (user, re.sub('\\n', ' ', exec_result)))
            return re.sub('\\n', ' ', exec_result)

    @staticmethod
    def get_pid_info(user, pid):
        cmd = "ps -aux| grep -E ' %s '| grep '^%s ' | grep -v grep" % (pid, user)
        exec_result = SystemUtils.run_shell_command(cmd)
        if not exec_result:
            return None
        return exec_result

    @staticmethod
    def modify_user_jdk(jdk, system_type='suse'):
        if system_type == 'suse':
            res_file = '.profile'
        else:
            res_file = '.bashrc'
        res_file = '.bashrc'
        cmd = "echo 'JAVA_HOME=%s' >> %s" % (jdk, res_file)
        cmd = "%s;echo 'PATH=\\$JAVA_HOME/bin:\\$PATH' >> %s" % (cmd, res_file)
        cmd = "%s;echo 'CLASSPATH=.:\\$JAVA_HOME/lib/dt.jar:\\$JAVA_HOME/lib/tools.jar' >> %s" % (cmd, res_file)
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

    @staticmethod
    def scan_port(ip, port):
        try:
            server = telnetlib.Telnet()
            server.open(ip, port)
            logger.info('port %d at %s is open' % (port, ip))
            return True
        except Exception as e:
            logger.error('port %d at %s not open' % (port, ip))
            return False

    @staticmethod
    def redis_connect_test(redis_hosts, method='sentinel', password=None):
        if method != 'sentinel':
            raise Exception('only support sentinel method')
        if not password:
            raise Exception('pwd can not be None')
        hosts = []
        for host in redis_hosts:
            host = (host.split(':')[0], host.split(':')[1])
            hosts.append(host)
        sentinel = Sentinel(hosts, socket_timeout=0.5)
        sentinel.discover_master('server-1M')
        master = sentinel.master_for('server-1M', socket_timeout=0.5, password=password, db=15)
        master.get('foo')



def show_help():
    print(u'错误的输入格式:')
    print(u'python ccod.py create: 完成域模块部署')
    print(u'pythron ccod.py restart: 重新启动所有的域模块')
    print(u'python ccod.py restart app1 app2...appN: 将顺序重启指定的域模块，app1、app2..为指定域模块的别名')
    print(u'python ccod.py deploy app1 app2...appN: 将顺序重新部署指定的域模块，app1、app2..为指定域模块的别名')
    print(u'python ccod.py list: 列出所有域模块的当前状态')
    print(u'python ccod.py list app1 app2...appN: 列出指定的域模块当前状态，app1、app2..为指定域模块的别名')
    print(u'python ccod.py nginx: 更新域模块相关nginx配置')


if __name__ == '__main__':
    # test_hosts = ['redis-headless:26379', 'redis-headless:26379', 'redis-headless:26379']
    test_hosts = ['10.130.41.218:31302']
    SystemUtils.redis_connect_test(test_hosts, password='ccodredis')
    platform = CCODPlatform(deploy_param_file, deploy_config_file)
    current_domain_id = platform.apps[0].domain_id
    print('**********************************')
    if len(sys.argv) == 2 and sys.argv[1] == 'create':
        platform.test()
        print('begin to create domain %s' % current_domain_id)
        platform.create()
        print('create domain %s finish' % current_domain_id)
    elif len(sys.argv) >= 2 and sys.argv[1] == 'restart':
        if len(sys.argv) == 2:
            print('begin to restart all apps at domain %s' % current_domain_id)
            input_list = None
        else:
            print('begin to restart %s at domain %s' % (','.join(sys.argv[2:]), current_domain_id))
            input_list = sys.argv[2:]
        platform.restart_apps(input_list)
    elif len(sys.argv) >= 3 and sys.argv[1] == 'deploy':
        platform.redeploy_apps(sys.argv[2:])
    elif len(sys.argv) >= 2 and sys.argv[1] == 'list':
        if len(sys.argv) == 2:
            print('begin to check all apps status at domain %s' % current_domain_id)
            input_list = None
        else:
            print('begin to check %s status at domain %s' % (','.join(sys.argv[2:]), current_domain_id))
            input_list = sys.argv[2:]
        apps_status_result = platform.list_status(input_list)
        print('**********************************')
        for app_status_result in apps_status_result:
            print(app_status_result[1])
    elif len(sys.argv) == 2 and sys.argv[1] == 'nginx':
        print('begin to update nginx for domain %s' % platform.apps[0].domain_id)
        platform.config_nginx()
        print('update nginx of domain %s finish' % platform.apps[0].domain_id)
    else:
        show_help()
    print('**********************************')
