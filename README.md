Night Light is a blue light filter app that is designed specifically to use Android Nougat's native night mode functionality, for accurate filtering that doesn't affect contrast or other dark colors.

## Features
* Easily toggle night mode on or off
* Can start/stop automatically based on predefined times or your location's sunset/sunrise
* Blacklist feature to turn Night Light off while using certain apps
* Includes Quick Settings tile for convenient access
* Tasker integration

## Download
* Google Play (https://play.google.com/store/apps/details?id=com.farmerbb.nightlight)

## How to Build
Prerequisites:
* Windows, Mac, or Linux machine
* JDK 8
* Internet connection (to download dependencies)

Once all the prerequisites are met, simply cd to the base directory of the project and run "./gradlew assembleDebug" to start the build.  Dependencies will download and the build will run.  After the build completes, cd to "app/build/outputs/apk" where you will end up with the APK file "app-debug.apk", ready to install on your Android device.
