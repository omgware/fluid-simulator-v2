package com.fluidsimulator.gameobjects;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.ArrayMap;

public class Piece {
	
	public int hash;
	public Shape shape;
	public ArrayList<Shape> shapes;
	public Vector2 pos;
	public BodyType type;
	public Body body;
	public float angle = 0;
	public float friction = 0.5f;
	public float restitution = 0.5f;
	public float gravityScale = 1.0f;
	public float density = 5.0f;
	public float radius;
	public int index;
	public boolean isBullet = false;
	public boolean isPortalAllowed = false;
	public boolean isSensor = false;
	public boolean isPortalIn = false;
	public boolean isPortalOut = false;
	public boolean isCreated = false;
	public boolean isEnemy = false;
	public boolean isSticky = false;
	public static final Vector2 ZERO_VEC = new Vector2();
	public ArrayMap<Integer, Particle> rayCastArray = new ArrayMap<Integer, Particle>();
	public Vector2 collisionImpulse = new Vector2();
	public Vector2 contactPoint = new Vector2();
	
	public Piece(Piece anotherPiece) {
		this.shape = anotherPiece.shape;
		if (anotherPiece.pos != null)
			this.pos = new Vector2(anotherPiece.pos);
		this.type = anotherPiece.type;
		this.body = anotherPiece.body;
		this.angle = anotherPiece.angle;
		this.friction = anotherPiece.friction;
		this.restitution = anotherPiece.restitution;
		this.gravityScale = anotherPiece.gravityScale;
		this.isPortalAllowed = anotherPiece.isPortalAllowed;
		this.isSensor = anotherPiece.isSensor;
		this.isPortalIn = anotherPiece.isPortalIn;
		this.isPortalOut = anotherPiece.isPortalOut;
		this.isCreated = anotherPiece.isCreated;
		this.isEnemy = anotherPiece.isEnemy;
		this.radius = anotherPiece.radius;
	}
	
	/** Void Shape */
	public Piece (BodyType type) {
		this.shape = null;
		this.pos = new Vector2();
		this.type = type;
	}
	
	public Piece (float radius, BodyType type, boolean isComposite) {
		ChainShape shape = new ChainShape();
//		ArrayList<Vector2> vectors = createArc(0, 0, radius, 180, 360, 0.07f, false);
//		vectors.add(new Vector2(vectors.get(vectors.size() - 1).x - 1, vectors.get(vectors.size() - 1).y));
//		vectors.add(0, new Vector2(vectors.get(0).x+1, vectors.get(0).y));
//		vectors.addAll(createArc(0, 0, radius-1, 0, -180, 0.07f, true));
//		Vector2[] finalVectors = new Vector2[vectors.size()];
//		((ChainShape)shape).createLoop(vectors.toArray(finalVectors));
//		vectors.clear();
//		finalVectors = null;
		this.shape = shape;
		this.pos = new Vector2();
		this.type = type;
	}
	
	/** Circle Shape */
	public Piece (float radius, BodyType type) {
		this.shape = new CircleShape();
		((CircleShape)this.shape).setRadius(radius);
		this.pos = new Vector2();
		this.type = type;
		this.radius = radius;
	}
	
	/** Box Shape */
	public Piece (float halfWidth, float halfHeight, float angle, BodyType type) {
		this.shape = new PolygonShape();
		((PolygonShape)this.shape).setAsBox(halfWidth, halfHeight, ZERO_VEC, 0);
		this.pos = new Vector2();
		this.type = type;
		this.angle = (float)Math.toRadians(angle);
	}
	
	/** Polygon Shape **/
	public Piece (BodyType type, boolean flag, Vector2... pos) {
		this.shape = new PolygonShape();
		((PolygonShape)this.shape).set(pos);
		this.pos = new Vector2();
		this.type = type;
	}
	
	/** Chain Shape */
	public Piece (BodyType type, Vector2... pos) {
		this.shape = new ChainShape();
		((ChainShape)this.shape).createLoop(pos);
		this.pos = null;
		this.type = type;
	}
	
	public ArrayList<Vector2> createArc(float centerX, float centerY, float radius, float angleFrom, float angleTo, float precision, boolean revert) {
		ArrayList<Vector2> vectors = new ArrayList<Vector2>();
		float angleDiff = Math.abs(angleTo - angleFrom);
		int steps = Math.round(angleDiff * precision);
		float angle = angleFrom;
		float px = (float) (centerX + radius * Math.cos(Math.toRadians(angle)));
		float py = (float) (centerY + radius * Math.sin(Math.toRadians(angle)));
		vectors.add(new Vector2(px, py));
		for (int i=1; i<=steps; i++) {
			if (revert)
				angle = angleFrom - angleDiff / steps * i;
			else
				angle = angleFrom + angleDiff / steps * i;
			vectors.add(new Vector2(centerX + radius * (float)Math.cos(Math.toRadians(angle)), centerY + radius * (float)Math.sin(Math.toRadians(angle))));
		}
		return vectors;
	}
	
	public Piece setPhysics(float friction, float restitution, float gravityScale, boolean isSensor) {
		this.friction = friction;
		this.restitution = restitution;
		this.gravityScale = gravityScale;
		this.isSensor = isSensor;
		return this;
	}
	
	public Piece setSensor(boolean value) {
		this.isSensor = value;
		return this;
	}
	
	public Piece setBody(Body body) {
		this.body = body;
		isCreated = true;
		return this;
	}
	
	public Piece setPortalIn(boolean value) {
		this.isPortalIn = value;
		return this;
	}
	
	public Piece setPortalOut(boolean value) {
		this.isPortalOut = value;
		return this;
	}
	
	public Piece setPortalAllowed(boolean value) {
		this.isPortalAllowed = value;
		return this;
	}
	
	public Piece setEnemy(boolean value) {
		this.isEnemy = value;
		return this;
	}
	
	public Piece setAngle(float angle) {
		this.angle = (float)Math.toRadians(angle);
		return this;
	}
	
	public void addShape(Shape newShape) {
		if (shapes == null)
			shapes = new ArrayList<Shape>();
		shapes.add(newShape);
	}
	
	public Piece setIndex(int value) {
		this.index = value;
		return this;
	}
	
	public Piece setHash(int value) {
		this.hash = value;
		return this;
	}
	
	public Piece setSticky(boolean value) {
		this.isSticky = value;
		return this;
	}
}