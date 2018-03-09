package com.beraldo.hpe;

/**
 * Created by malhar on 2/13/18.
 */



public class GazeTracker
{

    public enum GazeState
    {
        PRESENT_FOCUSSED, PRESENT_DISTRACTED, NOT_PRESENT;
    }

    // This will return a GazeTracker object
    public GazeState getData(int i)
    {
        GazeState gazeState;

        // some kind of logic to return different data.
        if( i%2 == 0)
        {
            gazeState = GazeState.PRESENT_FOCUSSED;
        }else if( i%3 == 0)
        {
            gazeState = GazeState.PRESENT_DISTRACTED;
        }
        else
        {
            gazeState = GazeState.NOT_PRESENT;
        }

        // fills in the gaze data.
        return gazeState;
    }
}
