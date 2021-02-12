package bode.pki.authorization.system.services.authorization;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import bode.pki.authorization.system.utils.authorization.AuthorizationSystem;

public class AuthorizationServiceVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        AuthorizationSystem.create(vertx, config(), certificateConnectorReady -> {
            if (certificateConnectorReady.succeeded()) {
                AuthorizationService.create(certificateConnectorReady.result(), serviceReady -> {
                    if (serviceReady.succeeded()) {
                        new ServiceBinder(vertx).setAddress(AuthorizationService.SERVICE_ADDRESS)
                                .register(AuthorizationService.class, serviceReady.result());
                        startPromise.complete();
                    } else {
                        startPromise.fail(serviceReady.cause());
                    }
                });
            } else {
                startPromise.fail(certificateConnectorReady.cause());
            }
        });
    }
}
