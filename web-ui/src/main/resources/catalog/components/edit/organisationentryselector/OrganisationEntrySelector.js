(function() {
  goog.provide('gn_organisation_entry_selector');



  goog.require('gn_editor_xml_service');
  goog.require('gn_metadata_manager_service');
  goog.require('gn_schema_manager_service');

  var module = angular.module('gn_organisation_entry_selector',
      ['gn_metadata_manager_service', 'gn_schema_manager_service',
       'gn_editor_xml_service']);

  /**
   *
   *
   */
  module.directive('gnOrganisationEntrySelector',
      ['$rootScope', '$timeout', '$q', '$http',
        'gnEditor', 'gnSchemaManagerService',
        'gnEditorXMLService', 'gnHttp', 'gnConfig',
        'gnCurrentEdit', 'gnConfigService', 'gnElementsMap',
        function($rootScope, $timeout, $q, $http, 
            gnEditor, gnSchemaManagerService, 
            gnEditorXMLService, gnHttp, gnConfig, 
            gnCurrentEdit, gnConfigService, gnElementsMap) {

         return {
           restrict: 'A',
           replace: false,
           scope: {
             mode: '@gnOrganisationEntrySelector',
             schema: '@',
             elementName: '@',
             elementRef: '@',
             domId: '@',
             tagName: '@',
             paramName: '@',
             templateAddAction: '@'
           },
           templateUrl: '../../catalog/components/edit/' +
           'organisationentryselector/partials/' +
           'organisationentryselector.html',
           link: function(scope, element, attrs) {
             // Separator between each org XML
             // snippet
             var separator = '&&&';
             // URL used for creating XLink. Could be good to have
             // that as settings to define local:// or http:// xlinks.
             var url = gnConfigService.getServiceURL() + 'eng/subtemplate';
             scope.gnConfig = gnConfig;
             scope.templateAddAction = scope.templateAddAction === 'true';

             // Search only for mcp:CI_Organisation subtemplate 
             scope.params = {
               _root: 'mcp:CI_Organisation',
               _isTemplate: 's',
               fast: 'false'
             };

             scope.snippet = null;
             scope.snippetRef = gnEditor.
             buildXMLFieldName(scope.elementRef, scope.elementName);

             scope.add = function() {
               gnEditor.add(gnCurrentEdit.id,
                   scope.elementRef, scope.elementName,
                   scope.domId, 'before').then(function() {
                 if (scope.templateAddAction) {
                   gnEditor.save(gnCurrentEdit.id, true);
                 }
               });
               return false;
             };

             scope.addOrganisation = function(org, usingXlink) {
               if (!(org instanceof Array)) {
                 org = [org];
               }

               scope.snippet = '';
               var snippets = [];

               var checkState = function() {
                 if (snippets.length === org.length) {
                   scope.snippet = snippets.join(separator);

                   // Clean results
                   // TODO: should call clean result from searchFormController
                   //                   scope.searchResults.records = null;
                   //                   scope.searchResults.count = null;

                    $timeout(function() {
                      // Save the metadata and refresh the form
                      gnEditor.save(gnCurrentEdit.id, true);
                    });
                 }
               };

               angular.forEach(org, function(c) {
                 var id = c['geonet:info'].id,
                 uuid = c['geonet:info'].uuid;
                 var params = {uuid: uuid};

                 gnHttp.callService(
                     'subtemplate', params).success(function(xml) {
                   if (usingXlink) {
                     snippets.push(gnEditorXMLService.
                     buildXMLForXlink(scope.schema, scope.elementName,
                         url + '?uuid=' + uuid + '&process=' + params.process));
                   } else {
                     snippets.push(gnEditorXMLService.
                     buildXML(scope.schema, scope.elementName, xml));
                   }
                   checkState();
                 });
               });

               return false;
             };

           }
         };
       }]);
})();
