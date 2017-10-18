/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package org.fao.geonet.kernel;

import org.fao.geonet.kernel.datamanager.IMetadataValidator;
import org.jdom.Element;
import jeeves.server.context.ServiceContext;

public enum HarvestValidationEnum {

    NOVALIDATION {
        public void validate(ServiceContext context, Element xml) throws Exception {
        }
    },

    /**
     * Process validation against schema
     */
    XSDVALIDATION {
        public void validate(ServiceContext context, Element xml) throws Exception {
            DataManager.setNamespacePrefix(xml);

            String schema = context.getBean(SchemaManager.class).autodetectSchema(xml);
            context.getBean(IMetadataValidator.class).validateMetadata(schema, xml, context, null);

        }

    },

    /**
     * Process validation against schematron and XSD
     */
    SCHEMATRONVALIDATION {
        public void validate(ServiceContext context, Element xml) throws Exception {
            String schema = context.getBean(SchemaManager.class).autodetectSchema(xml);
            context.getBean(IMetadataValidator.class).validateMetadata(schema, xml, context, null);
        }

    };

    public static HarvestValidationEnum lookup(final String name) {
        if ("true".equals(name)) {
            return HarvestValidationEnum.XSDVALIDATION;
        }
        if ("false".equals(name)) {
            return HarvestValidationEnum.NOVALIDATION;
        }
        if (HarvestValidationEnum.NOVALIDATION.name().equals(name)) {
            return HarvestValidationEnum.NOVALIDATION;
        }
        if (HarvestValidationEnum.XSDVALIDATION.name().equals(name)) {
            return HarvestValidationEnum.XSDVALIDATION;
        }
        if (HarvestValidationEnum.SCHEMATRONVALIDATION.name().equals(name)) {
            return HarvestValidationEnum.SCHEMATRONVALIDATION;
        }
        return HarvestValidationEnum.NOVALIDATION;
    }

    public void validate(ServiceContext context, Element xml) throws Exception {
      String schema = context.getBean(SchemaManager.class).autodetectSchema(xml);
      context.getBean(IMetadataValidator.class).validateMetadata(schema, xml, context, null);
 	  }
}
