package strategy;

public interface EncryptionStrategy {
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] data);
}