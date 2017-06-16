<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:mcp="http://schemas.aodn.org.au/mcp-2.0"
    xmlns:mcp-old="http://bluenet3.antcrc.utas.edu.au/mcp"
    xmlns:gmx="http://www.isotc211.org/2005/gmx"
    xmlns:gmd="http://www.isotc211.org/2005/gmd"
    xmlns:gco="http://www.isotc211.org/2005/gco"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    exclude-result-prefixes="mcp-old xsi">

  <xsl:output method="xml" indent="yes"/>

  <!-- S.Pigot, December-2016, Initial Coding -->

  <xsl:variable name="mapping" select="document('../mcp-equipment/equipmentToDataParamsMapping.xml')"/>

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


	<!-- copy everything that isn't part of the mcp namespace -->
	<xsl:template match="@*|node()">
		<xsl:copy copy-namespaces="no">
			<xsl:apply-templates select="@*[name()!='xsi:schemaLocation']|node()"/>
		</xsl:copy>
	</xsl:template>

	<!-- Match the root element so we can force the namespaces we want -->
	<xsl:template match="mcp-old:MD_Metadata" priority="100">
    <xsl:element name="mcp:MD_Metadata" namespace="http://schemas.aodn.org.au/mcp-2.0">
      <xsl:namespace name="gmd" select="'http://www.isotc211.org/2005/gmd'"/>
      <xsl:namespace name="gco" select="'http://www.isotc211.org/2005/gco'"/>
      <xsl:namespace name="gmx" select="'http://www.isotc211.org/2005/gmx'"/>
      <xsl:namespace name="gml" select="'http://www.opengis.net/gml'"/>
      <xsl:namespace name="xlink" select="'http://www.w3.org/1999/xlink'"/>
			<xsl:apply-templates select="@*[name()!='xsi:schemaLocation']|node()"/>
    </xsl:element>  
  </xsl:template> 

	<!-- Set version number to 2.0 -->
	<xsl:template match="gmd:metadataStandardVersion" priority="100">
    <xsl:copy copy-namespaces="no">
    	<xsl:element name="gco:CharacterString" namespace="http://www.isotc211.org/2005/gco">2.0</xsl:element>
		</xsl:copy>
	</xsl:template>

  <!-- catch all @codeList attributes -->
  <xsl:template match="@codeList">
    <xsl:variable name="parent" select="local-name(..)"/>
    <xsl:attribute name="codeList">
      <xsl:value-of select="concat('http://schemas.aodn.org.au/mcp-2.0/schema/resources/Codelist/gmxCodelists.xml#',$parent)"/>
    </xsl:attribute>
  </xsl:template>

  <!-- catch all for mcp-old namespace elements - overrides for specific ones are below -->
	<xsl:template match="*[namespace-uri()='http://bluenet3.antcrc.utas.edu.au/mcp']">
		<xsl:element name="mcp:{local-name()}">
			<xsl:apply-templates select="@*[name()!='xsi:schemaLocation']|node()"/>
		</xsl:element>
	</xsl:template>

  <!-- catch all for mcp-old namespace attributes -->
	<xsl:template match="@*[namespace-uri()='http://bluenet3.antcrc.utas.edu.au/mcp']">
		<xsl:attribute name="mcp:{local-name()}">
			<xsl:copy-of select="."/>
		</xsl:attribute>
	</xsl:template>

  <xsl:template match="mcp-old:MD_DataIdentification" priority="100">
    <xsl:element name="mcp:MD_DataIdentification">
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
      <xsl:apply-templates select="gmd:descriptiveKeywords"/>
      <xsl:apply-templates select="gmd:resourceSpecificUsage"/>
      <xsl:apply-templates select="gmd:resourceConstraints"/>
      <xsl:apply-templates select="gmd:aggregationInfo"/>
      <xsl:apply-templates select="gmd:spatialRepresentationType"/>
      <xsl:apply-templates select="gmd:spatialResolution"/>
      <xsl:apply-templates select="gmd:language"/>
      <xsl:apply-templates select="gmd:characterSet"/>
      <xsl:apply-templates select="gmd:topicCategory"/>
      <xsl:apply-templates select="gmd:environmentDescription"/>
      <xsl:apply-templates select="gmd:extent"/>
      <xsl:apply-templates select="gmd:supplementalInformation"/>
      <xsl:apply-templates select="mcp-old:samplingFrequency"/>
      <!-- These two are very unlikely but would be valid so we include them -->
      <xsl:apply-templates select="mcp-old:sensor"/>
      <xsl:apply-templates select="mcp-old:sensorCalibrationProcess"/>
      <!-- Add data parameters if we have an equipment keyword that matches one in our mapping -->
      <!-- if we have an equipment thesaurus with a match keyword then we process -->

      <xsl:variable name="equipPresent">
         <xsl:for-each select="gmd:descriptiveKeywords/gmd:MD_Keywords[gmd:thesaurusName/gmd:CI_Citation/gmd:identifier/gmd:MD_Identifier/gmd:code/gmx:Anchor=$equipThesaurus]/gmd:keyword/gmx:Anchor">
        <xsl:element name="dp">
          <mcp:dataParameters>
           <mcp:DP_DataParameters>
           <xsl:variable name="currentKeyword" select="text()"/>
           <xsl:comment>Automatically created dp from <xsl:value-of select="$currentKeyword"/></xsl:comment>
           <xsl:for-each select="$mapping/map/equipment">
              <xsl:variable name="tokens" select="tokenize(string(),',')"/>
              <!-- <xsl:message>Checking <xsl:value-of select="$tokens[2]"/></xsl:message> -->
              <xsl:if test="$currentKeyword=$tokens[2]">
                 <xsl:message>KW MATCHED TOKEN: <xsl:value-of select="$tokens[2]"/></xsl:message>
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
      	<xsl:copy-of select="$equipPresent/dp/*"/>
      </xsl:for-each>

			<!-- Finally, copy in the resourceContactInfo -->
      <xsl:apply-templates select="mcp-old:resourceContactInfo"/>
   
    </xsl:element>
  </xsl:template> 

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
						<mcp:vocabularyTermURL>
							<gmd:URL><xsl:value-of select="$tokens[8]"/></gmd:URL>
						</mcp:vocabularyTermURL>
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
						<mcp:vocabularyTermURL>
							<gmd:URL><xsl:value-of select="$tokens[10]"/></gmd:URL>
						</mcp:vocabularyTermURL>
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
						<mcp:vocabularyTermURL>
							<gmd:URL><xsl:value-of select="$tokens[6]"/></gmd:URL>
						</mcp:vocabularyTermURL>
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
						<mcp:vocabularyTermURL>
							<gmd:URL><xsl:value-of select="$tokens[4]"/></gmd:URL>
						</mcp:vocabularyTermURL>
					</mcp:DP_Term>
				</mcp:platform>
      </mcp:DP_DataParameter>
    </mcp:dataParameter>
  </xsl:template>

</xsl:stylesheet>
