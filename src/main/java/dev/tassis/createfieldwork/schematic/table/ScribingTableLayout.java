package dev.tassis.createfieldwork.schematic.table;

/** Immutable shared menu/screen geometry. Coordinates are unscaled GUI pixels. */
public final class ScribingTableLayout {
    public static final int WIDTH = 232;
    public static final int HEIGHT = 254;
    public static final int TEXTURE_SIZE = 256;
    public static final Point SCHEMATIC = new Point(66, 64);
    public static final Point MEDIUM = new Point(176, 44);
    public static final Point OUTPUT = new Point(176, 94);
    public static final Rect TITLE = new Rect(14, 3, 204, 15);
    public static final Rect BLUEPRINT_PAPER = new Rect(10, 27, 128, 91);
    public static final Rect SCHEMATIC_FRAME = new Rect(55, 54, 38, 35);
    public static final Rect FLOW_LABEL = new Rect(146, 29, 76, 12);
    public static final Rect PROGRESS = new Rect(180, 66, 8, 24);
    public static final Rect SCHEMATIC_LABEL = new Rect(26, 98, 96, 12);
    public static final Rect STATUS = new Rect(10, 120, 212, 16);
    public static final Rect STATUS_TEXT = new Rect(22, 122, 194, 12);
    public static final Rect OPTIONS = new Rect(10, 140, 82, 18);
    public static final Rect PRINT = new Rect(140, 140, 80, 18);
    public static final Rect INVENTORY_LABEL = new Rect(35, 161, 162, 10);
    public static final Rect SETTINGS = new Rect(12, 28, 208, 90);
    public static final Rect SETTINGS_TITLE = new Rect(18, 31, 196, 11);
    public static final Rect MODE = new Rect(18, 49, 196, 18);
    public static final Rect BLOCK_ENTITIES = new Rect(18, 77, 196, 18);

    public record Point(int x, int y) {
        public Rect well() { return new Rect(x - 1, y - 1, 18, 18); }
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }

    public static Point tableSlot(int index) {
        return switch (index) {
            case 0 -> SCHEMATIC;
            case 1 -> MEDIUM;
            case 2 -> OUTPUT;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public static Point playerSlot(int index) {
        if (index < 0 || index >= 36) {
            throw new IndexOutOfBoundsException(index);
        }
        return new Point(35 + index % 9 * 18, index < 27 ? 172 + index / 9 * 18 : 230);
    }

    private ScribingTableLayout() {}
}
