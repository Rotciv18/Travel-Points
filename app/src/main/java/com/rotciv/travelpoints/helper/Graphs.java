package com.rotciv.travelpoints.helper;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Graphs {

    public static double[][] mountAdjacencyMatrix(List<Location> locations) {

        int size = locations.size();
        double[][] adjacentMatrix = new double[size][size];

        for ( int i = 0 ; i < size ; i++ ) {

            for ( int j = 0 ; j < size ; j++ ) {

                double distance = Math.round(locations.get(i).distanceTo(locations.get(j)) * 10) / 10.0;
                adjacentMatrix[i][j] = distance;

            }

        }
        Log.d("mizera", adjacentMatrix.toString());
        return adjacentMatrix;
    }

    /*
    * Returns an array which represents a solution for the given graph
    * The array contains the way through n points, which can be interpreted as 'way[k] -> way[k+1]'
     * */
    public static int[] nearestNeightbor( double[][] adjacencyMatrix ) {

        int size = adjacencyMatrix[0].length;
        int[] way = new int[size];
        boolean[] visited = new boolean[size];

        for (int i = 0 ; i < size ; i++) {
            visited[i] = false;
        }
        visited[0] = true;
        way[0] = 0;

        for (int i = 0 ; i < size - 1 ; i++) {
            double lowestCost = Double.MAX_VALUE;
            int lowestCostIndex = 0;

            for (int j = 0 ; j < size ; j++ ) {

                if (way[i] == j) {
                    continue;
                }

                if ( adjacencyMatrix[way[i]][j] < lowestCost && !visited[j] ) {
                    lowestCost = adjacencyMatrix[way[i]][j];
                    lowestCostIndex = j;
                }
            }
            way[i+1] = lowestCostIndex;
            visited[lowestCostIndex] = true;
        }

        return way;
    }
}
