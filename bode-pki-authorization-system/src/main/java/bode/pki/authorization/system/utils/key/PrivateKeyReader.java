package bode.pki.authorization.system.utils.key;

import bode.pki.authorization.system.utils.hash.HashHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

public class PrivateKeyReader {

    public static void getRSA(Vertx vertx, String path, Handler<AsyncResult<PrivateKey>> handler) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            vertx.fileSystem().readFile(path, ar -> {
                if (ar.succeeded()) {
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(ar.result().getBytes());
                    try {
                        PrivateKey privateKey = keyFactory.generatePrivate(spec);
                        handler.handle(Future.succeededFuture(privateKey));
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

    public static void getECDSA(Vertx vertx, String path, Handler<AsyncResult<PrivateKey>> handler) {
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

    public static void getECDSA(String key, Handler<AsyncResult<PrivateKey>> handler) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            byte[] keyInBytes = HashHelper.hexToBytes(key.replaceFirst("0x", ""));
            BigInteger keyValue = new BigInteger(1, Arrays.copyOfRange(keyInBytes, 0, keyInBytes.length));

            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC", "BC");
            algorithmParameters.init(new ECGenParameterSpec("secp256k1"));
            ECParameterSpec ecParametersSpec = algorithmParameters.getParameterSpec(ECParameterSpec.class);
            ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(keyValue, ecParametersSpec);

            BCECPrivateKey bcecPrivateKey = (BCECPrivateKey) keyFactory.generatePrivate(ecPrivateKeySpec);

            handler.handle(Future.succeededFuture(bcecPrivateKey));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | InvalidParameterSpecException e) {
            e.printStackTrace();
            handler.handle(Future.failedFuture(e.getMessage()));
        }
    }
}
