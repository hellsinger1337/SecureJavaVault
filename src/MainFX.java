import facade.PasswordManagerFacade;
import model.PasswordEntry;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.List;

public class MainFX extends Application {
    private PasswordManagerFacade facade;
    private TableView<PasswordEntryWrapper> tableView;
    private ObservableList<PasswordEntryWrapper> data;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Покажем диалог для ввода мастер-пароля
        char[] masterPassword = showMasterPasswordDialog();
        if (masterPassword == null) {
            // Пользователь отменил ввод
            System.exit(0);
        }

        facade = new PasswordManagerFacade(masterPassword);
        Arrays.fill(masterPassword, '\0');

        if (!facade.isUnlocked()) {
            showAlert("Ошибка", "Неверный мастер-пароль");
            System.exit(0);
        }

        primaryStage.setTitle("Password Manager");

        // Создадим таблицу для отображения записей
        tableView = new TableView<>();
        TableColumn<PasswordEntryWrapper, String> sourceCol = new TableColumn<>("Источник");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));

        TableColumn<PasswordEntryWrapper, String> loginCol = new TableColumn<>("Логин");
        loginCol.setCellValueFactory(new PropertyValueFactory<>("login"));

        tableView.getColumns().addAll(sourceCol, loginCol);

        loadAllEntries();

        // Кнопка Add
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            AddEntryResult result = showAddEntryDialog();
            if (result != null) {
                facade.addEntry(result.source, result.login, result.password);
                Arrays.fill(result.source, '\0');
                Arrays.fill(result.login, '\0');
                Arrays.fill(result.password, '\0');
                loadAllEntries();
            }
        });

        // Кнопка Get
        Button getBtn = new Button("Get");
        getBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись");
                return;
            }
            char[] pass = facade.getPassword(selected.source.toCharArray(), selected.login.toCharArray());
            if (pass == null) {
                showAlert("Информация", "Пароль не найден (возможно ошибка)");
            } else {
                showAlert("Пароль", "Пароль: " + new String(pass));
                Arrays.fill(pass, '\0');
            }
        });

        // Поле для поиска
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск...");
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            if (keyword.isEmpty()) {
                loadAllEntries();
            } else {
                loadSearchEntries(keyword);
            }
        });

        // Кнопка выхода
        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> {
            facade.close();
            primaryStage.close();
        });

        HBox buttonsBox = new HBox(10, addBtn, getBtn, searchField, searchBtn, exitBtn);
        buttonsBox.setPadding(new Insets(10));
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, tableView, buttonsBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadAllEntries() {
        List<PasswordEntry> entries = facade.getAll();
        data = FXCollections.observableArrayList();
        for (PasswordEntry e : entries) {
            data.add(new PasswordEntryWrapper(e));
        }
        tableView.setItems(data);
    }

    private void loadSearchEntries(String keyword) {
        List<PasswordEntry> entries = facade.search(keyword);
        data = FXCollections.observableArrayList();
        for (PasswordEntry e : entries) {
            data.add(new PasswordEntryWrapper(e));
        }
        tableView.setItems(data);
    }

    private char[] showMasterPasswordDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Мастер-пароль");

        Label label = new Label("Введите мастер-пароль:");
        PasswordField passwordField = new PasswordField();
        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10, label, passwordField, btnBox);
        vbox.setPadding(new Insets(10));

        okBtn.setOnAction(e -> {
            if (!passwordField.getText().isEmpty()) {
                dialog.setUserData(passwordField.getText().toCharArray());
                dialog.close();
            } else {
                showAlert("Ошибка", "Пароль не может быть пустым");
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.setUserData(null);
            dialog.close();
        });

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);
        dialog.showAndWait();
        return (char[]) dialog.getUserData();
    }

    private AddEntryResult showAddEntryDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Добавить запись");

        TextField sourceField = new TextField();
        sourceField.setPromptText("Источник");
        TextField loginField = new TextField();
        loginField.setPromptText("Логин");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10,
                new Label("Источник:"), sourceField,
                new Label("Логин:"), loginField,
                new Label("Пароль:"), passwordField,
                btnBox);
        vbox.setPadding(new Insets(10));

        okBtn.setOnAction(e -> {
            String s = sourceField.getText().trim();
            String l = loginField.getText().trim();
            String p = passwordField.getText();
            if (s.isEmpty() || l.isEmpty() || p.isEmpty()) {
                showAlert("Ошибка", "Все поля должны быть заполнены!");
            } else {
                AddEntryResult result = new AddEntryResult();
                result.source = s.toCharArray();
                result.login = l.toCharArray();
                result.password = p.toCharArray();
                dialog.setUserData(result);
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.setUserData(null);
            dialog.close();
        });

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);
        dialog.showAndWait();
        return (AddEntryResult) dialog.getUserData();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Вспомогательный класс для хранения введённых данных при добавлении записи
    private static class AddEntryResult {
        char[] source;
        char[] login;
        char[] password;
    }

    // Обёртка для PasswordEntry, чтобы удобно отобразить в TableView
    public static class PasswordEntryWrapper {
        private final String source;
        private final String login;

        public PasswordEntryWrapper(PasswordEntry entry) {
            this.source = new String(entry.getSource());
            this.login = new String(entry.getLogin());
        }

        public String getSource() {
            return source;
        }

        public String getLogin() {
            return login;
        }
    }
}