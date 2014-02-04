package vswe.stevesfactory;


import net.minecraft.util.StatCollector;

public enum Localization {
    DETECTOR_MENU,
    REQUIRE_ALL_TARGETS,
    REQUIRE_ONE_TARGET,
    RUN_SHARED_ONCE,
    RUN_ONE_PER_TARGET,
    SELECTED,
    OVERFLOW_MENU,
    NO_DETECTOR_ERROR,
    NO_OVERFLOW_ERROR,
    OVERFLOW_INFO,
    CONTAINER_TYPE_MENU,
    CRAFTING_MENU,
    EMITTER_MENU,
    NO_EMITTER_ERROR,
    REQUIRES_ALL,
    IF_ANY,
    WHITE_LIST,
    BLACK_LIST,
    EMPTY_TANK,
    FILLED_TANK,
    STRONG_POWER,
    WEAK_POWER,
    SEQUENTIAL,
    SPLIT,
    INTERVAL_MENU,
    INTERVAL_INFO,
    SECOND,
    INVENTORY_MENU,
    NO_INVENTORY_ERROR,
    ITEM_MENU,
    DAMAGE_VALUE,
    NO_CONDITION_ERROR,
    BUCKETS,
    MILLI_BUCKETS,
    LIQUIDS_MENU,
    USE_ALL,
    REVERSED,
    LOOP_ORDER_MENU,
    VALUE_ORDER_MENU,
    REDSTONE_NODE_MENU,
    NO_NODE_ERROR,
    DO_EMIT_PULSE,
    SECONDS,
    TICKS,
    PULSE_MENU,
    RECEIVERS_MENU,
    NO_RECEIVER_ERROR,
    REDSTONE_OUTPUT_MENU,
    REDSTONE_STRENGTH,
    DIGITAL_TOGGLE,
    NO_REDSTONE_SIDES_ERROR,
    REDSTONE_SIDES_MENU,
    REDSTONE_SIDES_INFO,
    UPDATE_SIDES_INFO,
    REDSTONE_SIDES_MENU_TRIGGER,
    UPDATE_SIDES_MENU,
    NO_SIDES_ERROR,
    INVERT_SELECTION,
    REDSTONE_STRENGTH_MENU,
    REDSTONE_STRENGTH_INFO,
    THROUGH,
    INVALID_REDSTONE_RANGE_ERROR,
    REDUNDANT_REDSTONE_RANGE_ERROR,
    REDSTONE_STRENGTH_MENU_CONDITION,
    CONNECTIONS_MENU,
    FAIR_SPLIT,
    EMPTY_PINS,
    SPLIT_MENU,
    SPECIFY_AMOUNT,
    DELETE,
    DELETE_ITEM_SELECTION,
    GO_BACK,
    CANCEL,
    EMPTY_WHITE_LIST_ERROR,
    TANK_MENU,
    NO_TANK_ERROR,
    ACTIVATE,
    DEACTIVATE,
    ACTIVATE_LONG,
    DEACTIVATE_LONG,
    NO_DIRECTION_ERROR,
    ALL_SLOTS,
    ID_RANGE,
    ALL_SLOTS_LONG,
    ID_RANGE_LONG,
    INVALID_RANGE,
    ADVANCED_MODE,
    SIMPLE_MODE,
    ADVANCED_MODE_LONG,
    SIMPLE_MODE_LONG,
    USE_ID,
    INVERT,
    UPDATE_BLOCK_MENU,
    META,
    GLOBAL_VALUE_SET,
    VARIABLE_MENU,
    NOT_DECLARED_ERROR,
    ALREADY_DECLARED_ERROR,
    VARIABLE_CONTAINERS_MENU,
    VARIABLE_LIST,
    VARIABLE_ELEMENT,
    LOOP_VARIABLE_MENU,
    LIST_NOT_DECLARED,
    ELEMENT_NOT_DECLARED
    ;



    private String name;

    private Localization() {
        String[] split = super.toString().split("_");
        this.name = "";
        for (String s : split) {
            this.name += s.charAt(0) + s.substring(1).toLowerCase();
        }
    }

    public String toString() {
        return StatCollector.translateToLocal("gui." + StevesFactoryManager.UNLOCALIZED_START + name);
    }
}