//
//  ImageFilter.m
//  HelloWorld
//
//  Created by dat tran on 3/3/17.
//
//

#import "ImageFilter.h"

#define CDV_PHOTO_PREFIX @"cdv_photo_"

@interface ImageFilter ()
@property (copy) NSString* callbackId;
@property (assign) NSUInteger quality;
@property (assign) NSUInteger targetWidth;
@property (assign) NSUInteger targetHeight;
@end

@implementation ImageFilter

static CIContext *context;
static UIImage *currentEditingImage;
static UIImage *currentPreviewImage;
static UIImage *currentThumbnailImage;
static NSString *currentImagePath = nil;
static NSString *base64Image = nil;
static CGSize screenSize;


static NSString* toBase64(NSData* data) {
  SEL s1 = NSSelectorFromString(@"cdv_base64EncodedString");
  SEL s2 = NSSelectorFromString(@"base64EncodedString");
  SEL s3 = NSSelectorFromString(@"base64EncodedStringWithOptions:");

  if ([data respondsToSelector:s1]) {
    NSString* (*func)(id, SEL) = (void *)[data methodForSelector:s1];
    return func(data, s1);
  } else if ([data respondsToSelector:s2]) {
    NSString* (*func)(id, SEL) = (void *)[data methodForSelector:s2];
    return func(data, s2);
  } else if ([data respondsToSelector:s3]) {
    NSString* (*func)(id, SEL, NSUInteger) = (void *)[data methodForSelector:s3];
    return func(data, s3, 0);
  } else {
    return nil;
  }
}

static UIImage * base64ToImage(NSString *base64Image) {
  NSData *imageData = [[NSData alloc] initWithBase64EncodedString:base64Image options:0];
  UIImage *normalizedImage = [UIImage imageWithData:imageData];
  return [UIImage imageWithCGImage:normalizedImage.CGImage scale: 1 orientation:normalizedImage.imageOrientation];
}

- (void)pluginInitialize {
  [super pluginInitialize];

  EAGLContext *eaglCxt = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
  context = [CIContext contextWithEAGLContext:eaglCxt];

  screenSize = [[UIScreen mainScreen] bounds].size;
}

- (void)validateInput:(CDVInvokedUrlCommand *)command {
  NSString *pathOrData = [command argumentAtIndex:0 withDefault:nil];
  NSURL *pathUrl = [NSURL URLWithString:pathOrData];
  pathOrData = pathUrl.path;

  NSNumber *isBase64Image = [command argumentAtIndex:3 withDefault:@(0)];

  if ([isBase64Image intValue] == 0)
  {
    currentImagePath = pathOrData;
    currentEditingImage = [UIImage imageWithContentsOfFile:pathOrData];

    float ratio = screenSize.width / currentEditingImage.size.width;
    CGSize newSize = CGSizeMake(currentEditingImage.size.width * ratio, currentEditingImage.size.height * ratio);
    UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
    [currentEditingImage drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    currentPreviewImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    CGSize thumbnailSize = CGSizeMake(currentPreviewImage.size.width * 0.2f, currentPreviewImage.size.height * 0.2f);
    UIGraphicsBeginImageContextWithOptions(thumbnailSize, NO, 0.0);
    [currentPreviewImage drawInRect:CGRectMake(0, 0, thumbnailSize.width, thumbnailSize.height)];
    currentThumbnailImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
  }
  else if ([isBase64Image intValue] == 1) {
    base64Image = pathOrData;
    currentEditingImage = base64ToImage(pathOrData);

    float ratio = screenSize.width / currentEditingImage.size.width;
    CGSize newSize = CGSizeMake(currentEditingImage.size.width * ratio, currentEditingImage.size.height * ratio);
    UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
    [currentEditingImage drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    currentPreviewImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    CGSize thumbnailSize = CGSizeMake(currentPreviewImage.size.width * 0.2f, currentPreviewImage.size.height * 0.2f);
    UIGraphicsBeginImageContextWithOptions(thumbnailSize, NO, 0.0);
    [currentPreviewImage drawInRect:CGRectMake(0, 0, thumbnailSize.width, thumbnailSize.height)];
    currentThumbnailImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
  }
  else {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

- (void)applyEffect:(CDVInvokedUrlCommand*)command {
  [self validateInput:command];

  NSArray *filters = [command argumentAtIndex:1 withDefault:nil];
  NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(1)];
  NSDictionary *filter;
  for(filter in filters){
    NSString *filterType = filter[@"filter"];
    NSNumber *weight = filter[@"weight"];
    currentEditingImage = [self applyImage:currentEditingImage filter:filterType compressionQuality:compressionQuality weight:weight];
  }

  [self filterImage:currentEditingImage compressionQuality:compressionQuality completion:^(NSData *data) {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
    dispatch_sync(dispatch_get_main_queue(), ^{
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    });
  }];
}

- (void) cropImage: (CDVInvokedUrlCommand *) command {
  NSString *imagePath = [command.arguments objectAtIndex:0];
  UIImage *image = base64ToImage(imagePath);

  NSDictionary *options = [command.arguments objectAtIndex:1];

  self.quality = options[@"quality"] ? [options[@"quality"] intValue] : 100;
  self.targetWidth = options[@"targetWidth"] ? [options[@"targetWidth"] intValue] : -1;
  self.targetHeight = options[@"targetHeight"] ? [options[@"targetHeight"] intValue] : -1;
  self.widthRatio = options[@"widthRatio"] ? [options[@"widthRatio"] intValue] : -1;
  self.heightRatio = options[@"heightRatio"] ? [options[@"heightRatio"] intValue] : -1;

  PECropViewController *cropController = [[PECropViewController alloc] init];
  cropController.delegate = self;
  cropController.image = image;

  CGFloat width = self.targetWidth > -1 ? (CGFloat)self.targetWidth : image.size.width;
  CGFloat height = self.targetHeight > -1 ? (CGFloat)self.targetHeight : image.size.height;
  CGFloat croperWidth;
  CGFloat croperHeight;
  cropController.toolbarHidden = YES;
  cropController.rotationEnabled = NO;
  cropController.keepingCropAspectRatio = YES;

  if (self.widthRatio < 0 || self.heightRatio < 0){
    cropController.keepingCropAspectRatio = NO;
    croperWidth = MIN(width, height);
    croperHeight = MIN(width, height);
  } else {
    cropController.keepingCropAspectRatio = YES;
    if(self.widthRatio > self.heightRatio) {
      croperWidth = width;
      croperHeight = width * self.heightRatio / self.widthRatio;
    } else {
      croperWidth = height * self.widthRatio / self.heightRatio;
      croperHeight = height;
    }
  }

  self.callbackId = command.callbackId;
  UINavigationController *navigationController = [[UINavigationController alloc] initWithRootViewController:cropController];

  if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
    navigationController.modalPresentationStyle = UIModalPresentationFormSheet;
  }

  [self.viewController presentViewController:navigationController animated:YES completion:NULL];
}

- (void)applyEffectForReview:(CDVInvokedUrlCommand*)command {
  [self validateInput:command];

  NSArray *filters = [command argumentAtIndex:1 withDefault:nil];
  NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(1)];
  NSDictionary *filter;
  for(filter in filters){
    NSString *filterType = filter[@"filter"];
    NSNumber *weight = filter[@"weight"];
    currentPreviewImage = [self applyImage:currentPreviewImage filter:filterType compressionQuality:compressionQuality weight:weight];
  }
  [self filterImage:currentPreviewImage compressionQuality:compressionQuality completion:^(NSData *data) {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
    dispatch_sync(dispatch_get_main_queue(), ^{
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    });
  }];
}

- (void)applyEffectForThumbnail:(CDVInvokedUrlCommand *)command {
  [self validateInput:command];

  NSArray *filters = [command argumentAtIndex:1 withDefault:nil];
  NSNumber *compressionQuality = [command argumentAtIndex:2 withDefault:@(1)];
  NSDictionary *filter;
  for(filter in filters){
    NSString *filterType = filter[@"filter"];
    NSNumber *weight = filter[@"weight"];
    currentThumbnailImage = [self applyImage:currentThumbnailImage filter:filterType compressionQuality:compressionQuality weight:weight];
  }

  [self filterImage:currentThumbnailImage compressionQuality:compressionQuality completion:^(NSData *data) {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];
    dispatch_sync(dispatch_get_main_queue(), ^{
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    });
  }];
}

- (UIImage *)applyImage:(UIImage *)image filter:(NSString *)filterType compressionQuality:(NSNumber *)quality weight:(NSNumber *)weight {
  return [self applySelectedEffect:image effect:filterType weight:weight];
}

- (void)filterImage:(UIImage *)image compressionQuality:(NSNumber *)quality completion:(void(^)(NSData *))completion {
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    @autoreleasepool {
      NSData *data = UIImageJPEGRepresentation(image, [quality floatValue]);
      completion(data);
    }
  });
}

- (UIImage *)applySelectedEffect:(UIImage *)image effect:(NSString *)effect weight:(NSNumber *)weight{
  if ([effect isEqualToString:@"aged"])
  return [self applyAgedEffect:image];
  else if ([effect isEqualToString:@"blackWhite"])
  return [self applyBlackWhiteEffect:image];
  else if ([effect isEqualToString:@"cold"])
  return [self applyColdEffect:image];
  else if ([effect isEqualToString:@"rosy"])
  return [self applyRosyEffect:image];
  else if ([effect isEqualToString:@"intense"])
  return [self applyIntenseEffect:image];
  else if ([effect isEqualToString:@"warm"])
  return [self applyWarmEffect:image];
  else if ([effect isEqualToString:@"light"])
  return [self applyLightEffect:image];
  else return [self applyStandardEffect:image effect:effect weight:weight];
}

- (UIImage *)applyAgedEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(0.00516)
  brightness:@(0.04124)
  contrast:@(0.8763)
  gamma:@(0.7474)
  exposure:@(0.1804)
  sharpen:@(1.0103)
  hue:nil
  red:@(0.7835)
  green:@(0.719)
  blue:@(0.616)];
}

- (UIImage *)applyBlackWhiteEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(0.0f)
  brightness:nil
  contrast:nil
  gamma:@(1.2282)
  exposure:@(0.2062)
  sharpen:@(0.268)
  hue:nil
  red:nil
  green:nil
  blue:nil];
}

- (UIImage *)applyColdEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(0.216495)
  brightness:@(-0.134021)
  contrast:@(0.85567)
  gamma:@(1.061856)
  exposure:@(0.603093)
  sharpen:nil
  hue:nil
  red:@(0.708763)
  green:@(0.832474)
  blue:nil];
}

- (UIImage *)applyRosyEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(0.79897)
  brightness:@(-0.164948)
  contrast:@(0.819588)
  gamma:@(0.881443)
  exposure:@(0.474227)
  sharpen:nil
  hue:nil
  red:nil
  green:@(0.822165)
  blue:@(0.876289)];
}

- (UIImage *)applyIntenseEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(2.0)
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:@(0.12371)
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
}

- (UIImage *)applyWarmEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(1.2577)
  brightness:@(-0.085)
  contrast:@(0.964)
  gamma:@(0.8763)
  exposure:@(0.4536)
  sharpen:nil
  hue:nil
  red:@(0.83)
  green:@(0.8092)
  blue:@(0.7938)];
}

- (UIImage *)applyLightEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:@(1.4484)
  brightness:@(-0.0592)
  contrast:@(0.7629)
  gamma:@(0.7835)
  exposure:@(0.4124)
  sharpen:@(-0.0825)
  hue:nil
  red:nil
  green:nil
  blue:nil];
}

- (UIImage *)applyNoEffect:(UIImage *)img {
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
}

- (UIImage *)applyStandardEffect:(UIImage *)img effect:(NSString *)effect weight:(NSNumber *)weight  {
  if ([effect isEqualToString:@"saturation"])
  return [self applyFilter:img
  saturation:weight
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"brightness"])
  return [self applyFilter:img
  saturation:nil
  brightness:weight
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"contrast"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:weight
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"gamma"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:weight
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"exposure"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:weight
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"sharpen"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:weight
  hue:nil
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"hue"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:weight
  red:nil
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"red"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:weight
  green:nil
  blue:nil];
  else if ([effect isEqualToString:@"green"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:weight
  blue:nil];
  else if ([effect isEqualToString:@"blue"])
  return [self applyFilter:img
  saturation:nil
  brightness:nil
  contrast:nil
  gamma:nil
  exposure:nil
  sharpen:nil
  hue:nil
  red:nil
  green:nil
  blue:weight];
  else return [self applyNoEffect:img];
}

- (UIImage *)applyFilter:(UIImage *)image saturation:(NSNumber *)saturation brightness:(NSNumber *)brightness contrast:(NSNumber *)contrast gamma:(NSNumber *)gamma exposure:(NSNumber *)exposure sharpen:(NSNumber *)sharpen hue:(NSNumber *)hue red:(NSNumber *)red green:(NSNumber *)green blue:(NSNumber *)blue {

  UIImage *result;
  NSMutableArray<GPUImageFilter*> *filters = [NSMutableArray new];
  GPUImageFilter *tmp;
  if (saturation) {
    GPUImageFilter *filter = [self applySaturation:result saturation:saturation];
    [filters addObject:filter];
    tmp = filter;
  }
  if (brightness) {
    GPUImageFilter *filter = [self applyBrightness:result brightness:brightness];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (contrast) {
    GPUImageFilter *filter = [self applyContrast:result contrast:contrast];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (gamma){
    GPUImageFilter *filter = [self applyGamma:result value:gamma];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (exposure){
    GPUImageFilter *filter = [self applyExposure:result value:exposure];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (sharpen){
    GPUImageFilter *filter = [self applySharpen:result value:sharpen];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (hue){
    GPUImageFilter *filter = [self applyHue:result value:hue];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (red){
    GPUImageFilter *filter = [self applyRed:result value:red];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (green){
    GPUImageFilter *filter = [self applyGreen:result value:green];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }
  if (blue){
    GPUImageFilter *filter = [self applyBlue:result value:blue];
    [filters addObject:filter];
    [tmp addTarget:filter];
    tmp = filter;
  }


  GPUImageFilterGroup *group = [GPUImageFilterGroup new];
  if(filters.count > 0){
    [group setInitialFilters:[NSArray arrayWithObject:filters.firstObject]];
    [group setTerminalFilter:filters.lastObject];

    result = [group imageByFilteringImage:image];
  }else{
    result = image;
  }

  return result;
}

- (GPUImageFilter *)applyBrightness:(UIImage *)image brightness:(NSNumber*)brightness {
  GPUImageBrightnessFilter *filter = [GPUImageBrightnessFilter new];
  [filter setBrightness:[brightness floatValue]];
  return filter;

}
- (GPUImageFilter *)applySaturation:(UIImage *)image saturation:(NSNumber*)saturation {
  GPUImageSaturationFilter *filter = [GPUImageSaturationFilter new];
  [filter setSaturation:[saturation floatValue]];
  return filter;
}
- (GPUImageFilter *)applyContrast:(UIImage *)image contrast:(NSNumber*)contrast {
  GPUImageContrastFilter *filter = [GPUImageContrastFilter new];
  [filter setContrast:[contrast floatValue]];
  return filter;
}

- (GPUImageFilter *)applyExposure:(UIImage *)image value:(NSNumber*)value {
  GPUImageExposureFilter *filter = [GPUImageExposureFilter new];
  [filter setExposure:[value floatValue]];
  return filter;
}

- (GPUImageFilter *)applyGamma:(UIImage *)image value:(NSNumber*)value {
  GPUImageGammaFilter *filter = [GPUImageGammaFilter new];
  [filter setGamma:[value floatValue]];
  return filter;
}

- (GPUImageFilter *)applySharpen:(UIImage *)image value:(NSNumber*)value {
  GPUImageSharpenFilter *filter = [GPUImageSharpenFilter new];
  [filter setSharpness:[value floatValue]];
  return filter;
}

- (GPUImageFilter *)applyHue:(UIImage *)image value:(NSNumber*)value {
  GPUImageHueFilter *filter = [GPUImageHueFilter new];
  [filter setHue:[value floatValue]];
  return filter;
}

- (GPUImageFilter*)applyRed:(UIImage*)image value:(NSNumber *)value {
  GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
  [filter setRed:[value floatValue]];
  return filter;
}
- (GPUImageFilter*)applyGreen:(UIImage*)image value:(NSNumber *)value {
  GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
  [filter setGreen:[value floatValue]];
  return filter;
}
- (GPUImageFilter*)applyBlue:(UIImage*)image value:(NSNumber *)value {
  GPUImageRGBFilter *filter = [GPUImageRGBFilter new];
  [filter setBlue:[value floatValue]];
  return filter;
}

#pragma mark - PECropViewControllerDelegate

- (void)cropViewController:(PECropViewController *)controller didFinishCroppingImage:(UIImage *)croppedImage {
  [controller dismissViewControllerAnimated:YES completion:nil];
  if (!self.callbackId) return;

  NSData *data = UIImageJPEGRepresentation(croppedImage, (CGFloat) self.quality);
  CDVPluginResult *result;

  result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(data)];

  [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
  self.callbackId = nil;
}

- (void)cropViewControllerDidCancel:(PECropViewController *)controller {
  [controller dismissViewControllerAnimated:YES completion:nil];
  NSDictionary *err = @{
    @"message": @"User cancelled",
    @"code": @"userCancelled"
  };
  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:err];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
  self.callbackId = nil;
}

@end
