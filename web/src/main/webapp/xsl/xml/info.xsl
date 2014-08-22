<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
	<!-- ============================================================================================= -->
	<!-- === This stylesheet is used by the xml.info service -->
	<!-- ============================================================================================= -->

	<xsl:template match="/">
		<info>
			<xsl:apply-templates select="*"/>
		</info>
	</xsl:template>

	<xsl:template match="config">
		<xsl:for-each select="root/*[name()!='env']">
			<xsl:apply-templates mode="config" select="."/>
		</xsl:for-each>
		<xsl:copy-of select="root/env"/>
	</xsl:template>

	<xsl:template mode="config" match="system">
		<system.site.name><xsl:value-of select="system/children/site/children/name/value"/></system.site.name>
		<system.site.organization><xsl:value-of select="system/children/site/children/organization/value"/></system.site.organization>
		<system.site.siteId><xsl:value-of select="system/children/site/children/siteId/value"/></system.site.siteId>
		<system.platform.version><xsl:value-of select="system/children/platform/children/version/value"/></system.platform.version>
		<system.platform.subVersion><xsl:value-of select="system/children/platform/children/subVersion/value"/></system.platform.subVersion>
		<system.server.host><xsl:value-of select="system/children/server/children/host/value"/></system.server.host>
		<system.server.protocol><xsl:value-of select="system/children/server/children/protocol/value"/></system.server.protocol>
		<system.server.port><xsl:value-of select="system/children/server/children/port/value"/></system.server.port>
		<system.userSelfRegistration.enable><xsl:value-of select="system/children/userSelfRegistration/children/enable/value"/></system.userSelfRegistration.enable>
		<system.xlinkResolver.enable><xsl:value-of select="system/children/xlinkResolver/children/enable/value"/></system.xlinkResolver.enable>
		<system.searchStats.enable><xsl:value-of select="system/children/searchStats/children/enable/value"/></system.searchStats.enable>
		<system.inspire.enableSearchPanel><xsl:value-of select="system/children/inspire/children/enableSearchPanel/value"/></system.inspire.enableSearchPanel>
		<system.harvester.enableEditing><xsl:value-of select="system/children/harvester/children/enableEditing/value"/></system.harvester.enableEditing>
		<system.metadata.defaultView><xsl:value-of select="system/children/metadata/children/defaultView/value"/></system.metadata.defaultView>
		<system.metadataprivs.usergrouponly><xsl:value-of select="system/children/metadataprivs/children/usergrouponly/value"/></system.metadataprivs.usergrouponly>
		<map.config>{"useOSM":true,"context":"","layer":{"url":"http://www2.demis.nl/mapserver/wms.asp?","layers":"Countries","version":"1.1.1"},"projection":"EPSG:3857","projectionList":[{"code":"EPSG:4326","label":"WGS84 (EPSG:4326)"},{"code":"EPSG:3857","label":"Google mercator (EPSG:3857)"}]}</map.config>
		<map.proj4js>[{"code":"EPSG:2154","value":"+proj=lcc +lat_1=49 +lat_2=44 +lat_0=46.5 +lon_0=3 +x_0=700000 +y_0=6600000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"}]</map.proj4js>
		<metadata.editor.schemaConfig>{
			"iso19110":
			{
				"defaultTab":"default",
				"displayToolTip":false,
				"related":
				{	
						"display":true,
						"readonly":true,
						"categories":["dataset"]
				},
				"validation": 
				{
					"display":true
				}
			},
			"iso19139":{
				"defaultTab":"default",
				"displayToolTip":false,
				"related":
				{
					"display":true,
					"categories":[]
				},
				"suggestion":
				{
					"display":true
				},
				"validation":
				{
					"display":true
				}
			},
			"dublin-core":
			{
				"defaultTab":"default",
				"related":
				{
					"display":true,
					"readonly":false,
					"categories":["parent","onlinesrc"]
				}
			},
			"iso19139.mcp":
			{
				"defaultTab":"default",
				"displayToolTip":false,
				"related":
				{
					"display":true,
					"categories":[]
				},
				"suggestion":
				{
					"display":false
				},
				"validation": 
				{
					"display":true
				}
			},
			"iso19139.mcp-1.4":
			{
				"defaultTab":"default",
				"displayToolTip":false,
				"related":
				{
					"display":true,
					"categories":[]
				},
				"suggestion":
				{
					"display":false
				},
				"validation": 
				{
					"display":true
				}
			},
			"iso19139.anzlic":
			{
				"defaultTab":"default",
				"displayToolTip":false,
				"related":
				{
					"display":true,
					"categories":[]
				},
				"suggestion":
				{
					"display":false
				},
				"validation":
				{
					"display":true
				}
			}
			}</metadata.editor.schemaConfig>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template mode="allprocessor" match="*">
		<xsl:variable name="qname" select="name()"/>
		<xsl:element name="{$qname}">
			<xsl:choose>
				<xsl:when test="children">
					<xsl:apply-templates mode="allprocessor" select="children/*"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="value"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>	
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="all">
		<system>
			<xsl:for-each select="root/system/system/children/*">
				<xsl:apply-templates mode="allprocessor" select="."/>
			</xsl:for-each>
		</system>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="settings">
		<xsl:choose>
			<xsl:when test="setting[@name='system/site/name']">
				<site>
					<name><xsl:value-of select="setting[@name='system/site/name']/@value"/></name>
					<organization><xsl:value-of select="setting[@name='system/site/organization']/@value"/></organization>
					<siteId><xsl:value-of select="setting[@name='system/site/siteId']/@value"/></siteId>
					<platform>
						<name>geonetwork</name>
						<version><xsl:value-of select="setting[@name='system/platform/version']/@value"/></version>
						<subVersion><xsl:value-of select="setting[@name='system/platform/subVersion']/@value"/></subVersion>
					</platform>
				</site>
			</xsl:when>
			<!-- Only INSPIRE -->
			<xsl:when test="not(setting[@name='system/site/name']) and setting[@name='system/inspire/enable']">
				<inspire>
					<enable><xsl:value-of select="setting[@name='system/inspire/enable']/@value"/></enable>
					<enableSearchPanel><xsl:value-of select="setting[@name='system/inspire/enableSearchPanel']/@value"/></enableSearchPanel>
				</inspire>
			</xsl:when>
			<xsl:when test="not(setting[@name='system/site/name']) and setting[@name='system/harvester/enableEditing']">
				<harvester>
					<enable><xsl:value-of select="setting[@name='system/harvester/enableEditing']/@value"/></enable>
				</harvester>
			</xsl:when>
		    <xsl:when test="not(setting[@name='system/site/name']) and setting[@name='system/metadataprivs/usergrouponly']">
		        <metadataprivs>
		            <userGroupOnly><xsl:value-of select="setting[@name='system/metadataprivs/usergrouponly']/@value"/></userGroupOnly>
		        </metadataprivs>
		    </xsl:when>
			<xsl:otherwise>
				
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- ============================================================================================= -->

    <xsl:template match="isolanguages">
        <xsl:copy>
            <xsl:for-each select="record">
                <xsl:sort select="name" order="ascending"/>
                <isolanguage id="{id}">
                    <xsl:copy-of select="code"/>
                    <xsl:copy-of select="label"/>
                </isolanguage>
            </xsl:for-each>
        </xsl:copy>
    </xsl:template>
	
	<!-- ============================================================================================= -->

	<xsl:template match="categories">
		<xsl:copy>
			<xsl:for-each select="record">
				<xsl:sort select="name" order="ascending"/>
				<category id="{id}">
					<xsl:copy-of select="name"/>
					<xsl:copy-of select="label"/>
				</category>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="z3950repositories">
		<xsl:copy>
			<xsl:for-each select="record">
				<xsl:sort select="name" order="ascending"/>
				<repository id="{id}">
					<xsl:copy-of select="id"/>
					<label>
						<xsl:value-of select="name"/>
					</label>
				</repository>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="groups">
		<xsl:copy>
			<xsl:for-each select="record">
				<xsl:sort select="name" order="ascending"/>
				<group id="{id}">
					<xsl:copy-of select="name"/>
					<xsl:copy-of select="description"/>
					<xsl:copy-of select="email"/>
					<xsl:copy-of select="referrer"/>
					<xsl:copy-of select="label"/>
				</group>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="operations">
		<xsl:copy>
			<xsl:for-each select="record">
				<operation id="{id}">
					<xsl:copy-of select="name"/>
					<xsl:copy-of select="reserved"/>
					<xsl:copy-of select="label"/>
				</operation>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="regions">
		<xsl:copy-of select="."/>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="sources">
		<xsl:copy-of select="."/>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="schemas">
		<xsl:copy-of select="."/>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="statusvalues">
		<xsl:copy>
			<xsl:for-each select="record">
				<xsl:sort select="name" order="ascending"/>
				<status id="{id}">
					<xsl:copy-of select="name"/>
					<xsl:copy-of select="reserved"/>
					<xsl:copy-of select="label"/>
				</status>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="templates">
		<xsl:copy>
			<xsl:for-each select="record">
				<xsl:sort select="name" order="ascending"/>
				<template id="{id}">
					<xsl:copy-of select="id"/>
					<title>
						<xsl:value-of select="name"/>
					</title>
					<schema>
						<xsl:value-of select="id/@code"/>
					</schema>
				</template>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="users">
		<xsl:copy-of select="."/>
	</xsl:template>

	<!-- ============================================================================================= -->

	<xsl:template match="me|env">
		<xsl:copy-of select="."/>
	</xsl:template>
	<!-- ============================================================================================= -->

	<xsl:template match="auth">
		<xsl:copy-of select="."/>
	</xsl:template>

    <!-- ============================================================================================= -->

    <xsl:template match="readonly|index">
        <xsl:copy-of select="."/>
    </xsl:template>

	<!-- ============================================================================================= -->

</xsl:stylesheet>
