package bode.pki.authorization.system.utils.node;

import bode.pki.authorization.system.utils.hash.HashHelper;
import bode.pki.authorization.system.utils.certificate.Certificate;
import io.vertx.core.*;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BesuConnector implements NodeConnector {

    private Certificate nodeCertificate;
    private int port;
    private Vertx vertx;
    private DnsClient dnsClient;

    private List<String> nodesAllowed;
    private List<String> nodesRevoked;
    private List<String> accountsAllowed;
    private List<String> accountsRevoked;

    public BesuConnector(Vertx vertx, Certificate nodeCertificate, int port,
                         Handler<AsyncResult<BesuConnector>> handler) {
        this.vertx = vertx;
        this.nodeCertificate = nodeCertificate;
        this.port = port;
        this.dnsClient = vertx.createDnsClient();

        this.nodesAllowed = new ArrayList<>();
        this.nodesRevoked = new ArrayList<>();
        this.accountsAllowed = new ArrayList<>();
        this.accountsRevoked = new ArrayList<>();

        handler.handle(Future.succeededFuture(this));
    }

    private String getAccountAddress(Certificate certificate) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            MessageDigest digest = MessageDigest.getInstance("KECCAK-256");
            return "0x" + HashHelper.hashStringLastTwenty(digest, certificate.getKey());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Usage: host
    private String getENode(Certificate certificate) {
        return "enode://" + certificate.getKey() + "@" + certificate.getHost() + ":" + certificate.getNodePort();
    }

    // Usage: production, node must be reachable under node.domain
    private String getENode(String ip, Certificate certificate) {
        return "enode://" + certificate.getKey() + "@" + ip + ":" + certificate.getNodePort();
    }

    public String getNodeAddress(Certificate certificate) {
        return getENode(certificate);
    }

    private void getNodeAddress(Certificate certificate, DnsClient dnsClient,
                                Handler<AsyncResult<String>> handler) {
        if (dnsClient == null) {
            handler.handle(Future.succeededFuture(getENode(certificate)));
        } else {
            dnsClient.resolveA("node." + certificate.getHost(), ar -> {
                if (ar.succeeded()) {
                    if (!ar.result().isEmpty()) {
                        handler.handle(Future.succeededFuture(getENode(ar.result().get(0), certificate)));
                    } else {
                        handler.handle(Future.failedFuture("Host not found"));
                    }
                } else {
                    handler.handle(Future.failedFuture(ar.cause()));
                }
            });
        }
    }

    private void processWorldState(Collection<Certificate> certificates, Handler<AsyncResult<Void>> handler) {
        List<String> nodesAllowed = new ArrayList<>();
        List<String> nodesRevoked = new ArrayList<>();
        List<String> accountsAllowed = new ArrayList<>();
        List<String> accountsRevoked = new ArrayList<>();

        List<Future> futureList = new ArrayList<>();
        for (Certificate certificate : certificates) {
            Promise<String> nodeAddressPromise = Promise.promise();
            futureList.add(nodeAddressPromise.future());
            getNodeAddress(certificate, dnsClient, ar -> {
                if (ar.succeeded()) {
                    if (!certificate.isRevoked()) {
                        nodesAllowed.add(ar.result());
                        accountsAllowed.add(getAccountAddress(certificate));
                    } else {
                        nodesRevoked.add(ar.result());
                        accountsRevoked.add(getAccountAddress(certificate));
                    }
                    nodeAddressPromise.complete(ar.result());
                } else {
                    nodeAddressPromise.fail(ar.cause());
                }
            });
        }

        CompositeFuture.all(futureList).setHandler(ar -> {
            this.nodesAllowed = nodesAllowed;
            this.nodesRevoked = nodesRevoked;
            this.accountsAllowed = accountsAllowed;
            this.accountsRevoked = accountsRevoked;
            handler.handle(Future.succeededFuture());
        });
    }

    private void updateBesuNode(Logger LOGGER) {
        HttpClient wsClient = vertx.createHttpClient();
        Promise<Void> webSocketPromise = Promise.promise();
        webSocketPromise.future().setHandler(ar -> wsClient.close());
        wsClient.webSocket(port, nodeCertificate.getNodeHost(), "", context -> {
            if (context.succeeded()) {
                LOGGER.debug("[UPDATE] START");
                context.result().textMessageHandler(result -> {
                    JsonObject response = new JsonObject(result);
                    if (response.getString("id").equals("updateAddNodesProcess")) {
                        LOGGER.debug("[UPDATE] Current nodes allow-list "
                                + response.getJsonArray("result").size());

                        JsonArray addToAllowList = new JsonArray();
                        for (Object obj : nodesAllowed) {
                            if (!response.getJsonArray("result").contains(obj)) {
                                addToAllowList.add(obj);
                            }
                        }
                        LOGGER.debug("[UPDATE] Add to nodes allow-list add " + addToAllowList.size());

                        JsonObject perm_addNodesToAllowlist = new JsonObject();
                        perm_addNodesToAllowlist.put("jsonrpc", "2.0");
                        perm_addNodesToAllowlist.put("method", "perm_addNodesToAllowlist");
                        perm_addNodesToAllowlist.put("params", new JsonArray().add(addToAllowList));
                        perm_addNodesToAllowlist.put("id", "updateAddNodesForward");
                        context.result().writeTextMessage(perm_addNodesToAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateAddNodesForward")) {
                        JsonObject perm_getNodesAllowlist = new JsonObject();
                        perm_getNodesAllowlist.put("jsonrpc", "2.0");
                        perm_getNodesAllowlist.put("method", "perm_getNodesAllowlist");
                        perm_getNodesAllowlist.put("params", new JsonArray().add(nodesAllowed));
                        perm_getNodesAllowlist.put("id", "updateRemoveNodesProcess");
                        context.result().writeTextMessage(perm_getNodesAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateRemoveNodesProcess")) {
                        LOGGER.debug("[UPDATE] Current nodes allow-list "
                                + response.getJsonArray("result").size());

                        JsonArray removeFromAllowList = new JsonArray();
                        for (Object obj : response.getJsonArray("result")) {
                            String node = (String) obj;
                            if (!nodesAllowed.contains(node) || nodesRevoked.contains(node)) {
                                removeFromAllowList.add(obj);
                            }
                        }
                        LOGGER.debug("[UPDATE] Remove from nodes allow-list " + removeFromAllowList.size());

                        JsonObject perm_removeNodesFromAllowlist = new JsonObject();
                        perm_removeNodesFromAllowlist.put("jsonrpc", "2.0");
                        perm_removeNodesFromAllowlist.put("method", "perm_removeNodesFromAllowlist");
                        perm_removeNodesFromAllowlist.put("params", new JsonArray().add(removeFromAllowList));
                        perm_removeNodesFromAllowlist.put("id", "updateRemoveNodesForward");
                        context.result().writeTextMessage(perm_removeNodesFromAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateRemoveNodesForward")) {
                        JsonObject perm_getAccountsAllowlist = new JsonObject();
                        perm_getAccountsAllowlist.put("jsonrpc", "2.0");
                        perm_getAccountsAllowlist.put("method", "perm_getAccountsAllowlist");
                        perm_getAccountsAllowlist.put("params", new JsonArray().add(nodesAllowed));
                        perm_getAccountsAllowlist.put("id", "updateAddAccountsProcess");
                        context.result().writeTextMessage(perm_getAccountsAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateAddAccountsProcess")) {
                        LOGGER.debug("[UPDATE] Current account allow-list "
                                + response.getJsonArray("result").size());

                        JsonArray addToAllowList = new JsonArray();
                        for (Object obj : accountsAllowed) {
                            if (!response.getJsonArray("result").contains(obj)) {
                                addToAllowList.add(obj);
                            }
                        }
                        LOGGER.debug("[UPDATE] Add to account allow-list "
                                + addToAllowList.size());

                        JsonObject perm_addAccountsToAllowlist = new JsonObject();
                        perm_addAccountsToAllowlist.put("jsonrpc", "2.0");
                        perm_addAccountsToAllowlist.put("method", "perm_addAccountsToAllowlist");
                        perm_addAccountsToAllowlist.put("params", new JsonArray().add(addToAllowList));
                        perm_addAccountsToAllowlist.put("id", "updateAddAccountsForward");
                        context.result().writeTextMessage(perm_addAccountsToAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateAddAccountsForward")) {
                        JsonObject perm_getAccountsAllowlist = new JsonObject();
                        perm_getAccountsAllowlist.put("jsonrpc", "2.0");
                        perm_getAccountsAllowlist.put("method", "perm_getAccountsAllowlist");
                        perm_getAccountsAllowlist.put("params", new JsonArray().add(nodesAllowed));
                        perm_getAccountsAllowlist.put("id", "updateRemoveAccountsProcess");
                        context.result().writeTextMessage(perm_getAccountsAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateRemoveAccountsProcess")) {
                        LOGGER.debug("[UPDATE] Current account allow-list "
                                + response.getJsonArray("result").size());

                        JsonArray removeFromAllowList = new JsonArray();
                        for (Object obj : response.getJsonArray("result")) {
                            String account = (String) obj;
                            if (!accountsAllowed.contains(account) || accountsRevoked.contains(account)) {
                                removeFromAllowList.add(account);
                            }
                        }
                        LOGGER.debug("[UPDATE] Remove from account allow-list "
                                + removeFromAllowList.size());

                        JsonObject perm_removeAccountsFromAllowlist = new JsonObject();
                        perm_removeAccountsFromAllowlist.put("jsonrpc", "2.0");
                        perm_removeAccountsFromAllowlist.put("method", "perm_removeAccountsFromAllowlist");
                        perm_removeAccountsFromAllowlist.put("params", new JsonArray().add(removeFromAllowList));
                        perm_removeAccountsFromAllowlist.put("id", "updateRemoveAccountsForward");
                        context.result().writeTextMessage(perm_removeAccountsFromAllowlist.toString());
                    }

                    if (response.getString("id").equals("updateRemoveAccountsForward")) {
                        LOGGER.debug("[UPDATE] END");
                        webSocketPromise.complete();
                    }
                });

                JsonObject perm_getNodesAllowlist = new JsonObject();
                perm_getNodesAllowlist.put("jsonrpc", "2.0");
                perm_getNodesAllowlist.put("method", "perm_getNodesAllowlist");
                perm_getNodesAllowlist.put("params", new JsonArray());
                perm_getNodesAllowlist.put("id", "updateAddNodesProcess");
                context.result().writeTextMessage(perm_getNodesAllowlist.toString());

                for (Object obj : accountsAllowed) {
                    String address = (String) obj;
                    JsonObject ibft_proposeValidatorVote = new JsonObject();
                    ibft_proposeValidatorVote.put("jsonrpc", "2.0");
                    ibft_proposeValidatorVote.put("method", "ibft_proposeValidatorVote");
                    ibft_proposeValidatorVote.put("params", new JsonArray().add(address).add(true));
                    ibft_proposeValidatorVote.put("id", "ibft_proposeValidatorVote: " + address);
                    context.result()
                            .writeTextMessage(ibft_proposeValidatorVote.toString())
                            //.textMessageHandler(System.out::println)
                            .exceptionHandler(Throwable::printStackTrace);
                }

                for (Object obj : accountsRevoked) {
                    String address = (String) obj;
                    JsonObject ibft_proposeValidatorVote = new JsonObject();
                    ibft_proposeValidatorVote.put("jsonrpc", "2.0");
                    ibft_proposeValidatorVote.put("method", "ibft_proposeValidatorVote");
                    ibft_proposeValidatorVote.put("params", new JsonArray().add(address).add(false));
                    ibft_proposeValidatorVote.put("id", "ibft_proposeValidatorVote: " + address);
                    context.result()
                            .writeTextMessage(ibft_proposeValidatorVote.toString())
                            //.textMessageHandler(System.out::println)
                            .exceptionHandler(Throwable::printStackTrace);
                }

                for (Object obj : nodesAllowed) {
                    String node = (String) obj;
                    JsonObject admin_addPeer = new JsonObject();
                    admin_addPeer.put("jsonrpc", "2.0");
                    admin_addPeer.put("method", "admin_addPeer");
                    admin_addPeer.put("params", new JsonArray().add(node));
                    admin_addPeer.put("id", "admin_addPeer: " + node);
                    context.result()
                            .writeTextMessage(admin_addPeer.toString())
                            //.textMessageHandler(System.out::println)
                            .exceptionHandler(Throwable::printStackTrace);
                }

                for (Object obj : nodesRevoked) {
                    String node = (String) obj;
                    JsonObject admin_removePeer = new JsonObject();
                    admin_removePeer.put("jsonrpc", "2.0");
                    admin_removePeer.put("method", "admin_removePeer");
                    admin_removePeer.put("params", new JsonArray().add(node));
                    admin_removePeer.put("id", "admin_removePeer: " + node);
                    context.result()
                            .writeTextMessage(admin_removePeer.toString())
                            //.textMessageHandler(System.out::println)
                            .exceptionHandler(Throwable::printStackTrace);
                }
            } else {
                LOGGER.error("[UPDATE] " + context.cause().getMessage());
            }
        });
    }

    @Override
    public void updateNode(Logger LOGGER, Collection<Certificate> certificates) {
        processWorldState(certificates, processWorldStateResult -> updateBesuNode(LOGGER));
    }
}
