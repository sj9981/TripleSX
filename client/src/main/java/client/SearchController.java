package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SearchController {

    @FXML
    private TextField searchField;

    @FXML
    private VBox usersContainer;

    @FXML
    private VBox tweetsContainer;

    @FXML
    public void initialize() {
        // Initial empty state
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        usersContainer.getChildren().clear();
        tweetsContainer.getChildren().clear();

        try {
            JSONObject response = NetworkManager.getInstance().search(query);

            if (response != null && response.optBoolean("success", false)) {
                JSONArray users = response.optJSONArray("users");
                if (users != null) {
                    for (int i = 0; i < users.length(); i++) {
                        addUserCard(users.getJSONObject(i));
                    }
                }

                JSONArray tweets = response.optJSONArray("tweets");
                if (tweets != null) {
                    for (int i = 0; i < tweets.length(); i++) {
                        addTweetCard(tweets.getJSONObject(i));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addUserCard(JSONObject userJson) {
        String username = userJson.optString("username", "");
        String displayName = userJson.optString("displayName", username);
        String bio = userJson.optString("bio", "");
        String avatarPath = userJson.optString("avatarPath", "");

        HBox card = new HBox(12);
        card.setStyle("-fx-padding: 10; -fx-background-color: #16181c; -fx-background-radius: 10; -fx-cursor: hand;");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle avatar = new Circle(20);
        Image avatarImg = resolveImage(avatarPath);
        if (avatarImg != null) {
            avatar.setFill(new ImagePattern(avatarImg));
        } else {
            avatar.setFill(Color.web("#333333"));
        }

        VBox textData = new VBox(2);
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label handleLabel = new Label("@" + username);
        handleLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 13px;");

        Label bioLabel = new Label(bio);
        bioLabel.setStyle("-fx-text-fill: #e7e9ea; -fx-font-size: 13px;");
        bioLabel.setWrapText(true);

        textData.getChildren().addAll(nameLabel, handleLabel, bioLabel);
        card.getChildren().addAll(avatar, textData);

        card.setOnMouseClicked(event -> navigateToProfile(username));

        usersContainer.getChildren().add(card);
    }

    private void addTweetCard(JSONObject tweetJson) {
        String author = tweetJson.optString("username", "Unknown");
        String text = tweetJson.optString("content", "");
        String createdAt = tweetJson.optString("created_at", "");
        String imagePath = tweetJson.optString("image_path", "");
        String avatarPath = tweetJson.optString("avatar_path", "");

        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 12; -fx-border-color: #2f3336; -fx-border-width: 0 0 1 0;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle avatar = new Circle(18);
        Image avatarImg = resolveImage(avatarPath);
        if (avatarImg != null) {
            avatar.setFill(new ImagePattern(avatarImg));
        } else {
            avatar.setFill(Color.web("#333333"));
        }

        Label nameLabel = new Label(author);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label handleLabel = new Label("@" + author.toLowerCase());
        handleLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 13px;");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);

        Label contentLabel = new Label(text);
        contentLabel.setStyle("-fx-text-fill: #e7e9ea; -fx-font-size: 14px;");
        contentLabel.setWrapText(true);

        card.getChildren().addAll(header, contentLabel);

        if (!imagePath.trim().isEmpty() && !"null".equalsIgnoreCase(imagePath)) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(300);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                }
            } catch (Exception ignored) {
            }
        }

        Label timeLabel = new Label(createdAt);
        timeLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 12px;");
        card.getChildren().add(timeLabel);

        tweetsContainer.getChildren().add(card);
    }

    private void navigateToProfile(String targetUsername) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();

            ProfileController profileController = loader.getController();
            profileController.initUserData(targetUsername);

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Image resolveImage(String path) {
        if (path == null || path.trim().isEmpty()) {
            return loadDefaultAvatar();
        }
        try {
            File file = new File(path);
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), false);
                if (!img.isError()) {
                    return img;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    return img;
                }
            }
        } catch (Exception ignored) {
        }
        return loadDefaultAvatar();
    }

    private Image loadDefaultAvatar() {
        try {
            InputStream stream = getClass().getResourceAsStream("/default-avatar.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @FXML
    private void handleGoToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            homeController.setUserInfo(SessionManager.getInstance().getUsername());

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Home");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToProfile() {
        navigateToProfile(SessionManager.getInstance().getUsername());
    }

    @FXML
    private void handleLogout() {
        try {
            NetworkManager.getInstance().disconnect();
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}