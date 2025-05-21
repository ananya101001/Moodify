# Moodify - Simple Mood Tracker

**Team:** AppSync (Ananya, Akshara, Dharmesh)
**Version:** 1.0 (as per PRD)
**Project Date:** 2024-10-27 (as per PRD)

## 🌟 Overview

Moodify is an Android application developed in Kotlin, designed to help users easily log their daily moods and journal entries. The app offers basic visualization of mood history and incorporates simple AI-powered sentiment analysis on journal text to provide additional insights. It's a user-friendly tool for self-reflection and mood awareness, intended for personal tracking and *not* as a clinical diagnostic or treatment tool.

## ✨ Core Features

*   **User Authentication:** Secure account creation and login (Email/Password via Firebase).
*   **Mood Logging:** Quickly log current mood from a predefined set (e.g., Happy, Sad, Anxious).
*   **Journaling:** Option to add free-text journal entries associated with each mood log.
*   **Timestamping:** Mood logs are automatically timestamped with date and time.
*   **Persistent Storage:** Moods and journal entries are saved securely (e.g., Firestore).
*   **Mood History:** View past mood entries in a chronological list or calendar view, with indicators for journal notes.
*   **Entry Details:** Tap to view full details of a mood entry, including the full text note if present.
*   **On-Device Sentiment Analysis:** AI-powered sentiment analysis (Positive, Negative, Neutral) on journal entries, performed on-device for privacy and offline capability.
*   **Basic Analytics Dashboard:** Simple bar chart visualizing mood distribution over the last 7 or 30 days.
*   **Privacy-Focused:** All user data is private to the individual user. No data sharing features.
*   **Intuitive UI:** Clean, simple, and responsive user interface following Android design patterns.

## 🛠️ Technologies Used

*   **Language:** Kotlin
*   **Platform:** Android (Target API 26+)
*   **IDE:** Android Studio
*   **Authentication:** Firebase Authentication (Email/Password)
*   **Database:** Firestore (for persistent data storage)
*   **AI/ML:** Firebase ML Kit (On-device Text Classification) or a TFLite model for sentiment analysis.
*   **UI:** Standard Android UI components, Material Design principles.

## ⚙️ Prerequisites (for Development/Setup)

*   Android Studio (latest stable version recommended)
*   Android SDK (API 26 or higher installed)
*   Kotlin Plugin for Android Studio (usually included)
*   A Firebase project set up.

## 🚀 Setup Instructions (For Developers)

1.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    cd moodify-project-directory
    ```

2.  **Open in Android Studio:**
    *   Launch Android Studio.
    *   Select "Open an existing Android Studio project."
    *   Navigate to and select the cloned project directory.

3.  **Firebase Setup:**
    *   Go to the [Firebase Console](https://console.firebase.google.com/).
    *   Create a new project or use an existing one.
    *   Add an Android app to your Firebase project:
        *   Follow the on-screen instructions to register your app. You'll need the package name (usually found in `app/build.gradle` as `applicationId`).
        *   Download the `google-services.json` file.
        *   Place the `google-services.json` file in the `app/` directory of your Android project.
    *   **Enable Firebase Services:**
        *   In the Firebase Console, navigate to "Authentication" and enable the "Email/Password" sign-in method.
        *   Navigate to "Firestore Database" (or "Cloud Firestore") and create a database. Start in "test mode" for initial development or set up security rules.
            *   **Security Rules (Firestore):** Ensure your Firestore security rules are set up to protect user data. A basic rule to allow only authenticated users to read/write their own data would look something like this (adapt as needed):
              ```
              rules_version = '2';
              service cloud.firestore {
                match /databases/{database}/documents {
                  // Users can only read and write their own data in a 'users' collection
                  match /users/{userId}/{document=**} {
                    allow read, write: if request.auth != null && request.auth.uid == userId;
                  }
                }
              }
              ```
        *   **Firebase ML Kit (If used for on-device sentiment analysis):**
            *   Ensure the necessary dependencies for Firebase ML Kit Text Classification are added to your `app/build.gradle` file.
            *   The model might be bundled or downloaded dynamically. The PRD suggests on-device, so this is likely handled via library dependencies. If using a custom TFLite model, ensure it's placed in the `assets` folder and loaded correctly.

4.  **Sync Gradle Files:**
    *   Android Studio should prompt you to sync Gradle files after opening the project or adding `google-services.json`. If not, click "Sync Project with Gradle Files" (elephant icon) in the toolbar.

5.  **Build the Project:**
    *   Go to "Build" > "Make Project" or "Build" > "Rebuild Project".

## ▶️ How to Run the Application

1.  **Connect an Android Device or Start an Emulator:**
    *   **Device:** Connect your Android device to your computer via USB. Ensure USB Debugging is enabled in Developer Options on your device.
    *   **Emulator:** Open the AVD Manager in Android Studio ("Tools" > "AVD Manager") and create/start an Android Virtual Device (Emulator) running API 26 or higher.

2.  **Select the Deployment Target:**
    *   In Android Studio, select your connected device or running emulator from the target device dropdown menu in the toolbar.

3.  **Run the App:**
    *   Click the "Run 'app'" button (green play icon) in the toolbar or select "Run" > "Run 'app'".
    *   Android Studio will build and install the app on the selected device/emulator.

## 📖 User Guide / How to Use Moodify

1.  **Account Creation/Login:**
    *   On first launch, you'll be prompted to create an account (using email and password) or log in if you already have one.
    *   Your session will persist until you explicitly log out.

2.  **Logging a Mood:**
    *   From the main screen, tap the "Log Mood" or similar button.
    *   Select your current mood from the predefined set of emojis/labels (e.g., Happy, Content, Neutral, Anxious, Sad, Angry).

3.  **Adding a Journal Entry (Optional):**
    *   After selecting a mood, you'll have the option to add a free-text journal note to provide more context about your feelings.

4.  **Saving the Entry:**
    *   Once you've selected a mood (and optionally added a note), save the entry. It will be automatically timestamped and stored.

5.  **Viewing Mood History:**
    *   Navigate to the "History" or "Timeline" section.
    *   You'll see a chronological list (or a calendar view) of your past mood entries.
    *   Each entry will display the mood (e.g., emoji), date, and an indicator if a journal note exists for that entry.

6.  **Viewing Entry Details & Sentiment Analysis:**
    *   Tap on any specific entry in the history view.
    *   This will display the full details: Mood, Date/Time, and the full text of your journal note (if present).
    *   If a journal note exists, the app will also display the result of the on-device sentiment analysis (e.g., "Sentiment: Positive", "Sentiment: Negative", "Sentiment: Neutral").
    *   *Note on Sentiment Analysis:* This feature provides an estimation based on the text and may not always perfectly reflect your intended emotion.

7.  **Checking the Analytics Dashboard:**
    *   Navigate to the "Dashboard" or "Analytics" section.
    *   Here, you'll find a simple visual summary (e.g., a bar chart) showing the distribution of your logged moods over a recent period (e.g., the last 7 or 30 days).

8.  **Logging Out:**
    *   You can log out of your account via a settings menu or a logout button, typically found in a profile section.

## 🔒 Privacy & Security

*   **User Authentication:** Securely managed via Firebase Authentication.
*   **Data Storage:** Your mood and journal data are stored using Firestore and protected by security rules, ensuring only you can access your own data.
*   **On-Device AI:** Sentiment analysis is performed directly on your device to enhance privacy and allow offline functionality.
*   **No Data Sharing:** Your personal mood and journal data are private and will not be shared.
*   A simple privacy notice may be presented within the app.

## ⚠️ Disclaimer

Moodify is intended for personal tracking, self-reflection, and mood awareness. It is **not** a clinical diagnostic or treatment tool. If you have concerns about your mental health, please consult a qualified healthcare professional.

## 🔭 Out of Scope (Features NOT included in this version)

*   Advanced mood prediction models or forecasting.
*   Emotion detection via camera (selfies) or microphone (voice stress).
*   AI-driven chatbots or mental health advice/interventions.
*   Social features (sharing moods, community feeds).
*   Integration with external factors (weather, calendar events, activity data).
*   Advanced analytics (correlations, complex pattern detection beyond basic charts).
*   Customizable mood scales, tags, or categories.
*   Reminders for logging moods (Could be a future enhancement).
*   Data export features.
*   Tablet-specific layouts (focus is on phone UI).

## 💡 Future Enhancements (Potential)

*   Reminders for logging moods.
*   Customizable mood scales or tags.
*   Basic data export functionality.

## 🤝 Contribution

(Optional: If this were an open-source project, you'd add contribution guidelines here. For a team project, this might not be necessary for a public README).
Example: "Feel free to fork this project, submit issues, and send pull requests."

## 📄 License

(Specify your license here, e.g., MIT, Apache 2.0, or state if it's proprietary)
Example: `This project is licensed under the MIT License - see the LICENSE.md file for details (if applicable).`
Or: `This project is proprietary and not licensed for redistribution.`

---

We hope you find Moodify helpful for your self-reflection journey!

**The AppSync Team**
(Ananya, Akshara, Dharmesh)
