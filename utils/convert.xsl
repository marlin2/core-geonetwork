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

  <!-- We will produce an output document that is XML, so indent the elements nicely in order to retain readability -->
	<xsl:output method="xml" indent="yes"/>

  <xsl:template match="*:MD_Metadata[namespace-uri()=$oldmcp]" priority="5">
    <xsl:message><xsl:copy-of select="$idcContact"/></xsl:message>
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
          gmd:dateStamp|
          gmd:metadataStandardName|
          gmd:metadataStandardVersion|
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
  <!-- convert mcp:EX_Extent to gmd:EX_Extent because we don't have
       mcp:taxonomicCoverage in mcp-2.0  -->

  <xsl:template match="oldmcp:EX_Extent">
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

  <xsl:template match="@xlink:href[.='http://www.marlin.csiro.au/geonetwork/srv/eng/subtemplate?uuid=urn:marlin.csiro.au:person:125_person_organisation']">
    <xsl:attribute name="xlink:href">local://xml.metadata.get?uuid=urn:marlin.csiro.au:person:125_person_organisation</xsl:attribute>
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

  <xsl:template match="gmd:URL">
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

	<xsl:template match="@*|node()">
		 <xsl:copy copy-namespaces="no">
			  <xsl:apply-templates select="@*|node()"/>
		 </xsl:copy>
	</xsl:template>

	<!-- ================================================================= -->

</xsl:stylesheet>
