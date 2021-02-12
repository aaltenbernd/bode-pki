package bode.pki.authorization.system.utils.hash;

import bode.pki.authorization.system.utils.certificate.Certificate;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public class HashBlock {

    private String hash;
    private String prevHash;
    private Certificate certificate;

    public HashBlock() {
        this.hash = null;
        this.prevHash = null;
        this.certificate = null;
    }

    public HashBlock(String hash, String prevHash, Certificate certificate) {
        this.hash = hash;
        this.prevHash = prevHash;
        this.certificate = certificate;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.put("hash", hash);
        result.put("prevHash", prevHash);
        result.put("certificate", certificate.toJson());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashBlock that = (HashBlock) o;
        return Objects.equals(hash, that.hash) &&
                Objects.equals(prevHash, that.prevHash) &&
                Objects.equals(certificate, that.certificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, prevHash, certificate);
    }
}
