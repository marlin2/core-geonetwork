/*
 * Copyright (C) 2012 GeoNetwork
 *
 * This file is part of GeoNetwork
 *
 * GeoNetwork is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GeoNetwork is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GeoNetwork.  If not, see <http://www.gnu.org/licenses/>.
 */

var loadedNMJS = false;

// The following functions are for the advanced search hiding and showing
function hide(id) {
    if (Ext.get(id)) {
        Ext.get(id).setVisibilityMode(Ext.Element.DISPLAY);
        Ext.get(id).hide();
    }
}
function show(id) {
    if (Ext.get(id)) {
        Ext.get(id).setVisibilityMode(Ext.Element.DISPLAY);
        Ext.get(id).show();
    }
}

function toggleLogin() {
    toggle('login-form');
    Ext.get('username').focus();
}

function toggle(id) {
    if (Ext.get(id)) {
        if (Ext.get(id).isDisplayed()) {
            hide(id);
        } else {
            show(id);
        }
    }
}

function showBrowse() {
    // Reset search for tag cloud and render the different sections of the page
    //catalogue.kvpSearch("fast=index&from=1&to=5&sortBy=changeDate", null, null, null, true);
		app.rebuildBrowse();

    show("main");
    hide("search-form");

    hideAbout();
    hideSearch();
    hideBigMap();
    hideMetadata();

    show("browser");
    //show("latest-metadata");
    //show("popular-metadata");

    //app.breadcrumb.setPrevious([]);
    //app.breadcrumb.setCurrent(app.breadcrumb.defaultSteps[0]);

    Ext.each(Ext.query('a', Ext.get("main-navigation").dom), function(a) {
        Ext.get(a).removeClass("selected");
    });

    Ext.get("browse-tab").addClass("selected");
}

function hideBrowse() {
    hide("browser");
    //hide("latest-metadata");
    //hide("popular-metadata");
}

function showAbout() {

    show("about");
    hide("search-form");

    hideBrowse();
    hideSearch();
    hideBigMap();
    hideMetadata();

    //app.breadcrumb.setCurrent(app.breadcrumb.defaultSteps[2]);

    Ext.each(Ext.query('a', Ext.get("main-navigation").dom), function(a) {
        Ext.get(a).removeClass("selected");
    });

    Ext.get("about-tab").addClass("selected");
}

function hideAbout() {
    hide("about");
}

					function doLoadSS(stylesheetUrl) {
						var l = document.createElement('link'); l.rel = 'stylesheet';
						l.href = stylesheetUrl;
						var h = document.getElementsByTagName('head')[0]; h.parentNode.insertBefore(l, h);
					};
					function doLoadJS(jsUrl){
					  var s = document.createElement('script');
						s.type = 'text/javascript';
						s.async = true;
						s.src = jsUrl;
						var x = document.getElementsByTagName('script')[0];
						x.parentNode.insertBefore(s, x);
					}

function showBigMap() {
    hideBrowse();
    hideSearch();
    hideAbout();
    hideMetadata();
    hide("search-form");

    // show map
    show("big-map-container");

    Ext.each(Ext.query('a', Ext.get("main-navigation").dom), function(a) {
        Ext.get(a).removeClass("selected");
    });

    Ext.get("map-tab").addClass("selected");

		if (!loadedNMJS) {
					// lazy load the css and js required for nationalmap once page load is finished, taken from
					// http://www.giftofspeed.com/defer-loading-css/ and the ga stuff above
					doLoadSS("../../nationalmap/public/third_party/leaflet/leaflet.css");
					doLoadSS("../../nationalmap/public/build/Cesium/Widgets/cesiumwidgetsbundle.css");
					doLoadSS("../../nationalmap/public/css/AusGlobeViewer.css");
					doLoadJS("../../static/nationalmap.js");
					loadedNMJS = true;
	 }
}

function hideBigMap() {
    hide("big-map-container");
}

function showSearch() {
    hideBrowse();
    hideAbout();
    hideMetadata();
    hideBigMap();
    show("search-form");

    show("secondary-aside");
    Ext.getCmp('resultsPanel').show();
    Ext.get('resultsPanel').show();
    show("main-aside");
		show("bread-crumb-div");
		show("bread-crumb-app");

    app.breadcrumb.setDefaultPrevious(1);
    app.breadcrumb.setCurrent(app.breadcrumb.defaultSteps[1]);

    if (!app.searchApp.firstSearch) {
        app.searchApp.firstSearch = true;

        Ext.getCmp('advanced-search-options-content-form').fireEvent('search');
    }


    Ext.each(Ext.query('a', Ext.get("main-navigation").dom), function(a) {
        Ext.get(a).removeClass("selected");
    });

    Ext.get("catalog-tab").addClass("selected");
    
}

function hideSearch() {
    // Comment the following if you want a push-down and not a hiding
    // for the results
    hide("secondary-aside");
    hide("resultsPanel");
    hide("main-aside");
		hide("bread-crumb-div");
		hide("bread-crumb-app");
}

function showMetadata() {

    hide("search-form");
    hideBrowse();
    hideAbout();
    hideSearch();
    hideBigMap();

    show("metadata-container");
		show("bread-crumb-app");

    app.breadcrumb.setDefaultPrevious(2);

    Ext.each(Ext.query('a', Ext.get("main-navigation").dom), function(a) {
        Ext.get(a).removeClass("selected");
    });

    Ext.get("catalog-tab").addClass("selected");
}

function hideMetadata() {
    hide("metadata-container");
    hide("share-capabilities");
		hide("bread-crumb-app");

    // Destroy potential existing panel
    Ext.getCmp('metadata-panel') && Ext.getCmp('metadata-panel').destroy();
    Ext.getCmp('editorPanel') && Ext.getCmp('editorPanel').destroy();
}

function resizeResultsPanel() {
    var resultsPanel = Ext.get("resultsPanel");
    Ext.each(resultsPanel.dom.children, function(div) {
        div = Ext.get(div);
        Ext.each(div.dom.children, function(child) {
            child = Ext.get(child);
            child.setWidth("100%");
            Ext.each(child.dom.children, function(gchild) {
                Ext.get(gchild).setWidth("100%");
            });
        });
    });
}

function showAdvancedSearch() {
    hide('show-advanced');
    show('legend-search');
    show('hide-advanced');
    Ext.get("search-form-fieldset").dom.style.border = "1px solid #fff";
    show('advanced-search-options');
    if (Ext.getCmp('advanced-search-options-content-form')) {
        Ext.getCmp('advanced-search-options-content-form').doLayout();

        // For reset and submit buttons:

        t = Ext.getCmp('advanced-search-options-content-form').toolbars[0];
        document.getElementById(t.el.parent().id).style.width = "";
    }
    if (cookie && cookie.get('user')) {
        cookie.get('user').searchTemplate = 'FULL';
    } else if (cookie) {
        cookie.set('user', {});
        cookie.get('user').searchTemplate = 'FULL';
    }
    if (catalogue && catalogue.resultsView && catalogue.resultsView.autoSelectTemplate) {
        catalogue.resultsView.autoSelectTemplate();
    }
}

function resetAdvancedSearch(updateSearch) {

    if (Ext.getCmp('advanced-search-options-content-form')) {

        GeoNetwork.state.History.suspendEvents();

        Ext.getCmp('advanced-search-options-content-form').getForm().reset();

        var value = Ext.getCmp("fullTextField").getValue();

        if (value.length > 0)
            Ext.getCmp('E_trueany').setValue(value + "*");
        else
            Ext.getCmp('E_trueany').setValue(value);

        Ext.getCmp('E_dynamic').suspendEvents(false);
        Ext.getCmp('E_dynamic').checked = (Ext.getCmp("o_dynamic").getValue());
        Ext.getCmp('E_dynamic').resumeEvents();

        Ext.getCmp('E_download').suspendEvents(false);
        Ext.getCmp('E_download').checked = (Ext.getCmp("o_download").getValue());
        Ext.getCmp('E_download').resumeEvents();

        Ext.getCmp('mymetadata').suspendEvents(false);
        Ext.getCmp('mymetadata').checked = false;
				Ext.getCmp('mymetadata').resumeEvents();

				Ext.getCmp('sortByToolBar').setValue("relevance");

        GeoNetwork.state.History.resumeEvents();

    }

    if (Ext.getCmp('facets-panel')) {
        Ext.getCmp('facets-panel').reset();
    }
}

function hideAdvancedSearch(updateSearch) {

    hide('advanced-search-options');
    hide('legend-search');
    hide('hide-advanced');
		if (Ext.get("search-form-fieldset")) Ext.get("search-form-fieldset").dom.style.border = "none";
    show('show-advanced');
    if (updateSearch) {
        if (cookie && cookie.get('user')) {
            cookie.get('user').searchTemplate = 'THUMBNAIL';
        } else if (cookie) {
            cookie.set('user', {});
            cookie.get('user').searchTemplate = 'THUMBNAIL';
        }
        if (catalogue && catalogue.resultsView 
                && catalogue.resultsView.autoSelectTemplate) {
            catalogue.resultsView.autoSelectTemplate();
        }
    }
}

/**
 * This function only works in IE. In IE8, the user has to confirm a security
 * alert. Other browsers not tested.
 */
function copyToClipboard(text) {
    var textfield = Ext.getCmp("copy-clipboard-ie_");

    if (!textfield) {
        textfield = new Ext.form.TextField({
            id : "copy-clipboard-ie_",
            renderTo : "copy-clipboard-ie"
        });
    }
    show(textfield);
    textfield.setValue(text);
    textfield.focus();
    textfield.selectText();

    if (window.clipboardData && clipboardData.setData) {
        window.clipboardData.setData('Text', textfield.getValue());
    } else {
        CopiedTxt = document.selection.createRange();
        CopiedTxt.execCommand("Copy");
    }
    // clearSelection();
    hide(textfield);
}
/**
 * @param uuid
 * @return {String}
 */
function metadataViewURL(uuid) {
    return window.location.href.match(/(http.*\/.*)\/srv\.*/, '')[1] + '#!'
            + uuid;
}

// Validate a WMS or WFS against the Geonovum service
function validateWMSWFS(capsURL, el, type) {

    el = Ext.get(el);

    if (el)
        el = el.parent().parent().parent().parent().parent();

    if (!el)
        el = Ext.getBody();

    // Load mask to prevent more than one click:
    if (el) {
        var mask = new Ext.LoadMask(el, {
            msg : OpenLayers.i18n('disclaimer.loading')
        });
        mask.show();
    }

    // do NOT use the REQUEST=GetCapabilities etc, because the validator
    // seesm to trip over this
    capsURL = capsURL.replace(/REQUEST=GetCapabilities/i, "");
    // capsURL = capsURL.replace(/SERVICE=WMS/i, "");
    capsURL = capsURL.replace(/VERSION=1.1.1/i, "");
    capsURL = capsURL.replace(/&&/gi, "&"); // replace any double &
    capsURL = capsURL.replace(/\?&/gi, "?"); // replace any combi of ?&

    // now use that url for validation and presentation of the results
    var params = {
        type : type,
        wmsurl : capsURL
    };
    var totalUrl = 'validators.wms';

    Ext.Ajax.request({
        method : 'GET',
        url : totalUrl,
        params : params,
        success : function(response) {
            mask.hide();

            Ext.MessageBox.show({
                title : OpenLayers.i18n('validityInfo'),
                msg : response.responseText,
                buttons : Ext.MessageBox.OK,
                icon : Ext.MessageBox.INFO,
                minWidth : 600,
                maxWidth : 800
            });

        },
        failure : function(response) {
            mask.hide();

            Ext.MessageBox.show({
                title : OpenLayers.i18n('validityInfo'),
                msg : OpenLayers.i18n('wxs-extract-service-not-found', {
                    url : "validation",
                    misc : response.responseText
                }),
                buttons : Ext.MessageBox.OK,
                icon : Ext.MessageBox.ERROR,
                minWidth : 600,
                maxWidth : 800
            });
        }
    });
}

