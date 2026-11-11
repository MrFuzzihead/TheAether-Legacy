package com.gildedgames.the_aether.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.gildedgames.the_aether.world.util.RandomTracker;

public class UtilityTest {

    @Test
    public void filledListStartsWithTheFillElementAndKeepsItsSize() {
        FilledList<String> list = new FilledList<String>(3, "empty");

        assertEquals(3, list.size());
        assertSame("empty", list.get(0));
        assertSame("empty", list.get(2));
    }

    @Test
    public void filledListSetAndClearWork() {
        FilledList<String> list = new FilledList<String>(2, "empty");

        list.set(1, "value");
        assertEquals("value", list.get(1));

        list.clear();
        assertSame("empty", list.get(0));
        assertSame("empty", list.get(1));
    }

    @Test
    public void randomTrackerNeverRepeatsThePreviousRoll() {
        RandomTracker tracker = new RandomTracker();
        java.util.Random random = new java.util.Random(1234L);
        int previous = -1;

        for (int i = 0; i < 100; i++) {
            int value = tracker.testRandom(random, 10);

            assertEquals(value, tracker.lastRand);
            org.junit.Assert.assertTrue("value must be in range", value >= 0 && value < 10);
            org.junit.Assert.assertNotEquals("consecutive rolls must differ", previous, value);

            previous = value;
        }
    }

}
