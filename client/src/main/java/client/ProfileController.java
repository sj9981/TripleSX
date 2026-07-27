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

    public void initUserData(String username)
    {
        this.currentUsername = username;

        setupAvatarView();
        setDefaultAvatar();
        loadProfileData();

        String loggedInUser = SessionManager.getInstance().getUsername();
        if (loggedInUser != null && loggedInUser.equalsIgnoreCase(username)) {
            editProfileButton.setVisible(true);
            editProfileButton.setManaged(true);
        } else {
            editProfileButton.setVisible(false);
            editProfileButton.setManaged(false);
        }
    }

    private void loadProfileData()
    {
        try
        {
            JSONObject response = NetworkManager.getInstance().getUserProfile(currentUsername);
            System.out.println("profile response = " + response.toString(2));

            if (!response.optBoolean("success", false))
            {
                System.err.println("Profile request failed.");
                return;
            }

            JSONObject user = response.getJSONObject("user");

            displayNameLabel.setText(user.optString("displayName", "No Name"));
            usernameLabel.setText("@" + user.optString("username", currentUsername));
            bioLabel.setText(user.optString("bio", "No bio yet..."));
            followerCountLabel.setText(String.valueOf(user.optInt("followerCount", 0)));
            followingCountLabel.setText(String.valueOf(user.optInt("followingCount", 0)));

            String avatarPath = user.optString("avatarPath", "");
            System.out.println("avatarPath from json = " + avatarPath);

            loadAvatarImage(avatarPath);

            JSONArray tweets = response.optJSONArray("tweets");
            userTweetsContainer.getChildren().clear();

            if (tweets != null)
            {
                for (int i = 0; i < tweets.length(); i++)
                {
                    addTweetToUI(tweets.getJSONObject(i));
                }
            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
            setDefaultAvatar();
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
                System.out.println("Avatar loaded successfully.");
            }
            else
            {
                System.out.println("Avatar image was null or invalid. Using default.");
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
            System.out.println("Avatar path is empty.");
            return loadDefaultAvatarImage();
        }

        System.out.println("Trying avatar path: " + path);

        try
        {
            File file = new File(path);
            System.out.println("absolute path = " + file.getAbsolutePath());
            System.out.println("file exists = " + file.exists());

            if (file.exists())
            {
                Image image = new Image(file.toURI().toString(), false);
                if (!image.isError())
                {
                    System.out.println("Avatar loaded from file system.");
                    return image;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed loading avatar from file system.");
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
                    System.out.println("Avatar loaded from resources: " + resourcePath);
                    return image;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed loading avatar from resources.");
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
                    System.out.println("Avatar loaded from resources by filename: " + fileNameOnly);
                    return image;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed loading avatar by filename from resources.");
        }

        System.out.println("Avatar not found anywhere. Falling back to default.");
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
        String content = tweetJson.optString("content", "");
        String imagePath = tweetJson.optString("imagePath", "");

        VBox tweetBox = new VBox();
        tweetBox.setStyle("-fx-padding: 10; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label contentLabel = new Label(content);
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        contentLabel.setWrapText(true);
        tweetBox.getChildren().add(contentLabel);

        if (!imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(300);
                    imageView.setPreserveRatio(true);
                    tweetBox.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.out.println("Could not load image in profile: " + imagePath);
            }
        }

        userTweetsContainer.getChildren().add(tweetBox);
    }


    @FXML
    private void handleBack()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            // FIX: Restore Home feed context using the actual logged-in user's username
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
}