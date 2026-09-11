package com.gildedgames.the_aether.world.util;

import java.util.Random;

public class RandomTracker {

    public int lastRand = -1;

    public int testRandom(Random random, int bound) {
        // Roll until the value differs from the previous roll. The old
        // implementation recursed without using the result, so a collision
        // always returned -1 (and consumed an extra draw per retry), which
        // also made the "not the same as last" intent ineffective.
        int inputRandom;

        do {
            inputRandom = random.nextInt(bound);
        } while (inputRandom == this.lastRand);

        this.lastRand = inputRandom;

        return inputRandom;
    }
}
