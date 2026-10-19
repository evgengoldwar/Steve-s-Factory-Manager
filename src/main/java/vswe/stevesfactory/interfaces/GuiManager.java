package vswe.stevesfactory.interfaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import vswe.stevesfactory.CollisionHelper;
import vswe.stevesfactory.Localization;
import vswe.stevesfactory.animation.AnimationController;
import vswe.stevesfactory.blocks.TileEntityManager;
import vswe.stevesfactory.components.ConnectionOption;
import vswe.stevesfactory.components.FlowComponent;
import vswe.stevesfactory.network.DataBitHelper;
import vswe.stevesfactory.network.DataWriter;
import vswe.stevesfactory.network.PacketHandler;

@SideOnly(Side.CLIENT)
public class GuiManager extends GuiBase {

    public GuiManager(TileEntityManager manager, InventoryPlayer player) {
        super(new ContainerManager(manager, player));

        xSize = MIN_WIDTH;
        ySize = MIN_HEIGHT;

        this.manager = manager;
        Keyboard.enableRepeatEvents(true);
    }

    private static final ResourceLocation COMPONENTS = registerTexture("FlowComponents");

    @Override
    public ResourceLocation getComponentResource() {
        return COMPONENTS;
    }

    public static int Z_LEVEL_COMPONENT_OPEN_DIFFERENCE = 100;
    public static int Z_LEVEL_COMPONENT_CLOSED_DIFFERENCE = 1;
    public static int Z_LEVEL_COMPONENT_START = 750;
    public static int Z_LEVEL_OPEN_MAXIMUM = 5;
    private static final int Z_LEVEL_OVERLAY = 900;

    // the interface fills the screen, but never gets smaller than the original fixed size
    private static final int MIN_WIDTH = 512;
    private static final int MIN_HEIGHT = 256;
    private static final int SCREEN_MARGIN = 4;

    private static final int BORDER = 4;
    private static final int PADDING = 6;
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int INFO_HEIGHT = 34;
    private static final int INFO_ICON_SIZE = 26;
    private static final int ZOOM_FIELD_WIDTH = 36;
    private static final int SIDEBAR_BUTTON_H = 16;
    private static final int SIDEBAR_SPACING = 18;
    private static final int SIDEBAR_LABEL_MAX = 80;
    private static final float SIDEBAR_TEXT_SCALE = 0.75F;
    private static final int CONTEXT_ITEM_HEIGHT = 14;
    private static final int CONTEXT_TOGGLE = 0;
    private static final int CONTEXT_RENAME = 1;
    private static final int CONTEXT_DELETE = 2;

    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 2F;
    private static final float ZOOM_STEP = 1.15F;
    private static final int KEY_PAN_DISTANCE = 40;
    private static final int PAN_THRESHOLD = 3;
    private static final int EDGE_PAN_SIZE = 12;
    private static final float EDGE_PAN_SPEED = 300F;

    private static final int GRID_SIZE = 10;
    private static final int GRID_MAJOR_SIZE = 50;
    private static final int GRID_MIN_SPACING = 6;

    private static final int TOOL_SIZE = 16;
    private static final int TOOL_SPACING = 18;
    private static final int TOOL_FIT = 0;
    private static final int TOOL_RESET_ZOOM = 1;
    private static final int TOOL_GRID = 2;
    private static final int TOOL_HELP = 3;
    private static final int TOOL_COUNT = 4;

    // vanilla inventory colors for the frame
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_TITLE = 0x404040;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_TEXT_GRAY = 0xA0A0A0;
    private static final int COLOR_TEXT_ERROR = 0xFF6060;

    // dark panels sunken into the frame
    private static final int COLOR_INSET = 0xFF2C2F34;
    private static final int COLOR_INSET_BORDER = 0xFF141517;
    private static final int COLOR_CANVAS_OUTSIDE = 0xFF24262A;
    private static final int COLOR_GRID_MINOR = 0xFF32353B;
    private static final int COLOR_GRID_MAJOR = 0xFF3A3D44;

    // dark raised elements, the commands and buttons
    private static final int COLOR_RAISED = 0xFF3E4146;
    private static final int COLOR_RAISED_ACTIVE = 0xFF33363B;
    private static final int COLOR_RAISED_TOP = 0xFF5C5F65;
    private static final int COLOR_RAISED_LEFT = 0xFF4E5157;
    private static final int COLOR_RAISED_BOTTOM = 0xFF25272A;
    private static final int COLOR_RAISED_RIGHT = 0xFF2D2F33;
    private static final int COLOR_RAISED_BORDER = 0xFF0F1011;
    private static final int COLOR_HIGHLIGHT = 0xFF5BCB3B;
    private static final int COLOR_SLOT = 0xFF222427;
    private static final int COLOR_SLOT_BORDER = 0xFF111214;
    private static final int COLOR_SLOT_LIGHT = 0xFF3A3D42;
    private static final int COLOR_ICON = 0xFFFFFFFF;

    private static final int COLOR_CONNECTION = 0xFF4CC34C;
    private static final int COLOR_CONNECTION_SHADOW = 0xFF101113;
    private static final int COLOR_CONNECTION_PREVIEW = 0xFFFFFFFF;
    private static final int LINE_WIDTH = 2;

    private static final int ROUTE_OFFSET = 6;
    private static final int ROUTE_DETOUR = 72;
    private static final int PULSE_SPACING = 12;
    private static final int PULSE_LENGTH = 3;
    private static final float PULSE_SPEED = 12F;
    private static final float PULSE_SPEED_HIGHLIGHT = 30F;

    // views are remembered per manager and per group while the game is running
    private static final Map<TileEntityManager, Map<Integer, float[]>> savedViews = new WeakHashMap<TileEntityManager, Map<Integer, float[]>>();
    private static boolean showGrid = true;

    private int canvasX;
    private int canvasY;
    private int canvasW;
    private int canvasH;
    private int buttonRows = 13;
    private int sidebarX;
    private int sidebarY;
    private int sidebarW;
    private int sidebarH;
    private int sidebarButtonW;
    private int sidebarLabelW;
    private String[] sidebarLabels = new String[0];
    private int infoX;
    private int infoY;
    private int infoW;
    private FlowComponent infoComponent;
    private FlowComponent contextComponent;
    private int contextX;
    private int contextY;

    private float cameraX;
    private float cameraY;
    private float zoom = 1F;
    private int currentGroupId = Integer.MIN_VALUE;

    private boolean isPanning;
    private boolean hasPanned;
    private int panButton;
    private int panStartX;
    private int panStartY;
    private float panStartCameraX;
    private float panStartCameraY;

    @Override
    public void initGui() {
        xSize = Math.max(MIN_WIDTH, width - SCREEN_MARGIN * 2);
        ySize = Math.max(MIN_HEIGHT, height - SCREEN_MARGIN * 2);

        super.initGui();

        int top = BORDER + TITLE_BAR_HEIGHT + 2;
        infoX = PADDING;
        infoW = xSize - PADDING * 2;
        infoY = ySize - BORDER - 2 - INFO_HEIGHT;

        // the side bar lists the buttons with their names, as wide as the longest name allows
        sidebarLabelW = 0;
        for (TileEntityManager.Button button : manager.buttons) {
            sidebarLabelW = Math
                    .max(sidebarLabelW, (int) Math.ceil(getStringWidth(button.getLabel()) * SIDEBAR_TEXT_SCALE));
        }
        sidebarLabelW = Math.min(SIDEBAR_LABEL_MAX, sidebarLabelW);
        sidebarLabels = new String[manager.buttons.size()];
        for (int i = 0; i < sidebarLabels.length; i++) {
            sidebarLabels[i] = fontRendererObj
                    .trimStringToWidth(manager.buttons.get(i).getLabel(), (int) (sidebarLabelW / SIDEBAR_TEXT_SCALE));
        }
        sidebarButtonW = 17 + sidebarLabelW + 4;
        sidebarX = PADDING;
        sidebarY = top;
        sidebarH = infoY - 4 - top;
        buttonRows = Math.max(1, (sidebarH - 6 - SIDEBAR_BUTTON_H) / SIDEBAR_SPACING + 1);
        int buttonColumns = Math.max(1, (manager.buttons.size() + buttonRows - 1) / buttonRows);
        sidebarW = 4 + buttonColumns * (sidebarButtonW + 2);

        canvasX = sidebarX + sidebarW + 6;
        canvasY = top + 2;
        canvasW = xSize - PADDING - 2 - canvasX;
        canvasH = sidebarH - 4;
    }

    @Override
    protected float getScale() {
        float mult = Math
                .min((width - SCREEN_MARGIN * 2) / (float) xSize, (height - SCREEN_MARGIN * 2) / (float) ySize);
        mult = Math.min(1F, mult);
        return (float) (Math.floor(mult * 1000)) / 1000F;
    }

    @Override
    public void drawWorldBackground(int val) {
        if (usePinkScreen) {
            drawRect(0, 0, width, height, 0xFFEC008C);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (useBlueScreen) {
            drawRect(0, 0, width, height, 0xFF000A91);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (useGreenScreen) {
            drawRect(0, 0, width, height, 0xFF00FF00);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            super.drawWorldBackground(val);
        }
    }

    private long lastTicks;

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        x -= guiLeft;
        y -= guiTop;

        long ticks = Minecraft.getSystemTime();
        float elapsedSeconds = lastTicks == 0 ? 0 : (ticks - this.lastTicks) / 1000F;

        boolean useScreen = useGreenScreen || useBlueScreen || usePinkScreen;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (!useScreen) {
            drawPanel(0, 0, xSize, ySize);
            drawTitleBar();
        }

        if (hasSpecialRenderer()) {
            // the special interfaces are designed for the original size, keep them centered
            int offsetX = getSpecialOffsetX();
            int offsetY = getSpecialOffsetY();
            if (!useScreen) {
                drawInsetPanel(offsetX - 2, offsetY - 2, MIN_WIDTH + 4, MIN_HEIGHT + 4);
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(offsetX, offsetY, 0);
            bindTexture(COMPONENTS);
            getSpecialRenderer().draw(this, x - offsetX, y - offsetY);
            getSpecialRenderer().drawMouseOver(this, x - offsetX, y - offsetY);
            GL11.glPopMatrix();
            this.lastTicks = ticks;
            return;
        }

        updateView();

        if (!useScreen) {
            drawInsetPanel(sidebarX, sidebarY, sidebarW, sidebarH);
            drawInsetPanel(canvasX - 2, canvasY - 2, canvasW + 4, canvasH + 4);
        }
        // the components are depth sorted, they should never end up behind the background
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        updateEdgePanning(x, y, elapsedSeconds);

        int worldX = toWorldX(x);
        int worldY = toWorldY(y);
        boolean onContextMenu = isOnContextMenu(x, y);

        startClipping(canvasX, canvasY, canvasW, canvasH);
        if (!useScreen) {
            drawCanvasBackground();
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(guiLeft + canvasX, guiTop + canvasY, 0);
        GL11.glScalef(zoom, zoom, 1);
        GL11.glTranslatef(-cameraX - guiLeft, -cameraY - guiTop, 0);

        bindTexture(COMPONENTS);

        // update components completely independent on their visibility
        if (controller != null) {
            controller.update(elapsedSeconds);
        }
        for (FlowComponent component : manager.getFlowItems()) {
            component.update(elapsedSeconds);
        }
        this.lastTicks = ticks;

        int zLevel = Z_LEVEL_COMPONENT_START;
        int openCount = 0;
        for (int i = 0; i < manager.getZLevelRenderingList().size(); i++) {
            FlowComponent itemBase = manager.getZLevelRenderingList().get(i);

            if (itemBase.isVisible()) {
                if (itemBase.isOpen() && openCount == Z_LEVEL_OPEN_MAXIMUM) {
                    itemBase.close();
                }

                if (itemBase.isOpen()) {
                    zLevel -= Z_LEVEL_COMPONENT_OPEN_DIFFERENCE;
                    openCount++;
                } else {
                    zLevel -= Z_LEVEL_COMPONENT_CLOSED_DIFFERENCE;
                }
                itemBase.draw(this, worldX, worldY, zLevel);

                if (itemBase.isBeingMoved() || CollisionHelper.inBounds(
                        itemBase.getX(),
                        itemBase.getY(),
                        itemBase.getComponentWidth(),
                        itemBase.getComponentHeight(),
                        worldX,
                        worldY)) {
                    CollisionHelper.disableInBoundsCheck = true;
                }
            }
        }
        CollisionHelper.disableInBoundsCheck = false;

        GL11.glPopMatrix();
        stopClipping();

        if (isInCanvas(x, y) && !onContextMenu) {
            FlowComponent hovered = getComponentAt(worldX, worldY, false);
            if (hovered != null) {
                infoComponent = hovered;
            }
        }
        if (infoComponent != null && (!infoComponent.isVisible() || !manager.getFlowItems().contains(infoComponent))) {
            infoComponent = null;
        }
        if (contextComponent != null
                && (!contextComponent.isVisible() || !manager.getFlowItems().contains(contextComponent))) {
            contextComponent = null;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, Z_LEVEL_OVERLAY);
        drawToolbar(x, y);

        bindTexture(COMPONENTS);
        if (useButtons) {
            for (int i = 0; i < manager.buttons.size(); i++) {
                TileEntityManager.Button button = manager.buttons.get(i);
                if (button.isVisible()) {
                    int buttonX = getButtonX(i);
                    int buttonY = getButtonY(i);
                    boolean hover = isOnButton(i, x, y);

                    drawDarkButton(buttonX, buttonY, sidebarButtonW, SIDEBAR_BUTTON_H, hover, false);
                    drawTexture(
                            buttonX + 2,
                            buttonY + 2,
                            TileEntityManager.BUTTON_INNER_SRC_X,
                            TileEntityManager.BUTTON_INNER_SRC_Y + i * TileEntityManager.BUTTON_INNER_SIZE_H,
                            TileEntityManager.BUTTON_INNER_SIZE_W,
                            TileEntityManager.BUTTON_INNER_SIZE_H);
                    drawShadowString(sidebarLabels[i], buttonX + 17, buttonY + 5, SIDEBAR_TEXT_SCALE, COLOR_TEXT);
                }
            }
        }

        if (useInfo) {
            drawInfoPanel();
        }

        if (contextComponent != null) {
            drawContextMenu(x, y);
        }
        GL11.glPopMatrix();

        if (useMouseOver && !onContextMenu) {
            if (useButtons) {
                for (int i = 0; i < manager.buttons.size(); i++) {
                    TileEntityManager.Button button = manager.buttons.get(i);
                    if (button.isVisible() && isOnButton(i, x, y)) {
                        drawMouseOver(button.getMouseOver(), x, y);
                    }
                }
            }

            int tool = getToolAt(x, y);
            if (tool != -1) {
                drawMouseOver(getToolMouseOver(tool), x, y);
            } else if (isInCanvas(x, y)) {
                setTooltipTransform(canvasX - cameraX * zoom, canvasY - cameraY * zoom, zoom);
                for (FlowComponent itemBase : manager.getZLevelRenderingList()) {
                    if (itemBase.isVisible()) {
                        itemBase.drawMouseOver(this, worldX, worldY);
                        if (itemBase.isBeingMoved() || CollisionHelper.inBounds(
                                itemBase.getX(),
                                itemBase.getY(),
                                itemBase.getComponentWidth(),
                                itemBase.getComponentHeight(),
                                worldX,
                                worldY)) {
                            CollisionHelper.disableInBoundsCheck = true;
                        }
                    }
                }
                resetTooltipTransform();
            }
        }
        CollisionHelper.disableInBoundsCheck = false;

        if (!Keyboard.isKeyDown(54) && doubleShiftFlag) {
            doubleShiftFlag = false;
        }
    }

    private void drawTitleBar() {
        // icon showing a few connected nodes
        int iconX = PADDING;
        int iconY = BORDER + 2;
        boolean started = beginRects();
        drawSlot(iconX, iconY, TOOL_SIZE, TOOL_SIZE, COLOR_SLOT_BORDER);
        drawColoredRect(iconX + 3, iconY + 3, 4, 4, COLOR_ICON);
        drawColoredRect(iconX + 9, iconY + 9, 4, 4, COLOR_ICON);
        drawColoredRect(iconX + 9, iconY + 3, 4, 4, COLOR_CONNECTION);
        drawColoredRect(iconX + 7, iconY + 4, 2, 1, COLOR_CONNECTION);
        drawColoredRect(iconX + 10, iconY + 7, 1, 2, COLOR_CONNECTION);
        endRects(started);

        drawString(Localization.MANAGER_TITLE.toString(), iconX + TOOL_SIZE + 5, iconY + 4, 1F, COLOR_TITLE);

        // the current zoom is shown in a field next to the tools
        String zoomText = Math.round(zoom * 100) + "%";
        int fieldX = getToolX(0) - ZOOM_FIELD_WIDTH - 4;
        drawSlot(fieldX, getToolY(), ZOOM_FIELD_WIDTH, TOOL_SIZE, COLOR_SLOT_BORDER);
        drawShadowString(
                zoomText,
                fieldX + (ZOOM_FIELD_WIDTH - getStringWidth(zoomText)) / 2,
                getToolY() + 4,
                COLOR_TEXT);
    }

    private void drawInfoPanel() {
        drawInsetPanel(infoX, infoY, infoW, INFO_HEIGHT);

        int slotX = infoX + 4;
        int slotY = infoY + 4;
        drawSlot(slotX, slotY, INFO_ICON_SIZE, INFO_ICON_SIZE, COLOR_SLOT_BORDER);

        // the path and the amount of commands are shown to the right
        String commands = getInfo(infoW / 2);
        drawShadowString(commands, infoX + infoW - 6 - getStringWidth(commands), infoY + 6, COLOR_TEXT_GRAY);

        FlowComponent component = infoComponent;
        if (component == null) {
            return;
        }

        bindTexture(COMPONENTS);
        GL11.glPushMatrix();
        GL11.glTranslatef(guiLeft + slotX + 1, guiTop + slotY + 1, 0);
        GL11.glScalef(2, 2, 1);
        GL11.glTranslatef(-guiLeft, -guiTop, 0);
        drawTexture(
                0,
                0,
                TileEntityManager.BUTTON_INNER_SRC_X,
                TileEntityManager.BUTTON_INNER_SRC_Y
                        + component.getType().ordinal() * TileEntityManager.BUTTON_INNER_SIZE_H,
                TileEntityManager.BUTTON_INNER_SIZE_W,
                TileEntityManager.BUTTON_INNER_SIZE_H);
        GL11.glPopMatrix();

        int textX = slotX + INFO_ICON_SIZE + 6;
        int textWidth = infoW / 2 - textX;
        drawShadowString(
                fontRendererObj.trimStringToWidth(component.getName(), textWidth),
                textX,
                infoY + 5,
                COLOR_TEXT);
        drawShadowString(component.getType().getLongName(), textX, infoY + 16, 0.75F, COLOR_TEXT_GRAY);
        if (!component.getErrors().isEmpty()) {
            String error = component.getErrors().get(0).replace("\n", " ");
            drawShadowString(
                    fontRendererObj.trimStringToWidth(error, (int) (textWidth / 0.75F)),
                    textX,
                    infoY + 24,
                    0.75F,
                    COLOR_TEXT_ERROR);
        }
    }

    private List<String> getContextItems() {
        List<String> items = new ArrayList<String>();
        items.add(
                contextComponent.isOpen() ? Localization.CONTEXT_CLOSE.toString()
                        : Localization.CONTEXT_OPEN.toString());
        items.add(Localization.CONTEXT_RENAME.toString());
        items.add(Localization.CONTEXT_DELETE.toString());
        return items;
    }

    private int getContextWidth() {
        int width = 0;
        for (String item : getContextItems()) {
            width = Math.max(width, getStringWidth(item));
        }
        return width + 16;
    }

    private int getContextHeight() {
        return getContextItems().size() * CONTEXT_ITEM_HEIGHT + 4;
    }

    private boolean isOnContextMenu(int x, int y) {
        return contextComponent != null && x >= contextX
                && x < contextX + getContextWidth()
                && y >= contextY
                && y < contextY + getContextHeight();
    }

    private int getContextItemAt(int x, int y) {
        if (!isOnContextMenu(x, y)) {
            return -1;
        }
        int item = (y - contextY - 2) / CONTEXT_ITEM_HEIGHT;
        return item >= 0 && item < getContextItems().size() ? item : -1;
    }

    private void drawContextMenu(int mX, int mY) {
        List<String> items = getContextItems();
        int w = getContextWidth();
        drawRaisedPanel(contextX, contextY, w, getContextHeight());
        int hovered = getContextItemAt(mX, mY);
        for (int i = 0; i < items.size(); i++) {
            int itemY = contextY + 2 + i * CONTEXT_ITEM_HEIGHT;
            if (i == hovered) {
                drawColoredRect(contextX + 2, itemY, w - 4, CONTEXT_ITEM_HEIGHT, COLOR_HIGHLIGHT);
                drawColoredRect(contextX + 3, itemY + 1, w - 6, CONTEXT_ITEM_HEIGHT - 2, COLOR_RAISED_ACTIVE);
            }
            drawShadowString(items.get(i), contextX + 8, itemY + 3, COLOR_TEXT);
        }
    }

    private void openContextMenu(FlowComponent component, int x, int y) {
        contextComponent = component;
        contextX = Math.min(x, xSize - BORDER - getContextWidth());
        contextY = Math.min(y, ySize - BORDER - getContextHeight());
    }

    private void onContextClick(int item) {
        FlowComponent component = contextComponent;
        contextComponent = null;
        switch (item) {
            case CONTEXT_TOGGLE:
                component.setOpen(!component.isOpen());
                bringToFront(component);
                break;
            case CONTEXT_RENAME:
                component.startRenaming();
                bringToFront(component);
                break;
            case CONTEXT_DELETE:
                // removed in the same way as when dropped on the delete button
                manager.removalTarget = component;
                TileEntityManager.Button button = manager.buttons.get(manager.deleteButtonId);
                DataWriter dw = PacketHandler.getButtonPacketWriter();
                dw.writeData(manager.deleteButtonId, DataBitHelper.GUI_BUTTON_ID);
                if (button.onClick(dw)) {
                    PacketHandler.sendDataToServer(dw);
                }
                manager.removalTarget = null;
                break;
        }
    }

    private void bringToFront(FlowComponent component) {
        manager.getZLevelRenderingList().remove(component);
        manager.getZLevelRenderingList().add(0, component);
    }

    private FlowComponent getComponentAt(int worldX, int worldY, boolean titleOnly) {
        for (FlowComponent component : manager.getZLevelRenderingList()) {
            if (component.isVisible() && worldX >= component.getX()
                    && worldX < component.getX() + component.getComponentWidth()
                    && worldY >= component.getY()
                    && worldY < component.getY()
                            + (titleOnly ? FlowComponent.getTitleHeight() : component.getComponentHeight())) {
                return component;
            }
        }
        return null;
    }

    private void drawPanel(int x, int y, int w, int h) {
        boolean started = beginRects();
        drawColoredRect(x + 1, y + 1, w - 2, h - 2, COLOR_PANEL);
        drawColoredRect(x + 2, y + 1, w - 5, 2, COLOR_PANEL_LIGHT);
        drawColoredRect(x + 1, y + 2, 2, h - 5, COLOR_PANEL_LIGHT);
        drawColoredRect(x + 3, y + h - 3, w - 5, 2, COLOR_PANEL_DARK);
        drawColoredRect(x + w - 3, y + 3, 2, h - 5, COLOR_PANEL_DARK);

        drawColoredRect(x + 2, y, w - 4, 1, COLOR_OUTLINE);
        drawColoredRect(x + 2, y + h - 1, w - 4, 1, COLOR_OUTLINE);
        drawColoredRect(x, y + 2, 1, h - 4, COLOR_OUTLINE);
        drawColoredRect(x + w - 1, y + 2, 1, h - 4, COLOR_OUTLINE);
        drawColoredRect(x + 1, y + 1, 1, 1, COLOR_OUTLINE);
        drawColoredRect(x + w - 2, y + 1, 1, 1, COLOR_OUTLINE);
        drawColoredRect(x + 1, y + h - 2, 1, 1, COLOR_OUTLINE);
        drawColoredRect(x + w - 2, y + h - 2, 1, 1, COLOR_OUTLINE);
        endRects(started);
    }

    /**
     * The panels inside the frame are dark and sunken into it
     */
    private void drawInsetPanel(int x, int y, int w, int h) {
        boolean started = beginRects();
        drawColoredRect(x, y, w - 1, 1, COLOR_PANEL_DARK);
        drawColoredRect(x, y, 1, h - 1, COLOR_PANEL_DARK);
        drawColoredRect(x + 1, y + h - 1, w - 1, 1, COLOR_PANEL_LIGHT);
        drawColoredRect(x + w - 1, y + 1, 1, h - 1, COLOR_PANEL_LIGHT);
        drawColoredRect(x + 1, y + 1, w - 2, h - 2, COLOR_INSET_BORDER);
        drawColoredRect(x + 2, y + 2, w - 4, h - 4, COLOR_INSET);
        endRects(started);
    }

    private void drawRaised(int x, int y, int w, int h, boolean active) {
        boolean started = beginRects();
        drawColoredRect(x, y, w, h, active ? COLOR_RAISED_ACTIVE : COLOR_RAISED);
        if (!active) {
            drawColoredRect(x, y, w, 1, COLOR_RAISED_TOP);
            drawColoredRect(x, y + 1, 1, h - 2, COLOR_RAISED_LEFT);
        }
        drawColoredRect(x, y + h - 1, w, 1, COLOR_RAISED_BOTTOM);
        drawColoredRect(x + w - 1, y + 1, 1, h - 2, COLOR_RAISED_RIGHT);
        endRects(started);
    }

    /**
     * Commands and menus are dark raised panels with rounded corners
     */
    public void drawRaisedPanel(int x, int y, int w, int h) {
        boolean started = beginRects();
        drawColoredRect(x + 1, y, w - 2, 1, COLOR_RAISED_BORDER);
        drawColoredRect(x + 1, y + h - 1, w - 2, 1, COLOR_RAISED_BORDER);
        drawColoredRect(x, y + 1, 1, h - 2, COLOR_RAISED_BORDER);
        drawColoredRect(x + w - 1, y + 1, 1, h - 2, COLOR_RAISED_BORDER);
        drawRaised(x + 1, y + 1, w - 2, h - 2, false);
        endRects(started);
    }

    /**
     * Buttons are dark raised boxes with a green border when hovered
     */
    public void drawDarkButton(int x, int y, int w, int h, boolean hover, boolean active) {
        boolean started = beginRects();
        drawColoredRect(x, y, w, h, hover ? COLOR_HIGHLIGHT : COLOR_RAISED_BORDER);
        drawRaised(x + 1, y + 1, w - 2, h - 2, active);
        endRects(started);
    }

    /**
     * A sunken slot, used behind icons and fields
     */
    public void drawSlot(int x, int y, int w, int h, int border) {
        boolean started = beginRects();
        drawColoredRect(x, y, w, h, border);
        drawColoredRect(x + 1, y + 1, w - 2, h - 2, COLOR_SLOT);
        drawColoredRect(x + 1, y + h - 2, w - 2, 1, COLOR_SLOT_LIGHT);
        endRects(started);
    }

    public int getSlotBorderColor() {
        return COLOR_SLOT_BORDER;
    }

    private void drawCanvasBackground() {
        boolean started = beginRects();
        // darken everything outside the area commands can be placed in
        int left = toLocalX(0);
        int top = toLocalY(0);
        int right = toLocalX(FlowComponent.MAX_POSITION_X + 1);
        int bottom = toLocalY(FlowComponent.MAX_POSITION_Y + 1);
        int canvasRight = canvasX + canvasW;
        int canvasBottom = canvasY + canvasH;
        drawColoredRect(canvasX, canvasY, left - canvasX, canvasH, COLOR_CANVAS_OUTSIDE);
        drawColoredRect(right, canvasY, canvasRight - right, canvasH, COLOR_CANVAS_OUTSIDE);
        drawColoredRect(canvasX, canvasY, canvasW, top - canvasY, COLOR_CANVAS_OUTSIDE);
        drawColoredRect(canvasX, bottom, canvasW, canvasBottom - bottom, COLOR_CANVAS_OUTSIDE);

        if (!showGrid) {
            endRects(started);
            return;
        }

        int step = GRID_SIZE * zoom >= GRID_MIN_SPACING ? GRID_SIZE : GRID_MAJOR_SIZE;
        int startX = Math.max(left, canvasX);
        int endX = Math.min(right, canvasRight);
        int startY = Math.max(top, canvasY);
        int endY = Math.min(bottom, canvasBottom);

        for (int worldX = Math.max(0, (int) Math.floor(cameraX / step) * step); worldX
                <= FlowComponent.MAX_POSITION_X; worldX += step) {
            int localX = toLocalX(worldX);
            if (localX >= canvasRight) {
                break;
            }
            if (localX >= canvasX) {
                drawColoredRect(
                        localX,
                        startY,
                        1,
                        endY - startY,
                        worldX % GRID_MAJOR_SIZE == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
            }
        }

        for (int worldY = Math.max(0, (int) Math.floor(cameraY / step) * step); worldY
                <= FlowComponent.MAX_POSITION_Y; worldY += step) {
            int localY = toLocalY(worldY);
            if (localY >= canvasBottom) {
                break;
            }
            if (localY >= canvasY) {
                drawColoredRect(
                        startX,
                        localY,
                        endX - startX,
                        1,
                        worldY % GRID_MAJOR_SIZE == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
            }
        }
        endRects(started);
    }

    private void drawToolbar(int mX, int mY) {
        for (int i = 0; i < TOOL_COUNT; i++) {
            int x = getToolX(i);
            int y = getToolY();
            boolean hover = CollisionHelper.inBounds(x, y, TOOL_SIZE - 1, TOOL_SIZE - 1, mX, mY);
            boolean active = i == TOOL_GRID && showGrid;
            int iconColor = COLOR_ICON;

            boolean started = beginRects();
            drawDarkButton(x, y, TOOL_SIZE, TOOL_SIZE, hover, active);

            int iconX = x + 4;
            int iconY = y + 4;
            switch (i) {
                case TOOL_FIT:
                    // four corners
                    drawColoredRect(iconX, iconY, 3, 1, iconColor);
                    drawColoredRect(iconX, iconY, 1, 3, iconColor);
                    drawColoredRect(iconX + 5, iconY, 3, 1, iconColor);
                    drawColoredRect(iconX + 7, iconY, 1, 3, iconColor);
                    drawColoredRect(iconX, iconY + 7, 3, 1, iconColor);
                    drawColoredRect(iconX, iconY + 5, 1, 3, iconColor);
                    drawColoredRect(iconX + 5, iconY + 7, 3, 1, iconColor);
                    drawColoredRect(iconX + 7, iconY + 5, 1, 3, iconColor);
                    break;
                case TOOL_RESET_ZOOM:
                    // magnifying glass
                    drawColoredRect(iconX + 1, iconY, 3, 1, iconColor);
                    drawColoredRect(iconX + 1, iconY + 4, 3, 1, iconColor);
                    drawColoredRect(iconX, iconY + 1, 1, 3, iconColor);
                    drawColoredRect(iconX + 4, iconY + 1, 1, 3, iconColor);
                    drawColoredRect(iconX + 4, iconY + 4, 2, 2, iconColor);
                    drawColoredRect(iconX + 6, iconY + 6, 2, 2, iconColor);
                    break;
                case TOOL_GRID:
                    for (int j = 0; j < 3; j++) {
                        drawColoredRect(iconX + 1 + j * 3, iconY, 1, 8, iconColor);
                        drawColoredRect(iconX, iconY + 1 + j * 3, 8, 1, iconColor);
                    }
                    break;
                case TOOL_HELP:
                    endRects(started);
                    started = false;
                    drawShadowString("?", x + 5, y + 4, iconColor);
                    break;
            }
            endRects(started);
        }
    }

    private int getToolX(int id) {
        return xSize - PADDING - (TOOL_COUNT - id) * TOOL_SPACING + (TOOL_SPACING - TOOL_SIZE);
    }

    private int getToolY() {
        return BORDER + 2;
    }

    private int getToolAt(int mX, int mY) {
        for (int i = 0; i < TOOL_COUNT; i++) {
            if (CollisionHelper.inBounds(getToolX(i), getToolY(), TOOL_SIZE - 1, TOOL_SIZE - 1, mX, mY)) {
                return i;
            }
        }
        return -1;
    }

    private String getToolMouseOver(int tool) {
        switch (tool) {
            case TOOL_FIT:
                return Localization.VIEW_FIT.toString();
            case TOOL_RESET_ZOOM:
                return Localization.VIEW_RESET_ZOOM.toString();
            case TOOL_GRID:
                return Localization.VIEW_GRID.toString();
            default:
                return Localization.VIEW_HELP.toString() + "\n"
                        + Localization.VIEW_HELP_PAN.toString()
                        + "\n"
                        + Localization.VIEW_HELP_ZOOM.toString()
                        + "\n"
                        + Localization.VIEW_HELP_KEYS.toString()
                        + "\n"
                        + Localization.VIEW_HELP_CANCEL.toString()
                        + "\n"
                        + Localization.VIEW_HELP_OPEN.toString()
                        + "\n"
                        + Localization.VIEW_HELP_CONTEXT.toString();
        }
    }

    private void onToolClick(int tool) {
        switch (tool) {
            case TOOL_FIT:
                fitView();
                break;
            case TOOL_RESET_ZOOM:
                setZoom(1F, canvasX + canvasW / 2, canvasY + canvasH / 2);
                break;
            case TOOL_GRID:
                showGrid = !showGrid;
                break;
        }
    }

    public void drawConnection(int[] pathX, int[] pathY, ConnectionOption.ConnectionType startType,
            ConnectionOption.ConnectionType endType, ConnectionOption colorOption, boolean highlight, boolean forward) {
        int color = getConnectionColor(colorOption);
        if (highlight) {
            color = getLighterColor(color);
        }
        drawRoute(
                createRoute(pathX, pathY, startType, endType),
                color,
                forward,
                highlight ? PULSE_SPEED_HIGHLIGHT : PULSE_SPEED);
    }

    public void drawConnectionPreview(int startX, int startY, ConnectionOption.ConnectionType startType, int endX,
            int endY) {
        ConnectionOption.ConnectionType endType = startType == ConnectionOption.ConnectionType.INPUT
                ? ConnectionOption.ConnectionType.OUTPUT
                : ConnectionOption.ConnectionType.INPUT;
        drawRoute(
                createRoute(new int[] { startX, endX }, new int[] { startY, endY }, startType, endType),
                COLOR_CONNECTION_PREVIEW,
                startType != ConnectionOption.ConnectionType.INPUT,
                PULSE_SPEED_HIGHLIGHT);
    }

    private static int getConnectionColor(ConnectionOption option) {
        if (option == null) {
            return COLOR_CONNECTION;
        }
        switch (option) {
            case CONDITION_TRUE:
                return 0xFFA6E84C;
            case CONDITION_FALSE:
                return 0xFFE84C4C;
            case INTERVAL:
                return 0xFFE8C43C;
            case FOR_EACH:
                return 0xFFC86AE8;
            case REDSTONE_PULSE_HIGH:
            case REDSTONE_PULSE_LOW:
            case REDSTONE_HIGH:
            case REDSTONE_LOW:
                return 0xFFC82828;
            case BUD:
            case BUD_PULSE_HIGH:
            case BUD_HIGH:
            case BUD_PULSE_LOW:
            case BUD_LOW:
                return 0xFFE8963C;
            default:
                return COLOR_CONNECTION;
        }
    }

    private static int getLighterColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = (r + 255) / 2;
        g = (g + 255) / 2;
        b = (b + 255) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int getDirectionX(ConnectionOption.ConnectionType type) {
        return type == ConnectionOption.ConnectionType.SIDE ? 1 : 0;
    }

    private static int getDirectionY(ConnectionOption.ConnectionType type) {
        if (type == ConnectionOption.ConnectionType.INPUT) {
            return -1;
        } else if (type == ConnectionOption.ConnectionType.OUTPUT) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Creates a path of only horizontal and vertical lines, like the lines of the NEI crafting tree
     */
    private static List<int[]> createRoute(int[] pathX, int[] pathY, ConnectionOption.ConnectionType startType,
            ConnectionOption.ConnectionType endType) {
        List<int[]> route = new ArrayList<int[]>();
        int last = pathX.length - 1;
        int startX = pathX[0];
        int startY = pathY[0];
        int endX = pathX[last];
        int endY = pathY[last];

        // leave and enter the connection points straight
        int[] exit = { startX + getDirectionX(startType) * ROUTE_OFFSET,
                startY + getDirectionY(startType) * ROUTE_OFFSET };
        int[] entry = { endX + getDirectionX(endType) * ROUTE_OFFSET, endY + getDirectionY(endType) * ROUTE_OFFSET };

        addRoutePoint(route, startX, startY);
        addRoutePoint(route, exit[0], exit[1]);
        if (last == 1) {
            boolean vertical = getDirectionY(startType) != 0 && getDirectionY(endType) != 0;
            boolean towardsEntry = getDirectionY(startType) * (entry[1] - exit[1]) >= 0;
            if (vertical && towardsEntry) {
                int middleY = (exit[1] + entry[1]) / 2;
                addRoutePoint(route, exit[0], middleY);
                addRoutePoint(route, entry[0], middleY);
            } else if (getDirectionX(startType) != 0) {
                addRoutePoint(route, entry[0], exit[1]);
            } else {
                // going back up, so pass beside the commands instead of through them
                int middleX = Math.abs(exit[0] - entry[0]) < ROUTE_DETOUR ? Math.max(exit[0], entry[0]) + ROUTE_DETOUR
                        : (exit[0] + entry[0]) / 2;
                addRoutePoint(route, middleX, exit[1]);
                addRoutePoint(route, middleX, entry[1]);
            }
        } else {
            // nodes placed by the player are visited in order, turning once between each of them
            int[] previous = exit;
            for (int i = 1; i < last; i++) {
                if (i == 1 && getDirectionX(startType) == 0) {
                    addRoutePoint(route, previous[0], pathY[i]);
                } else {
                    addRoutePoint(route, pathX[i], previous[1]);
                }
                addRoutePoint(route, pathX[i], pathY[i]);
                previous = new int[] { pathX[i], pathY[i] };
            }
            addRoutePoint(route, entry[0], previous[1]);
        }
        addRoutePoint(route, entry[0], entry[1]);
        addRoutePoint(route, endX, endY);
        return route;
    }

    private static void addRoutePoint(List<int[]> route, int x, int y) {
        int size = route.size();
        if (size > 0) {
            int[] previous = route.get(size - 1);
            if (previous[0] == x && previous[1] == y) {
                return;
            }
            if (size > 1) {
                int[] before = route.get(size - 2);
                // remove points in the middle of a straight line
                if ((before[0] == previous[0] && previous[0] == x) || (before[1] == previous[1] && previous[1] == y)) {
                    route.remove(size - 1);
                }
            }
        }
        route.add(new int[] { x, y });
    }

    private void drawRoute(List<int[]> route, int color, boolean forward, float speed) {
        // everything of the line is drawn at once, the shadow first so the line covers it
        boolean started = beginRects();

        // a dark shadow below and to the right, like the shadow of minecraft text
        for (int i = 1; i < route.size(); i++) {
            drawSegment(route.get(i - 1), route.get(i), 1, COLOR_CONNECTION_SHADOW);
        }
        int length = 0;
        for (int i = 1; i < route.size(); i++) {
            drawSegment(route.get(i - 1), route.get(i), 0, color);
            length += getSegmentLength(route.get(i - 1), route.get(i));
        }

        // short pulses moving along the line, a bit like a redstone signal
        if (length >= PULSE_SPACING) {
            List<int[]> path = route;
            if (!forward) {
                path = new ArrayList<int[]>(route);
                Collections.reverse(path);
            }
            int pulseColor = getLighterColor(color);
            int offset = (int) ((Minecraft.getSystemTime() % 100000L) / 1000F * speed) % PULSE_SPACING;
            int segment = 1;
            int segmentStart = 0;
            for (int distance = offset; distance + PULSE_LENGTH <= length; distance += PULSE_SPACING) {
                for (int j = 0; j < PULSE_LENGTH; j++) {
                    int target = distance + j;
                    while (segment < path.size() - 1
                            && target > segmentStart + getSegmentLength(path.get(segment - 1), path.get(segment))) {
                        segmentStart += getSegmentLength(path.get(segment - 1), path.get(segment));
                        segment++;
                    }
                    int[] from = path.get(segment - 1);
                    int[] to = path.get(segment);
                    int along = target - segmentStart;
                    drawColoredRect(
                            from[0] + Integer.signum(to[0] - from[0]) * along,
                            from[1] + Integer.signum(to[1] - from[1]) * along,
                            LINE_WIDTH,
                            LINE_WIDTH,
                            pulseColor);
                }
            }
        }

        endRects(started);
    }

    private static int getSegmentLength(int[] from, int[] to) {
        return Math.abs(to[0] - from[0]) + Math.abs(to[1] - from[1]);
    }

    private void drawSegment(int[] from, int[] to, int offset, int color) {
        drawColoredRect(
                Math.min(from[0], to[0]) + offset,
                Math.min(from[1], to[1]) + offset,
                Math.abs(to[0] - from[0]) + LINE_WIDTH,
                Math.abs(to[1] - from[1]) + LINE_WIDTH,
                color);
    }

    private void startClipping(int x, int y, int w, int h) {
        ScaledResolution resolution = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int factor = resolution.getScaleFactor();
        float scale = getScale();
        float offsetX = (width - xSize * scale) / 2;
        float offsetY = (height - ySize * scale) / 2;

        int pixelX = (int) Math.floor((offsetX + x * scale) * factor);
        int pixelY = (int) Math.floor((offsetY + y * scale) * factor);
        int pixelW = (int) Math.ceil(w * scale * factor);
        int pixelH = (int) Math.ceil(h * scale * factor);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(pixelX, this.mc.displayHeight - pixelY - pixelH, pixelW, pixelH);
    }

    private void stopClipping() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int getButtonX(int id) {
        return sidebarX + 3 + (id / buttonRows) * (sidebarButtonW + 2);
    }

    private int getButtonY(int id) {
        return sidebarY + 3 + (id % buttonRows) * SIDEBAR_SPACING;
    }

    private boolean isOnButton(int id, int x, int y) {
        return CollisionHelper.inBounds(getButtonX(id), getButtonY(id), sidebarButtonW - 1, SIDEBAR_BUTTON_H - 1, x, y);
    }

    private int getSpecialOffsetX() {
        return (xSize - MIN_WIDTH) / 2;
    }

    private int getSpecialOffsetY() {
        return (ySize - MIN_HEIGHT) / 2;
    }

    private boolean isInCanvas(int x, int y) {
        return x >= canvasX && x < canvasX + canvasW && y >= canvasY && y < canvasY + canvasH;
    }

    private int toWorldX(int x) {
        return (int) Math.floor((x - canvasX) / zoom + cameraX);
    }

    private int toWorldY(int y) {
        return (int) Math.floor((y - canvasY) / zoom + cameraY);
    }

    private int toLocalX(float worldX) {
        return (int) Math.floor(canvasX + (worldX - cameraX) * zoom);
    }

    private int toLocalY(float worldY) {
        return (int) Math.floor(canvasY + (worldY - cameraY) * zoom);
    }

    private void clampCamera() {
        float viewW = canvasW / zoom;
        float viewH = canvasH / zoom;
        float margin = 40;
        cameraX = Math.max(-viewW + margin, Math.min(FlowComponent.MAX_POSITION_X - margin, cameraX));
        cameraY = Math.max(-viewH + margin, Math.min(FlowComponent.MAX_POSITION_Y - margin, cameraY));
    }

    private void setZoom(float newZoom, int anchorX, int anchorY) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (Math.abs(newZoom - 1F) < 0.03F) {
            newZoom = 1F;
        }
        // keep the point below the anchor in the same place
        float worldX = (anchorX - canvasX) / zoom + cameraX;
        float worldY = (anchorY - canvasY) / zoom + cameraY;
        zoom = newZoom;
        cameraX = worldX - (anchorX - canvasX) / zoom;
        cameraY = worldY - (anchorY - canvasY) / zoom;
        clampCamera();
    }

    private void fitView() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (FlowComponent component : manager.getFlowItems()) {
            if (component.isVisible()) {
                minX = Math.min(minX, component.getX());
                minY = Math.min(minY, component.getY());
                maxX = Math.max(maxX, component.getX() + component.getComponentWidth());
                maxY = Math.max(maxY, component.getY() + component.getComponentHeight());
            }
        }

        if (minX > maxX) {
            zoom = 1F;
            cameraX = 0;
            cameraY = 0;
        } else {
            int padding = 20;
            float w = maxX - minX + padding * 2;
            float h = maxY - minY + padding * 2;
            zoom = Math.max(MIN_ZOOM, Math.min(1F, Math.min(canvasW / w, canvasH / h)));
            cameraX = (minX + maxX) / 2F - canvasW / zoom / 2;
            cameraY = (minY + maxY) / 2F - canvasH / zoom / 2;
        }
        clampCamera();
    }

    private Map<Integer, float[]> getSavedViews() {
        Map<Integer, float[]> views = savedViews.get(manager);
        if (views == null) {
            views = new HashMap<Integer, float[]>();
            savedViews.put(manager, views);
        }
        return views;
    }

    private void updateView() {
        FlowComponent group = manager.getSelectedComponent();
        int groupId = group == null ? -1 : group.getId();
        if (groupId != currentGroupId) {
            float[] view = getSavedViews().get(groupId);
            if (view != null) {
                cameraX = view[0];
                cameraY = view[1];
                zoom = view[2];
            } else {
                cameraX = 0;
                cameraY = 0;
                zoom = 1F;
            }
            currentGroupId = groupId;
        }
        clampCamera();
        getSavedViews().put(groupId, new float[] { cameraX, cameraY, zoom });

        // place new commands in the middle of the visible area
        manager.spawnX = FlowComponent.clampPositionX(toWorldX(canvasX + canvasW / 2) - 32);
        manager.spawnY = FlowComponent.clampPositionY(toWorldY(canvasY + canvasH / 2) - 10);
    }

    private void updateEdgePanning(int x, int y, float elapsedSeconds) {
        boolean isMoving = false;
        for (FlowComponent component : manager.getFlowItems()) {
            if (component.isVisible() && component.isBeingMoved()) {
                isMoving = true;
                break;
            }
        }
        if (!isMoving || !isInCanvas(x, y) || elapsedSeconds <= 0) {
            return;
        }

        float speed = EDGE_PAN_SPEED * Math.min(elapsedSeconds, 0.1F) / zoom;
        float oldX = cameraX;
        float oldY = cameraY;
        if (x < canvasX + EDGE_PAN_SIZE) {
            cameraX -= speed;
        } else if (x >= canvasX + canvasW - EDGE_PAN_SIZE) {
            cameraX += speed;
        }
        if (y < canvasY + EDGE_PAN_SIZE) {
            cameraY -= speed;
        } else if (y >= canvasY + canvasH - EDGE_PAN_SIZE) {
            cameraY += speed;
        }
        clampCamera();

        if (oldX != cameraX || oldY != cameraY) {
            // the mouse hasn't moved but the canvas below it has, let the moved command follow
            for (FlowComponent component : manager.getZLevelRenderingList()) {
                if (component.isVisible()) {
                    component.onDrag(toWorldX(x), toWorldY(y));
                }
            }
        }
    }

    public void handleMouseInput() {
        super.handleMouseInput();

        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            if (hasSpecialRenderer()) {
                getSpecialRenderer().onScroll(scroll);
                return;
            }

            int x = scaleX(Mouse.getEventX() * this.width / (float) this.mc.displayWidth) - guiLeft;
            int y = scaleY(this.height - Mouse.getEventY() * this.height / (float) this.mc.displayHeight - 1) - guiTop;

            if (!isCtrlKeyDown()) {
                int worldX = toWorldX(x);
                int worldY = toWorldY(y);
                for (FlowComponent component : manager.getZLevelRenderingList()) {
                    if (component.isVisible() && component.isOpen()
                            && component.getOpenMenuId() != -1
                            && CollisionHelper.inBounds(
                                    component.getX(),
                                    component.getY(),
                                    component.getComponentWidth(),
                                    component.getComponentHeight(),
                                    worldX,
                                    worldY)) {
                        component.doScroll(scroll);
                        return;
                    }
                }
            }

            if (isInCanvas(x, y)) {
                setZoom(scroll > 0 ? zoom * ZOOM_STEP : zoom / ZOOM_STEP, x, y);
            }
        }
    }

    private String getInfo(int maxWidth) {
        String ret = Localization.COMMANDS.toString() + ": " + manager.getFlowItems().size() + "  ";

        String path = "";
        FlowComponent component = manager.getSelectedComponent();

        if (component != null) {
            ret += "|";
        }
        while (component != null) {
            String nextPath = "> " + component.getName() + " " + path;
            if (getStringWidth(ret + nextPath) > maxWidth) {
                path = "... " + path;
                break;
            }
            path = nextPath;
            component = component.getParent();
        }
        ret += path;

        return ret;
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        x = scaleX(x);
        y = scaleY(y);

        super.mouseClicked(x, y, button);

        x -= guiLeft;
        y -= guiTop;

        if (hasSpecialRenderer()) {
            getSpecialRenderer().onClick(this, x - getSpecialOffsetX(), y - getSpecialOffsetY(), button);
            return;
        }

        if (contextComponent != null) {
            int item = getContextItemAt(x, y);
            if (item != -1) {
                if (button == 0) {
                    onContextClick(item);
                }
                return;
            }
            contextComponent = null;
        }

        int tool = getToolAt(x, y);
        if (tool != -1) {
            if (button == 0) {
                onToolClick(tool);
            }
            return;
        }

        boolean inCanvas = isInCanvas(x, y);
        if (inCanvas && button == 1) {
            // right clicking the title of a command shows the actions for it
            FlowComponent component = getComponentAt(toWorldX(x), toWorldY(y), true);
            if (component != null && !component.isEditingName()) {
                openContextMenu(component, x, y);
                return;
            }
        }
        if (inCanvas && button == 2) {
            startPanning(x, y, button);
            return;
        }

        boolean handled = false;
        if (inCanvas) {
            int worldX = toWorldX(x);
            int worldY = toWorldY(y);
            for (int i = 0; i < manager.getZLevelRenderingList().size(); i++) {
                FlowComponent itemBase = manager.getZLevelRenderingList().get(i);
                if (itemBase.isVisible() && itemBase.onClick(worldX, worldY, button)) {
                    manager.getZLevelRenderingList().remove(i);
                    manager.getZLevelRenderingList().add(0, itemBase);
                    handled = true;
                    break;
                }
            }
        }

        if (useButtons) {
            onClickButtonCheck(x, y, false);
        }

        if (inCanvas && !handled && (button == 0 || button == 1)) {
            startPanning(x, y, button);
        }
    }

    private void startPanning(int x, int y, int button) {
        isPanning = true;
        hasPanned = false;
        panButton = button;
        panStartX = x;
        panStartY = y;
        panStartCameraX = cameraX;
        panStartCameraY = cameraY;
    }

    private void onClickButtonCheck(int x, int y, boolean release) {
        for (int i = 0; i < manager.buttons.size(); i++) {
            TileEntityManager.Button guiButton = manager.buttons.get(i);
            if (guiButton.isVisible() && isOnButton(i, x, y) && guiButton.activateOnRelease() == release) {
                DataWriter dw = PacketHandler.getButtonPacketWriter();
                dw.writeData(i, DataBitHelper.GUI_BUTTON_ID);
                if (guiButton.onClick(dw)) {
                    PacketHandler.sendDataToServer(dw);
                }
                break;
            }
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long ticks) {
        x = scaleX(x);
        y = scaleY(y);

        super.mouseClickMove(x, y, button, ticks);

        x -= guiLeft;
        y -= guiTop;

        if (hasSpecialRenderer()) {
            getSpecialRenderer().onDrag(this, x - getSpecialOffsetX(), y - getSpecialOffsetY());
            return;
        }

        if (isPanning) {
            if (Math.abs(x - panStartX) > PAN_THRESHOLD || Math.abs(y - panStartY) > PAN_THRESHOLD) {
                hasPanned = true;
            }
            if (hasPanned) {
                cameraX = panStartCameraX - (x - panStartX) / zoom;
                cameraY = panStartCameraY - (y - panStartY) / zoom;
                clampCamera();
            }
            return;
        }

        int worldX = toWorldX(x);
        int worldY = toWorldY(y);
        for (FlowComponent itemBase : manager.getZLevelRenderingList()) {
            if (itemBase.isVisible()) {
                itemBase.onDrag(worldX, worldY);
            }
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        x = scaleX(x);
        y = scaleY(y);

        super.mouseMovedOrUp(x, y, button);

        x -= guiLeft;
        y -= guiTop;

        if (hasSpecialRenderer()) {
            getSpecialRenderer().onRelease(this, x - getSpecialOffsetX(), y - getSpecialOffsetY());
            return;
        }

        if (isPanning && button == panButton) {
            isPanning = false;
            if (!hasPanned && button == 1 && manager.getCurrentlyConnecting() != null) {
                // right clicking the empty canvas cancels the connection that is being made
                manager.setCurrentlyConnecting(null);
            }
        }

        if (useButtons) {
            onClickButtonCheck(x, y, true);
        }

        int worldX = toWorldX(x);
        int worldY = toWorldY(y);
        if (!manager.justSentServerComponentRemovalPacket) {
            for (FlowComponent itemBase : manager.getZLevelRenderingList()) {
                if (itemBase.isVisible()) {
                    itemBase.onRelease(worldX, worldY, button);
                }
            }
        }

        for (FlowComponent itemBase : manager.getZLevelRenderingList()) {
            if (itemBase.isVisible()) {
                itemBase.postRelease();
            }
        }

    }

    private AnimationController controller;
    private boolean doubleShiftFlag;
    private boolean useGreenScreen;
    private boolean useBlueScreen;
    private boolean usePinkScreen;
    private boolean useButtons = true;
    private boolean useInfo = true;
    private boolean useMouseOver = true;

    private List<SecretCode> codes = new ArrayList<SecretCode>();
    {
        codes.add(new SecretCode("animate") {

            @Override
            protected void trigger() {
                controller = new AnimationController(manager, 2);
            }
        });
        codes.add(new SecretCode("animslow") {

            @Override
            protected void trigger() {
                controller = new AnimationController(manager, 1);
            }
        });
        codes.add(new SecretCode("animfast") {

            @Override
            protected void trigger() {
                controller = new AnimationController(manager, 5);
            }
        });
        codes.add(new SecretCode("animrapid") {

            @Override
            protected void trigger() {
                controller = new AnimationController(manager, 20);
            }
        });
        codes.add(new SecretCode("animinstant") {

            @Override
            protected void trigger() {
                controller = new AnimationController(manager, 100);
            }
        });
        codes.add(new SecretCode("green") {

            @Override
            protected void trigger() {
                useGreenScreen = !useGreenScreen;
                useBlueScreen = false;
                usePinkScreen = false;
            }
        });
        codes.add(new SecretCode("blue") {

            @Override
            protected void trigger() {
                useBlueScreen = !useBlueScreen;
                useGreenScreen = false;
                usePinkScreen = false;
            }
        });
        codes.add(new SecretCode("pink") {

            @Override
            protected void trigger() {
                usePinkScreen = !usePinkScreen;
                useGreenScreen = false;
                useBlueScreen = false;
            }
        });
        codes.add(new SecretCode("buttons") {

            @Override
            protected void trigger() {
                useButtons = !useButtons;
            }
        });
        codes.add(new SecretCode("info") {

            @Override
            protected void trigger() {
                useInfo = !useInfo;
            }
        });
        codes.add(new SecretCode("mouse") {

            @Override
            protected void trigger() {
                useMouseOver = !useMouseOver;
            }
        });
    }

    private abstract class SecretCode {

        private final String code;
        private int triggerNumber;

        private SecretCode(String code) {
            this.code = code;
        }

        public boolean keyTyped(char c) {
            if (Character.isAlphabetic(c)) {
                if (code.charAt(triggerNumber) == c) {
                    if (triggerNumber + 1 > code.length() - 1) {
                        triggerNumber = 0;
                        trigger();
                    } else {
                        triggerNumber++;
                    }
                    return true;
                } else if (triggerNumber != 0) {
                    triggerNumber = 0;
                    keyTyped(c);
                }
            }

            return false;
        }

        protected abstract void trigger();
    }

    private boolean onNavigationKey(int k) {
        float distance = KEY_PAN_DISTANCE / zoom;
        switch (k) {
            case Keyboard.KEY_LEFT:
                cameraX -= distance;
                break;
            case Keyboard.KEY_RIGHT:
                cameraX += distance;
                break;
            case Keyboard.KEY_UP:
                cameraY -= distance;
                break;
            case Keyboard.KEY_DOWN:
                cameraY += distance;
                break;
            case Keyboard.KEY_EQUALS:
            case Keyboard.KEY_ADD:
                setZoom(zoom * ZOOM_STEP, canvasX + canvasW / 2, canvasY + canvasH / 2);
                return true;
            case Keyboard.KEY_MINUS:
            case Keyboard.KEY_SUBTRACT:
                setZoom(zoom / ZOOM_STEP, canvasX + canvasW / 2, canvasY + canvasH / 2);
                return true;
            case Keyboard.KEY_HOME:
                fitView();
                return true;
            default:
                return false;
        }
        clampCamera();
        return true;
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (hasSpecialRenderer()) {
            getSpecialRenderer().onKeyTyped(this, c, k);
        } else {
            if (contextComponent != null && k == Keyboard.KEY_ESCAPE) {
                contextComponent = null;
                return;
            }

            if (k == 54 && !doubleShiftFlag) {
                DataWriter dw = PacketHandler.getWriterForServerActionPacket();
                PacketHandler.sendDataToServer(dw);
                doubleShiftFlag = true;
            }

            for (FlowComponent itemBase : manager.getZLevelRenderingList()) {
                if (itemBase.isVisible() && itemBase.onKeyStroke(this, c, k) && k != 1) {
                    return;
                }
            }

            if (onNavigationKey(k)) {
                return;
            }

            boolean recognized = false;
            for (SecretCode code : codes) {
                if (code.keyTyped(c)) {
                    recognized = true;
                }
            }

            if (recognized) {
                return;
            }
        }

        super.keyTyped(c, k);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);

        for (FlowComponent flowComponent : manager.getFlowItems()) {
            flowComponent.onGuiClosed();
        }

        super.onGuiClosed();
    }

    private TileEntityManager manager;

    public TileEntityManager getManager() {
        return manager;
    }

    private boolean hasSpecialRenderer() {
        return getSpecialRenderer() != null;
    }

    private IInterfaceRenderer getSpecialRenderer() {
        return manager.specialRenderer;
    }

}
