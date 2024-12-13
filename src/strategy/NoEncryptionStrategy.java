package strategy;

public class NoEncryptionStrategy implements EncryptionStrategy {
    @Override
    public byte[] encrypt(byte[] data) {
        return data;
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return data;
    }
}