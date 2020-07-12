package cordova.plugin.gpu.image.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import org.apache.cordova.PluginResult;
import com.soundcloud.android.crop.*;
import android.widget.ImageView;
import android.media.ExifInterface;


import org.apache.commons.lang3.StringUtils;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Matrix;

import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap.*;
import android.util.DisplayMetrics;

import jp.co.cyberagent.android.gpuimage.*;

/**
* Created by Dat Tran on 3/7/17.
*/

//private class GPUImageGLSurfaceView extends GLSurfaceView {
//    public GPUImageGLSurfaceView(Context context) {
//        super(context);
//    }
//
//    public GPUImageGLSurfaceView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (mForceSize != null) {
//            super.onMeasure(MeasureSpec.makeMeasureSpec(mForceSize.width, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(mForceSize.height, MeasureSpec.EXACTLY));
//        } else {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//    }
//}

class MySize {
  public MySize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  private int width;
  private int height;
}

public class ImageFilter extends CordovaPlugin {

  private static final int JPEG = 0;
  private static final int PNG = 1;

  private static final float NOT_AVAILABLE = -9999;

  private static String currentImagePath;
  private static String base64Image;
  private static Bitmap currentPreviewImage;
  private static Bitmap currentEditingImage;
  private static Bitmap currentThumbnailImage;
  private static double compressCropQuality;
  //    private static GPUImage editingGPUImage;
  //    private static GPUImage previewGPUImage;
  //    private static GPUImage thumbnailGPUImage;

  //    private GLSurfaceView glSurfaceView;
  private static MySize screenSize;

  private Context context;

  public CallbackContext callbackContext;

  private Uri inputUri;
  private Uri outputUri;

  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();

    context = this.cordova.getActivity();

    DisplayMetrics displayMetrics = new DisplayMetrics();
    this.cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    screenSize = new MySize(width, height);

    //        glSurfaceView = new GLSurfaceView(context);

    //        editingGPUImage.setGLSurfaceView(glSurfaceView);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    this.callbackContext = callbackContext;

    if (action.equals("coolMethod")) {
      String message = args.getString(0);
      //            this.coolMethod(message, callbackContext);
      return true;
    }
    if (action.equals("applyEffect")
    || action.equals("applyEffectForReview")
    || action.equals("applyEffectForThumbnail")) {
      String path = args.getString(0);
      JSONArray filters = args.getJSONArray(1);
      //String filterType = args.getString(1);
      //double weight = args.getDouble(2);
      double compressQuality = args.getDouble(2);
      int isBase64Image = args.getInt(3);

      if (action.equals("applyEffect"))
      this.applyEffect(path, filters, compressQuality, isBase64Image, callbackContext);
      else if (action.equals("applyEffectForReview"))
      this.applyEffectForReview(path, filters, compressQuality, isBase64Image, callbackContext);
      else if (action.equals("applyEffectForThumbnail"))
      this.applyEffectForThumbnail(path, filters, compressQuality, isBase64Image, callbackContext);
      return true;
    }else if (action.equals("cropImage")) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try{
            String imagePath = args.getString(0);
            JSONObject options = args.getJSONObject(1);
            callbackContext.sendPluginResult(cropImage(imagePath,options,callbackContext));
          }catch(JSONException e){
          }
        }
      });

      return true;
    }
    return false;
  }

  private PluginResult cropImage(String imagePath,JSONObject options, CallbackContext callbackContext) {
    try{
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = false;
      currentEditingImage = base64ToBitmap(imagePath,opts);
      compressCropQuality = options.getDouble("quality");

      int widthRatio = options.getInt("widthRatio");
      int heightRatio = options.getInt("heightRatio");
      int targetWidth = options.getInt("targetWidth");
      int targetHeight = options.getInt("targetHeight");

      PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
      pr.setKeepCallback(true);
      callbackContext.sendPluginResult(pr);
      this.callbackContext = callbackContext;
      cordova.setActivityResultCallback(this);

      File file = new File(context.getCacheDir(),"tmpCrop.jpeg");
      File output = new File(context.getCacheDir(),"tmpCropOut.jpeg");
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      currentEditingImage.compress(Bitmap.CompressFormat.JPEG, 100, os);
      os.close();

      Crop crop = Crop.of(Uri.fromFile(file), Uri.fromFile(output));
      if(targetHeight > 0 && targetWidth > 0) {
        crop.withMaxSize(targetWidth, targetHeight);
      }

      if(widthRatio > 0 && heightRatio > 0){
        crop.withAspect(widthRatio, heightRatio);
      }

      crop.start(cordova.getActivity());
      return pr;
    }catch(JSONException e){
      return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
    }catch(FileNotFoundException e){
      return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
    }catch(IOException e){
      return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
    }
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == Crop.REQUEST_CROP) {
      if (resultCode == Activity.RESULT_OK) {
        try{
          InputStream is = context.getContentResolver().openInputStream(Crop.getOutput(intent));
          Bitmap imageUri = BitmapFactory.decodeStream(is);
          is.close();
          this.processPicture(imageUri, (float) this.compressCropQuality, JPEG, this.callbackContext);
          this.callbackContext = null;
        }catch(IOException e){
          this.callbackContext.error("Failed to write");
          this.callbackContext = null;
          e.printStackTrace();
        }
      } else if (resultCode == Crop.RESULT_ERROR) {
        try {
          JSONObject err = new JSONObject();
          err.put("message", "Error on cropping");
          err.put("code", String.valueOf(resultCode));
          err.put("errorStack", String.valueOf(Crop.getError(intent).toString()));
          this.callbackContext.error(err);
          this.callbackContext = null;
        } catch (JSONException e) {
          e.printStackTrace();
        }
      } else if (resultCode == Activity.RESULT_CANCELED) {
        try {
          JSONObject err = new JSONObject();
          err.put("message", "User cancelled");
          err.put("code", "userCancelled");
          this.callbackContext.error(err);
          this.callbackContext = null;
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, intent);

  }

  private static int calculateInSampleSize(
  BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      // Calculate the largest inSampleSize value that is a power of 2 and keeps both
      // height and width larger than the requested height and width.
      while ((halfHeight / inSampleSize) >= reqHeight
      && (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  private void validateInput(String pathOrData, int isBase64Image, int size) {

    if (isBase64Image == 0) {
      if (!StringUtils.isEmpty(pathOrData)
      && !pathOrData.equals(currentImagePath)) {

        currentImagePath = pathOrData;
        pathOrData = pathOrData.substring(7);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathOrData, options);
        switch(size){
          case 0:
          currentEditingImage = BitmapFactory.decodeFile(pathOrData);
          break;
          case 1:
          float ratio = (float) screenSize.getWidth() / (float) options.outWidth;
          MySize reviewSize = new MySize(Math.round(options.outWidth * ratio), Math.round(options.outHeight * ratio));
          options.inSampleSize = calculateInSampleSize(options,reviewSize.getWidth(),reviewSize.getHeight());
          options.inJustDecodeBounds = false;
          currentPreviewImage = BitmapFactory.decodeFile(pathOrData,options);
          break;
          case 2:
          MySize thumbSize = new MySize(Math.round(options.outWidth * 0.2f), Math.round(options.outHeight * 0.2f));
          options.inSampleSize = calculateInSampleSize(options,thumbSize.getWidth(),thumbSize.getHeight());
          options.inJustDecodeBounds = false;
          currentThumbnailImage = BitmapFactory.decodeFile(pathOrData,options);
          break;
        }
      }
    } else if (isBase64Image == 1) {
      if (!StringUtils.isEmpty(pathOrData)
      && !pathOrData.equals(base64Image)) {
        base64Image = pathOrData;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        byte[] decodedString = Base64.decode(pathOrData, Base64.DEFAULT);
        Bitmap tmpcurrentEditingImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
        switch(size){
          case 0:
          options.inJustDecodeBounds = false;
          currentEditingImage = base64ToBitmap(pathOrData,options);
          break;
          case 1:
          float ratio = (float) screenSize.getWidth() / (float) options.outWidth;
          MySize newSize = new MySize(Math.round(options.outWidth * ratio), Math.round(options.outHeight * ratio));
          options.inSampleSize = calculateInSampleSize(options,newSize.getWidth(),newSize.getHeight());
          options.inJustDecodeBounds = false;
          currentPreviewImage = base64ToBitmap(pathOrData,options);
          break;
          case 2:
          MySize newSize1 = new MySize(Math.round(options.outWidth * 0.2f), Math.round(options.outHeight * 0.2f));
          options.inSampleSize = calculateInSampleSize(options,newSize1.getWidth(),newSize1.getHeight());
          options.inJustDecodeBounds = false;
          currentThumbnailImage = base64ToBitmap(pathOrData,options);
          break;
        }
      }
    } else {
      this.callbackContext.error("something wrong while passing isBase64Image value");
    }
  }

  private Bitmap getThumbnailImage() {
    MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
    return Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
  }

  private void applyEffect(String pathOrData, final JSONArray filters, final double compressQuality, int isBase64Image, CallbackContext callbackContext) {
    synchronized (this) {
      this.validateInput(pathOrData, isBase64Image,0);

      Bitmap bmp = currentEditingImage;

      for(int i =0;i<filters.length();i++){
        try{
          GPUImage editingGPUImage = new GPUImage(context);
          editingGPUImage.setImage(bmp);
          JSONObject filter = filters.getJSONObject(i);
          String filterType = filter.getString("filter");
          double weight = filter.getDouble("weight");
          if (filterType.equals("aged"))
          bmp = applyAgedEffect(editingGPUImage);
          else if (filterType.equals("blackWhite"))
          bmp = applyBlackWhiteEffect(editingGPUImage);
          else if (filterType.equals("cold"))
          bmp = applyColdEffect(editingGPUImage);
          else if (filterType.equals("rosy"))
          bmp = applyRosyEffect(editingGPUImage);
          else if (filterType.equals("intense"))
          bmp = applyIntenseEffect(editingGPUImage);
          else if (filterType.equals("warm"))
          bmp = applyWarmEffect(editingGPUImage);
          else if (filterType.equals("light"))
          bmp = applyLightEffect(editingGPUImage);
          else{
            Bitmap tmpBmp = applyStandardEffect(editingGPUImage,filterType, (float) weight);
            if(tmpBmp != null){
              bmp = tmpBmp;
            }
          }        }catch(JSONException e){

          }
        }
        processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
      }
    }

    private void applyEffectForReview(String pathOrData, final JSONArray filters, final double compressQuality, int isBase64Image, CallbackContext callbackContext) {
      synchronized (this) {
        this.validateInput(pathOrData, isBase64Image,1);

        Bitmap bmp = currentPreviewImage;

        for(int i =0;i<filters.length();i++){
          try{
            GPUImage previewGPUImage = new GPUImage(context);
            previewGPUImage.setImage(bmp);
            JSONObject filter = filters.getJSONObject(i);
            String filterType = filter.getString("filter");
            double weight = filter.getDouble("weight");
            if (filterType.equals("aged"))
            bmp = applyAgedEffect(previewGPUImage);
            else if (filterType.equals("blackWhite"))
            bmp = applyBlackWhiteEffect(previewGPUImage);
            else if (filterType.equals("cold"))
            bmp = applyColdEffect(previewGPUImage);
            else if (filterType.equals("rosy"))
            bmp = applyRosyEffect(previewGPUImage);
            else if (filterType.equals("intense"))
            bmp = applyIntenseEffect(previewGPUImage);
            else if (filterType.equals("warm"))
            bmp = applyWarmEffect(previewGPUImage);
            else if (filterType.equals("light"))
            bmp = applyLightEffect(previewGPUImage);
            else{
              Bitmap tmpBmp = applyStandardEffect(previewGPUImage,filterType, (float) weight);
              if(tmpBmp != null){
                bmp = tmpBmp;
              }
            }        }catch(JSONException e){

            }
          }
          processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
        }
      }

      private void applyEffectForThumbnail(final String pathOrData, final JSONArray filters, final double compressQuality, final int isBase64Image, final CallbackContext callbackContext) {
        validateInput(pathOrData, isBase64Image,3);
        Bitmap thumb = currentThumbnailImage;
        Bitmap bmp = thumb;

        for(int i =0;i<filters.length();i++){
          try{
            GPUImage thumbnailGPUImage = new GPUImage(context);
            thumbnailGPUImage.setImage(bmp);
            JSONObject filter = filters.getJSONObject(i);
            String filterType = filter.getString("filter");
            double weight = filter.getDouble("weight");
            if (filterType.equals("aged"))
            bmp = applyAgedEffect(thumbnailGPUImage);
            else if (filterType.equals("blackWhite"))
            bmp = applyBlackWhiteEffect(thumbnailGPUImage);
            else if (filterType.equals("cold"))
            bmp = applyColdEffect(thumbnailGPUImage);
            else if (filterType.equals("rosy"))
            bmp = applyRosyEffect(thumbnailGPUImage);
            else if (filterType.equals("intense"))
            bmp = applyIntenseEffect(thumbnailGPUImage);
            else if (filterType.equals("warm"))
            bmp = applyWarmEffect(thumbnailGPUImage);
            else if (filterType.equals("light"))
            bmp = applyLightEffect(thumbnailGPUImage);
            else{
              Bitmap tmpBmp = applyStandardEffect(thumbnailGPUImage,filterType, (float) weight);
              if(tmpBmp != null){
                bmp = tmpBmp;
              }
            }
          }catch(JSONException e){

          }
        }
        processPicture(bmp, (float) compressQuality, JPEG, callbackContext);
      }

      private Bitmap applyAgedEffect(GPUImage img) {
        return this.applyFilter(img, 0.00516f, 0.04124f, 0.8763f, 0.7474f, 0.1804f, 1.0103f,
        NOT_AVAILABLE, 0.7835f, 0.719f, 0.616f);
      }

      private Bitmap applyBlackWhiteEffect(GPUImage img) {
        return this.applyFilter(img, 0.0f, NOT_AVAILABLE, NOT_AVAILABLE, 1.2282f, 0.2062f, 0.268f,
        NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
      }

      private Bitmap applyRosyEffect(GPUImage img) {
        return this.applyFilter(img, 0.79897f, -0.164948f, 0.819588f, 0.881443f, 0.474227f, NOT_AVAILABLE,
        NOT_AVAILABLE, NOT_AVAILABLE, 0.822165f, 0.876289f);
      }

      private Bitmap applyIntenseEffect(GPUImage img) {
        return this.applyFilter(img, 2f, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, 0.12371f,
        NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
      }

      private Bitmap applyWarmEffect(GPUImage img) {
        return this.applyFilter(img, 1.2577f, -0.085f, 0.964f, 0.8763f, 0.4536f,
        NOT_AVAILABLE, NOT_AVAILABLE, 0.83f, 0.8092f, 0.7938f);
      }

      private Bitmap applyLightEffect(GPUImage img) {
        return this.applyFilter(img, 1.4484f, -0.0592f, 0.7629f, 0.7835f, 0.4124f, -0.0825f,
        NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
      }

      private Bitmap applyColdEffect(GPUImage img) {
        return this.applyFilter(img, 0.216495f, -0.134021f, 0.85567f, 1.061856f, 0.603093f, NOT_AVAILABLE,
        NOT_AVAILABLE, 0.708763f, 0.832474f, NOT_AVAILABLE);
      }

      private Bitmap applyStandardEffect(GPUImage img,String filterType, float weight) {
        System.out.println("---------------------------------------------" + weight + "---------------------------------------------" + filterType);
        if(filterType.equals("saturation")){
          return this.applyFilter(img, weight, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        }else if(filterType.equals("brightness")){
          return this.applyFilter(img, NOT_AVAILABLE, weight, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        }else if(filterType.equals("contrast")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, weight, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        }  else if(filterType.equals("gamma")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, weight, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        } else if(filterType.equals("exposure")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, weight, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        } else if(filterType.equals("sharpen")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, weight,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        } else if(filterType.equals("hue")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          weight, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE);
        } else if(filterType.equals("red")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, weight, NOT_AVAILABLE, NOT_AVAILABLE);
        } else if(filterType.equals("green")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, weight, NOT_AVAILABLE);
        } else if(filterType.equals("blue")){
          return this.applyFilter(img, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE,
          NOT_AVAILABLE, NOT_AVAILABLE, NOT_AVAILABLE, weight);
        }

        return null;
      }

      private Bitmap applyFilter(GPUImage img, float saturation, float brightness, float contrast, float gamma, float exposure, float sharpen, float hue,
      float red, float green, float blue) {

        GPUImageFilterGroup group = new GPUImageFilterGroup();

        if (saturation != NOT_AVAILABLE) {
          GPUImageSaturationFilter saturationFilter = new GPUImageSaturationFilter();
          saturationFilter.setSaturation(saturation);
          group.addFilter(saturationFilter);
        }
        if (brightness != NOT_AVAILABLE) {
          GPUImageBrightnessFilter brightnessFilter = new GPUImageBrightnessFilter();
          brightnessFilter.setBrightness(brightness);
          group.addFilter(brightnessFilter);
        }
        if (contrast != NOT_AVAILABLE) {
          GPUImageContrastFilter contrastFilter = new GPUImageContrastFilter();
          contrastFilter.setContrast(contrast);
          group.addFilter(contrastFilter);
        }
        if (gamma != NOT_AVAILABLE) {
          GPUImageGammaFilter gammaFilter = new GPUImageGammaFilter();
          gammaFilter.setGamma(gamma);
          group.addFilter(gammaFilter);
        }
        if (exposure != NOT_AVAILABLE) {
          GPUImageExposureFilter exposureFilter = new GPUImageExposureFilter();
          exposureFilter.setExposure(exposure);
          group.addFilter(exposureFilter);
        }
        if (sharpen != NOT_AVAILABLE) {
          GPUImageSharpenFilter sharpenFilter = new GPUImageSharpenFilter();
          sharpenFilter.setSharpness(sharpen);
          group.addFilter(sharpenFilter);
        }
        if (hue != NOT_AVAILABLE) {
          GPUImageHueFilter hueFilter = new GPUImageHueFilter();
          hueFilter.setHue(hue);
          group.addFilter(hueFilter);
        }
        if (red != NOT_AVAILABLE || green != NOT_AVAILABLE || blue != NOT_AVAILABLE) {
          GPUImageRGBFilter rgbFilter = new GPUImageRGBFilter();
          if (red != NOT_AVAILABLE)
          rgbFilter.setRed(red);
          if (green != NOT_AVAILABLE)
          rgbFilter.setGreen(green);
          if (blue != NOT_AVAILABLE)
          rgbFilter.setBlue(blue);
          group.addFilter(rgbFilter);
        }

        img.setFilter(group);
        return img.getBitmapWithFilterApplied();
      }

      public void failPicture(String err) {
        this.callbackContext.error(err);
      }

      private void processPicture(Bitmap bitmap, float compressQuality, int encodingType, CallbackContext callbackContext) {
        synchronized (this) {
          ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
          CompressFormat compressFormat = encodingType == JPEG ?
          CompressFormat.JPEG :
          CompressFormat.PNG;

          try {
            if (bitmap.compress(compressFormat, 100, jpeg_data)) {
              byte[] code = jpeg_data.toByteArray();
              byte[] output = Base64.encode(code, Base64.NO_WRAP);
              String js_out = new String(output);
              callbackContext.success(js_out);
              js_out = null;
              output = null;
              code = null;
            }
          } catch (Exception e) {
            this.failPicture("Error compressing image.");
          }
          if(bitmap != null) bitmap.recycle();
          jpeg_data = null;
        }
      }

      private Bitmap base64ToBitmap(String encodedImage,BitmapFactory.Options options) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        try{
          Bitmap tmpcurrentEditingImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
          InputStream targetStream = new ByteArrayInputStream(decodedString);
          ExifInterface exif = new ExifInterface(targetStream);
          return rotateBitmap(tmpcurrentEditingImage,exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
        }catch(IOException e){
          return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length,options);
        }
      }

      private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
          case ExifInterface.ORIENTATION_NORMAL:
          return bitmap;
          case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
          matrix.setScale(-1, 1);
          break;
          case ExifInterface.ORIENTATION_ROTATE_180:
          matrix.setRotate(180);
          break;
          case ExifInterface.ORIENTATION_FLIP_VERTICAL:
          matrix.setRotate(180);
          matrix.postScale(-1, 1);
          break;
          case ExifInterface.ORIENTATION_TRANSPOSE:
          matrix.setRotate(90);
          matrix.postScale(-1, 1);
          break;
          case ExifInterface.ORIENTATION_ROTATE_90:
          matrix.setRotate(90);
          break;
          case ExifInterface.ORIENTATION_TRANSVERSE:
          matrix.setRotate(-90);
          matrix.postScale(-1, 1);
          break;
          case ExifInterface.ORIENTATION_ROTATE_270:
          matrix.setRotate(-90);
          break;
          default:
          return bitmap;
        }
        try {
          Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
          bitmap.recycle();
          return bmRotated;
        }
        catch (OutOfMemoryError e) {
          e.printStackTrace();
          return null;
        }
      }

    }
