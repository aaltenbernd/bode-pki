import requests
import time
import random

ip = 'localhost'
# ip = '193.175.133.233'

primary = 'http://' + ip + ':8080'


def main():
    time.sleep(40)
    revoke(False, 10)
    time.sleep(20)
    revoke(True, 10)


def revoke(revoked, timeout):
    r = requests.get(primary + '/certificates')
    certificates = r.json()['certificates']
    random.shuffle(certificates)

    for certificate in certificates:
        uuid = certificate['uuid']
        if certificate['revoked'] == revoked:
            requests.get(primary + '/revoke?uuid=%s' % uuid)
            if revoked:
                print('[REVOKE] Revoked %s' % certificate['host'])
            else:
                print('[REVOKE] Reactivated %s' % certificate['host'])
        time.sleep(timeout)


main()
