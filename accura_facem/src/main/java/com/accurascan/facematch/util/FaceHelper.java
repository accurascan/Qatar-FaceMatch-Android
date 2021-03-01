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

import com.accurascan.facematch.R;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.inet.facelock.callback.FaceCallback;
import com.inet.facelock.callback.FaceDetectionResult;
import com.inet.facelock.callback.FaceLockHelper;

import org.json.JSONException;
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

public class FaceHelper {
    private Context context;
    public FaceDetectionResult leftResult = null;
    public FaceDetectionResult rightResult = null;
    public float match_score = 0.0f;
    public Bitmap face2 = null;
    public Bitmap face1 = null;
    private Activity activity;
    private FaceMatchCallBack faceMatchCallBack;
    private FaceCallback faceCallback;
    private String serverUrl = "";
    private String serverKey = "";
    private String livenessId = "";
    private boolean isSend = false;

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

        int nRet = FaceLockHelper.InitEngine(this.faceCallback, 30, 800, 1.18f, pathModel, pathWeight, context.getAssets());
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
    }

    /**
     * This is the function to get Face match score.
     * pass two image uri to detect face and get face match score between two images.
     * and get data on {@link FaceMatchCallBack}
     *
     * @param uri1 to pass for detect face from image uri.
     * @param uri2 to pass for detect face from image uri.
     */
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
        startFaceMatch();
//        }
    }

    /**
     * This is the function to get face from image path.
     *
     * @param bitmap pass bitmap to detect face from image.
     */
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
        startFaceMatch();
//        }
    }

    /**
     * This is the function to get face from image path.
     *
     * to match face between two images then must have to call {@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}.
     *
     * @param matchFile pass image file
     */
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
    public void setMatchImage(String matchPath) {
        if (face1 == null) {
            throw new RuntimeException(context.toString() + " Please set Input image First");
        }
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
        startFaceMatch();
    }

    /**
     * This is the function to get face from bitmap.
     *
     * to match face between two images then must have to call {@link FaceHelper#setInputImage(String)} and {@link FaceHelper#setMatchImage(String)}
     *
     * @param bitmap pass bitmap
     */
    public void setMatchImage(Bitmap bitmap) {
        if (face1 == null) {
            throw new RuntimeException(context.toString() + " Please set Input image First");
        }
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
        startFaceMatch();
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
            startFaceMatch();
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
    public void onLeftDetect(FaceDetectionResult faceResult) {
        leftResult = null;
        if (faceResult != null) {
            leftResult = faceResult;

            if (face2 != null) {
                Bitmap nBmp = face2.copy(Bitmap.Config.ARGB_8888, true);
                if (nBmp != null && !nBmp.isRecycled()) {
                    int w = nBmp.getWidth();
                    int h = nBmp.getHeight();
                    int s = (w * 32 + 31) / 32 * 4;
                    ByteBuffer buff = ByteBuffer.allocate(s * h);
                    nBmp.copyPixelsToBuffer(buff);
                    if (leftResult != null) {
                        FaceLockHelper.DetectRightFace(buff.array(), w, h, leftResult.getFeature());
                    } else {
                        FaceLockHelper.DetectRightFace(buff.array(), w, h, null);
                    }
                }
            }
        } else {
            if (face2 != null) {
                Bitmap nBmp = face2.copy(Bitmap.Config.ARGB_8888, true);
                if (nBmp != null && !nBmp.isRecycled()) {
                    int w = nBmp.getWidth();
                    int h = nBmp.getHeight();
                    int s = (w * 32 + 31) / 32 * 4;
                    ByteBuffer buff = ByteBuffer.allocate(s * h);
                    nBmp.copyPixelsToBuffer(buff);
                    if (leftResult != null) {
                        FaceLockHelper.DetectRightFace(buff.array(), w, h, leftResult.getFeature());
                    } else {
                        FaceLockHelper.DetectRightFace(buff.array(), w, h, null);
                    }
                }
            }
        }
        calcMatch(null);
    }

    /**
     * This the function to detect face onRightResult. and get face match score @onFaceMatch override method.
     * @param faceResult
     */
    public void onRightDetect(FaceDetectionResult faceResult) {
        Bitmap faceMatchImage = null;
        if (faceResult != null) {
            rightResult = faceResult;
            Bitmap bitmap = BitmapHelper.createFromARGB(faceResult.getNewImg(), faceResult.getNewWidth(), faceResult.getNewHeight());
            faceMatchImage = faceResult.getFaceImage(bitmap);
        } else {
            rightResult = null;
        }
        calcMatch(faceMatchImage);
    }


    private void calcMatch(Bitmap faceMatchImage) {
        if (leftResult == null || rightResult == null) {
            match_score = 0.0f;
        } else {
            match_score = FaceLockHelper.Similarity(leftResult.getFeature(), rightResult.getFeature(), rightResult.getFeature().length);
            match_score *= 100.0f;
        }
        if (faceMatchCallBack != null) {
            faceMatchCallBack.onFaceMatch(match_score);
            if (faceMatchImage != null && !faceMatchImage.isRecycled()) {
                send(context, faceMatchImage, match_score+"");
            }
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
        }
    }

    private void startFaceMatch() {

        if (face1 != null && leftResult == null) {
            //Bitmap nBmp = RecogEngine.g_recogResult.faceBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Bitmap nBmp = face1.copy(Bitmap.Config.ARGB_8888, true);
            int w = nBmp.getWidth();
            int h = nBmp.getHeight();
            int s = (w * 32 + 31) / 32 * 4;
            ByteBuffer buff = ByteBuffer.allocate(s * h);
            nBmp.copyPixelsToBuffer(buff);
            FaceLockHelper.DetectLeftFace(buff.array(), w, h);
        }

        if (face2 != null) {
            Bitmap nBmp = face2;
            int w = nBmp.getWidth();
            int h = nBmp.getHeight();
            int s = (w * 32 + 31) / 32 * 4;
            ByteBuffer buff = ByteBuffer.allocate(s * h);
            nBmp.copyPixelsToBuffer(buff);
            if (leftResult != null) {
                FaceLockHelper.DetectRightFace(buff.array(), w, h, leftResult.getFeature());
            } else
                FaceLockHelper.DetectRightFace(buff.array(), w, h, null);
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

    public void send(Context context, final Bitmap faceImage, String FmScore) {

        if (isNetworkAvailable(context)) {
            Map<String, String> map = new HashMap<>();
            map.put("liveness_id", livenessId);
            map.put("face_match", "True");
            map.put("face_match_score", FmScore);

            if (faceImage != null && !faceImage.isRecycled()) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                faceImage.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                map.put("facematch_image", Base64.encodeToString(byteArray, Base64.DEFAULT));
                faceImage.recycle();
            }

            AndroidNetworking.upload(serverUrl + "/api/facematch")
                    .addHeaders("Api-key", serverKey)
                    .addMultipartParameter(map)
                    .setPriority(Priority.HIGH)
                    .build()
                    .setUploadProgressListener(new UploadProgressListener() {
                        @Override
                        public void onProgress(long bytesUploaded, long totalBytes) {
                            // do nothing
                        }
                    })
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

    public boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}