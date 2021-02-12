import numpy
import web3
import time
import eth_account
import random
import requests
import json
import sys

amount = 10

open_data_ids = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

test_id = 9

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

keys = [
    '0xac594f2a43f92f38608f81b374684c996b15e20ed7625990165b2b17402398a9',
    '0x35494461cfe009b92b1303b2978d46293d1b417b0158c6aa30f64501a4866917',
    '0x560c7dbeafec4f1e937d8c112c58c74d93ab47df51f34ba0d058fb70161b3ab6',
    '0x75518e0bebbef51f5ece9d154b6efda6c4a171bef85c0370401393c39a464cd8',
    '0xa9c4011a679436081d7be81f7a3b82ad01990b52ed68c408e695f4815905de9d',
    '0x427acacc48caab37951835c3513216078a35d7871868eca3ed478b74bbf422cf',
    '0x947a3eb22697452de5726768e665df6c0ea1603c79ddd7ca9eade7ef403b0635',
    '0xdc1229b62190f3aa2e501ed8bf73120f58017a4055f921de2a3bff669f195058',
    '0x2ca867622a72e7c27cb9a3f9bdc0e9de2bf3d5e9276b0ef118fe598663272309',
    '0x092d6b2e30b203b5063c9a1cfc93032f4d1b8a0f02df553b9031ccf10428a6d2'
]

count = []
txn_hashes = []
datasets = []
dataset_count = 0

rpc_txpool = dict(
    jsonrpc="2.0",
    method="txpool_besuTransactions",
    params=[],
    id=1
)

p = sys.argv
if len(p) <= 1:
    print('Please a storing folder')
    sys.exit()

folder_id = p[1]


def get_data():
    catalog_id = 'govdata'

    url = 'https://www.europeandataportal.eu/data/search/search'
    query_filter = 'filter=dataset'
    query_aggregation = 'aggregation=false'
    query_facets = 'facets={%22catalog%22:[%22' + catalog_id + '%22]}'
    query_limit = 'limit=1000'

    request = url + '?' + query_filter + '&' + query_aggregation + '&' + query_facets + '&' + query_limit
    r = requests.get(request)
    for dataset in r.json()['result']['results']:
        datasets.append(dataset)


def get_transaction_count(i, j):
    w3 = web3.Web3(web3.Web3.HTTPProvider(nodes[i]))
    account = eth_account.Account.privateKeyToAccount(keys[j])
    checksum_address = web3.Web3.toChecksumAddress(account.address)
    return w3.eth.getTransactionCount(checksum_address)


def get_transaction_receipt(host, txn_hash):
    w3 = web3.Web3(web3.Web3.HTTPProvider(host))
    w3.eth.getTransactionReceipt(txn_hash)


def send_transaction(host, i, send_data, nonce):
    w3 = web3.Web3(web3.Web3.HTTPProvider(host))

    account = eth_account.Account.privateKeyToAccount(keys[i])
    checksum_address = web3.Web3.toChecksumAddress(account.address)

    signed_txn = w3.eth.account.signTransaction(dict(
        nonce=nonce,
        gasPrice=0,
        gas=50000000,
        to=checksum_address,
        value=0,
        data=web3.Web3.toHex(text=str(send_data))
    ),
        keys[i]
    )

    w3.eth.sendRawTransaction(signed_txn.rawTransaction)

    txn_hashes.append(signed_txn['hash'])

    return signed_txn['hash']


def revoke(host):
    r = requests.get(primary + '/certificates')
    certificates = r.json()['certificates']
    for cert in certificates:
        if cert['host'] == 'opendata.' + host + '.org':
            requests.get(primary + '/revoke?uuid=%s' % cert['uuid'])
            break


def onboarding(open_data_id):
    with open('./docker/config/node-operator/opendata-%s/config.json' % open_data_ids[open_data_id]) as json_file:
        certificate = json.load(json_file)['REPLICA']['certificate']
        requests.post(primary + '/allowMap', json=certificate)
        replica = replicas[open_data_id]
        requests.get(replica + '/triggerOnboarding')


def txpool_is_empty():
    result = 1
    for node in nodes:
        r = requests.post(node, json=rpc_txpool)
        txpool = r.json()['result']
        if len(txpool) > 0:
            result = 0
            break
    return result


def send_transactions(tx_amount, steps, test_type, offset):
    record = []

    nonce = get_transaction_count(0, 9)

    for step in range(0, steps * tx_amount):
        dataset = datasets[offset+step]

        start = time.time()

        result = [start]

        if step == tx_amount:
            if test_type == 'onboarding':
                print('[ACCESS CONTROL] Onboarding opendata-%s' % test_id)
                onboarding(test_id)
            elif test_type == 'revoke' or test_type == 'both':
                print('[ACCESS CONTROL] Revoking opendata-%s' % test_id)
                revoke(open_data_ids[test_id])
            elif test_type == 'reactivate':
                print('[ACCESS CONTROL] Reactivate opendata-%s' % test_id)
                revoke(open_data_ids[test_id])

        if steps > 2 and step == 2 * tx_amount:
            if test_type == 'both':
                print('[ACCESS CONTROL] Reactivate opendata-%s' % test_id)
                revoke(open_data_ids[test_id])

        random_nodes = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
        random.shuffle(random_nodes)

        for node_id in random_nodes:
            try:
                signed_txn_hash = send_transaction(nodes[node_id], test_id, dataset, nonce)
                result.append(node_id)
                result.append(test_id)
                result.append(signed_txn_hash.hex())
                # print(nonce)
                nonce = nonce + 1
                break
            except ValueError as e:
                compare = dict(code=-32007, message='Sender account not authorized to send transactions')
                if str(e) == str(compare):
                    result.append(node_id)
                    result.append(test_id)
                    result.append(0)
                    break

        record.append(result)

        end = time.time()

        sleep_time = 1 - (end - start)

        if sleep_time > 0:
            time.sleep(sleep_time)

    print('[ACCESS CONTROL] Saving recorded data!')
    numpy.savetxt('./results/%s/access_control/%s.csv' % (folder_id, test_type),
                  numpy.array(record, dtype=object), delimiter=',', fmt='%f,%d,%d,%s')


def main():
    get_data()

    for i in range(0, 9):
        print('[ACCESS CONTROL] Onboarding opendata-%s' % i)
        onboarding(i)

    print('[ACCESS CONTROL] Sleep for 60 seconds')
    time.sleep(60)

    while not txpool_is_empty():
        print('[ACCESS CONTROL] Waiting until tx pool is empty, this can take up to one hour')
        time.sleep(60)

    print('[ACCESS CONTROL] Sending transactions and recording data while onboarding opendata-9')
    send_transactions(100, 2, 'onboarding', 0)

    while not txpool_is_empty():
        print('[ACCESS CONTROL] Waiting until tx pool is empty, this can take up to one hour')
        time.sleep(60)

    print('[ACCESS CONTROL] Sending transactions and recording data while revoking opendata-9')
    send_transactions(100, 2, 'revoke', 200)

    while not txpool_is_empty():
        print('[ACCESS CONTROL] Waiting until tx pool is empty, this can take up to one hour')
        time.sleep(60)

    print('[ACCESS CONTROL] Sending transactions and recording data while reactivating opendata-9')
    send_transactions(100, 2, 'reactivate', 400)

    while not txpool_is_empty():
        print('[ACCESS CONTROL] Waiting until tx pool is empty, this can take up to one hour')
        time.sleep(60)

    print('[ACCESS CONTROL] Sending transactions and recording data while revoking and reactivating opendata-9')
    send_transactions(100, 3, 'both', 600)

    while not txpool_is_empty():
        print('[ACCESS CONTROL] Waiting until tx pool is empty, this can take up to one hour')
        time.sleep(60)


main()
