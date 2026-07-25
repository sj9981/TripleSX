package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.ResultSet;

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

    public static boolean createTweet(String username, String content)
    {
        int userId = getUserIdByUsername(username);
        if (userId == -1) return false;

        String sql = "INSERT INTO tweets (user_id, content) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {
            pstmt.setInt(1, userId);
            pstmt.setString(2, content);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Error creating tweet: " + e.getMessage());
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

    public static java.util.List<String> getUserTweets(String username)
    {
        java.util.List<String> tweets = new java.util.ArrayList<>();
        int userId = getUserIdByUsername(username);
        if (userId == -1) return tweets;

        String sql = "SELECT content FROM tweets WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery())
            {
                while (rs.next())
                {
                    tweets.add(rs.getString("content"));
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving user tweets: " + e.getMessage());
        }
        return tweets;
    }
}
