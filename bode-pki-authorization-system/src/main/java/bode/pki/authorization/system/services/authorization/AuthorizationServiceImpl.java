package bode.pki.authorization.system.services.authorization;

import bode.pki.authorization.system.utils.authorization.AuthorizationSystem;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class AuthorizationServiceImpl implements AuthorizationService {

    private AuthorizationSystem authorizationSystem;

    AuthorizationServiceImpl(AuthorizationSystem authorizationSystem, Handler<AsyncResult<AuthorizationService>> handler) {
        this.authorizationSystem = authorizationSystem;
        handler.handle(Future.succeededFuture(this));
    }

    @Override
    public AuthorizationService checkNonce(String nonce, Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.checkNonce(nonce, checkNonceResult -> {
            if (checkNonceResult.succeeded()) {
                handler.handle(Future.succeededFuture(checkNonceResult.result()));
            } else {
                handler.handle(Future.failedFuture(checkNonceResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getNonce(String endpoint, Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getNonce(endpoint, getNonceResult -> {
            if (getNonceResult.succeeded()) {
                handler.handle(Future.succeededFuture(getNonceResult.result()));
            } else {
                handler.handle(Future.failedFuture(getNonceResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getAuthorizationDatabase(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getAuthorizationDatabase(getAuthorizationDatabaseResultResult -> {
            if (getAuthorizationDatabaseResultResult.succeeded()) {
                handler.handle(Future.succeededFuture(getAuthorizationDatabaseResultResult.result()));
            } else {
                handler.handle(Future.failedFuture(getAuthorizationDatabaseResultResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getAllowMap(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getAllowMap(getAllowMapResult -> {
            if (getAllowMapResult.succeeded()) {
                handler.handle(Future.succeededFuture(getAllowMapResult.result()));
            } else {
                handler.handle(Future.failedFuture(getAllowMapResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService addCertificateToAllowMap(JsonObject payload,
                                                         Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.addCertificateToAllowMap(payload, addCertificateToAllowMapResult -> {
            if (addCertificateToAllowMapResult.succeeded()) {
                handler.handle(Future.succeededFuture(addCertificateToAllowMapResult.result()));
            } else {
                handler.handle(Future.failedFuture(addCertificateToAllowMapResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService removeCertificateFromAllowMap(String uuid,
                                                              Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.removeCertificateFromAllowMap(uuid, removeCertificateFromAllowMapResult -> {
            if (removeCertificateFromAllowMapResult.succeeded()) {
                handler.handle(Future.succeededFuture(removeCertificateFromAllowMapResult.result()));
            } else {
                handler.handle(Future.failedFuture(removeCertificateFromAllowMapResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService revokeCertificate(String uuid,
                                                  Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.revokeCertificate(uuid, revokeCertificateResult -> {
            if (revokeCertificateResult.succeeded()) {
                handler.handle(Future.succeededFuture(revokeCertificateResult.result()));
            } else {
                handler.handle(Future.failedFuture(revokeCertificateResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService sign(String message, String privateKey,
                                     Handler<AsyncResult<String>> handler) {
        authorizationSystem.sign(message, privateKey, signResult -> {
            if (signResult.succeeded()) {
                handler.handle(Future.succeededFuture(signResult.result()));
            } else {
                handler.handle(Future.failedFuture(signResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService verify(String message, String signature, String publicKey,
                                       Handler<AsyncResult<String>> handler) {
        authorizationSystem.verify(message, signature, publicKey, verifyResult -> {
            if (verifyResult.succeeded()) {
                handler.handle(Future.succeededFuture(verifyResult.result()));
            } else {
                handler.handle(Future.failedFuture(verifyResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getProof(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getProof(getProofResult -> {
            if (getProofResult.succeeded()) {
                handler.handle(Future.succeededFuture(getProofResult.result()));
            } else {
                handler.handle(Future.failedFuture(getProofResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService triggerOnboarding(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.triggerOnboarding(triggerOnboardingResult -> {
            if (triggerOnboardingResult.succeeded()) {
                handler.handle(Future.succeededFuture(triggerOnboardingResult.result()));
            } else {
                handler.handle(Future.failedFuture(triggerOnboardingResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getCertificates(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getCertificates(getCertificatesResult -> {
            if (getCertificatesResult.succeeded()) {
                handler.handle(Future.succeededFuture(getCertificatesResult.result()));
            } else {
                handler.handle(Future.failedFuture(getCertificatesResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService updateAuthorizationDatabase(JsonObject payload,
                                                            Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.updateAuthorizationDatabase(payload, updateAuthorizationDatabaseResult -> {
            if (updateAuthorizationDatabaseResult.succeeded()) {
                handler.handle(Future.succeededFuture(updateAuthorizationDatabaseResult.result()));
            } else {
                handler.handle(Future.failedFuture(updateAuthorizationDatabaseResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService getSignedRootHash(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.getSignedRootHash(getSignedRootHashResult -> {
            if (getSignedRootHashResult.succeeded()) {
                handler.handle(Future.succeededFuture(getSignedRootHashResult.result()));
            } else {
                handler.handle(Future.failedFuture(getSignedRootHashResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService checkSynced(JsonObject payload, Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.checkSynced(payload, checkSyncedResult -> {
            if (checkSyncedResult.succeeded()) {
                handler.handle(Future.succeededFuture(checkSyncedResult.result()));
            } else {
                handler.handle(Future.failedFuture(checkSyncedResult.cause()));
            }
        });
        return this;
    }

    @Override
    public AuthorizationService isSynced(Handler<AsyncResult<JsonObject>> handler) {
        authorizationSystem.isSynced(isSyncedResult -> {
            if (isSyncedResult.succeeded()) {
                handler.handle(Future.succeededFuture(isSyncedResult.result()));
            } else {
                handler.handle(Future.failedFuture(isSyncedResult.cause()));
            }
        });
        return this;
    }
}

