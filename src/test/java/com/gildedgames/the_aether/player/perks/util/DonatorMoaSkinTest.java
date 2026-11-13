package com.gildedgames.the_aether.player.perks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DonatorMoaSkinTest {

    @Test
    public void defaultsToTheVanillaSkin() {
        DonatorMoaSkin skin = new DonatorMoaSkin();

        assertEquals(0, skin.getWingColor());
        assertEquals(0, skin.getWingMarkingColor());
        assertEquals(0, skin.getBodyColor());
        assertEquals(0, skin.getMarkingColor());
        assertEquals(0, skin.getEyeColor());
        assertEquals(0, skin.getOutsideColor());
        assertTrue(skin.shouldUseDefualt());
    }

    @Test
    public void serializesAllColorsAndTheDefaultFlag() {
        DonatorMoaSkin original = new DonatorMoaSkin();
        original.shouldUseDefualt(false);
        original.setWingColor(0x112233);
        original.setWingMarkingColor(0x445566);
        original.setBodyColor(0x778899);
        original.setMarkingColor(0xAABBCC);
        original.setEyeColor(0xDDEEFF);
        original.setOutsideColor(0x010203);

        ByteBuf buffer = Unpooled.buffer();
        original.writeMoaSkin(buffer);
        DonatorMoaSkin decoded = DonatorMoaSkin.readMoaSkin(buffer);

        assertFalse(decoded.shouldUseDefualt());
        assertEquals(0x112233, decoded.getWingColor());
        assertEquals(0x445566, decoded.getWingMarkingColor());
        assertEquals(0x778899, decoded.getBodyColor());
        assertEquals(0xAABBCC, decoded.getMarkingColor());
        assertEquals(0xDDEEFF, decoded.getEyeColor());
        assertEquals(0x010203, decoded.getOutsideColor());
        assertEquals(0, buffer.readableBytes());
    }
}
