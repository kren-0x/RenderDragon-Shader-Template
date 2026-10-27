$input a_position, a_normal, a_texcoord0, a_color0
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 FogControl;
uniform vec4 OverlayColor;
uniform vec4 FogColor;
uniform vec4 TileLightColor;

void main() {
    mat4 World = u_model[0];

    float lightIntensity = calculateLightIntensity(World, vec4(a_normal.xyz, 0.0), TileLightColor);
    lightIntensity += OverlayColor.a * 0.35;
    vec4 light = vec4(lightIntensity * TileLightColor.rgb, 1.0);

    vec3 worldPosition;
#ifdef INSTANCING
    mat4 model = mtxFromCols(i_data1, i_data2, i_data3, vec4(0.0, 0.0, 0.0, 1.0));
    worldPosition = instMul(model, vec4(a_position, 1.0)).xyz;
#else
    worldPosition = mul(World, vec4(a_position, 1.0)).xyz;
#endif

    vec4 position = jitterVertexPosition(worldPosition);
    float cameraDepth = position.z;
    float fogIntensity = calculateFogIntensity(cameraDepth, FogControl.z, FogControl.x, FogControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

    uvec2 uv0_16 = uvec2(round(a_texcoord0 * 65535.0));
    vec2 uv0 = vec2(float((uv0_16.x & 32767u) << 1u), float((uv0_16.y & 32767u) << 1u)) * 1.525902189314365386962890625e-05;
    uv0.x += 3.0517578125e-05 * (2.0 * float((uv0_16.x & 32768u) >> 15u) - 1.0);
    uv0.y += 3.0517578125e-05 * (2.0 * float((uv0_16.y & 32768u) >> 15u) - 1.0);

#if defined(DEPTH_ONLY)
    v_color0 = vec4_splat(0.0);
    v_texcoord0 = vec2_splat(0.0);
#else
    v_color0 = a_color0;
    v_texcoord0 = uv0;
#endif

    v_clipPosition = position;
    v_fog = fog;
    v_light = light;
    v_worldPos = worldPosition;
    gl_Position = position;
}
