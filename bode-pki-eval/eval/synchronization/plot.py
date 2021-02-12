import numpy
import sys
import matplotlib
import matplotlib.pyplot as plt

open_data_ids = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

p = sys.argv
if len(p) <= 2:
    print('Please provide a filename and folder id')
    sys.exit()

filename = p[1]
folder_id = p[2]

numpy.set_printoptions(suppress=False)
numpy.set_printoptions(formatter={'all': lambda m: str(m)})

print('Load recorded data!')
record = numpy.genfromtxt('./results/%s/synchronization/%s.csv' % (folder_id, filename), delimiter=',')

label = ["not reachable", "not synced", "synced"]
ticks = [0, 1, 2]

t = record[:, 0]
t = numpy.subtract(t, t[0])
t = t[t < 301]

replica_0 = record[:, 1][:len(t)]
replica_1 = record[:, 2][:len(t)]
replica_2 = record[:, 3][:len(t)]
replica_3 = record[:, 4][:len(t)]
replica_4 = record[:, 5][:len(t)]
replica_5 = record[:, 6][:len(t)]
replica_6 = record[:, 7][:len(t)]
replica_7 = record[:, 8][:len(t)]
replica_8 = record[:, 9][:len(t)]
replica_9 = record[:, 10][:len(t)]

# plt.style.use('dark_background')
plt.rcParams["font.family"] = "serif"
plt.rcParams["font.size"] = 20
matplotlib.rc('text', usetex=True)

fig, axs = plt.subplots(10, 1, sharex=True, sharey=True)
fig.set_figheight(20)
fig.set_figwidth(15)

for i in range(0, 10):
    x = i
    y = 0
    axs[x].plot(t, record[:, i+1][:len(t)], color='k', alpha=0.3)
    axs[x].scatter(t, record[:, i+1][:len(t)], color='k', marker='.')
    axs[x].set_title('Authorization Replica %s' % i)

for ax in axs.flat:
    ax.set_axisbelow(True)
    ax.set(xlabel='Time [s]')
    ax.label_outer()
    ax.grid(axis='y', linewidth=0.1)
    ax.set_yticks(ticks)
    ax.set_yticklabels(label)
    ax.set_ylim(ymax=2.3)
    ax.set_ylim(ymin=-0.3)

fig.tight_layout()
fig.savefig('./results/%s/synchronization/%s.pdf' % (folder_id, filename))

synced_percentage = []
in_system = []

synced_number = []

for i in range(0, len(t)):
    num_synced = 0
    num_not_in_system = 0
    for j in range(1, len(record[i])):
        if record[i, j] == 2:
            num_synced = num_synced + 1
        if record[i, j] == -1:
            num_not_in_system = num_not_in_system + 1
    synced_number.append(num_synced)
    if (10 - num_not_in_system) > 0:
        in_system.append((10 - num_not_in_system))
        synced_percentage.append(num_synced / (10 - num_not_in_system))
    else:
        in_system.append(0)
        synced_percentage.append(0)

number_of_replicas = 10
synced = [x*100 for x in synced_percentage]
num = [x*10 for x in in_system]

ticks = [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100]

major_xticks = numpy.arange(0, 301, 50)
minor_xticks = numpy.arange(0, 301, 25)

fig, ax = plt.subplots()
fig.set_figheight(5)
fig.set_figwidth(15)

ax.set_axisbelow(True)
ax.set(xlabel='Time [s]')
ax.label_outer()
ax.grid(axis='y', linewidth=0.1)
ax.set_xticks(major_xticks)
ax.set_xticks(minor_xticks, minor=True)

ax.set_axisbelow(True)

ax.plot(t, synced_number, color='k', alpha=0.3)
ax.scatter(t, synced_number, color='k', marker='.')
ax.set(ylabel='Number of Synchronized Replicas')
ax.set_ylim(ymax=11)
ax.set_ylim(ymin=-1)
ax.set_yticks([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])

fig.tight_layout()
fig.savefig('./results/%s/synchronization/%s-synced.pdf' % (folder_id, filename))
