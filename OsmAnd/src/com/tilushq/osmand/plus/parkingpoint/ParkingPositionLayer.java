package com.tilushq.osmand.plus.parkingpoint;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import com.tilushq.osmand.plus.R;
import com.tilushq.osmand.plus.activities.MapActivity;
import com.tilushq.osmand.plus.views.ContextMenuLayer;
import com.tilushq.osmand.plus.views.OsmandMapLayer;
import com.tilushq.osmand.plus.views.OsmandMapTileView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Class represents a layer which depicts the position of the parked car
 * @author Alena Fedasenka
 * @see ParkingPositionPlugin
 *
 */
public class ParkingPositionLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	/**
	 * magic number so far
	 */
	private static final int radius = 20;

	private LatLon parkingPoint = null;

	private DisplayMetrics dm;
	
	private final MapActivity map;
	private OsmandMapTileView view;
	
	private Paint bitmapPaint;

	private Bitmap parkingNoLimitIcon;
	private Bitmap parkingLimitIcon;
	
	private boolean timeLimit;

	private ParkingPositionPlugin plugin;

	public ParkingPositionLayer(MapActivity map, ParkingPositionPlugin plugin) {
		this.map = map;
		this.plugin = plugin;
	}
	
	public LatLon getParkingPoint() {
		return parkingPoint;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		parkingPoint = plugin.getParkingPosition();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		bitmapPaint = new Paint();
		bitmapPaint.setDither(true);
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);
		parkingNoLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_parking_pos_no_limit);
		parkingLimitIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_parking_pos_limit);
		parkingPoint = plugin.getParkingPosition();
		timeLimit = plugin.getParkingType();
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		if (parkingPoint == null)
			return;
		
		Bitmap parkingIcon;
		if (!timeLimit) {
			parkingIcon = parkingNoLimitIcon;
		} else {
			parkingIcon = parkingLimitIcon;
		}
		double latitude = parkingPoint.getLatitude();
		double longitude = parkingPoint.getLongitude();
		if (isLocationVisible(latitude, longitude)) {
			int marginX = parkingNoLimitIcon.getWidth() / 2;
			int marginY = parkingNoLimitIcon.getHeight();
			int locationX = view.getMapXForPoint(longitude);
			int locationY = view.getMapYForPoint(latitude);
			canvas.rotate(-view.getRotate(), locationX, locationY);
			canvas.drawBitmap(parkingIcon, locationX - marginX, locationY - marginY, bitmapPaint);
		}
	}

	@Override
	public boolean onSingleTap(PointF point) {
		List <LatLon> parkPos = new ArrayList<LatLon>();
		getParkingFromPoint(point, parkPos);
		if(!parkPos.isEmpty()){
			StringBuilder res = new StringBuilder();
			res.append(view.getContext().getString(R.string.osmand_parking_position_description));
			AccessibleToast.makeText(view.getContext(), getObjectDescription(parkingPoint), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, List<Object> o) {
		getParkingFromPoint(point, o);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return parkingPoint;
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof LatLon) {
			StringBuilder timeLimitDesc = new StringBuilder();
			timeLimitDesc.append(map.getString(R.string.osmand_parking_position_description_add_time) + " ");
			timeLimitDesc.append(getFormattedTime(plugin.getStartParkingTime()) + ".");
			if (plugin.getParkingType()) {
				// long parkingTime = settings.getParkingTime();
				// long parkingStartTime = settings.getStartParkingTime();
				// Time time = new Time();
				// time.set(parkingTime);
				// timeLimitDesc.append(map.getString(R.string.osmand_parking_position_description_add) + " ");
				// timeLimitDesc.append(time.hour);
				// timeLimitDesc.append(":");
				// int minute = time.minute;
				// timeLimitDesc.append(minute<10 ? "0" + minute : minute);
				// if (!DateFormat.is24HourFormat(map.getApplicationContext())) {
				// timeLimitDesc.append(time.hour >= 12 ? map.getString(R.string.osmand_parking_pm) :
				// map.getString(R.string.osmand_parking_am));
				// }
				timeLimitDesc.append(map.getString(R.string.osmand_parking_position_description_add) + " ");
				timeLimitDesc.append(getFormattedTime(plugin.getParkingTime()));
			}
			return map.getString(R.string.osmand_parking_position_description, timeLimitDesc.toString());
		}
		return null;
	}

	String getFormattedTime(long timeInMillis) {
		StringBuilder timeStringBuilder = new StringBuilder();
		Time time = new Time();
		time.set(timeInMillis);
		timeStringBuilder.append(time.hour);
		timeStringBuilder.append(":");
		int minute = time.minute;
		timeStringBuilder.append(minute < 10 ? "0" + minute : minute);
		if (!DateFormat.is24HourFormat(map)) {
			timeStringBuilder.append(time.hour >= 12 ? map.getString(R.string.osmand_parking_pm) : map
					.getString(R.string.osmand_parking_am));
		}
		return timeStringBuilder.toString();
	}
	
	@Override
	public String getObjectName(Object o) {
		return view.getContext().getString(R.string.osmand_parking_position_name);
	}
	
	public void setParkingPointOnLayer(LatLon point, boolean timeLimit) {
		this.timeLimit = timeLimit;
		this.parkingPoint = point;
		if (view != null) {
			view.refreshMap();
		}
	}
	
	public void removeParkingPoint(){
		this.parkingPoint = null;
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return true if the parking point is located on a visible part of map
	 */
	private boolean isLocationVisible(double latitude, double longitude){
		if(parkingPoint == null || view == null){
			return false;
		}
		return view.isPointOnTheRotatedMap(latitude, longitude);
	}
	
	/**
	 * @param point
	 * @param parkingPosition is in this case not necessarily has to be a list, 
	 * but it's also used in method <link>collectObjectsFromPoint(PointF point, List<Object> o)</link>
	 */
	private void getParkingFromPoint(PointF point, List<? super LatLon> parkingPosition) {
		if (parkingPoint != null && view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			LatLon position = plugin.getParkingPosition();
			int x = view.getRotatedMapXForPoint(position.getLatitude(), position.getLongitude());
			int y = view.getRotatedMapYForPoint(position.getLatitude(), position.getLongitude());
			// the width of an image is 40 px, the height is 60 px -> radius = 20,
			// the position of a parking point relatively to the icon is at the center of the bottom line of the image
			if (Math.abs(x - ex) <= radius && ((y - ey) <= radius * 2) && ((y - ey) >= -radius)) {
				parkingPosition.add(parkingPoint);
			}
		}
	}
	
	
	
}
