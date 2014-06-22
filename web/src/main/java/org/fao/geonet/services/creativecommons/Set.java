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
import java.util.List;
import jeeves.interfaces.Service;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.utils.Util;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.exceptions.ConcurrentUpdateEx;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.services.metadata.Update;
import org.fao.geonet.lib.Lib;
import org.jdom.Element;

//=============================================================================

/** Handles the get operation
  */

public class Set implements Service
{
	private Element config;

	//-----------------------------------------------------------------------------
	//---
	//--- Init
	//---
	//-----------------------------------------------------------------------------

	public void init(String appPath, ServiceConfig params) throws Exception
	{
	}

	//-----------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//-----------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context) throws Exception
	{

		String id   = Util.getParam(params, Params.ID);
		String combo	= Util.getParam(params, Params.CHOICE);
		String jurisdiction	= Util.getParam(params, Params.JURISDICTION);
		String version = Util.getParam(params, Params.VERSION);
		String type = Util.getParam(params, Params.TYPE);
		String btn = Util.getParam(params, Params.BTN);

		//--- check if we have permission

		Lib.resource.checkEditPrivilege(context, id);

		//--- environment vars
	
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager dataMan = gc.getDataManager();
		Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);

		//--- check if the metadata has been modified from last time

		if (version != null && !dataMan.getVersion(id).equals(version)) 
			throw new ConcurrentUpdateEx(id);

   	//--- call the datamanager routine to run the xslt and write the info

		String tokens[];
		if (!btn.equals("Cancel")) {
			tokens = combo.split("#");
		} else {
			String original = Util.getParam(params, Params.ORIGINALCHOICE);
			tokens = original.split("#");
		}
			
		if (tokens.length != 3) {
			throw new IllegalArgumentException("choice parameter should be of the form licenseurl#imageurl#licensename eg. http://creativecommons.org/by/nc/2.5#http://i.creativecommons/l/...#CCLicense");
		} 
		String	licenseurl = tokens[0];
		String	imageurl = tokens[1];
		String	licensename = tokens[2];
		dataMan.setCreativeCommons(dbms,context,id,licenseurl,imageurl,jurisdiction,licensename,type);

		//--- pass out the id and the new version for the editor to continue

		Element response = new Element("a");
		response.addContent(new Element("id").setText(id));
		response.addContent(new Element("version").setText(dataMan.getNewVersion(id)));
		return response;
	}
}

//=============================================================================

