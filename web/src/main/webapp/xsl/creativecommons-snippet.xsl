<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:gco="http://www.isotc211.org/2005/gco"
	xmlns:gmd="http://www.isotc211.org/2005/gmd">

	<xsl:variable name="ccurl" select="//strings/creativeCommonsUrl/text()"/>

	<xsl:output method="xml"/>

	<xsl:template match="/root">
		<xsl:apply-templates select="/root/response"/>
	</xsl:template>



	<!-- ================================================================== -->

	<xsl:template match="response">
		<!-- define namespace -->
		<xsl:variable name="mcpns" select="ns"/>
		<!-- now build the element using the namespace -->
		<xsl:element name="mcp:MD_Commons" namespace="{$mcpns}">
			<xsl:attribute name="mcp:commonsType" namespace="{$mcpns}">Creative Commons</xsl:attribute>
			<xsl:attribute name="gco:isoType">gmd:MD_Constraints</xsl:attribute>
			<xsl:element name="mcp:jurisdictionLink" namespace="{$mcpns}">
				<gmd:URL><xsl:value-of select="concat($ccurl[1],'/international/',jurisdiction,'/')"/></gmd:URL>
			</xsl:element>
			<xsl:element name="mcp:licenseLink" namespace="{$mcpns}">
				<gmd:URL><xsl:value-of select="licenseurl"/></gmd:URL>
			</xsl:element>
			<xsl:element name="mcp:imageLink" namespace="{$mcpns}">
				<gmd:URL><xsl:value-of select="licenseimageurl"/></gmd:URL>
			</xsl:element>
			<xsl:element name="mcp:licenseName" namespace="{$mcpns}">
				<gco:CharacterString><xsl:value-of select="licensename"/></gco:CharacterString>
			</xsl:element>
		</xsl:element>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
