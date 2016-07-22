<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
		xmlns:app="http://www.deegree.org/app"
		xmlns:gml="http://www.opengis.net/gml"
		xmlns:wfs="http://www.opengis.net/wfs"
    xmlns:dc="http://purl.org/dc/elements/1.1/" 
    xmlns:dct="http://purl.org/dc/terms/" 
    xmlns:fn="http://www.w3.org/2005/02/xpath-functions" 
    xmlns:foaf="http://xmlns.com/foaf/0.1/"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"		
		xmlns:skos="http://www.w3.org/2004/02/skos/core#"
		xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />

	<xsl:variable name="uuid" select="'urn:lsid:marinespecies.org:taxname'"/>
  <xsl:variable name="aboutScheme" select="concat($uuid,':conceptscheme:20130300')"/>

	<xsl:key name="parents" match="//gml:featureMember" use="app:Taxon/app:parentid"/>

	<!-- 
			 This xslt transforms GetFeature outputs from the WFS Marlin database
	     into ISO metadata fragments. The fragments are used by GeoNetwork to 
			 build ISO metadata records.
	 -->

	<xsl:template match="wfs:FeatureCollection">
		<rdf:RDF>
			<xsl:message>Processing <xsl:value-of select="@numberOfFeatures"/></xsl:message>
      <xsl:variable name="df">[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]</xsl:variable>
			<skos:ConceptScheme rdf:about="{$aboutScheme}">
      	<dc:identifier><xsl:value-of select="$uuid"/></dc:identifier>
        <dc:title>WoRMS - World Register of Marine Species</dc:title>
        <dc:description>WoRMS - World Register of Marine Species</dc:description>
        <dc:URI>http://www.marlin.csiro.au/geonetwork/srv/eng/xml.metadata.get?uuid=urn:lsid:marinespecies.org:taxname</dc:URI>
        <dct:modified>2013-03-01</dct:modified>
        <dc:creator>
           <foaf:Organization>
              <foaf:name>WoRMS Editorial Board (2016). World Register of Marine Species. Available from http://www.marinespecies.org at VLIZ. Accessed 2016-07-22. doi:10.14284/170</foaf:name>
           </foaf:Organization>
        </dc:creator>
      	<dct:issued><xsl:value-of select="format-dateTime(current-dateTime(),$df)"/></dct:issued>

      	<!-- get all the top concepts and create the a skos:topConcepts element -->
      	<xsl:for-each select="//gml:featureMember/app:Taxon[app:parentid=1 and app:aphiaid=1]">
        	<skos:hasTopConcept rdf:resource="{concat($uuid,':',app:aphiaid)}"/>
      	</xsl:for-each>
    	</skos:ConceptScheme>

			<!-- Now do each concept: 1 per featureMember -->
			<xsl:apply-templates select="gml:featureMember">
				<xsl:with-param name="uuid" select="$uuid"/>
			</xsl:apply-templates>
		</rdf:RDF>
	</xsl:template>

	<!-- process a record from the worms table -->
	<xsl:template name="addTaxonRegisterItem">
		<xsl:param name="keywordUuid"/>

		<xsl:variable name="me" select="app:aphiaid"/>
    <xsl:variable name="children" as="node()">
      <children>
      	<xsl:for-each select="key('parents',$me)">
      		<xsl:copy-of select="app:Taxon"/>
				</xsl:for-each>
			</children>
    </xsl:variable>

    <xsl:message><xsl:value-of select="count($children/app:Taxon)"/></xsl:message>

		<skos:Concept rdf:about="{$keywordUuid}">
      <skos:prefLabel xml:lang="en"><xsl:value-of select="app:tu_displayname"/></skos:prefLabel>
      <skos:inScheme rdf:resource="{$aboutScheme}"/>
      <skos:scopeNote xml:lang="en"><xsl:value-of select="concat(app:tu_displayname,': ',app:rank)"/></skos:scopeNote>
      <xsl:for-each select="$children/app:Taxon">
        <skos:narrower rdf:resource="{concat($uuid,':',app:aphiaid)}"/>
      </xsl:for-each>
      <skos:broader rdf:resource="{concat($uuid,':',app:parentid)}"/>
		</skos:Concept>
	</xsl:template>

	<!-- process the featureMember elements in WFS response -->
	<xsl:template match="gml:featureMember">
		<xsl:apply-templates select="app:Taxon"/>
	</xsl:template>

	<!-- process the app:Taxon in WFS response -->
	<xsl:template match="app:Taxon">
			<xsl:call-template name="addTaxonRegisterItem">
				<xsl:with-param name="keywordUuid" select="concat($uuid,':',app:aphiaid)"/>
			</xsl:call-template>
	</xsl:template>

</xsl:stylesheet>
