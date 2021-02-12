package bode.pki.authorization.system.utils.hash;

import bode.pki.authorization.system.utils.certificate.Certificate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HashChain {

    private String rootHash;
    private List<HashBlock> blocks;

    public HashChain() {
        this.rootHash = "";
        this.blocks = new ArrayList<>();
    }

    public void add(Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String prevHash = rootHash;
            String certHash = HashHelper.hashString(digest, String.valueOf(certificate.hashCode()));
            String hash = HashHelper.hashString(digest, prevHash.concat(certHash));
            HashBlock hashBlock = new HashBlock(hash, prevHash, certificate);
            blocks.add(hashBlock);
            rootHash = hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public boolean revoke(String uuid) {
        for (HashBlock hashBlock : Lists.reverse(blocks)) {
            Certificate current = hashBlock.getCertificate();
            if (hashBlock.getCertificate().getUuid().equals(uuid)) {
                Certificate update = new Certificate(current);
                update.setRevoked(!update.isRevoked());
                add(update);
                return true;
            }
        }
        return false;
    }

    public boolean verify() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            HashBlock prev = blocks.get(0);
            for (HashBlock curr : blocks.subList(1, blocks.size())) {
                if (!curr.getPrevHash().equals(prev.getHash())) {
                    return false;
                }

                String prevHash = curr.getPrevHash();
                String certHash = HashHelper.hashString(digest, String.valueOf(curr.getCertificate().hashCode()));
                String currHash = HashHelper.hashString(digest, prevHash.concat(certHash));

                if (!curr.getHash().equals(currHash)) {
                    return false;
                }

                prev = curr;
            }
            return prev.getHash().equals(rootHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean consistencyCheck(HashChain chain) {
        return this.getRootHash().equals(chain.getBlocks().get(0).getPrevHash());
    }

    public void merge(HashChain chain) {
        this.getBlocks().addAll(0, chain.getBlocks());
    }

    public List<HashBlock> getBlocks() {
        return blocks;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public void setBlocks(List<HashBlock> blocks) {
        this.blocks = blocks;
    }

    public String getRootHash() {
        return rootHash;
    }

    public JsonObject toJson() {
        JsonArray certificateBlocks = new JsonArray();
        for (HashBlock hashBlock : this.blocks) {
            certificateBlocks.add(hashBlock.toJson());
        }
        JsonObject result = new JsonObject();
        result.put("rootHash", rootHash);
        result.put("blocks", certificateBlocks);
        return result;
    }

    public JsonObject recentToJson(String hash) {
        List<JsonObject> subChain = new ArrayList<>();
        boolean found = false;
        for (HashBlock hashBlock : Lists.reverse(this.blocks)) {
            subChain.add(0, hashBlock.toJson());
            if (hashBlock.getPrevHash().equals(hash)) {
                found = true;
                break;
            }
        }
        if (found) {
            JsonObject result = new JsonObject();
            result.put("rootHash", rootHash);
            result.put("blocks", new JsonArray(subChain));
            return result;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return toJson().encodePrettily();
    }
}
