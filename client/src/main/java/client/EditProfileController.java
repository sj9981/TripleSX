package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import server.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditProfileController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextArea bioField;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Button removePhotoButton;

    private String previousScene = "/profile.fxml";
    private String currentUsername;


    private String currentAvatarPath;
    private String selectedImagePath;
    private boolean removeAvatar = false;
    private Image defaultAvatarImage;

    @FXML
    public void initialize() {
        Circle clip = new Circle(55, 55, 55);
        profileImageView.setClip(clip);

        try {
            defaultAvatarImage = new Image(
                    getClass().getResource("/default-avatar.png").toExternalForm()
            );
            profileImageView.setImage(defaultAvatarImage);
        } catch (Exception e) {
            System.out.println("Default avatar not found: /default-avatar.png");
        }

        if (removePhotoButton != null) {
            removePhotoButton.setDisable(true);
        }
    }

    public void setPreviousScene(String previousScene) {
        this.previousScene = previousScene;
    }

    public void setUsername(String username) {
        this.currentUsername = username;
        loadAvatarFromDatabase();
    }

    public void initUserData(String name, String username, String bio) {
        nameField.setText(name);
        usernameField.setText(username);
        bioField.setText(bio);
        this.currentUsername = username;
        loadAvatarFromDatabase();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        goToProfile(event, currentUsername);
    }

    @FXML
    private void handleChoosePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) profileImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            removeAvatar = false;

            profileImageView.setImage(new Image(selectedFile.toURI().toString()));

            if (removePhotoButton != null) {
                removePhotoButton.setDisable(false);
            }
        }
    }

    @FXML
    private void handleRemovePhoto(ActionEvent event) {
        selectedImagePath = null;
        removeAvatar = true;

        if (defaultAvatarImage != null) {
            profileImageView.setImage(defaultAvatarImage);
        } else {
            profileImageView.setImage(null);
        }

        if (removePhotoButton != null) {
            removePhotoButton.setDisable(true);
        }
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

        String avatarPathToSave = currentAvatarPath;

        if (removeAvatar) {
            avatarPathToSave = null;
        } else if (selectedImagePath != null && !selectedImagePath.trim().isEmpty()) {
            avatarPathToSave = selectedImagePath;
        }

        try {
            boolean success = updateProfileInDataSource(
                    currentUsername,
                    newName,
                    newUsername,
                    newBio,
                    avatarPathToSave
            );

            if (success) {
                currentUsername = newUsername;
                currentAvatarPath = avatarPathToSave;
                selectedImagePath = null;
                removeAvatar = false;

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

            Object controller = loader.getController();
            if (controller instanceof ProfileController) {
                ((ProfileController) controller).initUserData(usernameToLoad);
            }

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

    private boolean updateProfileInDataSource(String oldUsername,
                                              String newName,
                                              String newUsername,
                                              String newBio,
                                              String avatarPath) {
        String sql = "UPDATE users SET display_name = ?, username = ?, bio = ?, avatar_path = ? WHERE username = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setString(2, newUsername);
            ps.setString(3, newBio);

            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, avatarPath);
            }

            ps.setString(5, oldUsername);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadAvatarFromDatabase() {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return;
        }

        String sql = "SELECT avatar_path FROM users WHERE username = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, currentUsername);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentAvatarPath = rs.getString("avatar_path");

                    if (currentAvatarPath != null && !currentAvatarPath.trim().isEmpty()) {
                        File file = new File(currentAvatarPath);
                        if (file.exists()) {
                            profileImageView.setImage(new Image(file.toURI().toString()));
                            if (removePhotoButton != null) {
                                removePhotoButton.setDisable(false);
                            }
                        } else {
                            showDefaultAvatar();
                        }
                    } else {
                        showDefaultAvatar();
                    }
                } else {
                    showDefaultAvatar();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showDefaultAvatar();
        }
    }

    private void showDefaultAvatar() {
        currentAvatarPath = null;

        if (defaultAvatarImage != null) {
            profileImageView.setImage(defaultAvatarImage);
        } else {
            profileImageView.setImage(null);
        }

        if (removePhotoButton != null) {
            removePhotoButton.setDisable(true);
        }
    }

    private void showAlert(String title, String message) {
        Alert.AlertType type = title.equalsIgnoreCase("Error")
                ? Alert.AlertType.ERROR
                : Alert.AlertType.INFORMATION;

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}