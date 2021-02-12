import subprocess
import time
import sys
import os
import pathlib

if sys.version_info[0] < 3:
    print('Please use python 3')
    sys.exit()

p = sys.argv
if len(p) <= 1:
    print('Please provide a parameter, options are: %s, %s, %s or %s' % ('all', 'basic', 'sync', 'access'))
    sys.exit()

i = 0
while 1:
    if not os.path.exists('./results/%s' % i):
        break
    i = i + 1

folder_id = str(i)

print('[MAIN] Storing results to: ./results/%s' % folder_id)

if p[1] == 'all' or p[1] == 'basic':

    print('[MAIN] Create folder: ./results/%s/basic_functionality/' % folder_id)
    pathlib.Path('./results/%s/basic_functionality/' % folder_id).mkdir(parents=True, exist_ok=True)

    # >>> CLEAR

    print('[MAIN] Shut down docker container')
    docker_down_process = subprocess.Popen(['docker-compose', 'down'])
    docker_down_process.wait()
    
    print('[MAIN] Reset all files')
    reset_process = subprocess.Popen([sys.executable, 'eval/reset.py', 'all'])
    reset_process.wait()
    
    print('[MAIN] Start up docker container')
    docker_up_process = subprocess.Popen(['docker-compose', 'up', '-d'])
    docker_up_process.wait()

    # <<< CLEAR

    print('[MAIN] Sleep for 5 minutes')
    time.sleep(300)

    # >>> BASIC FUNCTIONALITY

    print('[MAIN] Start basic functionality test')

    basic_functionality_process = \
        subprocess.Popen([sys.executable, 'eval/basic_functionality/basic_functionality.py', 'all', folder_id])
    basic_functionality_process.wait()

    basic_functionality_send_transaction_process = \
        subprocess.Popen([sys.executable, 'eval/basic_functionality/send_transactions.py', folder_id])
    basic_functionality_send_transaction_process.wait()

    print('[MAIN] Break basic functionality test')

    # BASIC FUNCTIONALITY <<<

    # >>> CLEAR

    print('[MAIN] Shut down docker container')
    docker_down_process = subprocess.Popen(['docker-compose', 'down'])
    docker_down_process.wait()

    print('[MAIN] Reset all files')
    reset_process = subprocess.Popen([sys.executable, 'eval/reset.py', 'all'])
    reset_process.wait()

    print('[MAIN] Start up docker container')
    docker_up_process = subprocess.Popen(['docker-compose', 'up', '-d'])
    docker_up_process.wait()

    # <<< CLEAR

    print('[MAIN] Sleep for 5 minutes')
    time.sleep(300)

    # >>> BASIC FUNCTIONALITY

    print('[MAIN] Continue basic functionality test')

    basic_functionality_process = \
        subprocess.Popen([sys.executable, 'eval/basic_functionality/accountability.py', folder_id])
    basic_functionality_process.wait()

    print('[MAIN] Finished basic functionality test')

    # BASIC FUNCTIONALITY <<<

if p[1] == 'all' or p[1] == 'sync':

    print('[MAIN] Create folder: ./results/%s/synchronization/' % folder_id)
    pathlib.Path('./results/%s/synchronization/' % folder_id).mkdir(parents=True, exist_ok=True)

    # >>> CLEAR

    print('[MAIN] Shut down docker container')
    docker_down_process = subprocess.Popen(['docker-compose', 'down'])
    docker_down_process.wait()

    print('[MAIN] Reset all files')
    reset_process = subprocess.Popen([sys.executable, 'eval/reset.py', 'all'])
    reset_process.wait()

    print('[MAIN] Start up docker container')
    docker_up_process = subprocess.Popen(['docker-compose', 'up', '-d'])
    docker_up_process.wait()

    # <<< CLEAR

    print('[MAIN] Sleep for 5 minutes')
    time.sleep(300)

    # >>> SYNC

    print('[MAIN] Start synchronization test')

    crash_process = subprocess.Popen([sys.executable, 'eval/synchronization/crash.py'])
    onboarding_process = \
        subprocess.Popen([sys.executable, 'eval/basic_functionality/basic_functionality.py', 'onboarding', 'None'])
    record_process = \
        subprocess.Popen([sys.executable, 'eval/synchronization/record.py',
                          'onboarding_with_crashes_and_delay', folder_id])

    crash_process.wait()
    onboarding_process.wait()
    record_process.wait()

    plot_process = subprocess.Popen(
        [sys.executable, "eval/synchronization/plot.py", 'onboarding_with_crashes_and_delay', folder_id])

    plot_process.wait()

    print('[MAIN] Sleep for 10 minutes')
    time.sleep(600)

    crash_process = subprocess.Popen([sys.executable, 'eval/synchronization/crash.py'])
    revoke_process = subprocess.Popen([sys.executable, 'eval/synchronization/revoke.py'])
    record_process = \
        subprocess.Popen([sys.executable, 'eval/synchronization/record.py', 'revoke_with_crashes_and_delay', folder_id])

    crash_process.wait()
    revoke_process.wait()
    record_process.wait()

    plot_process = \
        subprocess.Popen([sys.executable, "eval/synchronization/plot.py", 'revoke_with_crashes_and_delay', folder_id])

    plot_process.wait()

    print('[MAIN] Finished synchronization test')

    # SYNC <<<

if p[1] == 'all' or p[1] == 'access':

    print('[MAIN] Create folder: ./results/%s/access_control/' % folder_id)
    pathlib.Path('./results/%s/access_control/' % folder_id).mkdir(parents=True, exist_ok=True)

    # >>> CLEAR

    print('[MAIN] Shut down docker container')
    docker_down_process = subprocess.Popen(['docker-compose', 'down'])
    docker_down_process.wait()

    print('[MAIN] Reset all files')
    reset_process = subprocess.Popen([sys.executable, 'eval/reset.py', 'all'])
    reset_process.wait()

    print('[MAIN] Start up docker container')
    docker_up_process = subprocess.Popen(['docker-compose', 'up', '-d'])
    docker_up_process.wait()

    # <<< CLEAR

    print('[MAIN] Sleep for 5 minutes')
    time.sleep(300)

    # >>> ACCESS CONTROL

    print('[MAIN] Start access control test')

    access_control_send_transactions_process = \
        subprocess.Popen([sys.executable, './eval/access_control/send_transactions.py', folder_id])
    access_control_send_transactions_process.wait()

    access_control_extend_data_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/extend_data.py', 'onboarding', folder_id])
    access_control_extend_data_process.wait()

    access_control_extend_data_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/extend_data.py', 'revoke', folder_id])
    access_control_extend_data_process.wait()

    access_control_extend_data_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/extend_data.py', 'reactivate', folder_id])
    access_control_extend_data_process.wait()

    access_control_extend_data_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/extend_data.py', 'both', folder_id])
    access_control_extend_data_process.wait()

    access_control_plot_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/plot.py', 'onboarding', '200', folder_id])
    access_control_plot_process.wait()

    access_control_plot_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/plot.py', 'revoke', '200', folder_id])
    access_control_plot_process.wait()

    access_control_plot_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/plot.py', 'reactivate', '200', folder_id])
    access_control_plot_process.wait()

    access_control_plot_process = \
        subprocess.Popen([sys.executable, 'eval/access_control/plot.py', 'both', '300', folder_id])
    access_control_plot_process.wait()

    print('[MAIN] Finished access control test')

    # <<< ACCESS CONTROL

print('[MAIN] Shut down docker container')
docker_down_process = subprocess.Popen(['docker-compose', 'down'])
docker_down_process.wait()
