package client;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class FileTransferUtils {
    public static String encodeFileToBase64(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return null;
            byte[] bytes = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}