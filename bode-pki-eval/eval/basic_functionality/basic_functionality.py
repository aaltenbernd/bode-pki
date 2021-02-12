import requests
import json
import time
import sys
import collections

amount = 10

open_data_ids = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

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

nodes = [
    'http://' + ip + ':8545',
    'http://' + ip + ':8547',
    'http://' + ip + ':8549',
    'http://' + ip + ':8551',
    'http://' + ip + ':8553',
    'http://' + ip + ':8555',
    'http://' + ip + ':8557',
    'http://' + ip + ':8559',
    'http://' + ip + ':8561',
    'http://' + ip + ':8563',
]


p = sys.argv
if len(p) <= 1:
    print('Please type (options are: %s or %s)' % ('all', 'onboarding'))
    sys.exit()

folder_id = p[2]


def main():
    if p[1] == 'all' or p[1] == 'onboarding':
        time.sleep(40)
        test_onboarding()

    if p[1] == 'all':
        time.sleep(40)
        test_world_state()
        time.sleep(40)
        test_sync()
        test_peer_count()
        test_nodes_allow_list()
        test_accounts_allow_list()


def equal(certificate, allow_map_item):
    result = 1
    if certificate['key'] != allow_map_item['key']:
        result = 0
    if certificate['host'] != allow_map_item['host']:
        result = 0
    if certificate['asPort'] != allow_map_item['asPort']:
        result = 0
    if certificate['nodePort'] != allow_map_item['nodePort']:
        result = 0
    return result


def test_onboarding():
    while 1:
        try:
            requests.get(primary + '/authorizationDatabase')
            break
        except requests.exceptions.ConnectionError:
            print('primary not ready')

    print('[BASIC FUNCTIONALITY] Start onboarding test')
    passed = True
    for x in open_data_ids[0:amount]:
        with open(get_config_path(x)) as json_file:
            certificate = json.load(json_file)['REPLICA']['certificate']
            requests.post(primary + '/allowMap', json=certificate)
            r = requests.get(primary + '/allowMap')
            found = 0
            for key in r.json():
                if equal(certificate, r.json()[key]):
                    found = 1
                    break
            if not found:
                print('[BASIC FUNCTIONALITY] Certificate for opendata-%s not found in allow map' % x)
                print('[BASIC FUNCTIONALITY] Failed onboarding test')
                passed = False
            else:
                print('[BASIC FUNCTIONALITY] Certificate for opendata-%s found in allow map' % x)

            time.sleep(10)

            replica = replicas[open_data_ids.index(x)]
            requests.get(replica + '/triggerOnboarding')

            time.sleep(10)

    write_result('Allowance', passed)
    print('[BASIC FUNCTIONALITY] Finished onboarding test')


def test_world_state():
    print('[BASIC FUNCTIONALITY] Start world state test')
    passed = True
    r = requests.get(primary + '/certificates')
    for x in open_data_ids[0:amount]:
        with open(get_config_path(x)) as json_file:
            certificate = json.load(json_file)['REPLICA']['certificate']
            found = 0
            for cert in r.json()['certificates']:
                if equal(certificate, cert):
                    found = 1
                    break
            if not found:
                print('[BASIC FUNCTIONALITY] Certificate for opendata-%s not found in world state' % x)
                print('[BASIC FUNCTIONALITY] Failed world state test')
                passed = False
            else:
                print('[BASIC FUNCTIONALITY] Certificate for opendata-%s found in world state' % x)

    write_result('Onboarding', passed)
    print('[BASIC FUNCTIONALITY] Finished world state test')


def test_sync():
    print('[BASIC FUNCTIONALITY] Start sync test')
    passed = True
    primary_authorization_database = requests.get(primary + '/authorizationDatabase')
    for x in replicas[0:amount]:
        replica_authorization_database = requests.get(x + '/authorizationDatabase')
        if primary_authorization_database.json() != replica_authorization_database.json():
            print('[BASIC FUNCTIONALITY] Replica for opendata-%s not synced' % open_data_ids[replicas.index(x)])
            print('[BASIC FUNCTIONALITY] Failed sync test')
            passed = False
        else:
            print('[BASIC FUNCTIONALITY] Replica for opendata-%s synced' % open_data_ids[replicas.index(x)])
    write_result('Synchronization', passed)
    print('[BASIC FUNCTIONALITY] Finished sync test')


def test_peer_count():
    print('[BASIC FUNCTIONALITY] Start peer count test')
    passed = True
    prev = '0x0'
    for x in nodes[0:amount]:
        payload = dict(jsonrpc='2.0', method='net_peerCount', params=[], id=1)
        r = requests.post(x, json=payload)
        result = r.json()['result']
        index = nodes.index(x)
        if nodes.index(x) != 0:
            open_data_id = open_data_ids[index]
            if prev != result:
                print('[BASIC FUNCTIONALITY] Node opendata-%s not connected to same number of nodes as node opendata-%s'
                      % (open_data_id, open_data_ids[0]))
                print('[BASIC FUNCTIONALITY] Failed peer count test')
                passed = False
            else:
                print('[BASIC FUNCTIONALITY] Node opendata-%s connected to same number of nodes as node opendata-%s'
                      % (open_data_id, open_data_ids[0]))
        else:
            prev = result
    write_result('Connectivity', passed)
    print('[BASIC FUNCTIONALITY] Finished peer count test')


def compare(s, t):
    return collections.Counter(s) == collections.Counter(t)


def test_nodes_allow_list():
    print('[BASIC FUNCTIONALITY] Start nodes-allowlist test')
    passed = True
    prev = []
    for x in nodes[0:amount]:
        payload = dict(jsonrpc='2.0', method='perm_getNodesAllowlist', params=[], id=1)
        r = requests.post(x, json=payload)
        result = r.json()['result']
        index = nodes.index(x)
        if nodes.index(x) != 0:
            open_data_id = open_data_ids[index]
            if not compare(prev, result):
                print('[BASIC FUNCTIONALITY] Node opendata-%s has a different nodes-allowlist compared to node '
                      'opendata-%s' % (open_data_id, open_data_ids[0]))
                print('[BASIC FUNCTIONALITY] Failed nodes-allowlist test')
                passed = False
            else:
                print('[BASIC FUNCTIONALITY] Node opendata-%s has an equal nodes-allowlist compared to node opendata-%s'
                      % (open_data_id, open_data_ids[0]))
        else:
            prev = result
    write_result('Perm. Configuration I', passed)
    print('[BASIC FUNCTIONALITY] Finished nodes-allowlist test')


def test_accounts_allow_list():
    print('[BASIC FUNCTIONALITY] Start accounts-allowlist test')
    passed = True
    prev = []
    for x in nodes[0:amount]:
        payload = dict(jsonrpc='2.0', method='perm_getAccountsAllowlist', params=[], id=1)
        r = requests.post(x, json=payload)
        result = r.json()['result']
        index = nodes.index(x)
        if nodes.index(x) != 0:
            open_data_id = open_data_ids[index]
            if not compare(prev, result):
                print('[BASIC FUNCTIONALITY] Node opendata-%s has a different accounts-allowlist compared to node '
                      'opendata-%s' % (open_data_id, open_data_ids[0]))
                print('[BASIC FUNCTIONALITY] Failed account-allowlist test')
                passed = False
            else:
                print('[BASIC FUNCTIONALITY] Node opendata-%s has an equal accounts-allowlist compared to node '
                      'opendata-%s' % (open_data_id, open_data_ids[0]))
        else:
            prev = result
    write_result('Perm. Configuration II', passed)
    print('[BASIC FUNCTIONALITY] Finished accounts-allowlist test')


def get_config_path(open_data_id):
    return './docker/config/node-operator/opendata-%s/config.json' % open_data_id


def write_result(phase, passed):
    if folder_id is not None and folder_id != 'None':
        with open("./results/%s/basic_functionality/result.csv" % folder_id, "a") as result_file:
            result_file.write("%s: %s\n" % (phase, passed))


main()
