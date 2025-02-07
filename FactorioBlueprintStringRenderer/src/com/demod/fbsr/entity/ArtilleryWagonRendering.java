package com.demod.fbsr.entity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.demod.factorio.DataTable;
import com.demod.factorio.prototype.EntityPrototype;
import com.demod.fbsr.FPUtils;
import com.demod.fbsr.Layer;
import com.demod.fbsr.RenderUtils;
import com.demod.fbsr.Renderer;
import com.demod.fbsr.Sprite;
import com.demod.fbsr.WorldMap;
import com.demod.fbsr.bs.BSEntity;

public class ArtilleryWagonRendering extends RollingStockRendering {

	// XXX this is absolutely horrible
	private static final double[][] CANNON_BASE_SHIFTINGS = { { 0, 0.609375 }, { -0.046875, 0.59375 },
			{ -0.109375, 0.59375 }, { -0.171875, 0.578125 }, { -0.234375, 0.578125 }, { -0.28125, 0.5625 },
			{ -0.34375, 0.546875 }, { -0.40625, 0.546875 }, { -0.46875, 0.53125 }, { -0.515625, 0.515625 },
			{ -0.578125, 0.5 }, { -0.640625, 0.484375 }, { -0.6875, 0.46875 }, { -0.75, 0.453125 },
			{ -0.796875, 0.4375 }, { -0.859375, 0.40625 }, { -0.90625, 0.390625 }, { -0.953125, 0.375 },
			{ -1, 0.34375 }, { -1.0625, 0.328125 }, { -1.109375, 0.296875 }, { -1.15625, 0.28125 }, { -1.203125, 0.25 },
			{ -1.25, 0.234375 }, { -1.296875, 0.203125 }, { -1.328125, 0.171875 }, { -1.375, 0.140625 },
			{ -1.421875, 0.125 }, { -1.453125, 0.09375 }, { -1.5, 0.0625 }, { -1.53125, 0.03125 }, { -1.578125, 0 },
			{ -1.609375, -0.015625 }, { -1.640625, -0.046875 }, { -1.671875, -0.078125 }, { -1.703125, -0.109375 },
			{ -1.734375, -0.15625 }, { -1.765625, -0.1875 }, { -1.796875, -0.21875 }, { -1.8125, -0.25 },
			{ -1.84375, -0.28125 }, { -1.875, -0.328125 }, { -1.890625, -0.359375 }, { -1.90625, -0.390625 },
			{ -1.9375, -0.421875 }, { -1.953125, -0.46875 }, { -1.96875, -0.5 }, { -1.984375, -0.53125 },
			{ -2, -0.578125 }, { -2.015625, -0.609375 }, { -2.03125, -0.65625 }, { -2.03125, -0.6875 },
			{ -2.046875, -0.71875 }, { -2.046875, -0.765625 }, { -2.0625, -0.796875 }, { -2.0625, -0.828125 },
			{ -2.0625, -0.875 }, { -2.078125, -0.90625 }, { -2.078125, -0.9375 }, { -2.078125, -0.984375 },
			{ -2.078125, -1.015625 }, { -2.0625, -1.0625 }, { -2.0625, -1.09375 }, { -2.0625, -1.125 },
			{ -2.046875, -1.15625 }, { -2.0625, -1.203125 }, { -2.0625, -1.234375 }, { -2.078125, -1.265625 },
			{ -2.078125, -1.3125 }, { -2.078125, -1.34375 }, { -2.078125, -1.375 }, { -2.078125, -1.421875 },
			{ -2.078125, -1.453125 }, { -2.078125, -1.5 }, { -2.0625, -1.53125 }, { -2.0625, -1.5625 },
			{ -2.046875, -1.609375 }, { -2.046875, -1.640625 }, { -2.03125, -1.671875 }, { -2.015625, -1.71875 },
			{ -2.015625, -1.75 }, { -2, -1.796875 }, { -1.984375, -1.828125 }, { -1.96875, -1.859375 },
			{ -1.953125, -1.90625 }, { -1.921875, -1.9375 }, { -1.90625, -1.96875 }, { -1.890625, -2 },
			{ -1.859375, -2.046875 }, { -1.84375, -2.078125 }, { -1.8125, -2.109375 }, { -1.78125, -2.140625 },
			{ -1.75, -2.1875 }, { -1.71875, -2.21875 }, { -1.6875, -2.25 }, { -1.65625, -2.28125 }, { -1.625, -2.3125 },
			{ -1.59375, -2.34375 }, { -1.5625, -2.375 }, { -1.515625, -2.40625 }, { -1.484375, -2.4375 },
			{ -1.4375, -2.46875 }, { -1.40625, -2.5 }, { -1.359375, -2.53125 }, { -1.3125, -2.546875 },
			{ -1.265625, -2.578125 }, { -1.234375, -2.609375 }, { -1.1875, -2.625 }, { -1.140625, -2.65625 },
			{ -1.078125, -2.671875 }, { -1.03125, -2.703125 }, { -0.984375, -2.71875 }, { -0.9375, -2.75 },
			{ -0.890625, -2.765625 }, { -0.828125, -2.78125 }, { -0.78125, -2.8125 }, { -0.71875, -2.828125 },
			{ -0.671875, -2.84375 }, { -0.609375, -2.859375 }, { -0.546875, -2.875 }, { -0.5, -2.890625 },
			{ -0.4375, -2.90625 }, { -0.375, -2.90625 }, { -0.328125, -2.921875 }, { -0.265625, -2.9375 },
			{ -0.203125, -2.9375 }, { -0.140625, -2.953125 }, { -0.078125, -2.953125 }, { -0.015625, -2.96875 },
			{ 0.03125, -2.96875 }, { 0.09375, -2.953125 }, { 0.15625, -2.953125 }, { 0.21875, -2.9375 },
			{ 0.265625, -2.9375 }, { 0.328125, -2.921875 }, { 0.390625, -2.90625 }, { 0.453125, -2.890625 },
			{ 0.515625, -2.890625 }, { 0.5625, -2.875 }, { 0.625, -2.859375 }, { 0.671875, -2.84375 },
			{ 0.734375, -2.828125 }, { 0.78125, -2.796875 }, { 0.84375, -2.78125 }, { 0.890625, -2.765625 },
			{ 0.953125, -2.75 }, { 1, -2.71875 }, { 1.046875, -2.703125 }, { 1.09375, -2.671875 },
			{ 1.140625, -2.65625 }, { 1.1875, -2.625 }, { 1.234375, -2.609375 }, { 1.28125, -2.578125 },
			{ 1.328125, -2.546875 }, { 1.375, -2.515625 }, { 1.40625, -2.5 }, { 1.453125, -2.46875 },
			{ 1.484375, -2.4375 }, { 1.53125, -2.40625 }, { 1.5625, -2.375 }, { 1.609375, -2.34375 },
			{ 1.640625, -2.3125 }, { 1.671875, -2.28125 }, { 1.703125, -2.25 }, { 1.734375, -2.21875 },
			{ 1.765625, -2.1875 }, { 1.796875, -2.140625 }, { 1.828125, -2.109375 }, { 1.84375, -2.078125 },
			{ 1.875, -2.046875 }, { 1.890625, -2 }, { 1.921875, -1.96875 }, { 1.9375, -1.9375 }, { 1.953125, -1.90625 },
			{ 1.96875, -1.859375 }, { 1.984375, -1.828125 }, { 2, -1.796875 }, { 2.015625, -1.75 },
			{ 2.03125, -1.71875 }, { 2.046875, -1.671875 }, { 2.046875, -1.640625 }, { 2.0625, -1.609375 },
			{ 2.078125, -1.5625 }, { 2.078125, -1.53125 }, { 2.078125, -1.5 }, { 2.078125, -1.453125 },
			{ 2.09375, -1.421875 }, { 2.09375, -1.375 }, { 2.09375, -1.34375 }, { 2.078125, -1.3125 },
			{ 2.078125, -1.265625 }, { 2.078125, -1.234375 }, { 2.078125, -1.203125 }, { 2.078125, -1.171875 },
			{ 2.078125, -1.125 }, { 2.09375, -1.09375 }, { 2.09375, -1.0625 }, { 2.09375, -1.015625 },
			{ 2.09375, -0.984375 }, { 2.09375, -0.953125 }, { 2.09375, -0.90625 }, { 2.09375, -0.875 },
			{ 2.09375, -0.828125 }, { 2.078125, -0.796875 }, { 2.078125, -0.765625 }, { 2.0625, -0.71875 },
			{ 2.0625, -0.6875 }, { 2.046875, -0.640625 }, { 2.03125, -0.609375 }, { 2.03125, -0.578125 },
			{ 2.015625, -0.53125 }, { 2, -0.5 }, { 1.96875, -0.46875 }, { 1.953125, -0.421875 }, { 1.9375, -0.390625 },
			{ 1.921875, -0.359375 }, { 1.890625, -0.3125 }, { 1.875, -0.28125 }, { 1.84375, -0.25 },
			{ 1.8125, -0.21875 }, { 1.796875, -0.1875 }, { 1.765625, -0.140625 }, { 1.734375, -0.109375 },
			{ 1.703125, -0.078125 }, { 1.671875, -0.046875 }, { 1.625, -0.015625 }, { 1.59375, 0 }, { 1.5625, 0.03125 },
			{ 1.515625, 0.0625 }, { 1.484375, 0.09375 }, { 1.4375, 0.125 }, { 1.40625, 0.15625 },
			{ 1.359375, 0.171875 }, { 1.3125, 0.203125 }, { 1.265625, 0.234375 }, { 1.21875, 0.25 },
			{ 1.171875, 0.28125 }, { 1.125, 0.3125 }, { 1.078125, 0.328125 }, { 1.03125, 0.359375 },
			{ 0.984375, 0.375 }, { 0.921875, 0.390625 }, { 0.875, 0.421875 }, { 0.828125, 0.4375 },
			{ 0.765625, 0.453125 }, { 0.71875, 0.46875 }, { 0.65625, 0.484375 }, { 0.59375, 0.5 },
			{ 0.546875, 0.515625 }, { 0.484375, 0.53125 }, { 0.421875, 0.546875 }, { 0.359375, 0.5625 },
			{ 0.3125, 0.5625 }, { 0.25, 0.578125 }, { 0.1875, 0.59375 }, { 0.125, 0.59375 }, };

	private List<Point2D.Double> protoCannonBaseShifts;
	private FPRollingStockRotatedSlopedGraphics protoCannonBarrelPictures;
	private FPRollingStockRotatedSlopedGraphics protoCannonBasePictures;

	private double protoCannonBaseHeight;
	private double protoCannonBaseShiftWhenVertical;
	private double protoCannonBaseShiftWhenHorizontal;

	@Override
	public void createRenderers(Consumer<Renderer> register, WorldMap map, DataTable dataTable, BSEntity entity) {
		super.createRenderers(register, map, dataTable, entity);

		double orientation = entity.orientation.getAsDouble();

		// TODO sloped

		List<Sprite> cannonBaseSprites = protoCannonBasePictures.rotated.createSprites(orientation);
		List<Sprite> cannonBarrelSprites = protoCannonBarrelPictures.rotated.createSprites(orientation);

		// Old way
		int shiftIndex = (int) (FPUtils.projectedOrientation(orientation) * protoCannonBaseShifts.size());
		Point2D.Double cannonBaseShift = protoCannonBaseShifts.get(shiftIndex);
		RenderUtils.shiftSprites(cannonBaseSprites, cannonBaseShift);
		RenderUtils.shiftSprites(cannonBarrelSprites, cannonBaseShift);

		// TODO figure out new way of calculating cannon shift

		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, cannonBarrelSprites, entity, protoSelectionBox));
		register.accept(RenderUtils.spriteRenderer(Layer.HIGHER_OBJECT_UNDER, cannonBaseSprites, entity, protoSelectionBox));
	}

	@Override
	public void initFromPrototype(DataTable dataTable, EntityPrototype prototype) {
		super.initFromPrototype(dataTable, prototype);

		protoCannonBarrelPictures = new FPRollingStockRotatedSlopedGraphics(
				prototype.lua().get("cannon_barrel_pictures"));
		protoCannonBasePictures = new FPRollingStockRotatedSlopedGraphics(prototype.lua().get("cannon_base_pictures"));

		protoCannonBaseHeight = prototype.lua().get("cannon_base_height").optdouble(0.0);
		protoCannonBaseShiftWhenVertical = prototype.lua().get("cannon_base_shift_when_vertical").optdouble(0.0);
		protoCannonBaseShiftWhenHorizontal = prototype.lua().get("cannon_base_shift_when_horizontal").optdouble(0.0);

		protoCannonBaseShifts = new ArrayList<>();
		for (double[] shift : CANNON_BASE_SHIFTINGS) {
			protoCannonBaseShifts.add(new Point2D.Double(shift[0], shift[1]));
		}
	}
}
