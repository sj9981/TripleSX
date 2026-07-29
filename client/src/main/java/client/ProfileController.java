package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    @FXML private VBox userTweetsContainer;
    @FXML private ImageView bannerImageView;

    private String currentUsername;

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
    @FXML private Button followButton;
    private boolean isFollowingCurrent;

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

            loadAvatarImage(user.optString("avatarPath", ""));

            String bPath = user.optString("bannerPath", "");
            if (bPath != null && !bPath.trim().isEmpty() && !bPath.equals("null")) {
                Image bannerImg = resolveImage(bPath);
                if (bannerImg != null) {
                    bannerImageView.setImage(bannerImg);
                } else {
                    showDefaultBanner();
                }
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
            Image image = resolveImage(path);

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

    private Image resolveImage(String path)
    {
        if (path == null || path.trim().isEmpty())
        {
            return loadDefaultAvatarImage();
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

        return loadDefaultAvatarImage();
    }

    private Image loadDefaultAvatarImage()
    {
        String[] candidates = {
                "/default-avatar.png",
                "/test-avatar.png"
        };

        for (String path : candidates)
        {
            try
            {
                java.net.URL url = getClass().getResource(path);
                if (url == null)
                {
                    continue;
                }

                Image image = new Image(url.toExternalForm(), false);
                if (!image.isError())
                {
                    return image;
                }
            }
            catch (Exception ignored)
            {
            }
        }

        return null;
    }

    private void setDefaultAvatar()
    {
        Image defaultImage = loadDefaultAvatarImage();
        if (defaultImage != null)
        {
            avatarImageView.setImage(defaultImage);
        }
    }

    private void addTweetToUI(JSONObject tweetJson) {
        int tweetId = tweetJson.optInt("tweet_id");
        String content = tweetJson.optString("content", "");
        String imagePath = tweetJson.optString("imagePath", "");
        int likeCount = tweetJson.optInt("like_count", 0);
        boolean isLiked = tweetJson.optBoolean("is_liked", false);
        int rtCount = tweetJson.optInt("retweet_count", 0);
        boolean isRetweeted = tweetJson.optBoolean("is_retweeted", false);

        VBox tweetBox = new VBox(10);
        tweetBox.getStyleClass().add("tweet-card");
        tweetBox.setStyle("-fx-cursor: hand;");

        HBox topRow = new HBox();
        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("content-label");
        contentLabel.setWrapText(true);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        topRow.getChildren().addAll(contentLabel, spacer);

        String loggedInUser = SessionManager.getInstance().getUsername();
        if (loggedInUser != null && loggedInUser.equalsIgnoreCase(currentUsername)) {
            Button delBtn = new Button("🗑");
            delBtn.getStyleClass().add("delete-button");
            delBtn.setOnAction(e -> {
                JSONObject res = NetworkManager.getInstance().deleteTweet(tweetId);
                if (res.optBoolean("success")) userTweetsContainer.getChildren().remove(tweetBox);
            });
            topRow.getChildren().add(delBtn);
        }
        tweetBox.getChildren().add(topRow);

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(350);
                    imageView.setPreserveRatio(true);
                    tweetBox.getChildren().add(imageView);
                }
            } catch (Exception ignored) {}
        }

        // Like Logic
        Button likeBtn = new Button();
        final int[] pLikes = {likeCount};
        final boolean[] pIsLiked = {isLiked};
        setupLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);

        likeBtn.setOnAction(e -> {
            if (pIsLiked[0]) {
                if (NetworkManager.getInstance().unlikeTweet(tweetId).optBoolean("success")) {
                    pIsLiked[0] = false;
                    pLikes[0]--;
                    setupLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);
                }
            } else {
                if (NetworkManager.getInstance().likeTweet(tweetId).optBoolean("success")) {
                    pIsLiked[0] = true;
                    pLikes[0]++;
                    setupLikeButtonStyle(likeBtn, pIsLiked[0], pLikes[0]);
                }
            }
        });

        tweetBox.getChildren().add(likeBtn);

        Button rtBtn = new Button();
        final int[] pRts = {rtCount};
        final boolean[] pIsRted = {isRetweeted};
        updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);

        rtBtn.setOnAction(e -> {
            if (pIsRted[0]) {
                if (NetworkManager.getInstance().unretweet(tweetId).optBoolean("success")) {
                    pIsRted[0] = false;
                    pRts[0]--;
                    updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);
                }
            } else {
                if (NetworkManager.getInstance().retweet(tweetId).optBoolean("success")) {
                    pIsRted[0] = true;
                    pRts[0]++;
                    updateRtButtonStyle(rtBtn, pIsRted[0], pRts[0]);
                }
            }
        });

        HBox actions = new HBox(15, likeBtn, rtBtn);
        tweetBox.getChildren().add(actions);

        tweetBox.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Button) return;
            openTweetDetails(tweetId);
        });

        userTweetsContainer.getChildren().add(tweetBox);
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
            controller.setUsername(currentUsername);

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