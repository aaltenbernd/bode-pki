FROM openjdk:11-jre

ENV VERTICLE_FILE authorization-system-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles
ENV STORE_HOME /usr/verticles/store

EXPOSE 8080 8081

RUN addgroup --system vertx && adduser --system --group vertx

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY conf/config.json.sample_primary $VERTICLE_HOME/conf/config.json
COPY $DELAY_SCRIPT $VERTICLE_HOME/

RUN mkdir $STORE_HOME
RUN chown -R vertx:vertx $VERTICLE_HOME 
RUN chmod -R a+rwx $VERTICLE_HOME

USER vertx

# Launch the verticle
WORKDIR $VERTICLE_HOME
CMD ["exec java $JAVA_OPTS -jar $VERTICLE_FILE"]