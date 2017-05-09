package com.graphhopper.http;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.reader.gtfs.GraphHopperGtfs;
import com.graphhopper.reader.gtfs.GtfsStorage;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.TranslationMap;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;

public final class GraphHopperGtfsModule extends AbstractModule {

    @Provides
    @Singleton
    GraphHopperAPI createGraphHopper(EncodingManager encodingManager, TranslationMap translationMap, GraphHopperStorage graphHopperStorage, LocationIndex locationIndex, GtfsStorage gtfsStorage) {
        return new GraphHopperGtfs(encodingManager, translationMap, graphHopperStorage, locationIndex, gtfsStorage);
    }

    @Provides
    @Singleton
    GHDirectory createGHDirectory(CmdArgs args) {
        return GraphHopperGtfs.createGHDirectory(args.get("graph.location", "target/tmp"));
    }

    @Provides
    @Singleton
    GraphHopperStorage createGraphHopperStorage(CmdArgs args, GHDirectory directory, EncodingManager encodingManager, GtfsStorage gtfsStorage) {
        return GraphHopperGtfs.createOrLoad(directory, encodingManager, gtfsStorage,
                args.getBool("gtfs.createwalknetwork", false),
                args.has("gtfs.file") ? Arrays.asList(args.get("gtfs.file", "").split(",")) : Collections.emptyList(),
                args.has("datareader.file") ? Arrays.asList(args.get("datareader.file", "").split(",")) : Collections.emptyList());
    }

    @Provides
    @Singleton
    LocationIndex createLocationIndex(GraphHopperStorage graphHopperStorage, GHDirectory directory) {
        return GraphHopperGtfs.createOrLoadIndex(directory, graphHopperStorage);
    }

    @Provides
    @Singleton
    @Named("hasElevation")
    boolean hasElevation() {
        return false;
    }

    @Provides
    @Singleton
    GtfsStorage createGtfsStorage() {
        return GraphHopperGtfs.createGtfsStorage();
    }

    @Provides
    @Singleton
    EncodingManager createEncodingManager() {
        return GraphHopperGtfs.createEncodingManager();
    }

    @Provides
    @Singleton
    TranslationMap createTranslationMap() {
        return GraphHopperGtfs.createTranslationMap();
    }

    @Provides
    @Singleton
    RouteSerializer getRouteSerializer(GraphHopperStorage storage) {
        return new SimpleRouteSerializer(storage.getBounds());
    }

    @Override
    protected void configure() {}

}
