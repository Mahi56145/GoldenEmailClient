import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.Objects;

public class EmailGUI extends Application {

    private static String userEmail;
    private static String userPassword;
    private static String currentTheme = "dark";
    private List<File> selectedFiles;

    // --- ENGINES ---
    private EmailSender appSender;
    private EmailReceiver appReceiver;

    private static final String CONFIG_FILE = "config.properties";
    private Scene mainScene;

    @Override
    public void start(Stage stage) {
        loadConfig();

        StackPane root = new StackPane();
        mainScene = new Scene(root, 800, 600);

        applyTheme();

        try {
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {}

        stage.setScene(mainScene);
        stage.setTitle("Golden Email Client");
        stage.setMaximized(true);
        showLoginScreen(stage);
        stage.show();
    }

    private void switchScreen(Parent newRoot) {
        mainScene.setRoot(newRoot);
    }

    private void applyTheme() {
        mainScene.getStylesheets().clear();
        String cssPath = currentTheme.equals("light") ? "/light.css" : "/dark.css";
        try {
            if (getClass().getResource(cssPath) == null) return;
            String css = Objects.requireNonNull(getClass().getResource(cssPath)).toExternalForm();
            mainScene.getStylesheets().add(css);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Button createThemeButton() {
        String icon = currentTheme.equals("light") ? "🌙" : "☀️";
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: " + (currentTheme.equals("light") ? "#333" : "#ffd700") + ";");
        btn.setOnAction(e -> {
            currentTheme = currentTheme.equals("light") ? "dark" : "light";
            btn.setText(currentTheme.equals("light") ? "🌙" : "☀️");
            btn.setStyle("-fx-background-color: transparent; -fx-font-size: 24px; -fx-cursor: hand; -fx-text-fill: " + (currentTheme.equals("light") ? "#333" : "#ffd700") + ";");
            applyTheme();
            saveConfig();
        });
        return btn;
    }

    private ImageView createLogo() {
        ImageView iconView = new ImageView();
        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/logo.png")));
            iconView.setImage(icon);
            iconView.setFitWidth(80); iconView.setFitHeight(80); iconView.setPreserveRatio(true);
        } catch (Exception e) {}
        return iconView;
    }

    private void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            if (userEmail != null) prop.setProperty("email", userEmail);
            if (userPassword != null) prop.setProperty("password", userPassword);
            prop.setProperty("theme", currentTheme);
            prop.store(output, null);
        } catch (IOException io) { io.printStackTrace(); }
    }

    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Properties prop = new Properties();
            prop.load(input);
            userEmail = prop.getProperty("email");
            userPassword = prop.getProperty("password");
            String savedTheme = prop.getProperty("theme");
            if (savedTheme != null) currentTheme = savedTheme;
        } catch (IOException ex) {}
    }

    // --- SCREEN 1: LOGIN (WITH VALIDATION) ---
    private void showLoginScreen(Stage stage) {
        loadConfig();

        StackPane root = new StackPane();
        HBox topBar = new HBox(createThemeButton());
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(20));
        topBar.setPickOnBounds(false);

        VBox card = new VBox(15);
        card.setId("login-card");
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(350);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        ImageView logo = createLogo();
        logo.setFitWidth(100); logo.setFitHeight(100);

        Label title = new Label("Golden Email Client");
        title.getStyleClass().add("login-title");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email...");
        emailField.setAlignment(Pos.CENTER);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password...");
        passField.setAlignment(Pos.CENTER);

        Hyperlink helpLink = new Hyperlink("How to get your password?");
        helpLink.setStyle("-fx-text-fill: #b8860b; -fx-font-size: 12px; -fx-underline: true;");
        helpLink.setOnAction(e -> {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Gmail App Password Guide");
            info.setHeaderText("How to generate your Password");
            info.setContentText("1. Go to Google Account > Security.\n" +
                    "2. Enable '2-Step Verification'.\n" +
                    "3. Search for 'App Passwords'.\n" +
                    "4. Create one named 'GoldenEmail'.\n" +
                    "5. Copy the 16-character code and paste it here.");
            info.showAndWait();
        });

        CheckBox rememberMe = new CheckBox("Remember Me");

        if (userEmail != null) {
            emailField.setText(userEmail);
            rememberMe.setSelected(true);
        }
        if (userPassword != null) passField.setText(userPassword);

        // --- NEW: Loading Spinner ---
        ProgressIndicator loginSpinner = new ProgressIndicator();
        loginSpinner.setMaxSize(25, 25);
        loginSpinner.setVisible(false); // Hidden by default

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("gold-button");
        loginButton.setPrefWidth(200);

        // --- UPDATED LOGIN LOGIC ---
        loginButton.setOnAction(e -> {
            String inputEmail = emailField.getText();
            String inputPass = passField.getText();

            if (inputEmail.isEmpty() || inputPass.isEmpty()) {
                showAlert("Error", "Please enter both email and password.");
                return;
            }

            // 1. Show UI Loading state
            loginSpinner.setVisible(true);
            loginButton.setDisable(true);
            emailField.setDisable(true);
            passField.setDisable(true);

            // 2. Validate in Background Task
            Task<Boolean> loginTask = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    // Try to connect to server
                    EmailReceiver tempReceiver = new EmailReceiver(inputEmail, inputPass);
                    return tempReceiver.validateLogin();
                }
            };

            loginTask.setOnSucceeded(ev -> {
                // Login Success!
                userEmail = inputEmail;
                userPassword = inputPass;

                if (rememberMe.isSelected()) saveConfig();
                else {
                    String theme = currentTheme;
                    new File(CONFIG_FILE).delete();
                    currentTheme = theme;
                    saveConfig();
                }

                // Initialize global engines
                appSender = new EmailSender(userEmail, userPassword);
                appReceiver = new EmailReceiver(userEmail, userPassword);

                showDashboard(stage);
            });

            loginTask.setOnFailed(ev -> {
                // Login Failed
                loginSpinner.setVisible(false);
                loginButton.setDisable(false);
                emailField.setDisable(false);
                passField.setDisable(false);

                showAlert("Login Failed", "Invalid credentials.\nPlease check your email and App Password.");
            });

            new Thread(loginTask).start();
        });

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("version-label");

        // Add loginSpinner to the card
        card.getChildren().addAll(logo, title, emailField, passField, helpLink, rememberMe, loginButton, loginSpinner, version);

        root.getChildren().addAll(card, topBar);
        switchScreen(root);
    }

    // --- SCREEN 2: DASHBOARD ---
    private void showDashboard(Stage stage) {
        StackPane root = new StackPane();
        HBox topBar = new HBox(createThemeButton());
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(20));
        topBar.setPickOnBounds(false);

        VBox card = new VBox(25);
        card.setId("dashboard-card");
        card.setAlignment(Pos.CENTER);

        ImageView logo = createLogo();
        Label welcome = new Label("Welcome Back\n" + (userEmail != null ? userEmail : ""));
        welcome.getStyleClass().add("welcome-label");
        welcome.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button inboxButton = new Button("Show Inbox");
        inboxButton.getStyleClass().add("gold-button");
        inboxButton.setPrefWidth(200);
        inboxButton.setOnAction(e -> showInboxScreen(stage));

        Button composeButton = new Button("Compose Email");
        composeButton.getStyleClass().add("gold-button");
        composeButton.setPrefWidth(200);
        composeButton.setOnAction(e -> showComposeScreen(stage));

        buttonBox.getChildren().addAll(inboxButton, composeButton);

        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().add("red-button");
        logoutButton.setPrefWidth(420);
        logoutButton.setOnAction(e -> {
            userEmail = null;
            userPassword = null;
            appSender = null;
            appReceiver = null;
            showLoginScreen(stage);
        });

        card.getChildren().addAll(logo, welcome, buttonBox, logoutButton);
        root.getChildren().addAll(card, topBar);
        switchScreen(root);
    }

    // --- SCREEN 3: COMPOSE ---
    private void showComposeScreen(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backButton = new Button("⬅ Dashboard");
        backButton.setOnAction(e -> showDashboard(stage));

        ImageView logo = createLogo();
        logo.setFitHeight(40); logo.setFitWidth(40);
        Label screenTitle = new Label("Compose Email");
        screenTitle.getStyleClass().add("welcome-label");
        screenTitle.setStyle("-fx-font-size: 18px;");

        ProgressIndicator sendingSpinner = new ProgressIndicator();
        sendingSpinner.setMaxSize(30, 30);
        sendingSpinner.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button themeBtn = createThemeButton();

        topBar.getChildren().addAll(backButton, logo, screenTitle, sendingSpinner, spacer, themeBtn);
        root.setTop(topBar);

        VBox form = new VBox(15);
        form.setPadding(new Insets(20, 0, 0, 0));
        form.setAlignment(Pos.TOP_CENTER);

        TextField toField = new TextField();
        toField.setPromptText("To:");
        toField.setMaxWidth(800);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject:");
        subjectField.setMaxWidth(800);

        HTMLEditor messageEditor = new HTMLEditor();
        messageEditor.setPrefHeight(400);
        messageEditor.setMaxWidth(800);

        Button attachButton = new Button("📎 Attach Files");
        HBox previewBox = new HBox(10);
        previewBox.setAlignment(Pos.CENTER);
        Label fileLabel = new Label("No files selected");

        attachButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            selectedFiles = fileChooser.showOpenMultipleDialog(stage);
            previewBox.getChildren().clear();
            if (selectedFiles != null) {
                fileLabel.setText("Selected: " + selectedFiles.size() + " files");
                for (File file : selectedFiles) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                        ImageView thumb = new ImageView(new Image(file.toURI().toString()));
                        thumb.setFitHeight(50); thumb.setFitWidth(50); thumb.setPreserveRatio(true);
                        previewBox.getChildren().add(thumb);
                    }
                }
            }
        });

        Button sendButton = new Button("Send Email");
        sendButton.getStyleClass().add("gold-button");
        sendButton.setPrefWidth(200);

        sendButton.setOnAction(e -> {
            String to = toField.getText();
            String sub = subjectField.getText();
            String msg = messageEditor.getHtmlText();
            sendingSpinner.setVisible(true);
            sendButton.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    appSender.sendEmail(to, sub, msg, selectedFiles);
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                sendingSpinner.setVisible(false);
                sendButton.setDisable(false);
                showAlert("Success", "Email sent successfully!");
                toField.clear(); subjectField.clear(); messageEditor.setHtmlText("");
                selectedFiles = null; fileLabel.setText("No files selected");
                previewBox.getChildren().clear();
            });

            task.setOnFailed(ev -> {
                sendingSpinner.setVisible(false);
                sendButton.setDisable(false);
                showAlert("Error", "Failed to send email.\n" + task.getException().getMessage());
            });

            new Thread(task).start();
        });

        form.getChildren().addAll(toField, subjectField, messageEditor, attachButton, fileLabel, previewBox, sendButton);
        root.setCenter(form);
        switchScreen(root);
    }

    // --- SCREEN 4: INBOX ---
    private void showInboxScreen(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Button backButton = new Button("⬅ Dashboard");
        backButton.setOnAction(e -> showDashboard(stage));

        ImageView logo = createLogo();
        logo.setFitHeight(40); logo.setFitWidth(40);
        Label screenTitle = new Label("Inbox");
        screenTitle.getStyleClass().add("welcome-label");
        screenTitle.setStyle("-fx-font-size: 18px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(30, 30);
        spinner.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button themeBtn = createThemeButton();

        topBar.getChildren().addAll(backButton, logo, screenTitle, spinner, spacer, themeBtn);
        root.setTop(topBar);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 0, 0, 0));
        content.setAlignment(Pos.TOP_CENTER);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);

        ComboBox<String> folderBox = new ComboBox<>();
        folderBox.setPromptText("Select Folder");
        folderBox.setPrefWidth(200);
        Button loadFoldersBtn = new Button("📂 Load Folders");
        controls.getChildren().addAll(loadFoldersBtn, folderBox);

        Label emailsLabel = new Label("Emails:");
        emailsLabel.getStyleClass().add("gold-label");

        ComboBox<String> emailBox = new ComboBox<>();
        emailBox.setPromptText("Select Email to Read");
        emailBox.setPrefWidth(600);

        WebView emailWebView = new WebView();
        emailWebView.setPrefHeight(400);
        emailWebView.setMaxWidth(800);
        emailWebView.setPageFill(Color.TRANSPARENT);

        StackPane emailPaper = new StackPane(emailWebView);
        emailPaper.getStyleClass().add("email-paper");
        emailPaper.setMaxWidth(802);

        VBox attachmentLayout = new VBox(15);
        attachmentLayout.setAlignment(Pos.CENTER);
        Label attachLabel = new Label("Attachments:");
        attachLabel.getStyleClass().add("gold-label");

        content.getChildren().addAll(controls, emailsLabel, emailBox, emailPaper, attachmentLayout);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        loadFoldersBtn.setOnAction(e -> {
            spinner.setVisible(true);
            Task<List<String>> task = new Task<>() {
                @Override protected List<String> call() throws Exception { return appReceiver.getFolderList(); }
            };
            task.setOnSucceeded(ev -> {
                spinner.setVisible(false);
                folderBox.getItems().setAll(task.getValue());
                if (!task.getValue().isEmpty()) folderBox.getSelectionModel().select("INBOX");
            });
            task.setOnFailed(ev -> { spinner.setVisible(false); showAlert("Error", "Failed to load folders."); });
            new Thread(task).start();
        });

        folderBox.setOnAction(e -> {
            String folder = folderBox.getValue();
            if (folder != null) {
                spinner.setVisible(true);
                Task<List<String>> task = new Task<>() {
                    @Override protected List<String> call() throws Exception { return appReceiver.getEmailSubjects(folder); }
                };
                task.setOnSucceeded(ev -> {
                    spinner.setVisible(false);
                    emailBox.getItems().setAll(task.getValue());
                });
                new Thread(task).start();
            }
        });

        emailBox.setOnAction(e -> {
            int index = emailBox.getSelectionModel().getSelectedIndex();
            String folder = folderBox.getValue();
            if (index >= 0 && folder != null) {
                spinner.setVisible(true);
                emailWebView.getEngine().loadContent("<body style='color: black;'><h3>Loading content...</h3></body>");

                Task<EmailReceiver.EmailContent> task = new Task<>() {
                    @Override protected EmailReceiver.EmailContent call() throws Exception {
                        return appReceiver.readSpecificEmail(folder, index);
                    }
                };

                task.setOnSucceeded(ev -> {
                    spinner.setVisible(false);
                    EmailReceiver.EmailContent data = task.getValue();
                    String styledContent = "<body style='color: black; background-color: transparent; font-family: Segoe UI;'>"
                            + data.htmlBody + "</body>";
                    emailWebView.getEngine().loadContent(styledContent);

                    attachmentLayout.getChildren().clear();
                    if (!data.attachmentNames.isEmpty()) {
                        attachmentLayout.getChildren().add(attachLabel);
                        for (String filename : data.attachmentNames) {
                            VBox fileContainer = new VBox(5);
                            fileContainer.setAlignment(Pos.CENTER);
                            fileContainer.getStyleClass().add("file-container");
                            fileContainer.setMaxWidth(400);

                            HBox buttonRow = new HBox(10);
                            buttonRow.setAlignment(Pos.CENTER);

                            Button fileBtn = new Button("⬇ Download " + filename);
                            fileBtn.getStyleClass().add("file-button");
                            ProgressIndicator fileSpinner = new ProgressIndicator();
                            fileSpinner.setMaxSize(20, 20);
                            fileSpinner.setVisible(false);

                            buttonRow.getChildren().addAll(fileBtn, fileSpinner);
                            StackPane previewArea = new StackPane();
                            previewArea.setPadding(new Insets(10));
                            Label statusLabel = new Label("");
                            statusLabel.setStyle("-fx-text-fill: #888;");
                            previewArea.getChildren().add(statusLabel);

                            fileContainer.getChildren().addAll(buttonRow, previewArea);
                            attachmentLayout.getChildren().add(fileContainer);

                            fileBtn.setOnAction(event -> {
                                fileSpinner.setVisible(true);
                                fileBtn.setDisable(true);
                                statusLabel.setText("Downloading preview...");

                                Task<File> downloadTask = new Task<>() {
                                    @Override protected File call() throws Exception {
                                        String result = appReceiver.downloadAttachment(folder, index, filename);
                                        if (result.startsWith("Error")) throw new Exception(result);
                                        String path = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + filename;
                                        return new File(path);
                                    }
                                };

                                downloadTask.setOnSucceeded(de -> {
                                    fileSpinner.setVisible(false);
                                    fileBtn.setDisable(false);
                                    fileBtn.setText("✅ Saved: " + filename);
                                    statusLabel.setText("");
                                    File downloadedFile = downloadTask.getValue();
                                    String lowerName = filename.toLowerCase();
                                    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg")) {
                                        try {
                                            ImageView preview = new ImageView(new Image(downloadedFile.toURI().toString()));
                                            preview.setFitWidth(350);
                                            preview.setPreserveRatio(true);
                                            previewArea.getChildren().setAll(preview);
                                        } catch (Exception ex) {}
                                    } else { statusLabel.setText("File saved."); }
                                });
                                downloadTask.setOnFailed(de -> {
                                    fileSpinner.setVisible(false);
                                    fileBtn.setDisable(false);
                                    statusLabel.setText("❌ Error");
                                    showAlert("Error", "Download failed.");
                                });
                                new Thread(downloadTask).start();
                            });
                        }
                    }
                });
                new Thread(task).start();
            }
        });

        switchScreen(root);
        Platform.runLater(() -> loadFoldersBtn.fire());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}