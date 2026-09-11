package com.gildedgames.the_aether.client.gui.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.lwjgl.opengl.GL11;

import com.gildedgames.the_aether.network.AetherNetwork;
import com.gildedgames.the_aether.network.packets.PacketSetTime;
import com.gildedgames.the_aether.world.AetherWorldProvider;

public class GuiSunAltarSlider extends GuiButton {

    public float sliderValue;

    public boolean dragging = false;

    private World world;

    /**
     * While non-negative, the slider holds this value (rather than re-reading
     * the synced sky time) until the server's PacketSendTime confirms the
     * change, preventing a one-frame rebound to the old position after the
     * mouse is released.
     */
    private float pendingValue = -1.0F;

    public GuiSunAltarSlider(World world, int par2, int par3, String par5Str) {
        super(1, par2, par3, 150, 20, par5Str);

        this.world = world;
    }

    /**
     * The time-of-day (0..24000) that the slider should display. Reads the
     * Aether world provider's synced aetherTime — the authoritative sky time —
     * rather than the raw WorldInfo time, which does not track the Aether's
     * custom sky clock.
     */
    private long currentAetherTime() {
        WorldProvider provider = this.world.provider;

        if (provider instanceof AetherWorldProvider) {
            return ((AetherWorldProvider) provider).getAetherTime() % 24000L;
        }

        return this.world.getWorldInfo()
            .getWorldTime() % 24000L;
    }

    /**
     * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over this button and 2 if it IS hovering over
     * this button.
     */
    public int getHoverState(boolean par1) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.dragging) {
                this.sliderValue = (float) (mouseX - (this.xPosition + 4)) / (float) (this.width - 8);

                if (this.sliderValue < 0.0F) {
                    this.sliderValue = 0.0F;
                }

                if (this.sliderValue > 1.0F) {
                    this.sliderValue = 1.0F;
                }
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(
                this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)),
                this.yPosition,
                0,
                66,
                4,
                20);
            this.drawTexturedModalRect(
                this.xPosition + (int) (this.sliderValue * (float) (this.width - 8)) + 4,
                this.yPosition,
                196,
                66,
                4,
                20);
        }
    }

    @Override
    public void drawButton(Minecraft par1Minecraft, int mouseX, int mouseY) {
        if (!this.dragging) {
            float synced = this.currentAetherTime() / 24000.0F;

            if (this.pendingValue >= 0.0F) {
                // Hold the released position until the server's time catches
                // up to within one tick, then hand over to the synced value.
                if (Math.abs(synced - this.pendingValue) < (2.0F / 24000.0F)) {
                    this.pendingValue = -1.0F;
                } else {
                    this.sliderValue = this.pendingValue;
                    super.drawButton(par1Minecraft, mouseX, mouseY);

                    return;
                }
            }

            this.sliderValue = synced;
        }

        super.drawButton(par1Minecraft, mouseX, mouseY);
    }

    @Override
    public boolean mousePressed(Minecraft par1Minecraft, int par2, int par3) {
        if (super.mousePressed(par1Minecraft, par2, par3)) {
            this.sliderValue = (float) (par2 - (this.xPosition + 4)) / (float) (this.width - 8);

            if (this.sliderValue < 0.0F) {
                this.sliderValue = 0.0F;
            }

            if (this.sliderValue > 1.0F) {
                this.sliderValue = 1.0F;
            }

            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;

        // Hold the knob at the released position until the server's synced
        // time catches up, so it doesn't briefly rebound to the old time.
        this.pendingValue = this.sliderValue;

        AetherNetwork.sendToServer(new PacketSetTime(this.sliderValue, Minecraft.getMinecraft().thePlayer.dimension));
    }

}
