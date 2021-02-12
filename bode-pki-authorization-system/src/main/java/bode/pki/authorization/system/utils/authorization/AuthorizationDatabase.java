package bode.pki.authorization.system.utils.authorization;

import bode.pki.authorization.system.utils.certificate.Certificate;
import bode.pki.authorization.system.utils.hash.HashBlock;
import bode.pki.authorization.system.utils.hash.HashChain;
import bode.pki.authorization.system.utils.signature.SignatureHelper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonObject;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationDatabase {

    private HashChain chain;
    private String signature;

    public AuthorizationDatabase() {
        this.chain = new HashChain();
        this.signature = "";
    }

    public HashChain getChain() {
        return chain;
    }

    public void setChain(HashChain hashChain) {
        this.chain = hashChain;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void addCertificate(Certificate certificate, PrivateKey authorizationDatabaseSignerPrivateKey) {
        chain.add(certificate);
        signature = SignatureHelper.signSHA256withECDSA(chain.getRootHash(), authorizationDatabaseSignerPrivateKey);
    }

    public void revokeCertificate(String uuid, PrivateKey authorizationDatabaseSignerPrivateKey) {
        boolean revoked = chain.revoke(uuid);
        if (revoked) {
            signature = SignatureHelper.signSHA256withECDSA(chain.getRootHash(), authorizationDatabaseSignerPrivateKey);
        }
    }

    public void renewSignature(PrivateKey authorizationDatabaseSignerPrivateKey) {
        signature = SignatureHelper.signSHA256withECDSA(chain.getRootHash(), authorizationDatabaseSignerPrivateKey);
    }

    public String getRootHash() {
        return chain.getRootHash();
    }

    public JsonObject signedRootHashToJson() {
        JsonObject result = new JsonObject();
        result.put("rootHash", chain.getRootHash());
        result.put("signature", signature);
        return result;
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.put("chain", chain.toJson());
        result.put("signature", signature);
        return result;
    }

    public JsonObject recentToJson(String hash) {
        JsonObject result = new JsonObject();
        JsonObject chainJson = chain.recentToJson(hash);
        if (chainJson == null) {
            return null;
        } else {
            result.put("chain", chainJson);
            result.put("signature", signature);
            return result;
        }
    }

    public boolean verify(PublicKey authorizationDatabaseSignerPublicKey) {
        return chain.verify() && SignatureHelper.verifySHA256withECDSA(
                this.chain.getRootHash(), this.signature, authorizationDatabaseSignerPublicKey);
    }

    public boolean consistencyCheck(AuthorizationDatabase authorizationDatabase) {
        return this.chain.consistencyCheck(authorizationDatabase.getChain());
    }

    public void merge(AuthorizationDatabase authorizationDatabase) {
        this.chain.merge(authorizationDatabase.getChain());
    }

    public List<HashBlock> getBlocks() { return this.chain.getBlocks(); }

    public int size() {
        return chain.getBlocks().size();
    }
}
