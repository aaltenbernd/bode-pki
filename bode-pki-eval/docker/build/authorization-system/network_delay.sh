tc qdisc add dev eth0 root handle 1: prio &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.1 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.2 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.3 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.4 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.5 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.6 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.7 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.8 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.9 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.10 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.11 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.12 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.13 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.14 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.15 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.16 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.17 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.18 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.19 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.20 flowid 2:1 &&
tc filter add dev eth0 parent 1:0 protocol ip prio 1 u32 match ip dst 176.0.1.21 flowid 2:1 &&
tc qdisc add dev eth0 parent 1:1 handle 2: netem delay 1000ms 500ms distribution normal &&
exec java $JAVA_OPTS -jar $VERTICLE_FILE