import facade.PasswordManagerFacade;
import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            char[] masterPassword = promptMasterPassword(scanner);
            PasswordManagerFacade facade = new PasswordManagerFacade(masterPassword);
            Arrays.fill(masterPassword, '\0');
    
            if (!facade.isUnlocked()) {
                System.out.println("Неверный мастер-пароль. Выход...");
                return;
            }
    
            System.out.println("Простой менеджер паролей. Команды: add, get, search, exit");
            
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
    
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();
    
                switch (cmd) {
                    case "exit":
                        facade.close();
                        return;
                    case "add":
                        if (parts.length < 3) {
                            System.out.println("Формат: add <source> <login>");
                            continue;
                        }
                        char[] password = readPassword("Введите пароль для " + parts[1] + "/" + parts[2] + ": ", scanner);
                        facade.addEntry(parts[1].toCharArray(), parts[2].toCharArray(), password);
                        Arrays.fill(password, '\0');
                        System.out.println("Добавлено.");
                        break;
                    case "get":
                        if (parts.length < 3) {
                            System.out.println("Формат: get <source> <login>");
                            continue;
                        }
                        char[] pass = facade.getPassword(parts[1].toCharArray(), parts[2].toCharArray());
                        if (pass == null) {
                            System.out.println("Запись не найдена.");
                        } else {
                            System.out.println("Пароль: " + new String(pass));
                            Arrays.fill(pass, '\0');
                        }
                        break;
                    case "search":
                        if (parts.length < 2) {
                            System.out.println("Формат: search <keyword>");
                            continue;
                        }
                        facade.searchEntries(parts[1]);
                        break;
                    default:
                        System.out.println("Неизвестная команда.");
                }
            }
        }
    }
    
    private static char[] promptMasterPassword(Scanner scanner) {
        System.out.println("Введите мастер-пароль:");

        return scanner.nextLine().toCharArray();

    }

    private static char[] readPassword(String prompt, Scanner scanner) {
        System.out.print(prompt);
        return scanner.nextLine().toCharArray();
    } 
}