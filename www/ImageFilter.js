var exec = require('cordova/exec');

exports.applyEffect = function(args, success, error) {
    exec(success, error, "ImageFilter", "applyEffect", [args.path, args.filter, args.quality, 1]);
};

exports.applyEffectForReview = function(args, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForReview", [args.path, args.filter, args.quality, 1]);
};

exports.applyEffectForThumbnail = function(args, success, error) {
    exec(success, error, "ImageFilter", "applyEffectForThumbnail", [args.path, args.filter, args.quality, 1]);
};
