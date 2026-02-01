package org.apache.roller.weblogger.business.jpa;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.FileContentManager;
import org.apache.roller.weblogger.pojos.FileContent;
import org.apache.roller.weblogger.pojos.MediaFile;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ThumbnailService {
    private final FileContentManager contentManager;
    private final JPAPersistenceStrategy strategy;

    public ThumbnailService(FileContentManager contentManager,
                            JPAPersistenceStrategy strategy) {
        this.contentManager = contentManager;
        this.strategy = strategy;
    }

    public void createAndSaveThumbnail(MediaFile mediaFile) throws Exception {
        // Step 1: Load the original image
        BufferedImage originalImage = loadImage(mediaFile);

        // Step 2: Update dimensions in database
        saveDimensions(mediaFile, originalImage);

        // Step 3: Create thumbnail
        BufferedImage thumbnail = createThumbnail(originalImage, mediaFile);

        // Step 4: Save thumbnail to storage
        saveThumbnailToStorage(mediaFile, thumbnail);
    }

    private BufferedImage loadImage(MediaFile mediaFile) throws Exception {
        FileContent content = contentManager.getFileContent(
                mediaFile.getWeblog(),
                mediaFile.getId()
        );
        return ImageIO.read(content.getInputStream());
    }

    private void saveDimensions(MediaFile mediaFile, BufferedImage image) throws WebloggerException {
        mediaFile.setWidth(image.getWidth());
        mediaFile.setHeight(image.getHeight());
        strategy.store(mediaFile);
    }

    private BufferedImage createThumbnail(BufferedImage original, MediaFile mediaFile) {
        int thumbWidth = mediaFile.getThumbnailWidth();
        int thumbHeight = mediaFile.getThumbnailHeight();

        // Scale the image
        Image scaledImage = original.getScaledInstance(
                thumbWidth, thumbHeight, Image.SCALE_SMOOTH
        );

        // Convert to BufferedImage
        BufferedImage thumbnail = new BufferedImage(
                thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = thumbnail.createGraphics();
        g2.drawImage(scaledImage, 0, 0, thumbWidth, thumbHeight, null);
        g2.dispose();

        return thumbnail;
    }

    private void saveThumbnailToStorage(MediaFile mediaFile, BufferedImage thumbnail)
            throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "png", outputStream);

        contentManager.saveFileContent(
                mediaFile.getWeblog(),
                mediaFile.getId() + "_sm",
                new ByteArrayInputStream(outputStream.toByteArray())
        );
    }
}