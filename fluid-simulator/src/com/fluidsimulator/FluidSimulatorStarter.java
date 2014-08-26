package com.fluidsimulator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.badlogic.gdx.Game;

public class FluidSimulatorStarter extends Game {
	FluidSimulatorGeneric fluidSimulatorScreen;
	private String solverMethod = "LIQUID";

	@Override
	public void create() {
		
		Properties prop = new Properties();
		InputStream input = null;
	 
		try {
	 
			input = new FileInputStream("config.properties");
			prop.load(input);
			solverMethod = prop.getProperty("solver");
			System.out.println("Solver selected: " + solverMethod);
	 
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Failed to read config.properties file, falling back to LIQUID default solver algorithm");
				}
			}
		}
		
		
		setScreen(switchToFluidSimulator());
	}
	
	public FluidSimulatorGeneric switchToFluidSimulator() {
		/**
		 * remove comment from the line corresponding
		 * to the simulation solver you want to run
		 */
		if (fluidSimulatorScreen == null) {
			
			if (solverMethod.equals("SPH")) {
				//SPH (Viscoelastic Smoothed Particle Hidrodynamics)
				fluidSimulatorScreen = new FluidSimulatorSPH(this);
			}
			else if (solverMethod.equals("LIQUID")) {
				// Liquid (Heavily customized and optimized SPH)
				// NOTE: Box2D two-way coupling currently works only with this solver
				fluidSimulatorScreen = new FluidSimulatorLiquid(this);
			}
			else if (solverMethod.equals("PBF")) {
				// PBF (Position Based Fluid)
				fluidSimulatorScreen = new FluidSimulator(this);
			}
			else if (solverMethod.equals("MPM")) {
				// MPM (Material Point Method)
				fluidSimulatorScreen = new FluidSimulatorMPM(this);
			}
		}
		return fluidSimulatorScreen;
	}
}
