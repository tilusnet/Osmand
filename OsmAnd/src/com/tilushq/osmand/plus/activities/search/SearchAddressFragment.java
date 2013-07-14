package com.tilushq.osmand.plus.activities.search;


import java.text.MessageFormat;

import net.osmand.data.LatLon;

import com.tilushq.osmand.plus.OsmandApplication;
import com.tilushq.osmand.plus.OsmandSettings;
import com.tilushq.osmand.plus.R;
import com.tilushq.osmand.plus.activities.MapActivity;
import com.tilushq.osmand.plus.activities.MapActivityActions;
import com.tilushq.osmand.plus.resources.RegionAddressRepository;

import net.osmand.util.Algorithms;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class SearchAddressFragment extends SherlockFragment {

	public static final String SELECT_ADDRESS_POINT_INTENT_KEY = "SELECT_ADDRESS_POINT_INTENT_KEY";
	public static final int SELECT_ADDRESS_POINT_RESULT_OK = 1;	
	public static final String SELECT_ADDRESS_POINT_LAT = "SELECT_ADDRESS_POINT_LAT";
	public static final String SELECT_ADDRESS_POINT_LON = "SELECT_ADDRESS_POINT_LON";
	
	private Button showOnMap;
	private Button streetButton;
	private Button cityButton;
	private Button countryButton;
	private Button buildingButton;
	private Button navigateTo;
	
	private String region = null;
	private String city = null;
	private String postcode = null;
	private String street = null;
	private String building = null;
	private String street2 = null;
	private boolean radioBuilding = true;
	private Button searchOnline;
	
	private OsmandSettings osmandSettings;
	private LatLon searchPoint = null;

	private boolean selectAddressMode;
	private View view;
	

	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.search_address, container, false);
		
		showOnMap = (Button) findViewById(R.id.ShowOnMap);
		navigateTo = (Button) findViewById(R.id.NavigateTo);
		streetButton = (Button) findViewById(R.id.StreetButton);
		cityButton = (Button) findViewById(R.id.CityButton);
		countryButton = (Button) findViewById(R.id.CountryButton);
		buildingButton = (Button) findViewById(R.id.BuildingButton);
		searchOnline = (Button) findViewById(R.id.SearchOnline);
		osmandSettings = ((OsmandApplication) getApplication()).getSettings();
		attachListeners();
		return view;
	}
	
	private OsmandApplication getApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private Intent createIntent(Class<?> cl){
		LatLon location = null;
		Intent intent = getActivity().getIntent();
		if(intent != null){
			double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
			if(lat != 0 || lon != 0){
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		Intent newIntent = new Intent(getActivity(), cl);
		if (location != null) {
			newIntent.putExtra(SearchActivity.SEARCH_LAT, location.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, location.getLongitude());
		}
		return newIntent;
	}
	
	private void attachListeners() {
		if (getActivity() instanceof SearchActivity) {
			searchOnline.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					((SearchActivity) getActivity()).startSearchAddressOnline();
				}
			});
		} else {
			searchOnline.setVisibility(View.INVISIBLE);
		}
		countryButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchRegionByNameActivity.class));
			}
		});
		cityButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchCityByNameActivity.class));
			}
		});
		streetButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				startActivity(createIntent(SearchStreetByNameActivity.class));
			}
		});
		buildingButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(radioBuilding){
					startActivity(createIntent(SearchBuildingByNameActivity.class));
				} else {
					startActivity(createIntent(SearchStreet2ByNameActivity.class));
				}
			}
		});
		navigateTo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(true);
			}
		});
		showOnMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showOnMap(false);
			}
		});
		findViewById(R.id.ResetBuilding).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				building = null;
				searchPoint = null;
				updateUI();
			}
		 });
		 findViewById(R.id.ResetStreet).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					street = null;
					building = null;
					street2 = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCity).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					postcode = null;
					city = null;
					street = null;
					street2 = null;
					building = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 findViewById(R.id.ResetCountry).setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					region = null;
					postcode = null;
					city = null;
					street = null;
					street2 = null;
					building = null;
					searchPoint = null;
					updateUI();
				}
		 });
		 ((RadioGroup)findViewById(R.id.RadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					SearchAddressFragment.this.radioBuilding = checkedId == R.id.RadioBuilding;
					if(radioBuilding){
						SearchAddressFragment.this.street2 = null;
					} else {
						SearchAddressFragment.this.building = null;
					}
					updateBuildingSection();
				}
				
			});
	}
	
	public void showOnMap(boolean navigateTo) {
		if (searchPoint == null) {
			return;
		}
		String historyName = null;
		String objectName = "";
		int zoom = 14;
		if (!Algorithms.isEmpty(street2) && !Algorithms.isEmpty(street)) {
			String cityName = !Algorithms.isEmpty(postcode) ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_int_streets), street, street2,
					cityName);
			zoom = 17;
		} else if (!Algorithms.isEmpty(building)) {
			String cityName = !Algorithms.isEmpty(postcode) ? postcode : city;
			objectName = street + " " + building;
			historyName = MessageFormat.format(getString(R.string.search_history_building), building, street,
					cityName);
			zoom = 17;
		} else if (!Algorithms.isEmpty(street)) {
			String cityName = postcode != null ? postcode : city;
			objectName = street;
			historyName = MessageFormat.format(getString(R.string.search_history_street), street, cityName);
			zoom = 16;
		} else if (!Algorithms.isEmpty(city)) {
			historyName = MessageFormat.format(getString(R.string.search_history_city), city);
			objectName = city;
			zoom = 14;
		}
		if(selectAddressMode){
			Intent intent = getActivity().getIntent();
			intent.putExtra(SELECT_ADDRESS_POINT_INTENT_KEY, objectName);
			intent.putExtra(SELECT_ADDRESS_POINT_LAT, searchPoint.getLatitude());
			intent.putExtra(SELECT_ADDRESS_POINT_LON, searchPoint.getLongitude());
			getActivity().setResult(SELECT_ADDRESS_POINT_RESULT_OK, intent);
			getActivity().finish();
		} else {
			if (navigateTo) {
				MapActivityActions.navigatePointDialogAndLaunchMap(getActivity(), searchPoint.getLatitude(), searchPoint.getLongitude(), historyName);
			} else {
				osmandSettings.setMapLocationToShow(searchPoint.getLatitude(), searchPoint.getLongitude(), zoom, historyName);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
			
		}
	}
	
	
	protected void updateBuildingSection(){
		if(radioBuilding){
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_building);
			if(Algorithms.isEmpty(building)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_building);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(building);
			}
		} else {
			((TextView)findViewById(R.id.BuildingText)).setText(R.string.search_address_street);
			if(Algorithms.isEmpty(street2)){
				((TextView)findViewById(R.id.BuildingButton)).setText(R.string.choose_intersected_street);
			} else {
				((TextView)findViewById(R.id.BuildingButton)).setText(street2);
			}
		}
		findViewById(R.id.ResetBuilding).setEnabled(!Algorithms.isEmpty(street2) || !Algorithms.isEmpty(building));
	}

	private View findViewById(int resId) {
		return view.findViewById(resId);
	}

	protected void updateUI(){
		showOnMap.setEnabled(searchPoint != null);
		navigateTo.setEnabled(searchPoint != null);
		if(selectAddressMode) {
			navigateTo.setText(R.string.search_select_point);
			showOnMap.setVisibility(View.INVISIBLE);
			findViewById(R.id.SearchOnline).setVisibility(View.INVISIBLE);
		} else {
			navigateTo.setText(R.string.navigate_to);
			findViewById(R.id.SearchOnline).setVisibility(View.VISIBLE);
			showOnMap.setVisibility(View.VISIBLE);
		}
		findViewById(R.id.ResetCountry).setEnabled(!Algorithms.isEmpty(region));
		if(Algorithms.isEmpty(region)){
			countryButton.setText(R.string.ChooseCountry);
		} else {
			countryButton.setText(region.replace('_', ' '));
		}
		findViewById(R.id.ResetCity).setEnabled(!Algorithms.isEmpty(city) || !Algorithms.isEmpty(postcode));
		if(Algorithms.isEmpty(city) && Algorithms.isEmpty(postcode)){
			cityButton.setText(R.string.choose_city);
		} else {
			if(!Algorithms.isEmpty(postcode)){
				cityButton.setText(postcode);
			} else {
				cityButton.setText(city.replace('_', ' '));
			}
		}
		cityButton.setEnabled(!Algorithms.isEmpty(region));
		
		findViewById(R.id.ResetStreet).setEnabled(!Algorithms.isEmpty(street));
		if(Algorithms.isEmpty(street)){
			streetButton.setText(R.string.choose_street);
		} else {
			streetButton.setText(street);
		}
		streetButton.setEnabled(!Algorithms.isEmpty(city) || !Algorithms.isEmpty(postcode));
		
		buildingButton.setEnabled(!Algorithms.isEmpty(street));
		((RadioGroup)findViewById(R.id.RadioGroup)).setVisibility(Algorithms.isEmpty(street) ? View.GONE : View.VISIBLE);
		
		if(radioBuilding){
			((RadioButton)findViewById(R.id.RadioBuilding)).setChecked(true);
		} else {
			((RadioButton)findViewById(R.id.RadioIntersStreet)).setChecked(true);
		}
		updateBuildingSection();
		
	}
	
	public void loadData() {
		if (!Algorithms.isEmpty(region)) {
			String postcodeStr = osmandSettings.getLastSearchedPostcode();
			if (!Algorithms.isEmpty(postcodeStr)) {
				postcode = postcodeStr;
			} else {
				city = osmandSettings.getLastSearchedCityName();
			}

			if (!Algorithms.isEmpty(postcode) || !Algorithms.isEmpty(city)) {
				street = osmandSettings.getLastSearchedStreet();
				if (!Algorithms.isEmpty(street)) {
					String str = osmandSettings.getLastSearchedIntersectedStreet();
					radioBuilding = Algorithms.isEmpty(str);
					if (!radioBuilding) {
						street2 = str;
					} else {
						building = osmandSettings.getLastSearchedBuilding();
					}
				}
			}
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		searchPoint = osmandSettings.getLastSearchedPoint();
		
		Intent intent = getActivity().getIntent();
		if (intent != null) {
			selectAddressMode = intent.hasExtra(SELECT_ADDRESS_POINT_INTENT_KEY);
		} else {
			selectAddressMode = false;
		}

		region = null;
		postcode = null;
		city = null;
		street = null;
		building = null;
		region = osmandSettings.getLastSearchedRegion();
		RegionAddressRepository reg = ((OsmandApplication)getApplication()).getResourceManager().getRegionRepository(region);
		if(reg != null && reg.useEnglishNames() != osmandSettings.USE_ENGLISH_NAMES.get()){
			reg.setUseEnglishNames(osmandSettings.USE_ENGLISH_NAMES.get());
		}
		loadData();
		updateUI();
		
	}

	
	
}
