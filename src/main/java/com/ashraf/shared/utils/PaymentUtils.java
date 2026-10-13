package com.ashraf.shared.utils;
import org.apache.commons.codec.binary.Hex;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class PaymentUtils {
    public static boolean verifySignature(String payload, String expectedSignature, String secret) throws Exception {
        if (expectedSignature == null) return false;
        String actual = calculateRFC2104HMAC(payload, secret);
        return java.security.MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
    }
    private static String calculateRFC2104HMAC(String data, String secret) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encodeHex(rawHmac));
    }
}