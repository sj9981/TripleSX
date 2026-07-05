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

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * ثبت‌نام کاربر جدید
     */
    public static boolean registerUser(String username, String email, String rawPassword,
                                       String displayName, String bio, String avatarPath) {

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        String sql = "INSERT INTO users (username, email, password_hash, display_name, bio, avatar_path) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, displayName);
            pstmt.setString(5, bio);
            pstmt.setString(6, avatarPath);

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

        } catch (SQLException e)
        {
            System.err.println("Login error: " + e.getMessage());
        }
        return false;
    }

    public static boolean createTweet(String username, String content)
    {
        String sql = "INSERT INTO tweets (author_username, content) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql))
        {

            pstmt.setString(1, username);
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
}