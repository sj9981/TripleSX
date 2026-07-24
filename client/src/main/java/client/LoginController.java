package client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        // temporary. login is successful if username is "admin"
        boolean loginSuccess = username.equals("admin") && password.equals("password");

        if (loginSuccess) {
            statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            statusLabel.setText("Success! (Mocked response)");
            // TODO: Route to MainTimeline screen
        } else {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Invalid credentials. Try admin / password.");
        }
    }
}