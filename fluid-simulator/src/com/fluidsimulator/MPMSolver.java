package com.fluidsimulator;

import java.util.ArrayList;

public class MPMSolver {

	public static final int numMaterials = 2;

	public Particle[] particleArray = new Particle[10000];
	public ArrayList<Particle> particles = new ArrayList<Particle>();
	public Node[] grid;
	public ArrayList<Node> active = new ArrayList<Node>();
	
	public boolean pressed = false;
	public boolean pressedprev = false;
	public int mx = 0;
	public int my = 0;
	public int mxprev = 0;
	public int myprev = 0;

	public int gSizeX, gSizeY, gSizeY_3;
	public int i, j, k, n, l, len;
	public Particle p;
	public ArrayList<Material> materials = new ArrayList<Material>(numMaterials);

	public MPMSolver(int sizeX, int sizeY, int particlesX, int particlesY) {
		
		this.gSizeX = sizeX;
		this.gSizeY = sizeY;
		this.gSizeY_3 = sizeY - 3;
		// Water
		materials.add(new Material());
		materials.get(0).materialIndex = 0;
		materials.get(0).smoothing = 1;
		materials.get(0).restDensity = 0.5f;
		// Oil
		materials.add(new Material());
		materials.get(1).materialIndex = 1;
		materials.get(1).mass = 0.9f;

//		grid = new ArrayList<Node>(gSizeX*gSizeY);
//
//		for (i = 0; i < gSizeX*gSizeY; i++) {
//			grid.add(new Node());
//		}
		grid = new Node[gSizeX * gSizeY];

		for (i = 0; i < gSizeX*gSizeY; i++) {
			grid[i]= new Node();
		}
		
		for (i = 0; i < particlesX; i++) {
			for (j = 0; j < particlesY; j++) {
				Particle p = new Particle(materials.get(0), i, j);
				p.initializeWeights(gSizeY);
				particles.add(p);
			}
		}
		particleArray = particles.toArray(particleArray);
	}
	
	public void addParticle(int x, int y, int materialId) {
		Particle p = new Particle(materials.get(materialId), x, y);
		p.initializeWeights(gSizeY);
		particles.add(p);
	}

	public void simulate() {
		boolean drag = false;
		float mdx = 0, mdy = 0, weight = 0;
		if (pressed && pressedprev) {
			drag = true;
			mdx = (mx - mxprev);
			mdy = (my - myprev);
		}

		pressedprev = pressed;
		mxprev = mx;
		myprev = my;
		
		for (Particle p : particles) {
			Material mat = p.mat;
			
			float gu = 0, gv = 0, dudx = 0, dudy = 0, dvdx = 0, dvdy = 0;
			float[] ppx = p.px;
			float[] ppy = p.py;
			float[] pgx = p.gx;
			float[] pgy = p.gy;
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = ppx[i];
				float gxi = pgx[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = ppy[j];
					float gyj = pgy[j];
					float phi = pxi * pyj;
					gu += phi * n.u2;
					gv += phi * n.v2;
					float gx = gxi * pyj;
					float gy = pxi * gyj;
					// Velocity gradient
					dudx += n.u2 * gx;
					dudy += n.u2 * gy;
					dvdx += n.v2 * gx;
					dvdy += n.v2 * gy;
				}
			}
			
			// Update stress tensor
			float w1 = dudy - dvdx;
			float wT0 = .5f * w1 * (p.T01 + p.T01);
			float wT1 = .5f * w1 * (p.T00 - p.T11);
			float D00 = dudx;
			float D01 = .5f * (dudy + dvdx);
			float D11 = dvdy;
			float trace = .5f * (D00 + D11);
			p.T00 += .5f * (-wT0 + (D00 - trace) - mat.meltRate * p.T00);
			p.T01 += .5f * (wT1 + D01 - mat.meltRate * p.T01);
			p.T11 += .5f * (wT0 + (D11 - trace) - mat.meltRate * p.T11);
			
			float norm = p.T00 * p.T00 + 2 * p.T01 * p.T01 + p.T11 * p.T11;
			
			if (norm > mat.maxDeformation)
			{
				p.T00 = p.T01 = p.T11 = 0;
			}
			
			p.x += gu;
			p.y += gv;
			
			p.gu = gu;
			p.gv = gv;
			
			p.u += mat.smoothing*(gu-p.u);
			p.v += mat.smoothing*(gv-p.v);
			
			// Hard boundary correction (Random numbers keep it from clustering)
			if (p.x < 1) {
				p.x = 1 + .01f * (float)Math.random();
			} else if (p.x > gSizeX - 2) {
				p.x = gSizeX - 2 - .01f * (float)Math.random();
			}
			if (p.y < 1) {
				p.y = 1 + .01f * (float)Math.random();
			} else if (p.y > gSizeY - 2) {
				p.y = gSizeY - 2 - .01f * (float)Math.random();
			}
			
			// Update grid cell index and kernel weights
			int cx = p.cx = (int)(p.x - .5f);
			int cy = p.cy = (int)(p.y - .5f);
			p.gi = cx * gSizeY + cy;
			
			float x = cx - p.x;
			float y = cy - p.y;
			
			// Quadratic interpolation kernel weights - Not meant to be changed
			ppx[0] = .5f * x * x + 1.5f * x + 1.125f;
			pgx[0] = x + 1.5f;
			x++;
			ppx[1] = -x * x + .75f;
			pgx[1] = -2 * x;
			x++;
			ppx[2] = .5f * x * x - 1.5f * x + 1.125f;
			pgx[2] = x - 1.5f;
			
			ppy[0] = .5f * y * y + 1.5f * y + 1.125f;
			pgy[0] = y + 1.5f;
			y++;
			ppy[1] = -y * y + .75f;
			pgy[1] = -2 * y;
			y++;
			ppy[2] = .5f * y * y - 1.5f * y + 1.125f;
			pgy[2] = y - 1.5f;
			
			float m =  p.mat.mass;
			float mu = m * p.u;
			float mv = m * p.v;
			int mi = p.mat.materialIndex;
			float[] px = p.px;
			float[] gx = p.gx;
			float[] py = p.py;
			float[] gy = p.gy;
//			n = grid[p.gi);
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				float gxi = gx[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = py[j];
					float gyj = gy[j];
					float phi = pxi * pyj;
					// Add particle mass, velocity and density gradient to grid
					n.mass += phi * m;
					n.particleDensity += phi;
					n.u += phi * mu;
					n.v += phi * mv;
					n.cgx[mi] += gxi * pyj;
					n.cgy[mi] += pxi * gyj;
					n.active = true;
				}
			}
		}
		
		// Add active nodes to list
		active.clear();
		int gSizeXY = gSizeX * gSizeY;
		
		for (i = 0; i < gSizeXY; i++) {
			Node n = grid[i];
			if (n.active && n.mass > 0) {
				active.add(n);
				n.active = false;
				n.ax = n.ay = 0;
				n.gx = 0;
				n.gy = 0;
				n.u /= n.mass;
				n.v /= n.mass;
				for (j = 0; j < numMaterials; j++) {
					n.gx += n.cgx[j];
					n.gy += n.cgy[j];
				}
				for (j = 0; j < numMaterials; j++) {
					n.cgx[j] -= n.gx - n.cgx[j];
					n.cgy[j] -= n.gy - n.cgy[j];
				}
			}
		}
		
		int nActive = active.size();
		
		// Calculate pressure and add forces to grid
		for (Particle p : particles) {
			Material mat = p.mat;
			
			float fx = 0, fy = 0, dudx = 0, dudy = 0, dvdx = 0, dvdy = 0, sx = 0, sy = 0;
			Node n = grid[p.gi];
			float[] ppx = p.px;
			float[] pgx = p.gx;
			float[] ppy = p.py;
			float[] pgy = p.gy;
			
			int materialId = mat.materialIndex;
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = ppx[i];
				float gxi = pgx[i];
				for (j = 0; j < 3; j++, k++) {
					n = grid[p.gi + k];
					float pyj = ppy[j];
					float gyj = pgy[j];
					float phi = pxi * pyj;
					float gx = gxi * pyj;
					float gy = pxi * gyj;
					// Velocity gradient
					dudx += n.u * gx;
					dudy += n.u * gy;
					dvdx += n.v * gx;
					dvdy += n.v * gy;
					
					// Surface tension
					sx += phi * n.cgx[materialId];
					sy += phi * n.cgy[materialId];
				}
			}
			
			int cx = (int)p.x;
			int cy = (int)p.y;
			int gi = cx * gSizeY + cy;
			
			Node n1 = grid[gi];
			Node n2 = grid[gi+1];
			Node n3 = grid[gi+gSizeY];
			Node n4 = grid[gi+gSizeY+1];
			float density = uscip(n1.particleDensity, n1.gx, n1.gy, n2.particleDensity, n2.gx, n2.gy, n3.particleDensity, 
					n3.gx, n3.gy, n4.particleDensity, n4.gx, n4.gy, p.x - cx, p.y - cy);
			
			float pressure = mat.stiffness / mat.restDensity * (density - mat.restDensity);
			if (pressure > 2) {
				pressure = 2;
			}
			
			// Update stress tensor
			float w1 = dudy - dvdx;
			float wT0 = .5f * w1 * (p.T01 + p.T01);
			float wT1 = .5f * w1 * (p.T00 - p.T11);
			float D00 = dudx;
			float D01 = .5f * (dudy + dvdx);
			float D11 = dvdy;
			float trace = .5f * (D00 + D11);
			D00 -= trace;
			D11 -= trace;
			p.T00 += .5f * (-wT0 + D00 - mat.meltRate * p.T00);
			p.T01 += .5f * (wT1 + D01 - mat.meltRate * p.T01);
			p.T11 += .5f * (wT0 + D11 - mat.meltRate * p.T11);
			
			// Stress tensor fracture
			float norm = p.T00 * p.T00 + 2 * p.T01 * p.T01 + p.T11 * p.T11;
			
			if (norm > mat.maxDeformation)
			{
				p.T00 = p.T01 = p.T11 = 0;
			}
			
			float T00 = mat.mass * (mat.kElastic * p.T00 + mat.viscosity * D00 + pressure + trace * mat.bulkViscosity);
			float T01 = mat.mass * (mat.kElastic * p.T01 + mat.viscosity * D01);
			float T11 = mat.mass * (mat.kElastic * p.T11 + mat.viscosity * D11 + pressure + trace * mat.bulkViscosity);
			
			// Surface tension
			float lenSq = sx * sx + sy * sy;
			if (lenSq > 0)
			{
				float len = (float)Math.sqrt(lenSq);
				float a = mat.mass * mat.surfaceTension / len;
				T00 -= a * (.5f * lenSq - sx * sx);
				T01 -= a * (-sx * sy);
				T11 -= a * (.5f * lenSq - sy * sy);
			}
			
			// Wall force
			if (p.x < 4) {
				fx += (4 - p.x);
			} else if (p.x > gSizeX - 5) {
				fx += (gSizeX - 5 - p.x);
			}
			if (p.y < 4) {
				fy += (4 - p.y);
			} else if (p.y > gSizeY - 5) {
				fy += (gSizeY - 5 - p.y);
			}
			
			// Mouse Drag
			if (drag) {
				float vx = Math.abs(p.x - mx);
				float vy = Math.abs(p.y - my);
				if ((vx < 10.0f) && (vy < 10.0f)) {
					weight = p.mat.mass * (1.0f - vx * 0.10f) * (1.0f - vy * 0.10f);
					fx += weight * (mdx - p.u);
					fy += weight * (mdy - p.v);
				}
			}
			
			// Add forces to grid
			n = grid[p.gi];
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = ppx[i];
				float gxi = pgx[i];
				for (j = 0; j < 3; j++, k++) {
					n = grid[p.gi + k];
					float pyj = ppy[j];
					float gyj = pgy[j];
					float phi = pxi * pyj;
					
					float gx = gxi * pyj;
					float gy = pxi * gyj;
					n.ax += -(gx * T00 + gy * T01) + fx * phi;
					n.ay += -(gx * T01 + gy * T11) + fy * phi;
				}
			}
		}
		
		// Update acceleration of nodes
		for (i = 0; i < nActive; i++) {
			Node n = active.get(i);
			n.u2 = 0;
			n.v2 = 0;
			n.ax /= n.mass;
			n.ay /= n.mass;
		}

		for (Particle p : particles) {
			Material mat = p.mat;
			Node n = grid[p.gi];
			// Update particle velocities
			float[] px = p.px;
			float[] py = p.py;
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				for (j = 0; j < 3; j++, k++) {
					n = grid[p.gi + k];
					float pyj = py[j];
					float phi = pxi * pyj;
					p.u += phi * n.ax;
					p.v += phi * n.ay;
				}
			}
			
			p.v += mat.gravity;
			p.u *= 1-mat.damping;
			p.v *= 1-mat.damping;
			
			float m =  p.mat.mass;
			float mu = m * p.u;
			float mv = m * p.v;
			
			// Add particle velocities back to the grid
			n = grid[p.gi];
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				for (j = 0; j < 3; j++, k++) {
					n = grid[p.gi + k];
					float pyj = py[j];
					float phi = pxi * pyj;
					n.u2 += phi * mu;
					n.v2 += phi * mv;
				}
			}
		}
		
		// Update node velocities
		for (i = 0; i < nActive; i++) {
			Node n = active.get(i);
			n.u2 /= n.mass;
			n.v2 /= n.mass;
			
			n.mass = 0;
			n.particleDensity = 0;
			n.u = 0;
			n.v = 0;
//			n.cgx = new float[numMaterials];
//			n.cgy = new float[numMaterials];
			for (j=0; j<numMaterials; j++) {
				n.cgx[j] = 0;
				n.cgy[j] = 0;
			}
		}

	}
	
	public void simulateSimple() {
		boolean drag = false;
		float mdx = 0, mdy = 0, weight = 0;
		if (pressed && pressedprev) {
			drag = true;
			mdx = (mx - mxprev);
			mdy = (my - myprev);
		}

		pressedprev = pressed;
		mxprev = mx;
		myprev = my;
		
		// Reset grid nodes
		int nActive = active.size();
		for (int i = 0; i < nActive; i++) {
			active.get(i).active = false;
		}
		active.clear();
		
		// Add particle mass, velocity and density gradient to grid
//		for (Particle p : particles) {
		len = particles.size();
		for (l=0; l<len; l++) {
			p = particleArray[l];
			float[] px = p.px;
			float[] gx = p.gx;
			float[] py = p.py;
			float[] gy = p.gy;
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				float gxi = gx[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = py[j];
					float gyj = gy[j];
					float phi = pxi * pyj;
					if (n.active) {
						n.mass += phi;
						n.gx += gxi * pyj;
						n.gy += pxi * gyj;
					} else {
						n.active = true;
						n.mass = phi;
						n.gx = gxi * pyj;
						n.gy = pxi * gyj;
						n.ax = 0;
						n.ay = 0;
						active.add(n);
					}
				}
			}
		}
		
		nActive = active.size();
		
		// Calculate pressure and add forces to grid
//		for (Particle p : particles) {
		for (l=0; l<len; l++) {
			p = particleArray[l];
			Material mat = p.mat;
			
			float fx = 0, fy = 0;
			Node n = grid[p.gi];
			float[] ppx = p.px;
			float[] pgx = p.gx;
			float[] ppy = p.py;
			float[] pgy = p.gy;
			
			int cx = (int)p.x;
			int cy = (int)p.y;
			int gi = cx * gSizeY + cy;
			
			Node n1 = grid[gi];
			Node n2 = grid[gi+1];
			Node n3 = grid[gi+gSizeY];
			Node n4 = grid[gi+gSizeY+1];
			float density = uscip(n1.mass, n1.gx, n1.gy, n2.mass, n2.gx, n2.gy, n3.mass, n3.gx, n3.gy, n4.mass, n4.gx, n4.gy, p.x - cx, p.y - cy);
			
			float pressure = mat.stiffness / mat.restDensity * (density - mat.restDensity);
			if (pressure > 2) {
				pressure = 2;
			}
			
			// Wall force
			if (p.x < 4) {
				fx += (4 - p.x);
			} else if (p.x > gSizeX - 5) {
				fx += (gSizeX - 5 - p.x);
			}
			if (p.y < 4) {
				fy += (4 - p.y);
			} else if (p.y > gSizeY - 5) {
				fy += (gSizeY - 5 - p.y);
			}
			
			// Mouse Drag
			if (drag) {
				float vx = Math.abs(p.x - mx);
				float vy = Math.abs(p.y - my);
				if ((vx < 10.0f) && (vy < 10.0f)) {
					weight = p.mat.mass * (1.0f - vx * 0.10f) * (1.0f - vy * 0.10f);
					fx += weight * (mdx - p.u);
					fy += weight * (mdy - p.v);
				}
			}
			
			// Add forces to grid
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = ppx[i];
				float gxi = pgx[i];
				for (j = 0; j < 3; j++, k++) {
					n = grid[p.gi + k];
					float pyj = ppy[j];
					float gyj = pgy[j];
					float phi = pxi * pyj;
					
					float gx = gxi * pyj;
					float gy = pxi * gyj;
					n.ax += -(gx * pressure) + fx * phi;
					n.ay += -(gy * pressure) + fy * phi;
				}
			}
		}
		
		// Update acceleration of nodes
		for (int i = 0; i < nActive; i++) {
			Node n = active.get(i);
			n.u = 0;
			n.v = 0;
			if (n.mass > 0) {
				n.ax /= n.mass;
				n.ay /= n.mass;
			}
		}

//		for (Particle p : particles) {
		for (l=0; l<len; l++) {
			p = particleArray[l];
			Material mat = p.mat;
			
			// Update particle velocities
			float[] px = p.px;
			float[] py = p.py; 
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = py[j];
					float phi = pxi * pyj;
					p.u += phi * n.ax;
					p.v += phi * n.ay;
				}
			}
			
			p.v += mat.gravity;
			
			// Add particle velocities back to the grid
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = px[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = py[j];
					float phi = pxi * pyj;
					n.u += phi * p.u;
					n.v += phi * p.v;
				}
			}
		}
		
		// Update node velocities
		for (int i = 0; i < nActive; i++) {
			Node n = active.get(i);
			if (n.mass > 0) {
				n.u /= n.mass;
				n.v /= n.mass;
			}
		}
		
		// Advect particles
//		for (Particle p : particles) {
		for (l=0; l<len; l++) {
			p = particleArray[l];
			Material mat = p.mat;
			
			float gu = 0, gv = 0;
			float[] ppx = p.px;
			float[] ppy = p.py;
			float[] pgx = p.gx;
			float[] pgy = p.gy;
			for (i = 0, k = 0; i < 3; i++, k += gSizeY_3) {
				float pxi = ppx[i];
				for (j = 0; j < 3; j++, k++) {
					Node n = grid[p.gi + k];
					float pyj = ppy[j];
					float phi = pxi * pyj;
					gu += phi * n.u;
					gv += phi * n.v;
				}
			}
			
			p.x += gu;
			p.y += gv;
			
			p.u += mat.smoothing*(gu-p.u);
			p.v += mat.smoothing*(gv-p.v);
			
			// Hard boundary correction (Random numbers keep it from clustering)
			if (p.x < 1) {
				p.x = 1 + .01f*(float)Math.random();
			} else if (p.x > gSizeX - 2) {
				p.x = gSizeX - 2 - .01f*(float)Math.random();
			}
			if (p.y < 1) {
				p.y = 1 + .01f*(float)Math.random();
			} else if (p.y > gSizeY - 2) {
				p.y = gSizeY - 2 - .01f*(float)Math.random();
			}
			
			// Update grid cell index and kernel weights
			int cx = p.cx = (int)(p.x - .5f);
			int cy = p.cy = (int)(p.y - .5f);
			p.gi = cx * gSizeY + cy;
			
			float x = cx - p.x;
			float y = cy - p.y;
			
			// Quadratic interpolation kernel weights - Not meant to be changed
			ppx[0] = .5f * x * x + 1.5f * x + 1.125f;
			pgx[0] = x + 1.5f;
			x++;
			ppx[1] = -x * x + .75f;
			pgx[1] = -2 * x;
			x++;
			ppx[2] = .5f * x * x - 1.5f * x + 1.125f;
			pgx[2] = x - 1.5f;
			
			ppy[0] = .5f * y * y + 1.5f * y + 1.125f;
			pgy[0] = y + 1.5f;
			y++;
			ppy[1] = -y * y + .75f;
			pgy[1] = -2 * y;
			y++;
			ppy[2] = .5f * y * y - 1.5f * y + 1.125f;
			pgy[2] = y - 1.5f;
		}
	}
	
	public float uscip(float p00, float x00, float y00, float p01, float x01, float y01, float p10, float x10, 
			float y10, float p11, float x11, float y11, float u, float v)
	{
		float dx = x00 - x01;
		float dy = y00 - y10;
		float a = p01 - p00;
		float b = p11 - p10 - a;
		float c = p10 - p00;
		float d = y11 - y01;
		return ((((d - 2 * b - dy) * u - 2 * a + y00 + y01) * v +
				 ((3 * b + 2 * dy - d) * u + 3 * a - 2 * y00 - y01)) * v +
				((((2 * c - x00 - x10) * u + (3 * b + 2 * dx + x10 - x11)) * u - b - dy - dx) * u + y00)) * v +
		(((x11 - 2 * (p11 - p01 + c) + x10 + x00 + x01) * u +
		  (3 * c - 2 * x00 - x10)) * u +
		 x00) * u + p00;
	}

	public class Particle {
		public Material mat;
		public float x, y, u, v, gu, gv, T00, T01, T11;
		public int cx, cy, gi;
		public float[] px;
		public float[] py;
		public float[] gx;
		public float[] gy;

		public Particle(Material mat) {
			this.mat = mat;
			this.x = 0;
			this.y = 0;
			this.u = 0;
			this.v = 0;
			this.gu = 0;
			this.gv = 0;
			this.T00 = 0;
			this.T01 = 0;
			this.T11 = 0;
			this.cx = 0;
			this.cy = 0;
			this.gi = 0;

			this.px = new float[] { 0, 0, 0 };
			this.py = new float[] { 0, 0, 0 };
			this.gx = new float[] { 0, 0, 0 };
			this.gy = new float[] { 0, 0, 0 };
		}

		public Particle(Material mat, int x, int y) {
			this.mat = mat;
			this.x = x;
			this.y = y;
			this.u = 0;
			this.v = 0;
			this.gu = 0;
			this.gv = 0;
			this.T00 = 0;
			this.T01 = 0;
			this.T11 = 0;
			this.cx = 0;
			this.cy = 0;
			this.gi = 0;

			this.px = new float[] { 0, 0, 0 };
			this.py = new float[] { 0, 0, 0 };
			this.gx = new float[] { 0, 0, 0 };
			this.gy = new float[] { 0, 0, 0 };
		}

		public Particle(Material mat, int x, int y, float u, float v) {
			this.mat = mat;
			this.x = x;
			this.y = y;
			this.u = u;
			this.v = v;
			this.gu = 0;
			this.gv = 0;
			this.T00 = 0;
			this.T01 = 0;
			this.T11 = 0;
			this.cx = 0;
			this.cy = 0;
			this.gi = 0;

			this.px = new float[] { 0, 0, 0 };
			this.py = new float[] { 0, 0, 0 };
			this.gx = new float[] { 0, 0, 0 };
			this.gy = new float[] { 0, 0, 0 };
		}
		
		public void initializeWeights(int gSizeY) {
			cx = (int)(x - .5f);
			cy = (int)(y - .5f);
			gi = cx * gSizeY + cy;
			
			float cx_x = cx - x;
			float cy_y = cy - y;
			
			// Quadratic interpolation kernel weights - Not meant to be changed
			px[0] = .5f * cx_x * cx_x + 1.5f * cx_x + 1.125f;
			gx[0] = cx_x + 1.5f;
			cx_x++;
			px[1] = -cx_x * cx_x + .75f;
			gx[1] = -2 * cx_x;
			cx_x++;
			px[2] = .5f * cx_x * cx_x - 1.5f * cx_x + 1.125f;
			gx[2] = cx_x - 1.5f;
			
			py[0] = .5f * cy_y * cy_y + 1.5f * cy_y + 1.125f;
			gy[0] = cy_y + 1.5f;
			cy_y++;
			py[1] = -cy_y * cy_y + .75f;
			gy[1] = -2 * cy_y;
			cy_y++;
			py[2] = .5f * cy_y * cy_y - 1.5f * cy_y + 1.125f;
			gy[2] = cy_y - 1.5f;
		}
	}

	public class Material {
		
		public float mass;
		public float restDensity;
		public float stiffness;
		public float bulkViscosity;
		public float surfaceTension;
		public float kElastic;
		public float maxDeformation;
		public float meltRate;
		public float viscosity;
		public float damping;
		public float friction;
		public float stickiness;
		public float smoothing;
		public float gravity;
		public int materialIndex;
		
		public Material() {
			this.mass = 1;
			this.restDensity = 2;
			this.stiffness = 1;
			this.bulkViscosity = 1;
			this.surfaceTension = 0;
			this.kElastic = 0;
			this.maxDeformation = 0;
			this.meltRate = 0;
			this.viscosity = 0.02f;
			this.damping = 0.001f;
			this.friction = 0;
			this.stickiness = 0;
			this.smoothing = 0.02f;
			this.gravity = 0.09f;
		}

		public Material(float mass, float restDensity, float stiffness, float bulkViscosity, float surfaceTension, 
				float kElastic, float maxDeformation, float meltRate, float viscosity, float damping, float friction, 
				float stickiness, float smoothing, float gravity) {
			this.mass = mass;
			this.restDensity = restDensity;
			this.stiffness = stiffness;
			this.bulkViscosity = bulkViscosity;
			this.surfaceTension = surfaceTension;
			this.kElastic = kElastic;
			this.maxDeformation = maxDeformation;
			this.meltRate = meltRate;
			this.viscosity = viscosity;
			this.damping = damping;
			this.friction = friction;
			this.stickiness = stickiness;
			this.smoothing = smoothing;
			this.gravity = gravity;
		}

	}

	public class Node {
		
		public float mass, particleDensity, gx, gy, u, v, u2, v2, ax, ay;
		public float[] cgx;
		public float[] cgy;
		public boolean active;

		public Node() {
			mass = 0;
			particleDensity = 0;
			gx = 0;
			gy = 0;
			u = 0;
			v = 0;
			u2 = 0;
			v2 = 0;
			ax = 0;
			ay = 0;
			active = false;
			cgx = new float[numMaterials];
			cgy = new float[numMaterials];
		}

		public void clear() {
			mass = 0;
			particleDensity = 0;
			gx = 0;
			gy = 0;
			u = 0;
			v = 0;
			u2 = 0;
			v2 = 0;
			ax = 0;
			ay = 0;
			active = false;
			for (i=0; i<numMaterials; i++) {
				cgx[i] = 0;
				cgy[i] = 0;
			}
		}
	}
}