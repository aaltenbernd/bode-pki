package bode.pki.authorization.system.utils.key;

import bode.pki.authorization.system.utils.hash.HashHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

public class PublicKeyReader {

    public static void getRSA(Vertx vertx, String path, Handler<AsyncResult<PublicKey>> handler) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            vertx.fileSystem().readFile(path, ar -> {
                if (ar.succeeded()) {
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(ar.result().getBytes());
                    try {
                        PublicKey publicKey = keyFactory.generatePublic(spec);
                        handler.handle(Future.succeededFuture(publicKey));
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                        handler.handle(Future.failedFuture(e.getMessage()));
                    }
                } else {
                    handler.handle(Future.failedFuture(ar.cause()));
                }
            });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            handler.handle(Future.failedFuture(e.getMessage()));
        }
    }

    public static void getECDSA(Vertx vertx, String path, Handler<AsyncResult<PublicKey>> handler) {
        vertx.fileSystem().readFile(path, ar -> {
            if (ar.succeeded()) {
                String result = String.valueOf(ar.result());
                getECDSA(result, getECDSAResult -> {
                    if (getECDSAResult.succeeded()) {
                        handler.handle(Future.succeededFuture(getECDSAResult.result()));
                    } else {
                        handler.handle(Future.failedFuture(getECDSAResult.cause()));
                    }
                });
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public static void getECDSA(String key, Handler<AsyncResult<PublicKey>> handler) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            byte[] keyInBytes = HashHelper.hexToBytes(key);

            BigInteger x = new BigInteger(1,
                    Arrays.copyOfRange(keyInBytes, 0, keyInBytes.length/2));
            BigInteger y = new BigInteger(1,
                    Arrays.copyOfRange(keyInBytes, keyInBytes.length/2, keyInBytes.length));

            ECPoint ecPoint = new ECPoint(x, y);

            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC", "BC");
            algorithmParameters.init(new ECGenParameterSpec("secp256k1"));
            ECParameterSpec ecParametersSpec = algorithmParameters.getParameterSpec(ECParameterSpec.class);
            ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(ecPoint, ecParametersSpec);

            BCECPublicKey bcecPublicKey = (BCECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);

            handler.handle(Future.succeededFuture(bcecPublicKey));
        } catch (NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidKeySpecException
                | InvalidParameterSpecException e) {
            e.printStackTrace();
            handler.handle(Future.failedFuture(e.getMessage()));
        }
    }
}
