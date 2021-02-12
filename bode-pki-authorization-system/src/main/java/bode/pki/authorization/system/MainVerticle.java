package bode.pki.authorization.system;

import bode.pki.authorization.system.handler.AuthorizationHandler;
import bode.pki.authorization.system.services.authorization.AuthorizationService;
import bode.pki.authorization.system.services.authorization.AuthorizationServiceVerticle;
import bode.pki.authorization.system.verticles.ShellVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    private AuthorizationHandler authorizationHandler;

    @Override
    public void start(Promise<Void> startPromise) {
        loadConfig().compose(this::bootstrapVerticles).compose(this::startServer).setHandler(handler -> {
            if (handler.succeeded()) {
                LOGGER.info("[MAIN] Successfully launched opendata-blockchain-node");
                startPromise.complete();
            } else {
                LOGGER.error("[MAIN] Failed to launch opendata-blockchain-node: " + handler.cause());
                startPromise.fail(handler.cause());
            }
        });
    }

    private Future<Void> startServer(JsonObject config) {
        Promise<Void> promise = Promise.promise();

        Integer service_port = config.getInteger("PORT", 8080);
        boolean replica = config.containsKey("REPLICA");

        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.OPTIONS);
        allowedMethods.add(HttpMethod.DELETE);
        allowedMethods.add(HttpMethod.PATCH);
        allowedMethods.add(HttpMethod.PUT);

        String openApiPath = replica ? "webroot/openapi_replica.yaml" : "webroot/openapi_primary.yaml";
        String indexPath = replica ? "webroot/index_replica.html" : "webroot/index_primary.html";

        OpenAPI3RouterFactory.create(vertx, openApiPath, handler -> {
            if (handler.succeeded()) {
                OpenAPI3RouterFactory routerFactory = handler.result();
                RouterFactoryOptions options = new RouterFactoryOptions()
                        .setMountNotImplementedHandler(true)
                        .setRequireSecurityHandlers(true);

                routerFactory.setOptions(options);

                routerFactory.addHandlerByOperationId("getAuthorizationDatabase",
                        authorizationHandler::getAuthorizationDatabase);
                routerFactory.addHandlerByOperationId("getCertificates",
                        authorizationHandler::getCertificates);
                routerFactory.addHandlerByOperationId("getSignedRootHash",
                        authorizationHandler::getSignedRootHash);

                if (replica) {
                    routerFactory.addHandlerByOperationId("getProof",
                            authorizationHandler::getProof);
                    routerFactory.addHandlerByOperationId("updateAuthorizationDatabase",
                            authorizationHandler::updateAuthorizationDatabase);
                    routerFactory.addHandlerByOperationId("isSynced",
                            authorizationHandler::isSynced);
                    routerFactory.addHandlerByOperationId("checkSynced",
                            authorizationHandler::checkSynced);
                    routerFactory.addHandlerByOperationId("triggerOnboarding",
                            authorizationHandler::triggerOnboarding);
                } else {
                    routerFactory.addHandlerByOperationId("checkNonce",
                            authorizationHandler::checkNonce);
                    routerFactory.addHandlerByOperationId("getNonce",
                            authorizationHandler::getNonce);
                    routerFactory.addHandlerByOperationId("addCertificateToAllowMap",
                            authorizationHandler::addCertificateToAllowMap);
                    routerFactory.addHandlerByOperationId("removeCertificateFromAllowMap",
                            authorizationHandler::removeCertificateFromAllowMap);
                    routerFactory.addHandlerByOperationId("getAllowMap",
                            authorizationHandler::getAllowMap);
                    routerFactory.addHandlerByOperationId("revokeCertificate",
                            authorizationHandler::revokeCertificate);
                }

                Router router = routerFactory.getRouter();
                router.route().handler(StaticHandler.create());
                router.route("/").handler(context -> context.response().sendFile(indexPath).end());
                router.route().handler(
                        CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods)
                );
                router.errorHandler(400, context -> {
                    Throwable failure = context.failure();
                    if (failure instanceof ValidationException) {
                        LOGGER.error("[MAIN] " + failure.getMessage());
                        context.response().putHeader("Content-Type", "application/json");
                        JsonObject response = new JsonObject();
                        response.put("status", "error");
                        response.put("message", failure.getMessage());
                        context.response().setStatusCode(400);
                        context.response().end(response.encode());
                    }
                });

                HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(service_port));
                server.requestHandler(router).listen((ar) ->  {
                    if (ar.succeeded()) {
                        LOGGER.info("[MAIN] Successfully launched server on port [{}]", service_port);
                        promise.complete();
                    } else {
                        LOGGER.error("[MAIN] Failed to start server at [{}]: {}", service_port, handler.cause());
                        promise.fail(ar.cause());
                    }
                });
            } else {
                // Something went wrong during router factory initialization
                LOGGER.error("[MAIN] Failed to start server at [{}]: {}", service_port, handler.cause());
                promise.fail(handler.cause());
            }
        });

        return promise.future();
    }

    private Future<JsonObject> loadConfig() {
        Promise<JsonObject> promise = Promise.promise();

        ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray()
                        .add("PORT")
                        .add("CLI_PORT")
                        .add("PRIMARY")
                        .add("REPLICA")
                ));

        ConfigStoreOptions fileStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setOptional(true)
                .setConfig(new JsonObject().put("path", "conf/config.json"));

        ConfigRetriever.create(vertx, new ConfigRetrieverOptions()
                .addStore(fileStoreOptions)
                .addStore(envStoreOptions)).getConfig(handler -> {
            if (handler.succeeded()) {
                LOGGER.info("[MAIN] " + handler.result().encodePrettily());
                promise.complete(handler.result());
            } else {
                promise.fail(handler.cause());
            }
        });

        return promise.future();
    }

    private Future<JsonObject> bootstrapVerticles(JsonObject config) {
        Promise<JsonObject> promise = Promise.promise();

        Promise<String> shellPromise = Promise.promise();
        vertx.deployVerticle(ShellVerticle.class.getName(), new DeploymentOptions()
                .setConfig(config).setWorker(true), shellPromise);

        Promise<String> logServicePromise = Promise.promise();
        vertx.deployVerticle(AuthorizationServiceVerticle.class.getName(), new DeploymentOptions()
                .setConfig(config).setWorker(true), logServicePromise);

        CompositeFuture.all(shellPromise.future(), logServicePromise.future()).setHandler(ar -> {
            if (ar.succeeded()) {
                authorizationHandler =
                        new AuthorizationHandler(vertx, AuthorizationService.SERVICE_ADDRESS);
                promise.complete(config);
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }
}
