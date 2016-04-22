(function() {
  goog.provide('gn_editor_xml_service');

  goog.require('gn_schema_manager_service');

  var module = angular.module('gn_editor_xml_service',
      ['gn_schema_manager_service']);

  module.value('gnXmlTemplates', {
    'CRS': '<gmd:referenceSystemInfo ' +
        "xmlns:gmd='http://www.isotc211.org/2005/gmd' " +
        "xmlns:gco='http://www.isotc211.org/2005/gco'>" +
        '<gmd:MD_ReferenceSystem>' +
        '<gmd:referenceSystemIdentifier>' +
        '<gmd:RS_Identifier>' +
        '<gmd:code>' +
        '<gco:CharacterString>{{description}}' +
        '</gco:CharacterString>' +
        '</gmd:code>' +
        '<gmd:codeSpace>' +
        '<gco:CharacterString>{{codeSpace}}</gco:CharacterString>' +
        '</gmd:codeSpace>' +
        '<gmd:version>' +
        '<gco:CharacterString>{{version}}</gco:CharacterString>' +
        '</gmd:version>' +
        '</gmd:RS_Identifier>' +
        '</gmd:referenceSystemIdentifier>' +
        '</gmd:MD_ReferenceSystem>' +
        '</gmd:referenceSystemInfo>',
		'iso19115-3': '<mdb:referenceSystemInfo ' +
        "xmlns:mdb='http://standards.iso.org/iso/19115/-3/mdb/1.0' " +
        "xmlns:mcc='http://standards.iso.org/iso/19115/-3/mcc/1.0' " +
        "xmlns:mrs='http://standards.iso.org/iso/19115/-3/mrs/1.0' " +
        "xmlns:cit='http://standards.iso.org/iso/19115/-3/cit/1.0' " +
        "xmlns:gco='http://standards.iso.org/iso/19115/-3/gco/1.0'>" +
        '<mrs:MD_ReferenceSystem>' +
        '<mrs:referenceSystemIdentifier>' +
        '<mcc:MD_Identifier>' +
        '<mcc:authority>' +
        '<cit:CI_Citation>' +
				'<cit:title>' +
				'<gco:CharacterString>European Petroleum Survey Group (EPSG) Geodetic Parameter Registry</gco:CharacterString>' +
        '</cit:title>' +
        '<cit:date>' +
        '<cit:CI_Date>' +
        '<cit:date>' +
        '<gco:DateTime>2008-11-12</gco:DateTime>' +
        '</cit:date>' +
        '<cit:dateType>' +
        '<cit:CI_DateTypeCode codeList="codeListLocation" codeListValue="publication">publication</cit:CI_DateTypeCode>' +
        '</cit:dateType>' +
        '</cit:CI_Date>' +
        '</cit:date>' +
        '<cit:citedResponsibleParty>' +
        '<cit:CI_Responsibility>' +
        '<cit:role gco:nilReason="missing"/>' +
        '<cit:party>' +
        '<cit:CI_Organisation>' +
        '<cit:name>' +
        '<gco:CharacterString>European Petroleum Survey Group</gco:CharacterString>' +
        '</cit:name>' +
        '<cit:contactInfo>' +
        '<cit:CI_Contact>' +
        '<cit:onlineResource>' +
        '<cit:CI_OnlineResource>' +
        '<cit:linkage>' +
        '<gco:CharacterString>http://www.epsg-registry.org/</gco:CharacterString>' +
        '</cit:linkage>' +
        '</cit:CI_OnlineResource>' +
        '</cit:onlineResource>' +
        '</cit:CI_Contact>' +
        '</cit:contactInfo>' +
        '</cit:CI_Organisation>' +
        '</cit:party>' +
        '</cit:CI_Responsibility>' +
        '</cit:citedResponsibleParty>' +
        '</cit:CI_Citation>' +
        '</mcc:authority>' +
        '<mcc:code>' +
        '<gco:CharacterString>{{description}}' +
        '</gco:CharacterString>' +
        '</mcc:code>' +
        '<mcc:codeSpace>' +
        '<gco:CharacterString>{{codeSpace}}' +
        '</gco:CharacterString>' +
        '</mcc:codeSpace>' +
        '<mcc:version>' +
        '<gco:CharacterString>{{version}}</gco:CharacterString>' +
        '</mcc:version>' +
        '</mcc:MD_Identifier>' +
        '</mrs:referenceSystemIdentifier>' +
        '</mrs:MD_ReferenceSystem>' +
        '</mdb:referenceSystemInfo>'
  });

  module.factory('gnEditorXMLService',
      ['gnSchemaManagerService',
       'gnXmlTemplates',
       function(
       gnSchemaManagerService, gnXmlTemplates) {

				 /**
				  * Create xmlns attribute for element belonging to schema. If schema
					* not specified then search all schemas for first prefix match.
					*/
         var getNamespacesForElement = function(schema, elementName) {
          var nsDeclaration = [];
          var ns = elementName.split(':');
          if (ns.length === 2) {
           	nsDeclaration = ['xmlns:', ns[0], "='",
                       	gnSchemaManagerService.findNamespaceUri(ns[0], schema), 
												"'"];
          }
          return nsDeclaration.join('');
         };

         return {
           /**
            * Create a referenceSystemInfo XML snippet replacing
            * description, codeSpace and version properties of
            * the CRS defined for a schema or the default one for
						* iso19139 based stuff if no schema specified.
            */
           buildCRSXML: function(crs, schema) {
             var replacement = ['description', 'codeSpace', 'version'];
						 var xml;
						 if (schema) {
						 	 xml = gnXmlTemplates[schema];
						 } else {
             	 xml = gnXmlTemplates['CRS'];
						 }
             angular.forEach(replacement, function(key) {
               xml = xml.replace('{{' + key + '}}', crs[key]);
             });
             return xml;
           },
           /**
            * Create an XML snippet to be inserted in a form field.
            * The element name will be the parent element of the
            * snippet provided and come from the schema provided.
            *
            * The element namespace should be defined
            * in the list of namespaces obtained from gnSchemaManager
            */
           buildXML: function(schema, elementName, snippet) {
             if (snippet.match(/^<\?xml/g)) {
               var xmlDeclaration =
                   '<?xml version="1.0" encoding="UTF-8"?>';
               snippet = snippet.replace(xmlDeclaration, '');
             }

             var nsDeclaration = getNamespacesForElement(schema, elementName);

             var tokens = [
               '<', elementName,
               ' ', nsDeclaration, '>',
               snippet, '</', elementName, '>'];
             return tokens.join('');
           },
           /**
            * Build an XML snippet for the element name
            * and xlink provided from schema provided.
            */
           buildXMLForXlink: function(schema, elementName, xlink) {
             var nsDeclaration = getNamespacesForElement(schema, elementName);

             // Escape & in XLink url
             xlink = xlink.replace('&', '&amp;');

             var tokens = [
               '<', elementName,
               ' ', nsDeclaration,
               ' xmlns:xlink="', gnSchemaManagerService.findNamespaceUri("xlink"), '"',
               ' xlink:href="',
               xlink, '"/>'];
             return tokens.join('');
           }
         };
       }]);
})();
