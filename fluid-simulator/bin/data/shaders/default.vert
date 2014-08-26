attribute vec4 a_pos;
attribute vec4 a_color;

varying vec4 v_color;

void main() {
	v_color = a_color;
	gl_Position = a_pos;
}