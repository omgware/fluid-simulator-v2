package com.fluidsimulator.gameobjects.sph;

import java.util.ArrayList;
import java.util.Iterator;

abstract public class SpatialTable<V> implements Iterable<V> {

	/** default nearby table sizes
	 * 
	 * These two variables can be tweaked to affect the accuracy 
	 * and performance of PBF and SPH neighbors search
	 */
	private static final int DEFAULT_NEARBY_SIZE = 50;
	// the actual spatial table
	private final ArrayList<V> table;
	// a void list initialized here for reuse
	private ArrayList<V> voidList = new ArrayList<V>(1);
	// the nearby elements table
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
	private int xPrev;
	private int yPrev;
	// abstract position variables must be implemented on actual class instantiation
	abstract protected int posX(V value);
	abstract protected int posY(V value);
	abstract protected int prevPosX(V value);
	abstract protected int prevPosY(V value);
	
	@SuppressWarnings("unchecked")
	public SpatialTable(int column, int row) {
		this.row = row; 
		this.column = column;
		table = new ArrayList<V>((row*column)/2);
		nearby = new ArrayList[column][row];
	}

	/**
	 * Initialize the nearby table to the default size
	 */
	public void initialize() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j] = new ArrayList<V>(DEFAULT_NEARBY_SIZE);
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
	
	public int size() {
		return table.size();
	}

	/**
	 * Returns an array of neighbors for the provided central object
	 */
	public ArrayList<V> nearby(V value) {
		x = posX(value);
		y = posY(value);
		if (!isInRange(x, y)) 
			return voidList; 
		return nearby[x][y];
	}
	
	/**
	 * Update position for an item
	 */
	public void updatePosition(V value) {
		x = posX(value);
		y = posY(value);
		xPrev = prevPosX(value);
		yPrev = prevPosY(value);
		if (isInRange(xPrev, yPrev))
			nearby[xPrev][yPrev].remove(value);
		if (isInRange(x, y))
			nearby[x][y].add(value);
	}
	
	public int sizeNearby(V value) {
		return nearby(value).size();
	}

	/**
	 * Updates the spatial table based on new values position
	 */
	public void rehash() {
		for (i=0; i<column; i++) {
			for (j=0; j<row; j++) {
				if (nearby[i][j] != null)
					nearby[i][j].clear();
			}
		}
		tempSize = table.size();
		for (z=0; z<tempSize; z++) {
			addInCell(table.get(z));
		}
	}

	/**
	 * Add element to its position and neighbor cells.
	 */
	private void addInCell(V value) {
		for (i = -1; i < 2; ++i) {
			for (j = -1; j < 2; ++j) {
				x = posX(value)+i;
				y = posY(value)+j;
				if (isInRange(x, y))
					nearby[x][y].add(value);
			}
		}
	}
	
	private boolean isInRange(float x, float y) {
		return (x > 0 && x < column && y > 0 && y < row);
	}

}
