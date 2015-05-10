package net.osmand.plus.osmedit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;

/**
 * Created by Denis on
 * 20.03.2015.
 */
public class OsmEditsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	private static final int startZoom = 10;
	private final OsmEditingPlugin plugin;
	private final MapActivity activity;
	private Bitmap poi;
	private Bitmap bug;
	private OsmandMapTileView view;
	private Paint pointAtUI;
	private Paint paintIcon;
	private Paint point;



	public OsmEditsLayer(MapActivity activity, OsmEditingPlugin plugin) {
		this.activity = activity;
		this.plugin = plugin;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;


		pointAtUI = new Paint();
		pointAtUI.setColor(0xa0FF3344);
		pointAtUI.setStyle(Paint.Style.FILL);

		poi = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_poi);
		bug = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_poi);

		paintIcon = new Paint();

		point = new Paint();
		point.setColor(Color.RED);
		point.setAntiAlias(true);
		point.setStyle(Paint.Style.STROKE);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			DataTileManager<OsmPoint> points = plugin.getLocalOsmEdits();
			final QuadRect latlon = tileBox.getLatLonBounds();
			List<OsmPoint> objects = points.getObjects(latlon. top, latlon.left, latlon.bottom, latlon.right);
			
			for (OsmPoint o : objects) {
				int locationX = tileBox.getPixXFromLonNoRot(o.getLongitude());
				int locationY = tileBox.getPixYFromLatNoRot(o.getLatitude());
				canvas.rotate(-view.getRotate(), locationX, locationY);	
				Bitmap b;
				if (o.getGroup() == OsmPoint.Group.POI) {
					b = poi;
				} else if (o.getGroup() == OsmPoint.Group.BUG) {
					b = bug;
				} else {
					b = poi;
				}
				canvas.drawBitmap(b, locationX - b.getWidth() / 2, locationY - b.getHeight(), paintIcon);
				canvas.rotate(view.getRotate(), locationX, locationY);
			}
		}
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}


	public void getOsmEditsFromPoint(PointF point, RotatedTileBox tileBox, List<? super OsmPoint> am) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		int compare = getRadiusPoi(tileBox);
		int radius = compare * 3 / 2;
		for (OsmPoint n : plugin.getAllEdits()) {
			int x = (int) tileBox.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, compare)) {
				compare = radius;
				am.add(n);
			}
		}
	}

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		if(tb.getZoom()  < startZoom){
			r = 0;
		} else {
			r = 15;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		getOsmEditsFromPoint(point, tileBox, o);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OsmPoint) {
			return new LatLon(((OsmPoint)o).getLatitude(),((OsmPoint)o).getLongitude());
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof OsmPoint) {
			OsmPoint point =  (OsmPoint) o;
			return OsmEditingPlugin.getEditName(point);
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof OsmPoint) {
			OsmPoint point =  (OsmPoint) o;
			String name = "";
			String type = "";
			if (point.getGroup() == OsmPoint.Group.POI){
				name = ((OpenstreetmapPoint) point).getName();
				type = PointDescription.POINT_TYPE_OSM_NOTE;
			} else if (point.getGroup() == OsmPoint.Group.BUG) {
				name = ((OsmNotesPoint) point).getText();
				type = PointDescription.POINT_TYPE_OSM_BUG;
			}
			return new PointDescription(type, name);
		}
		return null;
	}
}
