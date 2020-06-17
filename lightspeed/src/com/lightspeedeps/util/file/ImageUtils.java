package com.lightspeedeps.util.file;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Image;
import com.lightspeedeps.util.app.Constants;

public class ImageUtils {
	private static Log log = LogFactory.getLog(ImageUtils.class);

	private final static byte[] EMPTY_THUMB_NAIL = null; // { 'x' };

	private ImageUtils() {
	}

	/**
	 * Possibly reduce the size of the image passed, and optionally create a
	 * thumb-nail image to match.
	 *
	 * @param image The Image object containing an image to be manipulated.
	 * @throws IOException
	 */
	public static void resizeImage(Image image) throws IOException {
		int pMaxEdge = Constants.MAX_THUMBNAIL_SIZE;
		byte[] newcontent;
		// Create an ImageIcon from the image data
		ImageIcon imageIcon = new ImageIcon(image.getContent());
		int width = imageIcon.getIconWidth();
		int height = imageIcon.getIconHeight();
		if (width < 0 || height < 0) {
			log.debug("imageIcon invalid, attempting conversion, len=" + image.getContent().length);
			newcontent = convertOtherToJPG(image.getContent()); // handles BMP
			if (newcontent == null) {
				// unable to convert; leave it as-is, and without a thumb-nail
				image.setThumbnail(EMPTY_THUMB_NAIL);
				return;
			}
			image.setContent(newcontent);
			log.debug("converted to JPG, len=" + image.getContent().length);
			imageIcon = new ImageIcon(image.getContent());
			width = imageIcon.getIconWidth();
			height = imageIcon.getIconHeight();
		}
		log.debug("image width=" + width + ", ht=" + height);

		// If the full image is too big, we shrink it before storing it.
		int len1 = image.getContent().length;
		if (len1 > Constants.TARGET_IMAGE_FILESIZE) {
			// first ensure it's JPG & reasonably compressed...
			newcontent = resizeAsJPG(imageIcon, width, height);
			int len2 = newcontent.length;
			if (len2 > Constants.TARGET_IMAGE_FILESIZE) {
				double ratio = (double)len2 / Constants.TARGET_IMAGE_FILESIZE;
				ratio = Math.sqrt(ratio);
				height = (int)(height / ratio);
				width = (int)(width / ratio);
				newcontent = resizeAsJPG(imageIcon, width, height);
			}
			image.setContent(newcontent);
			log.debug("image length shrunk from " + len1 + " to " + newcontent.length);
			log.debug("new width=" + width + ", ht=" + height);
		}

		// If the image is larger than the thumb-nail max width or height, we need to
		// generate a smaller image to use for thumb-nails.
		boolean resize = false;
		if (width > pMaxEdge) {
			// Determine the shrink ratio
			double ratio = (double)pMaxEdge / width;
			height = (int)(height * ratio);
			width = pMaxEdge;
			log.debug("resize ratio: " + ratio + ", post scale width: " + width + " height: " + height);
			resize = true;
		}
		if (height > pMaxEdge) {
			// Determine the shrink ratio
			double ratio = (double)pMaxEdge / height;
			width = (int)(width * ratio);
			height = pMaxEdge;
			log.debug("resize ratio: " + ratio + ", post scale width: " + width + " height: " + height);
			resize = true;
		}

		byte[] thumb = null;
		if (resize) {
			thumb = resizeAsJPG(imageIcon, width, height);
		}
		if (thumb != null) { // successful resize
			len1 = thumb.length;
			if (len1 >= image.getContent().length) { // got bigger?!
				image.setThumbnail(EMPTY_THUMB_NAIL);
			}
			else {
				image.setThumbnail(thumb);
			}
		}
		else {
			image.setThumbnail(EMPTY_THUMB_NAIL);
		}
		return;
	}

	/**
	 * Take the image in the imageIcon object and convert it to a JPG of the
	 * specified width and height. The conversion should work if the original
	 * input was a JPG, PNG, GIF, and possibly other types.
	 *
	 * @param imageIcon The image to convert.
	 * @param width The width of the target image.
	 * @param height The height of the target image.
	 * @return A byte array of the JPG data for the resized image.
	 * @throws IOException
	 */
	private static byte[] resizeAsJPG(ImageIcon imageIcon, int width, int height)
			throws IOException {
		// Create a new empty image buffer to "draw" the resized image into
		BufferedImage bufferedResizedImage = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		// Create a Graphics object to do the "drawing"
		Graphics2D g2d = bufferedResizedImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// We fill the space with white first, so any transparent pixels will end up white;
		// without this, they ended up black in the thumb-nails.
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);
		// Draw the resized image
		g2d.drawImage(imageIcon.getImage(), 0, 0, width, height, null);
		g2d.dispose();

		// Now our buffered image is ready
		// Encode it as a JPEG
		ByteArrayOutputStream encoderOutputStream = new ByteArrayOutputStream();

		boolean b = ImageIO.write(bufferedResizedImage, "jpg", encoderOutputStream);
		encoderOutputStream.close();
		if (! b) {
			log.error("JPG write failed");
			return null;
		}

		return encoderOutputStream.toByteArray();
	}

	/**
	 * Attempt to convert an image that did not load directly (into an
	 * ImageIcon) into a JPG file.  This works for BMP files, but not
	 * for TIFFs!
	 * @param content The image content (uploaded file).
	 * @return The JPG image as a byte array.
	 * @throws IOException
	 */
	public static byte[] convertOtherToJPG(byte[] content) {
		ByteArrayOutputStream output;
		try {
			InputStream input = new ByteArrayInputStream(content);
			BufferedImage image = ImageIO.read(input);
			if (image == null) {
				//String[] names = ImageIO.getReaderFormatNames();
				log.error("unable to convert input file to JPG");
				return null;
			}
			output = new ByteArrayOutputStream();
			ImageIO.write(image, "JPG", output);
			output.close();
		}
		catch (Exception e) {
			log.error("unable to convert input file to JPG");
			return null;
		}
		return output.toByteArray();
	}

	/*
	 * Convenience method that returns a scaled instance of the provided
	 * {@code BufferedImage}.
	 *
	 * @param img the original image to be scaled
	 * @param targetWidth the desired width of the scaled instance, in pixels
	 * @param targetHeight the desired height of the scaled instance, in pixels
	 * @param hint one of the rendering hints that corresponds to
	 *            {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality if true, this method will use a multi-step scaling
	 *            technique that provides higher quality than the usual one-step
	 *            technique (only useful in downscaling cases, where
	 *            {@code targetWidth} or {@code targetHeight} is smaller than
	 *            the original dimensions, and generally only when the
	 *            {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 *
	 *         from
	 *         http://today.java.net/pub/a/today/2007/04/03/perils-of-image-
	 *         getscaledinstance.html
	 */
//	public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
//			int targetHeight, Object hint, boolean higherQuality) {
//		int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
//				: BufferedImage.TYPE_INT_ARGB;
//		BufferedImage ret = (BufferedImage)img;
//		int w, h;
//		if (higherQuality) {
//			// Use multi-step technique: start with original size, then
//			// scale down in multiple passes with drawImage()
//			// until the target size is reached; avoids quality loss.
//			w = img.getWidth();
//			h = img.getHeight();
//		}
//		else {
//			// Use one-step technique: scale directly from original
//			// size to target size with a single drawImage() call
//			w = targetWidth;
//			h = targetHeight;
//		}
//		do {
//			if (higherQuality && w > targetWidth) {
//				w /= 2;
//				if (w < targetWidth) {
//					w = targetWidth;
//				}
//			}
//			if (higherQuality && h > targetHeight) {
//				h /= 2;
//				if (h < targetHeight) {
//					h = targetHeight;
//				}
//			}
//			BufferedImage tmp = new BufferedImage(w, h, type);
//			Graphics2D g2 = tmp.createGraphics();
//			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
//			g2.drawImage(ret, 0, 0, w, h, null);
//			g2.dispose();
//			ret = tmp;
//		} while (w != targetWidth || h != targetHeight);
//
//		return ret;
//	}

}
