import numpy
import sys
import web3
import web3.exceptions as web3ex
import matplotlib
import matplotlib.pyplot as plt


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


def write_result(phase, write_content):
    with open("./results/%s/access_control/result.csv" % folder_id, "a") as result_file:
        result_file.write("%s: %s\n" % (phase, write_content))


def get_transaction_receipt(host, txn_hash):
    try:
        w3 = web3.Web3(web3.Web3.HTTPProvider(host))
        w3.eth.getTransactionReceipt(txn_hash)
        return 1
    except web3ex.TransactionNotFound:
        return 0


p = sys.argv
if len(p) <= 3:
    print('Please provide a file and length')
    print('Please provide a folder id')
    sys.exit()

test_type = p[1]
folder_id = p[3]

numpy.set_printoptions(suppress=False)
numpy.set_printoptions(formatter={'all': lambda x: str(x)})

print('[ACCESS CONTROL] Load extended data: %s_extended.csv' % test_type)
record = numpy.genfromtxt('./results/%s/access_control/%s_extended.csv' % (folder_id, test_type), delimiter=',',
                          dtype=[float, int, int, "|S256", int])

# plt.style.use('dark_background')
plt.rcParams["font.family"] = "serif"
plt.rcParams["font.size"] = 20
matplotlib.rc('text', usetex=True)

plt.gca().invert_yaxis()
fig = plt.figure()
ax = fig.add_subplot(1, 1, 1)

fig.set_figheight(6)
fig.set_figwidth(15)

ylabel = ['Node %s' % x for x in [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]]
yticks = [-0, -1, -2, -3, -4, -5, -6, -7, -8, -9]
xticks = [0, 50, 100, 150, 200, 250, 300]

length = int(p[2])

major_xticks = numpy.arange(0, length + 1, 50)
minor_xticks = numpy.arange(0, length + 1, 25)

xlabel = [str(item) for item in major_xticks]

xlabel[2] = '{\\fontseries{sb}\\selectfont %s}' % xlabel[2]
xlabel[4] = '{\\fontseries{sb}\\selectfont %s}' % xlabel[4]
ylabel[9] = '{\\fontseries{sb}\\selectfont %s}' % ylabel[9]

times = []
blockchain = []
receiving_nodes = []
in_chain_times = []
in_chain = []
not_in_chain_times = []
not_in_chain = []
for i in range(0, 10):
    times.append([])
    blockchain.append([])
    receiving_nodes.append([])
    in_chain_times.append([])
    in_chain.append([])
    not_in_chain_times.append([])
    not_in_chain.append([])

for i in range(0, len(record)):
    receiving_host = record[i][1]
    account = record[i][2]
    t = record[i][0] - record[0][0]
    times[account].append(t)
    blockchain[account].append(record[i][4])
    receiving_nodes[account].append(receiving_host)

for account in range(0, 10):
    for i in range(0, len(blockchain[account])):
        if blockchain[account][i]:
            in_chain_times[account].append(times[account][i])
            in_chain[account].append(-receiving_nodes[account][i])
        else:
            not_in_chain_times[account].append(times[account][i])
            not_in_chain[account].append(-receiving_nodes[account][i])

ax.set(xlabel='Time [s]')
ax.label_outer()
ax.grid(which='both', linewidth=0.1)
ax.set_xticks(major_xticks)
ax.set_xticks(minor_xticks, minor=True)
ax.set_xticklabels(xlabel)
ax.set_yticks(yticks)
ax.set_yticklabels(ylabel)
ax.set_ylim(ymax=1)
ax.set_ylim(ymin=-10)

account = 9
scatter1 = ax.scatter(in_chain_times[account], in_chain[account], color='g', marker='.')
scatter2 = ax.scatter(not_in_chain_times[account], not_in_chain[account], color='r', marker='.')
ax.set_axisbelow(True)

legend = plt.legend((scatter1, scatter2),
                    ('Transaction written to Blockchain', 'Transaction not written to Blockchain'),
                    ncol=3,
                    loc='upper center',
                    borderpad=0.4,
                    handlelength=0.5,
                    bbox_to_anchor=(0.5, 1.15),
                    fancybox=False
                    )

legend.get_frame().set_edgecolor((0, 0, 0, 0))
legend.get_frame().set_alpha(None)
legend.get_frame().set_facecolor((1, 1, 1, 0))
fig.tight_layout()

if test_type == 'onboarding' or test_type == 'reactivate':
    mid = int(length / 2)
    incorrect_txn_before = len([i for i in in_chain_times[account] if i < mid])
    incorrect_txn_after = len([i for i in not_in_chain_times[account] if i >= mid])
    incorrect_txn_total = incorrect_txn_before+incorrect_txn_after
    print("[ACCESS CONTROL] Number of incorrect transactions: %s " % incorrect_txn_total)

    delay_correct = in_chain_times[account][0] - mid
    delay_incorrect = not_in_chain_times[account][len(not_in_chain_times[account])-1] - mid
    print("[ACCESS CONTROL] Delay of first correct transaction: %s " % delay_correct)
    print("[ACCESS CONTROL] Delay of last incorrect transaction: %s " % delay_incorrect)

    test_result = "%s; %s; %s" % (incorrect_txn_total, delay_correct, delay_incorrect)
    write_result(test_type, test_result)

elif test_type == 'revoke':
    mid = int(length / 2)
    incorrect_txn_before = len([i for i in not_in_chain_times[account] if i < mid])
    incorrect_txn_after = len([i for i in in_chain_times[account] if i >= mid])
    incorrect_txn_total = incorrect_txn_before + incorrect_txn_after
    print("[ACCESS CONTROL] Number of incorrect transactions: %s " % incorrect_txn_total)

    delay_correct = not_in_chain_times[account][0] - mid
    delay_incorrect = in_chain_times[account][len(in_chain_times[account])-1] - mid
    print("[ACCESS CONTROL] Delay of first correct transaction: %s " % delay_correct)
    print("[ACCESS CONTROL] Delay of last incorrect transaction: %s " % delay_incorrect)

    test_result = "%s; %s; %s" % (incorrect_txn_total, delay_correct, delay_incorrect)
    write_result(test_type, test_result)

elif test_type == 'both':
    mid = int(length / 2)
    first_third = int(length / 3)
    second_third = 2 * first_third
    incorrect_txn_before_first_third = len([i for i in not_in_chain_times[account] if i < first_third])
    incorrect_txn_after_first_third = len([i for i in in_chain_times[account] if first_third <= i < mid])
    incorrect_txn_total_first_third = incorrect_txn_before_first_third + incorrect_txn_after_first_third

    not_in_chain_times_first_half = [i for i in not_in_chain_times[account] if i < mid]
    delay_correct_first_third = not_in_chain_times_first_half[0] - first_third
    in_chain_times_first_half = [i for i in in_chain_times[account] if i < mid]
    delay_incorrect_first_third = in_chain_times_first_half[len(in_chain_times_first_half) - 1] - first_third
    print("[ACCESS CONTROL] Delay of first correct transaction at first third: %s " % delay_correct_first_third)
    print("[ACCESS CONTROL] Delay of last incorrect transaction at first third: %s " % delay_incorrect_first_third)

    test_result = "%s; %s; %s" \
                  % (incorrect_txn_total_first_third, delay_correct_first_third, delay_incorrect_first_third)
    write_result(test_type + " I.", test_result)

    print("[ACCESS CONTROL] Number of incorrect transactions at first third: %s " % incorrect_txn_total_first_third)

    incorrect_txn_before_second_third = len([i for i in in_chain_times[account] if mid <= i < second_third])
    incorrect_txn_after_second_third = len([i for i in not_in_chain_times[account] if second_third <= i])
    incorrect_txn_total_second_third = incorrect_txn_before_second_third + incorrect_txn_after_second_third

    print("[ACCESS CONTROL] Number of incorrect transactions at second third: %s " % incorrect_txn_total_second_third)

    in_chain_times_second_half = [i for i in in_chain_times[account] if i >= mid]
    delay_correct_second_third = in_chain_times_second_half[0] - second_third
    not_in_chain_times_first_half = [i for i in not_in_chain_times[account] if i >= mid]
    delay_incorrect_second_third = not_in_chain_times_first_half[len(not_in_chain_times_first_half) - 1] - second_third
    print("[ACCESS CONTROL] Delay of first correct transaction at second third: %s " % delay_correct_second_third)
    print("[ACCESS CONTROL] Delay of last incorrect transaction at second third: %s " % delay_incorrect_second_third)

    test_result = "%s; %s; %s" \
                  % (incorrect_txn_total_second_third, delay_correct_second_third, delay_incorrect_second_third)
    write_result(test_type + " II.", test_result)

print('[ACCESS CONTROL] Save plotted data: %s.pdf' % test_type)
fig.savefig('./results/%s/access_control/%s.pdf' % (folder_id, test_type))
