package com.fluidsimulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import javolution.util.FastMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.fluidsimulator.gameobjects.ObjectInfo;
import com.fluidsimulator.gameobjects.Particle;
import com.fluidsimulator.gameobjects.Piece;
import com.fluidsimulator.gameobjects.Portal;
import com.fluidsimulator.gameobjects.Spring;
import com.fluidsimulator.utils.Vector2;

/**
 * This class is used as a generic simulation class containing common code.
 * Due to the experimental nature of the project, there is still redundant code
 * in the subclasses
 */
public class FluidSimulatorGeneric implements Screen, InputProcessor, ContactListener {

	public static final boolean IS_DESKTOP = true;
	public boolean DEBUG_ENABLED = false;
	
	// FPS Management
	private final float FIXED_DELTA_BOX2D = 1.0f / 30.0f;
	public int speedCounter;
	public float lastDeltaTime;
	public float timeStep;
	public float timeStep2;
	public float interpolation;
	public float nextGameTick;
	public float nextGameTick2;
	public boolean stepped;
	public int loops;
	public int loops2;
	public long time;
	
	// Tune these statics for platform specific behaviors
	public final int SIZE = 5460;
	public final int ANDROID_SIZE = 600;
	public final float EPSILON = 1.1920928955078125E-7f;
	public final float LINE_VELOCITY_FACTOR = IS_DESKTOP ? 0.03f : 0.03f;
	public final float LINE_DENSITY_FACTOR = IS_DESKTOP ? 0.2f : 0.2f;
	public final int WORLD_WIDTH = IS_DESKTOP ? 480 : 240;
	public final int WORLD_HEIGHT = IS_DESKTOP ? 320 : 180;
	// Box properties can be set by specific simulations like MPM
	public int BOX_WIDTH = IS_DESKTOP ? 480 : 240;
	public int BOX_HEIGHT = IS_DESKTOP ? 320 : 180;
	public int BOX_WIDTH_HALF = BOX_WIDTH / 2;
	public int BOX_HEIGHT_HALF = BOX_HEIGHT / 2;
	public final int INITIAL_HEIGHT = IS_DESKTOP ? 10 : 10;
	public final float TIMESTEP = 0.022f;
	public final float COLLISION_FORCE = IS_DESKTOP ? 0.05f : 0.05f;
	public final float RIGID_FLUID_COLLISION_FORCE = IS_DESKTOP ? 0.1f : 0.1f;
	public final int wpadding = IS_DESKTOP ? 10 : 10;
	public final int hpadding = IS_DESKTOP ? 10 : 10;
	public int prevHash = 1;
	
	// Particles arrays and spatial table
	public Particle[] particleArray = new Particle[IS_DESKTOP ? SIZE : ANDROID_SIZE];
	public final ArrayList<Particle> drawParticles = new ArrayList<Particle>();

	// Generic Framework objects and flags
	public FluidSimulatorStarter game;
	public GL20 gl = null;
	public boolean firstRender;
	public SpriteBatch batch;
	public BitmapFont font;
	public OrthographicCamera camera;
    public ShaderProgram defaultShader;
    public ShaderProgram refractionShader;
    public ShaderProgram defaultIMShader;
    public Texture bgTexture;
    public Sprite bgSprite;
    public Texture glossMapTexture;
    public Texture displacementMap;
    public Texture displacementMap2;
	public ImmediateModeRenderer20 immediateRenderer;
	public ImmediateModeRenderer20 irt2;
	public Renderer20 irt;
	public ShapeRenderer shapeRenderer;
	public boolean touching;
	public Random rnd = new Random();
	public Object fluidLock = new Object();
	public boolean multitouch;
	public int touched;
	public boolean exitApp;
	
	//3D - only used as an alternative rendering mode for the 2D simulation, because it's cool :)
	public PerspectiveCamera camera3D;
	public Environment environment;
    public ModelBatch modelBatch;
    public Model model;
    public ModelInstance instance;
    public Camera3DController camController;
    
	 /**  Pieces and portals are a utility developed for The Balance Game project available
	 *  on my Github https://github.com/omgware
	 *  They work perfectly fine with fluids too, might just need few modifications **/
	public ArrayList<Piece> pieceTemplates = new ArrayList<Piece>();
	public HashMap<Integer, Piece> pieces = new HashMap<Integer, Piece>();
	public ArrayList<Portal> portalIn = new ArrayList<Portal>();
	public ArrayList<Portal> portalOut = new ArrayList<Portal>();
	public final float PORTAL_FORCE_OUT = 200;
	
	// Box2D elements
	public Box2DDebugRenderer renderer;
	public World world;
	public int prevPieceHash = 1;
	public BodyDef def = new BodyDef();
	public FixtureDef fd = new FixtureDef();
	public Fixture tempFixture = null;
	public Piece newPiece;
	public Piece newPiece2;
	public Body logicHitBody = null;
	public boolean allowPortalTransferForce = true;
	public boolean allowOutOfWorldDestruction;
	public Vector2 vec1 = new Vector2();
	public Vector2 vec2 = new Vector2();
	public Vector2 vec3 = new Vector2();
	public Vector2 vec4 = new Vector2();
	public Vector2 bodyCollisionImpulse = new Vector2();
	public Particle hitParticle;
	
	// Box2D/Fluid collision
	public Vector2 collisionPoint = new Vector2();
	public Vector2 collisionNormal = new Vector2();
	public Piece collisionPiece;
	
	// Box2D Drag
	public Body groundBody;
	public Body hitBody = null;
	public MouseJoint mouseJoint;
	public Vector2 target = new Vector2();
	public boolean isDragging;

	// Simulation management
	public final int VELOCITY_CAP = 100;
	public ArrayList<Spring> springs;
	public FastMap<Integer, ArrayList<Integer>> springPresenceTable;
	public Iterator<Spring> springIter;
	public ArrayList<Particle> disposableParticles;
	public ArrayList<Particle> tempParticles;
	public Particle[] tempParticles2;
	public float deformation;
	public float dropRadius;
	public int dropRadiusPixel;
	public int dropRadiusPixel2;
	
	
	// Temp variables mostly for calculations and graphics processing purposes
	public int i;
	public int j;
	public int a;
	public int k;
	public int z;
	public int s;
	public int len;
	public int len2;
	public int w;
	public float q;
	public float qq;
	public float qqqq;
	public float p;
	public float pnear;
	public float pressure;
	public float presnear;
	public float changex;
	public float changey;
	public float factor;
	public float lastTotalPressure;
	public float totalPressure;
	public float dX;
	public float dY;
	public float relvx;
	public float relvy;
	public float D;
	public float distX;
	public float distY;
	public float vx;
	public float vy;
	public boolean waitingRehash;
	public float u;
	public float I;
	public Vector2 dx = new Vector2(0.0f, 0.0f);
	public Vector2 rij = new Vector2(0.0f, 0.0f);
	public Vector2 tempVect = new Vector2(0.0f, 0.0f);
	public Vector2 tempVect2 = new Vector2(0.0f, 0.0f);
	public Vector2 attract_vect = new Vector2(0.0f, 0.0f);
	public Vector2 repulse_vect = new Vector2(0.0f, 0.0f);
	public boolean isAttracting;
	public boolean isRepulsing;
	public float checkTime;
	public float checkTime2;
	public float checkTime3;
	public Texture dropTexture;
	public Texture dropTexture2;
	public Sprite dropSprite;
	public float spriteColor;
	public Vector3 dropCoordinate = new Vector3(0.0f, 0.0f, 0.0f);
	public Vector3 dropSize = new Vector3(0.0f, 0.0f, 0.0f);
	public Vector3 tempVect3 = new Vector3(0.0f, 0.0f, 0.0f);
	public Particle tempParticle;
	public Particle neighborP;
	public Particle mainP;
	public int neighborHash;
	public float tempFloat = 0;
	public long tempLong = 0;
	public boolean tempBoolean = false;
	public int tempInt = 0;
	public Spring tempSpring;
	public Mesh lineMesh;
	public float[] lineVertices;
	public int vertexIndex = 0;
	public Vector3 testPoint = new Vector3();
	public Vector2 testPoint2D = new Vector2();
	public Vector2 oldDragPos = new Vector2();
	public Vector2 dragVelocity = new Vector2();
	
	
	public FluidSimulatorGeneric(FluidSimulatorStarter fluidSimulatorStarter) {
		this.game = fluidSimulatorStarter;
		// LibGDX single batches cannot have a size more than 5460
		batch = new SpriteBatch(IS_DESKTOP ? 5460 : ANDROID_SIZE);
		font = new BitmapFont();
		camera = new OrthographicCamera(WORLD_WIDTH, WORLD_HEIGHT);
		camera.position.set(0, (WORLD_HEIGHT / 2) - 1, 0);
		immediateRenderer = new ImmediateModeRenderer20(SIZE*6, false, true, 0);
		irt = new Renderer20(SIZE*6, false, true, 1);
		irt2 = new ImmediateModeRenderer20(SIZE*11, false, true, 1);
		shapeRenderer = new ShapeRenderer(SIZE);
		renderer = new Box2DDebugRenderer(true, true, false, true, false, false);
		
		//3D
		camera3D = new PerspectiveCamera(67, WORLD_WIDTH, WORLD_HEIGHT);
		camera3D.position.set(0, 130f, 250f);
		camera3D.lookAt(0,150f,0);
		camera3D.near = 0.1f;
		camera3D.far = 500f;
		camera3D.update();
		ModelBuilder modelBuilder = new ModelBuilder();
//        model = modelBuilder.createSphere(5f, 5f, 5f, 4, 4, GL10.GL_TRIANGLES,
//                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
//                Usage.Position | Usage.Normal);
        model = modelBuilder.createBox(5f, 5f, 5f,
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            Usage.Position | Usage.Normal);
        instance = new ModelInstance(model);
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0, -0.8f, -0.2f));
        camController = new Camera3DController(camera3D);
        camController.setFluidSimulator(this);
		
		world = new World(new Vector2(0, -9.8f), false);
		world.setContactListener(this);
	}
	
	
	/** Create and save body templates **/
	public void setupPieces() {
		// Reallocate arrays
		pieceTemplates = new ArrayList<Piece>(60);
		portalIn = new ArrayList<Portal>(20);
		portalOut = new ArrayList<Portal>(20);
		/** Portal In vertical **/
		addNewPieceTemplate((new Piece(2.0f, 6.5f, 0, BodyType.StaticBody)).setSensor(true).setPortalIn(true)); // 0
		/** Portal Out vertical **/
		addNewPieceTemplate((new Piece(2.0f, 6.5f, 0, BodyType.StaticBody)).setSensor(true).setPortalOut(true)); // 1
		/** Portal In horizontal **/
		addNewPieceTemplate((new Piece(1.5f, 0.5f, 0, BodyType.StaticBody)).setSensor(true).setPortalIn(true)); // 2
		/** Portal Out horizontal **/
		addNewPieceTemplate((new Piece(1.5f, 0.5f, 0, BodyType.StaticBody)).setSensor(true).setPortalOut(true)); // 3
		/** Circle 20.0 DynamicBody **/
		addNewPieceTemplate(new Piece(20, BodyType.DynamicBody)); // 4
		/** Circle 0.2 DynamicBody **/
		addNewPieceTemplate(new Piece(0.5f, BodyType.DynamicBody)); // 5
		/** Large MAIN box **/
		addNewPieceTemplate(new Piece(15, 15f, 0, BodyType.DynamicBody)); // 6
		/** SECONDARY mini box **/
		addNewPieceTemplate(new Piece(3, 0.2f, 0, BodyType.KinematicBody)); // 7
		/** Mini Box **/
		addNewPieceTemplate(new Piece(0.5f, 0.5f, 0, BodyType.DynamicBody)); // 8
		/** Basket Piece **/
		addNewPieceTemplate(new Piece(5, BodyType.KinematicBody, true)); // 9
		/** Large Box **/
		addNewPieceTemplate(new Piece(1.2f, 1.2f, 0, BodyType.DynamicBody)); // 10
		/** Large Ball **/
		addNewPieceTemplate(new Piece(2, BodyType.DynamicBody)); // 11
		/** Arrow **/
		addNewPieceTemplate(new Piece(2f, 0.2f, 0, BodyType.KinematicBody)); // 12
		/** Ultra Large Box **/
		addNewPieceTemplate(new Piece(1.5f, 1.5f, 0, BodyType.DynamicBody)); // 13
		/** Mega Box **/
		addNewPieceTemplate(new Piece(60.0f, 10.0f, 0, BodyType.StaticBody)); // 14
		/** Large Planet **/
		addNewPieceTemplate(new Piece(30, BodyType.KinematicBody)); // 15
		/** Little Planet **/
		addNewPieceTemplate(new Piece(5, BodyType.KinematicBody)); // 16
		/** Circle 20.0 StaticBody **/
		addNewPieceTemplate(new Piece(20, BodyType.StaticBody)); // 17
		/** Wall Box **/
		addNewPieceTemplate(new Piece(10.0f, 250.0f, 0, BodyType.StaticBody)); // 18
	}
	
	public Piece addNewPieceTemplate(Piece piece) {
		pieceTemplates.add(piece);
		return piece;
	}
	
	public Piece getNewPieceInstanceFromTemplate(int templateIndex) {
		return (new Piece(pieceTemplates.get(templateIndex))).setIndex(templateIndex).setHash(prevPieceHash++);
	}
	
	public void createPortalIn(float x, float y, int bodyAngle, int portalAngle) {
		createBodyAndFixture(getNewPieceInstanceFromTemplate(0).setSensor(true).setPortalIn(true).setAngle(bodyAngle), x, y, true);
		portalIn.add(new Portal(tempFixture, portalAngle, PORTAL_FORCE_OUT));
	}
	
	public void createPortalOut(float x, float y, int bodyAngle, int portalAngle) {
		createBodyAndFixture(getNewPieceInstanceFromTemplate(1).setSensor(true).setPortalOut(true).setAngle(bodyAngle), x, y, true);
		portalOut.add(new Portal(tempFixture, portalAngle, PORTAL_FORCE_OUT));
	}
	
	public Body createBodyAndFixture(Piece piece, float x, float y, boolean addToPiecesList) {
		piece.pos.set(x, y);
		def.position.set(x, y);
		def.type = piece.type;
		def.angle = piece.angle;
		def.gravityScale = piece.gravityScale;
		def.bullet = piece.isBullet;
		Body body = world.createBody(def);
		if (body.getType() == BodyType.StaticBody) {
			if (piece.shapes != null) {
				for (Shape shape : piece.shapes) {
					fd.shape = shape;
					tempFixture = body.createFixture(fd);
				}
			}
			else {
				fd.shape = piece.shape;
				tempFixture = body.createFixture(fd);
			}
		}
		else {
			if (piece.shapes != null) {
				for (Shape shape : piece.shapes) {
					tempFixture = body.createFixture(shape, piece.density);
				}
			}
			else {
				tempFixture = body.createFixture(piece.shape, piece.density);
			}
		}
		tempFixture.setSensor(piece.isSensor);
		tempFixture.setFriction(piece.friction);
		tempFixture.setRestitution(piece.restitution);
		piece.setBody(body);
//		if (body.getType() == BodyType.DynamicBody || body.getType() == BodyType.KinematicBody) {
			tempFixture.getBody().setUserData(new ObjectInfo(piece));
			if (piece.isPortalAllowed) {
				((ObjectInfo)tempFixture.getBody().getUserData()).isPortalAllowed = true;
			}
//		}
		if (addToPiecesList)
			pieces.put(piece.hash, piece);
		return body;
	}
	
	
	public Piece createPiece(int templateId, float x, float y, float angle, float minVelocity, float maxVelocity, 
			boolean isPortalAllowed, boolean sticky, boolean addToPiecesList) {
		return createPiece(templateId, x, y, angle, minVelocity, maxVelocity, 0.1f, 0.2f, isPortalAllowed, sticky, addToPiecesList);
	}
	
	public Piece createPiece(int templateId, float x, float y, float angle, float minVelocity, float maxVelocity, 
			float friction, float restitution, boolean isPortalAllowed, boolean sticky, boolean addToPiecesList) {
		newPiece = getNewPieceInstanceFromTemplate(templateId);
		newPiece.setPhysics(friction, restitution, 1, false);
		newPiece.isPortalAllowed = isPortalAllowed;
		newPiece.setSticky(sticky);
		createBodyAndFixture(newPiece, x, y, addToPiecesList);
		tempFloat = rnd.nextBoolean() ? 1 : -1;
		newPiece.body.setLinearVelocity(tempFloat * (minVelocity + rnd.nextFloat() * maxVelocity * 0.8f), 5 + rnd.nextFloat() * maxVelocity);
		newPiece.body.setTransform(newPiece.body.getPosition(), (float)Math.toRadians(angle));
		return newPiece;
	}
	
	public void rigidBodiesLogic(float deltaTime) {
		world.step(FIXED_DELTA_BOX2D, 2, 2);
		/** PORTALS **/
		logicHitBody = null;
		if (portalIn.size() > 0 && portalOut.size() > 0 && portalIn.size() == portalOut.size()) {
			for (int i=0; i<portalIn.size(); i++) {
				world.QueryAABB(portalInCallback, portalIn.get(i).getX() - 2, portalIn.get(i).getY() - 6.5f, portalIn.get(i).getX() + 2, portalIn.get(i).getY() + 6.5f);
				world.QueryAABB(portalOutCallback, portalOut.get(i).getX() - 2, portalOut.get(i).getY() - 6.5f, portalOut.get(i).getX() + 2, portalOut.get(i).getY() + 6.5f);
			}
		}
	}
	
	public QueryCallback portalInCallback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			if (fixture.getBody().getType() != BodyType.StaticBody && !fixture.isSensor() && ((ObjectInfo)fixture.getBody().getUserData()).isPortalAllowed) {
				// Prevent portal looping
				if (!((ObjectInfo)fixture.getBody().getUserData()).hasTimePassed(300))
					return true;
				for (int i=0; i<portalIn.size(); i++) {
					if (portalIn.get(i).fixture.testPoint(fixture.getBody().getPosition().x, fixture.getBody().getPosition().y)) {
						logicHitBody = fixture.getBody();
						if (logicHitBody != null) {
							logicHitBody.setTransform(portalOut.get(i).getBody().getPosition(), 0);
							if (portalOut.get(i).normal != null) {
								// New velocity angle
								//System.out.println("vel: "+logicHitBody.getLinearVelocity().angle()+" norm: " + portalOut.get(i).normal.angle()+" angle: " + portalOut.get(i).angle);
								logicHitBody.setLinearVelocity(logicHitBody.getLinearVelocity().rotate(portalOut.get(i).angle - logicHitBody.getLinearVelocity().angle()));
								// Apply a little more linear force
								if (allowPortalTransferForce)
									logicHitBody.applyForceToCenter(portalOut.get(i).transferForce, true);
							}
							if (fixture.getBody().getUserData() != null)
								((ObjectInfo)fixture.getBody().getUserData()).updateTime();
//							handlePortalCallbackRendering(portalIn.get(i).getBody().getPosition(), portalOut.get(i).getBody().getPosition());
						}
					}
				}
			}
			return true;
		}
	};
	
	
	public QueryCallback portalOutCallback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			if (fixture.getBody().getType() != BodyType.StaticBody && !fixture.isSensor() && ((ObjectInfo)fixture.getBody().getUserData()).isPortalAllowed) {
				// Prevent portal looping
				if (!((ObjectInfo)fixture.getBody().getUserData()).hasTimePassed(300))
					return true;
				for (int i=0; i<portalIn.size(); i++) {
					if (portalOut.get(i).fixture.testPoint(fixture.getBody().getPosition().x, fixture.getBody().getPosition().y)) {
						logicHitBody = fixture.getBody();
						if (logicHitBody != null) {
							logicHitBody.setTransform(portalIn.get(i).getBody().getPosition(), 0);
							if (portalIn.get(i).normal != null) {
								// New velocity angle
								logicHitBody.setLinearVelocity(logicHitBody.getLinearVelocity().rotate(portalIn.get(i).normal.angle() - logicHitBody.getLinearVelocity().angle()));
								// Apply a little more linear force
								if (allowPortalTransferForce)
									logicHitBody.applyForceToCenter(portalIn.get(i).transferForce, true);
							}
							if (fixture.getBody().getUserData() != null)
								((ObjectInfo)fixture.getBody().getUserData()).updateTime();
//							handlePortalCallbackRendering(portalOut.get(i).getBody().getPosition(), portalIn.get(i).getBody().getPosition());
						}
					}
				}
			}
			return true;
		}
	};
	
	public class RayCastCallBack implements RayCastCallback {

		@Override
		public float reportRayFixture(Fixture fixture, com.badlogic.gdx.math.Vector2 point, com.badlogic.gdx.math.Vector2 normal, float fraction) {
			if (fixture.getBody().getUserData() != null) {
				collisionPiece = ((ObjectInfo)fixture.getBody().getUserData()).pieceInfo;
				collisionPoint.set(point);
				collisionNormal.set(normal);
			}
			return 0;
		}
		
	}
	
	public RayCastCallBack rayCastCallback = new RayCastCallBack();

	public QueryCallback callback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			logicHitBody = fixture.getBody();
			return false;
		}
	};
	
	public void portalFluidSolver(Particle particle, float deltaTime) {
		if (!particle.hasTimePassed(300))
			return;
		for (int i=0; i<portalIn.size(); i++) {
			if (portalIn.get(i).fixture.testPoint(particle.pos.x, particle.pos.y)) {
				particle.pos.set(portalOut.get(i).getBody().getPosition());
				particle.velocity.rotate(portalOut.get(i).angle - particle.velocity.angle());
				particle.velocity.scl(PORTAL_FORCE_OUT * deltaTime);
				particle.updateTime();
				return;
			}
		}
		for (int i=0; i<portalOut.size(); i++) {
			if (portalOut.get(i).fixture.testPoint(particle.pos.x, particle.pos.y)) {
				particle.pos.set(portalIn.get(i).getBody().getPosition());
				particle.velocity.rotate(portalIn.get(i).angle - particle.velocity.angle());
				particle.velocity.scl(PORTAL_FORCE_OUT * deltaTime);
				particle.updateTime();
				return;
			}
		}
	}
	

	public void box2dFluidSolverTest(Piece piece, Particle particle, float deltaTime) {
		logicHitBody = null;
		world.QueryAABB(callback, particle.pos.x - 0.01f, particle.pos.y - 0.01f, 
				particle.pos.x + 0.01f, particle.pos.y + 0.01f);
		if (logicHitBody != null) {
			
		}
	}

	public void capVelocity(Vector2 v) {
		if (v.x > VELOCITY_CAP)
			v.x = VELOCITY_CAP;
		else if (v.x < -VELOCITY_CAP)
			v.x = -VELOCITY_CAP;
		if (v.y > VELOCITY_CAP)
			v.y = VELOCITY_CAP;
		else if (v.y < -VELOCITY_CAP)
			v.y = -VELOCITY_CAP;
	}

	public void prepareDeleteOutOfBoundsParticles(Particle pi) {
		if ((pi.pos.x < -BOX_WIDTH / 2)
				|| (pi.pos.x > BOX_WIDTH / 2) || (pi.pos.y < 0)
				|| (pi.pos.y > BOX_HEIGHT)) {
			disposableParticles.add(pi);
		}
	}

	public void deleteOutOfBoundsParticles() {
		disposableParticles.clear();
	}

	@Override
	public void beginContact(Contact contact) {
		
		
	}

	@Override
	public void endContact(Contact contact) {
		
		
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		
		
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
		
		
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.CONTROL_LEFT) {
			isDragging = true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		
		return false;
	}

	@Override
	public void render(float delta) {
		
		
	}

	@Override
	public void resize(int width, int height) {
		
		
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(this);
		touching = false;
	}

	@Override
	public void hide() {
		if (disposableParticles != null)
			disposableParticles.clear();
		if (springs != null)
			springs.clear();
		if (springPresenceTable != null)
			springPresenceTable.clear();
		pieces.clear();
		portalIn.clear();
		portalOut.clear();
	}

	@Override
	public void pause() {
		
		
	}

	@Override
	public void resume() {
		
		
	}

	@Override
	public void dispose() {
		if (disposableParticles != null)
			disposableParticles.clear();
		if (springs != null)
			springs.clear();
		if (springPresenceTable != null)
			springPresenceTable.clear();
		pieceTemplates.clear();
		pieces.clear();
		portalIn.clear();
		portalOut.clear();
		
		lineVertices = null;
		if (lineMesh != null)
			lineMesh.dispose();
		if (dropTexture != null)
			dropTexture.dispose();
		if (dropTexture2 != null)
			dropTexture2.dispose();
		if (bgTexture != null)
			bgTexture.dispose();
		if (defaultShader != null)
			defaultShader.dispose();
		if (refractionShader != null)
			refractionShader.dispose();
		lineMesh = null;
		renderer.dispose();
		world.dispose();
		
		//3D
		model.dispose();
		modelBatch.dispose();
	}

}
