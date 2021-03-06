package com.example.RaceGoferApp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Brian on 11/2/2014.
 */
public class JoinableRacesFragment extends Fragment{
    View rootView;
    Button searchButton;
    private ListView mListView;
    private List<Map<String, String>> raceList = new ArrayList<Map<String, String>>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_joinableraces, container, false);

        searchButton = (Button)rootView.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRaces();
            }
        });

        mListView = (ListView) rootView.findViewById(R.id.listView);
        mListView.setAdapter(new SimpleAdapter(rootView.getContext(), raceList, android.R.layout.simple_list_item_1, new String[]{"race"}, new int[]{android.R.id.text1}));

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // selected item
                Map<String,String> item = (Map) mListView.getItemAtPosition(position);

                // Launching new Activity on selecting single List Item
                Intent i = new Intent(getActivity(), RacerViewActivity.class);
                // sending data to new activity
                i.putExtra("race", item.get("race"));
                i.putExtra("race_id", item.get("race_id"));
                startActivity(i);

                //TODO - join user into the race

            }
        });

        return rootView;
    }

    public void searchRaces(){
        //Get joinable races
        JSONArray races = new JSONArray();
        EditText search = (EditText)rootView.findViewById(R.id.searchText);
        races = getRaces(search.getText().toString());

        try{
            raceList.clear();
            for(int i=0;i<races.length();i++){
                JSONObject c = races.getJSONObject(i);
                String guid = c.getString("raceId");
                String name = c.getString("raceName");
                raceList.add(createRace(guid,name));
            }
        }
        catch (JSONException e){
            String err = (e.getMessage()==null)?"JSON Error":e.getMessage();
            Log.e("JSON Error", err);
        }

        mListView.setAdapter(new SimpleAdapter(rootView.getContext(), raceList, android.R.layout.simple_list_item_1, new String[]{"race"}, new int[]{android.R.id.text1}));
    }

    JSONArray getRaces(String search) {
        HttpConc http = new HttpConc(getActivity().getApplicationContext());
        String response;
        JSONArray obj = new JSONArray();

        try{
            URLParamEncoder encoder = new URLParamEncoder();
            response = http.sendGet("http://racegofer.com/api/GetRaces?search=" + encoder.encode(search));
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"GetRaces HTTP Error":e.getMessage();
            Log.e("GetRaces HTTP Error", err);
            Context context = getActivity().getApplicationContext();
            CharSequence text = "Could not retrieve races from server.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return obj;
        }

        try {
            obj = new JSONArray(response);
        }
        catch (Exception e){
            String err = (e.getMessage()==null)?"GetRaces JSON Error":e.getMessage();
            Log.e("GetRaces JSON Error", err);
            return obj;
        }

        return obj;
    }

    private HashMap<String,String> createRace(String guid, String racename){
        HashMap<String,String> race = new HashMap<String,String>();
        race.put("race", racename);
        race.put("race_id", guid);
        return race;
    }
}