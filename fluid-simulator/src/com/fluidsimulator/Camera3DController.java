package com.fluidsimulator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;

public class Camera3DController extends CameraInputController {

	private int touched;
	private boolean multiTouch;
	private FluidSimulatorGeneric simulator;
	
	public Camera3DController(final Camera camera) {
		super(camera);
	}

	protected Camera3DController(final CameraGestureListener gestureListener, final Camera camera) {
		super(gestureListener, camera);
	}
	
	
	public void setFluidSimulator(FluidSimulatorGeneric simulator) {
		this.simulator = simulator;
	}
	

	@Override
	public boolean keyUp (int keycode) {
		boolean toReturn = super.keyUp(keycode);
		if (keycode == Keys.C) {
			Gdx.input.setInputProcessor(simulator);
			simulator.camera3D.position.set(0, 130f, 250f);
			simulator.camera3D.lookAt(0,150f,0);
			simulator.camera3D.near = 0.1f;
			simulator.camera3D.far = 300f;
			update();
		}
		return toReturn;
	}
	
	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		boolean toReturn = super.touchDown(screenX, screenY, pointer, button);
		touched |= (1 << pointer);
		multiTouch = !MathUtils.isPowerOfTwo(touched);
		if (multiTouch) {
//			Gdx.input.setInputProcessor(simulator);
		}
		return toReturn;
	}

	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		boolean toReturn = super.touchUp(screenX, screenY, pointer, button);
		touched &= -1 ^ (1 << pointer);
		multiTouch = !MathUtils.isPowerOfTwo(touched);
		return toReturn;
	}
}
