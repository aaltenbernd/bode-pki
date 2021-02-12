import numpy
import sys
import web3
import web3.exceptions as web3ex

ip = 'localhost'
# ip = '193.175.133.233'

open_data_ids = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

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


def get_transaction_receipt(host, txn_hash):
    try:
        w3 = web3.Web3(web3.Web3.HTTPProvider(host))
        w3.eth.getTransactionReceipt(txn_hash)
        return 1
    except web3ex.TransactionNotFound:
        return 0


p = sys.argv
if len(p) <= 2:
    print('Please provide a file')
    print('Please provide a storing folder')
    sys.exit()

filename = p[1]
folder_id = p[2]

numpy.set_printoptions(suppress=False)
numpy.set_printoptions(formatter={'all': lambda x: str(x)})

print('[ACCESS CONTROL] Load recorded data: %s.csv' % filename)
record = numpy.genfromtxt('./results/%s/access_control/%s.csv' %
                          (folder_id, filename), delimiter=',', dtype=[float, int, int, "|S256"])

print('[ACCESS CONTROL] Check whether transactions were written to the blockchain')
result = []
for i in range(0, len(record)):
    current = [record[i][0], record[i][1], record[i][2], record[i][3].decode('UTF-8')]
    if get_transaction_receipt(nodes[0], record[i][3].decode('UTF-8')):
        current.append(1)
    else:
        current.append(0)
    result.append(current)

print('[ACCESS CONTROL] Saving extended data: %s_extended.csv' % filename)
numpy.savetxt('./results/%s/access_control/%s_extended.csv' % (folder_id, filename), numpy.array(result, dtype=object),
              delimiter=',', fmt='%f,%d,%d,%s,%d')
