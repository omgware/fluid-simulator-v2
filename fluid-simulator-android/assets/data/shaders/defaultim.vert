attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projModelView;

varying vec4 v_col;
varying vec3 Position;
varying vec2 v_tex0;

void main() {

	Position = vec3(u_projModelView * a_position);
	
	gl_Position = u_projModelView * a_position;
	v_col = a_color;
	v_tex0 = a_texCoord0;
	gl_PointSize = 1.0;
}