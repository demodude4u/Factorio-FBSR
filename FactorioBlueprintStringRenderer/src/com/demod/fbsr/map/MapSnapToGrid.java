package com.demod.fbsr.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import com.demod.fbsr.Layer;

public class MapSnapToGrid extends MapRenderable implements MapBounded {

    private static final Color GRID_COLOR1 = Color.GREEN.darker().darker();
    private static final Color GRID_COLOR2 = Color.GREEN.darker();
    private static final BasicStroke GRID_STROKE1 = new BasicStroke(
        0.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {0.6f, 0.4f}, 0.8f
    );
    private static final BasicStroke GRID_STROKE2 = new BasicStroke(
        0.19f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {0.57f, 0.43f}, 0.785f
    );

    private MapRect bounds;

    public MapSnapToGrid(MapRect bounds) {
        super(Layer.BUILDING_PREVIEW_REFERENCE_BOX);

        this.bounds = bounds;
    }

    @Override
    public void render(Graphics2D g) {
        Rectangle2D.Double rect = new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g.setColor(GRID_COLOR1);
        g.setStroke(GRID_STROKE1);
        g.draw(rect);
        g.setColor(GRID_COLOR2);
        g.setStroke(GRID_STROKE2);
        g.draw(rect);
    }

    @Override
    public MapRect getBounds() {
        return bounds;
    }
}
