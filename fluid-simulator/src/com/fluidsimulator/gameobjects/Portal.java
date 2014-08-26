package com.fluidsimulator.gameobjects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;

public class Portal {
	public Fixture fixture;
	public Vector2 normal = null;
	public Vector2 transferForce = new Vector2(0,0);
	public int angle = 0;
	public float forceOut = 500.0f;
	
	public Portal(Fixture fixture, int angle, float forceOut) {
		this.forceOut = forceOut;
		this.angle = angle;
		this.fixture = fixture;
		this.normal = new Vector2(1, 0);
		this.normal.rotate(angle);
		this.normal.nor();
		this.transferForce.set(this.normal);
		this.transferForce.scl(forceOut);
	}
	
	public Portal(Fixture fixture) {
		this.fixture = fixture;
	}
	
	public Body getBody() {
		return fixture.getBody();
	}
	
	public float getX() {
		return fixture.getBody().getPosition().x;
	}
	
	public float getY() {
		return fixture.getBody().getPosition().y;
	}
	
}
