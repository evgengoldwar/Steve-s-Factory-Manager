package vswe.stevesfactory.components;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import vswe.stevesfactory.CollisionHelper;
import vswe.stevesfactory.Localization;
import vswe.stevesfactory.interfaces.ContainerManager;
import vswe.stevesfactory.interfaces.GuiManager;
import vswe.stevesfactory.network.DataBitHelper;
import vswe.stevesfactory.network.DataReader;
import vswe.stevesfactory.network.DataWriter;
import vswe.stevesfactory.network.PacketHandler;

public abstract class ComponentMenuTarget extends ComponentMenu {

    public ComponentMenuTarget(FlowComponent parent) {
        super(parent);

        selectedDirectionId = -1;

    }

    // the sides are listed to the left, the settings of the selected side are shown to the right
    private static final int MENU_WIDTH = 200;
    private static final int MENU_HEIGHT = 74;
    private static final int DIRECTION_SIZE_W = 56;
    private static final int DIRECTION_SIZE_H = 14;
    private static final int DIRECTION_X_LEFT = 4;
    private static final int DIRECTION_X_RIGHT = 64;
    private static final int DIRECTION_Y = 4;
    private static final int DIRECTION_MARGIN = 6;

    private static final int SEPARATOR_X = 124;
    private static final int COLOR_SEPARATOR = 0xFF141517;
    protected static final int PANEL_X = 128;
    protected static final int PANEL_W = MENU_WIDTH - PANEL_X - 4;
    private static final int PANEL_TITLE_Y = 5;
    private static final int BUTTON_SIZE_H = 14;
    protected static final int FIRST_BUTTON_Y = 16;
    protected static final int SECOND_BUTTON_Y = 34;
    protected static final int ADVANCED_Y = 54;

    private Button[] buttons = { new Button(FIRST_BUTTON_Y) {

        @Override
        protected String getLabel() {
            return isActive(selectedDirectionId) ? Localization.DEACTIVATE.toString()
                    : Localization.ACTIVATE.toString();
        }

        @Override
        protected String getMouseOverText() {
            return isActive(selectedDirectionId) ? Localization.DEACTIVATE_LONG.toString()
                    : Localization.ACTIVATE_LONG.toString();
        }

        @Override
        protected void onClicked() {
            writeData(DataTypeHeader.ACTIVATE, isActive(selectedDirectionId) ? 0 : 1);
        }
    }, getSecondButton() };

    protected abstract Button getSecondButton();

    @Override
    public String getName() {
        return Localization.TARGET_MENU.toString();
    }

    public static ForgeDirection[] directions = ForgeDirection.VALID_DIRECTIONS;

    protected int selectedDirectionId;
    private boolean[] activatedDirections = new boolean[directions.length];
    private boolean[] useRangeForDirections = new boolean[directions.length];

    @SideOnly(Side.CLIENT)
    @Override
    public void draw(GuiManager gui, int mX, int mY) {
        for (int i = 0; i < directions.length; i++) {
            int x = getDirectionX(i);
            int y = getDirectionY(i);
            gui.drawToggleButton(
                    x,
                    y,
                    DIRECTION_SIZE_W,
                    DIRECTION_SIZE_H,
                    Localization.getForgeDirectionLocalization(i).toString(),
                    CollisionHelper.inBounds(x, y, DIRECTION_SIZE_W - 1, DIRECTION_SIZE_H - 1, mX, mY),
                    isActive(i),
                    selectedDirectionId == i);
        }

        gui.drawColoredRect(SEPARATOR_X, DIRECTION_Y, 1, MENU_HEIGHT - DIRECTION_Y * 2, COLOR_SEPARATOR);

        if (selectedDirectionId != -1) {
            gui.drawString(
                    Localization.getForgeDirectionLocalization(selectedDirectionId).toString(),
                    PANEL_X,
                    PANEL_TITLE_Y,
                    0.8F,
                    0xFFFFFF);

            for (Button button : buttons) {
                gui.drawMenuButton(
                        PANEL_X,
                        button.y,
                        PANEL_W,
                        BUTTON_SIZE_H,
                        button.getLabel(),
                        CollisionHelper.inBounds(PANEL_X, button.y, PANEL_W - 1, BUTTON_SIZE_H - 1, mX, mY),
                        false);
            }

            if (useAdvancedSetting(selectedDirectionId)) {
                drawAdvancedComponent(gui, mX, mY);
            }
        } else {
            gui.drawSplitString(Localization.SELECT_SIDE.toString(), PANEL_X, PANEL_TITLE_Y, PANEL_W, 0.7F, 0xA0A0A0);
        }
    }

    @Override
    public int getMenuWidth() {
        return MENU_WIDTH;
    }

    @Override
    public int getMenuHeight() {
        return MENU_HEIGHT;
    }

    public boolean isActive(int i) {
        return activatedDirections[i];
    }

    private int getDirectionX(int i) {
        return i % 2 == 0 ? DIRECTION_X_LEFT : DIRECTION_X_RIGHT;
    }

    public boolean useAdvancedSetting(int i) {
        return useRangeForDirections[i];
    }

    private int getDirectionY(int i) {
        return DIRECTION_Y + (DIRECTION_SIZE_H + DIRECTION_MARGIN) * (i / 2);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawMouseOver(GuiManager gui, int mX, int mY) {
        if (selectedDirectionId != -1) {
            for (Button button : buttons) {
                if (CollisionHelper.inBounds(PANEL_X, button.y, PANEL_W - 1, BUTTON_SIZE_H - 1, mX, mY)) {
                    gui.drawMouseOver(button.getMouseOverText(), mX, mY);
                }
            }
        }
    }

    @Override
    public void onClick(int mX, int mY, int button) {
        for (int i = 0; i < directions.length; i++) {
            if (CollisionHelper
                    .inBounds(getDirectionX(i), getDirectionY(i), DIRECTION_SIZE_W - 1, DIRECTION_SIZE_H - 1, mX, mY)) {
                if (selectedDirectionId == i) {
                    selectedDirectionId = -1;
                } else {
                    selectedDirectionId = i;
                    refreshAdvancedComponent();
                }

                break;
            }
        }

        if (selectedDirectionId != -1) {
            for (Button optionButton : buttons) {
                if (CollisionHelper.inBounds(PANEL_X, optionButton.y, PANEL_W - 1, BUTTON_SIZE_H - 1, mX, mY)) {
                    optionButton.onClicked();
                    break;
                }
            }

            if (useAdvancedSetting(selectedDirectionId)) {
                onAdvancedClick(mX, mY, button);
            }
        }
    }

    @Override
    public void onDrag(int mX, int mY, boolean isMenuOpen) {

    }

    @Override
    public void onRelease(int mX, int mY, boolean isMenuOpen) {

    }

    protected abstract class Button {

        private int y;

        protected Button(int y) {
            this.y = y;
        }

        protected abstract String getLabel();

        protected abstract String getMouseOverText();

        protected abstract void onClicked();
    }

    @Override
    public void writeData(DataWriter dw) {
        for (int i = 0; i < directions.length; i++) {
            dw.writeBoolean(isActive(i));
            dw.writeBoolean(useAdvancedSetting(i));
            if (useAdvancedSetting(i)) {
                writeAdvancedSetting(dw, i);
            }

        }
    }

    @SideOnly(Side.CLIENT)
    protected abstract void drawAdvancedComponent(GuiManager gui, int mX, int mY);

    protected abstract void refreshAdvancedComponent();

    protected abstract void writeAdvancedSetting(DataWriter dw, int i);

    protected abstract void readAdvancedSetting(DataReader dr, int i);

    protected abstract void copyAdvancedSetting(ComponentMenu menuTarget, int i);

    protected abstract void onAdvancedClick(int mX, int mY, int button);

    protected abstract void loadAdvancedComponent(NBTTagCompound directionTag, int i);

    protected abstract void saveAdvancedComponent(NBTTagCompound directionTag, int i);

    protected abstract void resetAdvancedSetting(int i);

    protected abstract void refreshAdvancedComponentData(ContainerManager container, ComponentMenu newData, int i);

    protected abstract void readAdvancedNetworkComponent(DataReader dr, DataTypeHeader header, int i);

    @Override
    public void readData(DataReader dr) {
        for (int i = 0; i < directions.length; i++) {

            activatedDirections[i] = dr.readBoolean();
            useRangeForDirections[i] = dr.readBoolean();
            if (useAdvancedSetting(i)) {
                readAdvancedSetting(dr, i);
            } else {
                resetAdvancedSetting(i);
            }

        }
    }

    @Override
    public void copyFrom(ComponentMenu menu) {
        ComponentMenuTarget menuTarget = (ComponentMenuTarget) menu;

        for (int i = 0; i < directions.length; i++) {
            activatedDirections[i] = menuTarget.activatedDirections[i];
            useRangeForDirections[i] = menuTarget.useRangeForDirections[i];
            copyAdvancedSetting(menu, i);
        }
    }

    @Override
    public void refreshData(ContainerManager container, ComponentMenu newData) {
        ComponentMenuTarget newDataTarget = (ComponentMenuTarget) newData;

        for (int i = 0; i < directions.length; i++) {
            if (activatedDirections[i] != newDataTarget.activatedDirections[i]) {
                activatedDirections[i] = newDataTarget.activatedDirections[i];

                writeUpdatedData(container, i, DataTypeHeader.ACTIVATE, activatedDirections[i] ? 1 : 0);
            }

            if (useRangeForDirections[i] != newDataTarget.useRangeForDirections[i]) {
                useRangeForDirections[i] = newDataTarget.useRangeForDirections[i];

                writeUpdatedData(container, i, DataTypeHeader.USE_ADVANCED_SETTING, useRangeForDirections[i] ? 1 : 0);
            }

            refreshAdvancedComponentData(container, newData, i);
        }
    }

    protected void writeUpdatedData(ContainerManager container, int id, DataTypeHeader header, int data) {
        DataWriter dw = getWriterForClientComponentPacket(container);
        writeData(dw, id, header, data);
        PacketHandler.sendDataToListeningClients(container, dw);
    }

    @Override
    public void readNetworkComponent(DataReader dr) {
        int direction = dr.readData(DataBitHelper.MENU_TARGET_DIRECTION_ID);
        int headerId = dr.readData(DataBitHelper.MENU_TARGET_TYPE_HEADER);
        DataTypeHeader header = getHeaderFromId(headerId);

        switch (header) {
            case ACTIVATE:
                activatedDirections[direction] = dr.readData(header.bits) != 0;
                break;
            case USE_ADVANCED_SETTING:
                useRangeForDirections[direction] = dr.readData(header.bits) != 0;
                if (!useAdvancedSetting(direction)) {
                    resetAdvancedSetting(direction);
                }
                break;
            default:
                readAdvancedNetworkComponent(dr, header, direction);
        }
    }

    protected void writeData(DataTypeHeader header, int data) {
        DataWriter dw = getWriterForServerComponentPacket();
        writeData(dw, selectedDirectionId, header, data);
        PacketHandler.sendDataToServer(dw);
    }

    private void writeData(DataWriter dw, int id, DataTypeHeader header, int data) {
        dw.writeData(id, DataBitHelper.MENU_TARGET_DIRECTION_ID);
        dw.writeData(header.id, DataBitHelper.MENU_TARGET_TYPE_HEADER);
        dw.writeData(data, header.bits);
    }

    protected enum DataTypeHeader {

        ACTIVATE(0, DataBitHelper.BOOLEAN),
        USE_ADVANCED_SETTING(1, DataBitHelper.BOOLEAN),
        START_OR_TANK_DATA(2, DataBitHelper.MENU_TARGET_RANGE),
        END(3, DataBitHelper.MENU_TARGET_RANGE);

        private int id;
        private DataBitHelper bits;

        private DataTypeHeader(int header, DataBitHelper bits) {
            this.id = header;
            this.bits = bits;
        }

        public int getId() {
            return id;
        }

        public DataBitHelper getBits() {
            return bits;
        }
    }

    private DataTypeHeader getHeaderFromId(int id) {
        for (DataTypeHeader header : DataTypeHeader.values()) {
            if (id == header.id) {
                return header;
            }
        }
        return null;
    }

    private static final String NBT_DIRECTIONS = "Directions";
    private static final String NBT_ACTIVE = "Active";
    private static final String NBT_RANGE = "UseRange";

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound, int version, boolean pickup) {
        NBTTagList directionTagList = nbtTagCompound.getTagList(NBT_DIRECTIONS, 10);

        for (int i = 0; i < directionTagList.tagCount(); i++) {
            NBTTagCompound directionTag = directionTagList.getCompoundTagAt(i);
            activatedDirections[i] = directionTag.getBoolean(NBT_ACTIVE);
            useRangeForDirections[i] = directionTag.getBoolean(NBT_RANGE);
            loadAdvancedComponent(directionTag, i);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound, boolean pickup) {
        NBTTagList directionTagList = new NBTTagList();

        for (int i = 0; i < directions.length; i++) {
            NBTTagCompound directionTag = new NBTTagCompound();
            directionTag.setBoolean(NBT_ACTIVE, isActive(i));
            directionTag.setBoolean(NBT_RANGE, useAdvancedSetting(i));
            saveAdvancedComponent(directionTag, i);
            directionTagList.appendTag(directionTag);
        }

        nbtTagCompound.setTag(NBT_DIRECTIONS, directionTagList);
    }

    @Override
    public void addErrors(List<String> errors) {
        for (int i = 0; i < directions.length; i++) {
            if (isActive(i)) {
                return;
            }
        }

        errors.add(Localization.NO_DIRECTION_ERROR.toString());
    }

    public void setActive(int side) {
        activatedDirections[side] = true;
    }
}
