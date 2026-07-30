package client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.InputStream;

public class ImageViewerUtils {

    public static void openFullSizeImage(Scene ownerScene, String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) return;

        try {
            Image image = null;
            File file = new File(imagePath);
            if (file.exists()) {
                image = new Image(file.toURI().toString());
            } else {
                String resourcePath = imagePath.startsWith("/") ? imagePath : "/" + imagePath;
                InputStream stream = ImageViewerUtils.class.getResourceAsStream(resourcePath);
                if (stream != null) {
                    image = new Image(stream);
                }
            }

            if (image == null || image.isError()) return;

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(ownerScene.getWindow());
            modal.initStyle(StageStyle.TRANSPARENT);

            StackPane root = new StackPane();
            root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);

            imageView.fitWidthProperty().bind(root.widthProperty().subtract(80));
            imageView.fitHeightProperty().bind(root.heightProperty().subtract(80));

            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.15); " +
                               "-fx-text-fill: white; " +
                               "-fx-background-radius: 20; " +
                               "-fx-font-size: 16px; " +
                               "-fx-font-weight: bold; " +
                               "-fx-cursor: hand; " +
                               "-fx-padding: 6 12;");
            closeBtn.setOnAction(e -> modal.close());

            StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(closeBtn, new Insets(20));

            root.getChildren().addAll(imageView, closeBtn);

            root.setOnMouseClicked(e -> {
                if (e.getTarget() != closeBtn && e.getTarget() != imageView) {
                    modal.close();
                }
            });

            Scene scene = new Scene(root, 900, 650);
            scene.setFill(Color.TRANSPARENT);

            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    modal.close();
                }
            });

            modal.setScene(scene);
            modal.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}