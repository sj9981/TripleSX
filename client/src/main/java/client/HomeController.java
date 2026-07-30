package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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

import javafx.scene.control.Hyperlink;

public class HomeController {
    private static HomeController instance;

    @FXML
    private TextArea tweetTextArea;

    @FXML
    private VBox feedContainer;

    @FXML
    private Circle userAvatarCircle;

    @FXML
    private HBox imagePreviewContainer;

    @FXML
    private Label charCountLabel;

    @FXML
    private Button postButton;

    private String username;
    private final java.util.List<String> selectedImagePaths = new java.util.ArrayList<>();

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

        if (tweetTextArea != null && charCountLabel != null && postButton != null) {
            tweetTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    charCountLabel.setText("280");
                    postButton.setDisable(false);
                    return;
                }
                int length = newValue.length();
                int remaining = 280 - length;
                charCountLabel.setText(String.valueOf(remaining));

                if (remaining < 0) {
                    charCountLabel.setStyle("-fx-text-fill: #f4212e; -fx-font-weight: bold;");
                    postButton.setDisable(true);
                } else if (remaining <= 20) {
                    charCountLabel.setStyle("-fx-text-fill: #ffd400; -fx-font-weight: bold;");
                    postButton.setDisable(false);
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #71767b; -fx-font-weight: normal;");
                    postButton.setDisable(false);
                }
            });
        }
    }

    public void setUsername(String username) {
        this.username = username;
        updateUserAvatar();
        loadTweetsFromServer();
    }

    public void setUserInfo(String username) {
        this.username = username;
        updateUserAvatar();
        loadTweetsFromServer();
    }

    private void updateUserAvatar() {
        String currentUser = SessionManager.getInstance().getUsername();
        if (currentUser != null && userAvatarCircle != null) {
            Image avatarImg = getUserAvatar(currentUser);
            if (avatarImg != null) {
                userAvatarCircle.setFill(new ImagePattern(avatarImg));
            } else {
                userAvatarCircle.setFill(Color.web("#333333"));
            }
        }
    }

    @FXML
    private void handlePostTweet()
    {
        String tweetText = tweetTextArea.getText().trim();
        String currentUser = SessionManager.getInstance().getUsername();

        if (currentUser == null || currentUser.trim().isEmpty()) {
            System.err.println("Username is null. Cannot post tweet.");
            return;
        }

        if (tweetText.isEmpty() && selectedImagePaths.isEmpty()) {
            System.out.println("Nothing to post.");
            return;
        }

        if (tweetText.length() > 280) {
            System.err.println("Tweet exceeds 280 characters.");
            return;
        }

        JSONObject response = NetworkManager.getInstance()
                .createTweet(currentUser, tweetText, selectedImagePaths, -1);

        if (response != null && response.optBoolean("success", false)) {
            tweetTextArea.clear();
            selectedImagePaths.clear();
            updateImagePreviews();
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
        int cardUiId = tweetJson.optInt("tweet_id", -1);
        int originalContentId = tweetJson.optInt("original_tweet_id", cardUiId);
        String authorUsername = tweetJson.optString("username", "Unknown");
        String displayName = tweetJson.optString("display_name", authorUsername);
        String text = tweetJson.optString("content", "");
        String createdAt = tweetJson.optString("created_at", "");
        String imagePath = tweetJson.optString("image_path", "");
        String avatarPath = tweetJson.optString("avatar_path", "");

        boolean isRetweet = tweetJson.optBoolean("is_retweet", false);
        String retweetedBy = tweetJson.optString("retweeted_by", "");

        int rtCount = tweetJson.optInt("retweet_count", 0);
        boolean isRetweetedByMe = tweetJson.optBoolean("is_retweeted", false);

        int likeCount = tweetJson.optInt("like_count", 0);
        boolean isLikedByMe = tweetJson.optBoolean("is_liked", false);
        int replyCount = tweetJson.optInt("reply_count", 0);

        VBox card = new VBox(8);
        card.setUserData(cardUiId);
        card.getStyleClass().add("tweet-card");
        card.setStyle("-fx-cursor: hand;");

        if (isRetweet) {
            Label rtHeader = new Label("🔄 " + retweetedBy + " Retweeted");
            rtHeader.setStyle("-fx-text-fill: #71767b; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 0 0 35;");
            card.getChildren().add(rtHeader);
        }

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle avatar = new Circle(18);
        Image avatarImg = resolveImage(avatarPath);
        if (avatarImg != null) avatar.setFill(new ImagePattern(avatarImg));
        else avatar.setFill(Color.web("#333333"));

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("username-label");
        Label handleLabel = new Label("@" + authorUsername.toLowerCase());
        handleLabel.getStyleClass().add("handle-label");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);
        header.setOnMouseClicked(e -> { e.consume(); navigateToProfile(authorUsername); });

        String currentUser = SessionManager.getInstance().getUsername();
        if (authorUsername.equalsIgnoreCase(currentUser) || retweetedBy.equalsIgnoreCase(currentUser)) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().add("delete-button");
            deleteBtn.setOnAction(event -> {
                event.consume();
                if (isRetweet && retweetedBy.equalsIgnoreCase(currentUser)) {
                    NetworkManager.getInstance().unretweet(originalContentId);
                } else {
                    confirmAndDelete(cardUiId, card);
                }
            });
            header.getChildren().addAll(spacer, deleteBtn);
        }

        javafx.scene.text.TextFlow contentFlow = HashtagUtils.parseTweetContent(text, 15, "#e7e9ea");
        card.getChildren().addAll(header, contentFlow);

        JSONArray imagePaths = tweetJson.optJSONArray("image_paths");
        if (imagePaths != null && imagePaths.length() > 0) {
            HBox imagesLayout = new HBox(8);
            imagesLayout.setStyle("-fx-padding: 5 0 5 0;");
            for (int j = 0; j < imagePaths.length(); j++) {
                String path = imagePaths.getString(j);
                if (path != null && !path.trim().isEmpty() && !"null".equalsIgnoreCase(path)) {
                    try {
                        File imgFile = new File(path);
                        if (imgFile.exists()) {
                            ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
                            if (imagePaths.length() == 1) {
                                iv.setFitWidth(400);
                            } else {
                                iv.setFitWidth(200); // Scale down when displaying side-by-side
                            }
                            iv.setPreserveRatio(true);
                            imagesLayout.getChildren().add(iv);
                        }
                    } catch (Exception ignored) {}
                }
            }
            card.getChildren().add(imagesLayout);
        }
        Button likeBtn = new Button();
        final int[] localLikeCount = {likeCount};
        final boolean[] localIsLiked = {isLikedByMe};

        updateLikeButtonUI(likeBtn, localIsLiked[0], localLikeCount[0]);

        likeBtn.setOnAction(e -> {
            e.consume();
            if (localIsLiked[0]) {
                if (NetworkManager.getInstance().unlikeTweet(originalContentId).optBoolean("success")) {
                    localIsLiked[0] = false;
                    localLikeCount[0]--;
                    updateLikeButtonUI(likeBtn, localIsLiked[0], localLikeCount[0]);
                }
            } else {
                if (NetworkManager.getInstance().likeTweet(originalContentId).optBoolean("success")) {
                    localIsLiked[0] = true;
                    localLikeCount[0]++;
                    updateLikeButtonUI(likeBtn, localIsLiked[0], localLikeCount[0]);
                }
            }
        });

        Button retweetBtn = new Button();
        final int[] localRtCount = {rtCount};
        final boolean[] localIsRetweeted = {isRetweetedByMe};

        updateRetweetButtonUI(retweetBtn, localIsRetweeted[0], localRtCount[0]);

        retweetBtn.setOnAction(e -> {
            e.consume();
            if (localIsRetweeted[0]) {
                if (NetworkManager.getInstance().unretweet(originalContentId).optBoolean("success")) {
                    localIsRetweeted[0] = false;
                    localRtCount[0]--;
                    updateRetweetButtonUI(retweetBtn, localIsRetweeted[0], localRtCount[0]);
                }
            } else {
                if (NetworkManager.getInstance().retweet(originalContentId).optBoolean("success")) {
                    localIsRetweeted[0] = true;
                    localRtCount[0]++;
                    updateRetweetButtonUI(retweetBtn, localIsRetweeted[0], localRtCount[0]);
                }
            }
        });

        Button replyBtn = new Button("💬 " + replyCount);
        replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-cursor: hand;");
        replyBtn.setOnAction(e -> { e.consume(); openTweetDetails(originalContentId); });

        HBox actionsBar = new HBox(20, likeBtn, replyBtn, retweetBtn);
        actionsBar.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
        card.getChildren().add(actionsBar);

        Label timeLabel = new Label(createdAt);
        timeLabel.getStyleClass().add("time-label");
        card.getChildren().add(timeLabel);

        card.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Button) return;
            openTweetDetails(originalContentId);
        });

        if (prepend) feedContainer.getChildren().add(0, card);
        else feedContainer.getChildren().add(card);
    }

    private void updateRetweetButtonUI(Button btn, boolean isRetweeted, int count) {
        btn.setText("🔄 " + count);
        if (isRetweeted) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ba7c; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-cursor: hand;");
        }
    }

    private void navigateToProfile(String targetUsername) {
        if (targetUsername == null || targetUsername.trim().isEmpty()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();

            ProfileController profileController = loader.getController();
            profileController.initUserData(targetUsername);

            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openTweetDetails(int tweetId) {
        if (tweetId == -1) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tweet-details.fxml"));
            Parent root = loader.load();
            TweetDetailsController controller = loader.getController();
            controller.setTweetId(tweetId);

            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Post");
        } catch (IOException e) {
            e.printStackTrace();
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

        try
        {
            String fileNameOnly = new File(path).getName();
            InputStream stream = getClass().getResourceAsStream("/" + fileNameOnly);
            if (stream != null)
            {
                Image img = new Image(stream);
                if (!img.isError())
                {
                    return img;
                }
            }
        } catch (Exception ignored) {}

        return loadDefaultAvatar();
    }

    private Image loadDefaultAvatar()
    {
        try
        {
            InputStream stream = getClass().getResourceAsStream("/default-avatar.png");
            if (stream != null)
            {
                return new Image(stream);
            }
        }
        catch (Exception e)
        {
            System.err.println("Default avatar resource not found!");
        }
        return null;
    }

    @FXML
    private void handleLogout()
    {
        try
        {
            NetworkManager.getInstance().disconnect();

            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToProfile()
    {
        try
        {
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
    private void handleChooseImage()
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        java.util.List<File> files = chooser.showOpenMultipleDialog(feedContainer.getScene().getWindow());
        if (files != null && !files.isEmpty())
        {
            for (File file : files) {
                String absolutePath = file.getAbsolutePath();
                if (!selectedImagePaths.contains(absolutePath)) {
                    selectedImagePaths.add(absolutePath);
                }
            }
            updateImagePreviews();
        }
    }

    private void updateImagePreviews() {
        imagePreviewContainer.getChildren().clear();
        if (selectedImagePaths.isEmpty()) {
            imagePreviewContainer.setVisible(false);
            imagePreviewContainer.setManaged(false);
            return;
        }

        imagePreviewContainer.setVisible(true);
        imagePreviewContainer.setManaged(true);

        for (String path : selectedImagePaths) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    StackPane itemPane = new StackPane();
                    itemPane.setPrefSize(100, 100);
                    itemPane.setMaxSize(100, 100);

                    ImageView iv = new ImageView(new Image(file.toURI().toString()));
                    iv.setFitWidth(100);
                    iv.setFitHeight(100);
                    iv.setPreserveRatio(false);

                    javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle(100, 100);
                    clipRect.setArcWidth(16);
                    clipRect.setArcHeight(16);
                    iv.setClip(clipRect);

                    Button removeBtn = new Button("✕");
                    removeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-background-radius: 12; -fx-font-size: 10px; -fx-padding: 2 5; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> {
                        selectedImagePaths.remove(path);
                        updateImagePreviews();
                    });

                    StackPane.setAlignment(removeBtn, javafx.geometry.Pos.TOP_RIGHT);
                    StackPane.setMargin(removeBtn, new javafx.geometry.Insets(4));

                    itemPane.getChildren().addAll(iv, removeBtn);
                    imagePreviewContainer.getChildren().add(itemPane);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRemovePreview()
    {
        selectedImagePaths.clear();
        updateImagePreviews();
    }
    @FXML
    private void handleGoToSearch()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/search.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) feedContainer.getScene().getWindow();
            double width = feedContainer.getScene().getWidth();
            double height = feedContainer.getScene().getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Search");
            stage.show();
        }
        catch (IOException e)
        {
            System.err.println("Could not load Search page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeTweetFromFeed(int tweetId) {
        feedContainer.getChildren().removeIf(node ->
                node instanceof VBox && Integer.valueOf(tweetId).equals(node.getUserData())
        );
    }

    @FXML
    private void handleTrendClick(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Hyperlink) {
            Hyperlink link = (Hyperlink) event.getSource();
            HashtagUtils.navigateToSearchWithQuery(link.getScene(), link.getText());
        }
    }
}