package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.stage.FileChooser;
import java.io.File;

public class HomeController
{
    @FXML private TextArea tweetTextArea;
    @FXML private VBox feedContainer;

    private String username;
    private String selectedImagePath = "";
    @FXML
    private VBox homeTweetsContainer;


    private final Map<String, Image> avatarCache = new HashMap<>();

    public void setUsername(String username)
    {
        this.username = username;
        loadTweetsFromServer();
    }

    public void setUserInfo(String username)
    {
        this.username = username;
        System.out.println("Logged in user set to: " + username);
        loadTweetsFromServer();
    }

    @FXML
    public void initialize()
    {
    }

    @FXML
    private void handlePostTweet()
    {
        String tweetText = tweetTextArea.getText().trim();
        String currentUser = SessionManager.getInstance().getUsername();

        System.out.println("Current user in HomeController: " + currentUser);
        System.out.println("Selected image path: " + selectedImagePath);

        if (currentUser == null || currentUser.trim().isEmpty())
        {
            System.err.println("Username is null. Cannot post tweet.");
            return;
        }

        if (tweetText.isEmpty() && selectedImagePath.isEmpty())
        {
            System.out.println("Nothing to post.");
            return;
        }

        JSONObject response = NetworkManager.getInstance()
                .createTweet(currentUser, tweetText, selectedImagePath);

        System.out.println("Create tweet response: " + response);

        if (response != null && response.optBoolean("success", false))
        {
            tweetTextArea.clear();
            selectedImagePath = "";
            loadTweetsFromServer();
        }
        else
        {
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
                if (tweets == null) return;

                for (int i = 0; i < tweets.length(); i++) {
                    JSONObject tweetObj = tweets.getJSONObject(i);
                    addTweetToFeed(
                            tweetObj.optString("username", "Unknown"),
                            tweetObj.optString("content", ""),
                            tweetObj.optString("created_at", "")
                    );
                }
            } else {
                System.err.println("Failed to load tweets: " +
                        (response != null ? response.optString("message") : "null response"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Image getUserAvatar(String user)
    {
        if (avatarCache.containsKey(user))
        {
            return avatarCache.get(user);
        }

        try {
            JSONObject response = NetworkManager.getInstance().getUserProfile(user);

            if (response != null && response.optBoolean("success", false))
            {
                JSONObject userObj = response.getJSONObject("user");
                String avatarPath = userObj.optString("avatarPath", "");

                Image image = resolveImage(avatarPath);
                if (image != null)
                {
                    avatarCache.put(user, image);
                    return image;
                }
            }

        }
        catch (Exception e)
        {
            System.err.println("Could not load avatar for user: " + user);
            e.printStackTrace();
        }

        Image defaultImg = loadDefaultAvatar();
        if (defaultImg != null)
        {
            avatarCache.put(user, defaultImg);
        }
        return defaultImg;
    }

    private Image resolveImage(String path)
    {
        if (path == null || path.trim().isEmpty())
        {
            return loadDefaultAvatar();
        }

        try
        {
            File file = new File(path);
            if (file.exists())
            {
                Image img = new Image(file.toURI().toString(), false);
                if (!img.isError())
                {
                    return img;
                }
            }
        } catch (Exception ignored) {}

        try
        {
            String resourcePath = path.startsWith("/") ? path : "/" + path;
            InputStream stream = getClass().getResourceAsStream(resourcePath);
            if (stream != null)
            {
                Image img = new Image(stream);
                if (!img.isError())
                {
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

    private void addTweetToFeed(String user, String text, String createdAt)
    {
        VBox card = new VBox(5);
        card.getStyleClass().add("tweet-card");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Circle avatar = new Circle(18);
        Image avatarImg = getUserAvatar(user);

        if (avatarImg != null)
        {
            avatar.setFill(new ImagePattern(avatarImg));
        }
        else
        {
            avatar.setFill(Color.web("#333333"));
        }

        avatar.setStroke(Color.web("#2f3336"));
        avatar.setStrokeWidth(1.0);

        Label nameLabel = new Label(user);
        nameLabel.getStyleClass().add("username-label");

        Label handleLabel = new Label("@" + user.toLowerCase());
        handleLabel.getStyleClass().add("handle-label");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);

        Label contentLabel = new Label(text);
        contentLabel.getStyleClass().add("content-label");
        contentLabel.setWrapText(true);

        Label timeLabel = new Label(createdAt);
        timeLabel.getStyleClass().add("time-label");

        card.getChildren().addAll(header, contentLabel, timeLabel);
        feedContainer.getChildren().add(card);
    }

    @FXML
    private void handleLogout()
    {
        try
        {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
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
            profileController.initUserData(this.username);

            Stage stage = (Stage) feedContainer.getScene().getWindow();

            double width = feedContainer.getScene().getWidth();
            double height = feedContainer.getScene().getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Profile");
            stage.show();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChooseImage()
    {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(feedContainer.getScene().getWindow());
        if (file != null)
        {
            selectedImagePath = file.getAbsolutePath();
            System.out.println("Selected image: " + selectedImagePath);
        }
    }

    private void addTweetToUI(JSONObject tweetJson) {
        VBox tweetBox = new VBox(10);
        tweetBox.setStyle("-fx-padding: 15; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        Label contentLabel = new Label(tweetJson.optString("content", ""));
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        tweetBox.getChildren().add(contentLabel);

        String imagePath = tweetJson.optString("imagePath", "");
        if (imagePath != null && !imagePath.isEmpty() && !imagePath.equals("null")) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);

                    imageView.setFitWidth(300);
                    imageView.setPreserveRatio(true);

                    tweetBox.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("خطا در بارگذاری تصویر در فید: " + e.getMessage());
            }
        }

        homeTweetsContainer.getChildren().add(tweetBox);
    }

}