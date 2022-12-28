/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.augmentedimage.rendering;

import android.content.Context;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer.BlendMode;
import java.io.IOException;

/** Renders an augmented image. */
public class AugmentedImageRenderer {
  private static final String TAG = "AugmentedImageRenderer";

  private static final float TINT_INTENSITY = 0.1f;
  private static final float TINT_ALPHA = 1.0f;
  private static final int[] TINT_COLORS_HEX = {
    0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
    0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800,
  };

  private final ObjectRenderer sunRenderer = new ObjectRenderer();
  private final ObjectRenderer mercuryRenderer = new ObjectRenderer();
  private final ObjectRenderer venusRenderer = new ObjectRenderer();
  private final ObjectRenderer earthRenderer = new ObjectRenderer();
  private final ObjectRenderer marsRenderer = new ObjectRenderer();
  private final ObjectRenderer jupiterRenderer = new ObjectRenderer();
  private final ObjectRenderer saturnRenderer = new ObjectRenderer();
  private final ObjectRenderer uranusRenderer = new ObjectRenderer();
  private final ObjectRenderer neptuneRenderer = new ObjectRenderer();


  public AugmentedImageRenderer() {}

  public void createOnGlThread(Context context) throws IOException {

    sunRenderer.createOnGlThread(
            context, "models/sun/sun.obj", "models/sun/sun.jpg");
    sunRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    sunRenderer.setBlendMode(BlendMode.AlphaBlending);

    mercuryRenderer.createOnGlThread(
            context, "models/mercury/moon.obj", "models/mercury/enhancedcolor_over_completebasemap.png");
    mercuryRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    mercuryRenderer.setBlendMode(BlendMode.AlphaBlending);

    venusRenderer.createOnGlThread(
        context, "models/venus/jupiter.obj", "models/venus/venmap.jpg");
    venusRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    venusRenderer.setBlendMode(BlendMode.AlphaBlending);

    earthRenderer.createOnGlThread(
        context, "models/earth/earth.obj", "models/earth/tierra1.jpg");
    earthRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    earthRenderer.setBlendMode(BlendMode.AlphaBlending);

    marsRenderer.createOnGlThread(
        context, "models/mars/planet.obj", "models/mars/8k_mars.jpg");
    marsRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    marsRenderer.setBlendMode(BlendMode.AlphaBlending);

    jupiterRenderer.createOnGlThread(
        context, "models/jupiter/jupiter.obj", "models/jupiter/Jupiter.jpeg");
    jupiterRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    jupiterRenderer.setBlendMode(BlendMode.AlphaBlending);

    saturnRenderer.createOnGlThread(
            context, "models/saturn/scene.gltf", "models/saturn/texture.jpeg");
    saturnRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    saturnRenderer.setBlendMode(BlendMode.AlphaBlending);

    uranusRenderer.createOnGlThread(
            context, "models/uranus/Uranus.obj", "models/uranus/Uranus.jpg");
    uranusRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    uranusRenderer.setBlendMode(BlendMode.AlphaBlending);

    neptuneRenderer.createOnGlThread(
            context, "models/neptune/moon.obj", "models/neptune/neptune2k.jpg");
    neptuneRenderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
    neptuneRenderer.setBlendMode(BlendMode.AlphaBlending);

  }

  // Adjust size of detected image and render it on-screen
  public void draw(
          float[] viewMatrix,
          float[] projectionMatrix,
          AugmentedImage augmentedImage,
          Anchor centerAnchor,
          float[] colorCorrectionRgba) {
    float[] tintColor =
            convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);

    final float mazeEdgeSize = 2; // Magic number of maze size
    final float maxImageEdgeSize = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size

    Pose anchorPose = centerAnchor.getPose();

    float mazeScaleFactor = maxImageEdgeSize / mazeEdgeSize; // scale to set Maze to image size
    float[] modelMatrix = new float[16];

    // OpenGL Matrix operation is in the order: Scale, rotation and Translation
    // So the manual adjustment is after scale
    // The 251.3f and 129.0f is magic number from the maze obj file
    // We need to do this adjustment because the maze obj file
    // is not centered around origin. Normally when you
    // work with your own model, you don't have this problem.
    Pose mazeModelLocalOffset = Pose.makeTranslation(0.0f, -0.1f, 0.0f); // x ist horitzontal -links +rechts, y ist tiefe -ferner +näher, z vertikal -oben +unten
    anchorPose.compose(mazeModelLocalOffset).toMatrix(modelMatrix, 0);

    switch (augmentedImage.getName()) {
      case "Jupiter.jpg":
        jupiterRenderer.updateModelMatrix(modelMatrix, maxImageEdgeSize / 2); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        jupiterRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "venus.jpg":
        venusRenderer.updateModelMatrix(modelMatrix, maxImageEdgeSize / 2); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        venusRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "erde.jpg":
        earthRenderer.updateModelMatrix(modelMatrix, maxImageEdgeSize / 2); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        earthRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "mars.jpg":
        marsRenderer.updateModelMatrix(modelMatrix, maxImageEdgeSize / 2); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        marsRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
    }

  }

  public void drawPlanet(
          float[] viewMatrix,
          float[] projectionMatrix,
          Anchor centerAnchor,
          float[] colorCorrectionRgba, String planet) {
    float[] tintColor = convertHexToColor(TINT_COLORS_HEX[0 % TINT_COLORS_HEX.length]);

    final float mazeEdgeSize = 2; // Magic number of maze size
    //final float maxImageEdgeSize = Math.max(augmentedImage.getExtentX(), augmentedImage.getExtentZ()); // Get largest detected image edge size

    Pose anchorPose = centerAnchor.getPose();

    //float mazeScaleFactor = maxImageEdgeSize / mazeEdgeSize; // scale to set Maze to image size
    float[] modelMatrix = new float[16];

    // OpenGL Matrix operation is in the order: Scale, rotation and Translation
    // So the manual adjustment is after scale
    // The 251.3f and 129.0f is magic number from the maze obj file
    // We need to do this adjustment because the maze obj file
    // is not centered around origin. Normally when you
    // work with your own model, you don't have this problem.
    Pose mazeModelLocalOffset = Pose.makeTranslation(0.0f, -0.1f, 0.0f); // x ist horitzontal -links +rechts, y ist tiefe -ferner +näher, z vertikal -oben +unten
    anchorPose.compose(mazeModelLocalOffset).toMatrix(modelMatrix, 0);


    switch (planet) {
      case "SONNE":
        System.out.println("Here comes the sun");
        sunRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        sunRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "MERKUR":
        System.out.println("Here comes the mercury");
        mercuryRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        mercuryRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "VENUS":
        System.out.println("Here comes the venus");
        venusRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        venusRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "ERDE":
        System.out.println("Here comes the earth");
        earthRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        earthRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "MARS":
        System.out.println("Here comes the mars");
        marsRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        marsRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "JUPITER":
        System.out.println("Here comes the jupiter");
        jupiterRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        jupiterRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "SATURN":
        System.out.println("Here comes the saturn");
        saturnRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        saturnRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "URANUS":
        System.out.println("Here comes the uranus");
        uranusRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        uranusRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;
      case "NEPTUN":
        System.out.println("Here comes the neptune");
        neptuneRenderer.updateModelMatrix(modelMatrix, 0.1F); // This line relies on a change in ObjectRenderer.updateModelMatrix later in this codelab.
        neptuneRenderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
        break;

    }


  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[] {red, green, blue, TINT_ALPHA};
  }
}
