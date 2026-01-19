# BA_Demo

An Android augmented reality application developed as part of a bachelor thesis project. This app enhances the signs of the "Planetenwanderweg Karlsaue" (Karlsaue Planetary Trail) with augmented reality using Google ARCore, OpenCV, and ML Kit.

## Overview

BA_Demo is an AR-enabled Android application that recognizes rectangular signs containing text and overlays them with augmented reality content. The app is specifically designed to work with planetary information signs along the Karlsaue Planetary Trail.

The application combines several technologies:
- **Google ARCore** for augmented reality capabilities
- **OpenCV** for rectangle detection in camera frames
- **ML Kit** for optical character recognition (OCR) to detect planet names
- **Android Camera API** for real-time image capture and processing

## Features

- Real-time rectangle detection using OpenCV
- Text recognition using ML Kit OCR
- AR overlay rendering on detected signs
- Support for all nine planets: Sonne (Sun), Merkur, Venus, Erde, Mars, Jupiter, Saturn, Uranus, and Neptun
- Configurable camera resolution (low, medium, high)
- Performance tracking and logging

## Requirements

### Hardware
- Android device with ARCore support
- Device must have a camera with AR capabilities

### Software
- Android SDK 24 (Android 7.0) or higher
- Target SDK: 33 (Android 13)
- ARCore installed on the device

## Technologies Used

- **ARCore 1.34.0** - Google's platform for building augmented reality experiences
- **OpenCV 4.5.1** - Computer vision library for rectangle detection
- **ML Kit Text Recognition** - Google's machine learning library for OCR
- **Glide 4.6.1** - Image loading and caching library
- **Obj loader 0.2.1** - 3D model loading for AR objects

## Project Structure

```
BA_Demo/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/ba/thesis/demo/
│   │       │   ├── augmentedimage/
│   │       │   │   ├── AugmentedImageActivity.java  # Main activity
│   │       │   │   ├── ImageConverter.java          # Image conversion utilities
│   │       │   │   ├── RectangleDetector.java       # OpenCV rectangle detection
│   │       │   │   └── rendering/
│   │       │   │       └── AugmentedImageRenderer.java
│   │       │   └── common/
│   │       │       ├── helpers/                     # ARCore helper classes
│   │       │       └── rendering/                   # Rendering utilities
│   │       ├── res/                                 # Android resources
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── opencv/                                          # OpenCV module
├── build.gradle
└── settings.gradle
```

## Building the Project

### Prerequisites

1. Install [Android Studio](https://developer.android.com/studio)
2. Install Android SDK with API level 33
3. Ensure you have JDK 8 or higher

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/EricRode/BA_Demo.git
   cd BA_Demo
   ```

2. Open the project in Android Studio

3. Sync Gradle files and download dependencies

4. Build the project:
   ```bash
   ./gradlew build
   ```

5. Connect an ARCore-compatible Android device

6. Run the application:
   ```bash
   ./gradlew installDebug
   ```

## Usage

1. Launch the application on an ARCore-compatible device
2. Grant camera permissions when prompted
3. Point the camera at a planetary trail sign
4. The app will automatically:
   - Detect the rectangular sign
   - Recognize the planet name using OCR
   - Overlay AR content on the sign
5. The AR overlay will track the sign as you move the device

## Permissions

The application requires the following permissions:
- `CAMERA` - Required for AR functionality and image capture

## Dependencies

The project uses the following major dependencies:
- `com.google.ar:core:1.34.0` - ARCore library
- `de.javagl:obj:0.2.1` - Wavefront OBJ file loader
- `com.github.bumptech.glide:glide:4.6.1` - Image loading library
- `com.google.mlkit:vision-common:17.2.1` - ML Kit vision common
- `com.google.android.gms:play-services-mlkit-text-recognition:18.0.2` - ML Kit text recognition
- `androidx.camera:camera-core:1.1.0` - AndroidX Camera library
- OpenCV 4.5.1 (included as a module)

## License

Portions of this code are adapted from:
- Google ARCore Augmented Images CodeLab
- Google ARCore HelloAR sample

Original Google code is licensed under the Apache License, Version 2.0:
```
Copyright 2016 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Author

Developed as part of a bachelor thesis project.

## Troubleshooting

### ARCore Not Installed
If you receive an error about ARCore not being installed:
1. Open Google Play Store on your device
2. Search for "Google Play Services for AR"
3. Install or update the app

### Device Not Compatible
Check if your device is ARCore compatible at:
https://developers.google.com/ar/devices

### Camera Permission Denied
The app requires camera permission to function. Grant the permission in device settings if denied.

## Related Projects

This project implements the OpenCV solution for rectangle detection. There is also another version that uses ARCore's augmented images solution.

## Support

For issues, questions, or contributions, please refer to the GitHub repository.
