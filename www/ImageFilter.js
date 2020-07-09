var exec = require('cordova/exec');

exports.applyEffect = function(options) {
  return new Promise(function (resolve, reject) {
    exec(resolve, reject, "ImageFilter", "applyEffect", [options.path, options.filters, options.quality, 1]);
  });
};

exports.applyEffectForReview = function(options) {
  return new Promise(function (resolve, reject) {
    exec(resolve, reject, "ImageFilter", "applyEffectForReview", [options.path, options.filters, options.quality, 1]);
  });
};

exports.cropImage = function(success, fail, image, options) {
  options = options || {}
  options.quality = options.quality || 100
  options.targetWidth = options.targetWidth || -1
  options.targetHeight = options.targetHeight || -1
  options.widthRatio = options.widthRatio || 4
  options.heightRatio = options.heightRatio || 5
  exec(success, fail, 'ImageFilter', 'cropImage', [image, options])
}

exports.cropAsync = function(image, options) {
  return new Promise(function (resolve, reject) {
    options = options || {}
    options.quality = options.quality || 100
    options.targetWidth = options.targetWidth || -1
    options.targetHeight = options.targetHeight || -1
    options.widthRatio = options.widthRatio || 4
    options.heightRatio = options.heightRatio || 5
    exec(resolve, reject, 'ImageFilter', 'cropImage', [image, options])
  });
}

exports.applyEffectForThumbnail = function(options) {
  return new Promise(function (resolve, reject) {
    exec(resolve, reject, "ImageFilter", "applyEffectForThumbnail", [options.path, options.filters, options.quality, 1]);
  });
};
