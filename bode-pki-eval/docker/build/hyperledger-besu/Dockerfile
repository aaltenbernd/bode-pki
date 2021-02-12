from hyperledger/besu:21.1

ENV NETWORK_DELAY_SCRIPT network_delay.sh

USER root

RUN apt-get update
RUN apt-get install -y iproute2 iputils-ping

COPY $NETWORK_DELAY_SCRIPT /opt/besu/

RUN mkdir -p /opt/besu/config

RUN chown -R root:root /opt/besu/$NETWORK_DELAY_SCRIPT
RUN chmod -R a+rwx /opt/besu/$NETWORK_DELAY_SCRIPT

ENTRYPOINT ["/bin/bash", "network_delay.sh"]