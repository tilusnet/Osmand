package net.osmand.plus.development;


import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
//import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.util.SunriseSunset;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsDevelopmentActivity extends SettingsBaseActivity {


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.debugging_and_development);
		PreferenceScreen cat = getPreferenceScreen();
		
		CheckBoxPreference dbg = createCheckBoxPreference(settings.DEBUG_RENDERING_INFO, 
				R.string.trace_rendering, R.string.trace_rendering_descr);
		cat.addPreference(dbg);
		
		cat.addPreference(createCheckBoxPreference(settings.DISABLE_COMPLEX_ROUTING, R.string.disable_complex_routing, R.string.disable_complex_routing_descr));
	
		cat.addPreference(createCheckBoxPreference(settings.USE_FAST_RECALCULATION, "Smart route recalculation", "Recalculate only initial part of the route for long trips"));
		
		
		cat.addPreference(createCheckBoxPreference(settings.USE_MAGNETIC_FIELD_SENSOR_COMPASS, R.string.use_magnetic_sensor, R.string.use_magnetic_sensor_descr));

		Preference pref = new Preference(this);
		pref.setTitle(R.string.test_voice_prompts);
		pref.setSummary(R.string.play_commands_of_currently_selected_voice);
		pref.setKey("test_voice_commands");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsDevelopmentActivity.this, TestVoiceActivity.class));
				return true;
			}
		});
		cat.addPreference(pref);

		pref = new Preference(this);
		pref.setTitle(R.string.app_modes_choose);
		pref.setSummary(R.string.app_modes_choose_descr);
		pref.setKey("available_application_modes");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				availableProfileDialog();
				return true;
			}
		});
		cat.addPreference(pref);

		pref = new Preference(this);
		pref.setTitle(R.string.global_app_allocated_memory);

		long javaAvailMem = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ (1024*1024l);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024*1024l);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024*1024l);
		pref.setSummary(getString(R.string.global_app_allocated_memory_descr, javaAvailMem, javaTotal, dalvikSize));
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		cat.addPreference(pref);
		
//		ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		long totalSize = memoryInfo.availMem / (1024*1024l);
		MemoryInfo mem = new Debug.MemoryInfo();
		Debug.getMemoryInfo(mem);
		pref = new Preference(this);
		pref.setTitle(R.string.native_app_allocated_memory);
		pref.setSummary(getString(R.string.native_app_allocated_memory_descr 
				, mem.nativePrivateDirty / 1024, mem.dalvikPrivateDirty / 1024 , mem.otherPrivateDirty / 1024
				, mem.nativePss / 1024, mem.dalvikPss / 1024 , mem.otherPss / 1024));
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		cat.addPreference(pref);

		final Preference agpspref = new Preference(this);
		agpspref.setTitle(R.string.agps_info);
		if (settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get() != 0L) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			agpspref.setSummary(getString(R.string.agps_data_last_downloaded, prt.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get())));
		} else {
			agpspref.setSummary(getString(R.string.agps_data_last_downloaded, "--"));
		}
		agpspref.setSelectable(true);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		agpspref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(getMyApplication().getSettings().isInternetConnectionAvailable(true)) {
					getMyApplication().getLocationProvider().redownloadAGPS();
					SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
					agpspref.setSummary(getString(R.string.agps_data_last_downloaded, prt.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get())));
				}
			return true;
			}
		});
		cat.addPreference(agpspref);
		
		SunriseSunset sunriseSunset = getMyApplication().getDaynightHelper().getSunriseSunset();
		pref = new Preference(this);
		pref.setTitle(R.string.day_night_info);
		if (sunriseSunset != null) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			pref.setSummary(getString(R.string.day_night_info_description, prt.format(sunriseSunset.getSunrise()),
					prt.format(sunriseSunset.getSunset())));
		} else {
			pref.setSummary(getString(R.string.day_night_info_description, "null", "null"));
		}
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		cat.addPreference(pref);	
	}
	
	protected void availableProfileDialog() {
		Builder b = new AlertDialog.Builder(this);
		final List<ApplicationMode> modes = ApplicationMode.allPossibleValues();
		modes.remove(ApplicationMode.DEFAULT);
		final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>(ApplicationMode.values(settings));
		selected.remove(ApplicationMode.DEFAULT);
		View v = AppModeDialog.prepareAppModeView(this, modes, selected, null, false, false,
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey()+",");
						for(ApplicationMode mode :  modes) {
							if(selected.contains(mode)) {
								vls.append(mode.getStringKey()+",");
							}
						}
						settings.AVAILABLE_APP_MODES.set(vls.toString());
					}
				});
		b.setTitle(R.string.profile_settings);
		b.setPositiveButton(R.string.shared_string_ok, null);
		b.setView(v);
		b.show();
	}



}
