package com.fluidsimulator.gameobjects;

import java.util.ArrayList;
import java.util.Iterator;

abstract public class SpatialTable implements Iterable<Particle> {

	/** default nearby table sizes
	 * 
	 * These two variables can be tweaked to affect the accuracy 
	 * and performance of PBF and SPH neighbors search
	 */
	public int MAX_NEIGHBORS = 50;
	public int MAX_NEIGHBORS2 = 50;
	// the actual spatial table
	public final ArrayList<Particle> table;
	// a void list initialized here for reuse
	private Particle[] voidList = new Particle[0];
	// the nearby elements table
//	private FastMap<Integer, ArrayList<Particle>> nearby;
	private Particle[][][] nearby;
	private byte[][] nearbySizes;
	private byte lastNearbyLength;
	// row and column of the spatial table
	private int row;
	private int column;
	// temporary variables for faster iterations and optimized object allocations
	private int i;
	private int j;
	private int tempSize;
	private int z;
	private int x;
	private int y;
	private int x2;
	private int y2;
	private int xPrev;
	private int yPrev;
	private int xPrev2;
	private int yPrev2;
	// abstract position variables must be implemented on actual class instantiation
	abstract protected int posX(Particle value);
	abstract protected int posY(Particle value);
	abstract protected int prevPosX(Particle value);
	abstract protected int prevPosY(Particle value);
	abstract protected void setPrevGridPos(Particle value, int x, int y);
	abstract protected int getPrevGridPosX(Particle value);
	abstract protected int getPrevGridPosY(Particle value);
	abstract protected void savePosX(Particle value, int x);
	abstract protected void savePosY(Particle value, int y);
	abstract protected int getPosX(Particle value);
	abstract protected int getPosY(Particle value);
	abstract protected int getHash(Particle value);
	
	@SuppressWarnings("unchecked")
	public SpatialTable(int column, int row, int size) {
		this.row = row; 
		this.column = column;
		table = new ArrayList<Particle>(size);
		nearby = new Particle[column][row][MAX_NEIGHBORS2];
		nearbySizes = new byte[column][row];
		lastNearbyLength = 0;
	}

	/**
	 * Initialize the nearby table to the default size
	 */
	public void initialize() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j] = new Particle[MAX_NEIGHBORS2];
				nearbySizes[i][j] = 0;
			}
		}
		lastNearbyLength = 0;
	}
	
	public boolean add(Particle value) {
		addInCell(value);
		table.add(value);
		return true;
	}
	
	public Iterator<Particle> iterator() {
		return table.iterator();
	}
	
	public Particle get(int i) {
		return table.get(i);
	}
	
	public boolean remove(Particle value) {
		table.remove(value);
		return true;
	}
	
	public void clear() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j] = null;
				nearbySizes[i][j] = 0;
			}
		}
		lastNearbyLength = 0;
		table.clear();
	}

	/**
	 * Clear only neighbors map
	 */
	public void clearNeighbors() {
		for (i=0; i<column; i++) {
			for (j=0; j<row; j++) {
				nearbySizes[i][j] = 0;
			}
		}
		lastNearbyLength = 0;
	}
	
	public int size() {
		return table.size();
	}

	/**
	 * Returns an array of neighbors for the provided central object
	 */
	public Particle[] nearby(Particle value) {
		x = getPosX(value);
		y = getPosY(value);
		lastNearbyLength = 0;
		if (!isInRange(x, y)) 
			return null;
		lastNearbyLength = nearbySizes[x][y];
		return nearby[x][y];
	}
	
	public byte lastSizeNearby() {
		return lastNearbyLength;
	}

	/**
	 * Updates the spatial table based on new values position
	 */
	public void rehash() {
		clearNeighbors();
		tempSize = table.size();
//		System.out.println(" ");
//		System.out.println(" ");
		for (z=0; z<tempSize; z++) {
//			System.out.print(z + " ");
			addInCell(table.get(z));
		}
	}

	/**
	 * Add element to its position and neighbor cells.
	 */
	private void addInCell(Particle value) {
		x = posX(value);
		y = posY(value);
		savePosX(value, x);
		savePosY(value, y);
//		System.out.println(x + " " + y);
		for (i = -1; i < 2; ++i) {
			for (j = -1; j < 2; ++j) {
				x2 = x+i;
				y2 = y+j;
//				x2 = x;
//				y2 = y;
				if (isInRange(x2, y2) && nearbySizes[x2][y2] < MAX_NEIGHBORS2) {
					if (nearbySizes[x2][y2] < 0)
						nearbySizes[x2][y2] = 0;
					nearby[x2][y2][nearbySizes[x2][y2]++] = value;
//					if (nearby[x2][y2].size() > 50)
//						System.out.println(nearby[x2][y2].size());
				}
			}
		}
	}
	
	private boolean isInRange(float x, float y) {
		return (x >= 0 && x < column && y >= 0 && y < row);
	}

}
