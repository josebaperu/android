# Android-Webview-App
A Simple To Use App That Shows Web Pages Within The App And Download Content Using Phone's Internal Downloader

[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)

### Screenshot
<img src="https://raw.githubusercontent.com/BishwasSagar/Android-Webview-App/master/screenshot_demo.png" width="416" height="720">

### Demo

Download here : [Demo Apk](https://github.com/BishwasSagar/Android-Webview-App/raw/master/demo.apk)

# Getting Started

[Download](https://github.com/BishwasSagar/Android-Webview-App/archive/refs/heads/master.zip) or clone this repository and import it into Android Studio.

### Build requirements
| | |
|---|---|
| Android Gradle Plugin | 9.3.0 |
| Gradle | 9.7.1 |
| JDK | 17 or newer (21 recommended) |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |

## Change Website URL
Open the ```app/src/main/java/com/webview/youtube/MainActivity.java``` file and replace the `BASE_URL` constant with your website
```java
private final static String BASE_URL = "https://www.youtube.com/";
```
The last visited page is persisted, so clear the app's storage after changing it.
