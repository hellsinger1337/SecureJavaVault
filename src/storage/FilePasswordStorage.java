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

public class FilePasswordStorage implements PasswordStorage {
    private final String filename;
    private EncryptionStrategy encryptionStrategy;
    private List<PasswordEntry> entries = new ArrayList<>();
    private boolean initialized = false;
    private byte[] salt; // Соль для PBKDF2

    public FilePasswordStorage(String filename) {
        this.filename = filename;
    }

    @Override
    public boolean loadOrInit(char[] masterPassword) {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            // Первый запуск - генерируем соль
            salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            encryptionStrategy = new PBKDF2AesGcmEncryptionStrategy(masterPassword, salt);
            // Создаём пустое хранилище + CHECK
            entries = new ArrayList<>();
            save();
            initialized = true;
            return true;
        } else {
            // Загрузка
            try (FileInputStream fis = new FileInputStream(file)) {
                // Читаем соль
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

    @Override
    public void save() {
        if (!initialized) return;
        try (FileOutputStream fos = new FileOutputStream(filename);
             DataOutputStream dos = new DataOutputStream(fos)) {
            // Пишем соль
            writeByteArray(dos, salt);
            byte[] data = serialize();
            byte[] encrypted = encryptionStrategy.encrypt(data);
            dos.write(encrypted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(PasswordEntry entry) {
        PasswordEntry existing = findBySourceAndLogin(entry.getSource(), entry.getLogin());
        if (existing != null) {
            entries.remove(existing);
        }
        entries.add(entry);
    }

    @Override
    public PasswordEntry findBySourceAndLogin(char[] source, char[] login) {
        for (PasswordEntry e : entries) {
            if (Arrays.equals(e.getSource(), source) && Arrays.equals(e.getLogin(), login)) {
                return e;
            }
        }
        return null;
    }

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

    @Override
    public List<PasswordEntry> getAll() {
        return entries;
    }

    private byte[] serialize() {
        // Структура: 
        // "CHECK"
        // int: количество записей
        // записи
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

    private void writeCharArray(DataOutputStream dos, char[] arr) throws IOException {
        dos.writeInt(arr.length);
        for (char c : arr) {
            dos.writeChar(c);
        }
    }

    private char[] readCharArray(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        char[] arr = new char[length];
        for (int i = 0; i < length; i++) {
            arr[i] = dis.readChar();
        }
        return arr;
    }

    private void writeByteArray(DataOutputStream dos, byte[] arr) throws IOException {
        dos.writeInt(arr.length);
        dos.write(arr);
    }

    private byte[] readByteArray(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] arr = new byte[length];
        int read = dis.read(arr);
        if (read != length) throw new IOException("Не удалось прочитать массив байт полностью");
        return arr;
    }
}