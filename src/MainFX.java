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
        char[] masterPassword = showMasterPasswordDialog();
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

        
        tableView = new TableView<>();
        TableColumn<PasswordEntryWrapper, String> sourceCol = new TableColumn<>("Источник");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        sourceCol.setPrefWidth(250);

        TableColumn<PasswordEntryWrapper, String> loginCol = new TableColumn<>("Логин");
        loginCol.setCellValueFactory(new PropertyValueFactory<>("login"));
        loginCol.setPrefWidth(250);

        tableView.getColumns().addAll(sourceCol, loginCol);

        loadAllEntries();

        
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            AddEntryResult result = showAddEntryDialog();
            if (result != null) {
                facade.addEntry(result.source.clone(), result.login.clone(), result.password.clone());
                
                Arrays.fill(result.source, '\0');
                Arrays.fill(result.login, '\0');
                Arrays.fill(result.password, '\0');
                loadAllEntries();
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
                Arrays.fill(pass, '\0');
            }
        });

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись для редактирования");
                return;
            }
            EditEntryResult result = showEditEntryDialog(selected);
            if (result != null) {
                facade.editEntry(
                        selected.source,
                        selected.login,
                        result.newSource.clone(),
                        result.newLogin.clone(),
                        result.newPassword.clone()
                );
                
                Arrays.fill(result.newSource, '\0');
                Arrays.fill(result.newLogin, '\0');
                Arrays.fill(result.newPassword, '\0');
                loadAllEntries();
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> {
            PasswordEntryWrapper selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Информация", "Не выбрана запись для удаления");
                return;
            }
            boolean confirmed = showConfirmationDialog("Удаление", "Вы уверены, что хотите удалить выбранную запись?");
            if (confirmed) {
                facade.deleteEntry(selected.source, selected.login);
                loadAllEntries();
            }
        });

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

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> {
            facade.close();
            primaryStage.close();
        });

        HBox buttonsBox = new HBox(10, addBtn, getBtn, editBtn, deleteBtn, searchField, searchBtn, exitBtn);
        buttonsBox.setPadding(new Insets(10));
        buttonsBox.setAlignment(Pos.CENTER_LEFT);


        VBox root = new VBox(10, tableView, buttonsBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 500); 
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
            char[] source = sourceField.getText().trim().toCharArray();
            char[] login = loginField.getText().trim().toCharArray();
            char[] password = passwordField.getText().toCharArray();
            if (source.length == 0 || login.length == 0 || password.length == 0) {
                showAlert("Ошибка", "Все поля должны быть заполнены!");
            } else {
                AddEntryResult result = new AddEntryResult();
                result.source = source;
                result.login = login;
                result.password = password;
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

    private EditEntryResult showEditEntryDialog(PasswordEntryWrapper entry) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Редактировать запись");

        TextField sourceField = new TextField(new String(entry.source));
        TextField loginField = new TextField(new String(entry.login));
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Новый пароль");

        Button okBtn = new Button("OK");
        Button cancelBtn = new Button("Cancel");

        HBox btnBox = new HBox(10, okBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        VBox vbox = new VBox(10,
                new Label("Источник:"), sourceField,
                new Label("Логин:"), loginField,
                new Label("Новый пароль:"), passwordField,
                btnBox);
        vbox.setPadding(new Insets(10));

        okBtn.setOnAction(e -> {
            char[] newSource = sourceField.getText().trim().toCharArray();
            char[] newLogin = loginField.getText().trim().toCharArray();
            char[] newPassword = passwordField.getText().toCharArray();
            if (newSource.length == 0 || newLogin.length == 0 || newPassword.length == 0) {
                showAlert("Ошибка", "Все поля должны быть заполнены!");
            } else {
                EditEntryResult result = new EditEntryResult();
                result.newSource = newSource;
                result.newLogin = newLogin;
                result.newPassword = newPassword;
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
        return (EditEntryResult) dialog.getUserData();
    }

    private boolean showConfirmationDialog(String title, String message) {
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

    private static class AddEntryResult {
        char[] source;
        char[] login;
        char[] password;
    }

    private static class EditEntryResult {
        char[] newSource;
        char[] newLogin;
        char[] newPassword;
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