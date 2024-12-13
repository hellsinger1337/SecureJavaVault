package facade;

import model.PasswordEntry;
import model.PasswordEntryBuilder;
import storage.PasswordStorage;
import storage.FilePasswordStorage;

import java.util.List;

public class PasswordManagerFacade {
    private final PasswordStorage storage;
    private final boolean unlocked;

    public PasswordManagerFacade(char[] masterPassword) {
        
        this.storage = new FilePasswordStorage("vault.dat");
        this.unlocked = storage.loadOrInit(masterPassword);
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void addEntry(char[] source, char[] login, char[] password) {
        if (!unlocked) return;
        PasswordEntry entry = new PasswordEntryBuilder()
                .setSource(source)
                .setLogin(login)
                .setPassword(password)
                .build();
        storage.add(entry);
        storage.save();
    }

    public char[] getPassword(char[] source, char[] login) {
        if (!unlocked) return null;
        PasswordEntry entry = storage.findBySourceAndLogin(source, login);
        if (entry != null) {
            return entry.getPassword();
        }
        return null;
    }

    public List<PasswordEntry> getAll() {
        if (!unlocked) return List.of();
        return storage.getAll();
    }

    public List<PasswordEntry> search(String keyword) {
        if (!unlocked) return List.of();
        return storage.search(keyword);
    }

    public void searchEntries(String keyword) {
        if (!unlocked) return;
        List<PasswordEntry> results = storage.search(keyword);
        if (results.isEmpty()) {
            System.out.println("Нет записей, соответствующих запросу.");
        } else {
            for (PasswordEntry e : results) {
                System.out.println("Источник: " + new String(e.getSource()) + ", Логин: " + new String(e.getLogin()));
            }
        }
    }

    public void close() {
        if (unlocked) {
            storage.save();
        }
    }
}