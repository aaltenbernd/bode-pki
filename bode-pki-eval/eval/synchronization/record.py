import time
import numpy
import sys
import grequests
import requests

record = []

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
    'http://' + ip + ':8100'
]

p = sys.argv
if len(p) <= 2:
    print('Please provide a filename and folder id')
    sys.exit()

filename = p[1]
folder_id = p[2]
filepath = './results/%s/synchronization/%s.csv' % (folder_id, filename)


def main():
    print('[RECORD SYNC] Recording data...')
    for i in range(0, 200):
        start = time.time()
        record_synchronization(time.time())
        end = time.time()

        sleep_time = 1.5 - (end - start)

        if sleep_time > 0:
            time.sleep(sleep_time)

    print('[RECORD SYNC] Saving recorded data!')
    numpy.savetxt(filepath, numpy.array(record), delimiter=',')


def exception_handler(request, exception):
    return None


def record_synchronization(t):
    result = [t]
    try:
        primary_log = requests.get(primary + '/authorizationDatabase')
        primary_certificates = requests.get(primary + '/certificates')
        certificates = primary_certificates.json()["certificates"]

        urls = [replicas[i] + '/authorizationDatabase' for i in range(0, 10)]
        rs = (grequests.get(u, timeout=0.5) for u in urls)
        ar = grequests.map(rs, exception_handler=exception_handler)

        for i in range(0, amount):
            found_and_not_revoked = 0
            for certificate in certificates:
                if certificate['host'] == 'opendata.' + open_data_ids[i] + '.org':
                    if not certificate['revoked']:
                        found_and_not_revoked = 1
                    break

            if not found_and_not_revoked:
                result.append(0)
            elif ar[i] is None:
                result.append(0)
            elif primary_log.json() != ar[i].json():
                result.append(1)
            else:
                result.append(2)

        record.append(result)
    except requests.exceptions.ConnectionError:
        print('primary not ready')


main()
