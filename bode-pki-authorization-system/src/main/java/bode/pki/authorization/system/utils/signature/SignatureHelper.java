package bode.pki.authorization.system.utils.signature;

import org.apache.sshd.common.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.*;

public class SignatureHelper {

    public static String signSHA256withRSA(String message, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.encodeBase64(sign.sign()), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySHA256withRSA(String message, String signature, PublicKey publicKey){
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(publicKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return sign.verify(Base64.decodeBase64(signature.getBytes(StandardCharsets.UTF_8)));
        } catch (SignatureException | InvalidKeyException e) {
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String signSHA256withECDSA(String message, PrivateKey privateKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withECDSA");
            sign.initSign(privateKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.encodeBase64(sign.sign()), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySHA256withECDSA(String message, String signature, PublicKey publicKey){
        try {
            Signature sign = Signature.getInstance("SHA256withECDSA");
            sign.initVerify(publicKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return sign.verify(Base64.decodeBase64(signature.getBytes(StandardCharsets.UTF_8)));
        } catch (SignatureException | InvalidKeyException e) {
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }
}
