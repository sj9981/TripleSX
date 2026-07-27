package client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager
{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    private static NetworkManager instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private NetworkManager() {}

    public static synchronized NetworkManager getInstance()
    {
        if (instance == null)
        {
            instance = new NetworkManager();
        }
        return instance;
    }

    public synchronized void connect() throws IOException
    {
        if (connected && socket != null && !socket.isClosed())
        {
            return;
        }
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        System.out.println("[CLIENT] Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
    }

    public synchronized String sendRequest(String jsonRequest)
    {
        try
        {
            if (!connected || socket == null || socket.isClosed())
            {
                connect();
            }
            out.println(jsonRequest);
            return in.readLine();
        }
        catch (IOException e)
        {
            connected = false;
            return "{\"success\": false, \"message\": \"Connection lost: " + e.getMessage() + "\"}";
        }
    }

    public synchronized void disconnect() {
        try
        {
            if (socket != null) socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            connected = false;
        }
    }

    public JSONObject login(String username, String password)
    {
        JSONObject request = new JSONObject();
        request.put("action", "login");
        request.put("username", username);
        request.put("password", password);

        String responseStr = sendRequest(request.toString());

        if (responseStr == null || responseStr.isEmpty())
        {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Empty response from server.");
            return error;
        }

        return new JSONObject(responseStr);
    }

    public JSONObject register(String username, String email, String password)
    {
        JSONObject request = new JSONObject();
        request.put("action", "register");
        request.put("username", username);
        request.put("email", email);
        request.put("password", password);

        String responseStr = sendRequest(request.toString());

        if (responseStr == null || responseStr.isEmpty())
        {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Empty response from server.");
            return error;
        }
        return new JSONObject(responseStr);
    }

    public JSONObject register(String username, String password)
    {
        String defaultEmail = username + "@xclone.com";
        return register(username, defaultEmail, password);
    }

    public JSONObject getUserProfile(String username)
    {
        JSONObject request = new JSONObject();
        request.put("action", "get_profile");
        request.put("username", username);

        String responseStr = sendRequest(request.toString());

        if (responseStr == null || responseStr.isEmpty())
        {
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Empty response from server.");
            return error;
        }
        return new JSONObject(responseStr);
    }
    public JSONObject createTweet(String username, String content, String imagePath)
    {
        JSONObject request = new JSONObject();
        request.put("action", "create_tweet");
        request.put("username", username);
        request.put("content", content);
        request.put("image_path", imagePath == null ? "" : imagePath);

        String response = sendRequest(request.toString());
        if (response == null || response.trim().isEmpty())
        {
            return null;
        }

        return new JSONObject(response);
    }


    public JSONObject getFeedTweets() {
        JSONObject request = new JSONObject();
        request.put("action", "get_feed_tweets");
        request.put("username", client.SessionManager.getInstance().getUsername());

        System.out.println("[CLIENT] Requesting feed for: " + client.SessionManager.getInstance().getUsername());
        String response = sendRequest(request.toString());

        if (response == null || response.trim().isEmpty()) {
            return new JSONObject().put("success", false).put("message", "Empty response");
        }
        return new JSONObject(response);
    }

}

