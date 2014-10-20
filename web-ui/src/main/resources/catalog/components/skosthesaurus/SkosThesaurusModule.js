(function() {
  goog.provide('gn_skos_thesaurus');

  goog.require('gn_multiselect');
  goog.require('ngSkos');
  goog.require('gn_skos_thesaurus_directive');
  goog.require('gn_skos_thesaurus_service');

  angular.module('gn_skos_thesaurus', [
    'gn_multiselect',
    'ngSkos',
    'gn_skos_thesaurus_service',
    'gn_skos_thesaurus_directive'
		]);
})();
