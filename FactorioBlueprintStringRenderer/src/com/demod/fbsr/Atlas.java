package com.demod.fbsr;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Atlas {

    public static class AtlasRef {
        private boolean valid = false;
        private Atlas atlas = null;
        private Rectangle rect = null;
        private Point trim = null;

        public AtlasRef() {
        }

        public boolean isValid() {
            return valid;
        }

        void set(Atlas atlas, Rectangle rect, Point trim) {
            valid = true;
            this.atlas = atlas;
            this.rect = rect;
            this.trim = trim;
        }

        void reset() {
            valid = false;
            atlas = null;
            rect = null;
            trim = null;
        }

        public Atlas getAtlas() {
            return atlas;
        }

        public Rectangle getRect() {
            return rect;
        }

        public Point getTrim() {
            return trim;
        }
    }

    private final AtlasPackage atlasPackage;
    private final int id;
    private final int width;
    private final int height;

    private final boolean iconMode;
    private final int iconSize;
    private final int iconColumns;
    private final int iconMaxCount;
    
    private final BufferedImage image;
    private final Quadtree occupied;
    private final List<Dimension> failedPackingSizes;
    private int iconCount = 0;

    private Atlas(AtlasPackage atlasPackage, int id, int width, int height, BufferedImage image, Quadtree occupied, boolean iconMode, int iconSize) {
        this.atlasPackage = atlasPackage;
        this.id = id;
        this.width = width;
        this.height = height;
        this.image = image;
        this.occupied = occupied;
        this.failedPackingSizes = new ArrayList<>();

        this.iconMode = iconMode;
        this.iconSize = iconSize;
        iconColumns = (width / iconSize);
        iconMaxCount = (height / iconSize) * iconColumns;
    }

    public AtlasPackage getAtlasPackage() {
        return atlasPackage;
    }

    public int getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    public BufferedImage getImage() {
        return image;
    }

    public Quadtree getOccupied() {
        return occupied;
    }

    public boolean isIconMode() {
        return iconMode;
    }

    public int getIconCount() {
        return iconCount;
    }

    public void setIconCount(int iconCount) {
        this.iconCount = iconCount;
    }

    public int getIconColumns() {
        return iconColumns;
    }

    public int getIconMaxCount() {
        return iconMaxCount;
    }

    public int getIconSize() {
        return iconSize;
    }

    public static Atlas init(AtlasPackage atlasPackage, int id, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Quadtree occupied = new Quadtree(0, new Rectangle(0, 0, width, height));
        return new Atlas(atlasPackage, id, width, height, image, occupied, false, -1);
    }

    public static Atlas initIcons(AtlasPackage atlasPackage, int id, int width, int height, int iconSize) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        return new Atlas(atlasPackage, id, width, height, image, null, true, iconSize);
    }

    public static Atlas load(AtlasPackage atlasPackage, int id, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        return new Atlas(atlasPackage, id, width, height, image, null, false, -1);
    }

    public List<Dimension> getFailedPackingSizes() {
        return failedPackingSizes;
    }
}