//=============================================================================
//===	Copyright (C) 2001-2005 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: GeoNetwork@fao.org
//==============================================================================

package org.fao.geonet.services.creativecommons;

import java.io.File;
import java.net.URL;
import java.util.List;
import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;
import jeeves.utils.Xml;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.services.metadata.Update;
import org.fao.geonet.lib.ResourceLib;
import org.jdom.Element;

//=============================================================================

/** Handles the get operation to extract creative commons licenses
  */

public class GetLicenses implements Service
{
	private Element config;
	private Update  update = new Update();

	//----------------------------------------------------------------------------
	//---
	//--- Init
	//---
	//----------------------------------------------------------------------------

	public void init(String appPath, ServiceConfig params) throws Exception
	{
		update.init(appPath, params);
	}

	//----------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//----------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context) throws Exception
	{

		Element results = new Element("creativecommons");
		String jurisdiction     = Util.getParam(params, "jurisdiction");
		results.setAttribute("jurisdiction", jurisdiction);
		// for the moment, skip rest cc
		if (jurisdiction.trim().length() != 0) return results; 

		String localeBit = "?&locale=" + context.getLanguage();

		// check cache to see whether we have the cc stuff already - if so then
		// return it, otherwise try and get it from the net

		// Get the Creative Commons License Classes for the locale specified. 
		// FIXME: Needs to be two char locale, otherwise cc webservice falls
		// back to en 
		Element classes = loadElementKvp("classes"+localeBit, context);
		if (classes == null) return results;

		for (Object o : classes.getChildren()) {
			if (o instanceof Element) {
				Element classElem = (Element)o;
				if (classElem.getName().equals("license")) {
					System.out.println("Returning license class "+classElem.getAttributeValue("id"));
					Element license = loadElementKvp("license/" + classElem.getAttributeValue("id") + localeBit, context);
					results.addContent(license);
				}
			}
		}
		System.out.println("Read licenses:\n"+Xml.getString(results));
		return results;
	}

	private Element loadElementKvp(String urlAddition, ServiceContext context) {
		// Use the Creative Commons License webservice api to get the required info
		String ccLicenseUrl = Geonet.CC_API_REST_URL + urlAddition;
		Element elem = null;
		try {
			context.info("Calling cc webservices with "+ccLicenseUrl);
			elem = Xml.loadFile(new URL(ccLicenseUrl));
		} catch (Exception e) {
			context.error("Cannot load request from " + ccLicenseUrl + "\n Error was: " + e); 
		}
		return elem;
		
	}

}
//=============================================================================

