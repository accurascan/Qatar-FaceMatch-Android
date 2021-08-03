package com.accurascan.facematch.util;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import androidx.annotation.Keep;

public class BitmapHelper {
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;

	    if (height > reqHeight || width > reqWidth) {

	        // Calculate ratios of height and width to requested height and
	        // width
	        final int heightRatio = Math.round((float) height
	                / (float) reqHeight);
	        final int widthRatio = Math.round((float) width / (float) reqWidth);

	        // Choose the smallest ratio as inSampleSize value, this will
	        // guarantee
	        // a final image with both dimensions larger than or equal to the
	        // requested height and width.
	        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
	    }

	    return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(res, resId, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth,
	            reqHeight);
 
	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static Bitmap decodeSampledBitmapFromData(byte data[], int reqWidth, int reqHeight, Rect orgRect) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeByteArray(data, 0, data.length, options);

	    if (orgRect != null) {
	    	orgRect.set(0, 0, options.outWidth, options.outHeight);
	    }
	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeByteArray(data, 0, data.length);
//	    return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}
	
	public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight, Rect orgRect) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(filePath, options);

	    if (orgRect != null) {
	    	orgRect.set(0, 0, options.outWidth, options.outHeight);
	    }
	    
	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeFile(filePath, options);
//	    return BitmapFactory.decodeFile(filePath);
	}
	
	public static Bitmap decodeSampledBitmapFromURL(String urlString, int reqWidth, int reqHeight, Rect orgRect) throws IOException {
		URL url = new URL(urlString);
	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    //BitmapFactory.decodeStream(is)
	    BitmapFactory.decodeStream(url.openStream(), null, options);

	    if (orgRect != null) {
	    	orgRect.set(0, 0, options.outWidth, options.outHeight);
	    }
	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeStream(url.openStream(), null, options);
	}

	@Keep
	public static Bitmap createFromARGB(byte[] buffer, int width, int height) {
		Bitmap bmp;
		bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bmp.copyPixelsFromBuffer(ByteBuffer.wrap(buffer));
		return bmp;
	}
}
