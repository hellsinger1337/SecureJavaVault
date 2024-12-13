package storage;

import model.PasswordEntry;

import java.util.List;

public interface PasswordStorage {
    boolean loadOrInit(char[] masterPassword);
    void save();
    void add(PasswordEntry entry);
    PasswordEntry findBySourceAndLogin(char[] source, char[] login);
    List<PasswordEntry> search(String keyword);
    List<PasswordEntry> getAll();
    void delete(char[] source, char[] login);
}