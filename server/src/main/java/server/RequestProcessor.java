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
                case "follow":
                    response = handleFollow(request);
                    break;
                case "unfollow":
                    response = handleUnfollow(request);
                    break;
                case "get_profile":
                    response = handleGetProfile(request);
                    break;
                case "get_feed_tweets":
                    response = handleGetFeedTweets(request);
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
            String username = request.optString("username", "").trim();
            String email = request.optString("email", "").trim();
            String password = request.optString("password", "").trim();

            if (username.isEmpty() || password.isEmpty())
            {
                res.put("success", false);
                res.put("message", "Username and password are required.");
                return res;
            }

            if (email.isEmpty())
            {
                email = username + "@example.com";
            }

            String displayName = request.optString("displayName", username);
            String bio = request.optString("bio", "");
            String avatar = request.optString("avatar", "default.png");
            String banner = request.optString("banner", "default_banner.png");

            boolean success = DatabaseManager.registerUser(username, email, password, displayName, bio, avatar, banner);
            res.put("success", success);
            res.put("message", success ? "Registration successful!" : "User already exists or registration failed.");
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
    {
        JSONObject res = new JSONObject();
        try
        {
            String username = request.getString("username");
            String content = request.getString("content");
            String imagePath = request.optString("image_path", "").trim();

            boolean success = DatabaseManager.createTweet(username, content, imagePath);
            res.put("success", success);
            res.put("message", success ? "Tweet published!" : "Failed to publish tweet.");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Invalid tweet data: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleFollow(JSONObject request)
    {
        JSONObject res = new JSONObject();
        try
        {
            String follower = request.getString("follower_username");
            String following = request.getString("following_username");

            if (follower.equals(following))
            {
                res.put("success", false);
                res.put("message", "You cannot follow yourself.");
                return res;
            }

            boolean success = DatabaseManager.followUser(follower, following);
            res.put("success", success);
            res.put("message", success ? "Successfully followed " + following : "Failed to follow user (User may not exist or already followed).");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Error in follow request: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleUnfollow(JSONObject request)
    {
        JSONObject res = new JSONObject();
        try
        {
            String follower = request.getString("follower_username");
            String following = request.getString("following_username");

            boolean success = DatabaseManager.unfollowUser(follower, following);
            res.put("success", success);
            res.put("message", success ? "Successfully unfollowed " + following : "Failed to unfollow user.");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Error in unfollow request: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleGetProfile(JSONObject request)
    {
        JSONObject res = new JSONObject();
        try
        {
            String username = request.getString("username");

            DatabaseManager.UserProfile userProfileData = DatabaseManager.getUserProfile(username);
            if (userProfileData == null)
            {
                res.put("success", false);
                res.put("message", "User not found.");
                return res;
            }

            int userId = DatabaseManager.getUserIdByUsername(username);
            int followerCount = DatabaseManager.getFollowerCount(userId);
            int followingCount = DatabaseManager.getFollowingCount(userId);

            res.put("success", true);
            res.put("message", "Profile data retrieved successfully.");

            JSONObject userProfileJson = new JSONObject();
            userProfileJson.put("username", userProfileData.getUsername());
            userProfileJson.put("displayName", userProfileData.getDisplayName());
            userProfileJson.put("bio", userProfileData.getBio());
            userProfileJson.put("avatarPath", userProfileData.getAvatarPath());
            userProfileJson.put("bannerPath", userProfileData.getBannerPath());
            userProfileJson.put("followerCount", followerCount);
            userProfileJson.put("followingCount", followingCount);
            res.put("user", userProfileJson);

            res.put("tweets", DatabaseManager.getUserTweets(username));

        }
        catch (org.json.JSONException e)
        {
            res.put("success", false);
            res.put("message", "Invalid request format.");
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Internal server error: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleGetFeedTweets()
    {
        JSONObject res = new JSONObject();

        try
        {
            res = DatabaseManager.getFeedTweets();
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", e.getMessage());
        }

        return res;
    }


    private JSONObject handleGetFeedTweets(JSONObject request)
    {
        String username = request.optString("username", "guest");

        JSONObject res = new JSONObject();
        try
        {
            res = DatabaseManager.getFeedTweets();
        }
        catch (Exception e)
        {
            res.put("success", false);
            res.put("message", "Error loading feed: " + e.getMessage());
        }
        return res;
    }


}

