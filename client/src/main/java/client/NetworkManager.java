package client;

import javafx.application.Platform;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NetworkManager
{
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;

    private static NetworkManager instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = false;
    private Thread listenerThread;

    private final Map<String, ArrayBlockingQueue<JSONObject>> pendingResponses = new ConcurrentHashMap<>();

    private NetworkManager()
    {
    }

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

        startListeningThread();
    }

    public synchronized void disconnect()
    {
        connected = false;

        try
        {
            if (listenerThread != null && listenerThread.isAlive())
            {
                listenerThread.interrupt();
            }
        } catch (Exception ignored) {
        }

        try
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket = null;
        out = null;
        in = null;
        listenerThread = null;
        pendingResponses.clear();

        System.out.println("[CLIENT] Disconnected from server.");
    }

    public JSONObject login(String username, String password) {
        JSONObject request = new JSONObject();
        request.put("action", "login");
        request.put("username", username);
        request.put("password", password);
        return sendRequestObject(request);
    }

    public JSONObject register(String username, String email, String password) {
        JSONObject request = new JSONObject();
        request.put("action", "register");
        request.put("username", username);
        request.put("email", email);
        request.put("password", password);
        return sendRequestObject(request);
    }

    public JSONObject register(String username, String password) {
        String defaultEmail = username + "@xclone.com";
        return register(username, defaultEmail, password);
    }

    public JSONObject getUserProfile(String username) {
        JSONObject request = new JSONObject();
        request.put("action", "get_profile");
        request.put("username", username);
        request.put("loggedInUser", SessionManager.getInstance().getUsername());
        return sendRequestObject(request);
    }

    public JSONObject createTweet(String username, String content, String imagePath, int parentTweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "create_tweet");
        request.put("username", username);
        request.put("content", content);
        request.put("image_path", imagePath == null ? "" : imagePath);
        if (parentTweetId > 0) {
            request.put("parent_tweet_id", parentTweetId);
        }
        return sendRequestObject(request);
    }

    public JSONObject createTweet(String username, String content, String imagePath)
    {
        return createTweet(username, content, imagePath, -1);
    }

    public JSONObject getTweetDetails(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "get_tweet_details");
        request.put("tweet_id", tweetId);
        request.put("username", SessionManager.getInstance().getUsername());
        return sendRequestObject(request);
    }

    public JSONObject getFeedTweets()
    {
        JSONObject request = new JSONObject();
        request.put("action", "get_feed_tweets");
        request.put("username", SessionManager.getInstance().getUsername());

        System.out.println("[CLIENT] Requesting feed for: " + SessionManager.getInstance().getUsername());
        return sendRequestObject(request);
    }

    public JSONObject updateProfile(String oldUsername, String newName, String newUsername, String newBio, String avatarPath) {
        JSONObject request = new JSONObject();
        request.put("action", "update_profile");
        request.put("old_username", oldUsername);
        request.put("display_name", newName);
        request.put("new_username", newUsername);
        request.put("bio", newBio);
        request.put("avatar_path", avatarPath == null ? "" : avatarPath);
        return sendRequestObject(request);
    }

    public synchronized String sendRequest(String jsonRequest)
    {
        try
        {
            JSONObject request = new JSONObject(jsonRequest);
            JSONObject response = sendRequestObject(request);
            return response.toString();
        }
        catch (Exception e)
        {
            return buildErrorResponse("Invalid JSON request: " + e.getMessage()).toString();
        }
    }

    public JSONObject sendRequestObject(JSONObject request)
    {
        String requestId = UUID.randomUUID().toString();
        request.put("requestId", requestId);

        ArrayBlockingQueue<JSONObject> responseQueue = new ArrayBlockingQueue<>(1);
        pendingResponses.put(requestId, responseQueue);

        try
        {
            if (!connected || socket == null || socket.isClosed())
            {
                connect();
            }

            synchronized (this)
            {
                out.println(request.toString());
                out.flush();
            }

            JSONObject response = responseQueue.poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response == null)
            {
                return buildErrorResponse("Request timed out.");
            }

            return response;

        } catch (IOException e)
        {
            connected = false;
            return buildErrorResponse("Connection lost: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildErrorResponse("Request interrupted.");
        } catch (Exception e) {
            return buildErrorResponse("Request failed: " + e.getMessage());
        } finally {
            pendingResponses.remove(requestId);
        }
    }

    public synchronized void startListeningThread()
    {
        if (listenerThread != null && listenerThread.isAlive())
        {
            return;
        }

        listenerThread = new Thread(() -> {
            System.out.println("[CLIENT] Listener thread started.");

            try {
                while (connected && socket != null && !socket.isClosed())
                {
                    String line = in.readLine();

                    if (line == null)
                    {
                        System.err.println("[CLIENT] Server closed the connection.");
                        connected = false;
                        break;
                    }

                    System.out.println("[CLIENT] Received raw: " + line);

                    JSONObject message;
                    try
                    {
                        message = new JSONObject(line);
                    } catch (Exception e) {
                        System.err.println("[CLIENT] Invalid JSON from server: " + e.getMessage());
                        continue;
                    }

                    if (message.has("requestId")) {
                        String requestId = message.optString("requestId", "");
                        ArrayBlockingQueue<JSONObject> queue = pendingResponses.get(requestId);

                        if (queue != null) {
                            queue.offer(message);
                        } else {
                            System.out.println("[CLIENT] No pending request for requestId: " + requestId);
                        }
                        continue;
                    }

                    if (message.has("type")) {
                        String type = message.optString("type", "");

                        if ("NEW_TWEET".equals(type)) {
                            System.out.println("[CLIENT] New tweet push received.");

                            Platform.runLater(() -> {
                                HomeController controller = HomeController.getInstance();
                                if (controller != null) {
                                    controller.addTweetToFeed(message, true);
                                } else {
                                    System.err.println("[CLIENT] HomeController instance is null.");
                                }
                            });
                        } else {
                            System.out.println("[CLIENT] Unknown push type: " + type);
                        }

                        continue;
                    }

                    System.out.println("[CLIENT] Unhandled message: " + message);
                }

            } catch (IOException e) {
                if (connected) {
                    System.err.println("[CLIENT] Listener thread error: " + e.getMessage());
                    connected = false;
                }
            } finally {
                System.out.println("[CLIENT] Listener thread stopped.");
            }
        });

        listenerThread.setName("NetworkListenerThread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private JSONObject buildErrorResponse(String message) {
        JSONObject error = new JSONObject();
        error.put("success", false);
        error.put("message", message);
        return error;
    }

    public JSONObject search(String query) {
        JSONObject request = new JSONObject();
        request.put("action", "search");
        request.put("query", query);
        return sendRequestObject(request);
    }

    public JSONObject deleteTweet(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "delete_tweet");
        request.put("tweet_id", tweetId);
        request.put("username", SessionManager.getInstance().getUsername());
        return sendRequestObject(request);
    }

    public JSONObject followUser(String follower, String following) {
        JSONObject request = new JSONObject();
        request.put("action", "follow");
        request.put("follower_username", follower);
        request.put("following_username", following);
        return sendRequestObject(request);
    }

    public JSONObject unfollowUser(String follower, String following) {
        JSONObject request = new JSONObject();
        request.put("action", "unfollow");
        request.put("follower_username", follower);
        request.put("following_username", following);
        return sendRequestObject(request);
    }

    public JSONObject likeTweet(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "like_tweet");
        request.put("username", SessionManager.getInstance().getUsername());
        request.put("tweet_id", tweetId);
        return sendRequestObject(request);
    }

    public JSONObject unlikeTweet(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "unlike_tweet");
        request.put("username", SessionManager.getInstance().getUsername());
        request.put("tweet_id", tweetId);
        return sendRequestObject(request);
    }

    public JSONObject getFollowersList(String username) {
        JSONObject request = new JSONObject();
        request.put("action", "get_followers_list");
        request.put("username", username);
        return sendRequestObject(request);
    }

    public JSONObject getFollowingList(String username) {
        JSONObject request = new JSONObject();
        request.put("action", "get_following_list");
        request.put("username", username);
        return sendRequestObject(request);
    }

    public JSONObject retweet(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "retweet");
        request.put("username", SessionManager.getInstance().getUsername());
        request.put("tweet_id", tweetId);
        return sendRequestObject(request);
    }

    public JSONObject unretweet(int tweetId) {
        JSONObject request = new JSONObject();
        request.put("action", "unretweet");
        request.put("username", SessionManager.getInstance().getUsername());
        request.put("tweet_id", tweetId);
        return sendRequestObject(request);
    }
}