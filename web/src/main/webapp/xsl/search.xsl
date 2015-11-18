<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:geonet="http://www.fao.org/geonetwork"
	xmlns:exslt="http://exslt.org/common" exclude-result-prefixes="exslt geonet">
	<xsl:include href="metadata/common.xsl" />
	<xsl:output omit-xml-declaration="no" method="html"
		doctype-public="html" indent="yes" encoding="UTF-8" />
	<xsl:variable name="hostUrl" select="concat(/root/gui/env/server/protocol, '://', /root/gui/env/server/host, ':', /root/gui/env/server/port)"/>
	<xsl:variable name="baseUrl" select="/root/gui/url" />
	<xsl:variable name="serviceUrl" select="concat($hostUrl, /root/gui/locService)" />
	<xsl:variable name="rssUrl" select="concat($serviceUrl, '/rss.search?sortBy=changeDate')" />
	<xsl:variable name="siteName" select="/root/gui/env/site/name"/>
	
	<!-- main page -->
	<xsl:template match="/">
    <html class="no-js">
           <xsl:attribute name="lang">
                <xsl:value-of select="/root/gui/language" />
            </xsl:attribute>
    
			<head>
				<meta http-equiv="Content-type" content="text/html;charset=UTF-8"></meta>
				<meta http-equiv="X-UA-Compatible" content="IE=9,chrome=1"></meta>
				<title><xsl:value-of select="$siteName" /></title>
				<meta name="description" content="" ></meta>
                <meta name="viewport" content="width=device-width"></meta>
				<meta name="og:title" content="{$siteName}"/>
				
				<link rel="icon" type="image/gif" href="../../images/logos/favicon.gif" />
				<link rel="alternate" type="application/rss+xml" title="{$siteName} - RSS" href="{$rssUrl}"/>
				<link rel="search" href="{$serviceUrl}/portal.opensearch" type="application/opensearchdescription+xml" title="{$siteName}"/>

    		<link rel="stylesheet" href="{concat($baseUrl, '/static/geonetwork-client_css.css')}"></link> 

				<script type="text/javascript">
					var _gaq = _gaq || [];
					_gaq.push(['_setAccount', 'UA-36263643-1']);
					_gaq.push(['_trackPageview']);

					(function() {
					var ga = document.createElement('script');
					ga.type = 'text/javascript';
					ga.async = true;
					ga.src = ('https:' == document.location.protocol ? 'https://ssl' :
					'http://www') + '.google-analytics.com/ga.js';
					var s = document.getElementsByTagName('script')[0];
					s.parentNode.insertBefore(ga, s);
					})();

				</script>

                 <xsl:choose>
                     <xsl:when test="/root/gui/config/map/osm_map = 'true'">
                         <script>
                             var useOSMLayers = true;
                         </script>
                     </xsl:when>

                     <xsl:otherwise>
                         <script>
                             var useOSMLayers = false;
                         </script>
                     </xsl:otherwise>
                 </xsl:choose>

			</head>
			<body>

			<div class="grey">
					<a href="javascript:window.print();" id="printer-button"><i class="fa fa-print"></i><xsl:value-of select="/root/gui/strings/print-button"/></a>
					<a id="rss-button" href="/geonetwork/srv/eng/rss.latest"><i class="fa fa-rss-square"></i><xsl:value-of select="/root/gui/strings/rss-button"/></a>
					<span id="login-stuff">
						<a id="user-button">
							<xsl:choose>
								<xsl:when test="string(/root/gui/session/userId)=''">
							  	<xsl:attribute name="href">javascript:toggleLogin();</xsl:attribute>
							 	</xsl:when>
								<xsl:otherwise>
							  	<xsl:attribute name="href">javascript:app.loginApp.logout();</xsl:attribute>
							 	</xsl:otherwise>
						  </xsl:choose>
							<i class="fa fa-user"></i>
							<span id="user-button_label">
								<xsl:choose>
									<xsl:when test="string(/root/gui/session/userId)=''">
										<xsl:value-of select="/root/gui/strings/signIn"/>
							 		</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="/root/gui/strings/signOut"/>
							 		</xsl:otherwise>
						  	</xsl:choose>
							</span>
						</a>
						<label id="username_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat(/root/gui/session/username,' ')"/>
							</xsl:if>
						</label>
						<label id="name_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat(/root/gui/session/name,' ',/root/gui/session/surname,' ')"/>
							</xsl:if>
						</label>
						<label id="profile_label">
							<xsl:if test="string(/root/gui/session/userId)!=''">
								<xsl:value-of select="concat('(',/root/gui/session/profile,')')"/>	
							</xsl:if>
						</label>
						<a href="javascript:catalogue.admin();" id="administration-button">
							<xsl:if test="string(/root/gui/session/userId)=''">
								<xsl:attribute name="style">display:none;</xsl:attribute>
							</xsl:if>
							<i class="fa fa-wrench"></i>
							<xsl:value-of select="/root/gui/strings/admin"/>
						</a>
						<script>function false_(){ return false; }</script>
						<form id="login-form" style="display: none;" onsubmit="return false_();">
							<div id="login_div">
								<label>User name:</label>
								<input type="text" id="username" name="username"/><br/>
								<label>Password: </label>
								<input type="password" id="password" name="password"/><br/>
								<input type="submit" id="login_button" value="Login"/>
					  	</div>
						</form>
				  </span>
					<!-- from here on, all elements are floated to the right so 
					     they are in reverse order -->
					<a id="help-button" target="_blank" href="/geonetwork/docs/eng/users">
						<i class="fa fa-question-circle"></i><xsl:value-of select="/root/gui/strings/help"/>
					</a>
					<a id="lang-button" href="javascript:toggle('lang-form');">
            <xsl:for-each select="/root/gui/config/languages/*">
              <xsl:variable name="lang" select="name(.)"/>
              <xsl:if test="/root/gui/language=$lang">
                <span id="current-lang"><xsl:value-of select="/root/gui/strings/*[name(.)=$lang]"/></span>&#160;<i class="fa fa-angle-double-down"></i>
              </xsl:if>
            </xsl:for-each>
						<div id="lang-form" style="display:none;"></div>
          </a>
			</div>
				
      <div id="page-container">  
				<div id="container">
					<div id="header">
					  <div id="logo"></div>
						<header class="wrapper clearfix">
							<div style="width: 100%; margin: 0 auto;">
								<nav id="nav">
									<ul id="main-navigation">
										<li>
											<a id="catalog-tab" class="selected" href="javascript:showSearch();">
												<xsl:value-of select="/root/gui/strings/porCatInfoTab" />
											</a>
										</li>
										<li>
											<a id="map-tab" href="javascript:showBigMap();">
												<xsl:value-of select="/root/gui/strings/map_label" />
											</a>
										</li>
										<li>
											<a id="browse-tab" href="javascript:showBrowse();">
												<xsl:value-of select="'Browse'" />
											</a>
										</li>
										<li>
											<a id="about-tab" href="javascript:showAbout();">
												<xsl:value-of select="/root/gui/strings/about" />
											</a>
										</li>
									</ul>
								</nav>
							</div>
						</header>
					</div>
					
					<div id="main">
			        <div id="copy-clipboard-ie"></div>
                       <div id="share-capabilities" style="display:none">
                            <a id="custom-tweet-button" href="javascript:void(0);" target="_blank">
                                    <xsl:value-of select="/root/gui/strings/tweet" />
                            </a>
                            <div id="fb-button">
                           </div>
                       </div>
                       <div id="permalink-div" style="display:none"></div>
                        <div id="bread-crumb-app"></div>
                        <div id="search-form" style="display:none">
                            <fieldset id="search-form-fieldset">
                                <legend id="legend-search">
                                    <xsl:value-of select="/root/gui/strings/search" />
                                </legend>
                                <span id='fullTextField'></span>
                                <input type="button"
                                    onclick="Ext.getCmp('advanced-search-options-content-form').fireEvent('search');"
                                    onmouseover="Ext.get(this).addClass('hover');"
                                    onmouseout="Ext.get(this).removeClass('hover');"
                                    id="search-submit" class="form-submit" value="&#xf002;">
                                </input>
                                <div class="form-dummy">
                                    <span><xsl:value-of select="/root/gui/strings/dummySearch" /></span>
	                                <div id="ck1"/>
	                                <div id="ck2"/>
	                                <div id="ck3"/>
                                </div>
                                
                                <div id="show-advanced" onclick="showAdvancedSearch()">
                                    <span class="button"><xsl:value-of select="/root/gui/strings/advancedOptions.show" />&#160;<i class="fa fa-angle-double-down fa-2x show-advanced-icon"></i></span>
                                </div>
                                <div id="hide-advanced" onclick="hideAdvancedSearch(true)" style="display: none;">
                                    <span class="button"><xsl:value-of select="/root/gui/strings/advancedOptions.hide" />&#160;<i class="fa fa-angle-double-up fa-2x hide-advanced-icon"></i></span>
                                </div>
                                <div id="advanced-search-options" >
                                    <div id="advanced-search-options-content"></div>
                                </div>
                            </fieldset>
                        </div>
					

	                    <div id="browser" style="display:none">
                        <aside class="tag-aside">
                          <div id="tags">
                            <header><h1><span><xsl:value-of select="/root/gui/strings/tag_label" /></span></h1></header>
                            <div id="cloud-tag"></div>
                          </div>
                        </aside>
                        <article>
                          <div>
                            <section>
                              <div id="latest-metadata">
                                <header><h1><span><xsl:value-of select="/root/gui/strings/latestDatasets" /></span></h1></header>
                              </div>
                              <div id="popular-metadata">
                                <header><h1><span><xsl:value-of select="/root/gui/strings/popularDatasets" /></span></h1></header>
                              </div>
                            </section>
                          </div>
                        </article>
                      </div>

	                    <div id="about" style="display:none;">
	                    	<div id="welcome-text">
	                     	  <xsl:copy-of select="/root/gui/strings/welcome.text"/>
											  </div>
	                    	<div id="about-text">
	                      	<xsl:copy-of select="/root/gui/strings/about.text"/>
                        </div>
                      </div>
	                    
						<div id="big-map-container" style="display:none;background: #000;">
							<div id="nationalmapContainer" class="nationalmap-container">
								<div id="cesiumContainer" class="cesium-container"></div>
							</div>
						</div>
                       <div id="metadata-info" style="display:none;"/>
						<div id="search-container" class="main wrapper clearfix">
							<div id="bread-crumb-div"></div>

							<aside id="main-aside" class="main-aside" style="display:none;">
								<header><xsl:value-of select="/root/gui/strings/filter" /></header>
								<div id="facets-panel-div"></div>
							</aside>
							<article>
								<div style="display:none;">
									<aside id="secondary-aside" class="secondary-aside" style="display:none;">
										<header><xsl:value-of select="/root/gui/strings/recentlyViewed" /></header>
                  	<div id="recent-viewed-div"></div>
                  	<div id="mini-map"></div>
									</aside>
								</div>
								<header>
								</header>
								<section>
									<div id="result-panel"></div>
								</section>
								<footer>
								</footer>
							</article>
						</div>
						<!-- .main .wrapper .clearfix -->
					</div>



					<div id="only_for_spiders">
						<xsl:for-each select="/root/*/record">
							<article>
								<xsl:attribute name="id"><xsl:value-of
									select="uuid" /></xsl:attribute>
								<xsl:apply-templates mode="elementEP"
									select="/root/*[name(.)!='gui' and name(.)!='request']">
									<xsl:with-param name="edit" select="false()" />
									<xsl:with-param name="uuid" select="uuid" />
								</xsl:apply-templates>
							</article>
						</xsl:for-each>
					</div>
					<!-- #main -->

					<div id="footer">
            <xsl:if test="/root/gui/config/html5ui-footer!='true'">
              <xsl:attribute name="style">display:none;</xsl:attribute>
            </xsl:if>
						<footer class="wrapper">
							<ul>
								<li style="float:left">
									<xsl:value-of select="/root/gui/strings/poweredBy"/> 
									<a href="http://geonetwork-opensource.org/">GeoNetwork OpenSource</a>
								</li>
								<li>
                                    <a href="http://www.gnu.org/copyleft/gpl.html">GPL</a>
								</li>
							</ul>
						</footer>
					</div>
				</div>

				<input type="hidden" id="x-history-field" />
				<iframe id="x-history-frame" height="0" width="0"></iframe>

				 <xsl:variable name="minimize">
				   <xsl:choose>
						 <xsl:when test="/root/request/debug">?minimize=false</xsl:when>
						 <xsl:otherwise></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>

				<script type="text/javascript" src="{concat($baseUrl, '/static/geonetwork-client-mini-nomap.js', $minimize)}"></script>

    		<script>L_PREFER_CANVAS = true;</script>
        </div>
		</body>
	</html>
	</xsl:template>
</xsl:stylesheet>
