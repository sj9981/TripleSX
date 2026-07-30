package client;

import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class HashtagUtils {

    public static TextFlow parseTweetContent(String content, double fontSize, String textColorHex) {
        TextFlow textFlow = new TextFlow();
        if (content == null || content.isEmpty()) {
            return textFlow;
        }

        String[] tokens = content.split("(?=\\s)|(?<=\\s)");

        for (String token : tokens) {
            if (token.startsWith("#") && token.length() > 1) {
                Hyperlink hashtagLink = new Hyperlink(token);
                hashtagLink.setStyle("-fx-text-fill: #1d9bf0; " +
                                     "-fx-font-size: " + fontSize + "px; " +
                                     "-fx-underline: false; " +
                                     "-fx-padding: 0; " +
                                     "-fx-border-color: transparent; " +
                                     "-fx-background-color: transparent;");

                hashtagLink.setOnAction(e -> {
                    navigateToSearchWithQuery(hashtagLink.getScene(), token);
                });

                textFlow.getChildren().add(hashtagLink);
            } else {
                Text textNode = new Text(token);
                textNode.setStyle("-fx-fill: " + textColorHex + "; -fx-font-size: " + fontSize + "px;");
                textFlow.getChildren().add(textNode);
            }
        }
        return textFlow;
    }

    public static void navigateToSearchWithQuery(Scene currentScene, String query) {
        try {
            FXMLLoader loader = new FXMLLoader(HashtagUtils.class.getResource("/search.fxml"));
            Parent root = loader.load();

            SearchController searchController = loader.getController();
            searchController.prefillAndSearch(query);

            Stage stage = (Stage) currentScene.getWindow();
            double width = currentScene.getWidth();
            double height = currentScene.getHeight();

            stage.setScene(new Scene(root, width, height));
            stage.setTitle("X Clone - Search");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}