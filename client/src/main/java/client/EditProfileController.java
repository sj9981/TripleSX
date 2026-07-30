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
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

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

    @FXML private ImageView bannerPreviewImageView;
    private String currentBannerPath;
    private String selectedBannerPath;
    private boolean removeBanner = false;

    @FXML
    private Button removeBannerButton;

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
        loadBannerFromDatabase();
    }

    public void initUserData(String name, String username, String bio) {
        nameField.setText(name);
        usernameField.setText(username);
        bioField.setText(bio);
        this.currentUsername = username;
        loadAvatarFromDatabase();
        loadBannerFromDatabase();
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

        String avatarPathToSave;
        if (removeAvatar) {
            avatarPathToSave = null;
        } else if (selectedImagePath != null && !selectedImagePath.trim().isEmpty()) {
            avatarPathToSave = selectedImagePath;
        } else {
            avatarPathToSave = currentAvatarPath;
        }

        String bannerPathToSave;
        if (removeBanner) {
            bannerPathToSave = null;
        } else if (selectedBannerPath != null && !selectedBannerPath.trim().isEmpty()) {
            bannerPathToSave = selectedBannerPath;
        } else {
            bannerPathToSave = currentBannerPath;
        }

        try {
            boolean success = updateProfileInDataSource(
                    currentUsername,
                    newName,
                    newUsername,
                    newBio,
                    avatarPathToSave,
                    bannerPathToSave
            );

            if (success) {
                currentUsername = newUsername;
                currentAvatarPath = avatarPathToSave;
                currentBannerPath = bannerPathToSave;
                selectedImagePath = null;
                selectedBannerPath = null;
                removeAvatar = false;
                removeBanner = false;

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
                                              String avatarPath,
                                              String bannerPath) {
        JSONObject response = NetworkManager.getInstance().updateProfile(
                oldUsername, newName, newUsername, newBio, avatarPath, bannerPath
        );
        return response != null && response.optBoolean("success", false);
    }

    private void loadAvatarFromDatabase() {
        if (currentUsername == null || currentUsername.trim().isEmpty()) return;
        try {
            JSONObject response = NetworkManager.getInstance().getUserProfile(currentUsername);
            if (response != null && response.optBoolean("success")) {
                JSONObject userObj = response.optJSONObject("user");
                currentAvatarPath = userObj.optString("avatarPath", null);

                Image avatarImg = resolveImage(currentAvatarPath);
                if (avatarImg != null) {
                    profileImageView.setImage(avatarImg);
                    if (removePhotoButton != null) removePhotoButton.setDisable(false);
                } else {
                    profileImageView.setImage(new Image(getClass().getResourceAsStream("/default-avatar.png")));
                    if (removePhotoButton != null) removePhotoButton.setDisable(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBannerFromDatabase() {
        try {
            JSONObject response = NetworkManager.getInstance().getUserProfile(currentUsername);
            if (response != null && response.optBoolean("success")) {
                JSONObject userObj = response.optJSONObject("user");
                currentBannerPath = userObj.optString("bannerPath", null);

                Image bannerImg = resolveImage(currentBannerPath);
                if (bannerImg != null) {
                    bannerPreviewImageView.setImage(bannerImg);
                    if (removeBannerButton != null) removeBannerButton.setDisable(false);
                } else {
                    try {
                        bannerPreviewImageView.setImage(new Image(getClass().getResourceAsStream("/default-banner.png")));
                    } catch (Exception e) {
                        bannerPreviewImageView.setImage(null);
                    }
                    if (removeBannerButton != null) removeBannerButton.setDisable(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    @FXML
    private void handleChooseBanner(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Banner Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());

        if (selectedFile != null) {
            selectedBannerPath = selectedFile.getAbsolutePath();
            removeBanner = false;
            bannerPreviewImageView.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void handleRemoveBanner(ActionEvent event) {
        selectedBannerPath = null;
        removeBanner = true;

        bannerPreviewImageView.setImage(null);

        if (removeBannerButton != null) {
            removeBannerButton.setDisable(true);
        }
    }

    private Image resolveImage(String path) {
        if (path == null || path.trim().isEmpty() || path.equalsIgnoreCase("null")) return null;

        try {
            File file = new File(path);
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), false);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        try {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            java.io.InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) return img;
            }
        } catch (Exception ignored) {}

        return null;
    }
}