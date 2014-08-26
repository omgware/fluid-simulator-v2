package com.fluidsimulator.gameobjects;

public class ObjectInfo {
	// millis
	public long timeCheck;
	public float lastDistanceCheck;
	public boolean isPortalAllowed = false;
	public boolean isSphere = false;
	public Piece pieceInfo;
	
	public ObjectInfo() {
		this(null);
	}
	
	public ObjectInfo(Piece pieceInfo) {
		this.timeCheck = 0;
		this.lastDistanceCheck = 0;
		this.pieceInfo = pieceInfo;
	}
	
	public void updateTime() {
		this.timeCheck = System.currentTimeMillis();
	}
	
	public boolean hasTimePassed(long delay) {
		return (System.currentTimeMillis() - timeCheck) >= delay;
	}
	
	public void updateDistanceCheck(float newDist) {
		this.lastDistanceCheck = newDist;
	}
	
	public boolean hasDistancePassed(float newDistance, float distanceGap) {
		return (newDistance - lastDistanceCheck) >= distanceGap;
	}
}
