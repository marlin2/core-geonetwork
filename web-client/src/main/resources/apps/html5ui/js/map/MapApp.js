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
            generateMaps(options, layers, fixedScales);
        },
        /**
         * Used by other functions that need to create and initialize a map
         * 
         * @param id
         *            of the div for the map
         * @returns
         */
        generateAuxiliaryMap : function(id) {

            var map = new OpenLayers.Map({
                maxExtent : GeoNetwork.map.MAP_OPTIONS.maxExtent.clone(),
                projection : GeoNetwork.map.MAP_OPTIONS.projection,
                resolutions : GeoNetwork.map.MAP_OPTIONS.resolutions,
                restrictedExtent : GeoNetwork.map.MAP_OPTIONS.restrictedExtent
                        .clone()
            });

            Ext.each(GeoNetwork.map.MAP_OPTIONS.controls_, function(control) {
                if (control) {
                    map.addControl(new control());
                }
            });

            Ext.each(GeoNetwork.map.BACKGROUND_LAYERS, function(layer) {
                map.addLayer(layer.clone());
            });

            var scaleLinePanel = new Ext.Panel({
                cls : 'olControlScaleLine overlay-element overlay-scaleline',
                border : false
            });

            scaleLinePanel.on('render', function() {
                var scaleLine = new OpenLayers.Control.ScaleLine({
                    div : scaleLinePanel.body.dom
                });

                map.addControl(scaleLine);
                scaleLine.activate();
            }, this);

            var zoomStore;

            var fixedScales = GeoNetwork.map.MAP_OPTIONS.resolutions;

            if (fixedScales && fixedScales.length > 0) {
                var zooms = [];
                var scales = fixedScales;
                var units = map.baseLayer.units;

                for ( var i = scales.length - 1; i >= 0; i--) {
                    var scale = scales[i];
                    zooms.push({
                        level : i,
                        resolution : OpenLayers.Util.getResolutionFromScale(
                                scale, units),
                        scale : scale
                    });
                }

                zoomStore = new GeoExt.data.ScaleStore({});
                zoomStore.loadData(zooms);

            } else {
                zoomStore = new GeoExt.data.ScaleStore({
                    map : map
                });
            }
            var zoomSelector = new Ext.form.ComboBox(
                    {
                        emptyText : 'Zoom level',
                        tpl : '<tpl for="."><div class="x-combo-list-item">1 : {[parseInt(values.scale)]}</div></tpl>',
                        editable : false,
                        triggerAction : 'all',
                        mode : 'local',
                        store : zoomStore,
                        width : 110
                    });

            zoomSelector.on('click', function(evt) {
                evt.stopEvent();
            });
            zoomSelector.on('mousedown', function(evt) {
                evt.stopEvent();
            });

            zoomSelector.on('select', function(combo, record, index) {
                map.zoomTo(record.data.level);
            }, this);

            var zoomSelectorWrapper = new Ext.Panel({
                items : [ zoomSelector ],
                cls : 'overlay-element overlay-scalechooser',
                border : false
            });

            var mapOverlay = new Ext.Panel({
                // title: "Overlay",
                cls : 'map-overlay',
                items : [ scaleLinePanel, zoomSelectorWrapper ]
            });

            mapOverlay.on("afterlayout", function() {
                scaleLinePanel.body.dom.style.position = 'relative';
                scaleLinePanel.body.dom.style.display = 'inline';

                mapOverlay.getEl().on("click", function(x) {
                    x.stopEvent();
                });
                mapOverlay.getEl().on("mousedown", function(x) {
                    x.stopEvent();
                });
            }, this);

            var panel = new GeoExt.MapPanel({
                height : 400,
                map : map,
                renderTo : id
            });

            return map;

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
