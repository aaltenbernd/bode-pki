package bode.pki.authorization.system.utils.node;

import bode.pki.authorization.system.utils.certificate.Certificate;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;

import java.util.Collection;

public interface NodeConnector {

    static void create(Vertx vertx, Certificate nodeCertificate, String type, int port,
                       Handler<AsyncResult<BesuConnector>> handler) {
        if (type != null && type.equals("besu")) {
            new BesuConnector(vertx, nodeCertificate, port, handler);
        } else {
            new BesuConnector(vertx, nodeCertificate, port, handler);
        }
    }

    void updateNode(Logger LOGGER, Collection<Certificate> certificates);
}
