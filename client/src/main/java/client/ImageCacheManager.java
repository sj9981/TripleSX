package client;

import javafx.scene.image.Image;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ImageCacheManager {
    // Folder where images will be saved on the user's computer
    private static final String CACHE_DIR = "client_storage/";

    public static Image getImage(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.equals("null")) {
            return null;
        }

        // Define the local path
        File localFile = new File(CACHE_DIR + fileName);

        // If it exists locally, load it directly from disk
        if (localFile.exists()) {
            return new Image(localFile.toURI().toString());
        }

        // If it doesn't exist, download it from the server
        System.out.println("[CACHE] File not found locally. Downloading: " + fileName);
        try {
            Files.createDirectories(new File(CACHE_DIR).toPath());

            // Ask server for the Base64 data
            String base64Data = NetworkManager.getInstance().downloadImageFromServer(fileName);

            if (base64Data != null) {
                // Decode and save to the client_storage folder
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                Files.write(localFile.toPath(), imageBytes);

                // Load the newly saved file into JavaFX
                return new Image(localFile.toURI().toString());
            }
        } catch (Exception e) {
            System.err.println("[CACHE] Failed to sync image: " + fileName);
            e.printStackTrace();
        }

        return null;
    }
}