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
            System.out.println("No entries matching the query.");
        } else {
            for (PasswordEntry e : results) {
                System.out.println("Source: " + new String(e.getSource()) + ", Login: " + new String(e.getLogin()));
            }
        }
    }
    
    public void deleteEntry(char[] source, char[] login) {
        if (!unlocked) return;
        storage.delete(source, login);
        storage.save();
    }

    public void editEntry(char[] oldSource, char[] oldLogin, char[] newSource, char[] newLogin, char[] newPassword) {
        if (!unlocked) return;
        PasswordEntry oldEntry = storage.findBySourceAndLogin(oldSource, oldLogin);
        if (oldEntry != null) {
            storage.delete(oldSource, oldLogin);
            PasswordEntry newEntry = new PasswordEntryBuilder()
                    .setSource(newSource)
                    .setLogin(newLogin)
                    .setPassword(newPassword)
                    .build();
            storage.add(newEntry);
            storage.save();
        }
    }

    public void close() {
        if (unlocked) {
            storage.save();
        }
    }
}