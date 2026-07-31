***

# 📐 System Architecture & Design Report: TripleSX

**Course:** Advanced Programming (Summer 2026)  
**Instructor:** Dr. Saeed Reza Kheradpisheh  
**University:** Shahid Beheshti University (SBU)  
**Project:** TripleSX (X/Twitter Desktop Application)

---

## 1. System Architecture

TripleSX is built on a **Decoupled Client-Server Architecture** operating over custom JSON protocols via persistent TCP Sockets. The system cleanly separates client-side GUI presentation from server-side business logic, thread concurrency, and database persistence.

```
+-----------------------------------------------------------------------+
|                            CLIENT SIDE                                |
|  +-----------------------------------------------------------------+  |
|  |                JavaFX Graphical User Interface                  |  |
|  |     (Login, Home, Profile, Search, TweetDetails Controllers)    |  |
|  +-----------------------------------------------------------------+  |
|                                  |                                    |
|                   Session & UI State Management                       |
|                                  |                                    |
|  +-----------------------------------------------------------------+  |
|  |           NetworkManager (Singleton TCP Client Socket)          |  |
|  |    - Persistent Socket Connection   - Request/Response Queue     |  |
|  |    - Background ListenerThread      - Real-Time Push Handling    |  |
|  +-----------------------------------------------------------------+  |
+-----------------------------------||----------------------------------+
                                    || TCP / IP (JSON over Port 5000)
+-----------------------------------||----------------------------------+
|                            SERVER SIDE                                |
|  +-----------------------------------------------------------------+  |
|  |                     MainServer (Port 5000)                      |  |
|  |             - ExecutorService (CachedThreadPool)                |  |
|  +-----------------------------------------------------------------+  |
|                                  |                                    |
|  +-----------------------------------------------------------------+  |
|  |                         ClientHandler                           |  |
|  |    - Socket Reader/Writer           - Session Registration     |  |
|  +-----------------------------------------------------------------+  |
|               |                                    |                  |
|               v                                    v                  |
|  +---------------------------+    +--------------------------------+  |
|  |     RequestProcessor      |    |       ConnectionManager        |  |
|  |  (Command Router / Parser)|    | (Active Session Push Registry) |  |
|  +---------------------------+    +--------------------------------+  |
|               |                                                       |
|               v                                                       |
|  +-----------------------------------------------------------------+  |
|  |              DatabaseManager (JDBC Access Layer)               |  |
|  |              - BCrypt Security    - PostgreSQL Queries          |  |
|  +-----------------------------------------------------------------+  |
|                                  |                                    |
|               +------------------+------------------+                 |
|               |                                     |                 |
|               v                                     v                 |
|     +--------------------+              +--------------------+        |
|     |  PostgreSQL DBMS   |              | Media File Storage |        |
|     +--------------------+              +--------------------+        |
+-----------------------------------------------------------------------+
```

### 1.1 Communication Flow
1. **Request-Response Sync:** The client sends a JSON request containing a unique `requestId` (`UUID`). The server processes it synchronously via `RequestProcessor` and returns a JSON response containing the matching `requestId`. The client’s `NetworkManager` matches requests using `ConcurrentHashMap<String, ArrayBlockingQueue<JSONObject>>`.
2. **Asynchronous Server Push:** When a user creates or deletes a post, `ConnectionManager` iterates through connected target users' sockets and pushes server-initiated JSON notifications (e.g., `NEW_TWEET`, `DELETE_TWEET`).
3. **UI Safe Threading:** Incoming push messages are parsed by `NetworkListenerThread` on the client and safely injected into the JavaFX Application Thread via `Platform.runLater()`.

---

## 2. Database Design & Relational Schema

Persistent storage is handled by **PostgreSQL 18.4** accessed through **JDBC**. Password credentials are protected using `jBCrypt` hashing.

```
 +------------------+            +-----------------------+            +-------------------+
 |      users       |            |        tweets         |            |    tweet_media    |
 +------------------+            +-----------------------+            +-------------------+
 | id (PK)          |<---+      | id (PK)               |<---+       | id (PK)           |
 | username (UQ)    |    |      | user_id (FK)          |    |       | tweet_id (FK)-----+
 | email (UQ)       |    +------| parent_tweet_id (FK)--+    +-------| media_path        |
 | password_hash    |    |      | is_retweet_of (FK)----+            +-------------------+
 | display_name     |    |      | content               |
 | bio              |    |      | created_at            |            +-------------------+
 | avatar_path      |    |      +-----------------------+            |     hashtags      |
 | banner_path      |    |                  |                        +-------------------+
 | created_at       |    |                  |                        | id (PK)           |
 +------------------+    |                  v                        | tag (UQ)          |
       ^      ^          |      +-----------------------+            +-------------------+
       |      |          |      |    tweet_hashtags     |                      ^
       |      |          |      +-----------------------+                      |
       |      |          +------| tweet_id (FK)         |                      |
       |      |                 | hashtag_id (FK)-------+----------------------+
       |      |                 +-----------------------+
       |      |
 +------------------+            +-----------------------+
 |     follows      |            |         likes         |
 +------------------+            +-----------------------+
 | follower_id (FK) |            | user_id (FK)          |
 | following_id (FK)|            | tweet_id (FK)         |
 +------------------+            +-----------------------+
```

### 2.1 Entity Explanations & Key Relationships
- **`users`**: Core account information. Password hashes are generated via `BCrypt.hashpw()`.
- **`tweets`**: Supports original posts, threaded replies (`parent_tweet_id` referencing `tweets.id`), and retweets (`is_retweet_of` referencing `tweets.id`).
- **`tweet_media`**: Enables a 1-to-Many relationship allowing posts to contain multiple image attachments.
- **`hashtags` & `tweet_hashtags`**: A Many-to-Many junction structure enabling tag detection, index searching, and trend calculation.
- **`follows` & `likes`**: Many-to-Many junction tables using composite Primary Keys to record social graph connections and post likes.

---

## 3. Object-Oriented Design (OOD)

### 3.1 Key Classes & Responsibilities

| Component / Class | Layer | Responsibility |
| :--- | :--- | :--- |
| `MainServer` | Server | Initializes `ServerSocket(5000)` and dispatches incoming client sockets to a thread pool. |
| `ClientHandler` | Server | Implements `Runnable`; manages individual client socket I/O streams and user session registration. |
| `RequestProcessor` | Server | Command router parsing JSON actions and calling database routines. |
| `ConnectionManager` | Server | Thread-safe registry (`ConcurrentHashMap`) tracking online users for real-time broadcasts. |
| `DatabaseManager` | Server Data | Executes SQL statements using `PreparedStatement` to prevent SQL injection. |
| `NetworkManager` | Client Network | **Singleton** handling TCP socket connections, async response queues, and push listener threads. |
| `SessionManager` | Client State | **Singleton** storing current authenticated user details. |
| `HomeController` | Client GUI | Controls the main feed, tweet creation, char limit counting, and push rendering. |
| `HashtagUtils` | Client Utility | Parses `#hashtags` from text into clickable JavaFX `Hyperlink` nodes. |

### 3.2 OOP Principles Applied
- **Encapsulation:** Internal socket connections, user passwords, and state fields are private and exposed strictly via controlled methods.
- **Abstraction:** Controllers communicate with the server using clean API abstractions (`NetworkManager.getInstance().likeTweet(id)`) without exposing underlying socket networking logic.
- **Polymorphism & Interfaces:** Extensive use of `Runnable` for concurrent execution across client listener threads and server client handlers.

### 3.3 Design Patterns
1. **Model-View-Controller (MVC):** Strict separation in JavaFX between Views (`*.fxml`), Styling (`style.css`), and Controllers (`HomeController.java`, etc.).
2. **Singleton Pattern:** Used in `NetworkManager` and `SessionManager` to maintain a single socket connection and session state across scene changes.
3. **Observer / Publish-Subscribe:** Implemented in `ConnectionManager` to push real-time updates to online followers when new posts or deletions occur.
4. **Command Routing Pattern:** Implemented in `RequestProcessor` using switch-case action routing to execute specific database tasks based on incoming JSON payloads.

---

## 4. AI Usage Disclosure

In compliance with course requirements, the team explicitly discloses the use of Generative AI tools during development.

### 4.1 AI Tools & Extent of Usage
- **Tools Used:** ChatGPT (OpenAI GPT) and Google Gemini.
- **Extent of Usage:** Approximately **15–20%** of total project effort.

### 4.2 Tasks Assisted by AI
1. **Boilerplate Layout Generation:** Initial FXML structural drafting and CSS styling helper snippets for JavaFX dark mode visuals.
2. **Regex & SQL Query Drafting:** Assisting in constructing complex recursive SQL queries for reply tree retrieval (`WITH RECURSIVE reply_tree AS ...`) and hashtag extraction regex patterns.
3. **Debugging & Refactoring:** Resolving JavaFX Application Thread concurrency exceptions (`Not on FX application thread`) by adding proper `Platform.runLater()` wrapping.
4. **AI-Assisted Development & Documentation:** Used for generating the README and final report, including advanced English proofing, grammar checks, and layout/formatting suggestions to improve overall visual readability.

 

### 4.3 Human Review & Validation Process
- All AI-generated snippets were reviewed, modified, and integrated manually by team members.
- Database queries and socket logic were validated through manual unit and integration tests across multiple concurrent client instances.