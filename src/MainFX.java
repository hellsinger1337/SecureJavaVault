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
        char[] masterPassword = promptPassword("Мастер-пароль", "Введите мастер-пароль:");
        if (masterPassword == null) {
            System.exit(0);
        }

        facade = new PasswordManagerFacade(masterPassword);
        Arrays.fill(masterPassword, '\0');

        if (!facade.isUnlocked()) {
            showAlert("Ошибка", "Неверный мастер-пароль");
            System.exit(0);
        }

        primaryStage.setTitle("Password Manager");
        tableView = createTableView();
        loadEntries(facade.getAll());

        HBox buttonsBox = createButtonsBox();

        VBox root = new VBox(10, tableView, buttonsBox);
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 800, 500));
        primaryStage.show();
    }

    private TableView<PasswordEntryWrapper> createTableView() {
        TableView<PasswordEntryWrapper> table = new TableView<>();
        table.getColumns().addAll(
            createColumn("Источник", "source", 250),
            createColumn("Логин", "login", 250)
        );
        return table;
    }

    private TableColumn<PasswordEntryWrapper, String> createColumn(String title, String property, double width) {
        TableColumn<PasswordEntryWrapper, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private HBox createButtonsBox() {
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            EntryResult result = showEntryDialog("Добавить запись", null);
            if (result != null) {
                facade.addEntry(result.source, result.login, result.password);
                loadEntries(facade.getAll());
            }
        });

        Button getBtn = new Button("Get");
        getBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись");
                return;
            }
            char[] pass = facade.getPassword(selected.source, selected.login);
            if (pass == null) {
                showAlert("Информация", "Пароль не найден");
            } else {
                showAlert("Пароль", "Пароль: " + new String(pass));
            }
        });

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись для редактирования");
                return;
            }
            EntryResult result = showEntryDialog("Редактировать запись", selected);
            if (result != null) {
                facade.editEntry(selected.source, selected.login, result.source, result.login, result.password);
                loadEntries(facade.getAll());
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись для удаления");
                return;
            }
            if (confirmAction("Удаление", "Вы уверены, что хотите удалить выбранную запись?")) {
                facade.deleteEntry(selected.source, selected.login);
                loadEntries(facade.getAll());
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск...");
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            loadEntries(keyword.isEmpty() ? facade.getAll() : facade.search(keyword));
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> {
            facade.close();
            ((Stage) exitBtn.getScene().getWindow()).close();
        });

        HBox box = new HBox(10, addBtn, getBtn, editBtn, deleteBtn, searchField, searchBtn, exitBtn);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void loadEntries(List<PasswordEntry> entries) {
        data = FXCollections.observableArrayList(entries.stream().map(PasswordEntryWrapper::new).toList());
        tableView.setItems(data);
    }

    private char[] promptPassword(String title, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(message);

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");
        okBtn.setDefaultButton(true);
        cancelBtn.setCancelButton(true);

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

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10, new Label(message), passwordField, btnBox);
        vbox.setPadding(new Insets(10));

        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
        return (char[]) dialog.getUserData();
    }

    private EntryResult showEntryDialog(String title, PasswordEntryWrapper entry) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        TextField sourceField = new TextField(entry != null ? new String(entry.source) : "");
        TextField loginField = new TextField(entry != null ? new String(entry.login) : "");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(entry == null ? "Пароль" : "Новый пароль");

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");
        okBtn.setDefaultButton(true);
        cancelBtn.setCancelButton(true);

        okBtn.setOnAction(e -> {
            char[] source = sourceField.getText().trim().toCharArray();
            char[] login = loginField.getText().trim().toCharArray();
            char[] password = passwordField.getText().toCharArray();
            if (source.length == 0 || login.length == 0 || password.length == 0) {
                showAlert("Ошибка", "Все поля должны быть заполнены!");
            } else {
                EntryResult result = new EntryResult(source, login, password);
                dialog.setUserData(result);
                dialog.close();
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.setUserData(null);
            dialog.close();
        });

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10,
                new Label("Источник:"), sourceField,
                new Label("Логин:"), loginField,
                new Label(entry == null ? "Пароль:" : "Новый пароль:"), passwordField,
                btnBox);
        vbox.setPadding(new Insets(10));

        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
        return (EntryResult) dialog.getUserData();
    }

    private boolean confirmAction(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static class EntryResult {
        char[] source;
        char[] login;
        char[] password;

        EntryResult(char[] source, char[] login, char[] password) {
            this.source = source;
            this.login = login;
            this.password = password;
        }
    }

    public static class PasswordEntryWrapper {
        private final char[] source;
        private final char[] login;

        public PasswordEntryWrapper(PasswordEntry entry) {
            this.source = entry.getSource();
            this.login = entry.getLogin();
        }

        public String getSource() {
            return new String(source);
        }

        public String getLogin() {
            return new String(login);
        }
    }
}