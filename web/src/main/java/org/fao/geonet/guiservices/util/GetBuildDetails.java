//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
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
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.guiservices.util;

import java.util.Properties;
import java.io.InputStream;

import jeeves.interfaces.Service;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.jdom.Element;

//=============================================================================

/** This service returns useful information about the maven build process - app name, date of last build
  */

public class GetBuildDetails implements Service
{
  private static final String VERSION = "version";
	private static final String REVISION = "revision";
	private static final String NAME = "name";
	private static final String TIMESTAMP = "timestamp";

	private static final String PROPSFILE = "geonetwork.buildnumber.properties";

	private static Properties buildProps = new Properties();
  private static boolean haveRead = false;	

	public void init(String appPath, ServiceConfig params) throws Exception {}

	//--------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//--------------------------------------------------------------------------

	public Element exec(Element params, ServiceContext context) throws Exception
	{

		if (!GetBuildDetails.getHaveRead()) {
			InputStream stream = null;
			try {
				stream = this.getClass().getClassLoader().getResourceAsStream(PROPSFILE);
				buildProps.load(stream);
				GetBuildDetails.setHaveRead(true);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (stream != null) stream.close();
			}
		}

		/*
#Created by build system. Do not modify
#Fri May 13 08:59:24 AEST 2016
version=2.10.5-SNAPSHOT
revision=367fe61f12b249c15608c736be372f69c170590d
name=ANZMEST GeoNetwork
timestamp=2016-05-13T08\:59\:24
    */

		String version = buildProps.getProperty(VERSION);
		String revision = buildProps.getProperty(REVISION);
		String name = buildProps.getProperty(NAME);
		String timestamp = buildProps.getProperty(TIMESTAMP);

		Element root = new Element("a");
		if (version != null) {
			root.addContent(new Element(VERSION).setText(version));
		} 
		if (revision != null) {
			root.addContent(new Element(REVISION).setText(revision));
		} 
		if (name != null) {
			root.addContent(new Element(NAME).setText(name));
		} 
		if (name != null) {
			root.addContent(new Element(TIMESTAMP).setText(timestamp));
		} 
		return root;
	}

  public static void setHaveRead(boolean value) {
    GetBuildDetails.haveRead = value;
  }

  public static boolean getHaveRead() {
    return GetBuildDetails.haveRead;
  }
}

//=============================================================================

