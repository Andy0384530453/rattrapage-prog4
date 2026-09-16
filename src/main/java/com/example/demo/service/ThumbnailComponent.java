package com.example.demo.service;

import static java.io.File.createTempFile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailComponent {

  public static final int THUMBNAIL_SIZE = 256;

  public File resizeTo256(File source) throws IOException {
    var sourceImage = ImageIO.read(source);
    if (sourceImage == null) {
      throw new IllegalArgumentException("Provided file is not a readable image: " + source);
    }
    return write(resize(sourceImage));
  }

  private BufferedImage resize(BufferedImage sourceImage) {
    var thumbnail = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = thumbnail.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(sourceImage, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
    graphics.dispose();
    return thumbnail;
  }

  private File write(BufferedImage thumbnail) throws IOException {
    var output = createTempFile("thumbnail-", ".png");
    var written = ImageIO.write(thumbnail, "png", output);
    if (!written) {
      throw new IOException("Unable to write thumbnail as PNG");
    }
    return output;
  }
}
