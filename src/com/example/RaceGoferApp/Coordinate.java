package com.example.RaceGoferApp;

/**
 * Created by Hamdan on 11/29/2014.
 */
public class Coordinate {

    double latitude;
    double longitude;

   Coordinate()
   {latitude = 0.0;
    longitude = 0.0;
   }

    Coordinate(double lat, double lon)
    {
        latitude = lat;
        longitude = lon;
    }

   double GetLat() {return  latitude;}

   double GetLon() {return  longitude;}

    void SetLat(double lat) {latitude = lat;}

    void SetLon(double lon) {longitude = lon;}
}
