varying vec3 Position;
varying vec3 Normal;
 
/*uniform sampler2D Texture;
uniform float ScreenResX, ScreenResY;
uniform float Alpha;*/
 
void main()
{
	/*vec2 PixelTexCoords = vec2(gl_FragCoord.x / ScreenResX, gl_FragCoord.y / ScreenResY);
 
	vec3 Refract = normalize(refract(Position, Normal, 1.20));
 
	gl_FragColor.rgb = mix(texture2D(Texture, PixelTexCoords + Refract.xy*0.1), gl_Color, Alpha).rgb;
	gl_FragColor.a = gl_Color.a;*/
	gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}




/*uniform sampler2D Texture;
uniform float RefractionIndex;
uniform vec3 SpecularColour;
uniform float Roughness;
uniform float SpecularIntensity;

varying vec3 V;
varying vec3 N;
varying vec3 L;

void main()
{
	vec3 v = normalize(V);
	vec3 i = -v;
	vec3 n = normalize(N);
	vec3 l = normalize(L);
	vec3 h = normalize(l+v);
	
	vec3 Refracted = refract(i,n,RefractionIndex);
	Refracted = vec3(gl_TextureMatrix[0] * vec4(Refracted,1.0));
	vec3 Reflected = reflect(i,n);
	Reflected = vec3(gl_TextureMatrix[0] * vec4(Reflected,1.0));
	
	float specular = pow(max(0.0,dot(n,h)),1/Roughness);
	
	vec3 refractColor = SpecularColour*specular*SpecularIntensity +
	mix(vec3(texture2D(Texture,Reflected)),
	vec3(texture2D(Texture,Refracted)),
	dot(n,v));
	
	gl_FragColor = vec4(refractColor,1.0);
}*/