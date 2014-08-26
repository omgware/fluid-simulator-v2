package com.fluidsimulator.utils;


public class Vector3 extends com.badlogic.gdx.math.Vector3 {

	private static final long serialVersionUID = 1788027793834557077L;

	public Vector3 () {
	}

	public Vector3 (float x, float y, float z) {
		this.set(x, y, z);
	}

	public Vector3 (final Vector3 vector) {
		this.set(vector);
	}
	
	public float Length() {
		float length = x * x + y * y + z * z;
		return (float) Math.sqrt(length);
	}
	
	public float LengthSquare(){
		return  x * x + y * y + z * z;
	}
	
	public static Vector3 Normalize(Vector3 vec){
		float length = vec.Length();
		if(length != 0){
			return new Vector3(vec.x / length, vec.y / length, vec.z / length);
		}else{
			return vec;
		}
	}
	
	public static Vector3 Multiply(Vector3 vec, float coe) {
		return new Vector3(vec.x * coe, vec.y * coe, vec.z * coe);
	}

	public static Vector3 Add(Vector3 a, Vector3 b) {
		return new Vector3(a.x + b.x, a.y + b.y, a.z + b.z);
	}

	public static Vector3 Substract(Vector3 a, Vector3 b) {
		return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z);
	}
	
	public static float DotProduct(Vector3 a, Vector3 b) {
		float product = a.x * b.x + a.y * b.y + a.z * b.z;
		return product;
	}
	
	public static Vector3 Scale(Vector3 vec, float scale){
		return new Vector3(vec.x * scale, vec.y * scale, vec.z * scale);
	}
	
	public void Add(Vector3 vec){
		this.x += vec.x;
		this.y += vec.y;
		this.z += vec.z;
	}
	
	public void Substract(Vector3 vec){
		this.x -= vec.x;
		this.y -= vec.y;
		this.z -= vec.z;
	}
	
	public void Scale(float scale){
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
	}
}
