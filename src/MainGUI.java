// ════════════════════════════════════════════════════════════════════
//  Wakanda Resource Management System — MainGUI.java
//  Upgraded per Technical Report (CMPG211 / CMPG215)
//
//  Improvements:
//    • SHA-256 password hashing (PasswordUtil)
//    • Brute-force lockout (3 failed attempts)
//    • Abstract User hierarchy (Administrator / Technician)
//    • ResourceManager CRUD layer
//    • AuthenticationService with session tokens
//    • SystemLog (read-only, encrypted append)
//    • ReportGenerator
//    • Role-Based Access Control (RBAC)
//    • Input validation & data encryption stubs
//    • All screens maximized with shared background image
//
//  Compile:
//    javac --module-path <fx-lib> --add-modules javafx.controls,javafx.base MainGUI.java
//  Run:
//    java  --module-path <fx-lib> --add-modules javafx.controls,javafx.base MainGUI
// ════════════════════════════════════════════════════════════════════

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.image.*;

// ════════════════════════════════════════════════════════════════════
//  UI — MainGUI  (JavaFX Application)
// ════════════════════════════════════════════════════════════════════
public class MainGUI extends Application
{
    // ── App identity ─────────────────────────────────────────────
    private static final String APP_NAME = "Wakanda Resource Management System";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Infrastructure (shared) ──────────────────────────────────
    private final SystemLog              sysLog  = new SystemLog();
    private final ResourceManager        resMgr  = new ResourceManager(sysLog);
    private final ReportGenerator        repGen  = new ReportGenerator(resMgr, sysLog);
    private final AuthenticationService  authSvc = new AuthenticationService(sysLog);

    // ── Domain state ─────────────────────────────────────────────
    private final List<NormalUser>  normalUsers = new ArrayList<>();
    private final List<Technician>  technicians = new ArrayList<>();
    private       Administrator     adminUser   = null;
    private       User              currentUser = null;

    // ── JavaFX stage ─────────────────────────────────────────────
    private Stage mainStage;

    // ── Temp sign-up state ───────────────────────────────────────
    private String tempEmail = "", tempPassword = "", tempRole = "", tempName = "";

    // ── Persistence ──────────────────────────────────────────────
    private static final String USERS_FILE = "users.txt";

    // ── Resource grid indicators — separated by type ─────────────────
    private static final String[] ELECTRICITY_REGIONS = {
        "Grid A", "Grid B", "Grid C", "Grid D"
    };
    private static final String[] WATER_REGIONS = {
        "Meter 9087", "Meter 7898", "Meter 2652", "Meter 1001", "Meter 3300"
    };

    // ── Shared background ────────────────────────────────────────
    /** Builds the shared Background from 1.jpg, covering the full pane. */
    private Background buildBackground(String name)
    {
        try
        {
            Image bgImage = new Image(name+".jpg");
            BackgroundSize bgSize = new BackgroundSize(
                    100, 100, true, true, false, true);
            BackgroundImage bg = new BackgroundImage(
                    bgImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    bgSize);
            return new Background(bg);
        }
        catch (Exception e)
        {
            // Fallback: dark gradient if image is missing
            return new Background(new BackgroundFill(
                    javafx.scene.paint.Color.web("#0a1628"), CornerRadii.EMPTY, Insets.EMPTY));
        }
    }

    /**
     * Wraps any content node in a StackPane with the shared background,
     * binds a Scene of the given base size, maximizes the stage, and shows it.
     */
    private void applySceneWithBackground(Region content, double baseW, double baseH,String name)
    {
        StackPane root = new StackPane(content);
        root.setBackground(buildBackground(name));
        // Allow content to grow naturally with the window — no scale binding
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        Scene scene = new Scene(root, baseW, baseH);
        mainStage.setScene(scene);
        mainStage.setMaximized(true);
        mainStage.show();
    }


    // ════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage)
    {
        mainStage = stage;
        loadUsersFromFile();
        mainStage.setOnCloseRequest(e -> saveUsersToFile());
        showLoginScreen();
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 1 — LOGIN
    // ════════════════════════════════════════════════════════════
    private void showLoginScreen()
    {
        mainStage.setTitle(APP_NAME + " — Login");

        Label appLabel = new Label(APP_NAME);
        appLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));
        appLabel.setStyle("-fx-text-fill: #2c7be5;");

        Label header = new Label("Sign In");
        header.setFont(Font.font("SansSerif", FontWeight.BOLD, 10));
        header.setStyle("-fx-text-fill: #2c7be5;");

        TextField     emailField    = new TextField();
        emailField.setPromptText("Email address");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        TextField passwordVisible = new TextField();
        passwordVisible.setManaged(false);
        passwordVisible.setVisible(false);

        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!passwordVisible.getText().equals(n)) passwordVisible.setText(n);
        });
        passwordVisible.textProperty().addListener((obs, o, n) -> {
            if (!passwordField.getText().equals(n)) passwordField.setText(n);
        });
        CheckBox showPw = new CheckBox("Show");
        showPw.setOnAction(e -> {
            boolean s = showPw.isSelected();
            passwordField.setManaged(!s); passwordField.setVisible(!s);
            passwordVisible.setManaged(s);  passwordVisible.setVisible(s);
        });
        StackPane pwStack = new StackPane(passwordField, passwordVisible);
        HBox pwRow = new HBox(6, pwStack, showPw);
        pwRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("User", "Technician", "Admin");
        roleBox.setPromptText("Select role");

        Button signInBtn = new Button("Sign In");
        Button signUpBtn = new Button("Create Account");
        Button forgotBtn = new Button("Forgot Password");
        forgotBtn.setStyle("-fx-font-size: 11px;");

        Label attemptsLbl = new Label("Attempts remaining: "
                + (authSvc.getMaxAttempts() - authSvc.getFailedAttempts()));
        attemptsLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        Label status = new Label();
        status.setStyle("-fx-text-fill: #c0392b;");

        // ── SIGN IN ──────────────────────────────────────────────
        signInBtn.setOnAction(e ->
        {
            String r  = roleBox.getValue();
            String em = emailField.getText().trim();
            String pw = passwordField.getText();

            if (r == null)         { status.setText("Please select a role.");               return; }
            if (!isValidEmail(em)) { status.setText("Enter a valid email address.");        return; }
            if (pw.isEmpty())      { status.setText("Enter your password.");                return; }

            if (authSvc.isLocked())
            {
                status.setText("Account locked — use 'Forgot Password' to reset.");
                return;
            }

            if (r.equals("Admin"))
            {
                if (adminUser == null) { status.setText("No admin account exists."); return; }
                TextInputDialog dlg = new TextInputDialog();
                dlg.setTitle("Admin Login"); dlg.setHeaderText("Enter your Admin ID");
                String id = dlg.showAndWait().orElse("");
                if (adminUser.login(id, em, pw))
                {
                    authSvc.unlockAccount();
                    currentUser = adminUser;
                    sysLog.recordAction(em, "Admin login successful");
                    showAdminDashboard();
                }
                else
                {
                    status.setText("Invalid admin credentials.");
                    sysLog.recordAction(em, "Admin login failed");
                }
                return;
            }

            User found = null;
            if (r.equals("User"))
                found = normalUsers.stream()
                        .filter(u -> u.getEmail().equals(em)).findFirst().orElse(null);
            else
                found = technicians.stream()
                        .filter(t -> t.getEmail().equals(em)).findFirst().orElse(null);

            if (found != null && authSvc.authenticateUser(found, pw))
            {
                currentUser = found;
                status.setStyle("-fx-text-fill: #27ae60;");
                status.setText("✅ Welcome back! Existing account recognised.");
                if (found instanceof Technician) showTechDashboard((Technician) found);
                else showUserDashboard((NormalUser) found);
            }
            else
            {
                
                
                attemptsLbl.setText("Attempts remaining: "
                        + Math.max(0, authSvc.getMaxAttempts() - authSvc.getFailedAttempts()));
                status.setText("Invalid credentials.");
                attemptsLbl.setStyle("-fx-text-fill: red;");
                status.setStyle("-fx-text-fill: red;");
            }
        });

        // ── SIGN UP ──────────────────────────────────────────────
        signUpBtn.setOnAction(e ->
        {
            String r  = roleBox.getValue();
            String em = emailField.getText().trim();
            String pw = passwordField.getText();

            if (r == null)         { status.setText("Please select a role.");                return; }
            if (!isValidEmail(em)) { status.setText("Enter a valid email address.");         return; }
            if (pw.isEmpty())      { status.setText("Enter a password.");                    return; }

            String pwErr = validatePassword(pw);
            if (pwErr != null)     { status.setText(pwErr);                                  return; }

            if (r.equals("Admin") && adminUser != null)
            { status.setText("Only one admin account is allowed."); return; }

            for (NormalUser u : normalUsers)
                if (u.getEmail().equals(em)) { status.setText("Email already registered."); return; }
            for (Technician t : technicians)
                if (t.getEmail().equals(em)) { status.setText("Email already registered."); return; }

            tempEmail    = em;
            tempPassword = PasswordUtil.hash(pw);
            tempRole     = r;
            showSignUpNameScreen();
        });

        // ── FORGOT PASSWORD ───────────────────────────────────────
        forgotBtn.setOnAction(e ->
        {
            String r  = roleBox.getValue();
            String em = emailField.getText().trim();
            if (!isValidEmail(em))
            { status.setText("Enter a valid email address first."); return; }
            showForgotPasswordScreen(r == null ? "User" : r, em);
        });

        // ── Layout ───────────────────────────────────────────────
        Label emailLbl = new Label("Email:");
        emailLbl.setStyle("-fx-text-fill: white;");
        Label passwordLbl = new Label("Password:");
        passwordLbl.setStyle("-fx-text-fill: white;");
        Label roleLbl = new Label("Role:");
        roleLbl.setStyle("-fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(24));
        grid.setVgap(12);
        grid.setHgap(10);

        grid.add(appLabel,                             0, 0, 2, 1);
        grid.add(header,                               0, 1, 2, 1);
        grid.add(emailLbl,                             0, 2); grid.add(emailField,  1, 2);
        grid.add(passwordLbl,                          0, 3); grid.add(pwRow,       1, 3);
        grid.add(roleLbl,                              0, 4); grid.add(roleBox,     1, 4);
        grid.add(new HBox(10, signInBtn, signUpBtn),   0, 5, 2, 1);
        grid.add(forgotBtn,                            0, 6, 2, 1);
        grid.add(attemptsLbl,                          0, 7, 2, 1);
        grid.add(status,                               0, 8, 2, 1);

        applySceneWithBackground(grid, 920, 560, "2");
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 2 — SIGN-UP NAME
    // ════════════════════════════════════════════════════════════
    private void showSignUpNameScreen()
    {
        mainStage.setTitle("Create Account");

        Label header = new Label("Create Account — " + tempRole);
        header.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
        header.setStyle("-fx-text-fill: #2c7be5;");

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");

        Button createBtn = new Button("Create");
        Button backBtn   = new Button("Back");
        Label  status    = new Label();

        createBtn.setOnAction(e ->
        {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { status.setText("Enter your full name."); return; }
            tempName = name;

            switch (tempRole)
            {
                case "Admin":
                    String adminID = "AD" + (1000 + new Random().nextInt(9000));
                    adminUser = new Administrator(tempEmail, tempPassword, adminID, tempName);
                    new Alert(Alert.AlertType.INFORMATION,
                            "✅ New Admin account created!\n\nAdmin ID: " + adminID
                            + "\n\nKeep this ID safe — you need it every login.").showAndWait();
                    sysLog.recordAction(tempEmail, "Admin account created");
                    break;

                case "Technician":
                    String techID = "TECH-" + (1000 + new Random().nextInt(9000));
                    technicians.add(new Technician(tempEmail, tempPassword, techID, tempName));
                    new Alert(Alert.AlertType.INFORMATION,
                            "✅ New Technician account created!\n\nTech ID: " + techID
                            + "\n\nAwait admin approval before you can be assigned.").showAndWait();
                    sysLog.recordAction(tempEmail, "Technician account created [" + techID + "]");
                    break;

                default:
                    normalUsers.add(new NormalUser(tempEmail, tempPassword, tempName));
                    new Alert(Alert.AlertType.INFORMATION,
                            "✅ New User account created!\n\nYou can now sign in with your email and password.").showAndWait();
                    sysLog.recordAction(tempEmail, "User account created");
                    break;
            }
            saveUsersToFile();
            clearTemp();
            showLoginScreen();
        });

        backBtn.setOnAction(e -> { clearTemp(); showLoginScreen(); });

        GridPane g = new GridPane();
        g.setAlignment(Pos.CENTER);
        g.setPadding(new Insets(40));
        g.setVgap(14);
        g.setHgap(12);

        Label nameLbl = new Label("Full name:");
        nameLbl.setStyle("-fx-text-fill: #2c2c2c;");

        g.add(header,                           0, 0, 2, 1);
        g.add(nameLbl,                          0, 1); g.add(nameField, 1, 1);
        g.add(new HBox(10, backBtn, createBtn), 0, 2, 2, 1);
        g.add(status,                           0, 3, 2, 1);

        applySceneWithBackground(g, 920, 560,"2");
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 3 — FORGOT PASSWORD
    // ════════════════════════════════════════════════════════════
    private void showForgotPasswordScreen(String role, String email)
    {
        User target = findUserByEmailAndRole(role, email);
        if (target == null)
        {
            new Alert(Alert.AlertType.ERROR,
                    "No " + role.toLowerCase() + " account found for: " + email).showAndWait();
            return;
        }

        String code = "" + (100 + new Random().nextInt(900));
        new Alert(Alert.AlertType.INFORMATION,
                "Verification code sent to: " + email
                + "\n\n[Simulated] Code: " + code).showAndWait();

        mainStage.setTitle("Reset Password");

        TextField     codeField  = new TextField(); codeField.setPromptText("3-digit code");
        PasswordField newPw      = new PasswordField(); newPw.setPromptText("New password");
        PasswordField confirm    = new PasswordField(); confirm.setPromptText("Confirm password");
        Button resetBtn          = new Button("Reset");
        Button cancelBtn         = new Button("Cancel");
        Label  status            = new Label();

        resetBtn.setOnAction(e ->
        {
            if (!codeField.getText().trim().equals(code))
            { status.setText("Incorrect code."); return; }
            String err = validatePassword(newPw.getText());
            if (err != null)                     { status.setText(err); return; }
            if (!newPw.getText().equals(confirm.getText()))
            { status.setText("Passwords do not match."); return; }
            target.setPassword(newPw.getText());
            sysLog.recordAction(email, "Password reset");
            authSvc.unlockAccount();
            new Alert(Alert.AlertType.INFORMATION, "Password reset successfully!").showAndWait();
            showLoginScreen();
        });

        cancelBtn.setOnAction(e -> showLoginScreen());

        Label acctLbl  = new Label("Account: " + email + " (" + role + ")");
        acctLbl.setStyle("-fx-text-fill: white;");
        Label codeLbl  = new Label("Code:");
        codeLbl.setStyle("-fx-text-fill: white;");
        Label newPwLbl = new Label("New password:");
        newPwLbl.setStyle("-fx-text-fill: white;");
        Label cfmLbl   = new Label("Confirm:");
        cfmLbl.setStyle("-fx-text-fill: white;");

        Label resetHeader = new Label("Reset Password");
        resetHeader.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
        resetHeader.setStyle("-fx-text-fill: #2c7be5;");

        GridPane g = new GridPane();
        g.setAlignment(Pos.CENTER);
        g.setPadding(new Insets(40));
        g.setVgap(14);
        g.setHgap(12);

        g.add(resetHeader, 0, 0, 2, 1);
        g.add(acctLbl,     0, 1, 2, 1);
        g.add(codeLbl,     0, 2); g.add(codeField, 1, 2);
        g.add(newPwLbl,    0, 3); g.add(newPw,     1, 3);
        g.add(cfmLbl,      0, 4); g.add(confirm,   1, 4);
        g.add(new HBox(10, cancelBtn, resetBtn), 0, 5, 2, 1);
        g.add(status,      0, 6, 2, 1);

        applySceneWithBackground(g, 920, 560,"5");
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 4 — ADMIN DASHBOARD  (RBAC: Admin only)
    // ════════════════════════════════════════════════════════════
    private void showAdminDashboard()
    {
        mainStage.setTitle(APP_NAME + " — Admin Dashboard");

        // ── Header ───────────────────────────────────────────────
        Label header = new Label(APP_NAME + "  |  Admin: "
                + adminUser.getName() + "  [ID: " + adminUser.getAdminID() + "]");
        header.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        header.setStyle("-fx-text-fill: blue;");

        // ── Filters ─────────────────────────────────────────────
        ComboBox<String> usageFilter = new ComboBox<>();
        usageFilter.getItems().addAll("Combined", "Water", "Electricity");
        usageFilter.setValue("Combined");

        ComboBox<String> timeFilter = new ComboBox<>();
        timeFilter.getItems().addAll("All Time", "Today", "This Week", "This Month");
        timeFilter.setValue("All Time");

        // ── Usage table ──────────────────────────────────────────
        TableView<UsageRecord> table = buildUsageTable();
        LocalDate today = LocalDate.now();

        Runnable loadData = () -> populateUsageTable(
                table, "Users", usageFilter.getValue(),
                timeFilter.getValue(), today);
        loadData.run();
        usageFilter.setOnAction(e -> loadData.run());
        timeFilter .setOnAction(e -> loadData.run());

        // ── Resource management ──────────────────────────────────
        Label resLabel = new Label("Resources");
        resLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        resLabel.setStyle("-fx-text-fill: #e0e8ff;");

        ListView<String> resList = new ListView<>();
        refreshResList(resList);
        resList.setPrefHeight(140);

        TextField resNameField = new TextField(); resNameField.setPromptText("Resource name");
        ComboBox<String> resType = new ComboBox<>();
        resType.getItems().addAll("Water", "Electricity"); resType.setValue("Water");
        Button addResBtn = new Button("Add Resource");
        addResBtn.setOnAction(e ->
        {
            String name = resNameField.getText().trim();
            if (name.isEmpty()) return;
            int id = resMgr.getAll().size() + 1;
            Resource r = resType.getValue().equals("Water")
                    ? new WaterResource(id, name, 0, "Unknown")
                    : new ElectricityResource(id, name, 0, 220, 0);
            resMgr.create(r);
            sysLog.recordAction(adminUser.getEmail(), "Admin added resource: " + name);
            resNameField.clear();
            refreshResList(resList);
        });

        // ── User actions ─────────────────────────────────────────
        ComboBox<User> userPicker = buildUserPicker();

        Button approveBtn  = new Button("Approve Tech");
        Button deleteBtn   = new Button("Delete");
        Button viewLogsBtn = new Button("View Logs");
        Button reportBtn   = new Button("Generate Report");
        Button logoutBtn   = new Button("Logout");
        Label  status      = new Label();
        status.setStyle("-fx-text-fill: #ffe082;");

        Button removeResBtn = new Button("Remove Resource");
        removeResBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
        removeResBtn.setOnAction(e ->
        {
            int selectedIndex = resList.getSelectionModel().getSelectedIndex();
            if (selectedIndex < 0)
            {
                status.setText("Select a resource from the list to remove.");
                status.setStyle("-fx-text-fill: #c0392b;");
                return;
            }
            List<Resource> sorted = new ArrayList<>(resMgr.getAll());
            sorted.sort(Comparator.comparing(r -> r.getResourceName().toLowerCase()));
            Resource toRemove = sorted.get(selectedIndex);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove resource \"" + toRemove.getResourceName() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Remove");
            confirm.showAndWait().ifPresent(btn ->
            {
                if (btn == ButtonType.YES)
                {
                    resMgr.delete(toRemove.getResourceID());
                    sysLog.recordAction(adminUser.getEmail(),
                            "Admin removed resource: " + toRemove.getResourceName());
                    refreshResList(resList);
                    status.setText("Resource \"" + toRemove.getResourceName() + "\" removed.");
                    status.setStyle("-fx-text-fill: #27ae60;");
                }
            });
        });
        approveBtn.setOnAction(e ->
        {
            User sel = userPicker.getValue();
            if (sel instanceof Technician)
            {
                ((Technician) sel).approve();
                sysLog.recordAction(adminUser.getName(),
                        "Approved technician: " + sel.getName());
                status.setText("Technician approved.");
            }
            else status.setText("Select a Technician first.");
        });

        deleteBtn.setOnAction(e ->
        {
            User sel = userPicker.getValue();
            if (sel == null) { status.setText("Select a user."); return; }
            normalUsers.remove(sel);
            technicians.remove(sel);
            sysLog.recordAction(adminUser.getName(), "Deleted user: " + sel.getName());
            userPicker.getItems().remove(sel);
            loadData.run();
            status.setText("User deleted.");
        });

        viewLogsBtn.setOnAction(e ->
        {
            User sel = userPicker.getValue();
            if (!(sel instanceof Technician)) { status.setText("Select a Technician."); return; }
            Technician t = (Technician) sel;
            String logs = t.getWorkLogs().isEmpty() ? "No logs yet."
                    : String.join("\n", t.getWorkLogs());
            new Alert(Alert.AlertType.INFORMATION, logs).showAndWait();
        });

        reportBtn.setOnAction(e ->
        {
            String report = repGen.generateResourceReport();
            new Alert(Alert.AlertType.INFORMATION, report).showAndWait();
        });

        logoutBtn.setOnAction(e ->
        {
            authSvc.invalidateSession();
            sysLog.recordAction(adminUser.getEmail(), "Admin logout");
            currentUser = null;
            showLoginScreen();
        });

        // ── System log viewer ────────────────────────────────────
        Label logLabel = new Label("System Log (read-only)");
        logLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        logLabel.setStyle("-fx-text-fill: #e0e8ff;");

        ListView<String> logView = new ListView<>();
        for (SystemLog.LogEntry le : sysLog.getEntries())
            logView.getItems().add(0, "[" + le.timestamp + "] " + le.username + " — " + le.action);
        logView.setPrefHeight(140);

        // ── Left column ──────────────────────────────────────────
        VBox leftCol = new VBox(10,
                resLabel,
                resList,
                new HBox(6, resNameField, resType, addResBtn),
                removeResBtn,
                new Separator(),
                logLabel,
                logView);
        leftCol.setPadding(new Insets(12));
        leftCol.setPrefWidth(380);
        leftCol.setStyle("-fx-background-color: rgba(10,22,40,0.72); -fx-background-radius: 8;");

        // ── Right column ─────────────────────────────────────────
        VBox rightCol = new VBox(10,
                new HBox(10, new Label("Usage:"), usageFilter,
                             new Label("Time:"), timeFilter),
                table,
                new HBox(8, new Label("Action on:"), userPicker),
                new HBox(6, approveBtn, deleteBtn, viewLogsBtn, reportBtn, logoutBtn),
                status);
        rightCol.setPadding(new Insets(12));
        rightCol.setStyle("-fx-background-color: rgba(10,22,40,0.72); -fx-background-radius: 8;");

        HBox cols = new HBox(10, leftCol, rightCol);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        VBox.setVgrow(cols, Priority.ALWAYS);

        VBox outer = new VBox(10, header, cols);
        outer.setPadding(new Insets(16));
        outer.setFillWidth(true);

        // Wrap in a BorderPane so content fills the maximized window naturally
        javafx.scene.layout.BorderPane bp = new javafx.scene.layout.BorderPane(outer);
        applySceneWithBackground(bp, 1200, 750,"1");
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 5 — USER DASHBOARD  (RBAC: User only)
    // ════════════════════════════════════════════════════════════
    private void showUserDashboard(NormalUser u)
    {
        mainStage.setTitle(APP_NAME + " — User Dashboard");

        Label header = new Label(APP_NAME + "  |  User: " + u.getName());
        header.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        header.setStyle("-fx-text-fill: #ffffff;");

        // ── Resource totals display ───────────────────────────────
        // Find matching water/electricity resources from resource manager
        Runnable[] refreshRef = new Runnable[1];

        Label waterTotalLbl = new Label();
        Label elecTotalLbl  = new Label();
        waterTotalLbl.setStyle("-fx-text-fill: #80deea; -fx-font-weight: bold;");
        elecTotalLbl .setStyle("-fx-text-fill: #ffe082; -fx-font-weight: bold;");

        Label userWaterLbl = new Label();
        Label userElecLbl  = new Label();
        userWaterLbl.setStyle("-fx-text-fill: #e0e8ff;");
        userElecLbl .setStyle("-fx-text-fill: #e0e8ff;");

        // ── Log usage ────────────────────────────────────────────
        Label logTitle = new Label("Log New Usage");
        logTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        logTitle.setStyle("-fx-text-fill: #90caf9;");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        // Separate region/meter dropdowns
        ComboBox<String> elecRegionBox = new ComboBox<>();
        elecRegionBox.getItems().addAll(ELECTRICITY_REGIONS);
        elecRegionBox.setPromptText("Select electricity region (e.g. Grid A)");
        elecRegionBox.setEditable(true);
        elecRegionBox.setPrefWidth(260);

        ComboBox<String> waterRegionBox = new ComboBox<>();
        waterRegionBox.getItems().addAll(WATER_REGIONS);
        waterRegionBox.setPromptText("Select water meter (e.g. Meter 9087)");
        waterRegionBox.setEditable(true);
        waterRegionBox.setPrefWidth(260);

        // Separate usage fields
        TextField elecFld = new TextField(); elecFld.setPromptText("Electricity usage (kWh)");
        TextField waterFld = new TextField(); waterFld.setPromptText("Water usage (L)");

        Button submitBtn = new Button("Submit Usage");
        Label  status    = new Label();
        status.setStyle("-fx-text-fill: #ffe082;");

        // Refresh totals: user running totals + resource pool remaining
        refreshRef[0] = () -> {
            userWaterLbl.setText("Your water logged:       " + String.format("%.1f", u.getWaterTotal()) + " L");
            userElecLbl .setText("Your electricity logged: " + String.format("%.1f", u.getElectricityTotal()) + " kWh");

            // Sum remaining in matching resource pool
            double waterPool = resMgr.getAll().stream()
                    .filter(r -> r instanceof WaterResource)
                    .mapToDouble(Resource::getUsage).sum();
            double elecPool = resMgr.getAll().stream()
                    .filter(r -> r instanceof ElectricityResource)
                    .mapToDouble(Resource::getUsage).sum();
            waterTotalLbl.setText("Water pool remaining:       " + String.format("%.1f", waterPool) + " L");
            elecTotalLbl .setText("Electricity pool remaining: " + String.format("%.1f", elecPool) + " kWh");
        };
        refreshRef[0].run();

        submitBtn.setOnAction(e ->
        {
            LocalDate d = datePicker.getValue();
            String elecRegion  = elecRegionBox.getValue()  != null ? elecRegionBox.getValue().trim()  : "";
            String waterRegion = waterRegionBox.getValue() != null ? waterRegionBox.getValue().trim() : "";
            if (d == null) { status.setText("Select a date."); status.setStyle("-fx-text-fill: #c0392b;"); return; }
            if (d.isAfter(LocalDate.now())) { status.setText("Cannot log a future date."); status.setStyle("-fx-text-fill: #c0392b;"); return; }

            double w = 0, el = 0;
            boolean hasWater = !waterFld.getText().isBlank();
            boolean hasElec  = !elecFld .getText().isBlank();

            if (!hasWater && !hasElec) { status.setText("Enter at least one usage value."); status.setStyle("-fx-text-fill: #c0392b;"); return; }
            if (hasWater && waterRegion.isEmpty()) { status.setText("Select a water meter for water usage."); status.setStyle("-fx-text-fill: #c0392b;"); return; }
            if (hasElec  && elecRegion .isEmpty()) { status.setText("Select an electricity region for electricity usage."); status.setStyle("-fx-text-fill: #c0392b;"); return; }

            try
            {
                if (hasWater) w  = Double.parseDouble(waterFld.getText().trim());
                if (hasElec)  el = Double.parseDouble(elecFld .getText().trim());
                if (w < 0 || el < 0) { status.setText("Values must be non-negative."); status.setStyle("-fx-text-fill: #c0392b;"); return; }

                if (u.hasEntryForDate(d))
                {
                    status.setText("Usage already recorded for " + d.format(DATE_FMT) + ". Only one entry per day.");
                    status.setStyle("-fx-text-fill: #c0392b;");
                    return;
                }

                // Subtract from matching resource pool (water resources for water, elec resources for elec)
                if (hasWater && w > 0)
                {
                    double remaining = w;
                    for (Resource r : resMgr.getAll())
                    {
                        if (r instanceof WaterResource && r.getUsage() > 0)
                        {
                            double take = Math.min(r.getUsage(), remaining);
                            r.setUsage(r.getUsage() - take);
                            remaining -= take;
                            if (remaining <= 0) break;
                        }
                    }
                }
                if (hasElec && el > 0)
                {
                    double remaining = el;
                    for (Resource r : resMgr.getAll())
                    {
                        if (r instanceof ElectricityResource && r.getUsage() > 0)
                        {
                            double take = Math.min(r.getUsage(), remaining);
                            r.setUsage(r.getUsage() - take);
                            remaining -= take;
                            if (remaining <= 0) break;
                        }
                    }
                }

                u.addUsageEntry(d, w, el);
                sysLog.recordAction(u.getEmail(),
                        String.format("Usage logged: water=%.1fL [%s], elec=%.1fkWh [%s] on %s",
                                w, waterRegion, el, elecRegion, d.format(DATE_FMT)));

                waterFld.clear(); elecFld.clear();
                elecRegionBox.setValue(null); waterRegionBox.setValue(null);
                refreshRef[0].run();
                status.setText("Usage logged for " + d.format(DATE_FMT) + ".");
                status.setStyle("-fx-text-fill: #27ae60;");
            }
            catch (NumberFormatException ex)
            { status.setText("Enter valid numeric values."); status.setStyle("-fx-text-fill: #c0392b;"); }
        });

        // ── Delete usage ─────────────────────────────────────────
        Label delTitle = new Label("Delete Usage Entry");
        delTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        delTitle.setStyle("-fx-text-fill: #90caf9;");

        DatePicker deleteDatePicker = new DatePicker(LocalDate.now());
        Button     deleteUsageBtn   = new Button("Delete Usage");
        deleteUsageBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        deleteUsageBtn.setOnAction(e ->
        {
            LocalDate d = deleteDatePicker.getValue();
            if (d == null) { status.setText("Select a date to delete."); status.setStyle("-fx-text-fill: #c0392b;"); return; }
            if (!u.hasEntryForDate(d))
            {
                status.setText("No usage entry found for " + d.format(DATE_FMT) + ".");
                status.setStyle("-fx-text-fill: #c0392b;");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete usage entry for " + d.format(DATE_FMT) + "?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Delete");
            confirm.showAndWait().ifPresent(btn ->
            {
                if (btn == ButtonType.YES)
                {
                    // Find the entry to restore resource pool
                    UsageEntry target = u.getUsageEntries().stream()
                            .filter(en -> en.date.isEqual(d)).findFirst().orElse(null);
                    if (target != null)
                    {
                        // Restore water pool
                        if (target.water > 0)
                        {
                            for (Resource r : resMgr.getAll())
                            {
                                if (r instanceof WaterResource) { r.setUsage(r.getUsage() + target.water); break; }
                            }
                        }
                        // Restore electricity pool
                        if (target.electricity > 0)
                        {
                            for (Resource r : resMgr.getAll())
                            {
                                if (r instanceof ElectricityResource) { r.setUsage(r.getUsage() + target.electricity); break; }
                            }
                        }
                    }
                    u.deleteUsageEntry(d);
                    sysLog.recordAction(u.getEmail(), "Usage entry deleted for " + d.format(DATE_FMT));
                    refreshRef[0].run();
                    status.setText("Usage entry for " + d.format(DATE_FMT) + " deleted.");
                    status.setStyle("-fx-text-fill: #27ae60;");
                }
            });
        });

        // ── Request technician ───────────────────────────────────
        Label reqTitle = new Label("Request a Technician");
        reqTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        reqTitle.setStyle("-fx-text-fill: #90caf9;");

        ComboBox<Technician> techPicker = new ComboBox<>();
        for (Technician t : technicians)
            if (t.isApproved()) techPicker.getItems().add(t);
        techPicker.setPromptText("Choose approved technician");
        techPicker.setCellFactory(lv -> techCell());
        techPicker.setButtonCell(techCell());

        TextField problemFld = new TextField(); problemFld.setPromptText("Describe the problem");
        Button    reqBtn     = new Button("Send Request");

        reqBtn.setOnAction(e ->
        {
            Technician t   = techPicker.getValue();
            String    prob = problemFld.getText().trim();
            if (t == null)      { status.setText("Select a technician."); return; }
            if (prob.isEmpty()) { status.setText("Describe the problem."); return; }
            String entry = "From " + u.getEmail() + " [" + LocalDate.now().format(DATE_FMT) + "]: " + prob;
            t.addUserRequest(entry);
            sysLog.recordAction(u.getEmail(), "Sent request to " + t.getEmail() + ": " + prob);
            problemFld.clear();
            status.setText("Request sent to " + t.getEmail() + ".");
            status.setStyle("-fx-text-fill: #27ae60;");
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e ->
        {
            authSvc.invalidateSession();
            sysLog.recordAction(u.getEmail(), "User logout");
            currentUser = null; showLoginScreen();
        });

        // ── Label helper ──────────────────────────────────────────
        java.util.function.Function<String, Label> lbl = txt -> {
            Label l = new Label(txt);
            l.setStyle("-fx-text-fill: #cdd8f0;");
            return l;
        };

        // ── Build form card ───────────────────────────────────────
        GridPane p = new GridPane();
        p.setPadding(new Insets(24));
        p.setVgap(10);
        p.setHgap(10);
        p.setStyle("-fx-background-color: rgba(10,22,40,0.78); -fx-background-radius: 10;");
        p.setMaxWidth(620);

        int row = 0;
        p.add(header,              0, row, 3, 1); row++;

        // Resource pool totals
        Label poolHeader = new Label("Resource Pool");
        poolHeader.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        poolHeader.setStyle("-fx-text-fill: #90caf9;");
        p.add(poolHeader,          0, row, 3, 1); row++;
        p.add(waterTotalLbl,       0, row, 3, 1); row++;
        p.add(elecTotalLbl,        0, row, 3, 1); row++;

        // User's own totals
        p.add(userWaterLbl,        0, row, 3, 1); row++;
        p.add(userElecLbl,         0, row, 3, 1); row++;
        p.add(new Separator(),     0, row, 3, 1); row++;

        // Log usage section
        p.add(logTitle,            0, row, 3, 1); row++;
        p.add(lbl.apply("Date:"),          0, row); p.add(datePicker,     1, row, 2, 1); row++;

        // Electricity row
        p.add(lbl.apply("Electricity Region:"), 0, row); p.add(elecRegionBox,  1, row, 2, 1); row++;
        p.add(lbl.apply("Electricity Usage:"),  0, row); p.add(elecFld,        1, row, 2, 1); row++;

        // Water row
        p.add(lbl.apply("Water Meter:"),   0, row); p.add(waterRegionBox, 1, row, 2, 1); row++;
        p.add(lbl.apply("Water Usage:"),   0, row); p.add(waterFld,       1, row, 2, 1); row++;

        p.add(submitBtn,           1, row); row++;
        p.add(new Separator(),     0, row, 3, 1); row++;

        // Delete section
        p.add(delTitle,            0, row, 3, 1); row++;
        p.add(lbl.apply("Date to delete:"), 0, row); p.add(deleteDatePicker, 1, row, 2, 1); row++;
        p.add(deleteUsageBtn,      1, row); row++;
        p.add(new Separator(),     0, row, 3, 1); row++;

        // Request technician section
        p.add(reqTitle,            0, row, 3, 1); row++;
        p.add(lbl.apply("Technician:"),    0, row); p.add(techPicker,  1, row, 2, 1); row++;
        p.add(lbl.apply("Problem:"),       0, row); p.add(problemFld,  1, row, 2, 1); row++;
        p.add(reqBtn,              1, row); row++;
        p.add(new Separator(),     0, row, 3, 1); row++;
        p.add(status,              0, row, 3, 1); row++;
        p.add(logoutBtn,           0, row);

        ScrollPane scroll = new ScrollPane(p);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        StackPane centred = new StackPane(scroll);
        centred.setAlignment(Pos.CENTER);

        applySceneWithBackground(centred, 1200, 800,"4");
    }


    // ════════════════════════════════════════════════════════════
    //  SCREEN 6 — TECHNICIAN DASHBOARD  (RBAC: Technician only)
    // ════════════════════════════════════════════════════════════
    private void showTechDashboard(Technician t)
    {
        mainStage.setTitle(APP_NAME + " — Technician Dashboard");

        Label header = new Label(APP_NAME + "  |  Technician: " + t.getName()
                + "  [" + t.getTechID() + "]");
        header.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        header.setStyle("-fx-text-fill: #ffffff;");

        Label statusLbl = new Label("Status: " + (t.isApproved() ? "✔ Approved" : "⏳ Pending"));
        statusLbl.setStyle("-fx-text-fill: #a5d6a7;");

        // ── View resources ───────────────────────────────────────
        Label resTitle = new Label("Resources");
        resTitle.setFont(Font.font(null, FontWeight.BOLD, 12));
        resTitle.setStyle("-fx-text-fill: #90caf9;");

        ListView<String> resView = new ListView<>();
        // Helper: load resources sorted by name (same format as User dashboard)
        Runnable loadResources = () -> {
            resView.getItems().clear();
            List<Resource> sorted = new ArrayList<>(resMgr.getAll());
            sorted.sort(Comparator.comparing(r -> r.getResourceName().toLowerCase()));
            for (Resource r : sorted)
                resView.getItems().add(String.format("[%s] %s — %.1f units",
                        r.getType(), r.getResourceName(), r.getUsage()));
        };
        loadResources.run();
        resView.setPrefHeight(140);

        TextField searchFld = new TextField(); searchFld.setPromptText("Search resources");
        Button    searchBtn = new Button("Search");
        Button    sortBtn   = new Button("Sort A-Z");
        sortBtn.setStyle("-fx-background-color: #2c7be5; -fx-text-fill: white;");

        searchBtn.setOnAction(e ->
        {
            resView.getItems().clear();
            List<Resource> results = resMgr.search(searchFld.getText().trim());
            for (Resource r : results)
                resView.getItems().add(String.format("[%s] %s — %.1f units",
                        r.getType(), r.getResourceName(), r.getUsage()));
        });

        sortBtn.setOnAction(e -> loadResources.run());

        // ── Update resource usage ────────────────────────────────
        Label updTitle = new Label("Update Resource Usage");
        updTitle.setFont(Font.font(null, FontWeight.BOLD, 12));
        updTitle.setStyle("-fx-text-fill: #90caf9;");

        ComboBox<Resource> resPicker = new ComboBox<>();
        resPicker.getItems().addAll(resMgr.getAll());
        resPicker.setCellFactory(lv -> resourceCell());
        resPicker.setButtonCell(resourceCell());
        TextField newUsageFld = new TextField(); newUsageFld.setPromptText("New usage value");
        Button    updateBtn   = new Button("Update Usage");
        Label     techStatus  = new Label();
        techStatus.setStyle("-fx-text-fill: #ffe082;");

        updateBtn.setOnAction(e ->
        {
            Resource sel = resPicker.getValue();
            String   val = newUsageFld.getText().trim();
            if (sel == null || val.isEmpty()) { techStatus.setText("Select a resource and enter a value."); return; }
            try
            {
                double newVal = Double.parseDouble(val);
                if (newVal < 0) { techStatus.setText("Value must be non-negative."); return; }
                sel.setUsage(newVal);
                sysLog.recordAction(t.getEmail(),
                        "Updated resource '" + sel.getResourceName() + "' usage to " + newVal);
                loadResources.run();
                newUsageFld.clear();
                techStatus.setText("Usage updated.");
                techStatus.setStyle("-fx-text-fill: #27ae60;");
            }
            catch (NumberFormatException ex) { techStatus.setText("Enter a valid number."); }
        });

        // ── User requests ────────────────────────────────────────
        Label reqTitle = new Label("User Requests (read-only)");
        reqTitle.setFont(Font.font(null, FontWeight.BOLD, 12));
        reqTitle.setStyle("-fx-text-fill: #90caf9;");

        ListView<String> reqView = new ListView<>();
        reqView.getItems().addAll(t.getUserRequests());
        reqView.setPrefHeight(130);
        Button refreshReqBtn = new Button("Refresh");
        refreshReqBtn.setOnAction(e -> reqView.getItems().setAll(t.getUserRequests()));

        // ── Work logs ────────────────────────────────────────────
        Label logTitle = new Label("My Work Logs");
        logTitle.setFont(Font.font(null, FontWeight.BOLD, 12));
        logTitle.setStyle("-fx-text-fill: #90caf9;");

        ListView<String> logView = new ListView<>();
        logView.getItems().addAll(t.getWorkLogs());
        logView.setPrefHeight(130);

        DatePicker logDate = new DatePicker(LocalDate.now());
        TextField  logFld  = new TextField(); logFld.setPromptText("Describe work done…"); logFld.setPrefWidth(200);
        Button     addLog  = new Button("Add Log");

        addLog.setOnAction(e ->
        {
            LocalDate d    = logDate.getValue();
            String    note = logFld.getText().trim();
            if (d == null)      { techStatus.setText("Select a date."); return; }
            if (d.isAfter(LocalDate.now()))
            { techStatus.setText("Cannot log a future date."); return; }
            if (note.isEmpty()) { techStatus.setText("Enter a log note."); return; }
            String entry = "[" + d.format(DATE_FMT) + "] " + note;
            t.addLog(entry);
            logView.getItems().add(entry);
            sysLog.recordAction(t.getEmail(), "Work log added: " + note);
            logFld.clear();
            techStatus.setText("Log added.");
            techStatus.setStyle("-fx-text-fill: #27ae60;");
        });

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e ->
        {
            authSvc.invalidateSession();
            sysLog.recordAction(t.getEmail(), "Technician logout");
            currentUser = null; showLoginScreen();
        });

        // ── Layout ───────────────────────────────────────────────
        GridPane p = new GridPane();
        p.setPadding(new Insets(20));
        p.setVgap(10);
        p.setHgap(10);
        p.setStyle("-fx-background-color: rgba(10,22,40,0.72); -fx-background-radius: 10;");
        p.setMaxWidth(860);

        int row = 0;
        p.add(header,     0, row, 4, 1); row++;
        p.add(statusLbl,  0, row, 4, 1); row++;
        p.add(new Separator(), 0, row, 4, 1); row++;

        p.add(resTitle, 0, row, 4, 1); row++;
        p.add(resView,  0, row, 4, 1); row++;
        p.add(new HBox(6, searchFld, searchBtn, sortBtn), 0, row, 4, 1); row++;

        p.add(updTitle, 0, row, 4, 1); row++;
        p.add(new HBox(6, new Label("Resource:"), resPicker,
                         new Label("New value:"), newUsageFld, updateBtn), 0, row, 4, 1); row++;

        p.add(new Separator(), 0, row, 4, 1); row++;
        p.add(reqTitle, 0, row, 4, 1); row++;
        p.add(reqView,  0, row, 4, 1); row++;
        p.add(refreshReqBtn, 0, row, 4, 1); row++;

        p.add(new Separator(), 0, row, 4, 1); row++;
        p.add(logTitle, 0, row, 4, 1); row++;
        p.add(logView,  0, row, 4, 1); row++;
        p.add(new HBox(6, logDate, logFld, addLog), 0, row, 4, 1); row++;

        p.add(new Separator(), 0, row, 4, 1); row++;
        p.add(techStatus, 0, row, 4, 1); row++;
        p.add(logoutBtn, 0, row);

        StackPane centred = new StackPane(p);
        centred.setAlignment(Pos.CENTER);

        applySceneWithBackground(centred, 1200, 800, "4");
    }


    // ════════════════════════════════════════════════════════════
    //  TABLE HELPERS
    // ════════════════════════════════════════════════════════════
    private static class UsageRecord
    {
        final String userName, date, water, electricity,technicianName;
        UsageRecord(String userName, String date,
                    String water, String electricity,String technicianName)
        {
            this.userName = userName; this.date = date;
            this.water = water; this.electricity = electricity;
            //this.usage = usage;
            this.technicianName = technicianName;
        }
    }

    private TableView<UsageRecord> buildUsageTable()
    {
        TableView<UsageRecord> t = new TableView<>();
        TableColumn<UsageRecord,String> c1 = new TableColumn<>("User");
        c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().userName)); c1.setPrefWidth(180);
        TableColumn<UsageRecord,String> c3 = new TableColumn<>("Date");
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date));     c3.setPrefWidth(120);
        TableColumn<UsageRecord,String> c4 = new TableColumn<>("Water (L)");
        c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().water));    c4.setPrefWidth(80);
        TableColumn<UsageRecord,String> c5 = new TableColumn<>("Elec (kWh)");
        c5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().electricity)); c5.setPrefWidth(80);
        //TableColumn<UsageRecord,String> c6 = new TableColumn<>("Total / Log");
        //c6.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().usage));    c6.setPrefWidth(160);
        TableColumn<UsageRecord,String> c7 = new TableColumn<>("Technician");
        c7.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().technicianName)); c7.setPrefWidth(160);
        t.getColumns().addAll(c1, c3, c4, c5, c7);
        return t;
    }

    private void populateUsageTable(TableView<UsageRecord> table,
                                    String vf, String uf, String tf,
                                    LocalDate today)
    {
        table.getItems().clear();
        for (NormalUser u : normalUsers)
        {
            double tw = 0, te = 0; int cnt = 0;
            String first = "", last = "";
            for (UsageEntry entry : u.getUsageEntries())
            {
                if (!matchesTimeFilter(entry.date, tf, today)) continue;
                tw += entry.water; te += entry.electricity; cnt++;
                String ds = entry.date.format(DATE_FMT);
                if (first.isEmpty()) first = ds;
                last = ds;
            }
            if (cnt == 0) continue;
            String range = cnt == 1 ? first : first + " – " + last + " (" + cnt + " days)";
            double tot   = uf.equals("Water") ? tw : uf.equals("Electricity") ? te : tw + te;

            String matched = technicians.stream()
                    .filter(t -> t.getUserRequests().stream()
                            .anyMatch(req -> req.startsWith("From " + u.getEmail())))
                    .map(t -> t.getName() + " [" + t.getTechID() + "]")
                    .findFirst().orElse("—");

            table.getItems().add(new UsageRecord(
                    u.getName(), range,
                    uf.equals("Electricity") ? "-" : String.format("%.1f", tw),
                    uf.equals("Water")       ? "-" : String.format("%.1f", te),
                    matched));
        }
    }

    @SuppressWarnings("unchecked")
    private ComboBox<User> buildUserPicker()
    {
        ComboBox<User> box = new ComboBox<>();
        ///box.getItems().addAll(normalUsers);
        
        
        box.getItems().addAll(technicians);
        
        box.setPromptText("Select Technician");
        box.setCellFactory(lv -> userCell());
        box.setButtonCell(userCell());
        return box;
    }

    private ListCell<User> userCell()
    {
        return new ListCell<User>()
        {
            @Override protected void updateItem(User item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : item.getName() + " (" + item.getRoleLabel() + ")");
            }
        };
    }

    private ListCell<Technician> techCell()
    {
        return new ListCell<Technician>()
        {
            @Override protected void updateItem(Technician item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : item.getName() + "  [" + item.getTechID() + "]");
            }
        };
    }

    private ListCell<Resource> resourceCell()
    {
        return new ListCell<Resource>()
        {
            @Override protected void updateItem(Resource item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : "[" + item.getType() + "] " + item.getResourceName());
            }
        };
    }

    private void refreshResList(ListView<String> lv)
    {
        lv.getItems().clear();
        List<Resource> sorted = new ArrayList<>(resMgr.getAll());
        sorted.sort(Comparator.comparing(r -> r.getResourceName().toLowerCase()));
        for (Resource r : sorted)
            lv.getItems().add(String.format("[%s] %s — %.1f units",
                    r.getType(), r.getResourceName(), r.getUsage()));
    }


    // ════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════════════════════════
    private boolean isValidEmail(String email)
    {
        if (email == null || email.isBlank()) return false;
        int at  = email.indexOf('@');
        if (at < 1) return false;
        int dot = email.indexOf('.', at);
        return dot > at + 1 && dot < email.length() - 1;
    }

    private String validatePassword(String pw)
    {
        if (pw == null || pw.length() < 8)  return "Password must be at least 8 characters.";
        if (!pw.chars().anyMatch(Character::isUpperCase))
            return "Password must contain at least one uppercase letter.";
        if (!pw.chars().anyMatch(Character::isDigit))
            return "Password must contain at least one digit.";
        if (!pw.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{}|;:',.<>?/`~".indexOf(c) >= 0))
            return "Password must contain at least one special character.";
        return null;
    }

    private User findUserByEmailAndRole(String role, String email)
    {
        switch (role)
        {
            case "Admin":
                return (adminUser != null && adminUser.getEmail().equals(email)) ? adminUser : null;
            case "Technician":
                return technicians.stream()
                        .filter(t -> t.getEmail().equals(email)).findFirst().orElse(null);
            default:
                return normalUsers.stream()
                        .filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
        }
    }

    private boolean matchesTimeFilter(LocalDate date, String filter, LocalDate today)
    {
        if (filter == null || "All Time".equals(filter)) return true;
        if (date == null) date = today;
        switch (filter)
        {
            case "Today":      return date.isEqual(today);
            case "This Week":
                LocalDate wStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                return !date.isBefore(wStart) && !date.isAfter(today);
            case "This Month":
                return date.getMonth() == today.getMonth()
                    && date.getYear()  == today.getYear();
            default:           return true;
        }
    }

    private LocalDate parseDateFromLog(String log, LocalDate fallback)
    {
        try
        {
            if (log != null && log.startsWith("["))
            {
                int end = log.indexOf(']');
                if (end > 1) return LocalDate.parse(log.substring(1, end), DATE_FMT);
            }
        }
        catch (Exception ignored) {}
        return fallback;
    }

    private void clearTemp() { tempEmail = ""; tempPassword = ""; tempRole = ""; tempName = ""; }


    // ════════════════════════════════════════════════════════════
    //  FILE PERSISTENCE
    // ════════════════════════════════════════════════════════════
    private void saveUsersToFile()
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE)))
        {
            if (adminUser != null)
                pw.println("ADMIN|" + adminUser.getEmail()
                        + "|" + adminUser.getRawHash()
                        + "|" + adminUser.getAdminID()
                        + "|" + adminUser.getName());

            for (NormalUser u : normalUsers)
                pw.println("USER|" + u.getEmail() + "|" + u.getRawHash() + "|" + u.getName());

            for (Technician t : technicians)
                pw.println("TECH|" + t.getEmail()
                        + "|" + t.getRawHash()
                        + "|" + t.getTechID()
                        + "|" + t.isApproved()
                        + "|" + t.getName());
        }
        catch (IOException ex)
        {
            System.err.println("Could not save users: " + ex.getMessage());
        }
    }

    private void loadUsersFromFile()
    {
        File f = new File(USERS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                switch (parts[0])
                {
                    case "ADMIN":
                        if (parts.length >= 5)
                            adminUser = new Administrator(parts[1], parts[2], parts[3], parts[4]);
                        else if (parts.length >= 4)
                            adminUser = new Administrator(parts[1], parts[2], parts[3], parts[1]);
                        break;
                    case "USER":
                        if (parts.length >= 4)
                            normalUsers.add(new NormalUser(parts[1], parts[2], parts[3]));
                        else if (parts.length >= 3)
                            normalUsers.add(new NormalUser(parts[1], parts[2], parts[1]));
                        break;
                    case "TECH":
                        if (parts.length >= 6)
                        {
                            Technician t = new Technician(parts[1], parts[2], parts[3], parts[5]);
                            if (Boolean.parseBoolean(parts[4])) t.approve();
                            technicians.add(t);
                        }
                        else if (parts.length >= 5)
                        {
                            Technician t = new Technician(parts[1], parts[2], parts[3], parts[1]);
                            if (Boolean.parseBoolean(parts[4])) t.approve();
                            technicians.add(t);
                        }
                        break;
                }
            }
        }
        catch (IOException ex)
        {
            System.err.println("Could not load users: " + ex.getMessage());
        }
    }


    // ════════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════════
    public static void main(String[] args) { launch(args); }
}