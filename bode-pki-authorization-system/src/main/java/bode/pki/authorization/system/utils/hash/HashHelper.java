package bode.pki.authorization.system.utils.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class HashHelper {

    public static String hashStringLastTwenty(MessageDigest digest, String content) {
        byte[] hash = digest.digest(hexToBytes(content));
        byte[] lastTwenty = Arrays.copyOfRange(hash, hash.length-20, hash.length);
        return bytesToHex(lastTwenty);
    }

    public static String hashString(MessageDigest digest, String content) {
        return bytesToHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] hexToBytes(String hex) {
        byte[] byteArray = new byte[hex.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hex.substring(index, index + 2), 16);
            byteArray[i] = (byte) j;
        }
        return byteArray;
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
