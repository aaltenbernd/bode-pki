package bode.pki.authorization.system.utils.certificate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificate {

    private String uuid;
    private String key;
    private String host;
    private Integer nodePort;
    private Integer asPort;
    private boolean revoked;

    public Certificate(Certificate certificate) {
        this.uuid = certificate.getUuid();
        this.key = certificate.getKey();
        this.host = certificate.getHost();
        this.nodePort = certificate.getNodePort();
        this.asPort = certificate.getAsPort();
        this.revoked = certificate.isRevoked();
    }

    public Certificate(String uuid) {
        this.uuid = uuid;
        this.key = null;
        this.host = null;
        this.nodePort = null;
        this.asPort = null;
        this.revoked = false;
    }

    public Certificate() {
        this.uuid = UUID.randomUUID().toString();
        this.key = null;
        this.host = null;
        this.nodePort = null;
        this.asPort = null;
        this.revoked = false;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

    public Integer getAsPort() {
        return asPort;
    }

    public void setAsPort(Integer asPort) {
        this.asPort = asPort;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return revoked == that.revoked &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(key, that.key) &&
                Objects.equals(host, that.host) &&
                Objects.equals(nodePort, that.nodePort) &&
                Objects.equals(asPort, that.asPort);
    }

    public boolean equalsBare(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificate that = (Certificate) o;
        return Objects.equals(key, that.key) &&
                Objects.equals(host, that.host) &&
                Objects.equals(nodePort, that.nodePort) &&
                Objects.equals(asPort, that.asPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, key, host, nodePort, asPort, revoked);
    }

    public String toString() {
        return toJson().toString();
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.put("uuid", uuid);
        if (key != null) result.put("key", key);
        if (host != null) result.put("host", host);
        if (nodePort != null) result.put("nodePort", nodePort);
        if (asPort != null) result.put("asPort", asPort);
        result.put("revoked", revoked);
        return result;
    }

    public String getAuthorizationSystemEndpoint() {
        if (host.equals("localhost") || host.equals("0.0.0.0") || host.equals("127.0.0.1")) {
            return "http://127.0.0.1:" + asPort;
        } else {
            // authorization system must be reachable under authorization-system.domain
            return "http://authorization-system." + host + ":" + asPort;
        }
    }

    public String getNodeHost() {
        if (host.equals("localhost") || host.equals("0.0.0.0") || host.equals("127.0.0.1")) {
            return host;
        } else {
            // node must be reachable under node.domain
            return "node." + host;
        }
    }
}
