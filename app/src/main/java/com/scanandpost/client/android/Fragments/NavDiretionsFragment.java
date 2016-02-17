package com.scanandpost.client.android.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.scanandpost.client.android.R;
import com.scanandpost.client.functions.GPSTracker;


/**
 * Created by Neha on 1/31/2016.
 */
public class NavDiretionsFragment extends Fragment {

    GoogleMap mGoogleMap;
    SupportMapFragment fragment;
    Double FromLat, FromLng;
    GPSTracker gps;


    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.nav_directions, container, false);
        gps = new GPSTracker(getActivity());
        FromLat = gps.getLatitude();
        FromLng = gps.getLongitude();
        initialise();
        return rootView;
    }

    private void initialise() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        fm = getChildFragmentManager();
        fragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        if (fragment == null) {
            fragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map, fragment).commit();
        }

        if (mGoogleMap == null) {
            mGoogleMap = fragment.getMap();
        }

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.getUiSettings().setAllGesturesEnabled(true);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(4));

        LatLng markerLoc=new LatLng(FromLat,FromLng);
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(markerLoc)      // Sets the center of the map to Mountain View
                .zoom(17)                   // Sets the zoom// Sets the tilt of the camera to 30 degrees
                .build();                   //
        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(FromLat, FromLng)).title("Source")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        Log.e("map is added", "map is added");

    }
}
