package client;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;
    @FXML private Label statusLabel;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisibleField;

    private boolean passwordVisible = false;

    private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    @FXML
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();

        String password = passwordVisible ? passwordVisibleField.getText() : passwordField.getText();
        String confirmPassword = passwordVisible ? confirmPasswordVisibleField.getText() : confirmPasswordField.getText();

        //Basic empty check
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("All fields are required!");
            return;
        }

        // Match check
        if (!password.equals(confirmPassword)) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Passwords do not match!");
            return;
        }

        // Strength validation check
        if (!isValidPassword(password)) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Password must be at least 8 characters long, contain a number, an uppercase letter, and a special character.");
            return;
        }

        statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
        statusLabel.setText("Registering your account...");

        Task<JSONObject> registerTask = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                return NetworkManager.getInstance().register(username, email, password);
            }
        };

        registerTask.setOnSucceeded(e -> {
            JSONObject response = registerTask.getValue();
            boolean success = response.optBoolean("success", false);
            String message = response.optString("message", "Registration failed.");

            if (success) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Success! Redirecting to login...");

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    Platform.runLater(this::goToLogin);
                }).start();
            } else {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(message);
            }
        });

        registerTask.setOnFailed(e -> {
            if (registerTask.getException() != null) {
                registerTask.getException().printStackTrace();
            }
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Server error. Try again later.");
        });

        new Thread(registerTask).start();
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            confirmPasswordVisibleField.setText(confirmPasswordField.getText());
            confirmPasswordVisibleField.setVisible(true);
            confirmPasswordVisibleField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);

            togglePasswordButton.setText("🙈");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);

            confirmPasswordField.setText(confirmPasswordVisibleField.getText());
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordVisibleField.setVisible(false);
            confirmPasswordVisibleField.setManaged(false);

            togglePasswordButton.setText("🙉");
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("X Clone - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin(MouseEvent event) {
        goToLogin();
    }
}