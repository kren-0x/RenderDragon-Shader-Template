$input a_color0, a_normal, a_position, a_texcoord0
$output v_clipPosition, v_color, v_fog, v_light, v_texCoords, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 UVAnimation;
uniform vec4 ZShiftValue;

void main() {
    vec3 worldPosition = mul(u_model[0], vec4(a_position, 1.0)).xyz;

#if defined(RASTERIZED_ALPHA_TEST) || defined(RASTERIZED_OPAQUE) || defined(RASTERIZED_TRANSPARENT)
    vec4 position = mul(u_viewProj, vec4(worldPosition, 1.0));
#else
    vec4 position = jitterVertexPosition(worldPosition);
#endif

    position.z += ZShiftValue.x;

#if defined(LIT)
    float lightIntensity = calculateLightIntensity(u_model[0], vec4(a_normal.xyz, 0.0), TileLightColor);
    lightIntensity += OverlayColor.a * 0.35;
    vec4 light = vec4(lightIntensity * TileLightColor.rgb, 1.0);
#else
    vec4 light = vec4_splat(0.0);
#endif

    float cameraDepth = position.z;
    float fogIntensity = calculateFogIntensity(cameraDepth, FogControl.z, FogControl.x, FogControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

    v_clipPosition = position;
    v_color = a_color0;
    v_fog = fog;
    v_light = light;
    v_texCoords = applyUvAnimation(a_texcoord0, UVAnimation);
    v_worldPos = worldPosition;
    gl_Position = position;
}
