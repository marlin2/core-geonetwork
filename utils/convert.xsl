<?xml version="1.0" encoding="UTF-8"?>

<!-- This XSLT will read and process an input mcp document and convert it to    
     mcp version 2.0. -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:oldmcp="http://bluenet3.antcrc.utas.edu.au/mcp"
  xmlns:mcp="http://schemas.aodn.org.au/mcp-2.0"
  xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:gco="http://www.isotc211.org/2005/gco"
  xmlns:srv="http://www.isotc211.org/2005/srv"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:gts="http://www.isotc211.org/2005/gts"
  xmlns:gmx="http://www.isotc211.org/2005/gmx"
  xmlns:gn="http://www.fao.org/geonetwork"
  xmlns:geonet="http://www.fao.org/geonetwork"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="gn geonet oldmcp">

  <xsl:param name="machine" select="'https://www.marlin.csiro.au'"/>

  <xsl:variable name="oldmcp" select="'http://bluenet3.antcrc.utas.edu.au/mcp'"/>
  <xsl:variable name="marlinUrl" select="'http://www.marlin.csiro.au'"/>
  <xsl:variable name="idcContact" select="document('http://www.marlin.csiro.au/geonetwork/srv/eng/subtemplate?uuid=urn:marlin.csiro.au:person:125_person_organisation')"/>

  <xsl:variable name="mapping" select="document('mcp-equipment/equipmentToDataParamsMapping.xml')"/>

  <!-- The csv layout for each element in the above file is:
                          1)OA_EQUIPMENT_ID,
                          2)OA_EQUIPMENT_LABEL,
                          3)AODN_PLATFORM,
                          4)Platform IRI,
                          5)AODN_INSTRUMENT,
                          6)Instrument IRI,
                          7)AODN_PARAMETER,
                          8)Parameter IRI,
                          9)AODN_UNITS,
                          10)UNITS IRI
        NOTE: can be multiple rows for each equipment keyword -->

  <xsl:variable name="equipThesaurus" select="'geonetwork.thesaurus.register.equipment.urn:marlin.csiro.au:Equipment'"/>

  <!-- We will produce an output document that is XML, so indent the elements nicely in order to retain readability -->
	<xsl:output method="xml" indent="yes"/>

  <xsl:template match="*:MD_Metadata[namespace-uri()=$oldmcp]" priority="5">
    <!-- <xsl:message><xsl:copy-of select="$idcContact"/></xsl:message> -->
    <xsl:element name="mcp:MD_Metadata">
      <xsl:attribute name="xsi:schemaLocation">http://schemas.aodn.org.au/mcp-2.0 http://schemas.aodn.org.au/mcp-2.0/schema.xsd http://www.isotc211.org/2005/srv http://schemas.opengis.net/iso/19139/20060504/srv/srv.xsd http://www.isotc211.org/2005/gmx http://www.isotc211.org/2005/gmx/gmx.xsd http://rs.tdwg.org/dwc/terms/ http://schemas.aodn.org.au/mcp-2.0/mcpDwcTerms.xsd</xsl:attribute>
      <xsl:namespace name="mcp" select="'http://schemas.aodn.org.au/mcp-2.0'"/>
      <xsl:namespace name="gmd" select="'http://www.isotc211.org/2005/gmd'"/>
      <xsl:namespace name="gco" select="'http://www.isotc211.org/2005/gco'"/>
      <xsl:namespace name="srv" select="'http://www.isotc211.org/2005/srv'"/>
      <xsl:namespace name="gts" select="'http://www.isotc211.org/2005/gts'"/>
      <xsl:namespace name="gtx" select="'http://www.isotc211.org/2005/gtx'"/>
      <xsl:namespace name="gmx" select="'http://www.isotc211.org/2005/gmx'"/>
      <xsl:namespace name="gml" select="'http://www.opengis.net/gml'"/>
      <xsl:namespace name="xsi" select="'http://www.w3.org/2001/XMLSchema-instance'"/>
      <xsl:namespace name="xlink" select="'http://www.w3.org/1999/xlink'"/>
      <xsl:apply-templates select="@*[name()!='xsi:schemaLocation']"/>
      <xsl:apply-templates select="
          gmd:fileIdentifier|
          gmd:language|
          gmd:characterSet|
          gmd:parentIdentifier|
          gmd:hierarchyLevel|
          gmd:hierarchyLevelName"/>
      <xsl:call-template name="addIDCContact"/>
      <xsl:apply-templates select="
          gmd:dateStamp"/>
      <gmd:metadataStandardName>
       <gco:CharacterString>Australian Marine Community Profile of ISO 19115:2005/19139</gco:CharacterString>
      </gmd:metadataStandardName>
      <gmd:metadataStandardVersion>
       <gco:CharacterString>2.0</gco:CharacterString>
      </gmd:metadataStandardVersion>
      <xsl:apply-templates select="
          gmd:dataSetURI|
          gmd:locale|
          gmd:spatialRepresentationInfo|
          gmd:referenceSystemInfo|
          gmd:metadataExtensionInfo|
          gmd:identificationInfo|
          gmd:contentInfo|
          gmd:distributionInfo|
          gmd:dataQualityInfo|
          gmd:portrayalCatalogueInfo|
          gmd:metadataConstraints|
          gmd:applicationSchemaInfo|
          gmd:metadataMaintenance|
          gmd:series|
          gmd:describes|
          gmd:propertyType|
          gmd:featureType|
          gmd:featureAttribute"/>
       <!-- all the rest - usually mcp stuff -->
       <xsl:apply-templates select="
        *[namespace-uri()!='http://www.isotc211.org/2005/gmd' and
          namespace-uri()!='http://www.isotc211.org/2005/srv']"/>
    </xsl:element>
  </xsl:template>

  <!-- ================================================================= -->

  <xsl:template match="oldmcp:MD_DataIdentification" priority="100">
    <mcp:MD_DataIdentification>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates select="gmd:citation"/>
      <xsl:apply-templates select="gmd:abstract"/>
      <xsl:apply-templates select="gmd:purpose"/>
      <xsl:apply-templates select="gmd:credit"/>
      <xsl:apply-templates select="gmd:status"/>
      <xsl:apply-templates select="gmd:pointOfContact"/>
      <xsl:apply-templates select="gmd:resourceMaintenance"/>
      <xsl:apply-templates select="gmd:graphicOverview"/>
      <xsl:apply-templates select="gmd:resourceFormat"/>
      <xsl:apply-templates select="gmd:descriptiveKeywords[count(descendant::gmd:keyword)>0]"/>
      <xsl:apply-templates select="gmd:resourceSpecificUsage"/>
      <xsl:apply-templates select="gmd:resourceConstraints"/>
      <xsl:apply-templates select="gmd:aggregationInfo"/>
      <xsl:apply-templates select="gmd:spatialRepresentationType"/>
      <xsl:choose>
        <xsl:when test="normalize-space(gmd:spatialResolution//gmd:equivalentScale//gco:denominator/gco:Integer)=''">
          <gmd:spatialResolution>
            <gmd:MD_Resolution>
              <gmd:equivalentScale>
                <gmd:MD_RepresentativeFraction>
                  <gmd:denominator gco:nilReason="inapplicable"/>
                </gmd:MD_RepresentativeFraction>
              </gmd:equivalentScale>
            </gmd:MD_Resolution>
          </gmd:spatialResolution>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="gmd:spatialResolution"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="gmd:language"/>
      <xsl:apply-templates select="gmd:characterSet"/>
      <xsl:apply-templates select="gmd:topicCategory"/>
      <xsl:apply-templates select="gmd:environmentDescription"/>
      <xsl:apply-templates select="gmd:extent"/>
      <xsl:apply-templates select="gmd:supplementalInformation"/>
      <xsl:apply-templates select="oldmcp:samplingFrequency"/>

      <!-- Add/Overwrite data parameters if we have an equipment keyword that matches one in our mapping -->
      <!-- if we have an equipment thesaurus with a match keyword then we process -->

      <xsl:variable name="equipPresent">
       <xsl:for-each select="//gmd:descriptiveKeywords/gmd:MD_Keywords[normalize-space(gmd:thesaurusName/gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor)=$equipThesaurus]/gmd:keyword/gmx:Anchor">
        <xsl:element name="dp">
          <mcp:dataParameters>
           <mcp:DP_DataParameters>
           <xsl:variable name="currentKeyword" select="text()"/>
           <!-- <xsl:message>Automatically created dp from <xsl:value-of select="$currentKeyword"/></xsl:message> -->
           <xsl:for-each select="$mapping/map/equipment">
              <xsl:variable name="tokens" select="tokenize(string(),',')"/>
              <!-- <xsl:message>Checking <xsl:value-of select="$tokens[2]"/></xsl:message> -->
              <xsl:if test="$currentKeyword=$tokens[2]">
                 <!-- <xsl:message>KW MATCHED TOKEN: <xsl:value-of select="$tokens[2]"/></xsl:message> -->
                 <xsl:call-template name="fillOutDataParameters">
                    <xsl:with-param name="tokens" select="$tokens"/>
                 </xsl:call-template>
              </xsl:if>
           </xsl:for-each>
           </mcp:DP_DataParameters>
          </mcp:dataParameters>
        </xsl:element>
       </xsl:for-each>
      </xsl:variable>

      <!-- Now copy the constructed data parameters into the record -->
      <xsl:for-each select="$equipPresent/dp/mcp:dataParameters[count(mcp:DP_DataParameters/*) > 0]">
        <xsl:copy-of select="."/>
      </xsl:for-each>
      <xsl:apply-templates select="oldmcp:resourceContactInfo"/>

    </mcp:MD_DataIdentification>
  </xsl:template>

	<!-- ================================================================= -->

  <xsl:template name="fillOutDataParameters">
    <xsl:param name="tokens"/>

    <mcp:dataParameter>
      <mcp:DP_DataParameter>
      	<mcp:parameterName>
					<mcp:DP_Term>
						<mcp:term>
							<gco:CharacterString><xsl:value-of select="$tokens[7]"/></gco:CharacterString>
						</mcp:term>
						<mcp:type>
							<mcp:DP_TypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_TypeCode" codeListValue="longName">longName</mcp:DP_TypeCode>
						</mcp:type>
						<mcp:usedInDataset>
							<gco:Boolean>false</gco:Boolean>
						</mcp:usedInDataset>
						<mcp:vocabularyRelationship>
						  <mcp:DP_VocabularyRelationship>
						    <mcp:relationshipType>
							    <mcp:DP_RelationshipTypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_RelationshipTypeCode" codeListValue="skos:exactmatch">skos:exactmatch</mcp:DP_RelationshipTypeCode>
						    </mcp:relationshipType>
						    <mcp:vocabularyTermURL>
							    <gmd:URL><xsl:value-of select="$tokens[8]"/></gmd:URL>
						    </mcp:vocabularyTermURL>
						    <mcp:vocabularyListURL gco:nilReason="inapplicable"/>
						  </mcp:DP_VocabularyRelationship>
						</mcp:vocabularyRelationship>
					</mcp:DP_Term>
			  </mcp:parameterName>
				<mcp:parameterUnits>
					<mcp:DP_Term>
						<mcp:term>
							<gco:CharacterString><xsl:value-of select="$tokens[9]"/></gco:CharacterString>
						</mcp:term>
						<mcp:type>
							<mcp:DP_TypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_TypeCode" codeListValue="longName">longName</mcp:DP_TypeCode>
						</mcp:type>
						<mcp:usedInDataset>
							<gco:Boolean>false</gco:Boolean>
						</mcp:usedInDataset>
						<mcp:vocabularyRelationship>
						  <mcp:DP_VocabularyRelationship>
						    <mcp:relationshipType>
							    <mcp:DP_RelationshipTypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_RelationshipTypeCode" codeListValue="skos:exactmatch">skos:exactmatch</mcp:DP_RelationshipTypeCode>
						    </mcp:relationshipType>
						    <mcp:vocabularyTermURL>
							    <gmd:URL><xsl:value-of select="$tokens[10]"/></gmd:URL>
						    </mcp:vocabularyTermURL>
						    <mcp:vocabularyListURL gco:nilReason="inapplicable"/>
						  </mcp:DP_VocabularyRelationship>
						</mcp:vocabularyRelationship>
					</mcp:DP_Term>
				</mcp:parameterUnits>
				<mcp:parameterMinimumValue gco:nilReason="missing">
					<gco:CharacterString/>
				</mcp:parameterMinimumValue>
				<mcp:parameterMaximumValue gco:nilReason="missing">
					<gco:CharacterString/>
				</mcp:parameterMaximumValue>
        <mcp:parameterDeterminationInstrument>
					<mcp:DP_Term>
						<mcp:term>
							<gco:CharacterString><xsl:value-of select="$tokens[5]"/></gco:CharacterString>
						</mcp:term>
						<mcp:type>
							<mcp:DP_TypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_TypeCode" codeListValue="longName">longName</mcp:DP_TypeCode>
						</mcp:type>
						<mcp:usedInDataset>
							<gco:Boolean>false</gco:Boolean>
						</mcp:usedInDataset>
						<mcp:vocabularyRelationship>
						  <mcp:DP_VocabularyRelationship>
						    <mcp:relationshipType>
							    <mcp:DP_RelationshipTypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_RelationshipTypeCode" codeListValue="skos:exactmatch">skos:exactmatch</mcp:DP_RelationshipTypeCode>
						    </mcp:relationshipType>
						    <mcp:vocabularyTermURL>
							    <gmd:URL><xsl:value-of select="$tokens[6]"/></gmd:URL>
						    </mcp:vocabularyTermURL>
						    <mcp:vocabularyListURL gco:nilReason="inapplicable"/>
						  </mcp:DP_VocabularyRelationship>
						</mcp:vocabularyRelationship>
					</mcp:DP_Term>
				</mcp:parameterDeterminationInstrument>
        <mcp:platform>
					<mcp:DP_Term>
						<mcp:term>
							<gco:CharacterString><xsl:value-of select="$tokens[3]"/></gco:CharacterString>
						</mcp:term>
						<mcp:type>
							<mcp:DP_TypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_TypeCode" codeListValue="longName">longName</mcp:DP_TypeCode>
						</mcp:type>
						<mcp:usedInDataset>
							<gco:Boolean>false</gco:Boolean>
						</mcp:usedInDataset>
						<mcp:vocabularyRelationship>
						  <mcp:DP_VocabularyRelationship>
						    <mcp:relationshipType>
							    <mcp:DP_RelationshipTypeCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#DP_RelationshipTypeCode" codeListValue="skos:exactmatch">skos:exactmatch</mcp:DP_RelationshipTypeCode>
						    </mcp:relationshipType>
						    <mcp:vocabularyTermURL>
							    <gmd:URL><xsl:value-of select="$tokens[4]"/></gmd:URL>
						    </mcp:vocabularyTermURL>
						    <mcp:vocabularyListURL gco:nilReason="inapplicable"/>
						  </mcp:DP_VocabularyRelationship>
						</mcp:vocabularyRelationship>
					</mcp:DP_Term>
				</mcp:platform>
      </mcp:DP_DataParameter>
    </mcp:dataParameter>
  </xsl:template>
	

	<!-- ================================================================= -->
  <!-- convert mcp:EX_Extent to gmd:EX_Extent because we don't have
       mcp:taxonomicCoverage in mcp-2.0  -->

  <xsl:template priority="5" match="oldmcp:EX_Extent">
    <xsl:element name="gmd:EX_Extent">
      <xsl:apply-templates select="*"/>
    </xsl:element>
  </xsl:template>

	<!-- ================================================================= -->
  <!-- convert any element that doesn't match above but has old mcp namespace
       into newmcp namespace -->

  <xsl:template match="*[namespace-uri()=$oldmcp]">
    <xsl:variable name="newname" select="concat('mcp:',local-name())"/>
    <xsl:element name="{$newname}">
      <xsl:apply-templates select="@*|*"/>
    </xsl:element>
  </xsl:template>

	<!-- ================================================================= -->
  <!-- convert attribute that has old mcp namespace
       into newmcp namespace -->

  <xsl:template match="@*[namespace-uri()=$oldmcp]">
    <xsl:variable name="newname" select="concat('mcp:',local-name())"/>
    <xsl:attribute name="{$newname}"><xsl:copy-of select="."/></xsl:attribute>
  </xsl:template>

	<!-- ================================================================= -->
  <!-- match the @codeList attribute and put the correct url of the mcp 2.0
       codelists -->

  <xsl:template match="@codeList">
    <xsl:variable name="elementName" select="local-name(..)"/>
    <xsl:attribute name="codeList"><xsl:value-of select="concat('http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#',$elementName)"/></xsl:attribute>
  </xsl:template>

	<!-- ================================================================= -->
  <!-- match the @xlink:href attribute and replace http://www.marlin.csiro.au
       part with the machine name param 
       (https://www.marlin.csiro.au by default) -->

  <xsl:template match="@xlink:href">
	  <xsl:variable name="url" select="replace(.,':80','')"/>
<!--
    <xsl:message><xsl:value-of select="if (contains($url,$marlinUrl)) then
                              concat($machine,substring-after($url,$marlinUrl)) 
                            else $url"/></xsl:message>
-->
    <xsl:attribute name="xlink:href">
      <xsl:value-of select="
        if (contains($url,$marlinUrl)) then
          concat($machine,substring-after($url,$marlinUrl)) 
        else $url"/>
    </xsl:attribute>
  </xsl:template>

	<!-- ================================================================= -->

  <xsl:template match="@xlink:href[contains(.,'/geonetwork/srv/eng/subtemplate')]" priority="5">
	  <xsl:variable name="url" select="replace(replace(.,':80',''),'&amp;process=undefined','')"/>
    <xsl:attribute name="xlink:href"><xsl:value-of select="replace($url,'http://www.marlin.csiro.au/geonetwork/srv/eng/subtemplate','local://xml.metadata.get')"/></xsl:attribute>
  </xsl:template>

	<!-- ================================================================= -->

  <xsl:template name="addIDCContact">
    <xsl:variable name="org" select="$idcContact//*:name/gco:CharacterString"/>
    <xsl:element name="gmd:contact">
      <xsl:element name="gmd:CI_ResponsibleParty">
        <xsl:element name="gmd:organisationName"><gco:CharacterString><xsl:value-of select="$org"/></gco:CharacterString></xsl:element>
        <xsl:element name="gmd:positionName"><gco:CharacterString><xsl:value-of select="$idcContact//*:positionName/gco:CharacterString"/></gco:CharacterString></xsl:element>
        <xsl:element name="gmd:contactInfo">
          <xsl:element name="gmd:CI_Contact">
            <xsl:copy-of select="$idcContact//*:contactInfo/gmd:CI_Contact/gmd:address" copy-namespaces="no"/>
            <gmd:onlineResource>
              <gmd:CI_OnlineResource>
                <gmd:linkage>
                  <gmd:URL>https://research.csiro.au/oa-idc/</gmd:URL>
                </gmd:linkage>
                <gmd:protocol>
                  <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
                </gmd:protocol>
                <gmd:name>
                  <gco:CharacterString><xsl:value-of select="concat($org,' homepage')"/></gco:CharacterString>
                </gmd:name>
                <gmd:description>
                  <gco:CharacterString><xsl:value-of select="concat('Link to ',$org,' homepage')"/></gco:CharacterString>
                </gmd:description>
              </gmd:CI_OnlineResource>
            </gmd:onlineResource>
          </xsl:element>
        </xsl:element>
        <gmd:role>
          <gmd:CI_RoleCode codeList="http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#CI_RoleCode"
                             codeListValue="pointOfContact">pointOfContact</gmd:CI_RoleCode>
        </gmd:role>
      </xsl:element>
    </xsl:element>
  </xsl:template>

	<!-- ================================================================= -->

  <xsl:template match="gmd:URL|gco:CharacterString[starts-with(.,'http')]">
	  <xsl:variable name="url" select="replace(.,':80','')"/>
    <xsl:copy copy-namespaces="no">
      <xsl:value-of select="if (starts-with($url,'http:')) then 
                              replace($url,'http:','https:')
                            else $url"/>
    </xsl:copy>
  </xsl:template>

	<!-- ================================================================= -->

  <xsl:template match="gmd:URL[../../gmd:protocol/gco:CharacterString='WWW:LINK-1.0-http--metadata-URL']">
	  <xsl:variable name="uuid" select="substring-after(.,'uuid=')"/>
    <xsl:copy copy-namespaces="no">
      <xsl:value-of select="concat($machine,'/geonetwork/srv/eng/catalog.search#/metadata/',$uuid)"/>
    </xsl:copy>
  </xsl:template>

	<!-- ================================================================= -->
  <!-- Remove empty date blocks -->

  <xsl:template match="gmd:date[descendant::gco:Date='']"/>
  <xsl:template match="gmd:date[descendant::gco:DateTime='']"/>

	<!-- ================================================================= -->
  <!-- Remove dq report pass/fail - not used or valid in marlin -->

  <xsl:template match="gmd:pass[ancestor::gmd:report]">
    <gmd:pass gco:nilReason='inapplicable'/>
  </xsl:template>

	<!-- ================================================================= -->

	<xsl:template match="@*|node()">
		 <xsl:copy copy-namespaces="no">
			  <xsl:apply-templates select="@*|node()"/>
		 </xsl:copy>
	</xsl:template>

	<!-- ================================================================= -->

</xsl:stylesheet>
