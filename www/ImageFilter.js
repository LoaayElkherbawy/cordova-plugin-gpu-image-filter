var exec = require('cordova/exec');

exports.applyEffect = function(options, success, error) {
    if(options.filters && options.filters.length > 0){
      applyfil(options,"applyEffect",options.filters[0],options.path,0,success,error)
    }
    else exec(success, error, "ImageFilter", "applyEffect", [options.path, options.filter,options.weight, options.quality, 1]);
};

exports.applyEffectForReview = function(options, success, error) {
  if(options.filters && options.filters.length > 0){
    applyfil(options,"applyEffectForReview",options.filters[0],options.path,0,success,error)
  }
  else exec(success, error, "ImageFilter", "applyEffectForReview", [options.path, options.filter,options.weight, options.quality, 1]);
};

exports.cropImage = function(success, fail, image, options) {
  options = options || {}
  options.quality = options.quality || 100
  options.targetWidth = options.targetWidth || -1
  options.targetHeight = options.targetHeight || -1
  exec(success, fail, 'ImageFilter', 'cropImage', [image, options])
}

exports.cropAsync = function(image, options) {
  return new Promise(function (resolve, reject) {
    options = options || {}
    options.quality = options.quality || 100
    options.targetWidth = options.targetWidth || -1
    options.targetHeight = options.targetHeight || -1
    exec(resolve, reject, 'ImageFilter', 'cropImage', [image, options])
  });
}

exports.applyEffectForThumbnail = function(options, success, error) {
  if(options.filters && options.filters.length > 0){
    applyfil(options,"applyEffectForThumbnail",options.filters[0],options.path,0,success,error)
  }
  else exec(success, error, "ImageFilter", "applyEffectForThumbnail", [options.path, options.filter,options.weight, options.quality, 1]);
};

function applyfil(options,action,item,path,index,success,error){
  if(index === options.filters.length - 1){
    exec(success,error, "ImageFilter", action, [path, item.filter,item.weight, options.quality, 1]);
  }else{
    exec(function(img){
      var nextItem = index+1;
      applyfil(options,action,options.filters[nextItem],img,nextItem,success,error);
    }, function(err){}, "ImageFilter", action, [path, item.filter,item.weight, options.quality, 1]);
  }
}
