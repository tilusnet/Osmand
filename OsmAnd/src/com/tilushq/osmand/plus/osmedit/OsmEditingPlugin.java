package com.tilushq.osmand.plus.osmedit;

import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;

import com.tilushq.osmand.plus.ContextMenuAdapter;
import com.tilushq.osmand.plus.OsmandApplication;
import com.tilushq.osmand.plus.OsmandPlugin;
import com.tilushq.osmand.plus.OsmandSettings;
import com.tilushq.osmand.plus.R;
import com.tilushq.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import com.tilushq.osmand.plus.activities.EnumAdapter;
import com.tilushq.osmand.plus.activities.LocalIndexInfo;
import com.tilushq.osmand.plus.activities.LocalIndexesActivity;
import com.tilushq.osmand.plus.activities.MapActivity;
import com.tilushq.osmand.plus.activities.SettingsActivity;
import com.tilushq.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import com.tilushq.osmand.plus.activities.LocalIndexesActivity.UploadVisibility;
import com.tilushq.osmand.plus.views.OsmandMapTileView;

import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class OsmEditingPlugin extends OsmandPlugin {
	private static final String ID = "osm.editing";
	private OsmandSettings settings;
	private OsmandApplication app;
	
	@Override
	public String getId() {
		return ID;
	}
	
	public OsmEditingPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		return true;
	}
	
	private OsmBugsLayer osmBugsLayer;
	private EditingPOIActivity poiActions;
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity){
		if (osmBugsLayer == null) {
			registerLayers(activity);
		}
		if(mapView.getLayers().contains(osmBugsLayer) != settings.SHOW_OSM_BUGS.get()){
			if(settings.SHOW_OSM_BUGS.get()){
				mapView.addLayer(osmBugsLayer, 2);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}
	}
	
	@Override
	public void registerLayers(MapActivity activity){
		osmBugsLayer = new OsmBugsLayer(activity);
	}
	
	public OsmBugsLayer getBugsLayer(MapActivity activity) {
		if(osmBugsLayer == null) {
			registerLayers(activity);
		}
		return osmBugsLayer;
	}

	@Override
	public void mapActivityCreate(MapActivity activity) {
		// Always create new actions !
		poiActions = new EditingPOIActivity(activity);
		activity.addDialogProvider(getPoiActions(activity));
		activity.addDialogProvider(getBugsLayer(activity));
	}
	
	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setTitle(R.string.osm_settings);
		grp.setSummary(R.string.osm_settings_descr);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsOsmEditingActivity.class));
				return true;
			}
		});
		screen.addPreference(grp);
		
	}
	
	public EditingPOIActivity getPoiActions(MapActivity activity) {
		if(poiActions == null) {
			poiActions = new EditingPOIActivity(activity);
		}
		return poiActions;
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			final Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_create_poi) {
					getPoiActions(mapActivity).showCreateDialog(latitude, longitude);
				} else if (resId == R.string.context_menu_item_open_bug) {
					if(osmBugsLayer == null) {
						registerLayers(mapActivity);
					}
					osmBugsLayer.openBug(latitude, longitude);
				} else if (resId == R.string.poi_context_menu_delete) {
					getPoiActions(mapActivity).showDeleteDialog((Amenity) selectedObj);
				} else if (resId == R.string.poi_context_menu_modify) {
					getPoiActions(mapActivity).showEditDialog((Amenity) selectedObj);
				}
			}
		};
		if(selectedObj instanceof Amenity) {
			adapter.registerItem(R.string.poi_context_menu_modify, R.drawable.list_activities_poi_modify, listener, 1);
			adapter.registerItem(R.string.poi_context_menu_delete, R.drawable.list_activities_poi_remove, listener, 2);
		} else {
			adapter.registerItem(R.string.context_menu_item_create_poi, R.drawable.list_activities_create_poi, listener, -1);
		}
		adapter.registerItem(R.string.context_menu_item_open_bug, R.drawable.list_activities_osm_bugs, listener, -1);
	}

	@Override
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
		adapter.registerSelectedItem(R.string.layer_osm_bugs, settings.SHOW_OSM_BUGS.get() ? 1 : 0, R.drawable.list_activities_osm_bugs,
				new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if (itemId == R.string.layer_osm_bugs) {
							settings.SHOW_OSM_BUGS.set(isChecked);
						}
					}
				}, 8);

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osm_editing_plugin_description);
	}
	
	@Override
	public void contextMenuLocalIndexes(final LocalIndexesActivity la, final LocalIndexInfo info, ContextMenuAdapter adapter) {
		if (info.getType() == LocalIndexType.GPX_DATA) {
			adapter.registerItem(R.string.local_index_mi_upload_gpx, 0, new OnContextMenuClick() {

				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					sendGPXFiles(la, info);
				}
			}, 0);
		}
	}
	
	@Override
	public void optionsMenuLocalIndexes(final LocalIndexesActivity la, ContextMenuAdapter optionsMenuAdapter) {
		optionsMenuAdapter.registerItem(R.string.local_index_mi_upload_gpx, 0, new OnContextMenuClick() {

			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				la.openSelectionMode(R.string.local_index_mi_upload_gpx, R.drawable.a_9_av_upload_dark, 
						R.drawable.a_9_av_upload_light, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								sendGPXFiles(la, la.getSelectedItems().toArray(new LocalIndexInfo[la.getSelectedItems().size()]));								
							}
						}, null, LocalIndexType.GPX_DATA);
			}
		}, 5);
	}
	
	public boolean sendGPXFiles(final LocalIndexesActivity la, final LocalIndexInfo... info){
		String name = settings.USER_NAME.get();
		String pwd = settings.USER_PASSWORD.get();
		if(Algorithms.isEmpty(name) || Algorithms.isEmpty(pwd)){
			AccessibleToast.makeText(la, R.string.validate_gpx_upload_name_pwd, Toast.LENGTH_LONG).show();
			return false;
		}
		Builder bldr = new AlertDialog.Builder(la);
		LayoutInflater inflater = (LayoutInflater)la.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.send_gpx_osm, null);
		final EditText descr = (EditText) view.findViewById(R.id.DescriptionText);
		if(info.length > 0 && info[0].getFileName() != null) {
			int dt = info[0].getFileName().indexOf('.');
			descr.setText(info[0].getFileName().substring(0, dt));
		}
		final EditText tags = (EditText) view.findViewById(R.id.TagsText);		
		final Spinner visibility = ((Spinner)view.findViewById(R.id.Visibility));
		EnumAdapter<UploadVisibility> adapter = new EnumAdapter<UploadVisibility>(la, android.R.layout.simple_spinner_item, UploadVisibility.values());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		visibility.setAdapter(adapter);
		visibility.setSelection(0);
		
		bldr.setView(view);
		bldr.setNegativeButton(R.string.default_buttons_no, null);
		bldr.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new UploadGPXFilesTask(la, descr.getText().toString(), tags.getText().toString(), 
				 (UploadVisibility) visibility.getItemAtPosition(visibility.getSelectedItemPosition())
					).execute(info);
			}
		});
		bldr.show();
		return true;
	}
	

	@Override
	public String getName() {
		return app.getString(R.string.osm_settings);
	}

}
