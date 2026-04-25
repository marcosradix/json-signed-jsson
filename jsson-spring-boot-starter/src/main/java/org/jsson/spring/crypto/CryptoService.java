package org.jsson.spring.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Service responsible for the core cryptographic operations of JSSON.
 */
public class CryptoService {

    /**
     * Signs the canonical string using the Ed25519 elliptic curve algorithm.
     * @param canonicalData raw text ordered by JSSON-C
     * @param privateKey Issuer's Private Key
     * @return Signature in Base64Url format
     */
    public static String sign(String canonicalData, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(canonicalData.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = signature.sign();
        // Use Base64Url without padding, similar to JWT
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
    }

    /**
     * Checks if the Hash/Signature of the canonical text is valid against the Public Key.
     * @param canonicalData raw text reconstructed at the receiver
     * @param signatureBase64Url Signature extracted from the reserved $jsson header
     * @param publicKey Issuer's Public Key
     * @return status indicating legitimacy of the processed data (not tampered)
     */
    public static boolean verify(String canonicalData, String signatureBase64Url, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(canonicalData.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getUrlDecoder().decode(signatureBase64Url);
            return signature.verify(sigBytes);
        } catch (Exception e) {
            e.printStackTrace();
            // In case of corruption or key manipulated outside B64Url pattern
            return false;
        }
    }
}
