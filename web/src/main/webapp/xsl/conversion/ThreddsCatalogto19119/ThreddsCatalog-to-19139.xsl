<?xml version="1.0" encoding="UTF-8"?>
<!--  Mapping between Thredds Catalog (version 1.0.1) to ISO19139 -->
<xsl:stylesheet xmlns:gmd="http://www.isotc211.org/2005/gmd"
                xmlns:gco="http://www.isotc211.org/2005/gco"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                xmlns:util="java:java.util.UUID"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:wms="http://www.opengis.net/wms"
                version="2.0"
                exclude-result-prefixes="wms xsl util">

  <!-- ============================================================================= -->

  <xsl:param name="uuid" select="util:toString(util:randomUUID())"/>
  <xsl:param name="lang">eng</xsl:param>
  <xsl:param name="topic"></xsl:param>
  <xsl:param name="url"></xsl:param>
  <xsl:param name="name"></xsl:param>
  <xsl:param name="type"></xsl:param>
  <xsl:param name="desc"></xsl:param>
  <xsl:param name="version"></xsl:param>
  <xsl:param name="props"></xsl:param>
  <xsl:param name="serverops"></xsl:param>
  <xsl:param name="bbox"></xsl:param>
  <xsl:param name="textent"></xsl:param>

  <!-- ============================================================================= -->

  <xsl:include href="resp-party.xsl"/>
  <xsl:include href="ref-system.xsl"/>
  <xsl:include href="identification.xsl"/>

  <!-- ============================================================================= -->

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

  <!-- ============================================================================= -->

  <xsl:template match="*">

    <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

    <gmd:MD_Metadata>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <gmd:fileIdentifier>
        <gco:CharacterString>
          <xsl:value-of select="$uuid"/>
        </gco:CharacterString>
      </gmd:fileIdentifier>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <gmd:language>
        <gco:CharacterString>
          <xsl:value-of select="$lang"/>
        </gco:CharacterString>
        <!-- English is default -->
      </gmd:language>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <gmd:characterSet>
        <gmd:MD_CharacterSetCode codeList="./resources/codeList.xml#MD_CharacterSetCode"
                                 codeListValue="utf8"/>
      </gmd:characterSet>

      <gmd:hierarchyLevel>
        <gmd:MD_ScopeCode codeList="./resources/codeList.xml#MD_ScopeCode" codeListValue="service"/>
      </gmd:hierarchyLevel>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <xsl:for-each select="wms:Service/wms:ContactInformation">
        <gmd:contact>
          <gmd:CI_ResponsibleParty>
            <xsl:apply-templates select="." mode="RespParty"/>
          </gmd:CI_ResponsibleParty>
        </gmd:contact>
      </xsl:for-each>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->
      <xsl:variable name="df">[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]</xsl:variable>
      <gmd:dateStamp>
        <gco:DateTime>
          <xsl:value-of select="format-dateTime(current-dateTime(),$df)"/>
        </gco:DateTime>
      </gmd:dateStamp>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <gmd:metadataStandardName>
        <gco:CharacterString>Australian Marine Community Profile of ISO 19115:2005/19139</gco:CharacterString>
      </gmd:metadataStandardName>

      <gmd:metadataStandardVersion>
        <gco:CharacterString>1.5-experimental</gco:CharacterString>
      </gmd:metadataStandardVersion>

      <!-- TODO - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <xsl:for-each select="refSysInfo">
        <gmd:referenceSystemInfo>
          <gmd:MD_ReferenceSystem>
            <xsl:apply-templates select="." mode="RefSystemTypes"/>
          </gmd:MD_ReferenceSystem>
        </gmd:referenceSystemInfo>
      </xsl:for-each>

      <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

      <gmd:identificationInfo>
        <gmd:MD_DataIdentification>
          <xsl:apply-templates select="." mode="DataIdentification">
            <xsl:with-param name="topic" select="$topic"/>
            <xsl:with-param name="name" select="$name"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="desc" select="$desc"/>
            <xsl:with-param name="props" select="$props"/>
            <xsl:with-param name="version" select="$version"/>
            <xsl:with-param name="serverops" select="$serverops"/>
            <xsl:with-param name="bbox" select="$bbox"/>
            <xsl:with-param name="textent" select="$textent"/>
          </xsl:apply-templates>
        </gmd:MD_DataIdentification>
      </gmd:identificationInfo>

      <!-- FIXME: If distributing opendap or netcdf subset service then
                  this should be filled out with netcdf and version number -->
      <gmd:distributionInfo>
        <gmd:MD_Distribution>
          <gmd:distributionFormat>
            <gmd:MD_Format>
              <gmd:name gco:nilReason="missing">
                <gco:CharacterString/>
              </gmd:name>
              <gmd:version gco:nilReason="missing">
                <gco:CharacterString/>
              </gmd:version>
            </gmd:MD_Format>
          </gmd:distributionFormat>
          <gmd:transferOptions>
            <gmd:MD_DigitalTransferOptions>
            <!-- URL will be a string of urls separated by ^^^ -->
            <xsl:for-each select="tokenize($url,'\^\^\^')"> 
              <gmd:onLine>
                <gmd:CI_OnlineResource>
                  <gmd:linkage>
                    <gmd:URL>
                      <xsl:value-of select="."/>
                    </gmd:URL>
                  </gmd:linkage>
                  <gmd:protocol>
										<xsl:choose>
                    	<xsl:when test="contains($type,'WMS')">
                  			<gco:CharacterString>OGC:WMS</gco:CharacterString>
											</xsl:when>
                    	<xsl:when test="contains($type,'NETCDFSUBSET')">
                  			<gco:CharacterString>"WWW:LINK-1.0-http--netcdfsubset</gco:CharacterString>
											</xsl:when>
                    	<xsl:when test="contains($type,'OPENDAP')">
                  			<gco:CharacterString>"WWW:LINK-1.0-http--opendap</gco:CharacterString>
											</xsl:when>
											<xsl:otherwise>
                  			<gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
											</xsl:otherwise>
										</xsl:choose>
                	</gmd:protocol>
                </gmd:CI_OnlineResource>
              </gmd:onLine>
						</xsl:for-each>
            </gmd:MD_DigitalTransferOptions>
          </gmd:transferOptions>
        </gmd:MD_Distribution>
      </gmd:distributionInfo>

    </gmd:MD_Metadata>
  </xsl:template>

  <!-- ============================================================================= -->

</xsl:stylesheet>
