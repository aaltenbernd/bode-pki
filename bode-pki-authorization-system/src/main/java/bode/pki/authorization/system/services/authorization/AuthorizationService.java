package bode.pki.authorization.system.services.authorization;

import bode.pki.authorization.system.utils.authorization.AuthorizationSystem;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
public interface AuthorizationService {

    String SERVICE_ADDRESS = "bode.pki.authorization.system.services.authorization.database.queue";

    static AuthorizationService create(AuthorizationSystem authorizationSystem,
                                       Handler<AsyncResult<AuthorizationService>> readyHandler) {
        return new AuthorizationServiceImpl(authorizationSystem, readyHandler);
    }

    static AuthorizationService createProxy(Vertx vertx, String address) {
        return new AuthorizationServiceVertxEBProxy(vertx, address);
    }

    // API:

    @Fluent
    AuthorizationService checkNonce(String nonce, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService getNonce(String endpoint, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService getProof(Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService getCertificates(Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService getAuthorizationDatabase(Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService updateAuthorizationDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService getSignedRootHash(Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService checkSynced(JsonObject payload, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService isSynced(Handler<AsyncResult<JsonObject>> handler);

    // CLI:

    @Fluent
    AuthorizationService sign(String message, String privateKey, Handler<AsyncResult<String>> handler);

    @Fluent
    AuthorizationService verify(String message, String signature, String publicKey, Handler<AsyncResult<String>> handler);

    // ONBOADRING:

    @Fluent
    AuthorizationService getAllowMap(Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService addCertificateToAllowMap(JsonObject payload, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService removeCertificateFromAllowMap(String uuid, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService revokeCertificate(String uuid, Handler<AsyncResult<JsonObject>> handler);

    @Fluent
    AuthorizationService triggerOnboarding(Handler<AsyncResult<JsonObject>> handler);
}
