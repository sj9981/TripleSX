package server;

import java.sql.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseManager
{
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "12345";
    private static final java.time.format.DateTimeFormatter TIMESTAMP_FORMATTER =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    public static boolean createTweet(String username, String content, String imagePath, int parentTweetId)
    {
        int userId = getUserIdByUsername(username);
        if (userId == -1) return false;

        String tweetSql = "INSERT INTO tweets (user_id, content, parent_tweet_id) VALUES (?, ?, ?)";
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
                    if (parentTweetId > 0) {
                        pstmt.setInt(3, parentTweetId);
                    } else {
                        pstmt.setNull(3, Types.INTEGER);
                    }

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

                if (imagePath != null && !imagePath.isEmpty())
                {
                    try (PreparedStatement pstmt = conn.prepareStatement(mediaSql))
                    {
                        pstmt.setInt(1, tweetId);
                        pstmt.setString(2, imagePath);
                        pstmt.executeUpdate();
                    }
                }

                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#(\\w+)");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String tag = matcher.group(1).toLowerCase();
                    int hashtagId = getOrCreateHashtag(conn, tag);
                    if (hashtagId != -1) {
                        linkTweetHashtag(conn, tweetId, hashtagId);
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

    public static boolean createTweet(String username, String content, String imagePath) {
        return createTweet(username, content, imagePath, -1);
    }

    private static int getOrCreateHashtag(Connection conn, String tag) throws SQLException {
        String selectSql = "SELECT id FROM hashtags WHERE tag = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, tag);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO hashtags (tag) VALUES (?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, tag);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignored) {
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, tag);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    private static void linkTweetHashtag(Connection conn, int tweetId, int hashtagId) throws SQLException {
        String sql = "INSERT INTO tweet_hashtags (tweet_id, hashtag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tweetId);
            ps.setInt(2, hashtagId);
            ps.executeUpdate();
        }
    }

    public static JSONObject getTweetDetails(int tweetId, String loggedInUsername) {
        JSONObject result = new JSONObject();
        int loggedInUserId = getUserIdByUsername(loggedInUsername);

        String mainSql = "SELECT t.id AS tweet_id, u.username, u.display_name, u.avatar_path, t.content, t.created_at, tm.media_path, " +
                "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id) AS like_count, " +
                "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id AND user_id = ?) AS is_liked, " +
                "(SELECT COUNT(*) FROM tweets WHERE parent_tweet_id = t.id) AS reply_count " +
                "FROM tweets t " +
                "JOIN users u ON t.user_id = u.id " +
                "LEFT JOIN tweet_media tm ON tm.tweet_id = t.id " +
                "WHERE t.id = ?";

        String repliesSql = "SELECT t.id AS tweet_id, u.username, u.display_name, u.avatar_path, t.content, t.created_at, tm.media_path, " +
                "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id) AS like_count, " +
                "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id AND user_id = ?) AS is_liked, " +
                "(SELECT COUNT(*) FROM tweets WHERE parent_tweet_id = t.id) AS reply_count " +
                "FROM tweets t " +
                "JOIN users u ON t.user_id = u.id " +
                "LEFT JOIN tweet_media tm ON tm.tweet_id = t.id " +
                "WHERE t.parent_tweet_id = ? " +
                "ORDER BY t.created_at ASC";

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(mainSql)) {
                ps.setInt(1, loggedInUserId);
                ps.setInt(2, tweetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        JSONObject tweet = new JSONObject();
                        tweet.put("tweet_id", rs.getInt("tweet_id"));
                        tweet.put("username", rs.getString("username"));
                        tweet.put("display_name", rs.getString("display_name"));
                        tweet.put("avatar_path", rs.getString("avatar_path") == null ? "" : rs.getString("avatar_path"));
                        tweet.put("content", rs.getString("content"));

                        Timestamp ts = rs.getTimestamp("created_at");
                        tweet.put("created_at", ts != null ? ts.toLocalDateTime().format(TIMESTAMP_FORMATTER) : "");
                        tweet.put("image_path", rs.getString("media_path") == null ? "" : rs.getString("media_path"));
                        tweet.put("like_count", rs.getInt("like_count"));
                        tweet.put("is_liked", rs.getInt("is_liked") > 0);
                        tweet.put("reply_count", rs.getInt("reply_count"));

                        result.put("tweet", tweet);
                    } else {
                        result.put("success", false);
                        result.put("message", "Tweet not found.");
                        return result;
                    }
                }
            }

            JSONArray replies = new JSONArray();
            try (PreparedStatement ps = conn.prepareStatement(repliesSql)) {
                ps.setInt(1, loggedInUserId);
                ps.setInt(2, tweetId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JSONObject reply = new JSONObject();
                        reply.put("tweet_id", rs.getInt("tweet_id"));
                        reply.put("username", rs.getString("username"));
                        reply.put("display_name", rs.getString("display_name"));
                        reply.put("avatar_path", rs.getString("avatar_path") == null ? "" : rs.getString("avatar_path"));
                        reply.put("content", rs.getString("content"));

                        Timestamp ts = rs.getTimestamp("created_at");
                        reply.put("created_at", ts != null ? ts.toLocalDateTime().format(TIMESTAMP_FORMATTER) : "");
                        reply.put("image_path", rs.getString("media_path") == null ? "" : rs.getString("media_path"));
                        reply.put("like_count", rs.getInt("like_count"));
                        reply.put("is_liked", rs.getInt("is_liked") > 0);
                        reply.put("reply_count", rs.getInt("reply_count"));

                        replies.put(reply);
                    }
                }
            }

            result.put("success", true);
            result.put("replies", replies);

        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "Error getting tweet details: " + e.getMessage());
        }

        return result;
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

    public static boolean isFollowing(String followerUsername, String followingUsername) {
        String sql = "SELECT 1 FROM follows WHERE follower_id = (SELECT id FROM users WHERE username = ?) " +
                "AND following_id = (SELECT id FROM users WHERE username = ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, followerUsername);
            pstmt.setString(2, followingUsername);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static JSONArray getUserTweets(String profileUsername, String loggedInUsername) {
        JSONArray tweets = new JSONArray();
        int profileUserId = getUserIdByUsername(profileUsername);
        int loggedInUserId = getUserIdByUsername(loggedInUsername);

        if (profileUserId == -1) return tweets;

        String sql = "SELECT t.id AS tweet_id, t.content, m.media_path, t.created_at, " +
                "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id) AS like_count, " +
                "(SELECT 1 FROM likes WHERE tweet_id = t.id AND user_id = ?) AS is_liked, " +
                "(SELECT COUNT(*) FROM tweets WHERE parent_tweet_id = t.id) AS reply_count " +
                "FROM tweets t " +
                "LEFT JOIN tweet_media m ON t.id = m.tweet_id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loggedInUserId);
            pstmt.setInt(2, profileUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject tweet = new JSONObject();
                    tweet.put("tweet_id", rs.getInt("tweet_id"));
                    tweet.put("content", rs.getString("content"));
                    tweet.put("imagePath", rs.getString("media_path") == null ? "" : rs.getString("media_path"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    String formattedDate = (ts != null) ? ts.toLocalDateTime().format(TIMESTAMP_FORMATTER) : "";
                    tweet.put("created_at", formattedDate);

                    tweet.put("like_count", rs.getInt("like_count"));
                    tweet.put("is_liked", rs.getInt("is_liked") > 0);
                    tweet.put("reply_count", rs.getInt("reply_count"));
                    tweets.put(tweet);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user tweets: " + e.getMessage());
        }
        return tweets;
    }

    public static JSONObject getFeedTweets(String username)
    {
        JSONObject result = new JSONObject();
        JSONArray tweets = new JSONArray();

        String sql =
                "SELECT t.id AS tweet_id, " +
                        "u.username, " +
                        "u.display_name, " +
                        "u.avatar_path, " +
                        "t.content, " +
                        "t.created_at, " +
                        "tm.media_path, " +
                        "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id) AS like_count, " +
                        "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id AND user_id = (SELECT id FROM users WHERE username = ?)) AS is_liked, " +
                        "(SELECT COUNT(*) FROM tweets WHERE parent_tweet_id = t.id) AS reply_count " +
                        "FROM tweets t " +
                        "JOIN users u ON t.user_id = u.id " +
                        "LEFT JOIN tweet_media tm ON tm.tweet_id = t.id " +
                        "WHERE t.parent_tweet_id IS NULL AND (" +
                        "   t.user_id = (SELECT id FROM users WHERE username = ?) " +
                        "   OR t.user_id IN ( " +
                        "       SELECT following_id " +
                        "       FROM follows " +
                        "       WHERE follower_id = (SELECT id FROM users WHERE username = ?) " +
                        "   ) " +
                        ") " +
                        "ORDER BY t.created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setString(3, username);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    JSONObject tweet = new JSONObject();

                    tweet.put("tweet_id", rs.getInt("tweet_id"));
                    tweet.put("username", rs.getString("username"));
                    tweet.put("display_name", rs.getString("display_name"));
                    tweet.put("avatar_path", rs.getString("avatar_path") == null ? "" : rs.getString("avatar_path"));
                    tweet.put("content", rs.getString("content"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    String formattedDate = (ts != null) ? ts.toLocalDateTime().format(TIMESTAMP_FORMATTER) : "";
                    tweet.put("created_at", formattedDate);

                    String image = rs.getString("media_path");
                    if (image == null) {
                        image = "";
                    }
                    tweet.put("image_path", image);

                    tweet.put("like_count", rs.getInt("like_count"));
                    tweet.put("is_liked", rs.getInt("is_liked") > 0);
                    tweet.put("reply_count", rs.getInt("reply_count"));
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

    public static boolean updateProfile(String oldUsername, String newName, String newUsername, String newBio, String avatarPath)
    {
        String sql = "UPDATE users SET display_name = ?, username = ?, bio = ?, avatar_path = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, newName);
            ps.setString(2, newUsername);
            ps.setString(3, newBio);

            if (avatarPath == null || avatarPath.trim().isEmpty())
            {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            else
            {
                ps.setString(4, avatarPath);
            }

            ps.setString(5, oldUsername);

            return ps.executeUpdate() > 0;
        }
        catch (SQLException e)
        {
            System.err.println("Database Error during profile update: " + e.getMessage());
            return false;
        }
    }

    public static JSONObject search(String query)
    {
        JSONObject result = new JSONObject();
        JSONArray users = new JSONArray();
        JSONArray tweets = new JSONArray();

        String cleanQuery = query.startsWith("#") ? query.substring(1) : query;
        String searchQuery = "%" + cleanQuery + "%";

        String userSql = "SELECT username, display_name, bio, avatar_path FROM users " +
                         "WHERE username ILIKE ? OR display_name ILIKE ?";

        String tweetSql = "SELECT DISTINCT t.id AS tweet_id, u.username, u.display_name, u.avatar_path, t.content, t.created_at, tm.media_path, " +
                          "(SELECT COUNT(*) FROM likes WHERE tweet_id = t.id) AS like_count, " +
                          "(SELECT COUNT(*) FROM tweets WHERE parent_tweet_id = t.id) AS reply_count " +
                          "FROM tweets t " +
                          "JOIN users u ON t.user_id = u.id " +
                          "LEFT JOIN tweet_media tm ON tm.tweet_id = t.id " +
                          "LEFT JOIN tweet_hashtags th ON th.tweet_id = t.id " +
                          "LEFT JOIN hashtags h ON h.id = th.hashtag_id " +
                          "WHERE t.content ILIKE ? OR h.tag ILIKE ? " +
                          "ORDER BY t.created_at DESC";

        try (Connection conn = getConnection())
        {
            try (PreparedStatement pstmt = conn.prepareStatement(userSql))
            {
                pstmt.setString(1, searchQuery);
                pstmt.setString(2, searchQuery);
                try (ResultSet rs = pstmt.executeQuery())
                {
                    while (rs.next())
                    {
                        JSONObject user = new JSONObject();
                        user.put("username", rs.getString("username"));
                        user.put("displayName", rs.getString("display_name"));
                        user.put("bio", rs.getString("bio") != null ? rs.getString("bio") : "");
                        user.put("avatarPath", rs.getString("avatar_path") != null ? rs.getString("avatar_path") : "");
                        users.put(user);
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(tweetSql))
            {
                pstmt.setString(1, searchQuery);
                pstmt.setString(2, searchQuery);
                try (ResultSet rs = pstmt.executeQuery())
                {
                    while (rs.next())
                    {
                        JSONObject tweet = new JSONObject();
                        tweet.put("tweet_id", rs.getInt("tweet_id"));
                        tweet.put("username", rs.getString("username"));
                        tweet.put("display_name", rs.getString("display_name"));
                        tweet.put("avatar_path", rs.getString("avatar_path") != null ? rs.getString("avatar_path") : "");
                        tweet.put("content", rs.getString("content"));

                        Timestamp ts = rs.getTimestamp("created_at");
                        tweet.put("created_at", ts != null ? ts.toLocalDateTime().format(TIMESTAMP_FORMATTER) : "");
                        tweet.put("image_path", rs.getString("media_path") != null ? rs.getString("media_path") : "");
                        tweet.put("like_count", rs.getInt("like_count"));
                        tweet.put("reply_count", rs.getInt("reply_count"));
                        tweets.put(tweet);
                    }
                }
            }

            result.put("success", true);
            result.put("users", users);
            result.put("tweets", tweets);
        }
        catch (SQLException e)
        {
            System.err.println("Database error during search: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    public static boolean deleteTweet(int tweetId, String username) {
        String sql = "DELETE FROM tweets WHERE id = ? AND user_id = (SELECT id FROM users WHERE username = ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tweetId);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting tweet: " + e.getMessage());
            return false;
        }
    }

    public static boolean likeTweet(String username, int tweetId) {
        int userId = getUserIdByUsername(username);
        if (userId == -1) return false;

        String sql = "INSERT INTO likes (user_id, tweet_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, tweetId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error liking tweet: " + e.getMessage());
            return false;
        }
    }

    public static boolean unlikeTweet(String username, int tweetId) {
        int userId = getUserIdByUsername(username);
        String sql = "DELETE FROM likes WHERE user_id = ? AND tweet_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, tweetId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error unliking tweet: " + e.getMessage());
            return false;
        }
    }

    public static int getLikeCount(int tweetId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE tweet_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tweetId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }
        return 0;
    }
}