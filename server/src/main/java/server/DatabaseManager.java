package server;

import java.sql.*;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import static org.postgresql.PGProperty.PASSWORD;

public class DatabaseManager
{
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Sa123456*";


    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static class UserProfile
    {
        String username;
        String displayName;
        String bio;
        String avatarPath;
        String bannerPath;

        public UserProfile(String username, String displayName, String bio, String avatarPath, String bannerPath)
        {
            this.username = username;
            this.displayName = displayName;
            this.bio = bio;
            this.avatarPath = avatarPath;
            this.bannerPath = bannerPath;
        }

        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getBio() { return bio; }
        public String getAvatarPath() { return avatarPath; }
        public String getBannerPath() { return bannerPath; }
    }


    public static UserProfile getUserProfile(String username)
    {
        String sql = "SELECT username, display_name, bio, avatar_path, banner_path FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery())
            {
                if (rs.next())
                {
                    return new UserProfile(
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("bio"),
                            rs.getString("avatar_path"),
                            rs.getString("banner_path")
                    );
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving user profile: " + e.getMessage());
        }
        return null;
    }

    public static boolean registerUser(String username, String email, String rawPassword,
                                       String displayName, String bio, String avatarPath, String bannerPath)
    {

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        String sql = "INSERT INTO users (username, email, password_hash, display_name, bio, avatar_path, banner_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, displayName);
            pstmt.setString(5, bio);
            pstmt.setString(6, avatarPath);
            pstmt.setString(7, bannerPath);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Database Error during registration: " + e.getMessage());
            return false;
        }
    }

    public static boolean loginUser(String username, String rawPassword)
    {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, username);

            try (ResultSet resultSet = pstmt.executeQuery())
            {
                if (resultSet.next())
                {
                    String storedHash = resultSet.getString("password_hash");
                    return BCrypt.checkpw(rawPassword, storedHash);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Login error: " + e.getMessage());
        }
        return false;
    }

    public static boolean createTweet(String username, String content, String imagePath)
    {
        int userId = getUserIdByUsername(username);
        if (userId == -1) return false;

        String tweetSql = "INSERT INTO tweets (user_id, content) VALUES (?, ?)";
        String mediaSql = "INSERT INTO tweet_media (tweet_id, media_path) VALUES (?, ?)";

        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);

            try
            {
                int tweetId;

                try (PreparedStatement pstmt = conn.prepareStatement(tweetSql, Statement.RETURN_GENERATED_KEYS))
                {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, content);

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0)
                    {
                        conn.rollback();
                        return false;
                    }

                    try (ResultSet rs = pstmt.getGeneratedKeys())
                    {
                        if (rs.next())
                        {
                            tweetId = rs.getInt(1);
                        }
                        else
                        {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                if (!imagePath.isEmpty())
                {
                    try (PreparedStatement pstmt = conn.prepareStatement(mediaSql))
                    {
                        pstmt.setInt(1, tweetId);
                        pstmt.setString(2, imagePath);
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            }
            catch (SQLException e)
            {
                conn.rollback();
                System.err.println("Error creating tweet: " + e.getMessage());
                return false;
            }
            finally
            {
                conn.setAutoCommit(true);
            }
        }
        catch (SQLException e)
        {
            System.err.println("Database connection error: " + e.getMessage());
            return false;
        }
    }

    public static int getUserIdByUsername(String username)
    {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("id");
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error finding user ID: " + e.getMessage());
        }
        return -1;
    }

    public static boolean followUser(String followerUsername, String followingUsername)
    {
        int followerId = getUserIdByUsername(followerUsername);
        int followingId = getUserIdByUsername(followingUsername);

        if (followerId == -1 || followingId == -1 || followerId == followingId) return false;

        String sql = "INSERT INTO follows (follower_id, following_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Follow error: " + e.getMessage());
            return false;
        }
    }

    public static boolean unfollowUser(String followerUsername, String followingUsername)
    {
        int followerId = getUserIdByUsername(followerUsername);
        int followingId = getUserIdByUsername(followingUsername);

        String sql = "DELETE FROM follows WHERE follower_id = ? AND following_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setInt(1, followerId);
            pstmt.setInt(2, followingId);
            return pstmt.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Unfollow error: " + e.getMessage());
            return false;
        }
    }

    public static int getFollowerCount(int userId)
    {
        String sql = "SELECT COUNT(*) FROM follows WHERE following_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt(1);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error getting follower count: " + e.getMessage());
        }
        return 0;
    }

    public static int getFollowingCount(int userId)
    {
        String sql = "SELECT COUNT(*) FROM follows WHERE follower_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt(1);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error getting following count: " + e.getMessage());
        }
        return 0;
    }

    public static org.json.JSONArray getUserTweets(String username)
    {
        org.json.JSONArray tweets = new org.json.JSONArray();

        int userId = getUserIdByUsername(username);
        if (userId == -1)
            return tweets;

        String sql =
                "SELECT t.content, m.media_path " +
                        "FROM tweets t " +
                        "LEFT JOIN tweet_media m ON t.id = m.tweet_id " +
                        "WHERE t.user_id = ? " +
                        "ORDER BY t.created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery())
            {
                while (rs.next())
                {
                    JSONObject tweet = new JSONObject();

                    tweet.put("content", rs.getString("content"));

                    String imagePath = rs.getString("media_path");
                    if (imagePath == null)
                        imagePath = "";

                    tweet.put("imagePath", imagePath);

                    tweets.put(tweet);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving user tweets: " + e.getMessage());
        }

        return tweets;
    }

    public static JSONObject getFeedTweets(String username)
    {
        JSONObject result = new JSONObject();
        org.json.JSONArray tweets = new org.json.JSONArray();

        String sql =
                "SELECT u.username, " +
                        "u.display_name, " +
                        "u.avatar_path, " +
                        "t.content, " +
                        "t.created_at, " +
                        "tm.media_path " +
                        "FROM tweets t " +
                        "JOIN users u ON t.user_id = u.id " +
                        "LEFT JOIN tweet_media tm ON tm.tweet_id = t.id " +
                        "WHERE t.user_id = (SELECT id FROM users WHERE username = ?) " +
                        "   OR t.user_id IN ( " +
                        "       SELECT following_id " +
                        "       FROM follows " +
                        "       WHERE follower_id = (SELECT id FROM users WHERE username = ?) " +
                        "   ) " +
                        "ORDER BY t.created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, username);
            ps.setString(2, username);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    JSONObject tweet = new JSONObject();

                    tweet.put("username", rs.getString("username"));
                    tweet.put("display_name", rs.getString("display_name"));
                    tweet.put("avatar_path", rs.getString("avatar_path"));
                    tweet.put("content", rs.getString("content"));
                    tweet.put("created_at", rs.getTimestamp("created_at").toString());

                    String image = rs.getString("media_path");
                    if (image == null) {
                        image = "";
                    }

                    tweet.put("image_path", image);

                    tweets.put(tweet);
                }
            }

            result.put("success", true);
            result.put("tweets", tweets);
        }
        catch (Exception e)
        {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    public static java.util.List<String> getFollowers(String username)
    {
        java.util.List<String> followers = new java.util.ArrayList<>();
        String sql = "SELECT u.username FROM users u " +
                "JOIN follows f ON u.id = f.follower_id " +
                "WHERE f.following_id = (SELECT id FROM users WHERE username = ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    followers.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting followers: " + e.getMessage());
        }
        return followers;
    }



}

