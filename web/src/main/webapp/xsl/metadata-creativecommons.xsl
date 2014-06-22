<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sc="scaling">

	<xsl:include href="res.xsl"/>

	<xsl:variable name="ccurl" select="/root/gui/schemas/iso19139.mcp/strings/creativeCommonsUrl"/>
	<xsl:variable name="iccurl" select="/root/gui/schemas/iso19139.mcp/strings/iCreativeCommonsUrl"/>

	<!--
	page content
	-->
	<xsl:template name="content">
		<xsl:variable name="juris" select="substring-before(substring-after(/root/creativecommons/@jurisdiction,concat($ccurl,'/international/')),'/')"/>
		<xsl:variable name="licenseselect">
			<xsl:choose>
				<xsl:when test="/root/creativecommons/dynamic">
					<xsl:value-of select="/root/creativecommons/dynamic/select"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="/root/gui/static/jurisdiction/@jurisdiction=$juris"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<table width="100%">
			
			<form name="cclicense" accept-charset="UTF-8" action="{/root/gui/locService}/metadata.creativecommons.set" method="post" enctype="multipart/form-data">
				<input type="hidden" name="id" value="{/root/creativecommons/@id}"/>
				<input type="hidden" name="version" value="{/root/creativecommons/@version}"/>
				<input type="hidden" name="type" value="{/root/creativecommons/@type}"/>
				<input type="hidden" name="jurisdiction" value="{/root/creativecommons/@jurisdiction}"/>

				<xsl:variable name="inurl" select="normalize-space(/root/creativecommons/@licenseurl)"/>
				<xsl:for-each select="/root/gui/static/jurisdiction[@name=$juris]/select/option">
					<xsl:variable name="url" select="normalize-space(@value)"/>
					<xsl:variable name="licName" select="normalize-space(.)"/>
					<xsl:variable name="imUrl" select="concat($iccurl,'/l',substring-after($url,concat($ccurl,'/licenses')),'88x31.png')"/>
					<tr>
					<td align="left">
						<xsl:choose>
							<xsl:when test="$inurl=$url or $inurl='none'">
								<input class="content" type="radio" value="{$url}#{$imUrl}#{$licName}" name="choice" checked="true">
									<a href="javascript:popWindow('{$url}')">
										<IMG align="middle" src="{$imUrl}" longdesc="{$url}" alt="{$licName}"></IMG>
									</a>
								</input>	

								<!-- save the original choice to reinstate on cancel -->
								<xsl:if test="$inurl=$url">
									<input name="originalchoice" type="hidden" value="{$url}#{$imUrl}#{$licName}"/>
								</xsl:if>
							</xsl:when>
							<xsl:otherwise>
								<input class="content" type="radio" value="{$url}#{$imUrl}#{$licName}" name="choice">
									<a href="javascript:popWindow('{$url}')">
										<IMG align="middle" src="{$imUrl}" longdesc="{$url}" alt="{$licName}"></IMG>
									</a>
								</input>	
							</xsl:otherwise>
						</xsl:choose>
								
					</td>
					<td align="left">
						<a href="javascript:popWindow('{$url}')"><xsl:value-of select="$licName"/></a>
					</td>
					</tr>
				</xsl:for-each>
				<tr>
					<td align="center" colspan="2" class="dots"/>
				</tr>
				<tr>
					<td align="center" colspan="2">
						<input class="content" type="submit" value="Set" name="btn"/>
						<!-- cancel button only shown if a choice had already been made -->
						<xsl:if test="$inurl!='none'">
							<xsl:text> </xsl:text>
							<input class="content" type="submit" value="Cancel" name="btn"/>
						</xsl:if>
					</td>
				</tr>
			</form>			
		</table>
	</xsl:template>

	<!-- ================================================================== -->
	
</xsl:stylesheet>
