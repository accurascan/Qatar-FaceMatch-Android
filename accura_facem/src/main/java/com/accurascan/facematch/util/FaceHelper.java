package com.accurascan.facematch.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Keep;

import com.accurascan.facematch.R;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.inet.facelock.callback.FaceCallback;
import com.inet.facelock.callback.FaceDetectionResult;
import com.inet.facelock.callback.FaceLockHelper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FaceHelper extends FaceLockHelper{
    private final Context context;
    public FaceDetectionResult leftResult = null;
    public FaceDetectionResult rightResult = null;
    public float match_score = 0.0f;
    public Bitmap face2 = null;
    public Bitmap face1 = null;
    private final Activity activity;
    private final FaceMatchCallBack faceMatchCallBack;
    private final FaceCallback faceCallback;
    private String serverUrl = "";
    private String serverKey = "";
    private String livenessId = "";
    private boolean isSend = false;

    @Keep
    public void setApiData(String serverUrl, String serverKey, String livenessId) {
        this.serverUrl = serverUrl;
        this.serverKey = serverKey;
        this.livenessId = livenessId;
    }

    /**
     * override method to get face detect on input image and match image and
     * face match score between input and match image.
     *
     */
    @Keep
    public interface FaceMatchCallBack {

        /**
         * This is called after face match.
         * @param ret
         */
        void onFaceMatch(float ret);

        /**
         * This is callback function to get bitmap.
         * @param src
         */
        void onSetInputImage(Bitmap src);
        void onSetMatchImage(Bitmap src);

    }

    @Keep
    public FaceHelper(Activity activity) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        if (activity instanceof FaceCallback) {
            this.faceCallback = (FaceCallback) activity;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement " + FaceCallback.class.getName());
        }
        if (activity instanceof FaceMatchCallBack) {
            this.faceMatchCallBack = (FaceMatchCallBack) activity;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement "+ FaceMatchCallBack.class.getName());
        }
//        this.faceMatchCallBack = faceMatchCallBack;

        initEngine();
    }

    //initialize the engine
    @Keep
    public void initEngine() {

        //call Sdk  method InitEngine
        // parameter to pass : FaceCallback callback, int fmin, int fmax, float resizeRate, String modelpath, String weightpath, AssetManager assets
        // this method will return the integer value
        //  the return value by initEngine used the identify the particular error
        // -1 - No key found
        // -2 - Invalid Key
        // -3 - Invalid Platform
        // -4 - Invalid License

        writeFileToPrivateStorage(R.raw.model, "model.prototxt"); //write file to private storage
        File modelFile = context.getFileStreamPath("model.prototxt");
        String pathModel = modelFile.getPath();
        writeFileToPrivateStorage(R.raw.weight, "weight.dat");
        File weightFile = context.getFileStreamPath("weight.dat");
        String pathWeight = weightFile.getPath();

        int nRet = InitEngine(this.context, 30, 800, 1.18f, pathModel, pathWeight, context.getAssets(), new byte[0], 0);
        if (nRet < 0) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
            if (nRet == -1) {
                builder1.setMessage("No Key Found");
            } else if (nRet == -2) {
                builder1.setMessage("Invalid Key");
            } else if (nRet == -3) {
                builder1.setMessage("Invalid Platform");
            } else if (nRet == -4) {
                builder1.setMessage("Invalid License");
            }

            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();

                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
        if (faceCallback!=null) {
            faceCallback.onInitEngine(nRet);
        }
    }

    /**
     * This is the function to get Face match score.
     * pass two image uri to detect face and get face match score between two images.
     * and get data on {@link FaceMatchCallBack}
     *
     * @param uri1 to pass for detect face from image uri.
     * @param uri2 to pass for detect face from image uri.
     */
    @Keep
    public void getFaceMatchScore(Uri uri1, Uri uri2) {
        if (uri1 != null && uri2 != null) {
            getFaceMatchScore(FileUtils.getPath(activity, uri1), FileUtils.getPath(activity, uri2));
        } else {
            throw new NullPointerException("uri1 & uri2 cannot be null");
        }
    }

    /**
     * This is the function to get Face match score.
     * pass two image file to detect face and get face match score between two images.
     *
     * @param file1 to pass for detect face from image file.
     * @param file2 to pass for detect face from image file.
     */
    @Keep
    public void getFaceMatchScore(File file1, File file2) {
        if (file1 != null && file2 != null) {
            getFaceMatchScore(file1.getAbsolutePath(), file2.getAbsolutePath());
        } else {
            throw new NullPointerException("file1 & file2 cannot be null");
        }
    }

    /**
     * This is the function to get face from image file.
     *
     * @param inputFile pass image file to detect fce from image.
     */
    @Keep
    public void setInputImage(File inputFile) {
        if (inputFile != null) {
            setInputImage(inputFile.getAbsolutePath());
        } else {
            throw new NullPointerException("inputFile cannot be null");
        }
    }

    /**
     * This is the function to get face from image file uri.
     *
     * @param fileUri pass image uri to detect face from image.
     */
    @Keep
    public void setInputImage(Uri fileUri) {
        if (fileUri != null) {
            setInputImage(FileUtils.getPath(activity, fileUri));
        } else {
            throw new NullPointerException("fileUri cannot be null");
        }
    }

    /**
     * This is the function to get face from image path.
     *
     * @param inputPath pass image path to detect face from image.
     */
    @Keep
    public void setInputImage(String inputPath) {
        if (inputPath != null) {
            face1 = getBitmap(inputPath);
            if (faceMatchCallBack != null) {
                if (face1 != null) {
                    faceMatchCallBack.onSetInputImage(face1);
                }
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement " + FaceMatchCallBack.class.getName());
            }
        } else {
            throw new NullPointerException("inputPath cannot be null");
        }
        leftResult = null;
        isSend = false;
//        if (face1 != null && face2 != null) {
        startFaceMatch(false);
//        }
    }

    /**
     * This is the function to get face from image path.
     *
     * @param bitmap pass bitmap to detect face from image.
     */
    @Keep
    public void setInputImage(Bitmap bitmap) {
        if (bitmap != null) {
            face1 = bitmap;
            if (faceMatchCallBack != null) {
                faceMatchCallBack.onSetInputImage(face1);
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement " + FaceMatchCallBack.class.getName());
            }
        } else {
            throw new NullPointerException("bitmap cannot be null");
        }
        leftResult = null;
        isSend = false;
//        if (face1 != null && face2 != null) {
        startFaceMatch(false);
//        }
    }

    /**
     * This is the function to get face from image path.
     *
     * to match face between two images then must have to call {@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}.
     *
     * @param matchFile pass image file
     */
    @Keep
    public void setMatchImage(File matchFile) {
        if (matchFile != null) {
            setMatchImage(matchFile.getAbsolutePath());
        } else {
            throw new NullPointerException("matchFile cannot be null");
        }
    }

    /**
     * This is the function to get face from image uri.
     *
     * to match face between two images then must have to call{@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}.
     *
     * @param fileUri pass image uri
     */
    @Keep
    public void setMatchImage(Uri fileUri) {
        if (fileUri != null) {
            setMatchImage(FileUtils.getPath(activity, fileUri));
        } else {
            throw new NullPointerException("fileUri cannot be null");
        }
    }

    /**
     * This is the function to get face from image uri.
     *
     * to match face between two images then must have to call {@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}.
     *
     * @param matchPath pass image path
     */
    @Keep
    public void setMatchImage(String matchPath) {
//        if (face1 == null) {
//            throw new RuntimeException(context.toString() + " Please set Input image First");
//        }
        if (matchPath != null) {
            face2 = getBitmap(matchPath);
            if (faceMatchCallBack != null) {
                if (face2 != null) {
                    faceMatchCallBack.onSetMatchImage(face2);
                }
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement " + FaceMatchCallBack.class.getName());

            }
        } else {
            throw new NullPointerException("matchPath cannot be null");
        }
        rightResult = null;
        isSend = true;
        startFaceMatch(true);
    }

    /**
     * This is the function to get face from bitmap.
     *
     * to match face between two images then must have to call {@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}
     *
     * @param bitmap pass bitmap
     */
    @Keep
    public void setMatchImage(Bitmap bitmap) {
//        if (face1 == null) {
//            throw new RuntimeException(context.toString() + " Please set Input image First");
//        }
        if (bitmap != null) {
            face2 = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            if (faceMatchCallBack != null) {
                if (face2 != null) {
                    faceMatchCallBack.onSetMatchImage(face2);
                }
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement " + FaceMatchCallBack.class.getName());

            }
        } else {
            throw new NullPointerException("matchPath cannot be null");
        }
        rightResult = null;
        isSend = true;
        startFaceMatch(true);
    }

    private Bitmap getBitmap(String path) {
        if (path != null) {
            Bitmap bmp1 = rotateImage(path);
            if (bmp1 != null) {
                return bmp1.copy(Bitmap.Config.ARGB_8888, true);
            }
            return null;
        } else {
            throw new NullPointerException("path cannot be null");
        }
    }

    /**
     * This is the function to get Face match score.
     * pass two image path to detect face and get face match score between two images.
     *
     * @param path1 to pass for detect face from image path.
     * @param path2 to pass for detect face from image path.
     */
    @Keep
    public void getFaceMatchScore(String path1, String path2) {
        if (path1 != null && path2 != null) {
            face1 = getBitmap(path1);
            face2 = getBitmap(path2);
            if (faceMatchCallBack != null) {
                faceMatchCallBack.onSetInputImage(face1);
                faceMatchCallBack.onSetMatchImage(face2);
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement " + FaceMatchCallBack.class.getName());
            }
            startFaceMatch(true);
        } else {
            throw new NullPointerException("path1 & path2 cannot be null");
        }
    }

    private void writeFileToPrivateStorage(int fromFile, String toFile) {
        InputStream is = context.getResources().openRawResource(fromFile);
        int bytes_read;
        byte[] buffer = new byte[4096];
        try {
            FileOutputStream fos = context.openFileOutput(toFile, Context.MODE_PRIVATE);

            while ((bytes_read = is.read(buffer)) != -1)
                fos.write(buffer, 0, bytes_read); // write

            fos.close();
            is.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This the function to detect face onLeftResult. and get face match score @onFaceMatch override method.
     * @param faceResult
     */
    @Keep
    public void onLeftDetect(FaceDetectionResult faceResult) {}

    private void _onLeftDetect(FaceDetectionResult faceResult) {
        if (faceResult.getFeature() == null) faceResult = null;
        leftResult = faceResult;
        if (faceCallback != null) {
            faceCallback.onLeftDetect(faceResult);
        }
        calcMatch(null);
    }

    /**
     * This the function to detect face onRightResult. and get face match score @onFaceMatch override method.
     * @param faceResult
     */
    @Keep
    public void onRightDetect(FaceDetectionResult faceResult) {}
    private void _onRightDetect(FaceDetectionResult faceResult) {
        if (faceResult.getFeature() == null) faceResult = null;
        rightResult = faceResult;
        if (faceCallback != null) {
            faceCallback.onRightDetect(faceResult);
        }
        Bitmap faceMatchImage = null;
        if (faceResult != null) {
            rightResult = faceResult;
            if (face2 != null && !face2.isRecycled()) {
                faceMatchImage = face2.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                faceMatchImage = BitmapHelper.createFromARGB(faceResult.getNewImg(), faceResult.getNewWidth(), faceResult.getNewHeight());
            }
        } else {
            rightResult = null;
        }
        calcMatch(faceMatchImage);
    }


    private void calcMatch(Bitmap faceMatchImage) {
        if (leftResult == null || rightResult == null) {
            match_score = 0.0f;
        } else {
            match_score = Similarity(leftResult.getFeature(), rightResult.getFeature(), rightResult.getFeature().length);
            match_score *= 100.0f;
        }
        if (faceMatchCallBack != null) {
            faceMatchCallBack.onFaceMatch(match_score);
            if (faceMatchImage != null && !faceMatchImage.isRecycled()) {
                send(context, faceMatchImage, String.format("%.2f %%", match_score));
            }
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
        }
    }

    private void startFaceMatch(boolean b) {

        if (face1 != null && leftResult == null) {
            //Bitmap nBmp = RecogEngine.g_recogResult.faceBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Bitmap nBmp = face1.copy(Bitmap.Config.ARGB_8888, true);
            int w = nBmp.getWidth();
            int h = nBmp.getHeight();
            int s = (w * 32 + 31) / 32 * 4;
            ByteBuffer buff = ByteBuffer.allocate(s * h);
            nBmp.copyPixelsToBuffer(buff);
            nBmp.recycle();
            FaceDetectionResult leftResult = new FaceDetectionResult();
            DetectLeftFace(buff.array(), w, h, leftResult);
            if (leftResult.getFeature() != null) {
                this._onLeftDetect(leftResult);
            }
        }

        if (face2 != null) {
            Bitmap nBmp = face2;
            int w = nBmp.getWidth();
            int h = nBmp.getHeight();
            int s = (w * 32 + 31) / 32 * 4;
            ByteBuffer buff = ByteBuffer.allocate(s * h);
            nBmp.copyPixelsToBuffer(buff);
            FaceDetectionResult _rightResult = new FaceDetectionResult();
            if (leftResult != null) {
                DetectRightFace(buff.array(), w, h, leftResult.getFeature(), _rightResult);
            } else
                DetectRightFace(buff.array(), w, h, null, _rightResult);
            if (b) {
                Matrix matrix = new Matrix();
                matrix.postScale(-1, 1, face2.getWidth() / 2f, face2.getHeight() / 2f);
                Bitmap invertTarget = Bitmap.createBitmap(face2, 0, 0, face2.getWidth(), face2.getHeight(), matrix, true);
//                Mat target = new Mat();
//                Utils.bitmapToMat(invertTarget, target);
//                Imgcodecs.imwrite("/storage/emulated/0/Download/inverImage.jpg", target);
                int iw = invertTarget.getWidth();
                int ih = invertTarget.getHeight();
                int is = (iw * 32 + 31) / 32 * 4;
                ByteBuffer iBuff = ByteBuffer.allocate(is * ih);
                invertTarget.copyPixelsToBuffer(iBuff);
                FaceDetectionResult invertRightResult = new FaceDetectionResult();
                if (leftResult != null) {
                    DetectRightFace(iBuff.array(), iw, ih, leftResult.getFeature(), invertRightResult);
                } else
                    DetectRightFace(iBuff.array(), iw, ih, null, invertRightResult);
                invertTarget.recycle();
                if (leftResult != null && leftResult.getFeature() != null && _rightResult.getFeature() != null && invertRightResult.getFeature() != null) {
                    float match_score = Similarity(leftResult.getFeature(), _rightResult.getFeature(), _rightResult.getFeature().length);
                    float score = (match_score * 100.0f);
                    if (invertRightResult.getFeature() != null) {
                        float i_match_score = Similarity(leftResult.getFeature(), invertRightResult.getFeature(), invertRightResult.getFeature().length);
                        float i_score = (i_match_score * 100.0f);
                        float final_score = 0;
                        if (score >= 60 && i_score >= 60) {
                            final_score = Math.max(score, i_score);
                        } else {
                            final_score = Math.min(score, i_score);
                        }
                        FaceHelper.this.rightResult = _rightResult;
                        faceCallback.onRightDetect(FaceHelper.this.rightResult);
                        if (faceMatchCallBack != null) {
                            faceMatchCallBack.onFaceMatch(final_score);
                            Bitmap faceMatchImage;
                            if (face2 != null && !face2.isRecycled()) {
                                faceMatchImage = face2.copy(Bitmap.Config.ARGB_8888, true);
                            } else {
                                faceMatchImage = BitmapHelper.createFromARGB(FaceHelper.this.rightResult.getNewImg(), FaceHelper.this.rightResult.getNewWidth(), FaceHelper.this.rightResult.getNewHeight());
                            }
                            if (faceMatchImage != null && !faceMatchImage.isRecycled()) {
                                send(context, faceMatchImage, String.format("%.2f %%", final_score));
                            }
                        } else {
                            throw new RuntimeException(context.toString()
                                    + " must implement com.inet.facelock.callback.FaceHelper.FaceMatchCallBack");
                        }
                        return;
                    }
                }
            }
            this._onRightDetect(_rightResult);
        }
    }

    /**
     * return Bitmap from given image path.
     *
     * @param path
     * @return bitmap according to the orientation.
     */
    private Bitmap rotateImage(final String path) {

        Bitmap b = decodeFileFromPath(path);

        try {
            ExifInterface ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                    break;
                default:
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                    //b.copyPixelsFromBuffer(ByteBuffer.)
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * Decode an path into a bitmap.
     *
     * @param path imagepath is not null
     * @return Bitmap
     */
    private Bitmap decodeFileFromPath(String path) {
        Uri uri = getImageUri(path);
        InputStream in = null;
        try {
            in = activity.getContentResolver().openInputStream(uri);

            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            int inSampleSize = 1024;
            if (o.outHeight > inSampleSize || o.outWidth > inSampleSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(inSampleSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = activity.getContentResolver().openInputStream(uri);
            // Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            int MAXCAP_SIZE = 512;
            Bitmap b = getResizedBitmap(BitmapFactory.decodeStream(in, null, o2), MAXCAP_SIZE);
            in.close();

            return b;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a Uri from a path.
     *
     * @param path
     * @return a Uri for the given path
     */
    private Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }

    /**
     * Return resize bitmap
     *
     * @param image existing bitmap
     * @param maxSize maxSize is height or width according to bitmap ratio.
     * @return
     */
    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Keep
    private void send(Context context, final Bitmap faceImage, String FmScore) {

        if (isNetworkAvailable(context)) {
            Map<String, String> map = new HashMap<>();
            map.put("liveness_id", livenessId);
            map.put("face_match", "True");
            map.put("face_match_score", FmScore);

            if (faceImage != null && !faceImage.isRecycled()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                faceImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                map.put("facematch_image", Base64.encodeToString(byteArray, Base64.DEFAULT));
                faceImage.recycle();
            }

            AndroidNetworking.post(serverUrl + "/api/facematch")
                    .addHeaders("Api-key", serverKey)
                    .addBodyParameter(map)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // do nothing

                        }

                        @Override
                        public void onError(ANError error) {

                        }
                    });
        }
    }

    private boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}