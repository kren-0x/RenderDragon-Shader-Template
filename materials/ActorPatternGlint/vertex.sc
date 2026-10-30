$input a_position, a_color0, a_texcoord0, a_indices, a_normal
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_clipPosition, v_color0, v_fog, v_layerUv, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>
#include <MinecraftRenderer.Materials/GlintUtil.dragonh>

uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 UVAnimation;
uniform vec4 UVScale;
uniform mat4 Bones[8];

void main() {
    mat4 World = u_model[0];

    World = mul(World, Bones[int(a_indices)]);

    vec2 texcoord0 = a_texcoord0;
    texcoord0 = applyUvAnimation(texcoord0, UVAnimation);

    vec2 layer1UV = calculateLayerUV(a_texcoord0, UVAnimation.x, UVAnimation.z, UVScale.xy);
    vec2 layer2UV = calculateLayerUV(a_texcoord0, UVAnimation.y, UVAnimation.w, UVScale.xy);
    vec4 layerUV = vec4(layer1UV, layer2UV);

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

#if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY_ALPHA_TEST)
    v_texcoord0 = vec2_splat(0.0);
    v_color0 = vec4_splat(0.0);
#else
    v_texcoord0 = texcoord0;
    v_color0 = a_color0;
#endif

    v_clipPosition = position;
    v_layerUv = layerUV;
    v_fog = fog;
    v_light = light;
    v_worldPos = worldPosition;
    gl_Position = position;
}
