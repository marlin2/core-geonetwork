<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:variable name="ccurl" select="/root/gui/schemas/iso19139.mcp/strings/creativeCommonsUrl"/>
	<xsl:variable name="iccurl" select="/root/gui/schemas/iso19139.mcp/strings/iCreativeCommonsUrl"/>

	<xsl:output method="xml"/>

	<!-- ================================================================== -->

	<xsl:template match="/root">
		<xsl:apply-templates select="response"/>
	</xsl:template>

	<!-- ================================================================== -->

	<xsl:template match="response">
		<xsl:variable name="juris" select="jurisdiction"/>

		<licenses>
			<jurisdiction><xsl:value-of select="$juris"/></jurisdiction>
			<xsl:for-each select="/root/gui/static/jurisdiction[@name=$juris]/select/option">
				<xsl:variable name="url" select="normalize-space(@value)"/>
				<xsl:variable name="licName" select="normalize-space(.)"/>
				<xsl:variable name="imUrl" select="concat($iccurl,'/l',substring-after($url,concat($ccurl,'/licenses')),'88x31.png')"/>
				<license>
					<url><xsl:value-of select="$url"/></url>
					<imageUrl><xsl:value-of select="$imUrl"/></imageUrl>
					<name><xsl:value-of select="$licName"/></name>
				</license>			
			</xsl:for-each>
		</licenses>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
