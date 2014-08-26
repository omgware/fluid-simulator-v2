package com.fluidsimulator.pbf;

import com.badlogic.gdx.math.Vector2;


public class Box {
	private float left;
	private float right;
	private float top;
	private float bottom;

	public Box(float left, float right, float bottom, float top) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}

	public boolean isInBox(Vector2 position) {
		if (position.x < left || position.x > right)
			return false;
		if (position.y < bottom || position.y > top)
			return false;
		return true;
	}

	public void forceInsideBox(Vector2 position, Vector2 velocity) {
		float padding = 0.01f;
		if (position.x < left + padding) {
			position.x = left + padding;
			if (velocity != null && velocity.x < 0)
				velocity.x *= -0.5f;
//				velocity.x = 0;
		}
		if (position.x > right - padding) {
			position.x = right - padding;
			if (velocity != null && velocity.x > 0)
				velocity.x *= -0.5f;
//				velocity.x = 0;
		}
		if (position.y < bottom + padding) {
			position.y = bottom + padding;
			if (velocity != null && velocity.y < 0)
				velocity.y *= -0.5f;
//				velocity.y = 0;
		}
		if (position.y > top - padding) {
			position.y = top - padding;
			if (velocity != null && velocity.y > 0)
				velocity.y *= -0.5f;
//				velocity.y = 0;
		}
	}
}
