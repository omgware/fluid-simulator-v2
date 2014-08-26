package com.fluidsimulator.gameobjects;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.fluidsimulator.utils.Vector2;

public class Particle implements Poolable {
	
	public int hash;
	public float density;
	public float nearDensity;
	public float pressure;
	public float nearPressure;
	public float mass = 1;
	public Vector2 prevPos = new Vector2();
	public Vector2 pos = new Vector2();
	public Vector2 drawPos = new Vector2();
	public Vector2 velocity = new Vector2();
	public Vector2 prevVelocity = new Vector2();
	public Vector2 relativeVelocity = new Vector2();
	public Vector2 prevGridPos = new Vector2();
	public Vector2 posStar = new Vector2(); // a temp pos before applying constaints
	public Vector2 deltaPos = new Vector2();
	public Vector2 force = new Vector2();
	public Particle[] neighbors;
	public int neighborsSize;
	public float lamda;
	public float constraint;
	public int gridPosX;
	public int gridPosY;
	public int type = 1;
	public long spawnTime;
	public int rGrad = 255;
	public int gGrad = 255;
	public int bGrad = 255;
	public Body attachedBody;
	public float xchange;
	public float ychange;
	public float xs;
	public float ys;
	public float vxs;
	public float vys;
	
	// Rigid bodies collisions
	public boolean rigidBodyHit;
	public Vector2 contactPoint = new Vector2();
	public Vector2 contactNormal = new Vector2();
	public Vector2 lastSafePos = new Vector2();
	public int contactPieceHash;
	public int collisionPieceHash;
	// Portals
	public long timeCheck;
	
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
		this.pos.set(newPos.x, newPos.y);
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
	
	public void updateTime() {
		this.timeCheck = System.currentTimeMillis();
	}
	
	public boolean hasTimePassed(long delay) {
		return (System.currentTimeMillis() - timeCheck) >= delay;
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

	public Vector2 getVelocity() {
		return velocity;
	}

	public void setVelocity(Vector2 value) {
		this.velocity = value;
	}

	public Vector2 getForce() {
		return force;
	}

	public void setForce(Vector2 value) {
		this.force = value;
	}

	public Vector2 getPos() {
		return pos;
	}

	public void setPosStar(Vector2 value) {
		this.posStar = value;
	}

	public Vector2 getPosStar() {
		return posStar;
	}

	public float getLamda() {
		return lamda;
	}

	public void setLamda(float f) {
		this.lamda = f;
	}
	
	public float getConstraint() {
		return constraint;
	}

	public void setConstraint(float f) {
		this.constraint = f;
	}

	public Vector2 getDeltaPos() {
		return deltaPos;
	}

	public void setDeltaPos(Vector2 delta) {
		this.deltaPos = delta;
	}

	public void setDensity(float value) {
		this.density = value;
	}
}
