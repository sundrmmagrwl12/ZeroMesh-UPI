package com.zeromesh.service.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeromesh.dto.EncryptedPayloadDto;
import com.zeromesh.dto.PaymentInstruction;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

// Hybrid encryption: RSA-2048 wraps a per-payment AES-256-GCM key
@Service
public class CryptoService {

    private static final String RSA_ALGO     = "RSA";
    private static final String RSA_TRANSFORM = "RSA/ECB/PKCS1Padding";
    private static final String AES_ALGO     = "AES";
    private static final String AES_TRANSFORM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE    = 256;
    private static final int GCM_IV_LENGTH   = 12;  // 12 bytes recommended for GCM
    private static final int GCM_TAG_LENGTH  = 128; // 128-bit auth tag

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Generates a 2048-bit RSA key pair (used only if no existing keys on disk)
    public KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(RSA_ALGO);
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * Encrypts a PaymentInstruction using hybrid cryptography:
     * 1. Generate random AES-256 key
     * 2. Encrypt PaymentInstruction JSON with AES-GCM
     * 3. Encrypt the AES key with RSA public key
     */
    public EncryptedPayloadDto encrypt(PaymentInstruction instruction, PublicKey rsaPublicKey) throws Exception {
        String json = objectMapper.writeValueAsString(instruction);

        KeyGenerator kg = KeyGenerator.getInstance(AES_ALGO);
        kg.init(AES_KEY_SIZE);
        SecretKey aesKey = kg.generateKey();

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORM);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertextBytes = aesCipher.doFinal(json.getBytes());

        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORM);
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());

        return EncryptedPayloadDto.builder()
                .encryptedAesKey(Base64.getEncoder().encodeToString(encryptedAesKeyBytes))
                .iv(Base64.getEncoder().encodeToString(iv))
                .ciphertext(Base64.getEncoder().encodeToString(ciphertextBytes))
                .build();
    }

    // Decrypts RSA-wrapped AES key, then AES-GCM decrypts the payload back to PaymentInstruction
    public PaymentInstruction decrypt(EncryptedPayloadDto dto, PrivateKey rsaPrivateKey) throws Exception {
        byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(dto.getEncryptedAesKey());
        byte[] ivBytes              = Base64.getDecoder().decode(dto.getIv());
        byte[] ciphertextBytes      = Base64.getDecoder().decode(dto.getCiphertext());

        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORM);
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        SecretKey aesKey = new SecretKeySpec(rsaCipher.doFinal(encryptedAesKeyBytes), AES_ALGO);

        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORM);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes));
        byte[] decrypted = aesCipher.doFinal(ciphertextBytes);

        return objectMapper.readValue(new String(decrypted), PaymentInstruction.class);
    }
}
