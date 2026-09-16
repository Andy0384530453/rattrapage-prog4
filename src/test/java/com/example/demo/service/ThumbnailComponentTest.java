package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ThumbnailComponentTest {

  private final ThumbnailComponent thumbnailComponent = new ThumbnailComponent();

  @Test
  void resize_png_to_256_by_256() throws Exception {
    var resizeResult = thumbnailComponent.resizeTo256(pngFile(800, 600));

    var resized = ImageIO.read(resizeResult);
    assertEquals(256, resized.getWidth());
    assertEquals(256, resized.getHeight());
  }

  @Test
  void resize_jpeg_to_256_by_256() throws Exception {
    var size = 480;
    var jpeg = File.createTempFile("source-", ".jpeg");
    var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, size, size);
    graphics.dispose();
    ImageIO.write(image, "jpeg", jpeg);

    var resizeResult = thumbnailComponent.resizeTo256(jpeg);

    var resized = ImageIO.read(resizeResult);
    assertEquals(256, resized.getWidth());
    assertEquals(256, resized.getHeight());
  }

  @Test
  void resize_non_image_throws() throws Exception {
    var notAnImage = File.createTempFile("not-an-image", ".txt");
    Files.write(notAnImage.toPath(), "not an image".getBytes(StandardCharsets.UTF_8));

    assertThrows(IllegalArgumentException.class, () -> thumbnailComponent.resizeTo256(notAnImage));
  }

  private File pngFile(int width, int height) throws Exception {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    var file = File.createTempFile("source-", ".png");
    ImageIO.write(image, "png", file);
    return file;
  }
}
