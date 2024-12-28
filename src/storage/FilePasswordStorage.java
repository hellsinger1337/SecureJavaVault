package storage;

import model.PasswordEntry;
import model.PasswordEntryBuilder;
import strategy.EncryptionStrategy;
import strategy.PBKDF2AesGcmEncryptionStrategy;

import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of PasswordStorage that handles storing and retrieving password entries
 * from a file with encryption using PBKDF2 for key derivation and AES-GCM for encryption.
 */
public class FilePasswordStorage implements PasswordStorage {
    private final String filename;
    private EncryptionStrategy encryptionStrategy;
    private List<PasswordEntry> entries = new ArrayList<>();
    private boolean initialized = false;
    private byte[] salt; // Salt for PBKDF2

    /**
     * Constructs a FilePasswordStorage with the specified filename.
     *
     * @param filename The name of the file where password entries are stored.
     */
    public FilePasswordStorage(String filename) {
        this.filename = filename;
    }

    /**
     * Loads existing password entries from the file or initializes a new storage if the file does not exist.
     * Derives the encryption key using the provided master password and salt.
     *
     * @param masterPassword The master password used for encryption and decryption.
     * @return {@code true} if loading or initialization is successful; {@code false} otherwise.
     */
    @Override
    public boolean loadOrInit(char[] masterPassword) {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            // First run - generate salt
            salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            encryptionStrategy = new PBKDF2AesGcmEncryptionStrategy(masterPassword, salt);
            // Create empty storage + CHECK
            entries = new ArrayList<>();
            save();
            initialized = true;
            return true;
        } else {
            // Loading existing storage
            try (FileInputStream fis = new FileInputStream(file)) {
                // Read salt
                DataInputStream dis = new DataInputStream(fis);
                salt = readByteArray(dis);
                encryptionStrategy = new PBKDF2AesGcmEncryptionStrategy(masterPassword, salt);
                
                byte[] encryptedData = dis.readAllBytes();
                byte[] data = encryptionStrategy.decrypt(encryptedData);
                if (data == null) return false;
                if (!deserialize(data)) return false;
                initialized = true;
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Saves the current list of password entries to the file with encryption.
     * If the storage is not initialized, the method returns without performing any action.
     */
    @Override
    public void save() {
        if (!initialized) return;
        try (FileOutputStream fos = new FileOutputStream(filename);
             DataOutputStream dos = new DataOutputStream(fos)) {
            // Write salt
            writeByteArray(dos, salt);
            byte[] data = serialize();
            byte[] encrypted = encryptionStrategy.encrypt(data);
            dos.write(encrypted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new password entry to the storage.
     * If an entry with the same source and login already exists, it is replaced.
     *
     * @param entry The PasswordEntry to add.
     */
    @Override
    public void add(PasswordEntry entry) {
        PasswordEntry existing = findBySourceAndLogin(entry.getSource(), entry.getLogin());
        if (existing != null) {
            entries.remove(existing);
        }
        entries.add(entry);
    }

    /**
     * Finds a password entry by its source and login.
     *
     * @param source The source associated with the password entry.
     * @param login  The login associated with the password entry.
     * @return The matching PasswordEntry if found; {@code null} otherwise.
     */
    @Override
    public PasswordEntry findBySourceAndLogin(char[] source, char[] login) {
        for (PasswordEntry e : entries) {
            if (Arrays.equals(e.getSource(), source) && Arrays.equals(e.getLogin(), login)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Searches for password entries that contain the specified keyword in their source or login.
     *
     * @param keyword The keyword to search for.
     * @return A list of PasswordEntry objects that match the search criteria.
     */
    @Override
    public List<PasswordEntry> search(String keyword) {
        keyword = keyword.toLowerCase();
        List<PasswordEntry> result = new ArrayList<>();
        for (PasswordEntry e : entries) {
            String s = new String(e.getSource()).toLowerCase();
            String l = new String(e.getLogin()).toLowerCase();
            if (s.contains(keyword) || l.contains(keyword)) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Retrieves all password entries stored.
     *
     * @return A list of all PasswordEntry objects.
     */
    @Override
    public List<PasswordEntry> getAll() {
        return entries;
    }

    /**
     * Deletes a password entry identified by its source and login.
     *
     * @param source The source associated with the password entry to delete.
     * @param login  The login associated with the password entry to delete.
     */
    @Override
    public void delete(char[] source, char[] login) {
        PasswordEntry toDelete = null;
        for (PasswordEntry e : entries) {
            if (Arrays.equals(e.getSource(), source) && Arrays.equals(e.getLogin(), login)) {
                toDelete = e;
                break;
            }
        }
        if (toDelete != null) {
            entries.remove(toDelete);
        }
    }

    /**
     * Serializes the list of password entries into a byte array for encryption and storage.
     *
     * @return The serialized byte array representing the password entries.
     */
    private byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            writeCharArray(dos, "CHECK".toCharArray());
            dos.writeInt(entries.size());
            for (PasswordEntry e : entries) {
                writeCharArray(dos, e.getSource());
                writeCharArray(dos, e.getLogin());
                writeCharArray(dos, e.getPassword());
            }
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the byte array back into the list of password entries.
     *
     * @param data The serialized byte array representing the password entries.
     * @return {@code true} if deserialization is successful; {@code false} otherwise.
     */
    private boolean deserialize(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            char[] check = readCharArray(dis);
            if (!"CHECK".equals(new String(check))) {
                return false;
            }
            int size = dis.readInt();
            entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                char[] source = readCharArray(dis);
                char[] login = readCharArray(dis);
                char[] password = readCharArray(dis);
                entries.add(new PasswordEntryBuilder()
                        .setSource(source)
                        .setLogin(login)
                        .setPassword(password)
                        .build());
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Writes a character array to the DataOutputStream with its length.
     *
     * @param dos The DataOutputStream to write to.
     * @param arr The character array to write.
     * @throws IOException If an I/O error occurs.
     */
    private void writeCharArray(DataOutputStream dos, char[] arr) throws IOException {
        dos.writeInt(arr.length);
        for (char c : arr) {
            dos.writeChar(c);
        }
    }

    /**
     * Reads a character array from the DataInputStream.
     *
     * @param dis The DataInputStream to read from.
     * @return The read character array.
     * @throws IOException If an I/O error occurs.
     */
    private char[] readCharArray(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        char[] arr = new char[length];
        for (int i = 0; i < length; i++) {
            arr[i] = dis.readChar();
        }
        return arr;
    }

    /**
     * Writes a byte array to the DataOutputStream with its length.
     *
     * @param dos The DataOutputStream to write to.
     * @param arr The byte array to write.
     * @throws IOException If an I/O error occurs.
     */
    private void writeByteArray(DataOutputStream dos, byte[] arr) throws IOException {
        dos.writeInt(arr.length);
        dos.write(arr);
    }

    /**
     * Reads a byte array from the DataInputStream.
     *
     * @param dis The DataInputStream to read from.
     * @return The read byte array.
     * @throws IOException If an I/O error occurs or the array cannot be fully read.
     */
    private byte[] readByteArray(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] arr = new byte[length];
        int read = dis.read(arr);
        if (read != length) throw new IOException("Failed to read the byte array completely");
        return arr;
    }
}