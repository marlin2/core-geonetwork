<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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
					<xsl:variable name="short" select="substring-before(substring-after(@value,'http://creativecommons.org/international/'),'/')"/>
					<shortname>
						<xsl:choose>
							<xsl:when test="normalize-space($short)!=''">
								<xsl:value-of select="$short"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="'International'"/>
							</xsl:otherwise>
						</xsl:choose>
					</shortname>
					<name><xsl:value-of select="."/></name>
					<url><xsl:value-of select="@value"/></url>
				</jurisdiction>			
			</xsl:for-each>
		</jurisdictions>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
