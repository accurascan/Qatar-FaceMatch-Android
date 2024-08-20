package com.inet.facelock.callback;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class FaceLockHelper {
    public static final int ARGB_MODE = 1;
    public static final int YUV420_MODE = 0;
    public static int IDENTIFY_THRETHOLD = 70;

    static {
        try { // for facematch
            System.loadLibrary("accurafacem");
            Log.e(FaceLockHelper.class.getSimpleName(), "static initializer: accurafacem" );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This is the function that initialize the face engine
     * This must be called when app is started.
     *
     * @param callback    callback to get the result
     * @param fmin        minimum distance between 2 eyes.
     * @param fmax        maximum distance between 2 eyes.
     * @param resizeRate scale factor to detect faces.
     *                    default value is 1.18.
     *                    the smaller, the slower
     * @param modelpath   deep CNN model file's path.
     * @param weightpath  deep CNN weight file's path.
     * @return result
     */
    public native int InitEngine(Context callback, int fmin, int fmax, float resizeRate, String modelpath, String weightpath, AssetManager assets, byte[] licBuff, int pLicLen1);
    /**
     * This is the function that finalize the face engine when app is closed.
     *
     * @return
     */
    public native boolean CloseEngine();

    /**
     * This is the function to detect faces.
     *
     * @param vBmp face image buffer
     *             its format is RGBA or YUV420
     */
    public native void DetectLeftFace(byte[] vBmp, int width, int height, FaceDetectionResult result);

    public native void DetectRightFace(byte[] vBmp, int width, int height, float[] feature, FaceDetectionResult result);


    /**
     * This is the function to extract feature from a face.
     *
     * @param vBmp       face image buffer
     *                   its format is RGBA or YUV420
     * @param pfaceRect  int array of 4 points' coordinates of face region.
     * @param plandmarks float array of face landmarks' coordinates
     *                   Look at the code in real use.
     */
    public native void Extractfeatures(byte[] vBmp, int width, int height, float[] plandmarks, int imgType);

    /**
     * This is the function to calculate the similarity of 2 face feature vectors.
     */
    public native float Similarity(float[] vFeat1, float[] vFeat2, int length);

}
