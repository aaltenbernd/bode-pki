package bode.pki.authorization.system.utils.authorization;

import bode.pki.authorization.system.utils.key.PrivateKeyReader;
import bode.pki.authorization.system.utils.response.ReturnHelper;
import bode.pki.authorization.system.utils.signature.SignatureHelper;
import bode.pki.authorization.system.utils.certificate.Certificate;
import bode.pki.authorization.system.utils.node.NodeConnector;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import bode.pki.authorization.system.utils.hash.HashBlock;
import bode.pki.authorization.system.utils.key.PublicKeyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.util.*;

public class AuthorizationSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationSystem.class);

    private AuthorizationDatabase authorizationDatabase;

    private Certificate primaryCertificate;
    private PrivateKey primaryPrivateKey;

    private Certificate replicaCertificate;
    private PrivateKey replicaPrivateKey;

    private Map<String, Certificate> allowed;
    private Map<String, Certificate> nonces;

    private List<String> allowedAuthorizationDatabaseEndpoints;
    private Map<String, Certificate> certificates;
    private int lastAuthorizationDatabaseSize;

    private CircuitBreaker breaker;
    private WebClient client;

    private JsonObject proof;

    private NodeConnector nodeConnector;

    private JsonObject synced;

    private Random randomGenerator;

    public static void create(Vertx vertx, JsonObject config,
                              Handler<AsyncResult<AuthorizationSystem>> handler) {
        new AuthorizationSystem(vertx, config, handler);
    }

    private AuthorizationSystem(Vertx vertx, JsonObject config, Handler<AsyncResult<AuthorizationSystem>> handler) {
        client = WebClient.create(vertx);

        randomGenerator = new Random();

        breaker = CircuitBreaker.create("send-breaker", vertx,
                new CircuitBreakerOptions()
                        .setMaxRetries(1000)
                        .setTimeout(-1)
        ).retryPolicy(retryCount -> {
            LOGGER.debug("[BREAKER] This was try number " + retryCount
                    + ". Next try in " + retryCount * 5 + " seconds.");
            return retryCount * 5000L;
        });

        JsonObject primary = config.getJsonObject("PRIMARY");

        if (primary == null || primary.isEmpty()) {
            handler.handle(Future.failedFuture("Primary config is missing!"));
            return;
        }

        nonces = new HashMap<>();
        allowed = new HashMap<>();

        allowedAuthorizationDatabaseEndpoints = new ArrayList<>();
        certificates = new HashMap<>();
        lastAuthorizationDatabaseSize = 0;
        synced = new JsonObject();

        proof = new JsonObject().put("signature", "");

        List<Future> futureList = new ArrayList<>();

        Promise<Void> authorizationDatabaseReader = Promise.promise();
        futureList.add(authorizationDatabaseReader.future());
        vertx.fileSystem().readFile("store/authorization_database.json", ar -> {
            if (ar.succeeded()) {
                JsonObject result = new JsonObject(ar.result());
                authorizationDatabase = Json.decodeValue(result.toString(), AuthorizationDatabase.class);
            } else {
                authorizationDatabase = new AuthorizationDatabase();
            }
            calcLists();
            authorizationDatabaseReader.complete();
        });

        JsonObject primaryCertificate = primary.getJsonObject("certificate");
        if (primaryCertificate == null ||primaryCertificate.isEmpty()) {
            handler.handle(Future.failedFuture("Primary certificate missing"));
            return;
        } else {
            try {
                this.primaryCertificate = Json.decodeValue(primaryCertificate.toString(), Certificate.class);
            } catch (DecodeException e) {
                e.printStackTrace();
                handler.handle(Future.failedFuture(e.getCause()));
                return;
            }
        }

        JsonObject replica = config.getJsonObject("REPLICA");
        if (replica == null || replica.isEmpty()) {
            String privateKey = primary.getString("private_key");

            if (privateKey == null || privateKey.isEmpty()) {
                handler.handle(Future.failedFuture("Primary private key is null or empty"));
                return;
            }

            Promise<Void> privateKeyReaderPromise = Promise.promise();
            futureList.add(privateKeyReaderPromise.future());
            PrivateKeyReader.getECDSA(privateKey, keyReaderResult -> {
                if (keyReaderResult.succeeded()) {
                    primaryPrivateKey = keyReaderResult.result();
                    privateKeyReaderPromise.complete();
                } else {
                    privateKeyReaderPromise.fail(keyReaderResult.cause());
                }

            });
        } else {
            Integer syncPeriod = replica.getInteger("sync_period", 5000);

            JsonObject connector = replica.getJsonObject("connector");
            if (connector == null || connector.isEmpty()) {
                handler.handle(Future.failedFuture("Replica connector is null"));
                return;
            }

            Integer nodeConenctorUpdatePeriod = connector.getInteger("update_period", 5000);

            Integer nodeConnectorPort = connector.getInteger("port");
            if (nodeConnectorPort == null) {
                handler.handle(Future.failedFuture("Replica nodeConnectorPort is null"));
                return;
            }

            JsonObject replicaCertificate = replica.getJsonObject("certificate");
            String privateKey = replica.getString("private_key");


            if (replicaCertificate == null || replicaCertificate.isEmpty()) {
                handler.handle(Future.failedFuture("Replica certificate is null or empty"));
                return;
            }

            if (privateKey == null || privateKey.isEmpty()) {
                handler.handle(Future.failedFuture("Replica private key is null or empty"));
                return;
            }

            Promise<Void> privateKeyReaderPromise = Promise.promise();
            futureList.add(privateKeyReaderPromise.future());
            PrivateKeyReader.getECDSA(privateKey, keyReaderResult -> {
                if (keyReaderResult.succeeded()) {
                    replicaPrivateKey = keyReaderResult.result();
                    privateKeyReaderPromise.complete();
                } else {
                    privateKeyReaderPromise.fail(keyReaderResult.cause());
                }
            });

            try {
                this.replicaCertificate = Json.decodeValue(replicaCertificate.toString(), Certificate.class);
            } catch (DecodeException e) {
                e.printStackTrace();
                handler.handle(Future.failedFuture(e.getCause()));
                return;
            }

            String nodeConnectorType = connector.getString("type");
            Promise<Void> createNodeConnectorPromise = Promise.promise();
            futureList.add(createNodeConnectorPromise.future());

            authorizationDatabaseReader.future().setHandler(ar ->
                    NodeConnector.create(vertx, this.replicaCertificate, nodeConnectorType, nodeConnectorPort,
                            nodeConnectorResult -> {
                        if (ar.succeeded()) {
                            nodeConnector = nodeConnectorResult.result();
                            createNodeConnectorPromise.complete();
                        } else {
                            createNodeConnectorPromise.fail(ar.cause());
                        }
            }));

            CompositeFuture.all(privateKeyReaderPromise.future(), createNodeConnectorPromise.future())
                    .setHandler(ar -> {
                vertx.setPeriodic(nodeConenctorUpdatePeriod, periodic ->
                        nodeConnector.updateNode(LOGGER, certificates.values()));
                vertx.setPeriodic(syncPeriod, periodic -> sync());
            });
        }

        CompositeFuture.all(futureList).setHandler(ar -> {
            if (ar.succeeded()) {
                vertx.setPeriodic(15000, periodic ->
                        vertx.fileSystem().writeFile("store/authorization_database.json",
                        authorizationDatabase.toJson().toBuffer(), storeAuthorizationDatabaseResult -> {
                            if (storeAuthorizationDatabaseResult.succeeded()) {
                                LOGGER.debug("[STORING] Successfully stored authorization database");
                            } else {
                                storeAuthorizationDatabaseResult.cause().printStackTrace();
                            }
                        }));
                handler.handle(Future.succeededFuture(this));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    // API:

    public void checkNonce(String nonce, Handler<AsyncResult<JsonObject>> handler) {
        Certificate certificate = nonces.get(nonce);
        if (certificate == null) {
            handler.handle(Future.failedFuture(
                    ReturnHelper.returnFailure(400, "Nonce not known")));
            return;
        }

        client.getAbs(certificate.getAuthorizationSystemEndpoint() + "/proof").send(ar -> {
            JsonObject result = ar.result().bodyAsJsonObject();
            String signature = result.getString("signature");

            PublicKeyReader.getECDSA(certificate.getKey(), ecdsaResult -> {
                if (SignatureHelper.verifySHA256withECDSA(nonce, signature, ecdsaResult.result())) {
                    authorizationDatabase.addCertificate(certificate, primaryPrivateKey);
                    getAuthorizationDatabase(handler);
                    handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                            new JsonObject().put("message", "Successfully checked nonce"))));
                    nonces.remove(nonce);
                    allowed.remove(certificate.getUuid());
                    pushToReplicas();
                } else {
                    handler.handle(Future.failedFuture(
                            ReturnHelper.returnFailure(400, "Signature invalid")));
                }
            });
        });
    }

    public void getNonce(String endpoint, Handler<AsyncResult<JsonObject>> handler) {
        boolean permitted = false;
        Certificate certificate = null;
        for (String uuid : allowed.keySet()) {
            certificate = allowed.get(uuid);
            if (certificate.getAuthorizationSystemEndpoint().equals(endpoint)) {
                permitted = true;
                break;
            }
        }
        if (permitted) {
            for (String key : nonces.keySet()) {
                if (certificate.equals(nonces.get(key))) {
                    handler.handle(Future.succeededFuture(
                            ReturnHelper.returnSuccess(200, new JsonObject().put("nonce", key))));
                    return;
                }
            }
            Random rand = new Random();
            long randomLong = rand.nextLong();
            String nonce = Long.toHexString(randomLong);
            nonces.put(nonce, certificate);
            handler.handle(Future.succeededFuture(
                    ReturnHelper.returnSuccess(200, new JsonObject().put("nonce", nonce))));
        } else {
            handler.handle(Future.failedFuture(
                    ReturnHelper.returnFailure(400, "Endpoint not permitted")));
        }
    }

    public void getProof(Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200, proof)));
    }

    public void getCertificates(Handler<AsyncResult<JsonObject>> handler) {
        JsonArray certificatesJson = new JsonArray();
        for (Certificate certificate : certificates.values()) {
            certificatesJson.add(certificate.toJson());
        }

        handler.handle(Future.succeededFuture(
                ReturnHelper.returnSuccess(200, new JsonObject().put("certificates", certificatesJson)
        )));
    }

    public void getAuthorizationDatabase(Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200, authorizationDatabase.toJson())));
    }

    public void updateAuthorizationDatabase(JsonObject payload, Handler<AsyncResult<JsonObject>> handler) {
        PublicKeyReader.getECDSA(primaryCertificate.getKey(), ar -> {
            if (ar.succeeded()) {
                AuthorizationDatabase authorizationDatabase =
                        Json.decodeValue(payload.toString(), AuthorizationDatabase.class);
                LOGGER.debug("[UPDATE AUTHORIZATION DATABASE] Received: "
                        + authorizationDatabase.toJson().encodePrettily());
                if (authorizationDatabase.verify(ar.result())) {
                    if (this.authorizationDatabase.consistencyCheck(authorizationDatabase)) {
                        authorizationDatabase.merge(this.authorizationDatabase);
                        this.authorizationDatabase = authorizationDatabase;
                        pushToReplicas();
                        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200, payload)));
                    } else {
                        handler.handle(Future.failedFuture(ReturnHelper
                                .returnFailure(400, "Authorization Database is not consistent")));
                    }
                } else {
                    handler.handle(Future.failedFuture(ReturnHelper
                            .returnFailure(400, "Hashes or signatures invalid, or inconsistent")));
                }
            } else {
                handler.handle(Future.failedFuture(ReturnHelper.returnFailure(400, "Key invalid")));
            }
        });
    }

    public void getSignedRootHash(Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(
                ReturnHelper.returnSuccess(200, authorizationDatabase.signedRootHashToJson())));
    }

    public void checkSynced(JsonObject payload, Handler<AsyncResult<JsonObject>> handler) {
        String endpoint = payload.getString("endpoint");
        if (allowedAuthorizationDatabaseEndpoints.contains(endpoint)) {
            PublicKeyReader.getECDSA(primaryCertificate.getKey(), ar -> {
                if (ar.succeeded()) {
                    LOGGER.debug("[SYNC] Received: " + payload.encodePrettily());
                    String rootHash = payload.getString("rootHash");
                    String signature = payload.getString("signature");
                    boolean verify = SignatureHelper.verifySHA256withECDSA(rootHash, signature, ar.result());
                    if (verify) {
                        if (authorizationDatabase.getRootHash().equals(rootHash)) {
                            handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                                    authorizationDatabase.signedRootHashToJson())));
                        } else {
                            JsonObject recent = this.authorizationDatabase.recentToJson(rootHash);
                            if (recent == null) {
                                handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                                        authorizationDatabase.signedRootHashToJson())));
                            } else {
                                handler.handle(Future.succeededFuture(
                                        ReturnHelper.returnSuccess(200, recent)));
                            }
                        }
                    } else {
                        handler.handle(Future.failedFuture(
                                ReturnHelper.returnFailure(400, "Signature invalid")));
                    }
                } else {
                    handler.handle(Future.failedFuture(ReturnHelper.returnFailure(400, "Key invalid")));
                }
            });
        } else {
            handler.handle(
                    Future.failedFuture(ReturnHelper.returnFailure(400, "Endpoint not allowed")));
        }
    }

    public void isSynced(Handler<AsyncResult<JsonObject>> handler) {
        JsonObject result = new JsonObject();
        LOGGER.debug(synced.toString());
        for(Certificate certificate : certificates.values()) {
            if (!replicaCertificate.getAuthorizationSystemEndpoint()
                    .equals(certificate.getAuthorizationSystemEndpoint())) {
                if (synced.getJsonObject(certificate.getAuthorizationSystemEndpoint()) != null) {
                    int code =
                            synced.getJsonObject(certificate.getAuthorizationSystemEndpoint()).getInteger("synced");
                    if (code == 1) {
                        LOGGER.debug(synced.getJsonObject(
                                certificate.getAuthorizationSystemEndpoint()).encodePrettily());
                        String rootHash = synced.getJsonObject(
                                certificate.getAuthorizationSystemEndpoint()).getString("rootHash");
                        if (!rootHash.equals(authorizationDatabase.getRootHash())) {
                            code = 2;
                        }
                    }
                    result.put(certificate.getHost(), code);
                } else {
                    result.put(certificate.getHost(), 0);
                }
            }
        }
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200, result)));
    }

    // ONBOARDING:

    public void triggerOnboarding(Handler<AsyncResult<JsonObject>> handler) {
        client.getAbs(primaryCertificate.getAuthorizationSystemEndpoint() + "/nonce?endpoint=" + replicaCertificate.getAuthorizationSystemEndpoint())
                .send(getNonceResult -> {
            if (getNonceResult.succeeded()) {
                int statusGetNonceResult = getNonceResult.result().statusCode();
                JsonObject bodyGetNonceResult = getNonceResult.result().bodyAsJsonObject();
                if (statusGetNonceResult == 200) {
                    String nonce = bodyGetNonceResult.getString("nonce");
                    LOGGER.debug("[ONBOARDING] Succeeded to connect to " + primaryCertificate.getAuthorizationSystemEndpoint() +
                            "/nonce?endpoint=" + replicaCertificate.getAuthorizationSystemEndpoint());
                    LOGGER.debug("[ONBOARDING] " + getNonceResult.result().bodyAsJsonObject());
                    proof.put("signature", SignatureHelper.signSHA256withECDSA(nonce, replicaPrivateKey));
                    client.getAbs(primaryCertificate.getAuthorizationSystemEndpoint() + "/checkNonce?nonce=" + nonce)
                            .send(checkNonceResult -> {
                        if (checkNonceResult.succeeded()) {
                            int statusCheckNonceResult = checkNonceResult.result().statusCode();
                            JsonObject bodyCheckNonceResult = checkNonceResult.result().bodyAsJsonObject();
                            if (statusCheckNonceResult == 200) {
                                LOGGER.debug("[ONBOARDING] Succeeded to connect to " +
                                        primaryCertificate.getAuthorizationSystemEndpoint() + "/checkNonce?nonce=" + nonce);
                                proof.put("signature", "");
                            } else {
                                LOGGER.error("[TRIGGER PROOF] " +
                                        bodyCheckNonceResult.encodePrettily());
                            }
                        } else {
                            LOGGER.error("[ONBOARDING] " + checkNonceResult.cause().getMessage());
                        }
                    });
                } else {
                    LOGGER.error("[ONBOARDING] " + bodyGetNonceResult.encodePrettily());
                }
            } else {
                LOGGER.error("[ONBOARDING] " + getNonceResult.cause().getMessage());
            }
        });
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                new JsonObject().put("message", "Successfully triggered onboarding"))));
    }

    public void getAllowMap(Handler<AsyncResult<JsonObject>> handler) {
        JsonObject result = new JsonObject();
        for (String key : allowed.keySet()) {
            result.put(key, allowed.get(key).toJson());
        }
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200, result)));
    }

    public void addCertificateToAllowMap(JsonObject payload, Handler<AsyncResult<JsonObject>> handler) {
        try {
            Certificate certificate = new Certificate();
            certificate.setKey(payload.getString("key"));
            certificate.setHost(payload.getString("host"));
            certificate.setAsPort(payload.getInteger("asPort"));
            certificate.setNodePort(payload.getInteger("nodePort"));
            for (Certificate other : certificates.values()) {
                if (other.equalsBare(certificate)) {
                    handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                            new JsonObject().put("message", "Already onboarded"))));
                    return;
                }
            }
            allowed.put(certificate.getUuid(), certificate);
            handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                    new JsonObject().put("message", "Successfully added certificate to allow map"))));
        } catch (DecodeException e) {
            handler.handle(Future.failedFuture("Wrong input"));
        }
    }

    public void removeCertificateFromAllowMap(String uuid, Handler<AsyncResult<JsonObject>> handler) {
        allowed.remove(uuid);
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                new JsonObject().put("message", "Successfully removed certificate from allow map"))));
    }

    public void revokeCertificate(String uuid, Handler<AsyncResult<JsonObject>> handler) {
        authorizationDatabase.revokeCertificate(uuid, primaryPrivateKey);
        pushToReplicas();
        handler.handle(Future.succeededFuture(ReturnHelper.returnSuccess(200,
                new JsonObject().put("message", "Successfully revoked certificate"))));
    }

    // CLI:

    public void sign(String message, String privateKey, Handler<AsyncResult<String>> handler) {
        PrivateKeyReader.getECDSA(privateKey, ar -> {
            if (ar.succeeded()) {
                String signature = SignatureHelper.signSHA256withECDSA(message, ar.result());
                handler.handle(Future.succeededFuture(signature));
            } else {
                handler.handle(Future.failedFuture(ar.cause().getMessage()));
            }
        });
    }

    public void verify(String message, String signature, String naPublicKey, Handler<AsyncResult<String>> handler) {
        PublicKeyReader.getECDSA(naPublicKey, ar -> {
            if (ar.succeeded()) {
                boolean verified = SignatureHelper.verifySHA256withECDSA(message, signature, ar.result());
                handler.handle(Future.succeededFuture(Boolean.toString(verified)));
            } else {
                handler.handle(Future.failedFuture(ar.cause().getMessage()));
            }
        });
    }

    // HELPER:

    private void calcLists() {
        int currentAuthorizationDatabaseSize = authorizationDatabase.size();
        calcCertificates(lastAuthorizationDatabaseSize, authorizationDatabase.size());
        lastAuthorizationDatabaseSize = currentAuthorizationDatabaseSize;

        List<String> allowedAuthorizationDatabaseEndpoints = new ArrayList<>();
        for (Certificate certificate : certificates.values()) {
            if (!certificate.isRevoked()) {
                allowedAuthorizationDatabaseEndpoints.add(certificate.getAuthorizationSystemEndpoint());
            }
        }
        this.allowedAuthorizationDatabaseEndpoints = allowedAuthorizationDatabaseEndpoints;
    }

    private void calcCertificates(int from, int to) {
        for (HashBlock hashBlock : authorizationDatabase.getBlocks().subList(from, to)) {
            Certificate certificate = hashBlock.getCertificate();
            certificates.put(certificate.getUuid(), certificate);
        }
    }

    private void pushToReplicas() {
        this.calcLists();
        for (Object obj : allowedAuthorizationDatabaseEndpoints) {
            String authorizationDatabaseEndpoint = (String) obj;
            if (replicaCertificate == null) {
                pushToReplicas(authorizationDatabaseEndpoint);
            } else {
                if (!authorizationDatabaseEndpoint.equals(replicaCertificate.getAuthorizationSystemEndpoint())) {
                    pushToReplicas(authorizationDatabaseEndpoint);
                }
            }
        }
    }

    private void pushToReplicas(String endpoint) {
        get(endpoint + "/signedRootHash", ar -> {
            if (ar.succeeded()) {
                String rootHash = ar.result().getString("rootHash");
                if (!this.authorizationDatabase.getRootHash().equals(rootHash)) {
                    JsonObject recent = this.authorizationDatabase.recentToJson(rootHash);
                    if (recent != null) {
                        send(this.authorizationDatabase.recentToJson(rootHash),
                                endpoint + "/authorizationDatabase");
                    }
                }
            }
        });
    }

    private void sync() {
        this.calcLists();
        if (allowedAuthorizationDatabaseEndpoints.size() > 0) {
            int index = randomGenerator.nextInt(allowedAuthorizationDatabaseEndpoints.size());
            String authorizationDatabaseEndpoint = allowedAuthorizationDatabaseEndpoints.get(index);
            if (!authorizationDatabaseEndpoint.equals(replicaCertificate.getAuthorizationSystemEndpoint())) {
                sync(authorizationDatabaseEndpoint);
            }
        }
    }

    private void sync(String endpoint) {
        client.putAbs(endpoint + "/sync").sendJson(authorizationDatabase.signedRootHashToJson()
                .put("endpoint", replicaCertificate.getAuthorizationSystemEndpoint()), sendResult -> {
            if (sendResult.succeeded()) {
                JsonObject otherResult = sendResult.result().bodyAsJsonObject();
                if (otherResult.containsKey("chain")) {
                    updateAuthorizationDatabase(otherResult, updateAuthorizationDatabaseResult -> {
                        if (updateAuthorizationDatabaseResult.succeeded()) {
                            // CASE 2:
                            JsonObject synced = new JsonObject();
                            synced.put("rootHash", otherResult.getJsonObject("chain").getString("rootHash"));
                            synced.put("synced", 1);
                            synced.put("message", "Replica is synced #1");
                            this.synced.put(endpoint, synced);
                        } else {
                            // FAILURE, something wrong with replica.
                            JsonObject synced = new JsonObject();
                            synced.put("rootHash", otherResult.getJsonObject("chain").getString("rootHash"));
                            synced.put("synced", 4);
                            synced.put("message", "Replica provides an invalid authorization database");
                            this.synced.put(endpoint, synced);
                        }
                    });
                } else if (otherResult.containsKey("rootHash")) {
                    PublicKeyReader.getECDSA(primaryCertificate.getKey(), ar -> {
                        if (ar.succeeded()) {
                            LOGGER.debug("[SYNC] Received: " + otherResult.encodePrettily());
                            String rootHash = otherResult.getString("rootHash");
                            String signature = otherResult.getString("signature");
                            if (rootHash.isEmpty() && signature.isEmpty()) {
                                // CASE EMPTY:
                                send(this.authorizationDatabase.toJson(), endpoint + "/authorizationDatabase");
                                JsonObject synced = new JsonObject();
                                synced.put("rootHash", this.authorizationDatabase.getRootHash());
                                synced.put("synced", 1);
                                synced.put("message", "Replica is synced #2");
                                this.synced.put(endpoint, synced);
                            } else {
                                boolean verify =
                                        SignatureHelper.verifySHA256withECDSA(rootHash, signature, ar.result());
                                if (verify) {
                                    if (authorizationDatabase.getRootHash().equals(rootHash)) {
                                        // CASE 1:
                                        JsonObject synced = new JsonObject();
                                        synced.put("rootHash", rootHash);
                                        synced.put("synced", 1);
                                        synced.put("message", "Replica is synced #3");
                                        this.synced.put(endpoint, synced);
                                    } else {
                                        JsonObject recent = this.authorizationDatabase.recentToJson(rootHash);
                                        if (recent != null) {
                                            // CASE 3A:
                                            send(recent, endpoint + "/authorizationDatabase");
                                            JsonObject synced = new JsonObject();
                                            synced.put("rootHash", this.authorizationDatabase.getRootHash());
                                            synced.put("synced", 1);
                                            synced.put("message", "Replica is synced #4");
                                            this.synced.put(endpoint, synced);
                                        } else {
                                            // CASE 3B:
                                            JsonObject synced = new JsonObject();
                                            synced.put("rootHash", rootHash);
                                            synced.put("synced", 3);
                                            synced.put("message", "Equivocate authorization database alert");
                                            this.synced.put(endpoint, synced);
                                        }
                                    }
                                } else {
                                    JsonObject synced = new JsonObject();
                                    synced.put("rootHash", authorizationDatabase.getRootHash());
                                    synced.put("synced", 4);
                                    synced.put("message", "Replica provides an invalid signature");
                                    this.synced.put(endpoint, synced);
                                }
                            }
                        }
                    });
                } else {
                    JsonObject synced = new JsonObject();
                    synced.put("rootHash", authorizationDatabase.getRootHash());
                    synced.put("synced", 4);
                    synced.put("message", "Replica provides no signed root hash or authorization database");
                    this.synced.put(endpoint, synced);
                }
            } else {
                // FAILURE, something wrong with replica.
                JsonObject synced = new JsonObject();
                synced.putNull("rootHash");
                synced.put("synced", 5);
                synced.put("message", "Replica is not reachable");
                this.synced.put(endpoint, synced);
            }
        });
    }

    private void get(String uri, Handler<AsyncResult<JsonObject>> handler) {
        breaker.<JsonObject>execute(
                future -> {
                    LOGGER.debug("[GET] Trying to get from " + uri);
                    client.getAbs(uri).send(ar -> {
                        if (ar.succeeded()) {
                            LOGGER.debug("[GET] Succeeded to connect to " + uri);
                            future.complete(ar.result().bodyAsJsonObject());
                        } else {
                            LOGGER.error("[GET] " + ar.cause().getMessage());
                            future.fail(ar.cause());
                        }
                    });
                }).setHandler(ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause().getMessage()));
            }
        });
    }

    private void send(JsonObject payload, String uri) {
        breaker.<JsonObject>execute(
                future -> {
                    LOGGER.debug("[SEND] Trying to send to " + uri);
                    client.putAbs(uri).sendJson(payload, ar -> {
                        if (ar.succeeded()) {
                            LOGGER.debug("[SEND] Succeeded to connect to " + uri);
                            future.complete(ar.result().bodyAsJsonObject());
                        } else {
                            LOGGER.error("[SEND] " + ar.cause().getMessage());
                            future.fail(ar.cause());
                        }
                    });
                }).setHandler(ar -> {
        });
    }
}
