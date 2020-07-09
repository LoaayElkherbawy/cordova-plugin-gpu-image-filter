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

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.broadcast.BroadcastAction;
import com.luck.picture.lib.broadcast.BroadcastManager;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnResultCallbackListener;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.ToastUtils;
import com.luck.picture.lib.tools.ValueOf;
import com.luck.picture.lib.instagram.InsGallery;


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
      currentEditingImage = base64ToBitmap(imagePath);
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

      InsGallery.openGallery(cordova.getActivity(), GlideEngine.createGlideEngine(), null);

      // crop.start(cordova.getActivity());
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
    super.onActivityResult(requestCode, resultCode, data);
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
    }else{
      if (resultCode == RESULT_OK) {
          switch (requestCode) {
              case PictureConfig.CHOOSE_REQUEST:
                  // 图片选择结果回调
                  List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                  // 例如 LocalMedia 里面返回五种path
                  // 1.media.getPath(); 原图path
                  // 2.media.getCutPath();裁剪后path，需判断media.isCut();切勿直接使用
                  // 3.media.getCompressPath();压缩后path，需判断media.isCompressed();切勿直接使用
                  // 4.media.getOriginalPath()); media.isOriginal());为true时此字段才有值
                  // 5.media.getAndroidQToPath();Android Q版本特有返回的字段，但如果开启了压缩或裁剪还是取裁剪或压缩路径；注意：.isAndroidQTransform 为false 此字段将返回空
                  // 如果同时开启裁剪和压缩，则取压缩路径为准因为是先裁剪后压缩
                  for (LocalMedia media : selectList) {
                      Log.i(TAG, "是否压缩:" + media.isCompressed());
                      Log.i(TAG, "压缩:" + media.getCompressPath());
                      Log.i(TAG, "原图:" + media.getPath());
                      Log.i(TAG, "是否裁剪:" + media.isCut());
                      Log.i(TAG, "裁剪:" + media.getCutPath());
                      Log.i(TAG, "是否开启原图:" + media.isOriginal());
                      Log.i(TAG, "原图路径:" + media.getOriginalPath());
                      Log.i(TAG, "Android Q 特有Path:" + media.getAndroidQToPath());
                      Log.i(TAG, "Size: " + media.getSize());
                  }
                  // mAdapter.setList(selectList);
                  // mAdapter.notifyDataSetChanged();
                  break;
          }
      }
    }
  }

  private void validateInput(String pathOrData, int isBase64Image) {

    if (isBase64Image == 0) {
      if (!StringUtils.isEmpty(pathOrData)
      && !pathOrData.equals(currentImagePath)) {

        currentImagePath = pathOrData;
        pathOrData = pathOrData.substring(7);

        currentEditingImage = base64ToBitmap(pathOrData);

        float ratio = (float) screenSize.getWidth() / (float) currentEditingImage.getWidth();
        MySize newSize = new MySize(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
        currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);

        MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
        currentThumbnailImage = Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
      }
    } else if (isBase64Image == 1) {
      if (!StringUtils.isEmpty(pathOrData)
      && !pathOrData.equals(base64Image)) {
        base64Image = pathOrData;
        currentEditingImage = base64ToBitmap(pathOrData);

        float ratio = (float) screenSize.getWidth() / (float) currentEditingImage.getWidth();
        MySize newSize = new MySize(Math.round(currentEditingImage.getWidth() * ratio), Math.round(currentEditingImage.getHeight() * ratio));
        currentPreviewImage = Bitmap.createScaledBitmap(currentEditingImage, newSize.getWidth(), newSize.getHeight(), false);

        MySize thumbSize = new MySize(Math.round(currentPreviewImage.getWidth() * 0.2f), Math.round(currentPreviewImage.getHeight() * 0.2f));
        currentThumbnailImage = Bitmap.createScaledBitmap(currentPreviewImage, thumbSize.getWidth(), thumbSize.getHeight(), false);
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
      this.validateInput(pathOrData, isBase64Image);

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
        this.validateInput(pathOrData, isBase64Image);

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
        validateInput(pathOrData, isBase64Image);
        Bitmap thumb = getThumbnailImage();
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

      private Bitmap base64ToBitmap(String encodedImage) {
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        try{
          Bitmap tmpcurrentEditingImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
          InputStream targetStream = new ByteArrayInputStream(decodedString);
          ExifInterface exif = new ExifInterface(targetStream);
          System.out.println("Exif interface value is: " + exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
          return rotateBitmap(tmpcurrentEditingImage,exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
        }catch(IOException e){
          return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
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
