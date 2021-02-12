import time
import eth_account
import requests
import web3
import web3.exceptions as web3ex
import sys

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

p = sys.argv
if len(p) <= 1:
    print('Please a folder id')
    sys.exit()

folder_id = p[1]


def get_data():
    r = requests.get('https://www.europeandataportal.eu/data/search/search?filter=dataset&limit=0')
    facets = r.json()['result']['facets']
    catalogs = facets[2]['items']
    catalog_ids = [x['id'] for x in catalogs[0:amount+1]]
    for catalog_id in catalog_ids:
        url = 'https://www.europeandataportal.eu/data/search/search'
        query_filter = 'filter=dataset'
        query_aggregation = 'aggregation=false'
        query_facets = 'facets={%22catalog%22:[%22' + catalog_id + '%22]}'
        query_limit = 'limit=10'

        request = url + '?' + query_filter + '&' + query_aggregation + '&' + query_facets + '&' + query_limit

        r = requests.get(request)
        results = r.json()['result']['results']
        datasets.append(results)


def revoke(host):
    r = requests.get(primary + '/certificates')
    certificates = r.json()['certificates']
    for cert in certificates:
        if cert['host'] == 'opendata.' + host + '.org':
            requests.get(primary + '/revoke?uuid=%s' % cert['uuid'])
            break


def get_transaction_count():
    for i in range(0, amount):
        w3 = web3.Web3(web3.Web3.HTTPProvider(nodes[i]))
        account = eth_account.Account.privateKeyToAccount(keys[i])
        checksum_address = web3.Web3.toChecksumAddress(account.address)
        count.append(w3.eth.getTransactionCount(checksum_address))


def get_transaction_receipt(host, txn_hash):
    w3 = web3.Web3(web3.Web3.HTTPProvider(host))
    w3.eth.getTransactionReceipt(txn_hash)


def send_transaction(host, node_id, send_data, nonce):
    w3 = web3.Web3(web3.Web3.HTTPProvider(host))

    account = eth_account.Account.privateKeyToAccount(keys[node_id])
    checksum_address = web3.Web3.toChecksumAddress(account.address)

    signed_txn = w3.eth.account.signTransaction(dict(
        nonce=nonce,
        gasPrice=0,
        gas=50000000,
        to=checksum_address,
        value=0,
        data=web3.Web3.toHex(text=str(send_data))
    ),
        keys[node_id]
    )

    txn_hashes.append(signed_txn['hash'])

    w3.eth.sendRawTransaction(signed_txn.rawTransaction)


def main():
    get_transaction_count()
    get_data()

    print('[BASIC FUNCTIONALITY] Start authorized transaction test')
    passed = True
    for j in range(0, amount):
        for i in range(0, amount):
            print('[BASIC FUNCTIONALITY] Send transaction for opendata-%s to opendata-%s' % (j, i))
            send_transaction(nodes[i], j, datasets[j][i], count[j] + i)

    step = 0
    exception = 1
    while exception == 1:
        exception = 0
        for txn_hash in txn_hashes:
            try:
                get_transaction_receipt(nodes[0], txn_hash)
            except web3ex.TransactionNotFound:
                exception = 1
        time.sleep(60)
        if step == 10:
            print('[BASIC FUNCTIONALITY] Failed authorized transaction test')
            passed = False
            break
        step = step + 1

    write_result('Access Control I.', passed)
    print('[BASIC FUNCTIONALITY] Finished authorized transaction test')

    print('[BASIC FUNCTIONALITY] Start unauthorized transaction test')
    passed = True
    test_id = amount-1

    print('[BASIC FUNCTIONALITY] Revoking opendata-%s' % open_data_ids[test_id])
    revoke(open_data_ids[test_id])

    step = 0
    synced = 0
    while synced == 0:
        print('[BASIC FUNCTIONALITY] Wait for synchronization to continue')
        synced = 1
        primary_authorization_database = requests.get(primary + '/authorizationDatabase')
        for x in replicas[0:amount]:
            replica_authorization_database = requests.get(x + '/authorizationDatabase')
            if primary_authorization_database.json() != replica_authorization_database.json():
                synced = 0

        if step == 10:
            break
        step = step + 1
        time.sleep(60)

    for i in range(0, amount):
        try:
            print('[BASIC FUNCTIONALITY] Send transaction for opendata-%s to opendata-%s' % (test_id, i))
            send_transaction(nodes[i], test_id, datasets[test_id][i], count[test_id] + 11)
            # send_faulty_transaction(nodes[i], datasets[amount][i], faulty_node['count'] + 1)
        except ValueError as e:
            compare = dict(code=-32007, message='Sender account not authorized to send transactions')
            if not str(e) == str(compare):
                print(e)
                print('[BASIC FUNCTIONALITY] Failed unauthorized transaction test')
                passed = False
                break

    print('[BASIC FUNCTIONALITY] Reactivating opendata-%s' % open_data_ids[test_id])
    revoke(open_data_ids[test_id])

    print('[BASIC FUNCTIONALITY] Finished unauthorized transaction test')
    write_result('Access Control II.', passed)
    time.sleep(60)


def write_result(phase, passed):
    with open("./results/%s/basic_functionality/result.csv" % folder_id, "a") as result_file:
        result_file.write("%s: %s\n" % (phase, passed))


main()
