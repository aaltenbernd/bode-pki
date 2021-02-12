package bode.pki.authorization.system.utils.hash;

import bode.pki.authorization.system.utils.certificate.Certificate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HashList {

    private List<Certificate> certificates;
    private List<String> hashes;
    private String rootHash;
    private int version;

    public HashList() {
        this.certificates = new ArrayList<>();
        this.hashes = new ArrayList<>();
        this.rootHash = "";
        this.version = 0;
    }

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public void setHashes(List<String> hashes) {
        this.hashes = hashes;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void addCertificate(Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String newHash = HashHelper.hashString(digest, String.valueOf(certificate.hashCode()));
            if (!hashes.contains(newHash)) {
                hashes.add(newHash);
                certificates.add(certificate);
                version++;
                calcRootHash(digest);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void revoke(String hash) {
        try {
            int indexOfHash = hashes.indexOf(hash);
            if (indexOfHash >= 0) {
                Certificate certificate = certificates.get(indexOfHash);
                if (certificate != null) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    //certificate.incrementRevoked();
                    String newHash = HashHelper.hashString(digest, String.valueOf(certificate.hashCode()));
                    hashes.remove(indexOfHash);
                    hashes.add(indexOfHash, newHash);
                    version++;
                    calcRootHash(digest);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void calcRootHash(MessageDigest digest) {
        String versionHash = HashHelper.hashString(digest, String.valueOf(version));
        String concat = "";
        for (String currentHash : hashes) {
            concat = concat.concat(currentHash);
        }
        concat = concat.concat(versionHash);
        rootHash = HashHelper.hashString(digest, concat);
    }

    public boolean verify() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int versionToVerify = 0;
            String concat = "";
            Iterator<String> it = hashes.iterator();
            for (Certificate certificate : certificates) {
                //versionToVerify += certificate.getRevoked()+1;
                String hashToVerify = HashHelper.hashString(digest, String.valueOf(certificate.hashCode()));
                if (!hashToVerify.equals(it.next())) {
                    return false;
                }
                concat = concat.concat(hashToVerify);
            }
            String versionHash = HashHelper.hashString(digest, String.valueOf(versionToVerify));
            concat = concat.concat(versionHash);
            String rootHashToVerify = HashHelper.hashString(digest, concat);
            return rootHash.equals(rootHashToVerify) && version == versionToVerify;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean consistencyCheck(HashList hashList) {
        return hashList.getCertificates().size() >= certificates.size()
                && certificates.equals(hashList.getCertificates().subList(0, certificates.size()));
    }

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public List<String> getHashes() {
        return hashes;
    }

    public String getRootHash() {
        return rootHash;
    }

    public int getVersion() {
        return version;
    }

    public JsonObject toJson() {
        JsonArray certificatesJson = new JsonArray();
        for (Certificate certificate : certificates) {
            certificatesJson.add(certificate.toJson());
        }
        JsonObject result = new JsonObject();
        result.put("rootHash", rootHash);
        result.put("version", version);
        result.put("certificates", certificatesJson);
        result.put("hashes", hashes);
        return result;
    }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}
