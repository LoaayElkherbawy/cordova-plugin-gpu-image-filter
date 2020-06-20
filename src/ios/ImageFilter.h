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

@interface ImageFilter : CDVPlugin

- (void)applyEffect:(CDVInvokedUrlCommand*)command;
- (void)cropImage:(CDVInvokedUrlCommand*)command;
- (void)applyEffectForReview:(CDVInvokedUrlCommand*)command;
- (void)applyEffectForThumbnail:(CDVInvokedUrlCommand *)command;

@end
