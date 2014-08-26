package com.fluidsimulator;

import java.awt.Toolkit;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopStarter {

	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.fullscreen = true;
		config.resizable = false;
		config.title = "Fluid Simulator v2.1";
		config.vSyncEnabled = true;
		config.width = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		config.height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
//		config.width = 800;
//		config.height = 480;
		config.useGL20 = true;
		new LwjglApplication(new FluidSimulatorStarter(), config);
	}

}
