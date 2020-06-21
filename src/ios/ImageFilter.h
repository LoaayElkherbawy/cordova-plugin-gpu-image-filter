//
//  ImageFilter.h
//  HelloWorld
//
//  Created by dat tran on 3/3/17.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>

#import <GPUImage/GPUImage.h>
#import "PECropViewController.h"

@interface ImageFilter : CDVPlugin <PECropViewControllerDelegate>

- (void)applyEffect:(CDVInvokedUrlCommand*)command;
- (void)cropImage:(CDVInvokedUrlCommand*)command;
- (void)applyEffectForReview:(CDVInvokedUrlCommand*)command;
- (void)applyEffectForThumbnail:(CDVInvokedUrlCommand *)command;

@end
