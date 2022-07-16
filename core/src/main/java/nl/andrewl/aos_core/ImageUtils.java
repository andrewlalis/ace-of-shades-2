package nl.andrewl.aos_core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtils {
	public static ByteBuffer decodePng(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		// Load texture contents into a byte buffer
		ByteBuffer buf = ByteBuffer.allocateDirect(4 * width * height );

		// decode image
		// ARGB format to -> RGBA
		for( int h = 0; h < height; h++ )
			for( int w = 0; w < width; w++ ) {
				int argb = image.getRGB( w, h );
				buf.put( (byte) ( 0xFF & ( argb >> 16 ) ) );
				buf.put( (byte) ( 0xFF & ( argb >> 8 ) ) );
				buf.put( (byte) ( 0xFF & ( argb ) ) );
				buf.put( (byte) ( 0xFF & ( argb >> 24 ) ) );
			}
		buf.flip();
		return buf;
	}
}
