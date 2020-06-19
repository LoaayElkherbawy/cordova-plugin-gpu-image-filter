var exec = require('cordova/exec');

exports.applyEffect = function(options, success, error) {
    exec(success, error, "ImageFilter", "applyEffect", [options.path, options.filter,options.weight, options.quality, 1]);
};

exports.applyEffectForReview = function(options, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForReview", [options.path, options.filter,options.weight, options.quality, 1]);
};

exports.applyEffectForThumbnail = function(options, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForThumbnail", [options.path, options.filter,options.weight, options.quality, 1]);
};
