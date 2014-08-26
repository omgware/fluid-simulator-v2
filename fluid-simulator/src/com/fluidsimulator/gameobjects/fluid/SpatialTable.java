package com.fluidsimulator.gameobjects.fluid;

import java.util.ArrayList;
import java.util.Iterator;

import com.fluidsimulator.FluidSimulatorGeneric;

abstract public class SpatialTable<V> implements Iterable<V> {

	/** default nearby table sizes
	 * 
	 * These two variables can be tweaked to affect the accuracy 
	 * and performance of PBF and SPH neighbors search
	 */
	public int MAX_NEIGHBORS = FluidSimulatorGeneric.IS_DESKTOP ? 300 : 300;
	public int MAX_NEIGHBORS2 = FluidSimulatorGeneric.IS_DESKTOP ? 300 : 300;
	// the actual spatial table
	public final ArrayList<V> table;
	// a void list initialized here for reuse
	private ArrayList<V> voidList = new ArrayList<V>(1);
	// the nearby elements table
//	private FastMap<Integer, ArrayList<V>> nearby;
	private ArrayList<V>[][] nearby;
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
	abstract protected int posX(V value);
	abstract protected int posY(V value);
	abstract protected int prevPosX(V value);
	abstract protected int prevPosY(V value);
	abstract protected void setPrevGridPos(V value, int x, int y);
	abstract protected int getPrevGridPosX(V value);
	abstract protected int getPrevGridPosY(V value);
	abstract protected void savePosX(V value, int x);
	abstract protected void savePosY(V value, int y);
	abstract protected int getPosX(V value);
	abstract protected int getPosY(V value);
	abstract protected int getHash(V value);
	
	@SuppressWarnings("unchecked")
	public SpatialTable(int column, int row, int size) {
		this.row = row; 
		this.column = column;
		table = new ArrayList<V>(size);
		nearby = new ArrayList[column][row];
	}

	/**
	 * Initialize the nearby table to the default size
	 */
	public void initialize() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j] = new ArrayList<V>(MAX_NEIGHBORS2);
			}
		}
	}
	
	public boolean add(V value) {
		addInCell(value);
		table.add(value);
		return true;
	}
	
	public Iterator<V> iterator() {
		return table.iterator();
	}
	
	public V get(int i) {
		return table.get(i);
	}
	
	public boolean remove(V value) {
		table.remove(value);
		return true;
	}
	
	public void clear() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j].clear();
				nearby[i][j] = null;
			}
		}
		table.clear();
	}

	/**
	 * Clear only neighbors map
	 */
	public void clearNeighbors() {
		for (i=0; i<column; i++) {
			for (j=0; j<row; j++) {
				if (nearby[i][j] != null) {
					nearby[i][j].clear();
				}
			}
		}
	}
	
	public int size() {
		return table.size();
	}

	/**
	 * Returns an array of neighbors for the provided central object
	 */
	public ArrayList<V> nearby(V value) {
		x = getPosX(value);
		y = getPosY(value);
		if (!isInRange(x, y)) 
			return voidList; 
		return nearby[x][y];
	}
	
	/**
	 * Update position for an item
	 */
	public void updatePosition(V value) {
		x = getPosX(value);
		y = getPosY(value);
		xPrev = getPrevGridPosX(value);
		yPrev = getPrevGridPosY(value);
		setPrevGridPos(value, x, y);
		for (i = -1; i < 2; ++i) {
			for (j = -1; j < 2; ++j) {
				xPrev2 = xPrev+i;
				yPrev2 = yPrev+j;
				x2 = x+i;
				y2 = y+j;
				if (isInRange(xPrev2, yPrev2))
					nearby[xPrev2][yPrev2].remove(value);
				if (isInRange(x2, y2) && nearby[x2][y2].size() < MAX_NEIGHBORS2)
					nearby[x2][y2].add(value);
			}
		}
	}
	
	public int sizeNearby(V value) {
		return nearby(value).size();
	}

	/**
	 * Updates the spatial table based on new values position
	 */
	public void rehash() {
		clearNeighbors();
		tempSize = table.size();
		for (z=0; z<tempSize; z++) {
			addInCell(table.get(z));
		}
	}

	/**
	 * Add element to its position and neighbor cells.
	 */
	private void addInCell(V value) {
		x = posX(value);
		y = posY(value);
		savePosX(value, x);
		savePosY(value, y);
		for (i = -1; i < 2; ++i) {
			for (j = -1; j < 2; ++j) {
				x2 = x+i;
				y2 = y+j;
//				x2 = x;
//				y2 = y;
				if (isInRange(x2, y2)/* && nearby[x2][y2].size() < MAX_NEIGHBORS2*/) {
					nearby[x2][y2].add(value);
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
