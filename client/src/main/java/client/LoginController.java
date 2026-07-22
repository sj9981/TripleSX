package client;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        statusLabel.setTextFill(javafx.scene.paint.Color.BLUE);
        statusLabel.setText("Logging in...");

        Task<JSONObject> loginTask = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                NetworkManager.getInstance().connect();

                JSONObject request = new JSONObject();
                request.put("action", "login");
                request.put("username", username);
                request.put("password", password);

                String rawResponse = NetworkManager.getInstance().sendRequest(request.toString());
                return new JSONObject(rawResponse);
            }
        };

        loginTask.setOnSucceeded(e -> {
            JSONObject response = loginTask.getValue();
            boolean success = response.optBoolean("success", false);
            String message = response.optString("message", "Unknown response received from server.");

            if (success) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Login successful! Welcome, " + username);
            } else {
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(message);
            }
        });

        loginTask.setOnFailed(e -> {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Could not connect to the server. Please verify it is running.");
        });

        new Thread(loginTask).start();
    }
}