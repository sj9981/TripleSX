package client;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;

public class LoginController
{
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    public void handleLogin()
    {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty())
        {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
        statusLabel.setText("Logging in...");

        Task<JSONObject> loginTask = new Task<>() {
            @Override
            protected JSONObject call() throws Exception
            {
                return NetworkManager.getInstance().login(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            JSONObject response = loginTask.getValue();
            if (response == null) {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText("No response from server.");
                return;
            }

            boolean success = response.optBoolean("success", false);
            String message = response.optString("message", "Unknown response received from server.");

            if (success) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Login successful! Welcome, " + username);

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
                    Parent root = loader.load();

                    HomeController homeController = loader.getController();
                    homeController.setUsername(username);

                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("X Clone - Home");
                    stage.show();

                } catch (IOException ex) {
                    ex.printStackTrace();
                    statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                    statusLabel.setText("Error loading Home screen. Check console.");
                }
            }
            else
            {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(message);
            }
        });

        loginTask.setOnFailed(e -> {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Could not connect to the server. Please verify it is running.");
            if (loginTask.getException() != null) {
                loginTask.getException().printStackTrace();
            }
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleRegister()
    {
        try
        {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("X Clone - Sign Up");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            statusLabel.setText("Error loading registration page.");
        }
    }
}