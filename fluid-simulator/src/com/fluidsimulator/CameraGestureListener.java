package com.fluidsimulator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.input.GestureDetector.GestureAdapter;

public class CameraGestureListener extends GestureAdapter {
	public CameraInputController controller;
	private float previousZoom; 
	@Override
	public boolean touchDown (float x, float y, int pointer, int button) {
		previousZoom = 0;
		return false;
	}

	@Override
	public boolean tap (float x, float y, int count, int button) {
		return false;
	}

	@Override
	public boolean longPress (float x, float y) {
		return false;
	}

	@Override
	public boolean fling (float velocityX, float velocityY, int button) {
		return false;
	}

	@Override
	public boolean pan (float x, float y, float deltaX, float deltaY) {
		return false;
	}

	@Override
	public boolean zoom (float initialDistance, float distance) {
		float newZoom = distance - initialDistance;
		float amount = newZoom - previousZoom;
		previousZoom = newZoom;
		float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
		return controller.zoom(amount / ((w > h) ? h : w));
	}
};