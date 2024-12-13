package strategy;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class PBKDF2AesGcmEncryptionStrategy implements EncryptionStrategy {
    private static final int AES_KEY_SIZE = 32; 
    private static final int GCM_TAG_LENGTH = 128; 
    private static final int PBKDF2_ITERATIONS = 100000; 
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGO = "AES/GCM/NoPadding";

    private final byte[] aesKey;

    public PBKDF2AesGcmEncryptionStrategy(char[] masterPassword, byte[] salt) {
        
        try {
            KeySpec spec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.aesKey = keyBytes;
            
            Arrays.fill(masterPassword, '\0');
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации ключа PBKDF2", e);
        }
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            
            byte[] iv = new byte[12];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(data);

            
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования AES-GCM", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        try {
            if (data.length < 12) return null;
            byte[] iv = Arrays.copyOfRange(data, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            return null;
        }
    }
}
