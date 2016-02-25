package com.scanandpost.client.android.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.scanandpost.client.android.R;
import com.scanandpost.client.functions.Constants;

import java.util.ArrayList;



/**
 * Created by Neha on 1/31/2016.
 */
public class NavDeliverFragment extends Fragment implements View.OnClickListener  {
    View rootView;
    Spinner deliver_spinner;
    MyAdapter mAdapter;
    String TAG = "NavDeliverFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.nav_deliver, container, false);
        initialise();
        return rootView;
    }

    private void initialise() {
        deliver_spinner = (Spinner) rootView.findViewById(R.id.deliver_spinner);


        ArrayList<String> menuItems = new ArrayList<String>();
        menuItems.add("Delivered to customer");
        menuItems.add("Other");
        menuItems.add("Refused by customer");
        menuItems.add("Partial Damaged");
        menuItems.add("Total Damage");

        mAdapter = new MyAdapter(getActivity(),
                menuItems);
        deliver_spinner.setAdapter(mAdapter);

        deliver_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                /*Deliver to customer :: 42
                other :: 28
                refussed by customer :: 5
                partial damaged :: 44
                total damage :: 43*/

                if (position == 0) {
                    Constants.ACTIVITY_ID = "42";
                    Log.e(TAG, "activity id=> " + Constants.ACTIVITY_ID);

                } else if (position == 1) {
                    Constants.ACTIVITY_ID = "28";
                    Log.e(TAG, "activity id=> " + Constants.ACTIVITY_ID);
                } else if (position == 2) {
                    Constants.ACTIVITY_ID = "5";
                    Log.e(TAG, "activity id=> " + Constants.ACTIVITY_ID);
                } else if (position == 3) {
                    Constants.ACTIVITY_ID = "44";
                    Log.e(TAG, "activity id=> " + Constants.ACTIVITY_ID);
                }else if (position == 4) {
                    Constants.ACTIVITY_ID = "43";
                    Log.e(TAG, "activity id=> " + Constants.ACTIVITY_ID);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onClick(View v) {

    }


    class MyAdapter extends BaseAdapter {

        LayoutInflater mInflater = null;

        ArrayList<String> menuItems = new ArrayList<String>();

        public MyAdapter(Context context,
                         ArrayList<String> menuList) {
            mInflater = LayoutInflater.from(getActivity());
            menuItems = menuList;

        }


        @Override
        public int getCount() {

            return menuItems.size();
        }

        @Override
        public Object getItem(int position) {
            return menuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, R.layout.menu_spinner_item, true);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, R.layout.menu_spinner_item_dropdown, false);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent, int spinnerRow, boolean isDefaultRow) {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(spinnerRow, parent, false);
            TextView txt = (TextView) row.findViewById(R.id.text);

            txt.setText(menuItems.get(position));


            return row;
        }

    }
}

