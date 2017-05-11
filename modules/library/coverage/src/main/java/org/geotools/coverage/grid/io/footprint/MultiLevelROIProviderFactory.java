/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015 - 2016, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverage.grid.io.footprint;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataUtilities;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.Utilities;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Factory class used for returning a {@link MultiLevelROIProvider} based on the input footprint properties
 * and files
 * 
 * @author Andrea Aime, GeoSolutions
 * @author Nicola Lagomarsini, GeoSolutions
 * @author Daniele Romagnoli, GeoSolutions
 */
public class MultiLevelROIProviderFactory {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
                    .getLogger(MultiLevelROIProviderFactory.class.toString());

    // well known properties
    public static final String SOURCE_PROPERTY = "footprint_source";

    // store types
    public static final String TYPE_SIDECAR = "sidecar";
    public static final String TYPE_RASTER = "raster";

    protected MultiLevelROIProviderFactory() {
    }

    /**
     * Builds a footprint provider from file location
     * 
     * @param the reference file. 
     * @return
     * @throws Exception
     * 
     * @see {@link #createFootprintProvider(File, Geometry)}
     */
    public static MultiLevelROIProvider createFootprintProvider(File referenceFile) {
        return createFootprintProvider(referenceFile, null);
    }

    /**
     * Builds a footprint provider from file location
     * 
     * @param It can be: 
     *      - a folder containing the footprint config files (if any) OR the footprint itself. 
     *      - the data file for which we are looking for a footprint.
     * @param The optional granuleBounds geometry. if not null, it will be used as data's reference geometry.
     * @return
     * @throws Exception
     */
    public static MultiLevelROIProvider createFootprintProvider(final File referenceFile, Geometry granuleBounds) {
        Utilities.ensureNonNull("referenceFile", referenceFile);
        File configDirectory = referenceFile.isDirectory() ? referenceFile : referenceFile.getParentFile();
        File configFile = new File(configDirectory, "footprints.properties");
        if (!configFile.exists()) {
            configFile = SidecarFootprintProvider.getAlternativeFile(configFile);
        }
        final Properties properties = initProperties(configFile);

        // load the type of config file
        // Only sidecar footprints are currently supported
        String source = (String) properties.get(SOURCE_PROPERTY);
        FootprintGeometryProvider provider = null;
        if (source == null || TYPE_SIDECAR.equals(source)) {
            provider = new SidecarFootprintProvider(referenceFile);
        } else {
            throw new IllegalArgumentException("Invalid source type, it should be a reference "
                    + "to a 'sidecar', but was '" + source + "' instead");
        }
        // Create the provider
        return createProvider(provider, properties, granuleBounds);
    }

    /**
     * Create the {@link MultiLevelROIProvider} given a {@link FootprintGeometryProvider}, 
     * footprint config properties (if any) and, optionally a Default GranuleBounds geometry
     * 
     * @param provider the {@link FootprintGeometryProvider} instance to be used to provide ROIs.
     * @param properties an optional {@link Properties} instance containing footprints configuration.
     * @param imposedGranuleBounds an optional default granuleBounds geometry
     * @return
     */
    protected static MultiLevelROIProvider createProvider(FootprintGeometryProvider provider,
            Properties properties, Geometry imposedGranuleBounds) {
        // handle inset if necessary
        double inset = FootprintInsetPolicy.getInset(properties);
        FootprintInsetPolicy insetPolicy = FootprintInsetPolicy.getInsetPolicy(properties);
        return new MultiLevelROIGeometryProvider(provider, inset, insetPolicy, imposedGranuleBounds);
    }

    protected static Properties initProperties(File configFile) {
        Properties properties = null;
        if (configFile != null && configFile.exists()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Retrieving footprints properties from "
                        + "the specified config file: " + configFile.getAbsolutePath());
            }
            properties = CoverageUtilities.loadPropertiesFromURL(DataUtilities.fileToURL(configFile));
        } else {
            properties = new Properties();
        }
        return properties;
    }
}
