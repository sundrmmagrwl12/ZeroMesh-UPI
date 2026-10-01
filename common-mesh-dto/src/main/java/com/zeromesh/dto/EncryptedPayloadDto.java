package com.zeromesh.dto;

public class EncryptedPayloadDto {

    private String encryptedAesKey;
    private String iv;
    private String ciphertext;

    public EncryptedPayloadDto() {}

    public EncryptedPayloadDto(String encryptedAesKey, String iv, String ciphertext) {
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.ciphertext = ciphertext;
    }

    public static EncryptedPayloadDtoBuilder builder() { return new EncryptedPayloadDtoBuilder(); }

    public String getEncryptedAesKey() { return encryptedAesKey; }
    public void setEncryptedAesKey(String v) { this.encryptedAesKey = v; }
    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String v) { this.ciphertext = v; }

    public static class EncryptedPayloadDtoBuilder {
        private String encryptedAesKey; private String iv; private String ciphertext;
        public EncryptedPayloadDtoBuilder encryptedAesKey(String v) { this.encryptedAesKey = v; return this; }
        public EncryptedPayloadDtoBuilder iv(String v) { this.iv = v; return this; }
        public EncryptedPayloadDtoBuilder ciphertext(String v) { this.ciphertext = v; return this; }
        public EncryptedPayloadDto build() { return new EncryptedPayloadDto(encryptedAesKey, iv, ciphertext); }
    }
}
