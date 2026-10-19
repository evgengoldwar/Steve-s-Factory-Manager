package vswe.stevesfactory.interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
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
    private static final int BUTTON_SPACING = 18;
    private static final int BUTTON_START = 5;
    private static final int STATUS_BAR_HEIGHT = 12;

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

    private static final int TOOL_SIZE = 14;
    private static final int TOOL_SPACING = 16;
    private static final int TOOL_FIT = 0;
    private static final int TOOL_RESET_ZOOM = 1;
    private static final int TOOL_GRID = 2;
    private static final int TOOL_MINIMAP = 3;
    private static final int TOOL_HELP = 4;
    private static final int TOOL_COUNT = 5;

    private static final int MINIMAP_WIDTH = 120;
    private static final int MINIMAP_HEIGHT = 76;
    private static final int MINIMAP_PADDING = 20;

    // vanilla inventory colors
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_PANEL_DARK = 0xFF555555;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_INSET_DARK = 0xFF373737;
    private static final int COLOR_TEXT = 0x404040;

    private static final int COLOR_CANVAS = 0xFF2B2B2B;
    private static final int COLOR_CANVAS_OUTSIDE = 0xFF1C1C1C;
    private static final int COLOR_GRID_MINOR = 0xFF333333;
    private static final int COLOR_GRID_MAJOR = 0xFF3E3E3E;

    private static final int COLOR_TOOL = 0xFF6F6F6F;
    private static final int COLOR_TOOL_HOVER = 0xFF7E88BF;
    private static final int COLOR_TOOL_ACTIVE = 0xFF5A8A4A;
    private static final int COLOR_TOOL_LIGHT = 0xFFAAAAAA;
    private static final int COLOR_TOOL_DARK = 0xFF3F3F3F;
    private static final int COLOR_ICON = 0xFFE0E0E0;

    private static final int COLOR_CONNECTION_OUTLINE = 0xFF141414;
    private static final int COLOR_CONNECTION_PREVIEW = 0xFFFFFF55;

    private static final float CONNECTION_WIDTH = 1F;
    private static final float CONNECTION_WIDTH_HIGHLIGHT = 1.6F;
    private static final float CONNECTION_OUTLINE = 1F;
    private static final float CONNECTION_GLOW = 5F;
    private static final float FLOW_SPACING = 14F;
    private static final float FLOW_SPEED = 16F;
    private static final float FLOW_SPEED_HIGHLIGHT = 36F;
    private static final float FLOW_SIZE = 1.5F;

    // views are remembered per manager and per group while the game is running
    private static final Map<TileEntityManager, Map<Integer, float[]>> savedViews = new WeakHashMap<TileEntityManager, Map<Integer, float[]>>();
    private static boolean showGrid = true;
    private static boolean showMinimap = true;

    private int canvasX;
    private int canvasY;
    private int canvasW;
    private int canvasH;
    private int buttonRows = 13;

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

    private boolean isDraggingMinimap;
    private int minimapX;
    private int minimapY;
    private int minimapW;
    private int minimapH;
    private float minimapScale;
    private float minimapOriginX;
    private float minimapOriginY;

    @Override
    public void initGui() {
        xSize = Math.max(MIN_WIDTH, width - SCREEN_MARGIN * 2);
        ySize = Math.max(MIN_HEIGHT, height - SCREEN_MARGIN * 2);

        super.initGui();

        int bottom = ySize - BORDER - STATUS_BAR_HEIGHT;
        buttonRows = Math.max(1, (bottom - BUTTON_START - TileEntityManager.BUTTON_SIZE_H) / BUTTON_SPACING + 1);
        int buttonColumns = (manager.buttons.size() + buttonRows - 1) / buttonRows;

        canvasX = BUTTON_START + Math.max(1, buttonColumns) * BUTTON_SPACING;
        canvasY = BORDER;
        canvasW = xSize - BORDER - canvasX;
        canvasH = bottom - canvasY - 1;
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
        }

        if (hasSpecialRenderer()) {
            // the special interfaces are designed for the original size, keep them centered
            int offsetX = getSpecialOffsetX();
            int offsetY = getSpecialOffsetY();
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
            drawInset(canvasX - 1, canvasY - 1, canvasW + 2, canvasH + 2);
            drawColoredRect(canvasX, canvasY, canvasW, canvasH, COLOR_CANVAS);
        }
        // the components are depth sorted, they should never end up behind the background
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        updateEdgePanning(x, y, elapsedSeconds);

        int worldX = toWorldX(x);
        int worldY = toWorldY(y);

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

        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, Z_LEVEL_OVERLAY);
        if (showMinimap) {
            drawMinimap();
        }
        drawToolbar(x, y);

        bindTexture(COMPONENTS);
        if (useButtons) {
            for (int i = 0; i < manager.buttons.size(); i++) {
                TileEntityManager.Button button = manager.buttons.get(i);
                if (button.isVisible()) {
                    int buttonX = getButtonX(i);
                    int buttonY = getButtonY(i);
                    int srcButtonY = CollisionHelper.inBounds(
                            buttonX,
                            buttonY,
                            TileEntityManager.BUTTON_SIZE_W,
                            TileEntityManager.BUTTON_SIZE_H,
                            x,
                            y) ? 1 : 0;

                    drawTexture(
                            buttonX,
                            buttonY,
                            TileEntityManager.BUTTON_SRC_X,
                            TileEntityManager.BUTTON_SRC_Y + srcButtonY * TileEntityManager.BUTTON_SIZE_H,
                            TileEntityManager.BUTTON_SIZE_W,
                            TileEntityManager.BUTTON_SIZE_H);
                    drawTexture(
                            buttonX + 1,
                            buttonY + 1,
                            TileEntityManager.BUTTON_INNER_SRC_X,
                            TileEntityManager.BUTTON_INNER_SRC_Y + i * TileEntityManager.BUTTON_INNER_SIZE_H,
                            TileEntityManager.BUTTON_INNER_SIZE_W,
                            TileEntityManager.BUTTON_INNER_SIZE_H);
                }
            }
        }

        if (useInfo) {
            int textY = ySize - BORDER - STATUS_BAR_HEIGHT + 2;
            String zoomText = Localization.ZOOM.toString() + ": " + Math.round(zoom * 100) + "%";
            int zoomWidth = getStringWidth(zoomText);
            drawString(getInfo(xSize - BORDER * 4 - zoomWidth), BORDER + 1, textY, 1F, COLOR_TEXT);
            drawString(zoomText, xSize - BORDER - 1 - zoomWidth, textY, 1F, COLOR_TEXT);
        }
        GL11.glPopMatrix();

        if (useMouseOver) {
            if (useButtons) {
                for (int i = 0; i < manager.buttons.size(); i++) {
                    TileEntityManager.Button button = manager.buttons.get(i);
                    if (button.isVisible() && CollisionHelper.inBounds(
                            getButtonX(i),
                            getButtonY(i),
                            TileEntityManager.BUTTON_SIZE_W,
                            TileEntityManager.BUTTON_SIZE_H,
                            x,
                            y)) {
                        drawMouseOver(button.getMouseOver(), x, y);
                    }
                }
            }

            int tool = getToolAt(x, y);
            if (tool != -1) {
                drawMouseOver(getToolMouseOver(tool), x, y);
            } else if (isInCanvas(x, y) && !isOnMinimap(x, y)) {
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

    private void drawPanel(int x, int y, int w, int h) {
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
    }

    private void drawInset(int x, int y, int w, int h) {
        drawColoredRect(x, y, w - 1, 1, COLOR_INSET_DARK);
        drawColoredRect(x, y, 1, h - 1, COLOR_INSET_DARK);
        drawColoredRect(x + 1, y + h - 1, w - 1, 1, COLOR_PANEL_LIGHT);
        drawColoredRect(x + w - 1, y + 1, 1, h - 1, COLOR_PANEL_LIGHT);
    }

    private void drawCanvasBackground() {
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
    }

    private void drawToolbar(int mX, int mY) {
        for (int i = 0; i < TOOL_COUNT; i++) {
            int x = getToolX(i);
            int y = getToolY();
            boolean hover = CollisionHelper.inBounds(x, y, TOOL_SIZE - 1, TOOL_SIZE - 1, mX, mY);
            boolean active = (i == TOOL_GRID && showGrid) || (i == TOOL_MINIMAP && showMinimap);

            drawColoredRect(x, y, TOOL_SIZE, TOOL_SIZE, COLOR_OUTLINE);
            drawColoredRect(
                    x + 1,
                    y + 1,
                    TOOL_SIZE - 2,
                    TOOL_SIZE - 2,
                    hover ? COLOR_TOOL_HOVER : active ? COLOR_TOOL_ACTIVE : COLOR_TOOL);
            drawColoredRect(x + 1, y + 1, TOOL_SIZE - 2, 1, COLOR_TOOL_LIGHT);
            drawColoredRect(x + 1, y + 1, 1, TOOL_SIZE - 2, COLOR_TOOL_LIGHT);
            drawColoredRect(x + 1, y + TOOL_SIZE - 2, TOOL_SIZE - 2, 1, COLOR_TOOL_DARK);
            drawColoredRect(x + TOOL_SIZE - 2, y + 1, 1, TOOL_SIZE - 2, COLOR_TOOL_DARK);

            int iconX = x + 3;
            int iconY = y + 3;
            switch (i) {
                case TOOL_FIT:
                    // four corners
                    drawColoredRect(iconX, iconY, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX, iconY, 1, 3, COLOR_ICON);
                    drawColoredRect(iconX + 5, iconY, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX + 7, iconY, 1, 3, COLOR_ICON);
                    drawColoredRect(iconX, iconY + 7, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX, iconY + 5, 1, 3, COLOR_ICON);
                    drawColoredRect(iconX + 5, iconY + 7, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX + 7, iconY + 5, 1, 3, COLOR_ICON);
                    break;
                case TOOL_RESET_ZOOM:
                    // magnifying glass
                    drawColoredRect(iconX + 1, iconY, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX + 1, iconY + 4, 3, 1, COLOR_ICON);
                    drawColoredRect(iconX, iconY + 1, 1, 3, COLOR_ICON);
                    drawColoredRect(iconX + 4, iconY + 1, 1, 3, COLOR_ICON);
                    drawColoredRect(iconX + 4, iconY + 4, 2, 2, COLOR_ICON);
                    drawColoredRect(iconX + 6, iconY + 6, 2, 2, COLOR_ICON);
                    break;
                case TOOL_GRID:
                    for (int j = 0; j < 3; j++) {
                        drawColoredRect(iconX + 1 + j * 3, iconY, 1, 8, COLOR_ICON);
                        drawColoredRect(iconX, iconY + 1 + j * 3, 8, 1, COLOR_ICON);
                    }
                    break;
                case TOOL_MINIMAP:
                    drawColoredRect(iconX, iconY, 8, 1, COLOR_ICON);
                    drawColoredRect(iconX, iconY + 7, 8, 1, COLOR_ICON);
                    drawColoredRect(iconX, iconY, 1, 8, COLOR_ICON);
                    drawColoredRect(iconX + 7, iconY, 1, 8, COLOR_ICON);
                    drawColoredRect(iconX + 2, iconY + 2, 3, 2, COLOR_ICON);
                    drawColoredRect(iconX + 4, iconY + 4, 2, 2, COLOR_ICON);
                    break;
                case TOOL_HELP:
                    drawString("?", x + 4, y + 3, 1F, 0xE0E0E0);
                    break;
            }
        }
    }

    private int getToolX(int id) {
        return canvasX + canvasW - 3 - (TOOL_COUNT - id) * TOOL_SPACING;
    }

    private int getToolY() {
        return canvasY + 3;
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
            case TOOL_MINIMAP:
                return Localization.VIEW_MINIMAP.toString();
            default:
                return Localization.VIEW_HELP.toString() + "\n"
                        + Localization.VIEW_HELP_PAN.toString()
                        + "\n"
                        + Localization.VIEW_HELP_ZOOM.toString()
                        + "\n"
                        + Localization.VIEW_HELP_KEYS.toString()
                        + "\n"
                        + Localization.VIEW_HELP_CANCEL.toString();
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
            case TOOL_MINIMAP:
                showMinimap = !showMinimap;
                break;
        }
    }

    private void drawMinimap() {
        minimapW = Math.min(MINIMAP_WIDTH, canvasW / 3);
        minimapH = Math.min(MINIMAP_HEIGHT, canvasH / 3);
        minimapX = canvasX + canvasW - minimapW - 4;
        minimapY = canvasY + canvasH - minimapH - 4;

        float viewW = canvasW / zoom;
        float viewH = canvasH / zoom;
        float minX = cameraX;
        float minY = cameraY;
        float maxX = cameraX + viewW;
        float maxY = cameraY + viewH;
        for (FlowComponent component : manager.getFlowItems()) {
            if (component.isVisible()) {
                minX = Math.min(minX, component.getX());
                minY = Math.min(minY, component.getY());
                maxX = Math.max(maxX, component.getX() + component.getComponentWidth());
                maxY = Math.max(maxY, component.getY() + component.getComponentHeight());
            }
        }
        minX -= MINIMAP_PADDING;
        minY -= MINIMAP_PADDING;
        maxX += MINIMAP_PADDING;
        maxY += MINIMAP_PADDING;

        minimapScale = Math.min((minimapW - 2) / (maxX - minX), (minimapH - 2) / (maxY - minY));
        minimapOriginX = minimapX + 1 + ((minimapW - 2) - (maxX - minX) * minimapScale) / 2 - minX * minimapScale;
        minimapOriginY = minimapY + 1 + ((minimapH - 2) - (maxY - minY) * minimapScale) / 2 - minY * minimapScale;

        drawColoredRect(minimapX - 1, minimapY - 1, minimapW + 2, minimapH + 2, COLOR_OUTLINE);
        drawColoredRect(minimapX, minimapY, minimapW, minimapH, 0xE01A1A1A);

        for (FlowComponent component : manager.getFlowItems()) {
            if (component.isVisible()) {
                int color = component.isBeingMoved() ? 0xFFFFFF55 : component.isOpen() ? 0xFFE0E0E0 : 0xFF9A9A9A;
                int x = (int) (minimapOriginX + component.getX() * minimapScale);
                int y = (int) (minimapOriginY + component.getY() * minimapScale);
                int w = Math.max(1, (int) (component.getComponentWidth() * minimapScale));
                int h = Math.max(1, (int) (component.getComponentHeight() * minimapScale));
                drawColoredRect(x, y, w, h, color);
            }
        }

        int viewX = (int) (minimapOriginX + cameraX * minimapScale);
        int viewY = (int) (minimapOriginY + cameraY * minimapScale);
        int viewRight = (int) (minimapOriginX + (cameraX + viewW) * minimapScale);
        int viewBottom = (int) (minimapOriginY + (cameraY + viewH) * minimapScale);
        drawColoredRect(viewX, viewY, viewRight - viewX, 1, 0xFFFFFFFF);
        drawColoredRect(viewX, viewBottom - 1, viewRight - viewX, 1, 0xFFFFFFFF);
        drawColoredRect(viewX, viewY, 1, viewBottom - viewY, 0xFFFFFFFF);
        drawColoredRect(viewRight - 1, viewY, 1, viewBottom - viewY, 0xFFFFFFFF);
    }

    private boolean isOnMinimap(int x, int y) {
        return showMinimap && CollisionHelper.inBounds(minimapX, minimapY, minimapW, minimapH, x, y);
    }

    private void moveToMinimapLocation(int x, int y) {
        if (minimapScale > 0) {
            float worldX = (x - minimapOriginX) / minimapScale;
            float worldY = (y - minimapOriginY) / minimapScale;
            cameraX = worldX - canvasW / zoom / 2;
            cameraY = worldY - canvasH / zoom / 2;
            clampCamera();
        }
    }

    public void drawConnection(int[] pathX, int[] pathY, ConnectionOption.ConnectionType startType,
            ConnectionOption.ConnectionType endType, ConnectionOption colorOption, boolean highlight, boolean forward) {
        float[][] path;
        if (pathX.length == 2) {
            path = createCurve(pathX[0], pathY[0], startType, pathX[1], pathY[1], endType);
        } else {
            path = new float[2][pathX.length];
            for (int i = 0; i < pathX.length; i++) {
                path[0][i] = pathX[i];
                path[1][i] = pathY[i];
            }
        }

        int color = getConnectionColor(colorOption);
        float width = highlight ? CONNECTION_WIDTH_HIGHLIGHT : CONNECTION_WIDTH;
        if (highlight) {
            drawPath(path, CONNECTION_GLOW, (color & 0xFFFFFF) | 0x50000000);
        }
        drawPath(path, width + CONNECTION_OUTLINE * 2, COLOR_CONNECTION_OUTLINE);
        drawPath(path, width, color);
        drawFlow(path, forward, highlight ? FLOW_SPEED_HIGHLIGHT : FLOW_SPEED, getLighterColor(color));
    }

    public void drawConnectionPreview(int startX, int startY, ConnectionOption.ConnectionType startType, int endX,
            int endY) {
        ConnectionOption.ConnectionType endType = startType == ConnectionOption.ConnectionType.INPUT
                ? ConnectionOption.ConnectionType.OUTPUT
                : ConnectionOption.ConnectionType.INPUT;
        float[][] path = createCurve(startX, startY, startType, endX, endY, endType);
        drawPath(path, CONNECTION_WIDTH + CONNECTION_OUTLINE * 2, COLOR_CONNECTION_OUTLINE);
        drawPath(path, CONNECTION_WIDTH, COLOR_CONNECTION_PREVIEW);
        drawFlow(path, startType != ConnectionOption.ConnectionType.INPUT, FLOW_SPEED_HIGHLIGHT, COLOR_PANEL_LIGHT);
    }

    private static int getConnectionColor(ConnectionOption option) {
        if (option == null) {
            return 0xFFD8D8D8;
        }
        switch (option) {
            case CONDITION_TRUE:
                return 0xFF55FF55;
            case CONDITION_FALSE:
                return 0xFFFF5555;
            case INTERVAL:
                return 0xFF55FFFF;
            case FOR_EACH:
                return 0xFFFF55FF;
            case REDSTONE_PULSE_HIGH:
            case REDSTONE_PULSE_LOW:
            case REDSTONE_HIGH:
            case REDSTONE_LOW:
                return 0xFFD02828;
            case BUD:
            case BUD_PULSE_HIGH:
            case BUD_HIGH:
            case BUD_PULSE_LOW:
            case BUD_LOW:
                return 0xFFFFAA00;
            default:
                return 0xFFD8D8D8;
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

    private static float getDirectionX(ConnectionOption.ConnectionType type) {
        return type == ConnectionOption.ConnectionType.SIDE ? 1 : 0;
    }

    private static float getDirectionY(ConnectionOption.ConnectionType type) {
        if (type == ConnectionOption.ConnectionType.INPUT) {
            return -1;
        } else if (type == ConnectionOption.ConnectionType.OUTPUT) {
            return 1;
        } else {
            return 0;
        }
    }

    private static float[][] createCurve(float startX, float startY, ConnectionOption.ConnectionType startType,
            float endX, float endY, ConnectionOption.ConnectionType endType) {
        float dx = endX - startX;
        float dy = endY - startY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float handle = Math.max(12, Math.min(60, distance * 0.5F));

        float c1x = startX + getDirectionX(startType) * handle;
        float c1y = startY + getDirectionY(startType) * handle;
        float c2x = endX + getDirectionX(endType) * handle;
        float c2y = endY + getDirectionY(endType) * handle;

        int segments = Math.max(8, Math.min(40, (int) (distance / 6)));
        float[][] path = new float[2][segments + 1];
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float u = 1 - t;
            float a = u * u * u;
            float b = 3 * u * u * t;
            float c = 3 * u * t * t;
            float d = t * t * t;
            path[0][i] = a * startX + b * c1x + c * c2x + d * endX;
            path[1][i] = a * startY + b * c1y + c * c2y + d * endY;
        }
        return path;
    }

    private void beginShape(int color) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4f(
                ((color >> 16) & 0xFF) / 255F,
                ((color >> 8) & 0xFF) / 255F,
                (color & 0xFF) / 255F,
                ((color >> 24) & 0xFF) / 255F);
        Tessellator.instance.startDrawingQuads();
    }

    private void endShape(boolean blend, boolean cull) {
        Tessellator.instance.draw();
        if (cull) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        if (!blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawPath(float[][] path, float width, int color) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        beginShape(color);
        Tessellator tessellator = Tessellator.instance;
        float half = width / 2;
        for (int i = 0; i < path[0].length - 1; i++) {
            float x1 = guiLeft + path[0][i];
            float y1 = guiTop + path[1][i];
            float x2 = guiLeft + path[0][i + 1];
            float y2 = guiTop + path[1][i + 1];
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.001F) {
                continue;
            }
            // extend each segment by half its width so the joints between segments are filled
            float extendX = dx / length * half;
            float extendY = dy / length * half;
            float normalX = -extendY;
            float normalY = extendX;
            x1 -= extendX;
            y1 -= extendY;
            x2 += extendX;
            y2 += extendY;

            tessellator.addVertex(x1 + normalX, y1 + normalY, zLevel);
            tessellator.addVertex(x2 + normalX, y2 + normalY, zLevel);
            tessellator.addVertex(x2 - normalX, y2 - normalY, zLevel);
            tessellator.addVertex(x1 - normalX, y1 - normalY, zLevel);
        }
        endShape(blend, cull);
    }

    private void drawFlow(float[][] path, boolean forward, float speed, int color) {
        int count = path[0].length;
        float[] lengths = new float[count];
        for (int i = 1; i < count; i++) {
            float dx = path[0][i] - path[0][i - 1];
            float dy = path[1][i] - path[1][i - 1];
            lengths[i] = lengths[i - 1] + (float) Math.sqrt(dx * dx + dy * dy);
        }
        float total = lengths[count - 1];
        if (total < FLOW_SPACING) {
            return;
        }

        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        beginShape(color);
        Tessellator tessellator = Tessellator.instance;
        float half = FLOW_SIZE / 2;
        float offset = (Minecraft.getSystemTime() % 100000L) / 1000F * speed % FLOW_SPACING;
        int segment = 1;
        for (float distance = offset; distance < total; distance += FLOW_SPACING) {
            float target = forward ? distance : total - distance;
            if (!forward) {
                segment = 1;
            }
            while (segment < count - 1 && lengths[segment] < target) {
                segment++;
            }
            float segmentLength = lengths[segment] - lengths[segment - 1];
            float t = segmentLength < 0.001F ? 0 : (target - lengths[segment - 1]) / segmentLength;
            float x = guiLeft + path[0][segment - 1] + (path[0][segment] - path[0][segment - 1]) * t;
            float y = guiTop + path[1][segment - 1] + (path[1][segment] - path[1][segment - 1]) * t;

            tessellator.addVertex(x - half, y + half, zLevel);
            tessellator.addVertex(x + half, y + half, zLevel);
            tessellator.addVertex(x + half, y - half, zLevel);
            tessellator.addVertex(x - half, y - half, zLevel);
        }
        endShape(blend, cull);
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
        return BUTTON_START + (id / buttonRows) * BUTTON_SPACING;
    }

    private int getButtonY(int id) {
        return BUTTON_START + (id % buttonRows) * BUTTON_SPACING;
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

        int tool = getToolAt(x, y);
        if (tool != -1) {
            if (button == 0) {
                onToolClick(tool);
            }
            return;
        }

        if (isOnMinimap(x, y)) {
            isDraggingMinimap = true;
            moveToMinimapLocation(x, y);
            return;
        }

        boolean inCanvas = isInCanvas(x, y);
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
            if (guiButton.isVisible() && CollisionHelper.inBounds(
                    getButtonX(i),
                    getButtonY(i),
                    TileEntityManager.BUTTON_SIZE_W,
                    TileEntityManager.BUTTON_SIZE_H,
                    x,
                    y) && guiButton.activateOnRelease() == release) {
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

        if (isDraggingMinimap) {
            moveToMinimapLocation(x, y);
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

        isDraggingMinimap = false;

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
