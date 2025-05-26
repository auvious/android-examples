# Auvious Android SDK Example
This repository contains an example of how to use the AuviousSDK in an Android project.

To use the AuviousSDK in your project, follow these steps:

## Setup Instructions

1. The Auvious Android SDK is built using Java 17 and requires your Android project to be configured to use Java 17 for compilation.

   Please ensure your module-level `build.gradle` (or `build.gradle.kts`) file includes the following settings:
   ```groovy
   kotlin {
    jvmToolchain(17)
    }
    ```

2. Add `https://nexus.auvious.com/repository/maven-releases` as a Nexus repository. If you're using Gradle, add this to your `build.gradle` repositories section:
   ```groovy
   repositories {
       mavenCentral()
       // ... other repositories
       // Auvious SDK Repo
       maven { url "https://nexus.auvious.com/repository/maven-releases" }
   }
   ```

3. Include the Auvious SDK dependency. Add this line to your `build.gradle` dependencies section:
   ```groovy
   dependencies {
       //... other dependencies
       // Auvious SDK
       implementation 'com.auvious.android:sdk:1.1.4'
   }
   ```

## Usage Example

To start a conference, use the `AuviousSimpleConferenceActivity` with configurable call options, such as microphone, camera, and speaker settings:

```kotlin
private fun startSimpleConferenceActivity(
    enableMic: Boolean = true,
    enableCam: Boolean = true,
    enableEarPieceSpeaker: Boolean = false,
    availableMicButton: Boolean = true,
    availableCamButton: Boolean = true,
    availableSpeakerButton: Boolean = true,
    customConferenceBackgroundColor: Boolean = false
) {
    val callOptions = AuviousSimpleConferenceOptions(
        "customer",
        "https://auvious.video",
        "wss://auvious.video/ws",
        mapOf(
            "ticket" to binding.ticketText.text.toString(),
            "grant_type" to "password",
            AuviousSimpleConferenceOptions.speakerOption to (!enableEarPieceSpeaker).toString(),
            AuviousSimpleConferenceOptions.microphoneOption to enableMic.toString(),
            AuviousSimpleConferenceOptions.cameraOption to enableCam.toString(),
            AuviousSimpleConferenceOptions.cameraAvailable to availableCamButton.toString(),
            AuviousSimpleConferenceOptions.microphoneAvailable to availableMicButton.toString(),
            AuviousSimpleConferenceOptions.speakerAvailable to availableSpeakerButton.toString(),
            AuviousSimpleConferenceOptions.conferenceBackgroundColor to if (customConferenceBackgroundColor) Color.parseColor("#3366ff").toString() else Color.BLACK.toString()
        )
    )
    activityForResult.launch(AuviousSimpleConferenceActivity.getIntent(this, callOptions))
}
```

### Option Descriptions

#### Enable default functionality
- **Microphone Configuration**
    - **Key**: `mic`
    - **Value**: `true` or `false`
        - `true`: Enables the microphone.
        - `false`: Disables the microphone.

- **Camera Configuration**
    - **Key**: `camera`
    - **Value**: `true` or `false`
        - `true`: Enables the camera.
        - `false`: Disables the camera.

- **Speaker Configuration**
    - **Key**: `speaker`
    - **Value**: `true` or `false`
        - `true`: Enables the speaker.
        - `false`: Enables only the earpiece.

#### Available conference control buttons
- **Microphone Button Availability**
    - **Key**: `mic_available`
    - **Value**: `true` or `false`
        - `true`: The microphone button will be available for toggling on/off.
        - `false`: The microphone button will be hidden.

- **Camera Button Availability**
    - **Key**: `camera_available`
    - **Value**: `true` or `false`
        - `true`: The camera button will be available for toggling on/off.
        - `false`: The camera button will be hidden.

- **Speaker Button Availability**
    - **Key**: `speaker_available`
    - **Value**: `true` or `false`
        - `true`: The speaker button will be available for toggling on/off.
        - `false`: The speaker button will be hidden.

- **Custom Conference Background Color**
    - **Key**: `conference_background_color`
    - **Value**: [Color](https://developer.android.com/reference/android/graphics/Color) object or a hex color by using the [Color.parseColor()](https://developer.android.com/reference/android/graphics/Color#parseColor(java.lang.String)) method
        - Set a custom color in conference background. Otherwise, the default background color will be black.

---

With these additional configurations, you have greater control over the UI and functionality within the [Auvious](www.auvious.com) conference experience, allowing you to fine-tune the behavior and appearance for your users.