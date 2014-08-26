package com.fluidsimulator.gameobjects.fluid;


public class Spring {
	
	public Particle pi;
	public Particle pj;
	public float restLength;
	public float currentDistance;
	
	public Spring(Particle p1, Particle p2, float restLength) {
		this.pi = p1;
		this.pj = p2;
		this.restLength = restLength;
	}
	
	public void update() {
		this.currentDistance = pi.pos.dst(pj.pos);
	}
	
	public boolean contains(Particle p) {
		return (pi.hashCode() == p.hashCode() || pj.hashCode() == p.hashCode());
	}
}
