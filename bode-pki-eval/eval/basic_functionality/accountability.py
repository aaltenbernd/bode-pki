import requests
import json
import os
import time
import sys

amount = 10

open_data_ids = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

test_id_0 = 0
test_id_1 = 1
test_id_2 = 2
test_id_3 = 3

ip = 'localhost'
# ip = '193.175.133.233'

primary = 'http://' + ip + ':8080'

replicas = [
    'http://' + ip + ':8082',
    'http://' + ip + ':8084',
    'http://' + ip + ':8086',
    'http://' + ip + ':8088',
    'http://' + ip + ':8090',
    'http://' + ip + ':8092',
    'http://' + ip + ':8094',
    'http://' + ip + ':8096',
    'http://' + ip + ':8098',
    'http://' + ip + ':8100',
]

p = sys.argv
if len(p) <= 1:
    print('Please a folder id')
    sys.exit()

folder_id = p[1]


def onboarding(open_data_id):
    with open('./docker/config/node-operator/opendata-%s/config.json' % open_data_ids[open_data_id]) as json_file:
        certificate = json.load(json_file)['REPLICA']['certificate']
        requests.post(primary + '/allowMap', json=certificate)
        replica = replicas[open_data_id]
        requests.get(replica + '/triggerOnboarding')


def main():
    print('[BASIC FUNCTIONALITY] Start accountability test')

    print('[BASIC FUNCTIONALITY] Onboarding opendata-%s' % test_id_0)
    onboarding(test_id_0)

    print('[BASIC FUNCTIONALITY] Onboarding opendata-%s' % test_id_1)
    onboarding(test_id_1)

    print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
    time.sleep(60)

    print('[BASIC FUNCTIONALITY] Kill opendata-%s' % test_id_1)
    kill(test_id_1)

    print('[BASIC FUNCTIONALITY] Copy authorization database')
    os.rename('./docker/data/network-authority/data/authorization_database.json',
              './docker/data/network-authority/data/authorization_database_ab.json')

    print('[BASIC FUNCTIONALITY] Onboarding opendata-%s' % test_id_2)
    onboarding(test_id_2)

    print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
    time.sleep(60)

    print('[BASIC FUNCTIONALITY] Kill opendata-%s' % test_id_0)
    kill(test_id_0)
    print('[BASIC FUNCTIONALITY] Kill opendata-%s' % test_id_2)
    kill(test_id_2)

    print('[BASIC FUNCTIONALITY] Kill authorization primary')
    s(cmd_docker_kill('authorization-system.na.org'))

    print('[BASIC FUNCTIONALITY] Restore previous copied authorization database')
    os.remove('./docker/data/network-authority/data/authorization_database.json')
    os.rename('./docker/data/network-authority/data/authorization_database_ab.json',
              './docker/data/network-authority/data/authorization_database.json')

    print('[BASIC FUNCTIONALITY] Start authorization primary')
    s(cmd_docker_start('authorization-system.na.org'))

    print('[BASIC FUNCTIONALITY] Start opendata-%s' % test_id_1)
    start(test_id_1)

    print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
    time.sleep(60)

    print('[BASIC FUNCTIONALITY] Onboarding opendata-%s' % test_id_3)
    onboarding(test_id_3)

    print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
    time.sleep(60)

    print('[BASIC FUNCTIONALITY] Start opendata-%s' % test_id_0)
    start(test_id_0)

    print('[BASIC FUNCTIONALITY] Start opendata-%s' % test_id_2)
    start(test_id_2)

    print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
    time.sleep(60)

    passed = True
    step = 0
    while 1:
        print('[BASIC FUNCTIONALITY] Check sync API')
        replica = replicas[test_id_0]
        r = requests.get(replica + '/sync')
        sync_to_opendata_1 = r.json()['opendata.' + open_data_ids[test_id_1] + '.org']

        replica = replicas[test_id_1]
        r = requests.get(replica + '/sync')
        sync_to_opendata_0 = r.json()['opendata.' + open_data_ids[test_id_0] + '.org']

        if sync_to_opendata_1 == 3 and sync_to_opendata_0 == 3:
            break

        if step == 10:
            print('[BASIC FUNCTIONALITY] Failed accountability test')
            passed = False
            break
        step = step + 1
        print('[BASIC FUNCTIONALITY] Sleep for 60 seconds')
        time.sleep(60)

    write_result('Accountability', passed)
    print('[BASIC FUNCTIONALITY] Finished accountability test')


def kill(kill_id):
    s(cmd_docker_kill('authorization-system.opendata.' + open_data_ids[kill_id] + '.org'))
    s(cmd_docker_kill('node.opendata.' + open_data_ids[kill_id] + '.org'))


def start(start_id):
    s(cmd_docker_start('authorization-system.opendata.' + open_data_ids[start_id] + '.org'))
    s(cmd_docker_start('node.opendata.' + open_data_ids[start_id] + '.org'))


def cmd_docker_kill(name):
    return 'docker-compose kill %s' % name


def cmd_docker_start(name):
    return 'docker-compose start %s' % name


def s(cmd):
    os.system(cmd)


def write_result(phase, passed):
    with open("./results/%s/basic_functionality/result.csv" % folder_id, "a") as result_file:
        result_file.write("%s: %s\n" % (phase, passed))


main()
