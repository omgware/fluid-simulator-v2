package com.fluidsimulator;

import java.util.ArrayList;

import javolution.util.FastMap;

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
import com.fluidsimulator.gameobjects.Spring;
import com.fluidsimulator.gameobjects.fluid.SpatialTable;
import com.fluidsimulator.utils.Vector2;

public class FluidSimulatorLiquid extends FluidSimulatorGeneric {
	
	// FPS Management
	private float TICKS_PER_SECOND = IS_DESKTOP ? 60 : 50;
	private float SKIP_TICKS = 1 / TICKS_PER_SECOND;
	private float FIXED_DELTA = 1.0f / 30.0f;
	private final int MAX_FRAMESKIP = 1;
	private int speedMultiplier = IS_DESKTOP ? 2 : 1;
	
	// Tune these statics for platform specific behaviors
	private float GRAVITY_FORCE = IS_DESKTOP ? -3.0f : -3.0f;
	private final Vector2 gravityVect = new Vector2(0.0f, GRAVITY_FORCE);

	// Particles arrays and spatial table
	private SpatialTable<Particle> particles = new SpatialTable<Particle>(40, 40, IS_DESKTOP ? SIZE : ANDROID_SIZE) {

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
	private float H = 50.0f;
	private float H2 = H * H;
	private float RAD = 17.0f;
	private float VISC = 0.001f;
	private float MULTIPLIER = 50 / RAD;
	private float K = 0.004f;
	private float K_ = 1.01f;
	private float SIGMA = 2;
	private float P0 = 210.0f;
	private final float ATTRACT_FORCE = 1.66f;
	private float ATTRACT_RANGE = H2 / 2;
	private final float REPULSE_FORCE = H / 2;
	private float REPULSE_RANGE = H2 / 4;
	private float DAMPING = 0.99f;
	private float EMITTER_FORCE = 10;
	private float EMITTER_SIZE = 5;
	private float K_SPRING = 10.0f;
	private final float REST_LENGTH = 5.0f;
	private final float YELD_RATIO_STRETCH = 0.2f;
	private final float YELD_RATIO_COMPRESS = 0.1f;
	private final float PLASTICITY = 0.3f;
	private final float STICKINESS_DIST = 5.0f;
	private final float STICKINESS_DIST2 = STICKINESS_DIST * STICKINESS_DIST;
	private float K_STICKINESS = 7.0f;
	private final float WET_FRICTION = 0.0f;
	private final float WET_RESTITUTION = 1.0f;
	private final int PARTICLE_SIZE = 5;
	private float dropRadiusK = 1.5f;

	// Temp variables mostly for calculations and graphics processing purposes
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
	private boolean shapes = IS_DESKTOP ? false : true;
	private boolean render3D = false;
	private boolean linesMode = false;
	private boolean smokeMode = false;
	private boolean whiteBackground = false;
	private boolean particleRendering = true;
	private boolean viscoElasticityEnabled = false;
	private boolean plasticityEnabled = false;
	private boolean initializePlasticity = false;
	private boolean initializeViscoelasticity = false;
	private boolean enableBox2DCollisions = false;
	private boolean glowMode = false;
	private boolean bgMode = false;

	public FluidSimulatorLiquid(FluidSimulatorStarter fluidSimulatorStarter) {
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
		// Boxes
		createPiece(6, 0, 160, 0, 0, 0, false, false, true);
		createPiece(14, -150, 200, 0, 0, 0, false, false, true);
		createPiece(14, 0, 140, 0, 0, 0, false, false, true);
		createPiece(14, -170, 60, 90, 0, 0, false, false, true);
		// Ball
		createPiece(4, 100, 100, 0, -20, 0, false, false, true);
		// Portals
		createPortalIn(-140, 65, 0, 0);
		createPortalOut(-140, 240, 0, 0);
		
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
		particles.initialize();
		createWorld();
	}

	// TODO: Query only particles within collision range
	public void box2dFluidSolver(Piece piece, Particle particle, float deltaTime) {
		if (particle.rigidBodyHit && particle.contactPieceHash == piece.hash
				&& (piece.body.getPosition().dst2(particle.contactPoint) > piece.body.getPosition().dst2(particle.pos)
						|| particle.pos.dst2(particle.contactPoint) > STICKINESS_DIST2)) {
			particle.rigidBodyHit = false;
		}

		vec2.set(particle.pos.x + (deltaTime * particle.velocity.x) * 1.5f,
				particle.pos.y + (deltaTime * particle.velocity.y) * 1.5f);
		
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
				particle.pos.set(particle.lastSafePos);
				if (piece.type == BodyType.DynamicBody) {
					particle.pos.set(collisionPoint);
					particle.pos.add(collisionNormal.x * 0.5f, collisionNormal.y * 0.5f);
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
				particle.prevPos.set(particle.pos);
				particle.pos.set(particle.pos.x + (deltaTime * particle.velocity.x),
						particle.pos.y + (deltaTime * particle.velocity.y));

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
				if (piece.body.getFixtureList().get(0).testPoint(particle.pos)) {
//					System.out.println("asd");
					particle.pos.set(particle.lastSafePos);
					particle.prevPos.set(particle.pos);
				}
			}
		}
		else {
//			if (!piece.body.getFixtureList().get(0).testPoint(particle.prevPos))
				particle.lastSafePos.set(particle.prevPos);
			
			if (particle.rigidBodyHit && particle.contactPieceHash == piece.hash) {
				if (piece.isSticky) {
					tempVect.set(particle.velocity);
					vec2.set(particle.pos);
					vec2.sub(particle.contactPoint);
					vec2.nor();
					// Calculate stickiness
					tempFloat = particle.pos.dst(particle.contactPoint);
					tempFloat = K_STICKINESS * tempFloat * (1 - (tempFloat / STICKINESS_DIST));
					if (tempFloat > 0) {
						vec2.scl(-tempFloat);
						tempVect.add(vec2);
					}
	//					System.out.println(vec2.len());
					particle.velocity.set(tempVect);
					// Re-update position
					particle.prevPos.set(particle.pos);
					particle.pos.set(particle.pos.x + (deltaTime * particle.velocity.x),
							particle.pos.y + (deltaTime * particle.velocity.y));
				}
			}
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
			for (i=0; i<len; i++) {
				mainP = particles.get(i);
				applyGravity(mainP);
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

		time = System.currentTimeMillis();
		if (waitingRehash)
			particles.rehash();
		waitingRehash = !waitingRehash;
		if (DEBUG_ENABLED)
			System.out.print("\t rehash: " + (System.currentTimeMillis() - time));

		if (viscoElasticityEnabled) {
			if (initializeViscoelasticity) {
				initializeViscoelasticity();
				initializeViscoelasticity = false;
			}
			calculateViscoelasticity(deltaTime);
		} else if (plasticityEnabled) {
			if (initializePlasticity) {
				initializePlasticity();
				initializePlasticity = false;
			}
			calculatePlasticity(deltaTime);
		}

		time = System.currentTimeMillis();
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

		drawParticles.clear();
		drawParticles.addAll(particles.table);
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
		for (i=0; i<len; i++) {
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
			mainP = particles.get(i);
			mainP.prevVelocity.set(mainP.velocity);
			mainP.velocity.set((mainP.pos.x - mainP.prevPos.x) / deltaTime,
					(mainP.pos.y - mainP.prevPos.y) / deltaTime);
			if (!enableBox2DCollisions)
				wallCollision(mainP);
			attract(mainP);
			repulse(mainP);
//				applyGravity(mainP);
			if (enableBox2DCollisions) {
				portalFluidSolver(mainP, deltaTime);
			}
			capVelocity(mainP.velocity);
			if (IS_DESKTOP)
				prepareDeleteOutOfBoundsParticles(mainP);
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

			tempParticles = particles.nearby(mainP);
			len2 = tempParticles.size();
//			tempParticles2 = particles.nearby(mainP);
//			len2 = particles.lastSizeNearby();
	        
	        // Particle pressure calculated by particle proximity
            // Pressures = 0 if all particles within range are H distance away
            p = 0.0f;
            pnear = 0.0f;
    		for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
    			neighborP = tempParticles.get(a);
//    			neighborP = tempParticles2[a];
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
            
            // Now actually apply the forces
            pressure = (p - 5f) / 2.0f; //normal pressure term
            presnear = (pnear) / 2.0f; //near particles term
    		
            changex = 0.0f;
            changey = 0.0f;
        	for (a=0; a<len2 && a<particles.MAX_NEIGHBORS; a++) {
        		neighborP = tempParticles.get(a);
//    			neighborP = tempParticles2[a];
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
						if (viscoElasticityEnabled)
							springPresenceTable.put(tempParticle.hash,
									new ArrayList<Integer>(SIZE));
						tempParticle.velocity.set(tempParticle.velocity.x,
								tempParticle.velocity.y - EMITTER_FORCE * deltaTime);
					}
				}
			}
		}
	}

	private void initializePlasticity() {
		springs.clear();
		len = particles.size();
		for (i=0; i<len; i++) {
			mainP = particles.get(i);
			for (j=0; j<len; j++) {
				neighborP = particles.get(j);
				if (mainP.hash == neighborP.hash)
					continue;
				q = mainP.pos.dst(neighborP.pos);
				rij.set(neighborP.pos);
				rij.sub(mainP.pos);
				rij.scl(1 / q);
				if (q < REST_LENGTH) {
					springs.add(new Spring(mainP, neighborP, q));
				}
			}
			mainP.velocity.set(0, 0);
		}
	}

	private void calculatePlasticity(float deltaTime) {
		len2 = springs.size();
		for (i=0; i<len2; i++) {
			springs.get(i).update();
			if (springs.get(i).currentDistance == 0)
				continue;
			rij.set(springs.get(i).pj.pos);
			rij.sub(springs.get(i).pi.pos);
			rij.scl(1 / springs.get(i).currentDistance);
//			D = deltaTime2 * K_SPRING * (1 - (springs.get(i).restLength/REST_LENGTH)) *(springs.get(i).restLength - springs.get(i).currentDistance);
			D = deltaTime * K_SPRING * (springs.get(i).restLength - springs.get(i).currentDistance);
			rij.scl(D * 0.5f);
			springs.get(i).pi.pos.set(springs.get(i).pi.pos.x - rij.x, springs.get(i).pi.pos.y - rij.y);
			springs.get(i).pj.pos.set(springs.get(i).pj.pos.x + rij.x, springs.get(i).pj.pos.y + rij.y);
		}
	}

	private void initializeViscoelasticity() {
		len = particles.size();
		for (i=0; i<len; i++) {
			mainP = particles.get(i);
			springPresenceTable.put(mainP.hash, new ArrayList<Integer>(SIZE));
			mainP.velocity.set(0, 0);
		}
	}

	/** NOTE: still in testing 
	 * tune YELD_RATIO_STRETCH, YELD_RATIO_COMPRESS and K_SPRING
	 * to test viscoelasticity
	 **/
	private void calculateViscoelasticity(float deltaTime) {
//		deltaTime2 = (deltaTime * deltaTime);
		len = particles.size();
		for (i=0; i<len; i++) {
			mainP = particles.get(i);
			tempParticles = particles.nearby(mainP);
			len2 = tempParticles.size();
//			tempParticles2 = particles.nearby(mainP);
//			len2 = particles.lastSizeNearby();
//			tempParticles2 = mainP.neighbors;
//			len2 = mainP.neighborsSize;
			if (tempParticles.size() <= 1)
				continue;
			for (j=0; j<len2 && j<particles.MAX_NEIGHBORS; j++) {
				neighborP = tempParticles.get(j);
				neighborHash = neighborP.hash;
				if (mainP.hash == neighborHash
						|| mainP.pos.dst2(neighborP.pos) > REST_LENGTH)
					continue;
//				q = mainP.pos.dst(neighborP.pos);
				if (!springPresenceTable.get(mainP.hash).contains(
						neighborHash)) {
					springs.add(new Spring(mainP, neighborP, REST_LENGTH));
					springPresenceTable.get(mainP.hash).add(neighborHash);
				}
			}
		}

		for (springIter = springs.iterator(); springIter.hasNext();) {
			tempSpring = springIter.next();
			tempSpring.update();
			deformation = tempSpring.restLength * YELD_RATIO_STRETCH;
			// Stretch
			if (tempSpring.currentDistance > (tempSpring.restLength + deformation)) {
				tempSpring.restLength += deltaTime * PLASTICITY
						* (tempSpring.currentDistance - tempSpring.restLength - (YELD_RATIO_STRETCH * tempSpring.restLength));
			}
			// Compress
			else {
				deformation = tempSpring.restLength * YELD_RATIO_COMPRESS;
				if (tempSpring.currentDistance < (tempSpring.restLength - deformation)) {
					tempSpring.restLength -= deltaTime * PLASTICITY
						* (tempSpring.restLength - tempSpring.currentDistance - (YELD_RATIO_COMPRESS * tempSpring.restLength));
				}
			}
			// Remove springs with restLength longer than REST_LENGTH
			if (tempSpring.restLength > REST_LENGTH) {
				springIter.remove();
				springPresenceTable.get(tempSpring.pi.hash).remove(
						(Integer) tempSpring.pj.hash);
			} else {
				if (tempSpring.currentDistance == 0)
					continue;
				rij.set(tempSpring.pj.pos);
				rij.sub(tempSpring.pi.pos);
				rij.scl(1 / tempSpring.currentDistance);
//				D = deltaTime2 * K_SPRING * (1 - (tempSpring.restLength/REST_LENGTH)) * (tempSpring.restLength - tempSpring.currentDistance);
				D = deltaTime * K_SPRING * (tempSpring.restLength - tempSpring.currentDistance);
				rij.scl(D * 0.5f);
				tempSpring.pi.pos.set(tempSpring.pi.pos.x - rij.x, tempSpring.pi.pos.y - rij.y);
				tempSpring.pj.pos.set(tempSpring.pj.pos.x + rij.x, tempSpring.pj.pos.y + rij.y);
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
				batch.setProjectionMatrix(camera.combined);
				batch.begin();
//					for (i=0; i<len; i++) {
//						mainP = particles.get(i);
				for (Particle mainP : drawParticles) {
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
			font.draw(batch, "(SPACE) Fluid Type: " + emitType
					+ "    (+/-) Gravity: " + GRAVITY_FORCE
					+ "    (UP/DOWN) Emitter: " + EMITTER_SIZE
					+ "    (E) Expand: " + expandMode + "    (S) Slow: "
					+ slowMotion + "    (C) Crazy: " + crazyMode
					+ "    (L) Lines: " + linesMode + "    (Q) Shapes: "
					+ shapes, 180.0f, Gdx.graphics.getHeight());
			font.draw(batch, "(K) Smoke: " + smokeMode 
					+ "    (P) Plasticity: " + plasticityEnabled 
					+ "    (V) ViscoElasticity: " + viscoElasticityEnabled 
					+ "    (R) Particle Render: " + particleRendering 
					+ "    (M) Mass: " + massMode
					+ "    (B) Box2DColl: " + enableBox2DCollisions
					+ "    (G) Glow: " + glowMode, 180, Gdx.graphics.getHeight() - 20);
			font.draw(batch,"K: " + K + "    Sigma: " + SIGMA
							+ "    Density: " + P0 + "    H: " + H
							+ "    Cell: " + CELL_SIZE + "    K_: " + K_
							+ "    Rad: " + dropRadiusK 
							+ "    K_SRING: " + K_SPRING 
							+ "    MN: " + particles.MAX_NEIGHBORS 
							+ "    Step: " + FIXED_DELTA
							+ "    STICK: " + K_STICKINESS
							+ "    RAD: " + RAD
							+ "    VISC: " + VISC
							+ "    DAMP: " + DAMPING, 180, Gdx.graphics.getHeight() - 40);
			font.draw(batch,"camera3D: " + camera3D.position, 180, Gdx.graphics.getHeight() - 60);
			batch.end();
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

		// H
		else if (keycode == Input.Keys.F4 && H < 200.0f) {
			H += 5.0f;
			H2 = H * H;
			ATTRACT_RANGE = H2 / 2;
			REPULSE_RANGE = H2 / 4;
		} else if (keycode == Input.Keys.NUM_4 && H > 5.0f) {
			H -= 5.0f;
			H2 = H * H;
			ATTRACT_RANGE = H2 / 2;
			REPULSE_RANGE = H2 / 4;
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

		// K_SPRING
		if (keycode == Input.Keys.F9 && K_SPRING < 10.0f)
			K_SPRING += 0.2f;
		else if (keycode == Input.Keys.NUM_9 && K_SPRING > 0.2f)
			K_SPRING -= 0.2f;
		
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
			for (Particle pi : particles)
				disposableParticles.add(pi);
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

		// Enable/Disable HUD
		if (keycode == Input.Keys.H)
			hudEnabled = !hudEnabled;
		
		// Mass Mode
		if (keycode == Input.Keys.M) {
			massMode = !massMode;
		}

		// Enable/Disable Plasticity
		if (keycode == Input.Keys.P && !viscoElasticityEnabled && IS_DESKTOP) {
			plasticityEnabled = !plasticityEnabled;
			if (plasticityEnabled) {
				initializePlasticity = true;
				if (springs == null && springPresenceTable == null) {
					springs = new ArrayList<Spring>(SIZE * 230);
					springPresenceTable = new FastMap<Integer, ArrayList<Integer>>(
							SIZE);
				}
			} else {
				springs.clear();
				springPresenceTable.clear();
			}
		}

		// Enable/Disable ViscoElasticity
		if (keycode == Input.Keys.V && !plasticityEnabled && IS_DESKTOP) {
			viscoElasticityEnabled = !viscoElasticityEnabled;
			if (viscoElasticityEnabled) {
				initializeViscoelasticity = true;
				if (springs == null && springPresenceTable == null) {
					springs = new ArrayList<Spring>(SIZE * 230);
					springPresenceTable = new FastMap<Integer, ArrayList<Integer>>(
							SIZE);
				}
				springs.clear();
				springPresenceTable.clear();
			} else {
				springs.clear();
				springPresenceTable.clear();
				System.gc();
			}
		}
		
		// Enable/Disable Shapes mode
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
		}
		else if (keycode == Input.Keys.ALT_RIGHT) {
			DAMPING -= 0.01f;
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
}