package com.tilushq.osmand.plus.srtmplugin;

import com.tilushq.osmand.plus.ApplicationMode;
import com.tilushq.osmand.plus.ContextMenuAdapter;
import com.tilushq.osmand.plus.OsmandApplication;
import com.tilushq.osmand.plus.OsmandPlugin;
import com.tilushq.osmand.plus.OsmandSettings;
import com.tilushq.osmand.plus.R;
import com.tilushq.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import com.tilushq.osmand.plus.OsmandSettings.CommonPreference;
import com.tilushq.osmand.plus.activities.MapActivity;
import com.tilushq.osmand.plus.views.OsmandMapTileView;

import android.content.DialogInterface;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	private OsmandApplication app;
	private boolean paid;
	private HillshadeLayer hillshadeLayer;
	private CommonPreference<Boolean> HILLSHADE;
	
	@Override
	public String getId() {
		return ID;
	}

	public SRTMPlugin(OsmandApplication app, boolean paid) {
		this.app = app;
		this.paid = paid;
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if(pref.get().equals("")) {
			for(ApplicationMode m : ApplicationMode.values()) {
				if(pref.getModeValue(m).equals("")) {
					pref.setModeValue(m, "13");
				}
			}
		}

	}
	
	public boolean isPaid() {
		return paid;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.srtm_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		HILLSHADE = app.getSettings().registerBooleanPreference("hillshade_layer", true);
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		if (hillshadeLayer != null) {
			activity.getMapView().removeLayer(hillshadeLayer);
		}
		if (HILLSHADE.get()) {
			hillshadeLayer = new HillshadeLayer(activity, this);
			activity.getMapView().addLayer(hillshadeLayer, 0.6f);
		}
	}

	public boolean isHillShadeLayerEnabled() {
		return HILLSHADE.get();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (HILLSHADE.get()) {
			if (hillshadeLayer == null) {
				registerLayers(activity);
			}
		} else {
			if (hillshadeLayer != null) {
				mapView.removeLayer(hillshadeLayer);
				hillshadeLayer = null;
				activity.refreshMap();
			}
		}
	}
	
	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.layer_hillshade) {
					dialog.dismiss();
					HILLSHADE.set(!HILLSHADE.get());
					updateLayers(mapView, mapActivity);
				}
			}
		};
		adapter.registerSelectedItem(R.string.layer_hillshade, HILLSHADE.get()? 1 : 0, R.drawable.list_activities_overlay_map, listener, 9);
	}
	
	@Override
	public void disable(OsmandApplication app) {
	}

}
