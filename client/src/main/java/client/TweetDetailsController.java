package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

public class TweetDetailsController {

    @FXML private VBox mainTweetContainer;
    @FXML private TextArea replyTextArea;
    @FXML private VBox repliesContainer;

    private int currentTweetId;

    public void setTweetId(int tweetId) {
        this.currentTweetId = tweetId;
        loadTweetDetails();
    }

    private void loadTweetDetails() {
        mainTweetContainer.getChildren().clear();
        repliesContainer.getChildren().clear();

        JSONObject response = NetworkManager.getInstance().getTweetDetails(currentTweetId);
        if (response != null && response.optBoolean("success", false)) {
            JSONObject mainTweet = response.optJSONObject("tweet");
            if (mainTweet != null) {
                renderMainTweet(mainTweet);
            }

            JSONArray replies = response.optJSONArray("replies");
            if (replies != null) {
                for (int i = 0; i < replies.length(); i++) {
                    renderReplyCard(replies.getJSONObject(i));
                }
            }
        }
    }

    private void renderMainTweet(JSONObject tweetObj) {
        String author = tweetObj.optString("username", "Unknown");
        String displayName = tweetObj.optString("display_name", author);
        String content = tweetObj.optString("content", "");
        String createdAt = tweetObj.optString("created_at", "");
        String imagePath = tweetObj.optString("image_path", "");
        String avatarPath = tweetObj.optString("avatar_path", "");
        int likeCount = tweetObj.optInt("like_count", 0);
        boolean isLiked = tweetObj.optBoolean("is_liked", false);
        int rtCount = tweetObj.optInt("retweet_count", 0);
        boolean isRetweeted = tweetObj.optBoolean("is_retweeted", false);

        VBox card = new VBox(12);

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand;");

        Circle avatar = new Circle(22);
        Image avatarImg = resolveImage(avatarPath);
        if (avatarImg != null) avatar.setFill(new ImagePattern(avatarImg));
        else avatar.setFill(Color.web("#333333"));

        VBox names = new VBox(2);
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label handleLabel = new Label("@" + author.toLowerCase());
        handleLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 14px;");
        names.getChildren().addAll(nameLabel, handleLabel);

        header.getChildren().addAll(avatar, names);
        header.setOnMouseClicked(event -> {
            event.consume();
            navigateToProfile(author);
        });

        Label contentLabel = new Label(content);
        contentLabel.setStyle("-fx-text-fill: #e7e9ea; -fx-font-size: 18px;");
        contentLabel.setWrapText(true);

        card.getChildren().addAll(header, contentLabel);

        if (!imagePath.trim().isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imgView = new ImageView(new Image(file.toURI().toString()));
                    imgView.setFitWidth(450);
                    imgView.setPreserveRatio(true);
                    card.getChildren().add(imgView);
                }
            } catch (Exception ignored) {}
        }

        Label timeLabel = new Label(createdAt);
        timeLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 14px;");
        card.getChildren().add(timeLabel);

        Button likeBtn = new Button();
        final int[] lCount = {likeCount};
        final boolean[] lLiked = {isLiked};
        updateLikeBtn(likeBtn, lLiked[0], lCount[0]);

        likeBtn.setOnAction(e -> {
            if (lLiked[0]) {
                if (NetworkManager.getInstance().unlikeTweet(currentTweetId).optBoolean("success")) {
                    lLiked[0] = false;
                    lCount[0]--;
                    updateLikeBtn(likeBtn, lLiked[0], lCount[0]);
                }
            } else {
                if (NetworkManager.getInstance().likeTweet(currentTweetId).optBoolean("success")) {
                    lLiked[0] = true;
                    lCount[0]++;
                    updateLikeBtn(likeBtn, lLiked[0], lCount[0]);
                }
            }
        });
        //retweet button logic
        Button rtBtn = new Button();
        final int[] rCount = {rtCount};
        final boolean[] rRetweeted = {isRetweeted};
        updateRtBtnUI(rtBtn, rRetweeted[0], rCount[0]);

        rtBtn.setOnAction(e -> {
            if (rRetweeted[0]) {
                if (NetworkManager.getInstance().unretweet(currentTweetId).optBoolean("success")) {
                    rRetweeted[0] = false;
                    rCount[0]--;
                    updateRtBtnUI(rtBtn, rRetweeted[0], rCount[0]);
                }
            } else {
                if (NetworkManager.getInstance().retweet(currentTweetId).optBoolean("success")) {
                    rRetweeted[0] = true;
                    rCount[0]++;
                    updateRtBtnUI(rtBtn, rRetweeted[0], rCount[0]);
                }
            }
        });

        HBox actions = new HBox(20, likeBtn, rtBtn);
        card.getChildren().add(actions);
        mainTweetContainer.getChildren().add(card);
    }

    private void updateRtBtnUI(Button btn, boolean isRetweeted, int count) {
        btn.setText("🔄 " + count);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " +
                (isRetweeted ? "#00ba7c" : "#71767b") + ";");
    }

    private void renderReplyCard(JSONObject tweetObj) {
        int replyId = tweetObj.optInt("tweet_id");
        String author = tweetObj.optString("username", "Unknown");
        String content = tweetObj.optString("content", "");
        String createdAt = tweetObj.optString("created_at", "");
        String avatarPath = tweetObj.optString("avatar_path", "");

        VBox card = new VBox(8);
        card.setStyle("-fx-padding: 12; -fx-border-color: #2f3336; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand;");

        Circle avatar = new Circle(16);
        Image avatarImg = resolveImage(avatarPath);
        if (avatarImg != null) avatar.setFill(new ImagePattern(avatarImg));
        else avatar.setFill(Color.web("#333333"));

        Label nameLabel = new Label(author);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label handleLabel = new Label("@" + author.toLowerCase());
        handleLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 13px;");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);
        header.setOnMouseClicked(event -> {
            event.consume();
            navigateToProfile(author);
        });

        Label contentLabel = new Label(content);
        contentLabel.setStyle("-fx-text-fill: #e7e9ea; -fx-font-size: 14px;");
        contentLabel.setWrapText(true);

        Label timeLabel = new Label(createdAt);
        timeLabel.setStyle("-fx-text-fill: #71767b; -fx-font-size: 12px;");

        card.getChildren().addAll(header, contentLabel, timeLabel);
        card.setOnMouseClicked(e -> setTweetId(replyId));

        repliesContainer.getChildren().add(card);
    }

    private void navigateToProfile(String targetUsername) {
        if (targetUsername == null || targetUsername.trim().isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();

            ProfileController profileController = loader.getController();
            profileController.initUserData(targetUsername);

            Stage stage = (Stage) mainTweetContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePostReply() {
        String replyText = replyTextArea.getText().trim();
        if (replyText.isEmpty()) return;

        String currentUser = SessionManager.getInstance().getUsername();
        JSONObject response = NetworkManager.getInstance().createTweet(currentUser, replyText, "", currentTweetId);

        if (response != null && response.optBoolean("success", false)) {
            replyTextArea.clear();
            loadTweetDetails();
        }
    }

    private void updateLikeBtn(Button btn, boolean isLiked, int count) {
        btn.setText((isLiked ? "❤ " : "♡ ") + count);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (isLiked ? "#f4212e" : "#71767b") + "; -fx-cursor: hand; -fx-font-size: 14px;");
    }

    private Image resolveImage(String path) {
        if (path == null || path.trim().isEmpty()) return loadDefaultAvatar();
        try {
            File file = new File(path);
            if (file.exists()) return new Image(file.toURI().toString());
        } catch (Exception ignored) {}
        return loadDefaultAvatar();
    }

    private Image loadDefaultAvatar() {
        try {
            InputStream stream = getClass().getResourceAsStream("/default-avatar.png");
            if (stream != null) return new Image(stream);
        } catch (Exception ignored) {}
        return null;
    }

    @FXML
    private void handleBack() { handleGoToHome(); }

    @FXML
    private void handleGoToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            HomeController homeController = loader.getController();
            homeController.setUserInfo(SessionManager.getInstance().getUsername());
            Stage stage = (Stage) mainTweetContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleGoToSearch() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/search.fxml"));
            Stage stage = (Stage) mainTweetContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleGoToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.initUserData(SessionManager.getInstance().getUsername());
            Stage stage = (Stage) mainTweetContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        NetworkManager.getInstance().disconnect();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) mainTweetContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}