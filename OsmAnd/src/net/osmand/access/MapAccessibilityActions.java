package net.osmand.access;

import com.tilushq.osmand.plus.activities.MapActivity;
import com.tilushq.osmand.plus.views.ContextMenuLayer;
import com.tilushq.osmand.plus.views.OsmandMapTileView;

import net.osmand.data.LatLon;
import android.graphics.PointF;
import android.os.Build;

// Accessibility actions for map view.
public class MapAccessibilityActions implements AccessibilityActionsProvider {

    private final MapActivity activity;

    public MapAccessibilityActions(final MapActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onClick(PointF point) {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().getInternalAPI().accessibilityEnabled()) {
        	// not sure if it is very clear why should I mark destination first when I tap on the object
        	return activity.getMyApplication().getLocationProvider().emitNavigationHint();
        }
        return false;
    }

    @Override
    public boolean onLongClick(PointF point) {
        if ((Build.VERSION.SDK_INT >= 14) && activity.getMyApplication().getInternalAPI().accessibilityEnabled()) {
            final OsmandMapTileView mapView = activity.getMapView();
            LatLon pressedLoc = mapView.getLatLonFromScreenPoint(point.x, point.y);
            ContextMenuLayer cm = activity.getMapLayers().getContextMenuLayer();
            LatLon loc = cm.selectObjectsForContextMenu(point);
            if (cm.getSelectedObjectName() != null) {
            	cm.showContextMenuForSelectedObjects(loc);
			} else {
				activity.getMapActions().contextMenuPoint(pressedLoc.getLatitude(), pressedLoc.getLongitude());
			}
            
//            activity.getMapActions().contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
            return true;
        }
        return false;
    }

}
