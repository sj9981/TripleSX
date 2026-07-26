package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditProfileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextArea bioField;

    private String previousScene = "/profile.fxml";
    private String currentUsername;

    public void setPreviousScene(String previousScene) {
        this.previousScene = previousScene;
    }

    public void setUsername(String username) {
        this.currentUsername = username;
    }

    public void initUserData(String name, String username, String bio) {
        nameField.setText(name);
        usernameField.setText(username);
        bioField.setText(bio);
        this.currentUsername = username;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        goToProfile(event, currentUsername);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String newName = nameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String newBio = bioField.getText().trim();

        if (newName.isEmpty() || newUsername.isEmpty()) {
            showAlert("Error", "Name and Username cannot be empty.");
            return;
        }

        try {
            boolean success = updateProfileInDataSource(currentUsername, newName, newUsername, newBio);

            if (success) {
                currentUsername = newUsername;
                showAlert("Success", "Profile updated successfully.");
                goToProfile(event, currentUsername);
            } else {
                showAlert("Error", "Profile update failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Something went wrong while saving profile.");
        }
    }

    private void goToProfile(ActionEvent event, String usernameToLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(previousScene));
            Parent root = loader.load();

            ProfileController controller = loader.getController();
            controller.initUserData(usernameToLoad);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Scene scene = new Scene(root, w, h);
            stage.setScene(scene);
            stage.setTitle("X Clone - Profile");
            stage.setWidth(w);
            stage.setHeight(h);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not return to profile page.");
        }
    }

    private boolean updateProfileInDataSource(String oldUsername, String newName, String newUsername, String newBio) {


        System.out.println("Updating profile...");
        System.out.println("Old Username: " + oldUsername);
        System.out.println("New Name: " + newName);
        System.out.println("New Username: " + newUsername);
        System.out.println("New Bio: " + newBio);

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}