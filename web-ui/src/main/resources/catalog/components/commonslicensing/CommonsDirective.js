(function() {
  // tell everyone what the name of this module is
  goog.provide('gn_commons_directive');

  // define an angular module with the same name
  var module = angular.module('gn_commons_directive', []);

  /**
   * The creative commons license jurisdiction selector is composed of a 
   * drop down list of jurisdictions available. On selection,
   * an empty XML fragment is requested and added to the form
   * before the editor is saved and refreshed.
   */

  // this module is a directive - it will be used when an HTML element has an attribute data-gn-commons-jurisdiction-selector (see restrict)
  // Documentation on directives can be found at https://docs.angularjs.org/guide/directive
  module.directive('gnCommonsJurisdictionSelector',
     // pass in the other modules we will use in this directive - we use only one: gnCommonsService
     [ 'gnCommonsService', 'gnCurrentEdit',
       function(gnCommonsService, gnCurrentEdit) {
         return {
            // directive options
           restrict: 'A',    // this directive can only be matched if the attribute data-gn-commons-jurisdiction-selector is present
           replace: true,    // replace element that specifies this directive with the contents of the directive
           transclude: true, // access the outside scope provided by the gnEditController as well as our own scope - which is next
           scope: {          // pass in the attribute values provided with the directive, names are mapped
             mode: '@gnCommonsJurisdictionSelector',                    // unused - data-gn-commons-jurisdiction-selector
             elementRef: '@',                                           // data-element-ref
             currentJurisdictionUrl: '@jurisdictionUrl',                // data-jurisdiction-url
             namespace: '@namespace',                                   // data-namespace
             currentLicenseName: '@licenseName',                        // data-license-name
             currentLicenseUrl: '@licenseUrl',                          // data-license-url
             currentLicenseImageUrl: '@licenseImageUrl',                // data-license-image-url
             attributionConstraint: '@attributionConstraint',           // data-attribution-constraint
             derivativeConstraint: '@derivativeConstraint',             // data-derivative-constraint
             commercialUseConstraint: '@commercialUseConstraint'        // data-commercial-use-constraint
           },
           templateUrl: '../../catalog/components/commonslicensing/' +     
           'partials/licenseselector.html',                  // this is the HTML template that our directive will add to the editor form

           // Since our directive manipulates the DOM, we use the link option
           link: function(scope, element, attrs) {
              scope.jurisdiction = null;
             scope.snippet = null;
             scope.snippetRef = null;
             scope.currentLicense = null;
						 scope.constraints = {
					      attribution: scope.attributionConstraint,
								derivative: scope.derivativeConstraint,
								commercialUse: scope.commercialUseConstraint 
						 };

             // get all CC license jurisdictions and find the one that matches ours - if found get the matching license and 
             // create a copy of the XML fragment ready to be submitted when the editor form is saved
             gnCommonsService.getAllJurisdictions().then( function(listOfJurisdictions) {
               scope.jurisdiction = listOfJurisdictions;
               if (scope.currentJurisdictionUrl != '') {
                 // find scope.currentJurisdictionUrl in listOfJurisdictions
                var len = listOfJurisdictions.length;
                for (var i = 0; i < len; i++) {
                  if (listOfJurisdictions[i].getUrl() == scope.currentJurisdictionUrl) {
                    scope.currentJurisdiction = listOfJurisdictions[i].getKey();
                     scope.layoutCommons(scope.currentJurisdiction, listOfJurisdictions[i].getName());
                  }
                }
              }
             });

             // layout the CC licenses that apply to the specified jurisdiction
             scope.layoutCommons = function(jurisdictionKey, jurisdictionName) {
                scope.currentJurisdiction = jurisdictionKey;
               scope.currentJurisdictionName = jurisdictionName;
               // get all licenses, find the one that matches ours or just use the first one if we haven't chosen one yet
                gnCommonsService.getAllLicenses(scope.currentJurisdiction).then(
                 function(listOfLicenses) {
                  scope.license = listOfLicenses;
                  if (scope.currentLicenseUrl === '') {
                     scope.currentLicenseName = listOfLicenses[0].getName();
                     scope.currentLicenseUrl  = listOfLicenses[0].getUrl();
                     scope.currentLicenseImageUrl = listOfLicenses[0].getImageUrl(); 
										 scope.currentLicense = listOfLicenses[0];
                  } else {
										for (var i = 0;i < listOfLicenses.length;i++) {
											var lic = listOfLicenses[i];
											if (lic.getName() === scope.currentLicenseName) scope.currentLicense = lic;
										}
									}
									scope.addCommons(scope.currentLicense);
               });
               return false;
             };

             // add the specified license to the form as an XML fragment that will be saved into the 
             // metadata record when the editor form is submitted
             scope.addCommons = function(l) {
                scope.currentLicenseName = l.getName();
							 gnCurrentEdit.saving = true;
               gnCommonsService
                 .getXML(scope.currentJurisdiction, scope.namespace, l.getName(), l.getImageUrl(), l.getUrl(), scope.constraints.attribution, scope.constraints.derivative, scope.constraints.commercialUse).then(
                 function(data) {
                   // Add the fragment to the form, ready for saving later on
                  var xmlDeclaration =
                          '<?xml version="1.0" encoding="UTF-8"?>';
                   scope.snippet = data.replace(xmlDeclaration,'');
                   scope.snippetRef = '_X'+scope.elementRef;
									 gnCurrentEdit.saving = false;
               });
               scope.currentLicense = l;
               return false;
             };
           }
         };
       }]);
})();
