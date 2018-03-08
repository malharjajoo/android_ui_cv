package com.beraldo.hpe;

/**
 * Created by malhar on 2/17/18.
 */

public class NoiseTracker
{
    public class NoiseData
    {
        public int noise; // noise level in dB

        public int getNoiseLevel()
        {
            return noise;
        }
    }

    public NoiseData getData()
    {
        NoiseData noiseData = new NoiseData();

        // TODO: get noise level from sensor
        noiseData.noise = 20;

        return noiseData;
    }



}
