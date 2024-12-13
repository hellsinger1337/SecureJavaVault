package model;

public class PasswordEntryBuilder {
    private char[] source;
    private char[] login;
    private char[] password;

    public PasswordEntryBuilder setSource(char[] source) {
        this.source = source;
        return this;
    }

    public PasswordEntryBuilder setLogin(char[] login) {
        this.login = login;
        return this;
    }

    public PasswordEntryBuilder setPassword(char[] password) {
        this.password = password;
        return this;
    }

    public PasswordEntry build() {
        return new PasswordEntry(source, login, password);
    }
}