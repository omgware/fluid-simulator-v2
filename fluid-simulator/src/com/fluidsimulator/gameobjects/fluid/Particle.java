package com.fluidsimulator.gameobjects.fluid;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

public class Particle implements Poolable {
	
	public int hash;
	public float density;
	public float nearDensity;
	public float pressure;
	public float nearPressure;
	public float mass = 1;
	public Vector2 prevPos = new Vector2();
	public Vector2 pos = new Vector2();
	public Vector2 velocity = new Vector2();
	public Vector2 prevGridPos = new Vector2();
	public int type = 1;
	public long spawnTime;
	public int rGrad = 255;
	public int gGrad = 255;
	public int bGrad = 255;
	
	public Particle(float posX, float posY, int hash) {
		this.pos.set(posX, posY);
		this.spawnTime = System.currentTimeMillis();
		this.hash = hash;
	}
	
	public Particle setPos(float x, float y) {
		this.pos.set(x, y);
		this.spawnTime = System.currentTimeMillis();
		return this;
	}
	
	public Particle setPos(Vector2 vec) {
		this.pos.set(vec.x, vec.y);
		this.spawnTime = System.currentTimeMillis();
		return this;
	}
	
	public Particle(Vector2 newPos) {
		this.pos.set(newPos);
	}
	
	public void setRGrad(int value) {
		if (value < 0)
			value = 0;
		this.rGrad = value;
	}
	
	public void setGGrad(int value) {
		if (value < 0)
			value = 0;
		this.gGrad = value;
	}
	
	public void setBGrad(int value) {
		if (value < 0)
			value = 0;
		this.bGrad = value;
	}
	
	@Override
	public void reset() {
		this.pos.set(0, 0);
		this.prevPos.set(0, 0);
		this.velocity.set(0, 0);
		this.spawnTime = System.currentTimeMillis();
		this.density = 0;
		this.nearDensity = 0;
		this.pressure = 0;
		this.nearPressure = 0;
		this.mass = 1;
		this.rGrad = 255;
		this.gGrad = 255;
		this.bGrad = 255;
	}
}
