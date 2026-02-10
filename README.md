## Compiling and running the Beiwe Android app

## Compiling
1. To compile and sign the app, you must add a directory called `private` (`beiwe-android`'s `.gitignore` file keeps the `private` directory out of Git), and a file called `private/keystore.properties`, containing these lines (no quotes around the values that you fill in):
    ```
    storePassword=KEYSTORE_PASSWORD
    keyPassword=KEY_PASSWORD
    keyAlias=KEY_ALIAS
    storeFile=KEYSTORE_FILEPATH
    ```

2. If it's your first time running the project, **open Android Studio and click "Sync Project with Gradle Files"**.  If you run into errors, open Android Studio's "SDK Manager" and make sure you have the correct SDK Platform installed (the "API level" should match the one specified in app/build.gradle's `compileSdkVersion`).

3. You'll need to **generate a key file** by running Build -> Generate Signed Bundle/APK, and then, inside `private/keystore.properties`, update the four values with the information from your newly-generated key and keystore.

4. (Optional) You can also configure a Sentry DSN for each build type inside your `private/keystore.properties` file.
    ```
    releaseDSN=https://publicKey:secretKey@host:port/1?options
    betaDSN=https://publicKey:secretKey@host:port/1?options
    developmentDSN=https://publicKey:secretKey@host:port/1?options
    ```

## Setting up Firebase
To receive push notifications, add Firebase to your Android Project following [these steps](https://firebase.google.com/docs/android/setup) up through Step 3. 


## Build Variants and Product Flavors
When you build the Android app, you choose one of the Build Variants and one of the Product Flavors.  There are three of each, specified in the `buildTypes` section of `app/build.gradle`.  To select which Build Variant the app compiles as, go to **Build** > **Select Build Variant** in the menu bar [(see the documentation)](https://developer.android.com/studio/run/index.html#changing-variant).

#### The three Build Variants are:

| Build Variant | App name | Password requirements | Debug interface |
| --- | --- | --- | --- |
| **release** | "Beiwe" or "Beiwe2" | At least 6 characters | No |
| **beta** | "Beiwe-beta" or "Beiwe2-beta" | At least 1 character | Yes |
| **development** | "Beiwe-development" or "Beiwe2-development" | At least 1 character | Yes, plus extra buttons only useful for develpers, like buttons to crash the app.  Also includes some extra logging statements that write to LogCat, but not to app log files that get uploaded. |

#### The three Product Flavors are:

| Product Flavor | App Name | Intended for | Server URL | Record text message and call log stats | Request background location permission |
| --- | --- | --- | --- | --- | --- |
| **onnelaLabServer** | Beiwe | Download APK from studies.beiwe.org/download | Hardcoded to studies.beiwe.org | Yes | Yes |
| **commStatsCustomUrl** | Beiwe | Download APK from `/download` link on other Beiwe deployments | Customizable at registration | Yes | Yes |

## Distributing the app
_This section is only intended for developers maintaining the canonical copies of the Beiwe and Beiwe2 Android apps; this is not useful for anyone working on a fork of the repo._

When we update the app version, we need to update the three public versions of the app: the googlePlayStore flavor that's downloadable from Google Play, and the onnelaLabServer and commStatsCustomUrl flavors which get compiled as APKs, stored in a public bucket on Amazon S3, and linked to via `/download` links on other Beiwe deployments.

We use [Fastlane](https://docs.fastlane.tools/getting-started/android/setup/), running inside GitHub Actions, for building and distributing the app.

### Creating and distributing new builds
1. **Upgrade the app version**
   1. In `app/build.gradle`, increment `versionCode` (a sequential integer) and increase `versionName` (a traditional x.y.z format version number).
   2. In `DeviceInfo.java`, write a message about what changes the new version brings.
   3. Create a new commit on `master` that only does the above two things, and does not introduce any other changes. I like to use a commit message formatted exactly like this: `Updates app version to x.y.z, level XX`. Then, only make builds off of this commit. That way it's very clear what Git commit each build corresponds to. If you want to change something in the build, create a new version.
2. **Manually run the ["Build"](https://github.com/onnela-lab/beiwe-android/actions/workflows/build.yml) GitHub Action**- this will run Fastlane, executing two "lanes":
   1. `fastlane buildAAB` builds the AAB file and uploads it to Google Play's "Alpha" (closed testing) track.
   2. `fastlane buildAPKs` builds the two APK files (onnelaLabServer and commStatsCustomUrl) and uploads them to the S3 bucket.  It uploads two copies of each file:
      1. One copy of each APK gets saved with a quasi-permalink in this format: https://beiwe-app-backups.s3.amazonaws.com/release/Beiwe-x.y.z-commStatsCustomUrl-release.apk
      2. The other copy of each APK gets saved to the "latest" download link, with a format like this: https://beiwe-app-backups.s3.amazonaws.com/release/Beiwe-LATEST-commStatsCustomUrl.apk.  This "latest" link is what a Beiwe deployment's `/download` link should point to.
3. **In the [Google Play Console](https://play.google.com/apps/publish/), promote the Alpha release to production.**

#### Fastlane local setup
_You shouldn't need to do this,_ other than for debugging the Fastlane setup.  We intend Fastlane to be primarily run on the cloud, inside GitHub Actions.  But if for some reason you decide to run it locally, here's how to do it:
1. Follow [Fastlane's Android getting started documenation](https://docs.fastlane.tools/getting-started/android/setup/), including: install Ruby.  Then `gem install bundler`, and then  `bundle install`.
2. Make sure you have the following credentials, and that they're correct:
   1. The keystore inside this repo at `private/signing_keystore.jks`
   2. Your `private/keystore.properties` file, which should include `releaseDSN`
   3. Your Google Service Account credentials JSON file, which enables you to upload the Android App Bundle to Google Play.  Its location is defined in `faslane/Appfile`.
   4. AWS IAM credentials that allow you to upload an object to the bucket used for hosting APK files for download.  Your credentials should be exported as two environment variables called `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.
3. Run `fastlane buildAAB`
4. Run `fastlane buildAPKs`
