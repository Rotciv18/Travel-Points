package com.rotciv.travelpoints.helper;

import android.util.Log;

public class VND {

    private int[] way;
    private double[][] adjacentMatrix;
    private double solution;
    private boolean returnToOrigin;

    public VND(int[] way, double[][] adjacentMatrix, double solution, boolean returnToOrigin) {
        this.way = way;
        this.adjacentMatrix = adjacentMatrix;
        this.solution = solution;
        this.returnToOrigin = returnToOrigin;

        this.neighborhoodSearch();
    }

    private void neighborhoodSearch () {

        while (true) {
            Log.d("VND", "Swap: " + solution);
            if (swap())
                continue;

            Log.d("VND", "Reinsertion: " + solution);
            if (reinsertion())
                continue;

            Log.d("VND", "2-Opt: " + solution);
            if (twoOpt())
                continue;

            break;
        }

    }

    private boolean swap () {

        int i, j, start, v1, v2, aux;
        double newSolution, bestSolution;
        boolean swaped = false;

        // "1" since 1st vertex can't be manipulated
        i = j = start = 1;
        v1 = v2 = 0;
        bestSolution = this.solution;
        newSolution = Double.MAX_VALUE;

        int size = returnToOrigin ? way.length - 2 : way.length - 1;

        while (i < size) {
            while (j < size) {

                if (i == j) {
                    j++;
                    continue;
                }

                if (j == i+1){ // Adjacent vertexes

                    newSolution = this.solution - adjacentMatrix[way[i-1]][way[i]] - adjacentMatrix[way[j]][way[j+1]]
                            + adjacentMatrix[way[i-1]][way[j]] + adjacentMatrix[way[i]][way[j+1]];

                } else {

                    newSolution = this.solution - adjacentMatrix[way[i-1]][way[i]] - adjacentMatrix[way[j]][way[j+1]]
                            + adjacentMatrix[way[i-1]][way[j]] + adjacentMatrix[way[i]][way[j+1]]
                            - adjacentMatrix[way[i]][way[i+1]] - adjacentMatrix[way[j-1]][way[j]]
                            + adjacentMatrix[way[j]][way[i+1]] + adjacentMatrix[way[j-1]][way[i]];

                }
                if (newSolution < solution) {
                    bestSolution = newSolution;
                    v1 = i;
                    v2 = j;
                }
                ++j;
            }
            j = ++start; // Same vertexes should not be tested twice!
            i++;
        }

        if (bestSolution < solution) {
            aux = way[v1];
            way[v1] = way[v2];
            way[v2] = aux;
            swaped = true;
            solution = bestSolution;
        }
        return swaped;
    }

    private boolean reinsertion() {

        int i, j, v1, v2, aux;
        double newSolution, bestSolution;
        boolean swaped = false;

        // "1" since 1st vertex can't be manipulated
        i = 1;
        j = 0;
        v1 = v2 = 0;
        bestSolution = this.solution;
        newSolution = Double.MAX_VALUE;

        int size = returnToOrigin ? way.length - 2 : way.length - 1;

        while (i < size) {
            while (j < size + 1) {
                if (i == j || j == i-1){
                    j++;
                    continue;
                }

                newSolution = solution - adjacentMatrix[way[i-1]][way[i]] - adjacentMatrix[way[i]][way[i+1]] - adjacentMatrix[way[j]][way[j+1]]
                        + adjacentMatrix[way[i-1]][way[i+1]] + adjacentMatrix[way[j]][way[i]] + adjacentMatrix[way[i]][way[j+1]];

                if (newSolution < bestSolution) {
                    bestSolution = newSolution;
                    v1 = i;
                    v2 = j;
                }
                j++;
            }
            j = 0;
            i++;
        }

        if (bestSolution < solution) {
            solution = bestSolution;
            aux = way[v1];

            if (v2 > v1) {
                for (i = v1; i <= v2; i++){
                    if (i == v2){
                        way[i] = aux;
                        break;
                    }
                    way[i] = way[i+1];
                }
            } else {
                for (i = v1; i >= v2; i--){
                    if (i-1 == v2){
                        way[i] = aux;
                        break;
                    }
                    way[i] = way[i-1];
                }
            }
            swaped = true;
        }

        return swaped;

    }

    private boolean twoOpt() {

        int i, j, v1, v2, aux;
        double newSolution, bestSolution;
        boolean swaped = false;

        // "1" since 1st vertex can't be manipulated
        i = j = 1;
        v1 = v2 = 0;
        bestSolution = this.solution;
        newSolution = Double.MAX_VALUE;

        int size = returnToOrigin ? way.length - 2 : way.length - 1;

        while (i < size - 1) {
            for (j = i+3 ; j < size + 1 ; j++) {

                newSolution = solution - adjacentMatrix[way[i-1]][way[i]] - adjacentMatrix[way[j+1]][way[j]]
                        + adjacentMatrix[way[j+1]][way[i]] + adjacentMatrix[way[i-1]][way[j]];
                if (newSolution < bestSolution){
                    bestSolution = newSolution;
                    v1 = i;
                    v2 = j;
                }
            }
            i++;
        }

        j = v2;
        if (bestSolution < solution) {
            solution = bestSolution;

            for (i = v1; i<j;i++) {
                aux = way[i];
                way[i] = way[j];
                way[j] = aux;
                j--;
            }

            swaped = true;
        }
        return swaped;
    }

    public int[] getWay() {
        return way;
    }

    public double getSolution() {
        return solution;
    }
}
