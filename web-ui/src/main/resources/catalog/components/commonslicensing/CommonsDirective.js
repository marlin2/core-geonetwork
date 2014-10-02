(function() {
  goog.provide('gn_commons_directive');

  var module = angular.module('gn_commons_directive', []);

  /**
   * The creative commons license jurisdiction selector is composed of a 
	 * drop down list of jurisdictions available. On selection,
   * an empty XML fragment is requested and added to the form
   * before the editor is saved and refreshed.
   */
  module.directive('gnCommonsJurisdictionSelector',
      ['$timeout',
       'gnCommonsService', 'gnEditor',
       'gnEditorXMLService', 'gnCurrentEdit',
       function($timeout,
               gnCommonsService, gnEditor,
               gnEditorXMLService, gnCurrentEdit) {

         return {
           restrict: 'A',
           replace: true,
           transclude: true,
           scope: {
             mode: '@gnCommonsJurisdictionSelector',
             elementRef: '@',
             currentJurisdictionUrl: '@jurisdictionUrl',
						 namespace: '@namespace',
             currentLicenseName: '@licenseName',
             currentLicenseUrl: '@licenseUrl',
             currentLicenseImageUrl: '@licenseImageUrl'
           },
           templateUrl: '../../catalog/components/commonslicensing/' +
           'partials/licenseselector.html',
           link: function(scope, element, attrs) {
					 	 scope.jurisdiction = null;
             scope.snippet = null;
             scope.snippetRef = null;

             gnCommonsService.getAllJurisdictions().then(
             function(listOfJurisdictions) {
               scope.jurisdiction = listOfJurisdictions;
							 if (scope.currentJurisdictionUrl != '') {
							 	// find scope in listOfJurisdictions
								var len = listOfJurisdictions.length;
								for (var i = 0; i < len; i++) {
									if (listOfJurisdictions[i].getUrl() == scope.currentJurisdictionUrl) {
										scope.currentJurisdiction = listOfJurisdictions[i].getKey();
							 			scope.layoutCommons(scope.currentJurisdiction,
																				listOfJurisdictions[i].getName());
									}
								}
							}
             });

             scope.layoutCommons = function(jurisdictionKey, jurisdictionName) {
						 	 scope.currentJurisdiction = jurisdictionKey;
							 scope.currentJurisdictionName = jurisdictionName;
						 	 gnCommonsService.getAllLicenses(scope.currentJurisdiction).then(
							 	function(listOfLicenses) {
									scope.license = listOfLicenses;
									if (scope.currentLicenseUrl === '') {
             				scope.currentLicenseName = listOfLicenses[0].getName();
             				scope.currentLicenseUrl  = listOfLicenses[0].getUrl();
             				scope.currentLicenseImageUrl = listOfLicenses[0].getImageUrl(); 
									} 
               		gnCommonsService
                		.getXML(scope.currentJurisdiction, 
														scope.namespace,
														scope.currentLicenseName,
														scope.currentLicenseImageUrl,
														scope.currentLicenseUrl).then(
               		function(data) {
                 		// Add the fragment to the form
                    var xmlDeclaration =
                          '<?xml version="1.0" encoding="UTF-8"?>';
                 		scope.snippet = data.replace(xmlDeclaration,'');
                 		scope.snippetRef = '_X'+scope.elementRef;
               		});
							 });
               return false;
             };

             scope.addCommons = function(l) {
						 	 scope.currentLicenseName = l.getName();
               gnCommonsService
               	.getXML(scope.currentJurisdiction, scope.namespace, l.getName(), l.getImageUrl(), l.getUrl()).then(
               	function(data) {
               		// Add the fragment to the form, ready for saving later on
                  var xmlDeclaration =
                          '<?xml version="1.0" encoding="UTF-8"?>';
                 	scope.snippet = data.replace(xmlDeclaration,'');
                 	scope.snippetRef = '_X'+scope.elementRef;
               });
               return false;
             };
           }
         };
       }]);
})();
