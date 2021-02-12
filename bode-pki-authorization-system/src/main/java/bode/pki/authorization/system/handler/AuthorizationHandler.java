package bode.pki.authorization.system.handler;

import bode.pki.authorization.system.services.authorization.AuthorizationService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationHandler extends ContextHandler {

    private AuthorizationService authService;

    public AuthorizationHandler(Vertx vertx, String address) {
        authService = AuthorizationService.createProxy(vertx, address);
    }

    public void checkNonce(RoutingContext context) {
        String nonce = context.queryParams().get("nonce");
        authService.checkNonce(nonce, ar -> handleContext(context, ar));
    }

    public void getNonce(RoutingContext context) {
        String endpoint = context.queryParams().get("endpoint");
        authService.getNonce(endpoint, ar -> handleContext(context, ar));
    }

    public void getProof(RoutingContext context) {
        authService.getProof(ar -> handleContext(context, ar));
    }

    public void getCertificates(RoutingContext context) {
        authService.getCertificates(ar -> handleContext(context, ar));
    }

    public void getAuthorizationDatabase(RoutingContext context) {
        authService.getAuthorizationDatabase(ar -> handleContext(context, ar));
    }

    public void updateAuthorizationDatabase(RoutingContext context) {
        authService.updateAuthorizationDatabase(context.getBodyAsJson(), ar -> handleContext(context, ar));
    }

    public void getSignedRootHash(RoutingContext context) {
        authService.getSignedRootHash(ar -> handleContext(context, ar));
    }

    public void checkSynced(RoutingContext context) {
        authService.checkSynced(context.getBodyAsJson(), ar -> handleContext(context, ar));
    }

    public void isSynced(RoutingContext context) {
        authService.isSynced(ar -> handleContext(context, ar));
    }

    public void triggerOnboarding(RoutingContext context) {
        authService.triggerOnboarding(ar -> handleContext(context, ar));
    }

    public void addCertificateToAllowMap(RoutingContext context) {
        authService.addCertificateToAllowMap(context.getBodyAsJson(), ar -> handleContext(context, ar));
    }

    public void removeCertificateFromAllowMap(RoutingContext context) {
        String uuid = context.queryParams().get("uuid");
        authService.removeCertificateFromAllowMap(uuid, ar -> handleContext(context, ar));
    }

    public void getAllowMap(RoutingContext context) {
        authService.getAllowMap(ar -> handleContext(context, ar));
    }

    public void revokeCertificate(RoutingContext context) {
        String uuid = context.queryParams().get("uuid");
        authService.revokeCertificate(uuid, ar -> handleContext(context, ar));
    }
}
