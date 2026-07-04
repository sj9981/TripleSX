package server;

import org.json.JSONObject;

public class RequestProcessor
{

    public String process(String jsonRequest)
    {
        JSONObject response = new JSONObject();
        try
        {
            JSONObject request = new JSONObject(jsonRequest);
            if (!request.has("action"))
            {
                response.put("success", false);
                response.put("message", "Missing 'action' field.");
                return response.toString();
            }

            String action = request.getString("action");
            switch (action)
            {
                case "register":
                    response = handleRegister(request);
                    break;
                case "login":
                    response = handleLogin(request);
                    break;
                case "create_tweet":
                    response = handleCreateTweet(request);
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "Unknown action: " + action);
            }
        }
        catch (Exception e)
        {
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }
        return response.toString();
    }

    private JSONObject handleRegister(JSONObject request)
    {
        JSONObject res = new JSONObject();
        try
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         {
            String username = request.getString("username");
            String email = request.getString("email");
            String password = request.getString("password");

            String bio = request.has("bio") ? request.getString("bio") : "";
            String avatar = request.has("avatar") ? request.getString("avatar") : "";
            String banner = request.has("banner") ? request.getString("banner") : "";

            boolean success = DatabaseManager.registerUser(username, email, password, bio, avatar, banner);
            res.put("success", success);
            res.put("message", success ? "Registration successful!" : "Registration failed.");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleLogin(JSONObject request)
    {
        JSONObject res = new JSONObject();
        try
        {
            String username = request.getString("username");
            String password = request.getString("password");

            boolean success = DatabaseManager.loginUser(username, password);
            res.put("success", success);
            res.put("message", success ? "Login successful!" : "Invalid credentials.");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleCreateTweet(JSONObject request)
    {        JSONObject res = new JSONObject();
        try {
            String username = request.getString("username");
            String content = request.getString("content");

            boolean success = DatabaseManager.createTweet(username, content);
            res.put("success", success);
            res.put("message", success ? "Tweet published!" : "Failed to publish tweet.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Invalid tweet data: " + e.getMessage());
        }
        return res;
    }
}