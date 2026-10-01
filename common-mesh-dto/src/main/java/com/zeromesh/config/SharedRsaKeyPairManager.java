package com.zeromesh.config;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Manages the RSA-2048 key pair shared between Ingestion and Settlement services.
 *
 * On first startup: generates key pair and writes both keys to disk.
 * On subsequent starts: loads the same keys so both services stay in sync.
 *
 * Docker: both services mount the same volume at /app/zeromesh-keys/
 * Local:  keys stored in ./zeromesh-keys/ relative to working directory
 */
@Component
public class SharedRsaKeyPairManager {

    private static final String KEY_DIR          = "zeromesh-keys";
    private static final String PRIVATE_KEY_FILE = KEY_DIR + "/private.key";
    private static final String PUBLIC_KEY_FILE  = KEY_DIR + "/public.key";

    private KeyPair keyPair;

    @PostConstruct
    public void init() throws Exception {
        File privateKeyFile = new File(PRIVATE_KEY_FILE);
        File publicKeyFile  = new File(PUBLIC_KEY_FILE);

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            this.keyPair = loadKeyPair(privateKeyFile, publicKeyFile);
            System.out.println("[KeyManager] Loaded RSA-2048 key pair from: " + KEY_DIR);
        } else {
            this.keyPair = generateAndPersist();
            System.out.println("[KeyManager] Generated new RSA-2048 key pair → saved to: " + KEY_DIR);
        }
    }

    private KeyPair generateAndPersist() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        new File(KEY_DIR).mkdirs();
        Files.write(Paths.get(PRIVATE_KEY_FILE), Base64.getEncoder().encode(pair.getPrivate().getEncoded()));
        Files.write(Paths.get(PUBLIC_KEY_FILE),  Base64.getEncoder().encode(pair.getPublic().getEncoded()));

        return pair;
    }

    private KeyPair loadKeyPair(File privateKeyFile, File publicKeyFile) throws Exception {
        byte[] privateBytes = Base64.getDecoder().decode(Files.readAllBytes(privateKeyFile.toPath()));
        byte[] publicBytes  = Base64.getDecoder().decode(Files.readAllBytes(publicKeyFile.toPath()));

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
        PublicKey  publicKey  = kf.generatePublic(new X509EncodedKeySpec(publicBytes));

        return new KeyPair(publicKey, privateKey);
    }

    public KeyPair    getKeyPair()    { return keyPair; }
    public PublicKey  getPublicKey()  { return keyPair.getPublic(); }
    public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }
}
