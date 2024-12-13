package model;

import java.util.Arrays;

public class PasswordEntry {
    private final char[] source;
    private final char[] login;
    private final char[] password;

    PasswordEntry(char[] source, char[] login, char[] password) {
        this.source = source;
        this.login = login;
        this.password = password;
    }

    public char[] getSource() {
        return source.clone();
    }

    public char[] getLogin() {
        return login.clone();
    }

    public char[] getPassword() {
        return password.clone();
    }

    public void clear() {
        Arrays.fill(source, '\0');
        Arrays.fill(login, '\0');
        Arrays.fill(password, '\0');
    }
}