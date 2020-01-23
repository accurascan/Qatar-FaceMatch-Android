package com.accurascan.facematch.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import com.accurascan.facematch.R;
import com.inet.facelock.callback.FaceCallback;
import com.inet.facelock.callback.FaceDetectionResult;
import com.inet.facelock.callback.FaceLockHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class FaceHelper implements FaceCallback {
    private Context context;
    public FaceDetectionResult leftResult = null;
    public FaceDetectionResult rightResult = null;
    public float match_score = 0.0f;
    public Bitmap face2 = null;
    public Bitmap face1 = null;
    private Activity activity;
    private FaceMatchCallBack faceMatchCallBack;
    private FaceCallback faceCallback;

    public interface FaceMatchCallBack {

        void onFaceMatch(float ret);

        void onSetInputImage(Bitmap src1);

        void onSetMatchImage(Bitmap src2);

    }

    public FaceHelper(Activity activity) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        if (activity instanceof FaceCallback) {
            this.faceCallback = (FaceCallback) activity;
        } else {
            throw new RuntimeException(context.toString()
                    + " must imaplement com.inet.facelock.callback.FaceCallback");
        }
        if (activity instanceof FaceMatchCallBack) {
            this.faceMatchCallBack = (FaceMatchCallBack) activity;
        } else {
            throw new RuntimeException(context.toString()
                    + " must imaplement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
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

    public void getFaceMatchScore(Uri uri1, Uri uri2) {
        if (uri1 != null && uri2 != null) {
            getFaceMatchScore(FileUtils.getPath(activity, uri1), FileUtils.getPath(activity, uri2));
        } else {
            throw new NullPointerException("uri1 & uri2 cannot be null");
        }
    }

    public void getFaceMatchScore(File file1, File file2) {
        if (file1 != null && file2 != null) {
            getFaceMatchScore(file1.getAbsolutePath(), file2.getAbsolutePath());
        } else {
            throw new NullPointerException("file1 & file2 cannot be null");
        }
    }

    public void setInputFile(File inputFile) {
        if (inputFile != null) {
            setInputPath(inputFile.getAbsolutePath());
        } else {
            throw new NullPointerException("inputFile cannot be null");
        }
    }

    public void setInputUri(Uri fileUri) {
        if (fileUri != null) {
            setInputPath(FileUtils.getPath(activity, fileUri));
        } else {
            throw new NullPointerException("fileUri cannot be null");
        }
    }

    public void setInputPath(String inputPath) {
        if (inputPath != null) {
            face1 = getBitmap(inputPath);
            if (faceMatchCallBack != null) {
                if (face1 != null) {
                    faceMatchCallBack.onSetInputImage(face1);
                }
            } else {
                throw new RuntimeException(context.toString()
                        + " must imaplement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
            }
        } else {
            throw new NullPointerException("inputPath cannot be null");
        }
        leftResult = null;
//        if (face1 != null && face2 != null) {
        startFaceMatch();
//        }
    }


    public void setMatchFile(File matchFile) {
        if (matchFile != null) {
            setMatchPath(matchFile.getAbsolutePath());
        } else {
            throw new NullPointerException("matchFile cannot be null");
        }
    }

    public void setMatchUri(Uri fileUri) {
        if (fileUri != null) {
            setMatchPath(FileUtils.getPath(activity, fileUri));
        } else {
            throw new NullPointerException("fileUri cannot be null");
        }
    }

    public void setMatchPath(String matchPath) {
        if (face1 == null) {
            throw new RuntimeException(context.toString() + " Please set Input file First");
        }
        if (matchPath != null) {
            face2 = getBitmap(matchPath);
            if (faceMatchCallBack != null) {
                if (face2 != null) {
                    faceMatchCallBack.onSetMatchImage(face2);
                }
            } else {
                throw new RuntimeException(context.toString()
                        + " must imaplement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");

            }
        } else {
            throw new NullPointerException("matchFile cannot be null");
        }
        rightResult = null;
//        if (face1 != null && face2 != null) {
        startFaceMatch();
//        }
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

    public void getFaceMatchScore(String path1, String path2) {
        if (path1 != null && path2 != null) {
            face1 = getBitmap(path1);
            face2 = getBitmap(path2);
            if (faceMatchCallBack != null) {
                faceMatchCallBack.onSetInputImage(face1);
                faceMatchCallBack.onSetMatchImage(face2);
            } else {
                throw new RuntimeException(context.toString()
                        + " must imaplement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
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

    @Override
    public void onInitEngine(int ret) {

    }

    @Override
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
        calcMatch();
    }

    @Override
    public void onRightDetect(FaceDetectionResult faceResult) {
        if (faceResult != null) {
            rightResult = faceResult;
        } else {
            rightResult = null;
        }
        calcMatch();
    }

    @Override
    public void onExtractInit(int ret) {

    }


    private void calcMatch() {
        if (leftResult == null || rightResult == null) {
            match_score = 0.0f;
        } else {
            match_score = FaceLockHelper.Similarity(leftResult.getFeature(), rightResult.getFeature(), rightResult.getFeature().length);
            match_score *= 100.0f;
        }
        if (faceMatchCallBack != null) {
            faceMatchCallBack.onFaceMatch(match_score);
        } else {
            throw new RuntimeException(context.toString()
                    + " must imaplement com.accurascan.facematch.util.FaceHelper.FaceMatchCallBack");
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

    //used for rotate image of given path
    //parameter to pass : String path
    // return bitmap
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

    //Get bitmap image form path
    //parameter to pass : String path
    // return Bitmap
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

    //get image uwi from given string path
    //parameter to pass : String path
    // return Uri
    private Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }

    //Used for resizing bitmap
    //parameter to pass : Bitmap image, int maxSize
    // return bitmap
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

}