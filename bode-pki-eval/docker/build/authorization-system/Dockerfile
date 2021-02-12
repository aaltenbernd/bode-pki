FROM openjdk:11-jre

ENV VERTICLE_FILE authorization-system-fat.jar
ENV CONFIG_FILE config.json.sample_primary
ENV NETWORK_DELAY_SCRIPT network_delay.sh

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
ENV STORE_HOME /usr/verticles/store

EXPOSE 8080 8081

# Copy your fat jar to the container
COPY $VERTICLE_FILE $VERTICLE_HOME/
COPY $CONFIG_FILE $VERTICLE_HOME/conf/config.json
COPY $NETWORK_DELAY_SCRIPT $VERTICLE_HOME/

RUN mkdir $STORE_HOME
RUN chown -R root:root $VERTICLE_HOME
RUN chmod -R a+rwx $VERTICLE_HOME

RUN chown -R root:root $VERTICLE_HOME/$NETWORK_DELAY_SCRIPT
RUN chmod -R a+rwx $VERTICLE_HOME/$NETWORK_DELAY_SCRIPT

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["/bin/bash", "network_delay.sh"]