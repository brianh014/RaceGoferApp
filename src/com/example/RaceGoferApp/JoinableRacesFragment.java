package com.example.RaceGoferApp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Brian on 11/2/2014.
 */
public class JoinableRacesFragment extends Fragment {
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_joinableraces, container, false);

        //Get races user is in
        //Currently populate list with dummy races
        String[] races = {"Joinable Race","Another Joinable Race"};

        mListView = (ListView) rootView.findViewById(R.id.listView);
        mListView.setAdapter(new ArrayAdapter<String>(rootView.getContext(), android.R.layout.simple_list_item_1, races));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // selected item
                String race = ((TextView) view).getText().toString();

                // Launching new Activity on selecting single List Item
                Intent i = new Intent(getActivity(), RacerViewActivity.class);
                // sending data to new activity
                i.putExtra("race", race);
                i.putExtra("race_id", "1307dc6a-6200-4803-90ba-cb080337a1f1");
                startActivity(i);

            }
        });

        return rootView;
    }
}