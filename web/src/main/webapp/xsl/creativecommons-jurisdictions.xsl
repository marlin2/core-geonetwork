<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:variable name="ccurl" select="/root/gui/schemas/iso19139.mcp/strings/creativeCommonsUrl"/>
	<xsl:variable name="iccurl" select="/root/gui/schemas/iso19139.mcp/strings/iCreativeCommonsUrl"/>

	<xsl:output method="xml"/>

	<xsl:template match="/root">
		<xsl:apply-templates select="/root/gui/staticjurisdictions"/>
	</xsl:template>

	<!-- ================================================================== -->

	<xsl:template match="staticjurisdictions">
		<jurisdictions>
			<xsl:for-each select="option">
				<xsl:sort select="."/>
				<jurisdiction>
					<shortname><xsl:value-of select="substring-before(substring-after(@value,'http://creativecommons.org/international/'),'/')"/></shortname>
					<name><xsl:value-of select="."/></name>
					<url><xsl:value-of select="@value"/></url>
				</jurisdiction>			
			</xsl:for-each>
		</jurisdictions>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
