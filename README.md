# Auvious Android SDK Example
This repository contains an example of how to use the [Auvious](https://auvious.com/) Android SDK in an Android project.

To use the AuviousSDK in your project, follow these steps:

## Setup Instructions

1. The Auvious Android SDK is built using Java 17 and requires your Android project to be configured to use Java 17 for compilation.

   Please ensure your module-level `build.gradle` (or `build.gradle.kts`) file includes the following settings:
   ```groovy
   kotlin {
    jvmToolchain(17)
    }
    ```

2. **Minimum SDK Version**: The SDK now requires Android API 23 (Android 6.0 Marshmallow) or higher. Ensure your app's `minSdk` is set to at least 23:
   ```groovy
   android {
       defaultConfig {
           minSdk 23
       }
   }
   ```

3. Add `https://nexus.auvious.com/repository/maven-releases` as a Nexus repository. If you're using Gradle, add this to your `build.gradle` repositories section:
   ```groovy
   repositories {
       mavenCentral()
       // ... other repositories
       // Auvious SDK Repo
       maven { url "https://nexus.auvious.com/repository/maven-releases" }
   }
   ```

4. Include the Auvious SDK dependency. Add this line to your `build.gradle` dependencies section:
   ```groovy
   dependencies {
       //... other dependencies
       // Auvious SDK
       implementation 'com.auvious.android:sdk:1.2.0'
   }
   ```

5. **Required Permissions**: Ensure your `AndroidManifest.xml` includes these permissions:
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.RECORD_AUDIO" />

   <!-- Foreground service - required for Google Play policy compliance -->
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
   ```

   **To disable screen sharing** and avoid the foreground service permissions, set `screenShareAvailability` to `"false"` in your conference options, and you can remove the foreground service permissions from your manifest:
   ```xml
   <uses-permission
       android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"
       tools:node="remove"/>
   ```

6. **Google Play Console Requirements** (for apps targeting Android 14+ / API 34+):

   If your app uses screen sharing functionality, you must complete the **Foreground Service declaration** in Google Play Console before publishing:

   1. Navigate to: **Play Console → Your App → App content → Foreground service**
   2. Declare the `mediaProjection` foreground service type
   3. Provide:
      - **Description** of screen sharing functionality in your app
      - **Video demonstration** showing:
        - User action that initiates screen sharing
        - Screen sharing in progress with notification visible
        - User action to stop screen sharing
      - **Justification** explaining the user benefit

   **Important**: Google requires video evidence showing the feature is user-initiated. Failure to complete this declaration will result in app rejection during review.

   For more details, see [Google's foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821).

## Usage Example

To start a conference, use the `AuviousSimpleConferenceActivity` with configurable call options:

```kotlin
private fun startSimpleConferenceActivity(
    enableMic: Boolean = true,
    enableCam: Boolean = true,
    enableEarPieceSpeaker: Boolean = false,
    availableMicButton: Boolean = true,
    availableCamButton: Boolean = true,
    availableSpeakerButton: Boolean = true,
    customConferenceBackgroundColor: Boolean = false,
    enablePipMenu: Boolean = true,
    enableScreenShare: Boolean = true,
    autoEnterPipOnHome: Boolean = true
) {
    val callOptions = AuviousSimpleConferenceOptions(
        "customer",
        "https://auvious.video",
        "wss://auvious.video/ws",
        mapOf(
            "ticket" to binding.ticketText.text.toString(),
            "grant_type" to "password",
            // Media controls - initial state
            AuviousSimpleConferenceOptions.speakerOption to (!enableEarPieceSpeaker).toString(),
            AuviousSimpleConferenceOptions.microphoneOption to enableMic.toString(),
            AuviousSimpleConferenceOptions.cameraOption to enableCam.toString(),
            // Button visibility
            AuviousSimpleConferenceOptions.cameraAvailable to availableCamButton.toString(),
            AuviousSimpleConferenceOptions.microphoneAvailable to availableMicButton.toString(),
            AuviousSimpleConferenceOptions.speakerAvailable to availableSpeakerButton.toString(),
            // UI customization
            AuviousSimpleConferenceOptions.conferenceBackgroundColor to if (customConferenceBackgroundColor) Color.parseColor("#3366ff").toString() else Color.BLACK.toString(),
            // Picture-in-Picture options (Android 8.0+)
            AuviousSimpleConferenceOptions.pipAvailability to enablePipMenu.toString(),
            AuviousSimpleConferenceOptions.autoEnterPip to autoEnterPipOnHome.toString(),
            // Screen sharing (Android 5.0+)
            AuviousSimpleConferenceOptions.screenShareAvailability to enableScreenShare.toString()
        )
    )
    activityForResult.launch(AuviousSimpleConferenceActivity.getIntent(this, callOptions))
}
```

### Configuration Options Reference

#### Media Controls - Initial State
Control the initial state of media devices when joining a conference:

- **Microphone Configuration**
    - **Key**: `AuviousSimpleConferenceOptions.microphoneOption` (value: `"mic"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Sets whether the microphone is enabled when joining the conference.

- **Camera Configuration**
    - **Key**: `AuviousSimpleConferenceOptions.cameraOption` (value: `"camera"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Sets whether the camera is enabled when joining the conference.

- **Speaker Configuration**
    - **Key**: `AuviousSimpleConferenceOptions.speakerOption` (value: `"speaker"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls audio output routing.
        - `"true"`: Uses the speakerphone (loudspeaker).
        - `"false"`: Uses the earpiece for private listening.

#### Button Visibility
Control which control buttons are visible in the conference UI:

- **Microphone Button Availability**
    - **Key**: `AuviousSimpleConferenceOptions.microphoneAvailable` (value: `"mic_available"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls microphone button visibility.
        - `"true"`: Users can toggle the microphone on/off.
        - `"false"`: Microphone button is hidden (microphone state is locked to initial setting).

- **Camera Button Availability**
    - **Key**: `AuviousSimpleConferenceOptions.cameraAvailable` (value: `"camera_available"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls camera button visibility.
        - `"true"`: Users can toggle the camera on/off.
        - `"false"`: Camera button is hidden (camera state is locked to initial setting).

- **Speaker Button Availability**
    - **Key**: `AuviousSimpleConferenceOptions.speakerAvailable` (value: `"speaker_available"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls speaker toggle button visibility.
        - `"true"`: Users can switch between speakerphone and earpiece.
        - `"false"`: Speaker button is hidden (audio routing is locked to initial setting).

#### Picture-in-Picture (PiP) Options
Control Picture-in-Picture behavior (requires Android 8.0 / API 26 or higher):

- **Manual PiP Menu Entry**
    - **Key**: `AuviousSimpleConferenceOptions.pipAvailability` (value: `"pip_availability"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls whether the "Floating window" menu item is shown.
        - `"true"`: Users can manually enter PiP mode via the menu.
        - `"false"`: Manual PiP entry is hidden from the menu.
    - **Note**: This does NOT affect automatic PiP when screen sharing starts.

- **Automatic PiP on Home Button**
    - **Key**: `AuviousSimpleConferenceOptions.autoEnterPip` (value: `"auto_enter_pip"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls whether the app automatically enters PiP when the user presses the Home button.
        - `"true"`: App automatically enters PiP mode when leaving (default behavior).
        - `"false"`: App goes to background normally without entering PiP.
    - **Note**: Screen sharing always triggers auto-PiP for optimal UX, regardless of this setting.

#### Screen Sharing
Control screen sharing feature availability (requires Android 5.0 / API 21 or higher):

- **Screen Share Availability**
    - **Key**: `AuviousSimpleConferenceOptions.screenShareAvailability` (value: `"screen_share_availability"`)
    - **Values**: `"true"` or `"false"`
    - **Default**: `"true"`
    - **Description**: Controls whether screen sharing is available.
        - `"true"`: "Share screen" menu item is shown, and users can share their screen.
        - `"false"`: Screen sharing feature is completely hidden and disabled.
    - **Note**: When screen sharing starts, the app automatically enters PiP mode for optimal user experience.

#### UI Customization

- **Conference Background Color**
    - **Key**: `AuviousSimpleConferenceOptions.conferenceBackgroundColor` (value: `"conference_background_color"`)
    - **Values**: String representation of an Android Color integer
    - **Default**: Black (`Color.BLACK`)
    - **Description**: Sets a custom background color for the conference screen.
    - **Example**: `Color.parseColor("#3366ff").toString()` for a custom blue background.

- **Participant Name**
    - **Key**: `AuviousSimpleConferenceOptions.participantName` (value: `"participant_name"`)
    - **Values**: String
    - **Description**: Sets a display name for the participant in the conference.

---

With these configurations, you have comprehensive control over the UI and functionality within the [Auvious](https://auvious.com/) conference experience, allowing you to fine-tune the behavior and appearance for your users.