package com.demod.fbsr.def;

import java.awt.Dimension;
import java.awt.Rectangle;

public class MaterialDef extends ImageDef {

	private final int rows;
	private final int cols;
	private final Dimension tile;

	public MaterialDef(String path, ImageSheetLoader loader, Rectangle source, int rows, int cols) {
		super(path, loader, source, false);
		this.rows = rows;
		this.cols = cols;
		tile = new Dimension(source.width / cols, source.height / rows);
		setTrimmable(false);
	}

	public MaterialDef(String path, Rectangle source, int rows, int cols) {
		super(path, source, false);
		this.rows = rows;
		this.cols = cols;
		tile = new Dimension(source.width / cols, source.height / rows);
		setTrimmable(false);
	}

	public MaterialDef(ImageDef shared, int rows, int cols) {
		super(shared);
		this.rows = rows;
		this.cols = cols;
		tile = new Dimension(source.width / cols, source.height / rows);
		setTrimmable(false);
	}

	protected MaterialDef(MaterialDef shared) {
		super(shared);
		rows = shared.rows;
		cols = shared.cols;
		tile = shared.tile;
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public Dimension getTile() {
		return tile;
	}
}
