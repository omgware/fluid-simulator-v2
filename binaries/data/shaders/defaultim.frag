#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_col;
varying vec3 Position;
varying vec2 v_tex0;

uniform sampler2D u_sampler0;

uniform sampler2D Texture;
uniform sampler2D glossMap;
uniform sampler2D displacementMap;
uniform sampler2D displacementMap2;

uniform float ScreenResX, ScreenResY;
uniform float deltaTime;
uniform float time;
uniform float rippleIntensity;
//uniform float Alpha;

//
// fresnel approximation
// F(a) = F(0) + (1- cos(a))^5 * (1- F(0))
//
// Calculate fresnel term. You can approximate it 
// with 1.0-dot(normal, viewpos).	
//
float fast_fresnel(vec3 I, vec3 N, vec3 fresnelValues) {
    float bias = fresnelValues.x;
    float power = fresnelValues.y;
    float scale = 1.0 - bias;
    return bias + pow(1.0 - dot(I, N), power) * scale;
}

float very_fast_fresnel(vec3 I, vec3 N) { 
	return 1.0 - dot(N, I); 
}

void main() {
	const float Alpha = 0.01;
	const vec3 Normal = vec3(0, 0, 1);
	const vec3 fresnelValues = vec3(0.15, 2.0, 0);
	const vec3 IoR_Values = vec3(1.14, 1.12, 1.10);
	
	// Screen to texture coordinate
	
	vec2 PixelTexCoords = vec2(gl_FragCoord.x / ScreenResX, 1.0 - gl_FragCoord.y / ScreenResY);
	vec2 PixelTexCoordsDeltaPlus = vec2(fract(PixelTexCoords.x - time*0.01), PixelTexCoords.y);
	vec2 PixelTexCoordsDeltaMinus = vec2(fract(PixelTexCoords.x + time*0.03), PixelTexCoords.y);
	
	// ripple deformation texture coordinates
	
	vec2 cPos = -1.0 + 2.0 * PixelTexCoords;
	float cLength = length(cPos);
	vec2 uv = (cPos/cLength) * cos(cLength * 12.0 - time * 4.0) * rippleIntensity;
	//vec3 ripple_color = texture2D(Texture, uv).rgb;
	
	// Base Color
	
	vec3 base_color = v_col.rgb;
	
	// Reflection
	
	vec3 reflVec = normalize(reflect(Position, Normal));
	//vec3 reflectColor = texture2D(Texture, PixelTexCoords + reflVec.xy*0.1).rgb;
	vec3 reflectColor = texture2D(Texture, PixelTexCoords + uv).rgb;
 
 	// Refraction
 	
	vec3 Refract = normalize(refract(Position, Normal, 1.20));
	/*Refract.x = normalize(refract(Position, Normal, IoR_Values.x)).x;
	Refract.y = normalize(refract(Position, Normal, IoR_Values.y)).y;
	Refract.z = normalize(refract(Position, Normal, IoR_Values.z)).z;*/
	vec3 refractColor;
	//refractColor = mix(texture2D(Texture, PixelTexCoords + Refract.xy*0.05), v_col, Alpha).rgb * v_col.rgb;
	refractColor = mix(texture2D(Texture, PixelTexCoords + uv), v_col, Alpha).rgb * v_col.rgb;
	
	// Do a gloss map look up and compute the reflectivity.

	vec3 gloss_color = texture2D(glossMap, PixelTexCoords + v_tex0 * 0.06).rgb;
	float reflectivity = (gloss_color.r + gloss_color.g + gloss_color.b)/3.0;
	reflectivity += 0.2;
	reflectivity = 0.9;
	
	// Generic Noise displacements
	vec3 displacement_color = texture2D(displacementMap, PixelTexCoordsDeltaPlus + v_tex0 * 0.02).rgb;
	float displacement = (displacement_color.r + displacement_color.g + displacement_color.b)/3.0;
	vec3 displacement_color2 = texture2D(displacementMap2, PixelTexCoords + v_tex0 * 0.03).rgb;
	float displacement2 = (displacement_color2.r + displacement_color2.g + displacement_color2.b)/3.0;
	displacement += 0.2;
	displacement2 += 0.2;
	//displacement2 = displacement2 > 1.0 ? 1.0 : displacement2;
	    
	// Find the Fresnel term

	//float fresnelTerm = fast_fresnel(-Position, Normal, fresnelValues);
	float fresnelTerm = very_fast_fresnel(-Position, Normal);
	
	// Apply Fresnel

	vec3 color = mix(refractColor, reflectColor, fresnelTerm).rgb;
	
	// Apply Displacements
	
	color = mix(base_color, color, displacement).rgb;
	color = mix(base_color, color, displacement2).rgb;
	
	// Apply Gloss Map reflectivity
	
	vec3 final_color = mix(base_color, color, reflectivity);
	
	// Final Color
	
	//final_color = mix(final_color, v_col, Alpha);
	gl_FragColor = vec4(final_color, v_col.a * texture2D(u_sampler0, v_tex0).a);
  
}