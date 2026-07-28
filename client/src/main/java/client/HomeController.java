package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HomeController {
    private static HomeController instance;

    @FXML
    private Circle userAvatarCircle;

    @FXML
    private TextArea tweetTextArea;

    @FXML
    private VBox feedContainer;

    private String username;
    private String selectedImagePath = "";

    private final Map<String, Image> avatarCache = new HashMap<>();

    public HomeController() {
        instance = this;
    }

    public static HomeController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
    }

    public void setUsername(String username) {
        this.username = username;
        updateUserAvatar();
        loadTweetsFromServer();
    }

    public void setUserInfo(String username) {
        this.username = username;
        System.out.println("Logged in user set to: " + username);
        updateUserAvatar();
        loadTweetsFromServer();
    }

    @FXML
    private void handlePostTweet() {
        String tweetText = tweetTextArea.getText().trim();
        String currentUser = SessionManager.getInstance().getUsername();

        if (currentUser == null || currentUser.trim().isEmpty()) {
            System.err.println("Username is null. Cannot post tweet.");
            return;
        }

        if (tweetText.isEmpty() && selectedImagePath.isEmpty()) {
            System.out.println("Nothing to post.");
            return;
        }

        JSONObject response = NetworkManager.getInstance()
                .createTweet(currentUser, tweetText, selectedImagePath);

        if (response != null && response.optBoolean("success", false)) {
            tweetTextArea.clear();
            selectedImagePath = "";
            loadTweetsFromServer();
        } else {
            System.err.println("Failed to post tweet: " +
                    (response != null ? response.optString("message") : "null response"));
        }
    }

    private void loadTweetsFromServer() {
        String currentUser = SessionManager.getInstance().getUsername();
        if (currentUser == null || currentUser.trim().isEmpty()) {
            System.out.println("Username not set yet, skipping tweet load.");
            return;
        }

        try {
            JSONObject response = NetworkManager.getInstance().getFeedTweets();

            if (response != null && response.optBoolean("success", false)) {
                feedContainer.getChildren().clear();

                JSONArray tweets = response.optJSONArray("tweets");
                if (tweets == null) {
                    return;
                }

                for (int i = 0; i < tweets.length(); i++) {
                    JSONObject tweetObj = tweets.getJSONObject(i);
                    addTweetToFeed(tweetObj, false);
                }
            } else {
                System.err.println("Failed to load tweets: " +
                        (response != null ? response.optString("message") : "null response"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTweetToFeed(JSONObject tweetJson) {
        addTweetToFeed(tweetJson, true);
    }

    public void addTweetToFeed(JSONObject tweetJson, boolean prepend) {
        String user = tweetJson.optString("username", tweetJson.optString("author", "Unknown"));
        String text = tweetJson.optString("content", "");
        String createdAt = tweetJson.optString("created_at", "");
        String imagePath = tweetJson.optString("image_path", tweetJson.optString("imagePath", ""));
        int tweetId = tweetJson.optInt("tweet_id", -1);

        int likeCount = tweetJson.optInt("like_count", 0);
        boolean isLiked = tweetJson.optBoolean("is_liked", false);

        VBox card = new VBox(8);
        card.getStyleClass().add("tweet-card");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle avatar = new Circle(18);
        Image avatarImg = getUserAvatar(user);
        if (avatarImg != null) {
            avatar.setFill(new ImagePattern(avatarImg));
        } else {
            avatar.setFill(Color.web("#333333"));
        }
        avatar.setStroke(Color.web("#2f3336"));
        avatar.setStrokeWidth(1.0);

        Label nameLabel = new Label(user);
        nameLabel.getStyleClass().add("username-label");
        Label handleLabel = new Label("@" + user.toLowerCase());
        handleLabel.getStyleClass().add("handle-label");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);

        String currentUser = SessionManager.getInstance().getUsername();
        if (user.equalsIgnoreCase(currentUser) && tweetId != -1) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().add("delete-button");
            deleteBtn.setOnAction(event -> confirmAndDelete(tweetId, card));
            header.getChildren().addAll(spacer, deleteBtn);
        }

        Label contentLabel = new Label(text);
        contentLabel.getStyleClass().add("content-label");
        contentLabel.setWrapText(true);

        card.getChildren().addAll(header, contentLabel);

        if (imagePath != null && !imagePath.trim().isEmpty() && !"null".equalsIgnoreCase(imagePath)) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
                    imageView.setFitWidth(400);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                }
            } catch (Exception ignored) {}
        }

        Button likeBtn = new Button();
        updateLikeButtonUI(likeBtn, isLiked, likeCount);

        likeBtn.setOnAction(e -> {
            if (likeBtn.getText().contains("♡")) {
                JSONObject res = NetworkManager.getInstance().likeTweet(tweetId);
                if (res.optBoolean("success")) {
                    int currentNum = Integer.parseInt(likeBtn.getText().replaceAll("[^0-9]", ""));
                    updateLikeButtonUI(likeBtn, true, currentNum + 1);
                }
            } else {
                JSONObject res = NetworkManager.getInstance().unlikeTweet(tweetId);
                if (res.optBoolean("success")) {
                    int currentNum = Integer.parseInt(likeBtn.getText().replaceAll("[^0-9]", ""));
                    updateLikeButtonUI(likeBtn, false, currentNum - 1);
                }
            }
        });

        HBox actionsBar = new HBox(likeBtn);
        actionsBar.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
        card.getChildren().add(actionsBar);

        Label timeLabel = new Label(createdAt);
        timeLabel.getStyleClass().add("time-label");
        card.getChildren().add(timeLabel);

        if (prepend) {
            feedContainer.getChildren().add(0, card);
        } else {
            feedContainer.getChildren().add(card);
        }
    }

    private void updateLikeButtonUI(Button btn, boolean isLiked, int count) {
        if (isLiked) {
            btn.setText("❤ " + count);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f4212e; -fx-cursor: hand; -fx-font-size: 14px;");
        } else {
            btn.setText("♡ " + count);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-cursor: hand; -fx-font-size: 14px;");
        }
    }

    private void confirmAndDelete(int tweetId, VBox card) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        VBox container = new VBox(20);
        container.getStyleClass().add("delete-modal-pane");
        container.setPrefWidth(320);

        Label title = new Label("Delete post?");
        title.getStyleClass().add("delete-modal-title");

        Label body = new Label("This can’t be undone and it will be removed from your profile, the timeline of any accounts that follow you, and from search results.");
        body.getStyleClass().add("delete-modal-body");
        body.setWrapText(true);

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("confirm-delete-button");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> {
            JSONObject res = NetworkManager.getInstance().deleteTweet(tweetId);
            if (res.optBoolean("success")) {
                feedContainer.getChildren().remove(card);
            }
            modal.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-delete-button");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> modal.close());

        container.getChildren().addAll(title, body, deleteBtn, cancelBtn);

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        modal.setScene(scene);

        Window owner = feedContainer.getScene().getWindow();
        modal.setX(owner.getX() + (owner.getWidth() - 320) / 2);
        modal.setY(owner.getY() + (owner.getHeight() - 400) / 2);

        modal.show();
    }

    private Image getUserAvatar(String user) {
        if (avatarCache.containsKey(user)) {
            return avatarCache.get(user);
        }

        try {
            JSONObject response = NetworkManager.getInstance().getUserProfile(user);

            if (response != null && response.optBoolean("success", false)) {
                JSONObject userObj = response.getJSONObject("user");
                String avatarPath = userObj.optString("avatarPath", "");

                Image image = resolveImage(avatarPath);
                if (image != null) {
                    avatarCache.put(user, image);
                    return image;
                }
            }

        } catch (Exception e) {
            System.err.println("Could not load avatar for user: " + user);
            e.printStackTrace();
        }

        Image defaultImg = loadDefaultAvatar();
        if (defaultImg != null) {
            avatarCache.put(user, defaultImg);
        }
        return defaultImg;
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
        } catch (Exception ignored) {}

        try {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    return img;
                }
            }
        } catch (Exception ignored) {}

        try {
            String fileNameOnly = new File(path).getName();
            InputStream stream = getClass().getResourceAsStream("/" + fileNameOnly);
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    return img;
                }
            }
        } catch (Exception ignored) {}

        return loadDefaultAvatar();
    }

    private Image loadDefaultAvatar() {
        try {
            InputStream stream = getClass().getResourceAsStream("/default-avatar.png");
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            System.err.println("Default avatar resource not found!");
        }
        return null;
    }

    @FXML
    private void handleLogout() {
        try {
            NetworkManager.getInstance().disconnect();

            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();

            ProfileController profileController = loader.getController();
            profileController.initUserData(SessionManager.getInstance().getUsername());

            Stage stage = (Stage) feedContainer.getScene().getWindow();

            double width = feedContainer.getScene().getWidth();
            double height = feedContainer.getScene().getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Profile");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(feedContainer.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            System.out.println("Selected image: " + selectedImagePath);
        }
    }

    @FXML
    private void handleGoToSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/search.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) feedContainer.getScene().getWindow();
            double width = feedContainer.getScene().getWidth();
            double height = feedContainer.getScene().getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Search");
            stage.show();
        } catch (IOException e) {
            System.err.println("Could not load Search page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void updateUserAvatar() {
        String currentUser = SessionManager.getInstance().getUsername();
        if (currentUser != null && userAvatarCircle != null) {
            Image avatarImg = getUserAvatar(currentUser);
            if (avatarImg != null) {
                userAvatarCircle.setFill(new javafx.scene.paint.ImagePattern(avatarImg));
            } else {
                userAvatarCircle.setFill(Color.web("#333333"));
            }
        }
    }
}