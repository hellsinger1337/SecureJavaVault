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

/**
 * Implementation of EncryptionStrategy using PBKDF2 for key derivation
 * and AES-GCM for encryption and decryption.
 */
public class PBKDF2AesGcmEncryptionStrategy implements EncryptionStrategy {
    // Constants for encryption parameters
    private static final int AES_KEY_SIZE = 32; // 256 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag
    private static final int PBKDF2_ITERATIONS = 100000; // Number of PBKDF2 iterations
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256"; // PBKDF2 algorithm
    private static final String AES_ALGO = "AES/GCM/NoPadding"; // AES-GCM mode with no padding

    // Derived AES key
    private final byte[] aesKey;

    /**
     * Constructor that derives the AES key using PBKDF2 with the provided master password and salt.
     *
     * @param masterPassword The master password as a character array.
     * @param salt           The salt as a byte array.
     * @throws RuntimeException If key generation fails.
     */
    public PBKDF2AesGcmEncryptionStrategy(char[] masterPassword, byte[] salt) {
        try {
            // Define the key specification with the master password, salt, iterations, and key length
            KeySpec spec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE * 8);
            // Initialize the SecretKeyFactory with the PBKDF2 algorithm
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            // Generate the secret key and retrieve its encoded form
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.aesKey = keyBytes;

            // Clear the master password from memory for security
            Arrays.fill(masterPassword, '\0');
        } catch (Exception e) {
            throw new RuntimeException("Error generating PBKDF2 key", e);
        }
    }

    /**
     * Encrypts the provided data using AES-GCM.
     *
     * @param data The plaintext data to encrypt.
     * @return The encrypted data with the IV prepended.
     * @throws RuntimeException If encryption fails.
     */
    @Override
    public byte[] encrypt(byte[] data) {
        try {
            // Generate a 12-byte Initialization Vector (IV) using a secure random number generator
            byte[] iv = new byte[12];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(iv);

            // Initialize the Cipher for AES-GCM encryption
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // Perform the encryption
            byte[] encrypted = cipher.doFinal(data);

            // Prepend the IV to the encrypted data
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error during AES-GCM encryption", e);
        }
    }

    /**
     * Decrypts the provided data using AES-GCM.
     *
     * @param data The encrypted data with the IV prepended.
     * @return The decrypted plaintext data, or null if decryption fails.
     */
    @Override
    public byte[] decrypt(byte[] data) {
        try {
            // Ensure the data length is sufficient to contain the IV
            if (data.length < 12) return null;

            // Extract the IV from the beginning of the data
            byte[] iv = Arrays.copyOfRange(data, 0, 12);
            // Extract the encrypted portion of the data
            byte[] encrypted = Arrays.copyOfRange(data, 12, data.length);

            // Initialize the Cipher for AES-GCM decryption
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // Perform the decryption and return the plaintext
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            // Return null if decryption fails (e.g., authentication tag mismatch)
            return null;
        }
    }
}