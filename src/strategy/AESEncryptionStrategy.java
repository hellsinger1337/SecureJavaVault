package strategy;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class AESEncryptionStrategy implements EncryptionStrategy {
    private final SecretKeySpec secretKey;

    public AESEncryptionStrategy(char[] key) {
        // Формируем ключ 16 байт из мастер-пароля
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < key.length && i < 16; i++) {
            keyBytes[i] = (byte) key[i];
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        Arrays.fill(key, '\0');
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            return null;
        }
    }
}