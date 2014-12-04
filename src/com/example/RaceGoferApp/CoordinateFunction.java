package com.example.RaceGoferApp;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Hamdan on 11/28/2014.
 */
public class CoordinateFunction {

  private double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }

    //Function to calculate distance between two points
    double Distance(double lat1, double lon1, double lat2, double lon2)
    {

        int R = 3959; // Radius of the earth in miles
        double dLat = deg2rad(lat2-lat1);  // deg2rad below
        double dLon = deg2rad(lon2-lon1);
         double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c; // Distance in miles
        return d *5280;  //converting it into feets
    }

    //Function to calculate midpoint between two points
 private Coordinate MidPoint(double lat1,double lon1,double lat2,double lon2)

  {

        double dLon = Math.toRadians(lon2 - lon1);

        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        //print out in degrees
       return new Coordinate(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    //Function to recursively get mid points between two points separated by the variable Range
    Queue<Coordinate> GetRecursiveMidPoint(Coordinate C1, Coordinate C2)
    {

        Queue<Coordinate> MP = new LinkedList<Coordinate>();  //A queue to keep all midpoints
        Coordinate FirstTempC1 = C1;
        Coordinate FirstTempC2 = C2;
        Coordinate SecondTempC1 = C1;
        Coordinate SecondTempC2 = C2;
        Coordinate Mid = new Coordinate();
        float Range = 10; // Divide the line into midpoints at a distance of "Range"

        while(Distance(FirstTempC1.GetLat(), FirstTempC1.GetLon(), FirstTempC2.GetLat(), FirstTempC2.GetLon()) > Range)
        {  Mid = MidPoint(FirstTempC1.GetLat(), FirstTempC1.GetLon(), FirstTempC2.GetLat(), FirstTempC2.GetLon());
            MP.add(Mid);
           FirstTempC2 = Mid;
        }

        while(Distance(SecondTempC1.GetLat(), SecondTempC1.GetLon(), SecondTempC2.GetLat(), SecondTempC2.GetLon()) > Range)
        {  Mid = MidPoint(SecondTempC1.GetLat(), SecondTempC1.GetLon(), SecondTempC2.GetLat(), SecondTempC2.GetLon());
            MP.add(Mid);
            SecondTempC1 = Mid;
        }

        return MP;
    }

// Function to determine if user is in range of the line formed by checkpoints CP1 and CP2
    boolean IsUserInRange (Coordinate User, Coordinate CP1, Coordinate CP2)
    {
        boolean UserInRange = false;
        float Range = 300;   //A range of 300 feet

        Queue<Coordinate> Points = new LinkedList<Coordinate>(); //Queue to store all points between coordinates CP1 and CP2
        Points = GetRecursiveMidPoint(CP1, CP2);
        int size = Points.size();

        for (int i = 0 ; i < size; i++)
        {
            Coordinate Temp = Points.remove();


            double Distance  = Distance(User.GetLat(), User.GetLon(), Temp.GetLat(), Temp.GetLon() );
            if (Distance < Range)
            {
                UserInRange = true;
                return  UserInRange;
            }

        }

        return  UserInRange;

    }

   boolean RacerInRange(Coordinate Racer,Queue<Coordinate> _CheckPoints )
   {
       Queue<Coordinate> CheckPoints = new LinkedList<Coordinate>();
       CheckPoints.addAll(_CheckPoints);
       boolean RacerInRange = false;   //Initialize it to false

       int size = CheckPoints.size();

       if (size == 0) {return  true;} //CASE 1: No CheckPoints

       else if (size == 1)  //CASE 2: Only One CheckPoint
       {

           Coordinate Temp = new Coordinate();
            Temp = CheckPoints.remove();

           if (Distance(Racer.GetLat(), Racer.GetLon(), Temp.GetLat(), Temp.GetLon()) < 100)
           {
               RacerInRange = true;
               return  RacerInRange;
           }
       }

    else if (size > 1)  //CASE 3: More than one Checkpoint
       {
           Coordinate C1 = new Coordinate();
           C1 = CheckPoints.remove();

           while (!CheckPoints.isEmpty())
           {

               Coordinate C2 = new Coordinate();
               C2 = CheckPoints.remove();

            if (IsUserInRange(Racer, C1, C2) == true)
            {
                RacerInRange = true;
                return  RacerInRange;
            }

           C1 = new Coordinate(C2.GetLat(), C2.GetLon()) ;

           }

       }

       return  RacerInRange;
   }

}
