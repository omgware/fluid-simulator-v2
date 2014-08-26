package com.fluidsimulator;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.fluidsimulator.gameobjects.Particle;
import com.fluidsimulator.gameobjects.Piece;
import com.fluidsimulator.gameobjects.SpatialTable;
import com.fluidsimulator.pbf.Box;
import com.fluidsimulator.utils.Vector2;

public class FluidSimulator extends FluidSimulatorGeneric {
	
	// Thread Management
	// TODO: Real multithreaded behavior
	private Timer timer;
	private Thread thread;
	private Thread thread2;
	private Thread thread3;
	private Thread thread4;
	private boolean threadRunning = true;
	
	// FPS Management
	private float TICKS_PER_SECOND = IS_DESKTOP ? 60 : 50;
	private float SKIP_TICKS = 1 / TICKS_PER_SECOND;
	private float FIXED_DELTA = 1.0f / 60.0f;
	private final int MAX_FRAMESKIP = 1;
	private int speedMultiplier = IS_DESKTOP ? 2 : 1;
	
	// Tune theses for platform specific behaviors
	private float GRAVITY_FORCE = IS_DESKTOP ? -50.0f : -50.0f;
	private final Vector2 gravityVect = new Vector2(0.0f, GRAVITY_FORCE);

	// Particles arrays and spatial table
	private SpatialTable particles = new SpatialTable(40, 40, IS_DESKTOP ? SIZE : ANDROID_SIZE) {

		@Override
		protected int posX(Particle value) {
			return (int) MathUtils.map(value.pos.x, -BOX_WIDTH_HALF, BOX_WIDTH_HALF, 0, 40-.001f);
//			return (int) ((value.pos.x + BOX_WIDTH_HALF + 0.3f) / CELL_SIZE);
		}

		@Override
		protected int posY(Particle value) {
//			return (int) ((value.pos.y + BOX_HEIGHT_HALF + 0.3f) / CELL_SIZE);
			return (int) MathUtils.map(value.pos.y, INITIAL_HEIGHT, BOX_HEIGHT, 0, 40-.001f);
		}
		
		@Override
		protected int prevPosX(Particle value) {
			return (int) ((value.prevPos.x + BOX_WIDTH_HALF + 0.3f) / CELL_SIZE);
		}

		@Override
		protected int prevPosY(Particle value) {
			return (int) ((value.prevPos.y + BOX_HEIGHT_HALF + 0.3f) / CELL_SIZE);
		}

		@Override
		protected void setPrevGridPos(Particle value, int x, int y) {
			value.prevGridPos.set(x, y);
		}

		@Override
		protected int getPrevGridPosX(Particle value) {
			return (int)value.prevGridPos.x;
		}

		@Override
		protected int getPrevGridPosY(Particle value) {
			return (int)value.prevGridPos.y;
		}

		@Override
		protected void savePosX(Particle value, int x) {
			value.gridPosX = x;
		}

		@Override
		protected void savePosY(Particle value, int y) {
			value.gridPosY = y;
		}

		@Override
		protected int getPosX(Particle value) {
			return value.gridPosX;
		}

		@Override
		protected int getPosY(Particle value) {
			return value.gridPosY;
		}

		@Override
		protected int getHash(Particle value) {
			return value.hash;
		}
	};

	// Simulation management
	// Most of these can be tuned at runtime with F1-F10 and keys 1-0 (no numpad)
	private int CELL_SIZE = 2;
	private float H = 60.0f;
	private float H2 = H * H;
	private float RAD = 8.0f;
	private float VISC = 0.001f;
	private float MULTIPLIER = 50 / RAD;
	private float K = 0.004f;
	private float K_ = 1.01f;
	private float SIGMA = 2;
	private float P0 = 310.0f;
	private final float ATTRACT_FORCE = 1.66f;
	private float ATTRACT_RANGE = H2 / 2;
	private final float REPULSE_FORCE = H / 2;
	private float REPULSE_RANGE = H2 / 4;
	private float DAMPING = 0.99f;
	private float EMITTER_FORCE = 10;
	private float EMITTER_SIZE = 5;
	private final float STICKINESS_DIST = 5.0f;
	private final float STICKINESS_DIST2 = STICKINESS_DIST * STICKINESS_DIST;
	private float K_STICKINESS = 7.0f;
	private final float WET_FRICTION = 0.0f;
	private final float WET_RESTITUTION = 1.0f;
	private final int PARTICLE_SIZE = 5;
	private float dropRadiusK = 1.5f;

	// Temp variables mostly for calculations and graphics processing purpose
	private float[] qArray = new float[particles.MAX_NEIGHBORS];
	private float[] qqArray = new float[particles.MAX_NEIGHBORS];
	private float deltaTime2 = FIXED_DELTA * FIXED_DELTA;

	// Modes
	private int emitType = 2;
	private boolean expandMode = false;
	private boolean massMode = false;
	private boolean slowMotion = false;
	private boolean crazyMode = false;
	private boolean hudEnabled = IS_DESKTOP ? true : false;
	private boolean pbfEnabled = true; //Leave this to true
	private boolean shapes = IS_DESKTOP ? false : true;
	private boolean render3D = false;
	private boolean linesMode = false;
	private boolean smokeMode = false;
	private boolean whiteBackground = false;
	private boolean particleRendering = true;
	private boolean enableBox2DCollisions = false;
	private boolean glowMode = false;
	private boolean bgMode = false;

	public FluidSimulator(FluidSimulatorStarter fluidSimulatorStarter) {
		super(fluidSimulatorStarter);
		setupPieces();
	}

	private void createWorld() {
		dropRadius = 0.1f + dropRadiusK;
		dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
		dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
		dropTexture = new Texture("data/fluid_drop_64.png");
		dropTexture2 = new Texture("data/fluid_drop_64.png");
		dropSprite = new Sprite(dropTexture);
		dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
		if (IS_DESKTOP) {
			disposableParticles = new ArrayList<Particle>(SIZE);
		}
		defaultShader = new ShaderProgram(Gdx.files.internal("data/shaders/default.vert").readString(), 
				Gdx.files.internal("data/shaders/default.frag").readString());
		if (!defaultShader.isCompiled()) {
			Gdx.app.log("SHADER_LOG", "couldn't compile default shader: " + defaultShader.getLog());
		}
		defaultIMShader = new ShaderProgram(Gdx.files.internal("data/shaders/defaultim.vert").readString(), 
				Gdx.files.internal("data/shaders/defaultim.frag").readString());
		if (!defaultIMShader.isCompiled()) {
			Gdx.app.log("SHADER_LOG", "couldn't compile default IM shader: " + defaultIMShader.getLog());
		}
		ShaderProgram.pedantic = false;
		refractionShader = new ShaderProgram(Gdx.files.internal("data/shaders/refract.vert").readString(), 
				Gdx.files.internal("data/shaders/refract.frag").readString());
		if (!refractionShader.isCompiled()) {
			Gdx.app.log("SHADER_LOG", "couldn't compile refraction shader: " + refractionShader.getLog());
		}
		irt.setShader(defaultIMShader);
		bgTexture = new Texture("data/bg.png");
		glossMapTexture = new Texture("data/gloss_map2.png");
		displacementMap = new Texture("data/water1.png");
		displacementMap2 = new Texture("data/water2.png");
		bgSprite = new Sprite(bgTexture);
		bgSprite.setBounds(0, -1, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() + 1);

		// On Android populate directly
		if (!IS_DESKTOP) {
			for (float j = INITIAL_HEIGHT + hpadding + 2; j < BOX_HEIGHT - 2; j += 5.5f) {
				for (float i = -BOX_WIDTH / 3; i < BOX_WIDTH / 3; i += 5.5f) {
					particles.add(new Particle(i, j, prevHash++));
					tempParticle = particles.get(particles.size() - 1);
					tempParticle.type = (emitType);
					if (particles.size() >= ANDROID_SIZE)
						break;
				}
				if (particles.size() >= ANDROID_SIZE)
					break;
			}
		}
		
//		createPiece(17, 0, 150, 0, 0, 0, false, true, true);
		createPiece(17, 100, 240, 0, 0, 0, false, true, true);
//		createPortalIn(-140, 65, 0, 0, false, true);
//		createPortalOut(-140, 220, 0, 0, false, true);
		// Boxes
		createPiece(6, 0, 160, 0, 0, 0, false, false, true);
		createPiece(14, -150, 200, 0, 0, 0, false, false, true);
		createPiece(14, 0, 140, 0, 0, 0, false, false, true);
		createPiece(14, -170, 60, 90, 0, 0, false, false, true);
		// Ball
		createPiece(4, 100, 100, 0, -20, 0, false, false, true);
		
		// Ground cage
		createPiece(18, -BOX_WIDTH/2 + 7, BOX_HEIGHT/2, 0, 0, 0, 0.2f, 0.5f, false, false, true);
		createPiece(18, BOX_WIDTH/2 - 10, BOX_HEIGHT/2, 0, 0, 0, 0.2f, 0.5f, false, false, true);
		createPiece(18, 0, INITIAL_HEIGHT, 90, 0, 0, 0.2f, 0.5f, false, false, true);
		createPiece(18, 0, BOX_HEIGHT - 10, 90, 0, 0, 0.2f, 0.5f, false, false, true);
		
		
		// Ground body for mousejoint
		BodyDef bodyDef = new BodyDef();
//		bodyDef.type = BodyType.StaticBody;
		groundBody = world.createBody(bodyDef);
	}

	@Override
	public void show() {
		super.show();
//		particles.initialize();
		createWorld();

		// THREAD
		thread = new Thread() {
			public void run() {
				while (threadRunning) {
					stepped = false;
					loops = 0;
					float deltaTime = System.currentTimeMillis() / 1000f;
					timeStep += deltaTime;
//					System.out.println(timeStep);
					while (timeStep > nextGameTick
							&& loops < MAX_FRAMESKIP) {
						for (speedCounter = 0; speedCounter < speedMultiplier; speedCounter++) {
								performLogic(FIXED_DELTA);
								if (IS_DESKTOP) {
									prepareDeleteOutOfBoundsParticles();
									deleteOutOfBoundsParticles();
								}
						}
						stepped = true;
						nextGameTick += slowMotion ? SKIP_TICKS * 3 : SKIP_TICKS;
						loops++;
					}
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {}
				}
			}
		};
//		thread.start();
		timer = new Timer(true);
		TimerTask task = new TimerTask() {
			public void run() {
				performLogic(FIXED_DELTA);
				if (IS_DESKTOP) {
					prepareDeleteOutOfBoundsParticles();
					deleteOutOfBoundsParticles();
				}
			}
		};
//		timer.schedule(task, 0, 5);
//		thread2 = new Thread() {
//			public void run() {
//				while (threadRunning) {
//					loops2 = 0;
//					deltaTime2 = Gdx.graphics.getDeltaTime();
//					timeStep2 += deltaTime2;
//					while (timeStep2 > nextGameTick2
//							&& loops2 < MAX_FRAMESKIP) {
//						rigidBodiesLogic(deltaTime2);
//						if (enableBox2DCollisions) {
//							for (Piece piece : pieces.values()) {
//								if (piece.isSensor || piece.body.getFixtureList().size() <= 0)
//									continue;
//								piece.collisionImpulse.set(0, 0);
//								synchronized (particles) {
//									for (i=0; i<len; i++) {
//		//								if (particles.get(i).pos.dst2(piece.pos) < 1000)
//												box2dFluidSolver(piece, particles.get(i), deltaTime2);
//		//								box2dFluidSolverTest(piece, particles.get(i), deltaTime);
//									}
//								}
//								if (piece.collisionImpulse.x != 0 && piece.collisionImpulse.y != 0) {
//									piece.body.applyForce(piece.collisionImpulse, piece.contactPoint);
//	//								if (piece.shape instanceof CircleShape)
//										piece.body.setAngularDamping(0.8f);
//								}
//							}
//						}
//						nextGameTick2 += slowMotion ? SKIP_TICKS * 3 : SKIP_TICKS;
//					}
//				}
//			}
//		};
//		thread2.start();
		
		initializePBF();
	}

	// TODO: Query only particles within collision range
	public void box2dFluidSolver(Piece piece, Particle particle, float deltaTime) {
		if (pbfEnabled) {
			particle.posStar.x *= MULTIPLIER;
			particle.posStar.y *= MULTIPLIER;
			particle.velocity.x *= MULTIPLIER;
			particle.velocity.y *= MULTIPLIER;
		}
		else {
			particle.posStar.set(particle.pos);
		}
		if (particle.rigidBodyHit && particle.contactPieceHash == piece.hash
				&& (piece.body.getPosition().dst2(particle.contactPoint) > piece.body.getPosition().dst2(particle.posStar)
						|| particle.posStar.dst2(particle.contactPoint) > STICKINESS_DIST2)) {
			particle.rigidBodyHit = false;
		}

		vec2.set(particle.posStar.x + (deltaTime * particle.velocity.x) * 1.5f,
				particle.posStar.y + (deltaTime * particle.velocity.y) * 1.5f);
		
		vec4.set(particle.prevPos);
		vec4.sub(piece.body.getPosition());
		vec4.nor();
		vec4.scl(100);
		vec3.set(particle.prevPos);
		vec3.add(vec4);
		
		if (/*!particle.rigidBodyHit && */vec3.dst2(vec2) > 0
				&& (piece.body.getFixtureList().get(0).testPoint(vec2)
					/*|| piece.body.getFixtureList().get(0).testPoint(vec2.x + 0.5f, vec2.y + 0.5f)
					|| piece.body.getFixtureList().get(0).testPoint(vec2.x - 0.5f, vec2.y + 0.5f)
					|| piece.body.getFixtureList().get(0).testPoint(vec2.x - 0.5f, vec2.y - 0.5f)
					|| piece.body.getFixtureList().get(0).testPoint(vec2.x + 0.5f, vec2.y - 0.5f)*/)) {

			collisionPiece = null;
			world.rayCast(rayCastCallback, vec3, vec2);
//			if (piece.body.getUserData() != null) {
//				collisionPiece = ((ObjectInfo)piece.body.getUserData()).pieceInfo;
//				collisionPoint.set(vec2);
//				vec2.set(particle.velocity);
//				vec2.rotate(180);
//				vec2.nor();
//				collisionNormal.set(vec2);
//			}
			if (collisionPiece != null) {
				tempVect.set(particle.velocity);
//			
				particle.posStar.set(particle.lastSafePos);
				if (piece.type == BodyType.DynamicBody) {
					particle.posStar.set(collisionPoint);
					particle.posStar.add(collisionNormal.x * 0.5f, collisionNormal.y * 0.5f);
				}

				particle.rigidBodyHit = true;
				particle.contactPoint.set(collisionPoint);
				particle.contactNormal.set(collisionNormal);
				particle.contactPieceHash = piece.hash;
//				for (Particle p : particles.nearby(particle)) {
//					p.rigidBodyHit = true;
//					p.contactPoint.set(collisionPoint);
//					p.contactNormal.set(collisionNormal);
//					p.contactPieceHash = piece.hash;
//				}

				particle.relativeVelocity.set(particle.velocity.x - particle.prevVelocity.x, 
						particle.velocity.y - particle.prevVelocity.y);
				// Vnormal = (Vrelative . normal) * normal
				vec2.set(collisionNormal);
				vec2.scl(particle.velocity.dot(collisionNormal));
				// Vtangent = Vrelative - Vnormal
				vec3.set(particle.velocity.x - vec2.x, particle.velocity.y - vec2.y);

				// Calculate impulse
				vec1.set(vec2.x * (WET_RESTITUTION + piece.restitution) - ((WET_FRICTION - piece.friction) * vec3.x), 
						vec2.y * (WET_RESTITUTION + piece.restitution) - ((WET_FRICTION - piece.friction) * vec3.y));
				tempVect.sub(vec1);
				// Apply impulse
				particle.velocity.set(tempVect);
				
				// Re-update position
				particle.prevPos.set(particle.posStar);
				particle.posStar.set(particle.posStar.x + (deltaTime * particle.velocity.x),
						particle.posStar.y + (deltaTime * particle.velocity.y));

				if (piece.type == BodyType.DynamicBody) {
					vec4.set(collisionNormal);
					vec4.rotate(180);
					vec4.scl(45000);
//					vec1.scl(200);
//					vec4.set(vec1);
//					System.out.println(vec1.len());
					piece.collisionImpulse.add(vec4);
					piece.contactPoint.set(collisionPoint);
				}
				
				// Particle still inside the body
				if (piece.body.getFixtureList().get(0).testPoint(particle.posStar)) {
//					System.out.println("asd");
					particle.posStar.set(particle.lastSafePos);
					particle.prevPos.set(particle.posStar);
				}
			}
		}
		else {
//			if (!piece.body.getFixtureList().get(0).testPoint(particle.prevPos))
				particle.lastSafePos.set(particle.prevPos);
			
			if (particle.rigidBodyHit && particle.contactPieceHash == piece.hash) {
				if (piece.isSticky) {
					tempVect.set(particle.velocity);
					vec2.set(particle.posStar);
					vec2.sub(particle.contactPoint);
					vec2.nor();
					// Calculate stickiness
					tempFloat = particle.posStar.dst(particle.contactPoint);
					tempFloat = K_STICKINESS * tempFloat * (1 - (tempFloat / STICKINESS_DIST));
					if (tempFloat > 0) {
						vec2.scl(-tempFloat);
						tempVect.add(vec2);
					}
	//					System.out.println(vec2.len());
					particle.velocity.set(tempVect);
					// Re-update position
					particle.prevPos.set(particle.posStar);
					particle.posStar.set(particle.posStar.x + (deltaTime * particle.velocity.x),
							particle.posStar.y + (deltaTime * particle.velocity.y));
				}
			}
		}
		if (pbfEnabled) {
			particle.posStar.x /= MULTIPLIER;
			particle.posStar.y /= MULTIPLIER;
			particle.velocity.x /= MULTIPLIER;
			particle.velocity.y /= MULTIPLIER;
		}
	}

	public  void performLogic(float deltaTime) {
		if (!firstRender)
			return;
		
		
		if (IS_DESKTOP && !isDragging)
			spawnParticles(deltaTime);

		len = particles.size();
		time = System.currentTimeMillis();
		if (enableBox2DCollisions) {
//			for (Piece piece : pieces.values()) {
//				if (piece.isSensor || piece.body.getFixtureList().size() <= 0)
//					continue;
//				piece.collisionImpulse.set(0, 0);
//				for (i=0; i<len; i++) {
//					box2dFluidSolver(piece, particles.get(i), deltaTime);
////					box2dFluidSolverTest(piece, particles.get(i), deltaTime);
//				}
//				if (piece.collisionImpulse.x != 0 && piece.collisionImpulse.y != 0)
//					piece.body.applyForce(piece.collisionImpulse, piece.contactPoint);
//			}
			rigidBodiesLogic(deltaTime);
		}
		if (DEBUG_ENABLED)
			System.out.print("\nrigid: " + (System.currentTimeMillis() - time));
		
		if (!expandMode) {
			if (!pbfEnabled) {
				for (i=0; i<len; i++) {
					mainP = particles.get(i);
	//				applyGravity(mainP);
					mainP.velocity.scl(DAMPING);
					mainP.prevPos.set(mainP.pos);
					mainP.pos.set(mainP.pos.x + (deltaTime * mainP.velocity.x),
							mainP.pos.y + (deltaTime * mainP.velocity.y));
					
					mainP.xs = MULTIPLIER*mainP.pos.x;
					mainP.ys = MULTIPLIER*mainP.pos.y;
					mainP.vxs = MULTIPLIER*mainP.velocity.x;
					mainP.vys = MULTIPLIER*mainP.velocity.y;
					mainP.xchange = 0;
					mainP.ychange = 0;
				}
			}
		}

		time = System.currentTimeMillis();
		if (!pbfEnabled) {
			if (waitingRehash)
				particles.rehash();
			waitingRehash = !waitingRehash;
		}
		if (DEBUG_ENABLED)
			System.out.print("\t rehash: " + (System.currentTimeMillis() - time));

//		hashLocations();
		time = System.currentTimeMillis();
		if (!pbfEnabled) {
			relaxation2(deltaTime);
//			particleArray = particles.table.toArray(particleArray);
//			for (i=0; i<len; i++) {
////				mainP = particles.get(i);
//				mainP = particleArray[i];
//				mainP.density += i+i;
//
//				tempParticles = particles.nearby(mainP);
//				len2 = tempParticles.size();
//				tempParticles2 = particles.nearby(mainP);
//				len2 = particles.lastSizeNearby();
////				len2 = 40;
//	    		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//	    			neighborP = tempParticles2[a];
//	    			neighborP.density += mainP.density;
////					mainP.density += a+a;
//	    		}
//	    		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//	    			neighborP = tempParticles2[a];
//	    			neighborP.density += mainP.density;
////					mainP.density += a+a;
//	    		}
//			}
		}
		else
			UpdateFluid(deltaTime);
//		pbf.UpdateFluid(particles, box);
		if (DEBUG_ENABLED)
			System.out.print("\t fluid: " + (System.currentTimeMillis() - time));

//		if (enableBox2DCollisions) {
////			rigidBodiesLogic(deltaTime);
//			for (Piece piece : pieces.values()) {
//				if (piece.isSensor || piece.body.getFixtureList().size() <= 0)
//					continue;
//				piece.collisionImpulse.set(0, 0);
//				for (i=0; i<len; i++) {
////					if (particles.get(i).pos.dst2(piece.pos) < 1000)
//						box2dFluidSolver(piece, particles.get(i), deltaTime);
////					box2dFluidSolverTest(piece, particles.get(i), deltaTime);
//				}
//				if (piece.collisionImpulse.x != 0 && piece.collisionImpulse.y != 0) {
//					piece.body.applyForce(piece.collisionImpulse, piece.contactPoint);
////					if (piece.shape instanceof CircleShape)
//						piece.body.setAngularDamping(0.8f);
//				}
//			}
//		}

		time = System.currentTimeMillis();
		if (!pbfEnabled) {
			for (i=0; i<len; i++) {
				mainP = particles.get(i);
				mainP.prevVelocity.set(mainP.velocity);
				mainP.velocity.set((mainP.pos.x - mainP.prevPos.x) / deltaTime,
						(mainP.pos.y - mainP.prevPos.y) / deltaTime);
				if (!enableBox2DCollisions)
					wallCollision(mainP);
				attract(mainP);
				repulse(mainP);
				applyGravity(mainP);
				if (enableBox2DCollisions) {
					portalFluidSolver(mainP, deltaTime);
				}
				capVelocity(mainP.velocity);
				if (IS_DESKTOP)
					prepareDeleteOutOfBoundsParticles(mainP);
				 if (enableBox2DCollisions) {
					for (Piece piece : pieces.values()) {
						if (piece.isSensor || piece.body.getFixtureList().size <= 0)
							continue;
						piece.collisionImpulse.set(0, 0);
	//							if (particles.get(i).pos.dst2(piece.pos) < 1000)
								box2dFluidSolver(piece, particles.get(i), deltaTime);
	//							box2dFluidSolverTest(piece, particles.get(i), deltaTime);
						if (piece.collisionImpulse.x != 0 && piece.collisionImpulse.y != 0) {
							piece.body.applyForce(piece.collisionImpulse, piece.contactPoint, true);
	//							if (piece.shape instanceof CircleShape)
								piece.body.setAngularDamping(0.8f);
						}
					}
				}
			}
		}
		else {
//			for (i=0; i<len; i++) {
//				mainP = particles.get(i);
//				if (enableBox2DCollisions) {
//					portalFluidSolver(mainP, deltaTime);
//				}
////				capVelocity(mainP.velocity);
//				if (IS_DESKTOP)
//					prepareDeleteOutOfBoundsParticles(mainP);
//				 if (enableBox2DCollisions) {
//					for (Piece piece : pieces.values()) {
//						if (piece.isSensor || piece.body.getFixtureList().size() <= 0)
//							continue;
//						piece.collisionImpulse.set(0, 0);
//	//							if (particles.get(i).pos.dst2(piece.pos) < 1000)
//								box2dFluidSolver(piece, particles.get(i), deltaTime);
//	//							box2dFluidSolverTest(piece, particles.get(i), deltaTime);
//						if (piece.collisionImpulse.x != 0 && piece.collisionImpulse.y != 0) {
//							piece.body.applyForce(piece.collisionImpulse, piece.contactPoint);
//	//							if (piece.shape instanceof CircleShape)
//								piece.body.setAngularDamping(0.8f);
//						}
//					}
//				}
//			}
		}
		if (DEBUG_ENABLED)
			System.out.print("\t fluid/rigid: " + (System.currentTimeMillis() - time));
		
		// Gravity change with mobile accelerometer
//		if (Gdx.input.isPeripheralAvailable(Peripheral.Accelerometer) && (Gdx.input.getAccelerometerX() >= 0 || Gdx.input.getAccelerometerY() >= 0)) {
//			gravityVect.set(-Gdx.input.getAccelerometerY() * GRAVITY_FORCE * 0.5f,Gdx.input.getAccelerometerX() * GRAVITY_FORCE * 0.5f);
//		}
	}
	
	private void relaxation2(float deltaT) {
		lastTotalPressure = totalPressure;
		totalPressure = 0;
//		particleArray = particles.table.toArray(particleArray);
		for (Particle mainP : particles) {
//		for (i=0; i<len; i++) {
//			mainP = particleArray[i];
			
//			mainP.velocity.scl(DAMPING);
//			mainP.prevPos.set(mainP.pos);
//			mainP.pos.set(mainP.pos.x + (deltaT * mainP.velocity.x),
//					mainP.pos.y + (deltaT * mainP.velocity.y));
//			
//			mainP.xs = MULTIPLIER*mainP.pos.x;
//			mainP.ys = MULTIPLIER*mainP.pos.y;
//			mainP.vxs = MULTIPLIER*mainP.velocity.x;
//			mainP.vys = MULTIPLIER*mainP.velocity.y;
//			mainP.xchange = 0;
//			mainP.ychange = 0;

//			tempParticles = particles.nearby(mainP);
//			len2 = tempParticles.size();
			tempParticles2 = particles.nearby(mainP);
			len2 = particles.lastSizeNearby();
	        
	        // Particle pressure calculated by particle proximity
            // Pressures = 0 if all particles within range are H distance away
            p = 0.0f;
            pnear = 0.0f;
    		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//    			neighborP = tempParticles.get(a);
    			neighborP = tempParticles2[a];
                vx = neighborP.xs - mainP.xs;
                vy = neighborP.ys - mainP.ys;
                
                if(vx > -H && vx < H && vy > -H && vy < H) {
                    q = (vx * vx + vy * vy);
                    if(q < H2) {
                    	qArray[a] = (float)Double.longBitsToDouble(((Double.doubleToLongBits(q) >> 32) + 1072632448) << 31);
                    	if (qArray[a] < EPSILON) 
                    		qArray[a] = H - 0.01f;
                        qq = 1.0f - (qArray[a] / H);
                        qqArray[a] = qq;
                        qqqq = qq * qq;
                        p = (p + qqqq);
                        pnear = (pnear + qqqq*qq);

//                    	qArray[a] = q;
//                    	if (qArray[a] < EPSILON) 
//                    		qArray[a] = H2- 0.01f;
//                    	qq = H2 - q;
//                    	p += KPOLY * qq * qq * qq;
//            			pnear = p;
                    } else {
                    	qArray[a] = Float.MAX_VALUE;
                    }
                }
            }
    		mainP.density = p;
    		mainP.constraint = (p / REST_DENSITY) - 1f;
            
            // Now actually apply the forces
            pressure = (p - 5f) / 2.0f; //normal pressure term
            presnear = (pnear) / 2.0f; //near particles term
    		
            changex = 0.0f;
            changey = 0.0f;
        	for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//        		neighborP = tempParticles.get(a);
    			neighborP = tempParticles2[a];
                vx = neighborP.xs - mainP.xs;
                vy = neighborP.ys - mainP.ys;
                if(vx > -H && vx < H && vy > -H && vy < H) {
                    if(qArray[a] < H) {
//                        q = qArray[a] / H;
//                        qq = 1.0f - q;
                    	qq = qqArray[a];
                        factor = qq * (pressure + presnear * qq) / (2.0f * qArray[a]);
                        dX = vx * factor;
                        dY = vy * factor;
                        relvx = neighborP.vxs - mainP.vxs;
                        relvy = neighborP.vys - mainP.vys;
                        factor = VISC * qq * deltaT;
                        dX -= relvx * factor;
                        dY -= relvy * factor;
//                        neighborP.xchange += dX;
//                        neighborP.ychange += dY;
                        changex -= dX;
                        changey -= dY;
                        
                        neighborP.pos.x += dX / MULTIPLIER;
                        neighborP.pos.y += dY / MULTIPLIER;
                        neighborP.velocity.x += dX / (MULTIPLIER*deltaT);
                        neighborP.velocity.y += dY / (MULTIPLIER*deltaT);
                    }
                }
            }
//	        mainP.xchange += changex;
//	        mainP.ychange += changey;
	        
	        mainP.pos.x += changex / MULTIPLIER;
	        mainP.pos.y += changey / MULTIPLIER;
	        mainP.velocity.x += changex / (MULTIPLIER*deltaT);
	        mainP.velocity.y += changey / (MULTIPLIER*deltaT);
	        totalPressure += mainP.velocity.len2() / 100000;
	        mainP.drawPos.set(mainP.pos.x, mainP.pos.y);
        }
		
	}
	
	private void spawnParticles(float deltaTime) {
		if (touching && (!isAttracting) && (!isRepulsing)
				&& (particles.size() < SIZE - 1)) {
			if (!((testPoint2D.x < (-BOX_WIDTH / 2 + wpadding) || testPoint2D.x > (BOX_WIDTH / 2 - wpadding))
					|| (testPoint2D.y < (INITIAL_HEIGHT + hpadding) || testPoint2D.y > (BOX_HEIGHT - hpadding)))) {
				for (float i = -EMITTER_SIZE; i < EMITTER_SIZE; i += 10.0f) {
					for (float j = -EMITTER_SIZE; j < EMITTER_SIZE; j += 5.0f) {
						if (particles.size() >= SIZE - 1)
//						if (particles.size() >= 1)
							break;
						particles.add(new Particle(testPoint2D.x + i, testPoint2D.y - j, prevHash++));
						tempParticle = particles.get(particles.size() - 1);
						tempParticle.type = (emitType);
						if (!enableBox2DCollisions) {
							if (emitType == 2)
								tempParticle.mass = 2;
							else if (emitType == 3)
								tempParticle.mass = 3;
						}
						tempParticle.velocity.set(tempParticle.velocity.x,
								tempParticle.velocity.y - EMITTER_FORCE * deltaTime);
					}
				}
			}
		}
	}

	private void applyGravity(Particle pi) {
		if (massMode)
			pi.velocity.set(pi.velocity.x + (gravityVect.x * pi.mass), pi.velocity.y
					+ (gravityVect.y * pi.mass));
		else
			pi.velocity.set(pi.velocity.x + (gravityVect.x), pi.velocity.y
					+ (gravityVect.y));
		
	}

	private void attract(Particle pi) {
		if (isAttracting) {
			if (pi.pos.dst2(testPoint2D) > ATTRACT_RANGE)
				return;
			attract_vect.set(testPoint2D);
			attract_vect.sub(pi.pos);
			pi.velocity.set(pi.velocity.x
					+ (attract_vect.x * ATTRACT_FORCE), pi.velocity.y
					+ (attract_vect.y * ATTRACT_FORCE));
		}
	}

	private void repulse(Particle pi) {
		if (isRepulsing) {
			if (pi.pos.dst2(testPoint2D) > REPULSE_RANGE)
				return;
			repulse_vect.set(pi.pos);
			repulse_vect.sub(testPoint2D);
			pi.velocity.set(pi.velocity.x
					+ (repulse_vect.x * REPULSE_FORCE), pi.velocity.y
					+ (repulse_vect.y * REPULSE_FORCE));
		}
	}

	private void wallCollision(Particle pi) {
		tempVect.set(0, 0);
		boolean toDump = false;

		if (pi.pos.x > (BOX_WIDTH / 2 - wpadding)) {
			tempVect.sub((pi.pos.x - (BOX_WIDTH / 2 - wpadding))
					/ COLLISION_FORCE, 0);
			pi.pos.x = (BOX_WIDTH / 2 - wpadding);
		}
		else if (pi.pos.x < (-BOX_WIDTH / 2 + wpadding)) {
			tempVect.add(((-BOX_WIDTH / 2 + wpadding) - pi.pos.x)
					/ COLLISION_FORCE, 0);
			pi.pos.x = (-BOX_WIDTH / 2 + wpadding);
		}
		if (pi.pos.y > (BOX_HEIGHT - hpadding)) {
			tempVect.sub(0, (pi.pos.y - (BOX_HEIGHT - hpadding))
					/ COLLISION_FORCE);
			pi.pos.y = (BOX_HEIGHT - hpadding);
		}
		else if (pi.pos.y < (INITIAL_HEIGHT + hpadding)) {
			tempVect.add(0, ((INITIAL_HEIGHT + hpadding) - pi.pos.y)
					/ COLLISION_FORCE);
			toDump = true;
			pi.pos.y = (INITIAL_HEIGHT + hpadding);
		}

		pi.velocity.set(pi.velocity.x + tempVect.x, pi.velocity.y
				+ tempVect.y);
		if (toDump)
			pi.velocity.scl(0.98f);
	}

	private void prepareDeleteOutOfBoundsParticles() {
		len = disposableParticles.size();
		for (i=0; i<len; i++) {
			particles.remove(disposableParticles.get(i));
		}
	}

	@Override
	public void render(float deltaTime) {
		firstRender = true;
		timeStep += deltaTime;
		stepped = false;
		loops = 0;
		while (timeStep > nextGameTick
				&& loops < MAX_FRAMESKIP) {
			for (speedCounter = 0; speedCounter < speedMultiplier; speedCounter++) {
				performLogic(FIXED_DELTA);
				if (IS_DESKTOP) {
					prepareDeleteOutOfBoundsParticles();
					deleteOutOfBoundsParticles();
				}
			}
			stepped = true;
			nextGameTick += slowMotion ? SKIP_TICKS * 3 : SKIP_TICKS;
			loops++;
		}
//		interpolation = slowMotion ? (timeStep + (SKIP_TICKS*3) - nextGameTick) / (SKIP_TICKS*3) 
//				: (timeStep + SKIP_TICKS - nextGameTick) / SKIP_TICKS;

		long renderTime = System.currentTimeMillis();

		synchronized (drawParticles) {
//		synchronized (fluidLock) {
		camera.update();
		if (gl == null) {
			gl = Gdx.gl20;
		}
		if (whiteBackground)
			gl.glClearColor(1, 1, 1, 1);
		else
			gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		if (bgMode) {
			batch.disableBlending();
			batch.begin();
			bgSprite.draw(batch);
			batch.end();
			batch.enableBlending();
		}

		if (enableBox2DCollisions)
			renderer.render(world, camera.combined);

		gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
//		gl.glEnable(GL10.GL_LINE_SMOOTH);
//		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL20.GL_NICEST);
//		gl.glEnable(GL20.GL_BLEND);
//		gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//		gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

		
		// Begin Batch
			if (particleRendering && !render3D) {
				if (shapes) {
					len = drawParticles.size();
	//				defaultShader.begin();
	//				shapeRenderer.setProjectionMatrix(camera.combined);
	//				shapeRenderer.begin(ShapeType.FilledCircle);
	//				for (i=0; i<len; i++) {
	//					if (particles.get(i).type == 1)
	//						shapeRenderer.setColor(1, 0, 0, 1);
	//					if (particles.get(i).type == 2)
	//						shapeRenderer.setColor(0, 1, 0, 1);
	//					if (particles.get(i).type == 3)
	//						shapeRenderer.setColor(0, 0, 1, 1);
	//					dropCoordinate.set(particles.get(i).drawPos.x - dropRadius,
	//							particles.get(i).drawPos.y - dropRadius, 0.0f);
	//					shapeRenderer.filledCircle(dropCoordinate.x, dropCoordinate.y, dropRadius, 8);
	//				}
	//				shapeRenderer.end();
	//				defaultShader.end();
					batch.setProjectionMatrix(camera.combined);
					batch.begin();
//					for (i=0; i<len; i++) {
//						mainP = particles.get(i);
					for (Particle mainP : drawParticles) {
	//				synchronized (particles) {
//					particleArray = particles.table.toArray(particleArray);
//						for (k=0; k<len; k++) {
//							mainP = particleArray[k];
							if (mainP == null)
								break;
							dropCoordinate.set(mainP.drawPos.x - dropRadius,
									mainP.drawPos.y - dropRadius, 0.0f);
							spriteColor = 1.0f/* - (mainP.density * LINE_DENSITY_FACTOR / 255.0f)*/;
		//					if (spriteColor < 0.2f)
		//						spriteColor = 0.2f;
							if (mainP.type == 1 && k==0)
								batch.setColor(spriteColor, 0, 0, 1);
							else if (mainP.type == 2 && k==0)
								batch.setColor(0, spriteColor, 0, 1);
							else if (mainP.type == 3 && k==0)
								batch.setColor(0, 0, spriteColor, 1);
		//					dropSprite.setPosition(dropCoordinate.x, dropCoordinate.y);
		//					dropSprite.draw(batch);
		//					batch.setColor(dropSprite.getColor());
							batch.draw(dropTexture2, dropCoordinate.x, dropCoordinate.y, dropRadiusPixel, dropRadiusPixel);
						}
	//				}
					batch.end();
				}
				else if (!linesMode) {
					len = drawParticles.size();
					gl.glEnable(GL20.GL_BLEND);
					if (bgMode) {
	//					if (glowMode)
							gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	//					else
	//						gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
						gl.glEnable(GL20.GL_TEXTURE_2D);
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0);
						dropTexture.bind();
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE1);
						bgTexture.bind();
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE2);
						glossMapTexture.bind();
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE3);
						displacementMap.bind();
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE4);
						displacementMap2.bind();
						Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0);
						irt.begin(camera.combined, GL20.GL_TRIANGLES);
	//					for (i=0; i<len; i++) {
	//						mainP = particles.get(i);
	//					synchronized (particles) {
							for (Particle mainP : drawParticles) {
//							particleArray = particles.table.toArray(particleArray);
//								for (k=0; k<len; k++) {
//									mainP = particleArray[k];
//									if (mainP == null)
//										break;
		//						dropCoordinate.set(mainP.drawPos.x - dropRadius,
		//								mainP.drawPos.y - dropRadius, 0.0f);
		
								dropCoordinate.set((mainP.drawPos.x + (mainP.velocity.x * interpolation * deltaTime)) - dropRadius,
										(mainP.drawPos.y + (mainP.velocity.y * interpolation * deltaTime)) - dropRadius, 0.0f);
								spriteColor = 1.0f/* - (mainP.density * LINE_DENSITY_FACTOR / 255.0f)*/;
			//					if (spriteColor < 0.2f)
			//						spriteColor = 0.2f;
								if (mainP.type == 1 && k==0)
									dropSprite.setColor(spriteColor, 0, 0, 1);
								else if (mainP.type == 2 && k==0)
									dropSprite.setColor(0, spriteColor, 0, 1);
								else if (mainP.type == 3 && k==0)
									dropSprite.setColor(0.7f, 1, 1, 1);
								irt.texCoord(0, 0);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
								irt.texCoord(0, 1);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt.texCoord(1, 1);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt.texCoord(0, 0);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
								irt.texCoord(1, 1);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt.texCoord(1, 0);
								irt.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
							}
	//					}
//						System.out.println(totalPressure - lastTotalPressure);
						if (Math.abs(totalPressure - lastTotalPressure) >= 5)
							irt.factor -=  0.0001f;
						else
							irt.factor +=  0.0002f;
						irt.end();
						gl.glDisable(GL20.GL_TEXTURE_2D);
					}
					else {
	//					if (glowMode)
	//						gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	//					else
						if (whiteBackground)
							gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
						else
							gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
						gl.glEnable(GL20.GL_TEXTURE_2D);
						dropTexture.bind();
						irt2.begin(camera.combined, GL20.GL_TRIANGLES);
	//					for (i=0; i<len; i++) {
	//						mainP = particles.get(i);
	//					synchronized (particles) {
							for (Particle mainP : drawParticles) {
//							particleArray = particles.table.toArray(particleArray);
//							for (k=0; k<len; k++) {
//								mainP = particleArray[k];
//								if (mainP == null)
//									break;
		//						dropCoordinate.set(mainP.drawPos.x - dropRadius,
		//								mainP.drawPos.y - dropRadius, 0.0f);
								dropCoordinate.set((mainP.drawPos.x + (mainP.velocity.x * interpolation * deltaTime)) - dropRadius,
										(mainP.drawPos.y + (mainP.velocity.y * interpolation * deltaTime)) - dropRadius, 0.0f);
								spriteColor = 1.0f/* - (mainP.density * LINE_DENSITY_FACTOR / 255.0f)*/;
			//					if (spriteColor < 0.2f)
			//						spriteColor = 0.2f;
								if (mainP.type == 1 && k==0)
									dropSprite.setColor(spriteColor, 0, 0, 1);
								else if (mainP.type == 2 && k==0)
									dropSprite.setColor(0, spriteColor, 0, 1);
								else if (mainP.type == 3 && k==0)
									dropSprite.setColor(0, 0, spriteColor, 1);
								irt2.texCoord(0, 0);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
								irt2.texCoord(0, 1);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt2.texCoord(1, 1);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt2.texCoord(0, 0);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x - dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
								irt2.texCoord(1, 1);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y + dropRadiusPixel, 0);
								irt2.texCoord(1, 0);
								irt2.color(dropSprite.getColor().r, dropSprite.getColor().g, dropSprite.getColor().b, dropSprite.getColor().a);
								irt2.vertex(dropCoordinate.x + dropRadiusPixel, dropCoordinate.y - dropRadiusPixel, 0);
							}
	//					}
						irt2.end();
						gl.glDisable(GL20.GL_TEXTURE_2D);
	//					gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
						immediateRenderer.begin(camera.combined, GL20.GL_TRIANGLES);
	//					if (glowMode)
	//						gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_DST_COLOR);
	//					else
							gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_DST_COLOR);
						for (i=0; i< (glowMode ? 0 : 2); i++) {
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(-BOX_WIDTH/2, 0, 0);
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(-BOX_WIDTH/2, BOX_HEIGHT, 0);
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(BOX_WIDTH/2, BOX_HEIGHT, 0);
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(-BOX_WIDTH/2, 0, 0);
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(BOX_WIDTH/2, BOX_HEIGHT, 0);
							immediateRenderer.color(1, 1, 1, 1);
							immediateRenderer.vertex(BOX_WIDTH/2, 0, 0);
						}
						immediateRenderer.end();
					}
				} else {
					Gdx.gl.glLineWidth(2);
					immediateRenderer.begin(camera.combined, GL20.GL_LINES);
					vertexIndex = 0;
					len = particles.size();
	//				for (i=0; i<len; i++) {
	//					mainP = particles.get(i);
	//				synchronized (particles) {
						for (Particle mainP : drawParticles) {
//						particleArray = particles.table.toArray(particleArray);
//						for (k=0; k<len; k++) {
//							mainP = particleArray[k];
//							if (mainP == null)
//								break;
							spriteColor = mainP.density * LINE_DENSITY_FACTOR / 255.0f;
							if (spriteColor > 1.0f)
								spriteColor = 1.0f;
							if (smokeMode) {
								// Red Fire
								if (emitType == 1) {
									tempInt = 255 - (int) ((System.currentTimeMillis() - mainP
											.spawnTime) / 50);
									// Start by decreasing B value to 0
									mainP.setBGrad(240 - (int) ((System
											.currentTimeMillis() - mainP.spawnTime) / (3 * 1)));
									// Then decrease G value to 0
									if (mainP.bGrad < 150)
										mainP.setGGrad(255 - (int) ((System
												.currentTimeMillis() - mainP
												.spawnTime) / (10 * 1)));
									// Then decrease R value to 0
									if (mainP.gGrad < 150)
										mainP.setRGrad(255 - (int) ((System
												.currentTimeMillis() - mainP
												.spawnTime) / (25 * 1)));
									if (tempInt <= 0 || mainP.rGrad == 0) {
										disposableParticles.add(mainP);
										continue;
									}
								}
								// Blue Fire
								else if (emitType == 2) {
									tempInt = 255 - (int) ((System.currentTimeMillis() - mainP
											.spawnTime) / 50);
									// Start by decreasing R value to 0
									mainP.setRGrad(240 - (int) ((System
											.currentTimeMillis() - mainP.spawnTime) / (3 * 1)));
									// Then decrease G value to 0
									if (mainP.rGrad < 150)
										mainP.setGGrad(255 - (int) ((System
												.currentTimeMillis() - mainP
												.spawnTime) / (10 * 1)));
									// Then decrease B value to 0
									if (mainP.gGrad < 150)
										mainP.setBGrad(255 - (int) ((System
												.currentTimeMillis() - mainP
												.spawnTime) / (25 * 1)));
									if (tempInt <= 0 || mainP.bGrad == 0) {
										disposableParticles.add(mainP);
										continue;
									}
								}
								// Green Fire
								else if (emitType == 3) {
									tempInt = 255 - (int) ((System.currentTimeMillis() - mainP
											.spawnTime) / 50);
									// Start by decreasing R and B values to 0
									mainP.setRGrad(240 - (int) ((System
											.currentTimeMillis() - mainP.spawnTime) / (10 * 1)));
									mainP.setBGrad(240 - (int) ((System
											.currentTimeMillis() - mainP.spawnTime) / (10 * 1)));
									// Then decrease G value to 0
									if (mainP.rGrad < 150)
										mainP.setGGrad(255 - (int) ((System
												.currentTimeMillis() - mainP
												.spawnTime) / (25 * 1)));
									if (tempInt <= 0 || mainP.gGrad == 0) {
										disposableParticles.add(mainP);
										continue;
									}
								}
						}
						dropCoordinate.set((mainP.drawPos.x + (mainP.velocity.x * interpolation * deltaTime)) - dropRadius,
								(mainP.drawPos.y + (mainP.velocity.y * interpolation * deltaTime)) - dropRadius, 0.0f);
	//					camera.project(dropCoordinate);
	//					lineVertices[vertexIndex++] = dropCoordinate.x;
	//					lineVertices[vertexIndex++] = dropCoordinate.y;
						if (smokeMode) {
	//						lineVertices[vertexIndex++] = Color.toFloatBits(
	//								mainP.rGrad, mainP.gGrad, mainP.bGrad,
	//								tempInt);
							immediateRenderer.color((float)mainP.rGrad / 255.0f, 
									(float)mainP.gGrad / 255.0f, (float)mainP.bGrad / 255.0f, (float)tempInt / 255.0f);
						}
						else {
							if (mainP.type == 1)
	//							lineVertices[vertexIndex++] = Color.toFloatBits(
	//									255, (int) mainP.density
	//											* LINE_DENSITY_FACTOR, 0, 255);
								immediateRenderer.color(1, spriteColor, 0, 1);
							else if (mainP.type == 2)
	//							lineVertices[vertexIndex++] = Color
	//									.toFloatBits((int) mainP.density
	//											* LINE_DENSITY_FACTOR, 0, 255, 255);
								immediateRenderer.color(0, 0.79f, spriteColor/2, 1);
							else if (mainP.type == 3)
	//							lineVertices[vertexIndex++] = Color.toFloatBits(0,
	//									200, (int) mainP.density
	//											* LINE_DENSITY_FACTOR, 255);
								immediateRenderer.color(spriteColor/4, 0, 1, 1);
						}
	//					lineVertices[vertexIndex++] = dropCoordinate.x
	//							+ mainP.velocity.x * LINE_VELOCITY_FACTOR;
	//					lineVertices[vertexIndex++] = dropCoordinate.y
	//							+ mainP.velocity.y * LINE_VELOCITY_FACTOR;
						immediateRenderer.vertex(dropCoordinate.x, dropCoordinate.y, 0);
						if (smokeMode)
	//						lineVertices[vertexIndex++] = Color.toFloatBits(
	//								mainP.rGrad, mainP.gGrad, mainP.bGrad,
	//								tempInt);
							immediateRenderer.color((float)mainP.rGrad / 255.0f, 
									(float)mainP.gGrad / 255.0f, (float)mainP.bGrad / 255.0f, (float)tempInt / 255.0f);
						else {
							if (mainP.type == 1)
	//							lineVertices[vertexIndex++] = Color.toFloatBits(
	//									255, (int) mainP.density
	//											* LINE_DENSITY_FACTOR, 0, 255);
								immediateRenderer.color(1, spriteColor, 0, 1);
							else if (mainP.type == 2)
	//							lineVertices[vertexIndex++] = Color
	//									.toFloatBits((int) mainP.density
	//											* LINE_DENSITY_FACTOR, 0, 255, 255);
								immediateRenderer.color(0, 0.79f, spriteColor/2, 1);
							else if (mainP.type == 3)
	//							lineVertices[vertexIndex++] = Color.toFloatBits(0,
	//									200, (int) mainP.density
	//											* LINE_DENSITY_FACTOR, 255);
								immediateRenderer.color(spriteColor/4, 0, 1, 1);
						}
						immediateRenderer.vertex(dropCoordinate.x + mainP.velocity.x * LINE_VELOCITY_FACTOR, 
								dropCoordinate.y + mainP.velocity.y * LINE_VELOCITY_FACTOR, 0);
					}
	
	//				}
	//				lineMesh.setVertices(lineVertices, 0, vertexIndex);
	//				defaultShader.begin();
	//				lineMesh.render(defaultShader, GL20.GL_LINES);
	//				defaultShader.end();
					immediateRenderer.end();
				}
			}

		gl.glDisable(GL20.GL_BLEND);
		if (hudEnabled) {
			if (whiteBackground)
				font.setColor(0, 0, 0, 1);
			else
				font.setColor(1, 1, 1, 1);
			batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.begin();
			font.draw(batch,
					"fps:" + Gdx.graphics.getFramesPerSecond()
							+ ", particles: " + (particles.size()), 0, Gdx.graphics.getHeight() - 40);
			font.draw(batch, "deltaTime: " + deltaTime, 0, Gdx.graphics.getHeight() - 20);
			font.draw(batch, "pressure: " + (totalPressure - lastTotalPressure), 0, Gdx.graphics.getHeight());
//			if (IS_DESKTOP) {
				font.draw(batch, "(SPACE) Fluid Type: " + emitType
						+ "    (+/-) Gravity: " + GRAVITY_FORCE
						+ "    (UP/DOWN) Emitter: " + EMITTER_SIZE
						+ "    (E) Expand: " + expandMode + "    (S) Slow: "
						+ slowMotion + "    (C) Crazy: " + crazyMode
						+ "    (L) Lines: " + linesMode + "    (Q) Shapes: "
						+ shapes, 180.0f, Gdx.graphics.getHeight());
				font.draw(batch, "(K) Smoke: " + smokeMode 
						+ "    (R) Particle Render: " + particleRendering 
						+ "    (M) Mass: " + massMode
						+ "    (B) Box2DColl: " + enableBox2DCollisions
						+ "    (G) Glow: " + glowMode, 180, Gdx.graphics.getHeight() - 20);
				font.draw(batch,"ART: " + ARTI_PRESSURE_DELTA_Q + "    Sigma: " + SIGMA
								+ "    Density: " + P0 + "    H: " + H
								+ "    DensityR: " + REST_DENSITY + "    H_H: " + H_H
								+ "    Cell: " + CELL_SIZE + "    K_: " + K_
								+ "    Rad: " + dropRadiusK 
								+ "    MN: " + particles.MAX_NEIGHBORS 
								+ "    Step: " + FIXED_DELTA
								+ "    STICK: " + K_STICKINESS
								+ "    RAD: " + RAD
								+ "    VISC: " + VISC
								+ "    DAMP: " + DAMPING
								+ "	   RELAX: " + RELAXATION_EPSILON, 180, Gdx.graphics.getHeight() - 40);
				font.draw(batch,"camera3D: " + camera3D.position, 180, Gdx.graphics.getHeight() - 60);
//			}
			batch.end();
		}
//		}
		}
		
		//3D
		if (render3D) {
	//		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//			camera3D.update();
			camController.update();
			modelBatch.begin(camera3D);
			for (Particle p : drawParticles) {
				instance.transform.setToTranslation(p.drawPos.x, p.drawPos.y, 0);
		        modelBatch.render(instance, environment);
			}
	        modelBatch.end();
		}
		
		if (DEBUG_ENABLED)
			System.out.print("\t render: " + (System.currentTimeMillis() - renderTime));

		// Exit the application if ESC has been pressed
		if (exitApp) {
			threadRunning = false;
			timer.cancel();
			Gdx.app.exit();
		}
	}

	@Override
	public boolean keyUp(int keycode) {

		// K
		if (keycode == Input.Keys.F1 && K < 2.0f) {
			K += 0.1f;
		} else if (keycode == Input.Keys.NUM_1 && K > 0.004f) {
			K -= 0.1f;
			if (K < 0.004f)
				K = 0.004f;
		}
		if (keycode == Input.Keys.F1) {
			ARTI_PRESSURE_DELTA_Q /= H_H;
			ARTI_PRESSURE_DELTA_Q += 0.01f;
			ARTI_PRESSURE_DELTA_Q *= H_H;
			ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
			ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
		} else if (keycode == Input.Keys.NUM_1) {
			ARTI_PRESSURE_DELTA_Q /= H_H;
			ARTI_PRESSURE_DELTA_Q -= 0.01f;
			if (ARTI_PRESSURE_DELTA_Q < 0)
				ARTI_PRESSURE_DELTA_Q = 0.01f;
			ARTI_PRESSURE_DELTA_Q *= H_H;
			ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
			ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
		}

		// SIGMA
		if (keycode == Input.Keys.F2 && SIGMA < 50.0f)
			SIGMA += 2;
		else if (keycode == Input.Keys.NUM_2 && SIGMA > 0)
			SIGMA -= 2;

		// DENSITY
		else if (keycode == Input.Keys.F3 && P0 < 1000.0f)
			P0 += 100.0f;
		else if (keycode == Input.Keys.NUM_3 && P0 > 100.0f)
			P0 -= 100.0f;
		if (keycode == Input.Keys.F3 && REST_DENSITY < 100.0f)
			REST_DENSITY += 0.1f;
		if (keycode == Input.Keys.NUM_3 && REST_DENSITY > 0.1f)
			REST_DENSITY -= 0.1f;

		// H
		if (keycode == Input.Keys.F4 && H < 200.0f) {
			H += 5.0f;
			H2 = H * H;
			ATTRACT_RANGE = H2 / 2;
			REPULSE_RANGE = H2 / 4;
		} else if (keycode == Input.Keys.NUM_4 && H > 0.2f) {
			H -= 5.0f;
			H2 = H * H;
			ATTRACT_RANGE = H2 / 2;
			REPULSE_RANGE = H2 / 4;
		}

		if (keycode == Input.Keys.F4 && H_H < 100f) {
			H_H += 1.0f;
			H1 = (float) Math.sqrt(H_H);
			KPOLY = (float) (315f / (64.0f * Math.PI * Math.pow(Math.sqrt(H_H), 9)));
			KSPIKY = (float) (45f / (Math.PI * Math.pow(Math.sqrt(H_H), 6)));
			ARTI_PRESSURE_DELTA_Q = 0.1f * H_H;
			ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
			ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
		} 
		if (keycode == Input.Keys.NUM_4 && H_H > 1f) {
			H_H -= 1.0f;
			if (H_H < 1)
				H_H = 1;
			H1 = (float) Math.sqrt(H_H);
			KPOLY = (float) (315f / (64.0f * Math.PI * Math.pow(Math.sqrt(H_H), 9)));
			KSPIKY = (float) (45f / (Math.PI * Math.pow(Math.sqrt(H_H), 6)));
			ARTI_PRESSURE_DELTA_Q = 0.1f * H_H;
			ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
			ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
		}

		// CELL_SIZE
		if (keycode == Input.Keys.F5 && CELL_SIZE < 50) {
			CELL_SIZE += 1;
			particles.rehash();
		} else if (keycode == Input.Keys.NUM_5 && CELL_SIZE > 1) {
			CELL_SIZE -= 1;
			particles.rehash();
		}

		// K_
		if (keycode == Input.Keys.F6 && K_ < 10.0f) {
			K_ += 0.1f;
		} else if (keycode == Input.Keys.NUM_6 && K_ > 0.1f) {
			K_ -= 0.1f;
		}

		// MAX_NEIGHBORS
		if (keycode == Input.Keys.F7 && particles.MAX_NEIGHBORS < 500) {
			particles.MAX_NEIGHBORS += 5;
			qArray = new float[particles.MAX_NEIGHBORS];
			qqArray = new float[particles.MAX_NEIGHBORS];
		} else if (keycode == Input.Keys.NUM_7 && particles.MAX_NEIGHBORS > 0) {
			particles.MAX_NEIGHBORS -= 5;
			qArray = new float[particles.MAX_NEIGHBORS];
			qqArray = new float[particles.MAX_NEIGHBORS];
		}

		// dropRadiusK
		if (keycode == Input.Keys.F8 && dropRadiusK < 3.5f) {
			dropRadiusK += 0.2f;
			dropRadius = 0.1f + dropRadiusK;
			dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
			dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
			dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
		} else if (keycode == Input.Keys.NUM_8 && dropRadiusK >= 0.3f) {
			dropRadiusK -= 0.2f;
			dropRadius = 0.1f + dropRadiusK;
			dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
			dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
			dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
		}
		
		// K_STICKINESS
		if (keycode == Input.Keys.F10 && K_STICKINESS < 20.0f)
			K_STICKINESS += 1;
		else if (keycode == Input.Keys.NUM_0 && K_STICKINESS > 0)
			K_STICKINESS -= 1;
		
		// RAD
		if (keycode == Input.Keys.RIGHT && RAD < 50.0f) {
			RAD += 1;
			MULTIPLIER = 50 / RAD;
		}
		else if (keycode == Input.Keys.LEFT && RAD > 0) {
			RAD -= 1;
			MULTIPLIER = 50 / RAD;
		}
		
		// VISC
		if (keycode == Input.Keys.F11 && VISC < 1.0f)
			VISC += 0.0001f;
		else if (keycode == Input.Keys.F12 && VISC > 0)
			VISC -= 0.0001f;

		if (keycode == Input.Keys.BACKSPACE && isAttracting) {
			for (Particle pi : particles) {
				if (pi.pos.dst2(testPoint2D) < ATTRACT_RANGE) {
					disposableParticles.add(pi);
				}
			}
		} else if (keycode == Input.Keys.BACKSPACE && IS_DESKTOP) {
//			game.setScreen(game.switchToFluidSimulator());
//			synchronized (particles) {
				for (Particle pi : particles)
					disposableParticles.add(pi);
//			}
		}

		// Change Particle color
		if (keycode == Input.Keys.SPACE) {
			emitType += 1;
			if (emitType > 3) {
				emitType = 1;
			}
			if (!IS_DESKTOP) {
				for (Particle p : particles)
					p.type = emitType;
			}
		}

		// Increase/Decrease Gravity
		if ((keycode == Input.Keys.PLUS) && (GRAVITY_FORCE > -60.0f)) {
			GRAVITY_FORCE -= 1.0f;
			gravityVect.set(0.0f, GRAVITY_FORCE);
		} else if ((keycode == Input.Keys.MINUS) && (GRAVITY_FORCE < 0.0f)) {
			GRAVITY_FORCE += 1.0f;
//			if (GRAVITY_FORCE > -0.2f)
//				GRAVITY_FORCE = 0.0f;
			gravityVect.set(0.0f, GRAVITY_FORCE);
		}

		// Increase/Decrease Emitter Size
		if ((keycode == Input.Keys.DOWN) && (EMITTER_SIZE > 2)) {
			EMITTER_SIZE -= 3;
		} else if ((keycode == Input.Keys.UP) && (EMITTER_SIZE < 20)) {
			EMITTER_SIZE += 3;
		}

		// Enable/Disable Expand Mode
		if (keycode == Input.Keys.E)
			expandMode = !expandMode;

		// Enable/Disable Stop Motion
		if (keycode == Input.Keys.S)
			slowMotion = !slowMotion;

		// Enable/Disable Crazy Mode
		if (keycode == Input.Keys.C) {
//			crazyMode = !crazyMode;
			Gdx.input.setInputProcessor(camController);
		}
		
		// Enable/Disable Box2D Collisions
		if (keycode == Input.Keys.B)
			enableBox2DCollisions = !enableBox2DCollisions;

		// Enable/Disable Render Glow
		if (keycode == Input.Keys.G)
			glowMode = !glowMode;

		// Enable/Disable PBF
		if (keycode == Input.Keys.J) {
			pbfEnabled = !pbfEnabled;
			particles.rehash();
		}

		// Enable/Disable HUD
		if (keycode == Input.Keys.H)
			hudEnabled = !hudEnabled;
		
		// Mass Mode
		if (keycode == Input.Keys.M) {
			massMode = !massMode;
		}
		
		// Enable/Disable ShapeRenderer mode
		if (keycode == Input.Keys.Q) {
			shapes = !shapes;
			if (!shapes) {
				dropRadiusK = 1.5f;
				dropRadius = 0.1f + dropRadiusK;
				dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
				dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
				dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
			}
			else {
				dropRadiusK = 0.3f;
				dropRadius = 0.1f + dropRadiusK;
				dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
				dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
				dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
			}
		}

		// Enable/Disable Lines mode
		if (keycode == Input.Keys.L) {
			linesMode = !linesMode;
		}

		// Enable/Disable Smoke Mode
		if (keycode == Input.Keys.K)
			smokeMode = !smokeMode;

		// Enable/Disable Particle Rendering
		if (keycode == Input.Keys.R) {
			particleRendering = !particleRendering;
			render3D = !render3D;
		}

		// Enable/Disable White Background
		if (keycode == Input.Keys.X)
			whiteBackground = !whiteBackground;
		
		if (keycode == Keys.PAGE_UP) {
			FIXED_DELTA += 0.004f;
		}
		
		if (keycode == Keys.PAGE_DOWN) {
			FIXED_DELTA -= 0.004f;
			if (FIXED_DELTA <= 0.016f)
				FIXED_DELTA = 0.016f;
		}

		// Exit
		if (keycode == Input.Keys.ESCAPE) {
			exitApp = true;
		}

		if (keycode == Input.Keys.CONTROL_LEFT) {
			isDragging = false;
		}

		if (keycode == Input.Keys.CONTROL_RIGHT) {
			DAMPING += 0.01f;
			RELAXATION_EPSILON += 10f;
		}
		else if (keycode == Input.Keys.ALT_RIGHT) {
			DAMPING -= 0.01f;
			RELAXATION_EPSILON -= 10f;
			if (RELAXATION_EPSILON <= 0)
				RELAXATION_EPSILON = 0.1f;
		}

		// Enable/Disable BG Mode
		if (keycode == Input.Keys.N) {
			if (IS_DESKTOP)
				bgMode = !bgMode;
			else if (!enableBox2DCollisions) {
				shapes = !shapes;
				bgMode = !bgMode;
				if (shapes)
					dropRadiusK = 1.3f;
				else
					dropRadiusK = 2.5f;
				dropRadius = 0.1f + dropRadiusK;
				dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
				dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
				dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
			}
			
		}
		
		return false;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		touching = true;
		camera.unproject(testPoint.set(x, y, 0));
		testPoint2D.x = testPoint.x;
		testPoint2D.y = testPoint.y;
		oldDragPos.set(testPoint2D);
		
		if (button == Buttons.LEFT) {

			// Drag Mode
			if (isDragging) {
				for (Piece piece : pieces.values()) {
					hitBody = null;
					if (piece.body.getFixtureList().get(0).testPoint(testPoint2D)) {
						hitBody = piece.body;
						if (hitBody.getType() == BodyType.KinematicBody || hitBody.getType() == BodyType.StaticBody)
							continue;
						MouseJointDef def = new MouseJointDef();
						def.bodyA = groundBody;
						def.bodyB = hitBody;
						def.collideConnected = true;
						def.target.set(testPoint2D);
						def.maxForce = 10.0f * hitBody.getMass();
						mouseJoint = (MouseJoint)world.createJoint(def);
						hitBody.setAwake(true);
						break;
					}
				}
				if (mouseJoint != null)
					return false;
			}
			
			if (!IS_DESKTOP) {
				isRepulsing = true;
				isAttracting = false;
			} else {
				isAttracting = false;
				isRepulsing = false;
			}
		}
		if (button == Buttons.RIGHT) {
			isAttracting = true;
		}
		if (button == Buttons.MIDDLE) {
			isRepulsing = true;
		}
		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		camera.unproject(testPoint.set(x, y, 0));
		testPoint2D.x = testPoint.x;
		testPoint2D.y = testPoint.y;
		
		
		dragVelocity.set(testPoint2D);
		dragVelocity.sub(oldDragPos);
		oldDragPos.set(testPoint2D);
		if (mouseJoint != null) {
			mouseJoint.setTarget(target.set(testPoint2D));
			mouseJoint.getBodyB().setLinearVelocity(0, 0);
			mouseJoint.getBodyB().setTransform(testPoint2D.x, testPoint2D.y, 0);
		}

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		touching = false;
		
		if (!IS_DESKTOP) {
			isRepulsing = false;
			isAttracting = false;
		}
		if (button == Buttons.RIGHT) {
			isAttracting = false;
		}
		if (button == Buttons.MIDDLE) {
			isRepulsing = false;
		}
		
		// if a mouse joint exists we simply destroy it
		if (mouseJoint != null) {
			if (dragVelocity.len() > 1)
				mouseJoint.getBodyB().setLinearVelocity(dragVelocity.scl(50000));
			world.destroyJoint(mouseJoint);
			mouseJoint = null;
		}
		hitBody = null;
		dragVelocity.set(0, 0);
		
		if (pointer == 1 || pointer == 3) {
			if (pointer == 3)
				enableBox2DCollisions = !enableBox2DCollisions;
			if (pointer == 1)
				bgMode = !bgMode;
//			if (shapes)
//				dropRadiusK = 1.3f;
//			else
//				dropRadiusK = 2.5f;
//			dropRadius = 0.1f + dropRadiusK;
//			dropRadiusPixel = (int) (dropRadius * PARTICLE_SIZE);
//			dropRadiusPixel2 = dropRadiusPixel * dropRadiusPixel;
//			dropSprite.setSize(dropRadiusPixel, dropRadiusPixel);
		}
		else if (pointer == 2) {
			emitType ++;
			if (emitType > 3)
				emitType = 1;
			for (Particle p : particles)
				p.type = emitType;
		}
		
		return false;
	}

	@Override
	public void dispose() {
		super.dispose();
		particles.clear();
	}

	@Override
	public void hide() {
		super.hide();
		particles.clear();
	}
	
	
	
	/**
	 * 
	 * POSITION BASED FLUIDS
	 */
	
	public final Vector2 ZERO_VEC = new Vector2(0, 0);
	public float H_H = 6.00f;
	public float H1 = (float) Math.sqrt(H_H);
	public float KPOLY = (float) (315f / (64.0f * Math.PI * Math.pow(Math.sqrt(H_H), 9)));
	public float KCUSTOM = (float) (945f / (32.0f * Math.PI * Math.pow(Math.sqrt(H_H), 8))) * -3f;
	public float KSPIKY = (float) (45f / (Math.PI * Math.pow(Math.sqrt(H_H), 6)));
	public float SPIKY_F = (float) Math.pow(1.0f, -12);
	public float REST_DENSITY = 0.60f;
	public float INVERSE_REST_DENSITY = 1f / REST_DENSITY;
	public float ARTI_PRESSURE_DELTA_Q = 0.1f * H_H;
	public float ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
	public float ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
	public float RELAXATION_EPSILON = 10.0f;
	public final int PBF_ITERATIONS = 1;
	public final Box box = new Box(-35, 35, 3, 50);
	private Vector2 tempVec2 = new Vector2();
	private Vector2 delta = new Vector2();
	private Vector2 dist = new Vector2();
	private Vector2 gradient = new Vector2();
	private float r_r;
	public float r;
	private float aFloat;
	private float wpipj;
	
	// main function, apply the position-based algorithm
	public void UpdateFluid(float deltaTime) {
		//find neighbours
		long startTime=System.currentTimeMillis();
		particles.rehash();
		particleArray = particles.table.toArray(particleArray);
		long neighbourEndTime=System.currentTimeMillis();
		len = particles.size();
		if (DEBUG_ENABLED) {
			System.out.println("");
			System.out.print("neighbour = " + (neighbourEndTime - startTime) );
		}
//		if (thread == null) {
//			TimerTask task = new TimerTask() {
//				public void run() {
//					updatePBF(0, len/2);
//				}
//			};
//			thread = new Thread() {
//				public void run() {
//					updatePBF(0, len/4);
//				}
//			};
//			timer.schedule(task, 0, 15);
//		}
//		if (thread2 == null) {
//			TimerTask task = new TimerTask() {
//				public void run() {
//					updatePBF(len/2, len);
//				}
//			};
//			thread2 = new Thread() {
//				public void run() {
//					updatePBF(len/4, len/2);
//				}
//			};
//			timer.schedule(task, 0, 15);
//		}
//		if (thread3 == null) {
//			TimerTask task = new TimerTask() {
//				public void run() {
//					try {
//						updatePBF(len/2, 3*len/4);
//					} catch(NullPointerException npe) {}
//				}
//			};
//			thread3 = new Thread() {
//				public void run() {
//					updatePBF(len/2, 3*len/4);
//				}
//			};
//			timer.schedule(task, 0, 30);
//		}
//		if (thread4 == null) {
//			thread4 = new Thread() {
//				public void run() {
//					try {
//						updatePBF(3*len/4, len);
//					} catch(NullPointerException npe) {}
//				}
//			};
//			TimerTask task = new TimerTask() {
//				public void run() {
//					updatePBF(3*len/4, len);
//				}
//			};
//			timer.schedule(task, 0, 30);
//		}
//		if (!thread.isAlive())
//			thread.run();
//		if (!thread2.isAlive())
//			thread2.run();
//		if (!thread3.isAlive())
//			thread3.run();
//		if (!thread4.isAlive())
//			thread4.run();

//		synchronized (fluidLock) {
//			updatePBF(0, len, deltaTime);
		updatePBF(0, len, deltaTime);
//		}
//		ApplyExternalForce(deltaTime);
//		// make it align with constraints
//		for(s = 1; s <= PBF_ITERATIONS; s++) {
//			long beginDeltaPos=System.currentTimeMillis();
////			for(Particle particle : particles) {
//			for (i=0; i<len; i++) {
//				mainP = particleArray[i];
//				ComputeC(mainP);
//			}
//			long endCtime=System.currentTimeMillis();
////			for(Particle particle : particles) {
//				for (i=0; i<len; i++) {
//					mainP = particleArray[i];
//				ComputeLamda(mainP);
//			}
//			long endLamdaTime=System.currentTimeMillis();
////			for(Particle particle : particles) {
//				for (i=0; i<len; i++) {
//					mainP = particleArray[i];
//				ComputeDeltaPos(mainP);
//			}
//			long endDeltaPos=System.currentTimeMillis();
////			CollisionWithBox();
//			long endCollision=System.currentTimeMillis();
////			for(Particle particle : particles) {
//				for (i=0; i<len; i++) {
//					mainP = particleArray[i];
//					if(!box.isInBox(mainP.posStar)) {
//						box.ForceInsideBox(mainP.posStar, mainP.velocity);
//					}
//					mainP.posStar.add(mainP.deltaPos);
//			}
////			if (DEBUG_ENABLED)
//			System.out.println("neighbour time = " + (neighbourEndTime - startTime) + "\t delta pos = " + (endDeltaPos - beginDeltaPos) 
////					+ "\t collistion = " + (endCollision - endDeltaPos) + "\t C time = "  + (endCtime - beginDeltaPos) + "\tlandatime =" + (endLamdaTime - endCtime));
//		}
//		
//		//v = (posStar-pos) / t
////		for(Particle particle : particles) {
//		for (i=0; i<len; i++) {
//			mainP = particleArray[i];
//			mainP.velocity.set((mainP.posStar.x - mainP.pos.x) / deltaTime, (mainP.posStar.y - mainP.pos.y) / deltaTime);
////			mainP.velocity.scl(DAMPING);
//			
//			//apply vorticity confinement and XSPH viscosity
//			// pos = posStar
//			mainP.pos.set(mainP.posStar);
//			
//			mainP.pos.x *= MULTIPLIER;
//			mainP.pos.y *= MULTIPLIER;
//			mainP.velocity.x *= MULTIPLIER;
//			mainP.velocity.y *= MULTIPLIER;
//		}
//		try {
//			thread.join();
//			thread2.join();
//			thread3.join();
//			thread4.join();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
	
	public void updatePBF(int len1, int len2, float deltaTime) {
		//TODO Temp variable for each thread
		int i=0;

		long applyForcesStartTime=System.currentTimeMillis();
		synchronized (fluidLock) {
			for (i=len1; i<len2; i++) {
				mainP = particleArray[i];
				// Precalculate neighbors
				mainP.neighbors = particles.nearby(mainP);
				mainP.neighborsSize = particles.lastSizeNearby();
				//clear force
				mainP.force.set(0, 0);
				// Gravity
				if (massMode)
					mainP.force.add(gravityVect.x * mainP.mass, gravityVect.y * mainP.mass);
				else
					mainP.force.add(gravityVect);
				// Attract
				if (isAttracting) {
					if (mainP.pos.dst2(testPoint2D) <= ATTRACT_RANGE) {
						attract_vect.set(testPoint2D);
						attract_vect.sub(mainP.pos);
						mainP.force.add(attract_vect.x * ATTRACT_FORCE * 3, attract_vect.y * ATTRACT_FORCE * 3);
					}
				}
				// Repulse
				if (isRepulsing) {
					if (mainP.pos.dst2(testPoint2D) <= REPULSE_RANGE) {
						repulse_vect.set(mainP.pos);
						repulse_vect.sub(testPoint2D);
						mainP.force.add(repulse_vect.x * REPULSE_FORCE / 2f, repulse_vect.y * REPULSE_FORCE / 2f);
					}
				}
				
				// Scale
				mainP.pos.x /= MULTIPLIER;
				mainP.pos.y /= MULTIPLIER;
				mainP.velocity.x /= MULTIPLIER;
				mainP.velocity.y /= MULTIPLIER;
				
				// Apply Forces
				mainP.velocity.set(mainP.velocity.x + (mainP.force.x * deltaTime), mainP.velocity.y + (mainP.force.y * deltaTime));
				// Predict position
				mainP.posStar.set(mainP.pos.x + (mainP.velocity.x * deltaTime), mainP.pos.y + (mainP.velocity.y * deltaTime));
			}
		}
		if (DEBUG_ENABLED)
			System.out.print(" forces = " + (System.currentTimeMillis() - applyForcesStartTime) );
		long beginC = System.currentTimeMillis();

		lastTotalPressure = totalPressure;
		totalPressure = 0;
//		for(Particle particle : particles) {
		for (i=len1; i<len2; i++) {
			mainP = particleArray[i];
			computeC(mainP);
			totalPressure += mainP.constraint;
		}
		long endCtime = System.currentTimeMillis();
//		for(Particle particle : particles) {
		for (i=len1; i<len2; i++) {
			mainP = particleArray[i];
			computeLamda(mainP);
		}
		long endLamdaTime = System.currentTimeMillis();
		synchronized (fluidLock) {
//				for(Particle particle : particles) {
			for (i=len1; i<len2; i++) {
				mainP = particleArray[i];
				computeDeltaPos(mainP);
			}
		}
		long endDeltaPos = System.currentTimeMillis();
//				CollisionWithBox();
		synchronized (fluidLock) {
//				for(Particle particle : particles) {
			for (i=len1; i<len2; i++) {
				mainP = particleArray[i];
				if(!box.isInBox(mainP.posStar)) {
					box.forceInsideBox(mainP.posStar, mainP.velocity);
				}
				mainP.posStar.add(mainP.deltaPos);
//				preventParticleCohabitation(mainP);
//			}
//
//			for (i=len1; i<len2; i++) {
//				mainP = particleArray[i];
				mainP.velocity.set((mainP.posStar.x - mainP.pos.x) / deltaTime, (mainP.posStar.y - mainP.pos.y) / deltaTime);
	//						mainP.velocity.scl(DAMPING);
				
				//apply vorticity confinement and XSPH viscosity
				// pos = posStar
				mainP.pos.set(mainP.posStar);
				
				mainP.pos.x *= MULTIPLIER;
				mainP.pos.y *= MULTIPLIER;
				mainP.velocity.x *= MULTIPLIER;
				mainP.velocity.y *= MULTIPLIER;
//				capVelocity(mainP.velocity);
				mainP.drawPos.set(mainP.pos.x + (mainP.velocity.x * interpolation * Gdx.graphics.getDeltaTime()), mainP.pos.y + (mainP.velocity.y * interpolation * Gdx.graphics.getDeltaTime()));
			}
		}
		synchronized (drawParticles) {
			drawParticles.clear();
			drawParticles.addAll(particles.table);
		}
		long endCollision=System.currentTimeMillis();
		
		if (DEBUG_ENABLED)
			System.out.println("\t delta pos = " + (endDeltaPos - endLamdaTime) 
					+ "\t collision = " + (endCollision - endDeltaPos) + "\t C = "  
					+ (endCtime - beginC) + "\t landatime =" + (endLamdaTime - endCtime));
		
	}
	
	private Vector2 tempVec = new Vector2();
	private Vector2 tmpVec2 = new Vector2();
	private Vector2 tmpVec3 = new Vector2();
	private Vector2 tmpVec4 = new Vector2();

	public void ApplyExternalForce(float deltaTime) {
		for (Particle mainP : particles) {
			//clear force
			mainP.force.set(0, 0);
			// Gravity
			mainP.force.add(0, GRAVITY_FORCE);
			// Attract
			if (isAttracting) {
				if (mainP.pos.dst2(testPoint2D) <= ATTRACT_RANGE) {
					attract_vect.set(testPoint2D);
					attract_vect.sub(mainP.pos);
					mainP.force.add(attract_vect.x * ATTRACT_FORCE, attract_vect.y * ATTRACT_FORCE);
				}
			}
			// Repulse
			if (isRepulsing) {
				if (mainP.pos.dst2(testPoint2D) <= REPULSE_RANGE) {
					repulse_vect.set(mainP.pos);
					repulse_vect.sub(testPoint2D);
					mainP.force.add(repulse_vect.x * REPULSE_FORCE / 2f, repulse_vect.y * REPULSE_FORCE / 2f);
				}
			}
			
			// Scale
			mainP.pos.x /= MULTIPLIER;
			mainP.pos.y /= MULTIPLIER;
			mainP.velocity.x /= MULTIPLIER;
			mainP.velocity.y /= MULTIPLIER;
			
			// Apply Forces
//			wallCollision(mainP);
			mainP.velocity.set(mainP.velocity.x + (mainP.force.x * deltaTime), mainP.velocity.y + (mainP.force.y * deltaTime));
			// Predict position
			mainP.posStar.set(mainP.pos.x + (mainP.velocity.x * deltaTime), mainP.pos.y + (mainP.velocity.y * deltaTime));
		}
	}
	
	public void CollisionWithBox() {
		for(Particle particle:particles) {
			if(!box.isInBox(particle.posStar)) {
				box.forceInsideBox(particle.posStar, particle.velocity);
			}
		}
	}
	
	
	public void computeC(Particle particle) {
		//density
		p = 0;
//		tempParticles = particles.nearby(particle);
//		len2 = tempParticles.size();
//		tempParticles2 = particles.nearby(particle);
//		len2 = particles.lastSizeNearby();
		tempParticles2 = particle.neighbors;
		len2 = particle.neighborsSize;
//		System.out.println("neighbors = " + len2);
		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//			neighborP = tempParticles.get(a);
			neighborP = tempParticles2[a];
			p += Kernel(particle.posStar, neighborP.posStar);
		}
		particle.density = p;
		particle.constraint = (p / REST_DENSITY) - 1f;
//		System.out.println("density = " + p);
	}

	
	public void computeLamda(Particle particle) {
		float sumGradient = 0;
//		tempParticles = particles.nearby(particle);
//		len2 = tempParticles.size();
//		tempParticles2 = particles.nearby(particle);
//		len2 = particles.lastSizeNearby();
		tempParticles2 = particle.neighbors;
		len2 = particle.neighborsSize;
		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//			neighborP = tempParticles.get(a);
			neighborP = tempParticles2[a];
			Vector2 grad = ComputeGrandientC(particle, neighborP);
			sumGradient += grad.len2();
		}
//		System.out.println("constraint = " + particle.constraint + "\tsumgradient = " + sumGradient);
		particle.lamda = -1f * (particle.constraint / (sumGradient + RELAXATION_EPSILON));
//		System.out.println(particle.lamda);
	}
	
	
	public Vector2 ComputeGrandientC(Particle particle, Particle neighbour) {
		if(particle.hash == neighbour.hash) {	// k==i
			tmpVec4.set(0, 0);
//			tempParticles = particles.nearby(particle);
//			len2 = tempParticles.size();
//			tempParticles2 = particles.nearby(particle);
//			len2 = particles.lastSizeNearby();
			tempParticles2 = particle.neighbors;
			len2 = particle.neighborsSize;
			for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//				neighborP = tempParticles.get(a);
				neighborP = tempParticles2[a];
				if(neighborP.hash != particle.hash) {
					gradient = KernelGradient(particle.posStar, neighborP.posStar);
					tmpVec4.add(gradient);
				}
			}
			tmpVec4.scl(INVERSE_REST_DENSITY);
			return tmpVec4;
		}
		else {	// k == j
			gradient = KernelGradient(particle.posStar, neighbour.posStar);
			gradient.scl(-INVERSE_REST_DENSITY);
			return gradient;
		}
	}
	
	public void computeDeltaPos(Particle particle) {
		particle.deltaPos.set(0, 0);
//		tempParticles = particles.nearby(particle);
//		len2 = tempParticles.size();
//		tempParticles2 = particles.nearby(particle);
//		len2 = particles.lastSizeNearby();
		tempParticles2 = particle.neighbors;
		len2 = particle.neighborsSize;
		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
//			neighborP = tempParticles.get(a);
			neighborP = tempParticles2[a];
			if(neighborP.hash != particle.hash) {
				gradient = KernelGradient(particle.posStar, neighborP.posStar);
				gradient.scl(particle.lamda + neighborP.lamda + ComputeArtiPressure(particle, neighborP));
				particle.deltaPos.add(gradient);
			}
			
		}
		particle.deltaPos.scl(INVERSE_REST_DENSITY);
	}
	
	public Vector2 KernelGradient(Vector2 pi, Vector2 pj) {
		dist.set(pi);
		dist.sub(pj);
		
		// Poly6
//		r_r = dist.len2();
//		if (r_r > H_H) 
//			return ZERO_VEC;
//		aFloat = (float) Math.pow(H_H - r_r, 2);
//		tmpVec2.set(-2 * dist.x, -2 * dist.y);
//		tmpVec2.scl(KPOLY * 3f * aFloat);
//		return tmpVec2;
		
		// WPoly6Grad
//		r_r = dist.len2();
//		if (r_r > H_H) 
//			return ZERO_VEC;
//		aFloat = (H_H - r_r) * (H_H - r_r);
//		tmpVec2.set(dist.x, dist.y);
//		tmpVec2.scl(KPOLY * -6f * aFloat);
//		return tmpVec2;
		
		// WSpikyGrad
//		r_r = dist.len2();
//		if (r_r > H_H) 
//			return ZERO_VEC;
//		if (r_r < SPIKY_F)
//			r_r = SPIKY_F;
//		r = (float) Math.sqrt(r_r);
//		dist.nor();
//		aFloat = (H1 - r) * (H1 - r) / r;
////		aFloat = (H_H - r_r) * (H_H - r_r);
//		dist.scl(KSPIKY * -3f * aFloat);
//		return dist;
		
		// Custom
		r_r = dist.len2();
		if (r_r > H_H) 
			return ZERO_VEC;
//		aFloat = (H_H - r_r) * (H_H - r_r);
//		dist.scl(KCUSTOM * -3f * aFloat);
		aFloat = KCUSTOM * (H_H - r_r) * (H_H - r_r);
		dist.set((pi.x - pj.x) * aFloat, (pi.y - pj.y) * aFloat);
		return dist;
	}
	
	public float Kernel(Vector2 pi, Vector2 pj) {
//		tmpVec3.set(pi);
//		tmpVec3.sub(pj);
//		r_r = tmpVec3.len2();
		r_r = pi.dst2(pj);
		if (r_r > H_H) 
			return 0;
		return (KPOLY * (H_H - r_r) * (H_H - r_r) * (H_H - r_r));
	}
	
	public float ComputeArtiPressure(Particle pi, Particle pj) {
		wpipj = Kernel(pi.posStar, pj.posStar);
		
//		System.out.println("");
//		System.out.print(-0.1f * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE));
//		System.out.print(" " + (wpipj * wpipj * wpipj * wpipj * ARTI_PRESSURE_K));
//		return -0.1f * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE) * (wpipj / ARTI_PRESSURE);
		return wpipj * wpipj * wpipj * wpipj * ARTI_PRESSURE_K;
	}
	
	public void preventParticleCohabitation(Particle particle) {
		tempParticles2 = particle.neighbors;
		len2 = particle.neighborsSize;
		float minDist2 = (0.2f * H1) * (0.2f * H1);
		float minDist = (float)Math.sqrt(minDist2);
		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
			neighborP = tempParticles2[a];
			if(neighborP.hash != particle.hash) {
				dist.set(particle.posStar);
				dist.sub(neighborP.posStar);
				if (dist.len2() < minDist2) {
					float deltaLen = dist.len();
					float diff = 0.1f * 0.5f * (deltaLen - minDist) / deltaLen;
					dist.scl(diff);
					particle.posStar.Add(dist);
					neighborP.posStar.Substract(dist);
				}
			}
		}
	}
	
	public void initializePBF() {
		H_H = 6.0f;
		H1 = (float) Math.sqrt(H_H);
		KPOLY = (float) (315f / (64.0f * Math.PI * Math.pow(Math.sqrt(H_H), 9)));
		KSPIKY = (float) (45f / (Math.PI * Math.pow(Math.sqrt(H_H), 6)));
		KCUSTOM = (float) (945f / (32.0f * Math.PI * Math.pow(Math.sqrt(H_H), 8))) * -3f;
		ARTI_PRESSURE_DELTA_Q = 0.1f * H_H;
		ARTI_PRESSURE = (float) (KPOLY * Math.pow(H_H - ARTI_PRESSURE_DELTA_Q, 3));
		ARTI_PRESSURE_K = (float) (-0.05f / Math.pow(ARTI_PRESSURE, 4));
	}
	
}