'use strict';

import { NativeModules, Platform } from 'react-native';
var Q = require('q');

var RNPinch = {
    fetch: function (url, obj, callback) {
        var deferred = Q.defer();
        NativeModules.RNPinch.fetch(url, obj, (err, res) => {
            var err = {...err};
            if (err) {
                switch (getKeyByValue(err.code)) {
                    case 'kCFURLErrorTimedOut':
                        err.reason = 'Timeout';
                        break;
                    case 'kCFURLErrorUnsupportedURL':
                    case 'kCFURLErrorCannotFindHost':
                    case 'kCFURLErrorCannotConnectToHost':
                    case 'kCFURLErrorNetworkConnectionLost':
                    case 'kCFURLErrorDNSLookupFailed':
                    case 'kCFURLErrorHTTPTooManyRedirects':
                    case 'kCFURLErrorResourceUnavailable':
                    case 'kCFURLErrorNotConnectedToInternet':
                        err.reason = 'Network error';
                        break;
                    default:
                        err.reason = 'Unknown error';
                        break;
                }
                deferred.reject(err);
            } else {
                res.json = function () {
                    return Q.fcall(function () {
                        return JSON.parse(res.bodyString);
                    });
                };
                res.text = function () {
                    return Q.fcall(function () {
                        return res.bodyString;
                    });
                };
                res.url = url;
                deferred.resolve(res);
            }

            deferred.promise.nodeify(callback);
        });
        return deferred.promise;
    }
};

const obj = {
    kCFURLErrorUnknown: -998,
    kCFURLErrorCancelled: -999,
    kCFURLErrorBadURL: -1000,
    kCFURLErrorTimedOut: -1001,
    kCFURLErrorUnsupportedURL: -1002,
    kCFURLErrorCannotFindHost: -1003,
    kCFURLErrorCannotConnectToHost: -1004,
    kCFURLErrorNetworkConnectionLost: -1005,
    kCFURLErrorDNSLookupFailed: -1006,
    kCFURLErrorHTTPTooManyRedirects: -1007,
    kCFURLErrorResourceUnavailable: -1008,
    kCFURLErrorNotConnectedToInternet: -1009,
    kCFURLErrorRedirectToNonExistentLocation: -1010,
    kCFURLErrorBadServerResponse: -1011,
    kCFURLErrorUserCancelledAuthentication: -1012,
    kCFURLErrorUserAuthenticationRequired: -1013,
    kCFURLErrorZeroByteResource: -1014,
    kCFURLErrorCannotDecodeRawData: -1015,
    kCFURLErrorCannotDecodeContentData: -1016,
    kCFURLErrorCannotParseResponse: -1017,
    kCFURLErrorInternationalRoamingOff: -1018,
    kCFURLErrorCallIsActive: -1019,
    kCFURLErrorDataNotAllowed: -1020,
    kCFURLErrorRequestBodyStreamExhausted: -1021,
    kCFURLErrorFileDoesNotExist: -1100,
    kCFURLErrorFileIsDirectory: -1101,
    kCFURLErrorNoPermissionsToReadFile: -1102,
    kCFURLErrorDataLengthExceedsMaximum: -1103,
}

function getKeyByValue(value) {
    return Object.keys(obj).find(key => obj[key] === value);
}

module.exports = RNPinch;
