package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.scene.image.Image;
import java.io.File;

public class FollowListController {
    @FXML private Label titleLabel;
    @FXML private VBox userListContainer;
    private String profileOwner;

    public void loadData(String username, boolean showFollowers) {
        this.profileOwner = username;
        titleLabel.setText(showFollowers ? "Followers" : "Following");

        JSONObject response = showFollowers ?
                NetworkManager.getInstance().getFollowersList(username) :
                NetworkManager.getInstance().getFollowingList(username);

        if (response.optBoolean("success")) {
            JSONArray users = response.getJSONArray("users");
            for (int i = 0; i < users.length(); i++) {
                addUserRow(users.getJSONObject(i));
            }
        }
    }

    private void addUserRow(JSONObject user) {
        String username = user.getString("username");
        HBox row = new HBox(15);
        row.setStyle("-fx-padding: 10; -fx-cursor: hand;");

        Circle avatar = new Circle(20, Color.GRAY);
        String path = user.optString("avatarPath", "");
        if (!path.isEmpty() && new File(path).exists()) {
            avatar.setFill(new ImagePattern(new Image(new File(path).toURI().toString())));
        }

        VBox texts = new VBox(2);
        Label name = new Label(user.optString("displayName", username));
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label handle = new Label("@" + username);
        handle.setStyle("-fx-text-fill: #71767b;");
        texts.getChildren().addAll(name, handle);

        row.getChildren().addAll(avatar, texts);
        row.setOnMouseClicked(e -> navigateToProfile(username));
        userListContainer.getChildren().add(row);
    }

    private void navigateToProfile(String target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();
            ((ProfileController)loader.getController()).initUserData(target);
            Stage stage = (Stage) userListContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleBack() { navigateToProfile(profileOwner); }
}