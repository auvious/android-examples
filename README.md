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
- Include `com.auvious.android:sdk:1.0.9` dependency. Again if you are using Gradle, then your
  build.gradle dependencies section would look like this:
  ```groovy
  dependencies {
    //... other dependencies
  
    // Auvious SDK
    implementation 'com.auvious.android:sdk:1.0.12'
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
          "camera" to "true"
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