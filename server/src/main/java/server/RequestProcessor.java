package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class RequestProcessor {

    public String process(String jsonRequest) {
        JSONObject response = new JSONObject();

        try {
            JSONObject request = new JSONObject(jsonRequest);

            if (!request.has("action")) {
                response.put("success", false);
                response.put("message", "Missing 'action' field.");

                if (request.has("requestId")) {
                    response.put("requestId", request.getString("requestId"));
                }

                return response.toString();
            }

            String action = request.getString("action");

            switch (action) {
                case "register":
                    response = handleRegister(request);
                    break;
                case "login":
                    response = handleLogin(request);
                    break;
                case "create_tweet":
                    response = handleCreateTweet(request);
                    break;
                case "get_tweet_details":
                    response = handleGetTweetDetails(request);
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
                case "update_profile":
                    response = handleUpdateProfile(request);
                    break;
                case "search":
                    response = handleSearch(request);
                    break;
                case "delete_tweet":
                    response = handleDeleteTweet(request);
                    break;
                case "like_tweet":
                    response = handleLikeTweet(request);
                    break;
                case "unlike_tweet":
                    response = handleUnlikeTweet(request);
                    break;
                case "get_followers_list":
                    response = handleGetFollowList(request, true);
                    break;
                case "get_following_list":
                    response = handleGetFollowList(request, false);
                    break;
                case "retweet":
                    String rtUser = request.getString("username");
                    int originalId = request.getInt("tweet_id");

                    int newRetweetRecordId = DatabaseManager.retweet(rtUser, originalId);
                    boolean rtSuccess = (newRetweetRecordId != -1);
                    response.put("success", rtSuccess);

                    if (rtSuccess) {
                        // Fetch details of the original content to broadcast the retweet UI
                        JSONObject details = DatabaseManager.getTweetDetails(originalId, rtUser);
                        if (details.optBoolean("success")) {
                            JSONObject originalData = details.getJSONObject("tweet");
                            DatabaseManager.UserProfile rtUserProfile = DatabaseManager.getUserProfile(rtUser);

                            // Create the Push Notification
                            JSONObject push = new JSONObject(originalData.toString());
                            push.put("type", "NEW_TWEET");
                            push.put("tweet_id", newRetweetRecordId); // The ID of the NEW retweet record
                            push.put("original_tweet_id", originalId);
                            push.put("is_retweet", true);
                            push.put("retweeted_by", rtUserProfile.getDisplayName());

                            // Send to the person who just retweeted
                            ConnectionManager.sendToClient(rtUser, push.toString());

                            // Send to all their followers
                            List<String> followers = DatabaseManager.getFollowers(rtUser);
                            for (String f : followers) {
                                ConnectionManager.sendToClient(f, push.toString());
                            }
                        }
                    }
                    break;
                case "download_image":
                    String fileName = request.getString("file_name");
                    try {
                        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
                        if (Files.exists(filePath)) {
                            byte[] fileBytes = Files.readAllBytes(filePath);
                            String encoded = Base64.getEncoder().encodeToString(fileBytes);
                            response.put("success", true);
                            response.put("image_data", encoded);
                        } else {
                            response.put("success", false);
                            response.put("message", "File not found on server.");
                        }
                    } catch (Exception e) {
                        response.put("success", false);
                    }
                    break;
                case "unretweet":
                    String unUser = request.getString("username");
                    int origId = request.getInt("tweet_id");
                    int deletedId = DatabaseManager.unretweet(unUser, origId);
                    boolean deleted = (deletedId != -1);
                    response.put("success", deleted);

                    if (deleted) {
                        JSONObject deletePush = new JSONObject();
                        deletePush.put("type", "DELETE_TWEET");
                        deletePush.put("tweet_id", deletedId);

                        ConnectionManager.sendToClient(unUser, deletePush.toString());
                        List<String> followers = DatabaseManager.getFollowers(unUser);
                        for (String f : followers) {
                            ConnectionManager.sendToClient(f, deletePush.toString());
                        }
                    }
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "Unknown action: " + action);
            }

            if (request.has("requestId")) {
                response.put("requestId", request.getString("requestId"));
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Server error: " + e.getMessage());
        }

        return response.toString();
    }

    private JSONObject handleRegister(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String username = request.optString("username", "").trim();
            String email = request.optString("email", "").trim();
            String password = request.optString("password", "").trim();

            String banner = null;

            if (username.isEmpty() || password.isEmpty()) {
                res.put("success", false);
                res.put("message", "Username and password are required.");
                return res;
            }

            if (email.isEmpty()) {
                email = username + "@example.com";
            }

            String displayName = request.optString("displayName", username);
            String bio = request.optString("bio", "");
            String avatar = request.optString("avatar", "default.png");

            boolean success = DatabaseManager.registerUser(username, email, password, displayName, bio, avatar, banner);
            res.put("success", success);
            res.put("message", success
                    ? "Registration successful!"
                    : "User already exists or registration failed.");

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleLogin(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String username = request.getString("username");
            String password = request.getString("password");

            boolean success = DatabaseManager.loginUser(username, password);
            res.put("success", success);
            res.put("message", success ? "Login successful!" : "Invalid credentials.");

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleCreateTweet(JSONObject request) {
        JSONObject res = new JSONObject();
        try {
            String username = request.getString("username");
            String content = request.getString("content");
            int parentTweetId = request.optInt("parent_tweet_id", -1);

            // Get the list of Base64 strings sent by the client
            JSONArray imageDataJson = request.optJSONArray("image_data");
            List<String> serverFilenames = new ArrayList<>();

            if (imageDataJson != null) {
                for (int i = 0; i < imageDataJson.length(); i++) {
                    String base64 = imageDataJson.getString(i);
                    // Save to server disk and get the new filename
                    String savedName = saveBase64Image(base64);
                    if (savedName != null) {
                        serverFilenames.add(savedName);
                    }
                }
            }

            // Save to Database using the new server-side filenames
            boolean success = DatabaseManager.createTweet(username, content, serverFilenames, parentTweetId);

            res.put("success", success);
            res.put("message", success ? "Tweet published!" : "Failed to publish tweet.");

            // Broadcast to followers (Send filenames, not full paths)
            if (success && parentTweetId == -1) {
                List<String> followers = DatabaseManager.getFollowers(username);
                JSONObject notification = new JSONObject();
                notification.put("type", "NEW_TWEET");
                notification.put("username", username);
                notification.put("content", content);
                notification.put("image_paths", new JSONArray(serverFilenames)); // Clients will see "uuid.png"
                notification.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                for (String f : followers) {
                    ConnectionManager.sendToClient(f, notification.toString());
                }
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleGetTweetDetails(JSONObject request) {
        try {
            int tweetId = request.getInt("tweet_id");
            String username = request.optString("username", "");
            return DatabaseManager.getTweetDetails(tweetId, username);
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            res.put("success", false);
            res.put("message", "Error retrieving tweet details: " + e.getMessage());
            return res;
        }
    }

    private JSONObject handleFollow(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String follower = request.getString("follower_username");
            String following = request.getString("following_username");

            if (follower.equals(following)) {
                res.put("success", false);
                res.put("message", "You cannot follow yourself.");
                return res;
            }

            boolean success = DatabaseManager.followUser(follower, following);
            res.put("success", success);
            res.put("message", success
                    ? "Successfully followed " + following
                    : "Failed to follow user (User may not exist or already followed).");

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error in follow request: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleUnfollow(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String follower = request.getString("follower_username");
            String following = request.getString("following_username");

            boolean success = DatabaseManager.unfollowUser(follower, following);
            res.put("success", success);
            res.put("message", success
                    ? "Successfully unfollowed " + following
                    : "Failed to unfollow user.");

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error in unfollow request: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleGetProfile(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String username = request.getString("username");

            DatabaseManager.UserProfile userProfileData = DatabaseManager.getUserProfile(username);
            if (userProfileData == null) {
                res.put("success", false);
                res.put("message", "User not found.");
                return res;
            }

            int userId = DatabaseManager.getUserIdByUsername(username);
            int followerCount = DatabaseManager.getFollowerCount(userId);
            int followingCount = DatabaseManager.getFollowingCount(userId);
            int tweetCount = DatabaseManager.getTweetCount(userId);

            String loggedInUser = request.optString("loggedInUser", "");
            boolean isFollowing = false;
            if (!loggedInUser.isEmpty() && !loggedInUser.equalsIgnoreCase(username)) {
                isFollowing = DatabaseManager.isFollowing(loggedInUser, username);
            }

            res.put("success", true);
            res.put("isFollowing", isFollowing);

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
            userProfileJson.put("tweetCount", tweetCount);

            res.put("user", userProfileJson);
            res.put("tweets", DatabaseManager.getUserTweets(username, loggedInUser));

        } catch (org.json.JSONException e) {
            res.put("success", false);
            res.put("message", "Invalid request format.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Internal server error: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleGetFeedTweets(JSONObject request) {
        JSONObject res = new JSONObject();

        try {
            String username = request.getString("username");

            res = DatabaseManager.getFeedTweets(username);

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error loading feed: " + e.getMessage());
        }

        return res;
    }

    private JSONObject handleUpdateProfile(JSONObject request) {
        JSONObject res = new JSONObject();
        try {
            String oldUsername = request.getString("old_username");
            String newName = request.getString("display_name");
            String newUsername = request.getString("new_username");
            String newBio = request.getString("bio");

            // Look for Base64 data from client
            String avatarBase64 = request.optString("avatar_data", null);
            String bannerBase64 = request.optString("banner_data", null);

            // Process files if they were sent, otherwise keep current
            String avatarFilename = saveBase64Image(avatarBase64);
            String bannerFilename = saveBase64Image(bannerBase64);

            // If user didn't upload a new one, keep the old path (logic in DatabaseManager)
            boolean success = DatabaseManager.updateProfile(
                    oldUsername,
                    newName,
                    newUsername,
                    newBio,
                    avatarFilename,
                    bannerFilename
            );

            res.put("success", success);
            res.put("message", success ? "Profile updated." : "Update failed.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleSearch(JSONObject request) {
        JSONObject res = new JSONObject();
        try {
            String query = request.optString("query", "").trim();
            res = DatabaseManager.search(query);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error in search handler: " + e.getMessage());
        }
        return res;
    }

    private JSONObject handleDeleteTweet(JSONObject request) {
        JSONObject res = new JSONObject();
        int tweetId = request.getInt("tweet_id");
        String username = request.getString("username");

        boolean success = DatabaseManager.deleteTweet(tweetId, username);
        res.put("success", success);
        res.put("message", success ? "Tweet deleted successfully" : "Unauthorized or error.");
        return res;
    }

    private JSONObject handleLikeTweet(JSONObject request) {
        String username = request.getString("username");
        int tweetId = request.getInt("tweet_id");
        boolean success = DatabaseManager.likeTweet(username, tweetId);
        return new JSONObject().put("success", success).put("message", success ? "Liked" : "Error");
    }

    private JSONObject handleUnlikeTweet(JSONObject request) {
        String username = request.getString("username");
        int tweetId = request.getInt("tweet_id");
        boolean success = DatabaseManager.unlikeTweet(username, tweetId);
        return new JSONObject().put("success", success).put("message", success ? "Unliked" : "Error");
    }
    private JSONObject handleGetFollowList(JSONObject request, boolean isFollowers) {
        JSONObject res = new JSONObject();
        String username = request.getString("username");
        JSONArray users = isFollowers ?
                DatabaseManager.getFollowersList(username) :
                DatabaseManager.getFollowingList(username);

        res.put("success", true);
        res.put("users", users);
        return res;
    }

    private static final String UPLOAD_DIR = "server_uploads/";

    private String saveBase64Image(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) return null;
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String fileName = UUID.randomUUID().toString() + ".png";
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Files.write(Paths.get(UPLOAD_DIR + fileName), imageBytes);
            return fileName; // Return only the name to store in DB
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}