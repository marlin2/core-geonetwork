<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:gco="http://www.isotc211.org/2005/gco"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:mcp="http://bluenet3.antcrc.utas.edu.au/mcp"
	xmlns:gmd="http://www.isotc211.org/2005/gmd">

	<xsl:variable name="ccurl" select="/root/gui/schemas/iso19139.mcp/strings/creativeCommonsUrl"/>
	<xsl:variable name="iccurl" select="/root/gui/schemas/iso19139.mcp/strings/iCreativeCommonsUrl"/>

	<xsl:output method="xml"/>

	<xsl:template match="/root">
		<xsl:apply-templates select="/root/response"/>
	</xsl:template>

	<!-- ================================================================== -->

	<xsl:template match="response">
		<mcp:MD_Commons mcp:commonsType="Creative Commons" gco:isoType="gmd:MD_Constraints">
			<mcp:jurisdictionLink>
				<gmd:URL><xsl:value-of select="concat($ccurl,'/international/',jurisdiction,'/')"/></gmd:URL>
			</mcp:jurisdictionLink>
			<mcp:licenseLink>
				<gmd:URL><xsl:value-of select="licenseurl"/></gmd:URL>
			</mcp:licenseLink>
			<mcp:imageLink>
				<gmd:URL><xsl:value-of select="licenseimageurl"/></gmd:URL>
			</mcp:imageLink>
			<mcp:licenseName>
				<gco:CharacterString><xsl:value-of select="licensename"/></gco:CharacterString>
			</mcp:licenseName>
		</mcp:MD_Commons>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
