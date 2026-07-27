package server;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestProcessor requestProcessor;
    private String currentUsername = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.requestProcessor = new RequestProcessor();
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String message;

            while ((message = in.readLine()) != null) {
                String responseMessage = requestProcessor.process(message);

                try {
                    JSONObject requestJson = new JSONObject(message);

                    if ("login".equals(requestJson.optString("action"))) {
                        JSONObject responseJson = new JSONObject(responseMessage);

                        if (responseJson.optBoolean("success", false)) {
                            this.currentUsername = requestJson.optString("username", null);

                            if (this.currentUsername != null && !this.currentUsername.trim().isEmpty()) {
                                ConnectionManager.addClient(this.currentUsername, out);
                                System.out.println("[SERVER] User connected: " + this.currentUsername);
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[SERVER] Error while registering client connection: " + e.getMessage());
                }

                out.println(responseMessage);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] ClientHandler error: " + e.getMessage());
        } finally {
            if (currentUsername != null) {
                ConnectionManager.removeClient(currentUsername);
                System.out.println("[SERVER] User disconnected: " + currentUsername);
            }

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
