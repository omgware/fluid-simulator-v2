varying vec3 Position;
varying vec3 Normal;

//uniform mat4 u_projModelView;
 
void main()
{
	gl_TexCoord[0] = gl_MultiTexCoord0;
	gl_Position = ftransform();
	gl_FrontColor = gl_Color;
 
	Position = vec3(gl_ModelViewProjectionMatrix * gl_Vertex);
	Normal = normalize(gl_NormalMatrix * gl_Normal);
}




/*uniform vec3 LightPos;
varying vec3 N;
varying vec3 P;
varying vec3 V;
varying vec3 L;

void main()
{
	N = normalize(gl_NormalMatrix*gl_Normal);
	P = gl_Vertex.xyz;
	V = -vec3(gl_ModelViewMatrix*gl_Vertex);
	L = vec3(gl_ModelViewMatrix*(vec4(LightPos,1)-gl_Vertex));
	gl_Position = ftransform();
}*/