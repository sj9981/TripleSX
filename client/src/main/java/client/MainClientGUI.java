package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClientGUI extends Application
{

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        primaryStage.setTitle("X Clone - Login");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        Scene scene = new Scene(root, 1200, 900);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        // Set Minimums
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(900);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

