package com.fluidsimulator;

public class MathUtils extends com.badlogic.gdx.math.MathUtils {

	public final static float map(final float val, final float fromMin, final float fromMax, final float toMin,
			final float toMax) {
//		final float mult = (val - fromMin) / (fromMax - fromMin);
//		final float res = toMin + mult * (toMax - toMin);
//		return res;
		return toMin + ((val - fromMin) / (fromMax - fromMin)) * (toMax - toMin);
	}
}
