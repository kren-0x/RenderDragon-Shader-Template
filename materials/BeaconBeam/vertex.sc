$input a_color0, a_normal, a_position, a_texcoord0
$output v_color, v_fog, v_light, v_texCoords

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 UVAnimation;

void main() {
    vec4 position = jitterVertexPosition(a_position);
    float cameraDepth = position.z;
    float fogIntensity = calculateFogIntensity(cameraDepth, FogAndDistanceControl.z, FogAndDistanceControl.x, FogAndDistanceControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

    float lightIntensity = calculateLightIntensity(u_model[0], vec4(a_normal.xyz, 0.0), TileLightColor);
    lightIntensity += OverlayColor.a * 0.35;
    vec4 light = vec4(lightIntensity * TileLightColor.rgb, 1.0);

    v_color = a_color0;
    v_fog = fog;
    v_light = light;
    v_texCoords = applyUvAnimation(a_texcoord0, UVAnimation);
    gl_Position = position;
}
