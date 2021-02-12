import time
import random
import os
import requests
import threading

amount = 10

ip = 'localhost'
# ip = '193.175.133.233'

primary = 'http://' + ip + ':8080'


class CrashThread(threading.Thread):
    def __init__(self, thread_id, crash_time):
        threading.Thread.__init__(self)
        self.thread_id = thread_id
        self.crash_time = crash_time

    def run(self):
        print('[CRASH] Starting ' + self.name)
        crash(self.thread_id, self.crash_time)
        print('[CRASH] Exiting ' + self.name)


def main():
    time.sleep(40)
    for i in range(0, amount):
        r = requests.get(primary + '/certificates')
        certificates = r.json()['certificates']
        if len(certificates) > 0:
            certificate = random.choice(certificates)
            CrashThread(certificate['host'], random.randrange(30)).start()
        time.sleep(20)


def crash(open_data_host, crash_time):
    s(cmd_docker_kill('authorization-system.' + open_data_host))
    s(cmd_docker_kill('node.' + open_data_host))
    time.sleep(crash_time)
    s(cmd_docker_start('authorization-system.' + open_data_host))
    s(cmd_docker_start('node.' + open_data_host))


def cmd_docker_kill(name):
    return 'docker-compose kill %s' % name


def cmd_docker_start(name):
    return 'docker-compose start %s' % name


def s(cmd):
    os.system(cmd)


main()
