package com.gildedgames.the_aether.network.packets;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketRoundTripTest {

    @Test
    public void setTimePacketRoundTrips() {
        ByteBuf buffer = Unpooled.buffer();
        PacketSetTime original = new PacketSetTime(0.25F, 4);

        original.toBytes(buffer);

        PacketSetTime decoded = new PacketSetTime();
        decoded.fromBytes(buffer);

        assertEquals(0.25F, decoded.timeVariable, 0.00001F);
        assertEquals(4, decoded.dimensionId);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void dialoguePacketRoundTrips() {
        ByteBuf buffer = Unpooled.buffer();
        PacketDisplayDialogue original = new PacketDisplayDialogue(
            "test_dialogue",
            "A test dialogue",
            new ArrayList<String>(Arrays.asList("first option", "second option")));

        original.toBytes(buffer);

        PacketDisplayDialogue decoded = new PacketDisplayDialogue();
        decoded.fromBytes(buffer);

        assertEquals("test_dialogue", decoded.dialogueName);
        assertEquals("A test dialogue", decoded.dialogue);
        assertEquals(2, decoded.dialogueText.size());
        assertEquals("first option", decoded.dialogueText.get(0));
        assertEquals("second option", decoded.dialogueText.get(1));
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void swetJumpPacketRoundTrips() {
        ByteBuf buffer = Unpooled.buffer();
        PacketSwetJump original = new PacketSwetJump(17, true);

        original.toBytes(buffer);

        PacketSwetJump decoded = new PacketSwetJump();
        decoded.fromBytes(buffer);

        assertEquals(17, decoded.id);
        assertEquals(true, decoded.bool);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void timeDeltaMovesForwardToTheRequestedTime() {
        assertEquals(6000L, PacketSetTime.calculateTimeDelta(0.25F, 0L));
        assertEquals(24000L, PacketSetTime.calculateTimeDelta(0.25F, 6000L));
        assertEquals(24000L, PacketSetTime.calculateTimeDelta(0.0F, 0L));
    }

    @Test
    public void valkyrieFightPacketRoundTrips() {
        ByteBuf buffer = Unpooled.buffer();
        PacketInitiateValkyrieFight original = new PacketInitiateValkyrieFight(3, 42);

        original.toBytes(buffer);

        PacketInitiateValkyrieFight decoded = new PacketInitiateValkyrieFight();
        decoded.fromBytes(buffer);

        assertEquals(3, decoded.slotId);
        assertEquals(42, decoded.entityId);
        assertEquals(0, buffer.readableBytes());
    }
}
