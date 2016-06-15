(function() {
  goog.provide('gn_commons');

  goog.require('gn_multiselect');
  goog.require('gn_commons_directive');
  goog.require('gn_commons_service');

  angular.module('gn_commons', [
    'gn_multiselect',
    'gn_commons_service',
    'gn_commons_directive']);
})();
