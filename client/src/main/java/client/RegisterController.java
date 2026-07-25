package client;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONObject;

public class RegisterController
{

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSignUp()
    {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty())
        {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("All fields are required!");
            return;
        }

        statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
        statusLabel.setText("Registering your account...");

        Task<JSONObject> registerTask = new Task<>()
        {
            @Override
            protected JSONObject call() throws Exception
            {
                return NetworkManager.getInstance().register(username, email, password);
            }
        };

        registerTask.setOnSucceeded(e -> {
            JSONObject response = registerTask.getValue();
            boolean success = response.optBoolean("success", false);
            String message = response.optString("message", "Registration failed.");

            if (success) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Account created! Redirecting to login...");

                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                    javafx.application.Platform.runLater(this::goToLogin);
                }).start();
            }
            else
            {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(message);
            }
        });

        registerTask.setOnFailed(e -> {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Server error. Please try again.");
        });

        new Thread(registerTask).start();
    }

    @FXML
    private void goToLogin()
    {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("X Clone - Login");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}