package com.fluidsimulator;

import java.util.ArrayList;

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
import com.fluidsimulator.utils.Vector2;

public class FluidSimulatorMPM extends FluidSimulatorGeneric {

	// MPM
	private MPMSolver mpmSolver;

	// FPS Management
	private float TICKS_PER_SECOND = IS_DESKTOP ? 60 : 50;
	private float SKIP_TICKS = 1 / TICKS_PER_SECOND;
	private float FIXED_DELTA = 1.0f / 60.0f;
	private final int MAX_FRAMESKIP = 1;
	private int speedMultiplier = IS_DESKTOP ? 1 : 1;
	
	// Tune theses for platform specific behaviors
	private float GRAVITY_FORCE = IS_DESKTOP ? -50.0f : -50.0f;
	private final Vector2 gravityVect = new Vector2(0.0f, GRAVITY_FORCE);


	// Simulation management
	// Most of these can be tuned at runtime with F1-F10 and keys 1-0 (no numpad)
	private float DAMPING = 0.99f;
	private float EMITTER_FORCE = 10;
	private float EMITTER_SIZE = 5;
	private final int PARTICLE_SIZE = 5;
	private float dropRadiusK = 0.9f;

	// Modes
	private int emitType = 2;
	private boolean slowMotion = false;
	private boolean hudEnabled = IS_DESKTOP ? true : false;
	private boolean render3D = false;
	private boolean whiteBackground = false;

	public FluidSimulatorMPM(FluidSimulatorStarter fluidSimulatorStarter) {
		super(fluidSimulatorStarter);
		camera.position.set(WORLD_WIDTH/2, WORLD_HEIGHT/2, 0);
		camera3D.position.set(240, 130f, 250f);
		camera3D.lookAt(240,140f,00);
		camera3D.update();
		setupPieces();

		// MPM
		BOX_WIDTH = IS_DESKTOP ? 480 : 240;
		BOX_HEIGHT = IS_DESKTOP ? 320 : 180;
		BOX_WIDTH_HALF = BOX_WIDTH / 2;
		BOX_HEIGHT_HALF = BOX_HEIGHT / 2;
		mpmSolver = new MPMSolver(BOX_WIDTH, BOX_HEIGHT, 50, 200);
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
		createWorld();
	}

	public  void performLogic(float deltaTime) {
		if (!firstRender)
			return;
		
		
		if (IS_DESKTOP && !isDragging)
			spawnParticles(deltaTime);

		time = System.currentTimeMillis();
		
//		mpmSolver.simulate();
		mpmSolver.simulateSimple();
		if (DEBUG_ENABLED)
			System.out.print("\t fluid: " + (System.currentTimeMillis() - time));
		
		// Gravity change with mobile accelerometer
//		if (Gdx.input.isPeripheralAvailable(Peripheral.Accelerometer) && (Gdx.input.getAccelerometerX() >= 0 || Gdx.input.getAccelerometerY() >= 0)) {
//			gravityVect.set(-Gdx.input.getAccelerometerY() * GRAVITY_FORCE * 0.5f,Gdx.input.getAccelerometerX() * GRAVITY_FORCE * 0.5f);
//		}
	}
	
	private void spawnParticles(float deltaTime) {
		if (touching && !isAttracting && !isRepulsing) {
			if (!((testPoint2D.x < (-BOX_WIDTH / 2 + wpadding) || testPoint2D.x > (BOX_WIDTH / 2 - wpadding))
					|| (testPoint2D.y < (INITIAL_HEIGHT + hpadding) || testPoint2D.y > (BOX_HEIGHT - hpadding)))) {
				for (float i = -EMITTER_SIZE; i < EMITTER_SIZE; i += 10.0f) {
					for (float j = -EMITTER_SIZE; j < EMITTER_SIZE; j += 5.0f) {
						mpmSolver.addParticle((int)(testPoint2D.x + i), WORLD_HEIGHT - (int)(testPoint2D.y - j), 0);
					}
				}
			}
			mpmSolver.particleArray = mpmSolver.particles.toArray(mpmSolver.particleArray);
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

		
		// Begin Batch
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
							+ ", particles: " + (mpmSolver.particles.size()), 0, Gdx.graphics.getHeight() - 40);
			font.draw(batch, "deltaTime: " + deltaTime, 0, Gdx.graphics.getHeight() - 20);
			font.draw(batch, "pressure: " + (totalPressure - lastTotalPressure), 0, Gdx.graphics.getHeight());
			font.draw(batch, "(SPACE) Fluid Type: " + emitType
					+ "    (+/-) Gravity: " + GRAVITY_FORCE
					+ "    (UP/DOWN) Emitter: " + EMITTER_SIZE
					+ "    (S) Slow: "
					+ slowMotion, 180.0f, Gdx.graphics.getHeight());
			font.draw(batch,"Rad: " + dropRadiusK 
							+ "    Step: " + FIXED_DELTA
							+ "    DAMP: " + DAMPING, 180, Gdx.graphics.getHeight() - 20);
			font.draw(batch,"camera3D: " + camera3D.position, 180, Gdx.graphics.getHeight() - 40);
			font.draw(batch,"M: " + mpmSolver.materials.get(0).mass 
					+ "    D: " + mpmSolver.materials.get(0).restDensity
					+ "    Stiff: " + mpmSolver.materials.get(0).stiffness 
					+ "    BVisc: " + mpmSolver.materials.get(0).bulkViscosity
					+ "    SurfTens: " + mpmSolver.materials.get(0).surfaceTension 
					+ "    Elastic: " + mpmSolver.materials.get(0).kElastic
					+ "    Deform: " + mpmSolver.materials.get(0).maxDeformation 
					+ "    Melt: " + mpmSolver.materials.get(0).meltRate 
					+ "    Visc: " + mpmSolver.materials.get(0).viscosity 
					+ "    Damping: " + mpmSolver.materials.get(0).damping
					+ "    Friction: " + mpmSolver.materials.get(0).friction
					+ "    Smoothing: " + mpmSolver.materials.get(0).smoothing
					+ "    Grav: " + mpmSolver.materials.get(0).gravity, 180, Gdx.graphics.getHeight() - 60);
			batch.end();
		}
		
		//3D
		if (render3D) {
	//		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//			camera3D.update();
			camController.update();
			modelBatch.begin(camera3D);
			for (MPMSolver.Particle p : mpmSolver.particles) {
				dropCoordinate.set((float)p.x - dropRadius,
						WORLD_HEIGHT - (float)p.y - dropRadius, 0.0f);
				instance.transform.setToTranslation(dropCoordinate.x, dropCoordinate.y, 0);
		        modelBatch.render(instance, environment);
			}
	        modelBatch.end();
		}
		else {
			batch.setProjectionMatrix(camera.combined);
			batch.setColor(0, 1, 0, 1);
			batch.begin();
			for (MPMSolver.Particle p : mpmSolver.particles) {
				dropCoordinate.set((float)p.x - dropRadius,
						WORLD_HEIGHT - (float)p.y - dropRadius, 0.0f);
	//			if (p.mat.materialIndex == 0)
	//				batch.setColor(0, 0, 1, 1);
	//			else if (p.mat.materialIndex == 1)
	//				batch.setColor(0, 1, 0, 1);
	//			else if (p.mat.materialIndex == 2)
	//				batch.setColor(1, 0, 0, 1);
				batch.draw(dropTexture2, dropCoordinate.x, dropCoordinate.y, dropRadiusPixel, dropRadiusPixel);
			}
			batch.end();
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
		

		if (keycode == Input.Keys.BACKSPACE && isAttracting) {
			mpmSolver.particles.clear();
		} else if (keycode == Input.Keys.BACKSPACE && IS_DESKTOP) {
			mpmSolver.particles.clear();
		}

		// Change Particle color
		if (keycode == Input.Keys.SPACE) {
			emitType += 1;
			if (emitType > 3) {
				emitType = 1;
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

		// Enable/Disable Stop Motion
		if (keycode == Input.Keys.S)
			slowMotion = !slowMotion;
		
		// Enable/Disable HUD
		if (keycode == Input.Keys.H)
			hudEnabled = !hudEnabled;

		// Enable/Disable Particle Rendering
		if (keycode == Input.Keys.R) {
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
		
		
		// MPM

		// MASS
		if (keycode == Input.Keys.F1) {
			mpmSolver.materials.get(0).mass += 0.1f;
		} else if (keycode == Input.Keys.NUM_1 && mpmSolver.materials.get(0).mass > 0) {
			mpmSolver.materials.get(0).mass -= 0.1f;
		}

		// REST DENSITY
		else if (keycode == Input.Keys.F2)
			mpmSolver.materials.get(0).restDensity += 0.2f;
		else if (keycode == Input.Keys.NUM_2 && mpmSolver.materials.get(0).restDensity > 0)
			mpmSolver.materials.get(0).restDensity -= 0.2f;

		// STIFFNESS
		else if (keycode == Input.Keys.F3)
			mpmSolver.materials.get(0).stiffness += 0.1f;
		else if (keycode == Input.Keys.NUM_3 && mpmSolver.materials.get(0).stiffness > 0f)
			mpmSolver.materials.get(0).stiffness -= 0.1f;

		// BULK VISCOSITY
		else if (keycode == Input.Keys.F4) {
			mpmSolver.materials.get(0).bulkViscosity += 0.1f;
		} else if (keycode == Input.Keys.NUM_4 && mpmSolver.materials.get(0).bulkViscosity > 0) {
			mpmSolver.materials.get(0).bulkViscosity -= 0.1f;
		}

		// SURFACE TENSION
		else if (keycode == Input.Keys.F5) {
			mpmSolver.materials.get(0).surfaceTension += 0.1f;
		} else if (keycode == Input.Keys.NUM_5 && mpmSolver.materials.get(0).surfaceTension > 0) {
			mpmSolver.materials.get(0).surfaceTension -= 0.1f;
		}

		// ELASTICITY
		if (keycode == Input.Keys.F6) {
			mpmSolver.materials.get(0).kElastic += 0.1f;
		} else if (keycode == Input.Keys.NUM_6 && mpmSolver.materials.get(0).kElastic > 0) {
			mpmSolver.materials.get(0).kElastic -= 0.1f;
		}

		// MAX DEFORMATION
		if (keycode == Input.Keys.F7) {
			mpmSolver.materials.get(0).maxDeformation += 0.1f;
		} else if (keycode == Input.Keys.NUM_7 && mpmSolver.materials.get(0).maxDeformation > 0) {
			mpmSolver.materials.get(0).maxDeformation -= 0.1f;
		}

		// MELT RATE
		if (keycode == Input.Keys.F8) {
			mpmSolver.materials.get(0).meltRate += 0.1f;
		} else if (keycode == Input.Keys.NUM_8 && mpmSolver.materials.get(0).meltRate > 0) {
			mpmSolver.materials.get(0).meltRate -= 0.1f;
		}

		// VISCOSITY
		if (keycode == Input.Keys.F9)
			mpmSolver.materials.get(0).viscosity += 0.01f;
		else if (keycode == Input.Keys.NUM_9 && mpmSolver.materials.get(0).viscosity > 0)
			mpmSolver.materials.get(0).viscosity -= 0.01f;
		
		// DAMPING
		if (keycode == Input.Keys.F10)
			mpmSolver.materials.get(0).damping += 0.001f;
		else if (keycode == Input.Keys.NUM_0 && mpmSolver.materials.get(0).damping > 0)
			mpmSolver.materials.get(0).damping -= 0.001f;
		
		// FRICTION
		if (keycode == Input.Keys.F11)
			mpmSolver.materials.get(0).friction += 0.1f;
		else if (keycode == Input.Keys.LEFT && mpmSolver.materials.get(0).friction > 0)
			mpmSolver.materials.get(0).friction -= 0.1f;
		
		// SMOOTHING
		if (keycode == Input.Keys.F12)
			mpmSolver.materials.get(0).smoothing += 0.1f;
		else if (keycode == Input.Keys.RIGHT && mpmSolver.materials.get(0).smoothing > 0)
			mpmSolver.materials.get(0).smoothing -= 0.1f;

		// GRAV
		if ((keycode == Input.Keys.PLUS) && (mpmSolver.materials.get(0).gravity < 0.9f)) {
			mpmSolver.materials.get(0).gravity += 0.03f;
		} else if ((keycode == Input.Keys.MINUS) && (mpmSolver.materials.get(0).gravity > 0.0f)) {
			mpmSolver.materials.get(0).gravity -= 0.03f;
			if (mpmSolver.materials.get(0).gravity < 0)
				mpmSolver.materials.get(0).gravity = 0;
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
			
			// MPM
			mpmSolver.pressed = true;
			mpmSolver.mx = (int)testPoint2D.x;
			mpmSolver.my = BOX_HEIGHT - (int)testPoint2D.y;

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
		
		// MPM Fluid interaction
		mpmSolver.mx = (int)testPoint2D.x;
		mpmSolver.my = BOX_HEIGHT - (int)testPoint2D.y;
		
		
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
		
		// MPM Fluid interaction 
		mpmSolver.pressed = false;
		
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
		
		if (pointer == 2) {
			emitType ++;
			if (emitType > 3)
				emitType = 1;
		}
		
		return false;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void hide() {
		super.hide();
	}
	
}