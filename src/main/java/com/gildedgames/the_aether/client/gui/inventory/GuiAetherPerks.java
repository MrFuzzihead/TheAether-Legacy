package com.gildedgames.the_aether.client.gui.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.gildedgames.the_aether.entities.passive.mountable.EntityMoa;
import com.gildedgames.the_aether.entities.util.AetherMoaTypes;
import com.gildedgames.the_aether.network.AetherNetwork;
import com.gildedgames.the_aether.network.packets.PacketPerkChanged;
import com.gildedgames.the_aether.player.PlayerAether;
import com.gildedgames.the_aether.player.perks.AetherRankings;
import com.gildedgames.the_aether.player.perks.util.DonatorMoaSkin;
import com.gildedgames.the_aether.player.perks.util.EnumAetherPerkType;

public class GuiAetherPerks extends GuiScreen {

    private boolean enableMoaEditor, enableHaloEditor;

    private PlayerAether player;

    private EntityMoa moa;

    public GuiButton perkMoa, perkHalo;

    private GuiButton defualtSkin, enableHalo, confirmPreference;

    private GuiTextField moaWingMarking, moaWing, moaBody, moaBodyMarking, moaEye, moaOutside;

    private DonatorMoaSkin moaSkin = new DonatorMoaSkin();

    protected int guiLeft;

    protected int guiTop;

    public GuiAetherPerks() {
        super();

        this.mc = Minecraft.getMinecraft();
        this.player = PlayerAether.get(this.mc.thePlayer);
        this.moaSkin = this.player.donatorMoaSkin;

        this.moa = this.player.getEntity().ridingEntity instanceof EntityMoa
            ? (EntityMoa) this.player.getEntity().ridingEntity
            : new EntityMoa(this.mc.theWorld, AetherMoaTypes.blue);
    }

    @Override
    public void updateScreen() {
        this.moaWingMarking.updateCursorCounter();
        this.moaWing.updateCursorCounter();
        this.moaBody.updateCursorCounter();
        this.moaBodyMarking.updateCursorCounter();
        this.moaEye.updateCursorCounter();
        this.moaOutside.updateCursorCounter();

        if (!this.moaWingMarking.getText()
            .isEmpty()) {
            this.moaSkin.setWingMarkingColor(Integer.decode("0x" + this.moaWingMarking.getText()));
        }

        if (!this.moaWing.getText()
            .isEmpty()) {
            this.moaSkin.setWingColor(Integer.decode("0x" + this.moaWing.getText()));
        }

        if (!this.moaBody.getText()
            .isEmpty()) {
            this.moaSkin.setBodyColor(Integer.decode("0x" + this.moaBody.getText()));
        }

        if (!this.moaBodyMarking.getText()
            .isEmpty()) {
            this.moaSkin.setMarkingColor(Integer.decode("0x" + this.moaBodyMarking.getText()));
        }

        if (!this.moaEye.getText()
            .isEmpty()) {
            this.moaSkin.setEyeColor(Integer.decode("0x" + this.moaEye.getText()));
        }

        if (!this.moaOutside.getText()
            .isEmpty()) {
            this.moaSkin.setOutsideColor(Integer.decode("0x" + this.moaOutside.getText()));
        }

        this.moaSkin.shouldUseDefualt(this.defualtSkin.displayString.contains("true"));

        if (!this.enableHaloEditor) {
            this.enableHalo.visible = false;
        }

        if (!this.enableMoaEditor) {
            this.defualtSkin.visible = false;
            this.confirmPreference.visible = false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        this.guiLeft = (this.width - 176) / 2;
        this.guiTop = (this.height - 166) / 2;

        this.buttonList.add(this.perkMoa = new GuiButton(1, 4, 17, 100, 20, I18n.format("gui.perks.moa_customizer")));
        this.buttonList
            .add(this.perkHalo = new GuiButton(5, 110, 17, 100, 20, I18n.format("gui.perks.developer_perks")));

        this.buttonList.add(
            this.enableHalo = new GuiButton(
                6,
                this.width / 2 - 50,
                this.height - 20,
                100,
                20,
                I18n.format("gui.perks.enable_halo") + " "));
        this.buttonList.add(
            this.defualtSkin = new GuiButton(
                2,
                this.width / 2 - 100,
                this.height - 20,
                100,
                20,
                I18n.format("gui.perks.default_skin") + " " + this.player.donatorMoaSkin.shouldUseDefualt()));
        this.buttonList.add(
            this.confirmPreference = new GuiButton(
                4,
                this.width / 2,
                this.height - 20,
                100,
                20,
                I18n.format("gui.perks.confirm_skin")));

        if (!AetherRankings.isRankedPlayer(
            this.player.getEntity()
                .getUniqueID())) {
            this.perkHalo.visible = false;
        }

        this.enableHalo.displayString = this.enableHalo.displayString
            + Boolean.toString(this.player.shouldRenderHalo ? false : true);

        this.moaEye = new GuiTextField(this.fontRendererObj, (this.width / 2) + 105, 70, 45, 20);
        this.moaEye.setMaxStringLength(6);
        this.moaEye.setText(Integer.toHexString(this.player.donatorMoaSkin.getEyeColor()));

        this.moaWingMarking = new GuiTextField(this.fontRendererObj, (this.width / 2) + 105, this.height - 40, 45, 20);
        this.moaWingMarking.setMaxStringLength(6);
        this.moaWingMarking.setText(Integer.toHexString(this.player.donatorMoaSkin.getWingMarkingColor()));

        this.moaWing = new GuiTextField(this.fontRendererObj, (this.width / 2) + 105, this.height / 2 + 15, 45, 20);
        this.moaWing.setMaxStringLength(6);
        this.moaWing.setText(Integer.toHexString(this.player.donatorMoaSkin.getWingColor()));

        this.moaBody = new GuiTextField(this.fontRendererObj, (this.width / 2) - 155, this.height / 2 + 15, 45, 20);
        this.moaBody.setMaxStringLength(6);
        this.moaBody.setText(Integer.toHexString(this.player.donatorMoaSkin.getBodyColor()));

        this.moaBodyMarking = new GuiTextField(this.fontRendererObj, (this.width / 2) - 155, this.height - 40, 45, 20);
        this.moaBodyMarking.setMaxStringLength(6);
        this.moaBodyMarking.setText(Integer.toHexString(this.player.donatorMoaSkin.getMarkingColor()));

        this.moaOutside = new GuiTextField(this.fontRendererObj, (this.width / 2) - 155, 70, 45, 20);
        this.moaOutside.setMaxStringLength(6);
        this.moaOutside.setText(Integer.toHexString(this.player.donatorMoaSkin.getOutsideColor()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        if (this.enableMoaEditor) {
            if (keyCode > 1 && keyCode < 12 || keyCode == 14
                || keyCode == 30
                || keyCode == 48
                || keyCode == 46
                || keyCode == 32
                || keyCode == 18
                || keyCode == 33) {
                this.moaWingMarking.textboxKeyTyped(typedChar, keyCode);
                this.moaWing.textboxKeyTyped(typedChar, keyCode);
                this.moaBody.textboxKeyTyped(typedChar, keyCode);
                this.moaBodyMarking.textboxKeyTyped(typedChar, keyCode);
                this.moaEye.textboxKeyTyped(typedChar, keyCode);
                this.moaOutside.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (this.enableMoaEditor) {
            this.moaWingMarking.mouseClicked(mouseX, mouseY, mouseButton);
            this.moaWing.mouseClicked(mouseX, mouseY, mouseButton);
            this.moaBody.mouseClicked(mouseX, mouseY, mouseButton);
            this.moaBodyMarking.mouseClicked(mouseX, mouseY, mouseButton);
            this.moaEye.mouseClicked(mouseX, mouseY, mouseButton);
            this.moaOutside.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        AetherNetwork.sendToServer(
            new PacketPerkChanged(
                this.player.getEntity()
                    .getEntityId(),
                EnumAetherPerkType.Moa,
                this.moaSkin));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            boolean shouldEnable = this.enableMoaEditor ? false : true;

            this.enableMoaEditor = shouldEnable;
            this.enableHaloEditor = false;
            this.enableHalo.visible = false;
            this.confirmPreference.visible = true;
            this.defualtSkin.visible = true;
        } else if (button.id == 2) {
            boolean enableDefualt = this.player.donatorMoaSkin.shouldUseDefualt() ? false : true;

            this.defualtSkin.displayString = I18n.format("gui.perks.use_default") + " "
                + Boolean.toString(enableDefualt);
            this.player.donatorMoaSkin.shouldUseDefualt(enableDefualt);
        } else if (button.id == 4) {
            AetherNetwork.sendToServer(
                new PacketPerkChanged(
                    this.player.getEntity()
                        .getEntityId(),
                    EnumAetherPerkType.Moa,
                    this.moaSkin));
        } else if (button.id == 5) {
            boolean shouldEnable = this.enableHaloEditor ? false : true;

            this.enableHaloEditor = shouldEnable;
            this.enableMoaEditor = false;
            this.enableHalo.visible = true;
        } else if (button.id == 6) {
            boolean enableHalo = this.player.shouldRenderHalo ? false : true;

            this.player.shouldRenderHalo = enableHalo;
            AetherNetwork.sendToServer(
                new PacketPerkChanged(
                    this.player.getEntity()
                        .getEntityId(),
                    EnumAetherPerkType.Halo,
                    enableHalo));
            this.enableHalo.displayString = I18n.format("gui.perks.enable_halo") + " " + Boolean.toString(enableHalo);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.drawCenteredString(this.fontRendererObj, I18n.format("gui.perks.donator_options"), 45, 4, 16777215);

        if (this.enableMoaEditor) {
            GuiInventory.func_147046_a(
                this.width / 2,
                this.height / 2 + (this.height / 2) - 30,
                (this.height / 3) - 20,
                (float) (this.guiLeft + 51) - (float) mouseX,
                (float) (75 - 50) - (float) mouseY,
                this.moa);

            /* Left Side */

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.leg_beak_color"),
                (this.width / 2) - 170,
                55,
                16777215);

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.body_color"),
                (this.width / 2) - 160,
                this.height / 2,
                16777215);

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.body_marking_color"),
                (this.width / 2) - 180,
                this.height - 55,
                16777215);

            /* Right Side */

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.eye_color"),
                (this.width / 2) + 104,
                55,
                16777215);

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.wing_color"),
                (this.width / 2) + 102,
                this.height / 2,
                16777215);

            this.drawString(
                this.fontRendererObj,
                I18n.format("gui.perks.wing_marking_color"),
                (this.width / 2) + 82,
                this.height - 55,
                16777215);

            this.moaWingMarking.drawTextBox();
            this.moaWing.drawTextBox();
            this.moaBody.drawTextBox();
            this.moaBodyMarking.drawTextBox();
            this.moaEye.drawTextBox();
            this.moaOutside.drawTextBox();
        }

        if (this.enableHaloEditor) {
            GuiInventory.func_147046_a(
                this.width / 2,
                this.height / 2 + (this.height / 2) - 30,
                (this.height / 3) - 20,
                (float) (this.guiLeft + 51) - (float) mouseX,
                (float) (75 - 50) - (float) mouseY,
                this.player.getEntity());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawWorldBackground(int tint) {
        this.drawGradientRect(0, 0, this.width, 50, -1072689136, -804253680);

        if (this.mc.theWorld != null) {
            this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        } else {
            this.drawBackground(tint);
        }
    }

}
