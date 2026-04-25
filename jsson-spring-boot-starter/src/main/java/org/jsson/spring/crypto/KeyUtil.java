package org.jsson.spring.crypto;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility for handling Java Certificates and Keystores (JKS / PKCS12)
 * for secure corporate operations.
 */
public class KeyUtil {

    /**
     * Loads a KeyStore from the classpath or a physical file.
     */
    public static KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type != null ? type : "PKCS12");
        InputStream is;

        if (path.startsWith("classpath:")) {
            String cpPath = path.substring(10);
            if (!cpPath.startsWith("/")) {
                cpPath = "/" + cpPath;
            }
            is = KeyUtil.class.getResourceAsStream(cpPath);
        } else {
            is = new FileInputStream(path);
        }

        if (is == null) {
            throw new FileNotFoundException("Keystore/Certificate not found at: " + path);
        }

        try (is) {
            keyStore.load(is, password != null ? password.toCharArray() : null);
        }
        return keyStore;
    }

    /**
     * Reads a file content from the classpath or a physical file.
     */
    public static String readFileContent(String path) throws Exception {
        InputStream is;
        if (path.startsWith("classpath:")) {
            String cpPath = path.substring(10);
            if (!cpPath.startsWith("/")) {
                cpPath = "/" + cpPath;
            }
            is = KeyUtil.class.getResourceAsStream(cpPath);
        } else {
            is = new FileInputStream(path);
        }

        if (is == null) {
            throw new FileNotFoundException("File not found at: " + path);
        }

        try (is) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Extracts the Private Key associated with the Certificate to perform local signatures.
     */
    public static PrivateKey getPrivateKey(KeyStore keyStore, String alias, String keyPassword) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keyPassword != null ? keyPassword.toCharArray() : null);
    }

    /**
     * Extracts the Public Key associated with the Certificate for end-to-end verification.
     */
    public static PublicKey getPublicKey(KeyStore keyStore, String alias) throws Exception {
        Certificate cert = keyStore.getCertificate(alias);
        return cert != null ? cert.getPublicKey() : null;
    }

    /**
     * Returns the actual Certificate (X.509) from the storage, which can be embedded in the payload.
     */
    public static Certificate getCertificate(KeyStore keyStore, String alias) throws Exception {
        return keyStore.getCertificate(alias);
    }

    /**
     * Loads a Private Key from raw PEM text (OpenSSL) or Base64 content.
     */
    public static PrivateKey loadPrivateKeyFromPemOrBase64(String keyContent) throws Exception {
        String base64 = cleanPem(keyContent);
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePrivate(spec);
    }

    /**
     * Loads a Public Key from raw PEM text (OpenSSL) or Base64 content.
     */
    public static PublicKey loadPublicKeyFromPemOrBase64(String keyContent) throws Exception {
        String base64 = cleanPem(keyContent);
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        return kf.generatePublic(spec);
    }

    private static String cleanPem(String keyContent) {
        return keyContent
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");
    }
}
