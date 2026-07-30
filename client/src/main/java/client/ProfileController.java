package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ProfileController
{
    @FXML private Button editProfileButton;
    @FXML private ImageView avatarImageView;
    @FXML private Label displayNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label bioLabel;
    @FXML private Label followerCountLabel;
    @FXML private Label followingCountLabel;
    @FXML private Label tweetCountLabel;
    @FXML private VBox userTweetsContainer;
    @FXML private ImageView bannerImageView;
    @FXML private Button followButton;
    @FXML private Label postsLabel;
    private String currentUsername;
    private boolean isFollowingCurrent;

    @FXML
    public void initialize()
    {
        setupAvatarView();
    }

    private void setupAvatarView()
    {
        avatarImageView.setFitWidth(100);
        avatarImageView.setFitHeight(100);
        avatarImageView.setPreserveRatio(false);
        avatarImageView.setSmooth(true);

        Circle clip = new Circle(50, 50, 50);
        avatarImageView.setClip(clip);
    }

    public void initUserData(String username)
    {
        this.currentUsername = username;

        setupAvatarView();
        setDefaultAvatar();

        String loggedInUser = SessionManager.getInstance().getUsername();

        if (loggedInUser != null && loggedInUser.equalsIgnoreCase(username)) {
            editProfileButton.setVisible(true);
            editProfileButton.setManaged(true);
            followButton.setVisible(false);
            followButton.setManaged(false);
        } else {
            editProfileButton.setVisible(false);
            editProfileButton.setManaged(false);
            followButton.setVisible(true);
            followButton.setManaged(true);
        }

        loadProfileData();
    }

    private void loadProfileData() {
        try {
            JSONObject response = NetworkManager.getInstance().getUserProfile(currentUsername);
            if (response == null || !response.optBoolean("success", false)) return;

            this.isFollowingCurrent = response.optBoolean("isFollowing", false);
            updateFollowButtonUI();

            JSONObject user = response.getJSONObject("user");
            displayNameLabel.setText(user.optString("displayName", "No Name"));
            usernameLabel.setText("@" + user.optString("username", currentUsername));
            bioLabel.setText(user.optString("bio", ""));
            followerCountLabel.setText(String.valueOf(user.optInt("followerCount", 0)));
            followingCountLabel.setText(String.valueOf(user.optInt("followingCount", 0)));

            int tweetCount = user.optInt("tweetCount", 0);

            if (tweetCountLabel != null) {
                tweetCountLabel.setText(String.valueOf(tweetCount));
            }

            if (postsLabel != null) {
                postsLabel.setText(tweetCount == 1 ? "Post" : "Posts");
            }

            loadAvatarImage(user.optString("avatarPath", ""));

            String bPath = user.optString("bannerPath", "");
            Image bannerImg = resolveImage(bPath, "/default-banner.png");
            if (bannerImg != null) {
                bannerImageView.setImage(bannerImg);
            } else {
                showDefaultBanner();
            }

            JSONArray tweets = response.optJSONArray("tweets");
            userTweetsContainer.getChildren().clear();
            if (tweets != null) {
                for (int i = 0; i < tweets.length(); i++) {
                    addTweetToUI(tweets.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDefaultBanner() {
        try {
            bannerImageView.setImage(new Image(getClass().getResourceAsStream("/default-banner.png")));
        } catch (Exception e) {
            bannerImageView.setImage(null);
        }
    }

    private void updateFollowButtonUI() {
        if (isFollowingCurrent) {
            followButton.setText("Unfollow");
            followButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-border-color: #536471; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            followButton.setText("Follow");
            followButton.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handleFollowAction() {
        String loggedInUser = SessionManager.getInstance().getUsername();
        if (loggedInUser == null) return;

        JSONObject response;
        if (isFollowingCurrent) {
            response = NetworkManager.getInstance().unfollowUser(loggedInUser, currentUsername);
        } else {
            response = NetworkManager.getInstance().followUser(loggedInUser, currentUsername);
        }

        if (response != null && response.optBoolean("success", false)) {
            isFollowingCurrent = !isFollowingCurrent;
            updateFollowButtonUI();

            loadProfileData();
        } else {
            System.err.println("Follow/Unfollow action failed: " + (response != null ? response.optString("message") : "No response"));
        }
    }

    private void loadAvatarImage(String path)
    {
        try
        {
            Image image = resolveImage(path, "/default-avatar.png");
            if (image != null && !image.isError())
            {
                avatarImageView.setImage(image);
            }
            else
            {
                setDefaultAvatar();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            setDefaultAvatar();
        }
    }

    private Image resolveImage(String path, String defaultResource)
    {
        if (path == null || path.trim().isEmpty() || "null".equalsIgnoreCase(path))
        {
            return loadResourceImage(defaultResource);
        }

        try
        {
            File file = new File(path);
            if (file.exists())
            {
                Image image = new Image(file.toURI().toString(), false);
                if (!image.isError())
                {
                    return image;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            InputStream stream = getClass().getResourceAsStream(resourcePath);

            if (stream != null)
            {
                Image image = new Image(stream);
                if (!image.isError())
                {
                    return image;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            String fileNameOnly = new File(path).getName();
            InputStream stream = getClass().getResourceAsStream("/" + fileNameOnly);

            if (stream != null)
            {
                Image image = new Image(stream);
                if (!image.isError())
                {
                    return image;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        return loadResourceImage(defaultResource);
    }

    private Image loadResourceImage(String resourcePath)
    {
        try
        {
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null)
            {
                return new Image(stream);
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private void setDefaultAvatar()
    {
        Image defaultImage = loadResourceImage("/default-avatar.png");
        if (defaultImage != null)
        {
            avatarImageView.setImage(defaultImage);
        }
    }

    private void addTweetToUI(JSONObject tweetJson) {
        int tweetId = tweetJson.optInt("tweet_id");
        String content = tweetJson.optString("content", "");
        int likeCount = tweetJson.optInt("like_count", 0);
        boolean isLiked = tweetJson.optBoolean("is_liked", false);
        int rtCount = tweetJson.optInt("retweet_count", 0);
        boolean isRetweeted = tweetJson.optBoolean("is_retweeted", false);
        int replyCount = tweetJson.optInt("reply_count", 0);

        VBox tweetBox = new VBox(8);
        tweetBox.getStyleClass().add("tweet-card");
        tweetBox.setStyle("-fx-cursor: hand;");

        // Header (Avatar + Names + Delete)
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle miniAvatar = new Circle(18);
        if (avatarImageView.getImage() != null) {
            miniAvatar.setFill(new javafx.scene.paint.ImagePattern(avatarImageView.getImage()));
        } else {
            miniAvatar.setFill(javafx.scene.paint.Color.web("#333333"));
        }

        Label nameLabel = new Label(displayNameLabel.getText());
        nameLabel.getStyleClass().add("username-label");

        Label handleLabel = new Label(usernameLabel.getText());
        handleLabel.getStyleClass().add("handle-label");

        header.getChildren().addAll(miniAvatar, nameLabel, handleLabel);

        // Delete Button Logic
        String loggedInUser = SessionManager.getInstance().getUsername();
        if (loggedInUser != null && loggedInUser.equalsIgnoreCase(currentUsername)) {
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("delete-button");
            delBtn.setOnAction(e -> {
                e.consume();
                confirmAndDelete(tweetId, tweetBox);
            });

            header.getChildren().addAll(spacer, delBtn);
        }

        // Tweet Content
        javafx.scene.text.TextFlow contentFlow = HashtagUtils.parseTweetContent(content, 15, "#e7e9ea");

        tweetBox.getChildren().addAll(header, contentFlow);

        // Images Handling
        JSONArray imagePaths = tweetJson.optJSONArray("image_paths");
        if (imagePaths != null && imagePaths.length() > 0) {
            HBox imagesLayout = new HBox(8);
            imagesLayout.setStyle("-fx-padding: 5 0 5 0;");
            for (int j = 0; j < imagePaths.length(); j++) {
                String path = imagePaths.getString(j);
                if (path != null && !path.trim().isEmpty()) {
                    try {
                        File file = new File(path);
                        if (file.exists()) {
                            ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                            imageView.setFitWidth(imagePaths.length() == 1 ? 400 : 200);
                            imageView.setPreserveRatio(true);
                            imagesLayout.getChildren().add(imageView);
                        }
                    } catch (Exception ignored) {}
                }
            }
            tweetBox.getChildren().add(imagesLayout);
        }

        // Action Buttons Bar
        Button likeBtn = new Button();
        final int[] pLikes = {likeCount};
        final boolean[] pIsLiked = {isLiked};
        updateLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);
        likeBtn.setOnAction(e -> {
            e.consume();
            if (pIsLiked[0]) {
                if (NetworkManager.getInstance().unlikeTweet(tweetId).optBoolean("success")) {
                    pIsLiked[0] = false; pLikes[0]--;
                    updateLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);
                }
            } else {
                if (NetworkManager.getInstance().likeTweet(tweetId).optBoolean("success")) {
                    pIsLiked[0] = true; pLikes[0]++;
                    updateLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);
                }
            }
        });

        Button rtBtn = new Button();
        final int[] pRts = {rtCount};
        final boolean[] pIsRted = {isRetweeted};
        updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);
        rtBtn.setOnAction(e -> {
            e.consume();
            if (pIsRted[0]) {
                if (NetworkManager.getInstance().unretweet(tweetId).optBoolean("success")) {
                    pIsRted[0] = false; pRts[0]--;
                    updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);
                }
            } else {
                if (NetworkManager.getInstance().retweet(tweetId).optBoolean("success")) {
                    pIsRted[0] = true; pRts[0]++;
                    updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);
                }
            }
        });

        Button replyBtn = new Button("💬 " + replyCount);
        replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-cursor: hand;");
        replyBtn.setOnAction(e -> { e.consume(); openTweetDetails(tweetId); });

        HBox actionsBar = new HBox(20, likeBtn, replyBtn, rtBtn);
        actionsBar.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
        tweetBox.getChildren().add(actionsBar);

        // Timestamp (Identical to Home)
        Label timeLabel = new Label(tweetJson.optString("created_at", ""));
        timeLabel.getStyleClass().add("time-label");
        tweetBox.getChildren().add(timeLabel);

        // Card Click Action
        tweetBox.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Button) return;
            openTweetDetails(tweetId);
        });

        userTweetsContainer.getChildren().add(tweetBox);
    }

    private void updateLikeButtonStyle(Button btn, boolean isLiked, int count) {
        if (isLiked) {
            btn.setText("❤ " + count);
            btn.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: #f4212e; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold;");
        } else {
            btn.setText("♡ " + count);
            btn.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: #71767b; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: normal;");
        }
    }

    private void confirmAndDelete(int tweetId, VBox tweetBox) {
        javafx.stage.Stage modal = new javafx.stage.Stage();
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);

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
                userTweetsContainer.getChildren().remove(tweetBox);
                //Update tweet count
                int currentCount = Integer.parseInt(tweetCountLabel.getText());
                tweetCountLabel.setText(String.valueOf(Math.max(0, currentCount - 1)));
            }
            modal.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-delete-button");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> modal.close());

        container.getChildren().addAll(title, body, deleteBtn, cancelBtn);

        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        modal.setScene(scene);

        javafx.stage.Window owner = userTweetsContainer.getScene().getWindow();
        modal.setX(owner.getX() + (owner.getWidth() - 320) / 2);
        modal.setY(owner.getY() + (owner.getHeight() - 400) / 2);

        modal.show();
    }

    private void openTweetDetails(int tweetId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tweet-details.fxml"));
            Parent root = loader.load();
            TweetDetailsController controller = loader.getController();
            controller.setTweetId(tweetId);

            Stage stage = (Stage) displayNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("X Clone - Post");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupLikeButtonStyle(Button btn, boolean isLiked, int count) {
        if (isLiked) {
            btn.setText("❤ " + count);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f4212e; -fx-cursor: hand; -fx-font-size: 14px;");
        } else {
            btn.setText("♡ " + count);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #71767b; -fx-cursor: hand; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void handleBack()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            homeController.setUserInfo(SessionManager.getInstance().getUsername());

            Stage stage = (Stage) displayNameLabel.getScene().getWindow();
            double width = displayNameLabel.getScene().getWidth();
            double height = displayNameLabel.getScene().getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Home");
            stage.show();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edit-profile.fxml"));
            Parent root = loader.load();

            EditProfileController controller = loader.getController();
            controller.setPreviousScene("/profile.fxml");

            String currentName = displayNameLabel.getText() != null ? displayNameLabel.getText() : "";
            String currentBio = bioLabel.getText() != null ? bioLabel.getText() : "";

            controller.initUserData(currentName, currentUsername, currentBio);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            Scene scene = new Scene(root, w, h);
            stage.setScene(scene);
            stage.setTitle("X Clone - Edit Profile");
            stage.setWidth(w);
            stage.setHeight(h);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void showFollowers() {
        openFollowList(true);
    }

    @FXML
    private void showFollowing() {
        openFollowList(false);
    }

    private void openFollowList(boolean followers) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/follow-list.fxml"));
            Parent root = loader.load();

            FollowListController controller = loader.getController();
            controller.loadData(currentUsername, followers);

            Stage stage = (Stage) displayNameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRtButtonStyle(Button btn, boolean isRetweeted, int count) {
        btn.setText("🔄 " + count);
        if (isRetweeted) {
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #00ba7c; " +
                            "-fx-cursor: hand; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 14px;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #71767b; " +
                            "-fx-cursor: hand; " +
                            "-fx-font-weight: normal; " +
                            "-fx-font-size: 14px;"
            );
        }
    }
}