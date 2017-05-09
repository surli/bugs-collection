package com.graphhopper.reader.gtfs;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.util.EdgeIteratorState;

class PtExitPositionLookupEdgeFilter implements EdgeFilter {

	PtFlagEncoder encoder;

	PtExitPositionLookupEdgeFilter(PtFlagEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	public boolean accept(EdgeIteratorState edgeState) {
		GtfsStorage.EdgeType edgeType = encoder.getEdgeType(edgeState.getFlags());
		return edgeType == GtfsStorage.EdgeType.EXIT_PT;
	}
}
