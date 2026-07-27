# AP404FinalProject_X_App
A simple and efficient application designed to provide a smooth and reliable user experience.

# X Clone

A desktop-based social media application inspired by X (Twitter), built with **JavaFX**, **Java**, and **PostgreSQL**.

## Features

- User registration and login
- Secure password storage
- Profile page
- Edit profile functionality
- Change name, username, and bio
- Choose and remove profile photo
- Save avatar path in database
- Persistent profile changes after restarting the app
- Dark-themed modern UI
- Client/Server project structure
- PostgreSQL database integration using JDBC

## Tech Stack

- **Java**
- **JavaFX**
- **PostgreSQL**
- **JDBC**
- **Maven**
- **jBCrypt**
- **FXML / CSS**

## Project Structure
```bash
X Clone
├── client
│   ├── src/main/java/client
│   ├── src/main/resources
│   └── ...
├── server
│   ├── src/main/java/server
│   └── ...
└── database
```
## Implemented Parts
### Authentication

- User signup
- User login
- Password hashing using jBCrypt

### Profile Management

- Display user information
- Edit profile
- Update:
  - display name
  - username
  - bio
  - avatar photo
### Avatar Handling

- Select profile image from system
- Remove profile image
- Show default avatar when no custom image exists
- Save avatar path in database
- Load avatar after restarting the program

### UI Improvements
- Dark theme styling
- Responsive-centered edit profile layout
- Better button styling
- Improved text field and text area appearance

## Database
The application uses PostgreSQL for data storage.

Example user fields:
- display_name
- username
- password
- bio
- avatar_path

## How It Works

- User data is stored in PostgreSQL.
- Profile updates are saved through JDBC.
- Avatar image path is stored in the database.
- When the app starts again, user data and avatar are loaded from the database.

## Current Status
The following parts are working successfully:

- Editing profile information
- Choosing profile photo
- Removing profile photo
- Saving profile changes in database
- Keeping changes after closing and reopening the application

