package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.IOException;

public class HomeController
{
    @FXML private TextArea tweetTextArea;
    @FXML private VBox feedContainer;
    private String username;

    public void setUsername(String username)
    {
        this.username = username;
    }

    @FXML
    private void handlePostTweet()
    {
        String tweetText = tweetTextArea.getText().trim();
        if (tweetText.isEmpty()) return;

        addTweetToFeed(username, tweetText);
        tweetTextArea.clear();
    }

    private void addTweetToFeed(String user, String text)
    {
        VBox card = new VBox(5);
        card.getStyleClass().add("tweet-card");

        HBox header = new HBox(10);
        Circle avatar = new Circle(18, javafx.scene.paint.Color.web("#333"));
        Label nameLabel = new Label(user);
        nameLabel.getStyleClass().add("username-label");
        Label handleLabel = new Label("@" + user.toLowerCase());
        handleLabel.getStyleClass().add("handle-label");

        header.getChildren().addAll(avatar, nameLabel, handleLabel);

        Label contentLabel = new Label(text);
        contentLabel.getStyleClass().add("content-label");
        contentLabel.setWrapText(true);

        card.getChildren().addAll(header, contentLabel);

        feedContainer.getChildren().add(0, card);
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
}