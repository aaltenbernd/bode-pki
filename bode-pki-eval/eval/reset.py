import sys
import shutil
import pathlib

p = sys.argv
if len(p) <= 1:
    print('Please provide a parameter, options are: %s or %s' % ('all', 'perm'))
    sys.exit()


if p[1] == 'all':
    shutil.rmtree('./docker/data', ignore_errors=True)

    pathlib.Path('./docker/data/network-authority/data').mkdir(parents=True, exist_ok=True)

    for i in range(0, 10):
        pathlib.Path('./docker/data/node-operator/opendata-%s/data' % i).mkdir(parents=True, exist_ok=True)

    for i in range(0, 10):
        shutil.copyfile('./docker/config/node-operator/all/permissions_config.toml',
                        './docker/data/node-operator/opendata-%s/data/permissions_config.toml' % i)

elif p[1] == 'perm':
    for i in range(0, 10):
        shutil.copyfile('./docker/config/node-operator/all/permissions_config.toml',
                        './docker/data/node-operator/opendata-%s/data/permissions_config.toml' % i)

else:
    print('Please provide a parameter, options are: %s or %s' % ('all', 'perm'))
