package com.fluidsimulator.utils;


public class Vector2 extends com.badlogic.gdx.math.Vector2 {

	private static final long serialVersionUID = -1049661215629475906L;

	public Vector2 () {
	}

	public Vector2 (float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2 (Vector2 v) {
		set(v);
	}
	
	public float Length() {
		float length = x * x + y * y;
		return (float) Math.sqrt(length);
	}
	
	public float LengthSquare(){
		return  x * x + y * y;
	}
	
	public static Vector2 Normalize(Vector2 vec){
		float length = vec.Length();
		if(length != 0){
			return new Vector2(vec.x / length, vec.y / length);
		}else{
			return vec;
		}
	}
	
	public static Vector2 Multiply(Vector2 vec, float coe) {
		return new Vector2(vec.x * coe, vec.y * coe);
	}

	public static Vector2 Add(Vector2 a, Vector2 b) {
		return new Vector2(a.x + b.x, a.y + b.y);
	}

	public static Vector2 Substract(Vector2 a, Vector2 b) {
		return new Vector2(a.x - b.x, a.y - b.y);
	}
	
	public static float DotProduct(Vector2 a, Vector2 b) {
		float product = a.x * b.x + a.y * b.y;
		return product;
	}
	
	public static Vector2 Scale(Vector2 vec, float scale){
		return new Vector2(vec.x * scale, vec.y * scale);
	}
	
	public void Add(Vector2 vec){
		this.x += vec.x;
		this.y += vec.y;
	}
	
	public void Substract(Vector2 vec){
		this.x -= vec.x;
		this.y -= vec.y;
	}
	
	public void Scale(float scale){
		this.x *= scale;
		this.y *= scale;
	}
}
