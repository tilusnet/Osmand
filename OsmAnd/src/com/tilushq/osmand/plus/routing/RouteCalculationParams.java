package com.tilushq.osmand.plus.routing;

import java.util.List;

import com.tilushq.osmand.plus.ApplicationMode;
import com.tilushq.osmand.plus.ClientContext;
import com.tilushq.osmand.plus.routing.RouteProvider.GPXRouteParams;
import com.tilushq.osmand.plus.routing.RouteProvider.RouteService;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.router.RouteCalculationProgress;

public class RouteCalculationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;
	
	public ClientContext ctx;
	public ApplicationMode mode;
	public RouteService type;
	public GPXRouteParams gpxRoute;
	public RouteCalculationResult previousToRecalculate;
	public boolean fast;
	public boolean optimal;
	public boolean leftSide;
	public RouteCalculationProgress calculationProgress;
	public boolean preciseRouting;
}
