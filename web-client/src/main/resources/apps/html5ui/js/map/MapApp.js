"use strict";

/*
 * Copyright (C) 2014 GeoNetwork
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
Ext.namespace('GeoNetwork');

/**
 * Utility class for maps. Creates and manages maps: in this case, the minimap for preview
 * purposes and adding layers to nationalmap.
 * 
 * Helps App.js
 */
GeoNetwork.mapApp = function() {
    // private vars:

    var generateMaps = function(options, 
																layers, /* NOT USED? - layers come from Settings.js */
																scales) {
        var map;

        if (GeoNetwork.map.CONTEXT) {
            // Load map context
            var request = OpenLayers.Request.GET({
                url: GeoNetwork.map.CONTEXT,
                async: false
            });
            if (request.responseText) {

                var text = request.responseText;
                var format = new OpenLayers.Format.WMC();
                map = format.read(text, {map:options});
            }
        }
        else if (GeoNetwork.map.OWS) {
            // Load map context
            var request = OpenLayers.Request.GET({
                url: GeoNetwork.map.OWS,
                async: false
            });
            if (request.responseText) {
                var parser = new OpenLayers.Format.OWSContext();
                var text = request.responseText;
                map = parser.read(text, {map: options});
            }
        }
        else {
            map = new OpenLayers.Map('ol_map', options);
            fixedScales = scales;

            Ext.each(GeoNetwork.map.BACKGROUND_LAYERS, function(layer) {
                map.addLayer(layer.clone());
            });
        }

        map.events.register("click", map, function(e) {
            app.showBigMap();
        });

        var showBigMapButton = new OpenLayers.Control.Button({
            trigger : showBigMap,
            title : OpenLayers.i18n('bigMap')
        });

        OpenLayers.Util.extend(showBigMapButton, {
            displayClass : 'showBigMap'
        });

        var panel = new OpenLayers.Control.Panel();

        OpenLayers.Util.extend(panel, {
            displayClass : 'showBigMapPanel'
        });

        panel.addControls([ showBigMapButton ]);
        map.addControl(panel);

        new GeoExt.MapPanel({
            id : 'minimap',
            renderTo : 'mini-map',
            height : 200,
            width : 200,
            map : map,
            title : OpenLayers.i18n('Preview'),
            stateId : 'minimap',
            prettyStateKeys : true
        });
        Ext.get("mini-map").setVisibilityMode(Ext.Element.DISPLAY);

        app.mapApp.maps.push(map);

        addMapControls();
    };

    /**
     * Configure the map controls
     * 
     */
    var addMapControls = function() {
        Ext.each(app.mapApp.maps, function(map) {
            map.addControl(new GeoNetwork.Control.ZoomWheel());
            map.addControl(new OpenLayers.Control.LoadingPanel());
        });
    };

		/**
		 * Check when nationalmap becomes available (groups and subgroups)
		 */
		function whenNMAvailable(callback) {
			var interval = 100; // ms
			setTimeout(function() {
				if ((window.nmObjects === undefined)
				 || (window.nmObjects.nmApplicationViewModel === undefined)
				 || (window.nmObjects.nmApplicationViewModel.catalog === undefined)
				 || (window.nmObjects.nmApplicationViewModel.catalog.group === undefined)
				 || (window.nmObjects.nmApplicationViewModel.catalog.group.isLoading === true)) {
					//console.log("Not available");
					setTimeout(function() {
						whenNMAvailable(callback)
					}, interval);
				} else {
					var topLevelGroup = window.nmObjects.nmApplicationViewModel.catalog.group;
					var national = topLevelGroup.findFirstItemByName('GeoNetwork Data Sets');
					if (national === undefined) {
						//console.log("Not available");
						setTimeout(function() {
							whenNMAvailable(callback)
						}, interval);
					} else {
						var gnGroup = national.findFirstItemByName('WMS Layers');
						if (gnGroup === undefined) {
							//console.log("Not available");
							setTimeout(function() {
								whenNMAvailable(callback)
							}, interval);
						} else {
							//console.log("Available at last");
							callback();
						}
					}
				}
			}, interval);
		}

    // public space:
    return {
        initialize : false,
        /**
         * Add a list of WMS layers to the map
         * 
         * @param layers
         *            List of layers to load [[name, url, layer, metadata_id],
         *            [name, url, layer, metadata_id], ....]
				 * TODO: refactor to use nationalmap to add other types of layers, including
				 * ESRI stuff etc
         */
        addWMSLayer : function(layerList) {

            if (layerList.length === 0) {
                return;
            }

						// show national map in the map tab
            showBigMap();

						// grab the current national map application view model and get the catalog which
						// is an instance of CatalogViewModel - see main.js in nationalmap for how 
						// nmObjects is created
						whenNMAvailable(function() {

						var nmObjects = window.nmObjects;
						var catalog = nmObjects.nmApplicationViewModel.catalog;
						var topLevelGroup = catalog.group;
						var national = topLevelGroup.findFirstItemByName('GeoNetwork Data Sets');
						var gnGroup = national.findFirstItemByName('WMS Layers');

						// Now add each layer to the group as a webMapServiceItemViewModel (see nationalmap)
						for (var i = 0; i < layerList.length;i++) {
							var newItem = new nmObjects.viewModels.webMapServiceItemViewModel(catalog.application);
							newItem.name = layerList[i][0];
							newItem.description = layerList[i][3];
							newItem.url = layerList[i][1];
							newItem.layers = layerList[i][2];
							newItem.isEnabled = true;
							newItem.isShown = true;
							gnGroup.add(newItem);
						}

						});
				},

        /**
         * Add a list of WMTS layers to the map
         * 
         * @param layers
         *            List of layers to load [[name, url, layer, metadata_id],
         *            [name, url, layer, metadata_id], ....]
         */
        addWMTSLayer : function(layerList) {
            if (layerList.length === 0) {
                return;
            }

            showBigMap();

        },
        getViewport : function() {
					// TODO: Get info from nationalmap
        },
        maps : [],
        init : function(options, layers, fixedScales) {
            // generateMaps(options, layers, fixedScales);
        },
        /**
         * Used by other functions that need to create and initialize a map
         * 
         * @param id
         *            of the div for the map
         * @returns
         */
        generateAuxiliaryMap : function(id) {
				  // do nothing as we don't have an auxillary map with nationalmap and it slows search
        },
        /**
         * 
         * Given a URL, return the list of associated layers (WMS)
         * 
         * @param url
         * @returns WMS Capabilities
         */
        getCapabilitiesWMS : function(url) {
            var layers = [];

            if (!(/\?$/.test(url))) {
                if (/\?$/.test(url)) {
                    url = url + "&";
                } else {
                    url = url + "?";
                }
            }
            OpenLayers.Request.GET({
                async : false,
                url : url + "request=GetCapabilities&service=WMS",
                success : function(response) {
                    var format = new OpenLayers.Format.XML();
                    var xml = format.read(response.responseText);
                    var text = format.write(xml);
                    var CAPformat = new OpenLayers.Format.WMSCapabilities();
                    var cap = CAPformat.read(xml);
                    if (cap.capability) {
                        Ext.each(cap.capability.layers, function(layer) {
                            if (layer.queryable) {
                                layers.push(layer.name);
                            }
                        });
                    }
                }
            });
            return layers;
        },
        /**
         * 
         * Given a URL, return the list of associated layers (WMS)
         * 
         * @param url
         * @returns WFS Capabilities
         */
        getCapabilitiesWFS : function(url) {
            var layers = [];

            if (!(/\?$/.test(url))) {
                if (/\?$/.test(url)) {
                    url = url + "&";
                } else {
                    url = url + "?";
                }
            }

            var url2 = "version=1.1.0&request=DescribeFeatureType&service=WFS";
            var describeFormat = new OpenLayers.Format.WFSDescribeFeatureType();

            OpenLayers.Request.GET({
                async : false,
                url : url + url2,
                success : function(response) {
                    var format = new OpenLayers.Format.XML();
                    var xml = format.read(response.responseText);
                    var text = format.write(xml);
                    var describe = describeFormat.read(xml);

                    Ext.each(describe.featureTypes, function(ftype) {
                        Ext.each(ftype.properties, function(feat) {
                            var layer = new OpenLayers.Protocol.WFS({
                                url : url,
                                featurePrefix : describe.targetPrefix,
                                featureType : feat.type,
                                featureNS : describe.targetNamespace,
                                geometryName : feat.name
                            });

                            layers.push(layer);
                        });
                    });
                }

            });
            return layers;
        }

    };
}; // end of app
