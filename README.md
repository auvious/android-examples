# Auvious Android SDK Example
This repo contains example of how to use AuviousSDK.

To use AuviousSDK in your project follow these steps:
- Add `https://nexus.auvious.com/repository/maven-releases` nexus repository. If you are using gradle
  then your build.gradle repositories section should look something like this:
  ```groovy
      repositories {
        mavenCentral()
        // ... other repositories
        // Auvious SDK Repo
        maven { url "https://nexus.auvious.com/repository/maven-releases" }
    }
  ```
- Include `com.auvious.android:sdk:1.0.15` dependency. Again if you are using Gradle, then your
  build.gradle dependencies section would look like this:
  ```groovy
  dependencies {
    //... other dependencies
  
    // Auvious SDK
    implementation 'com.auvious.android:sdk:1.0.15'
  }
  ```

- Launch `AuviousSimpleConferenceActivity` to join a call with ticket like this:
  ```kotlin
  val callOptions = AuviousSimpleConferenceOptions(
      "customer",
      "https://auvious.video",
      "wss://auvious.video/ws",
      mapOf(
          "ticket" to edit_ticket.text.toString(),
          "grant_type" to "password",
          "mic" to "true",
          "camera" to "true",
          "speaker" to "false"
      )
  )
  
  val startForResult =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult())
      { result: ActivityResult ->
          if (result.resultCode != Activity.RESULT_OK) {
              //  you will get result here in result.data
              result.data?.getParcelableExtra<AuviousSdkSimpleConferenceError>(
                  AuviousSimpleConferenceActivity.getResultIntentName()
              )?.let {
                  Toast.makeText(this, "Error code is ${it.errorCode}", Toast.LENGTH_LONG)
                      .show()
              }
          }
          AuviousConferenceSDK.instance.onDestroy()
      }
  startForResult.launch(AuviousSimpleConferenceActivity.getIntent(this, callOptions))
  ```
Within the `mapOf()` configuration, there are three options for initializing the microphone, camera, and speaker for the call session. Here's how they work and how to adjust them:

**Microphone Configuration**
- Key: `mic`
- Value: `true` or `false`
  - `true`: This enables the microphone, allowing the user to send audio during the call.
  - `false`: This disables the microphone, so the user will not be able to send audio. However, they can still hear audio if the speaker is enabled.

**Camera Configuration**
- Key: `camera`
- Value: `true` or `false`
  - `true`: This enables the camera, allowing the user to send video during the call.
  - `false`: This disables the camera, so the user will not send video. However, they can still see the video from other participants.

**Speaker Configuration**
- Key: `speaker`
- Value: `true` or `false`
- `true`: This enables the speaker, allowing the user to hear audio from other participants.
- `false`: This disables the speaker, so the user will hear sound by device's earpiece.
