$input a_position, a_color0, a_texcoord0
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_color0, v_fog, v_occlusionHeight, v_occlusionUV, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 Dimensions;
uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 PositionBaseOffset;
uniform vec4 PositionForwardOffset;
uniform vec4 UVOffsetAndScale;
uniform vec4 Velocity;
uniform vec4 ViewPosition;

#if BGFX_SHADER_LANGUAGE_GLSL
    #if BGFX_SHADER_LANGUAGE_GLSL < 130
    float trunc(float _x) { return float(int(_x)); }
    #endif
float fmod(float _a, float _b) { return _a - _b * trunc(_a / _b); }
vec3 fmod(vec3 _a, vec3 _b) { return vec3(fmod(_a.x, _b.x), fmod(_a.y, _b.y), fmod(_a.z, _b.z)); }
#endif

void main() {
    float spriteSelector = a_color0.x * 255.0;

    vec2 texcoord0 = UVOffsetAndScale.xy + (a_texcoord0 * UVOffsetAndScale.zw);
#if !defined(NO_VARIETY)
    texcoord0.x += spriteSelector * UVOffsetAndScale.z;
#endif

    vec3 particleBox = vec3(30.0, 30.0, 30.0);

    vec3 worldSpacePosition = fmod(a_position + PositionBaseOffset.xyz, particleBox);
    worldSpacePosition -= particleBox * 0.5;
    worldSpacePosition += PositionForwardOffset.xyz;

    vec3 worldSpacePositionBottom = worldSpacePosition;
    vec3 worldSpacePositionTop = worldSpacePositionBottom + (Velocity.xyz * Dimensions.y);

    vec4 screenSpacePositionBottom = jitterVertexPosition(worldSpacePositionBottom);
    vec4 screenSpacePositionTop = jitterVertexPosition(worldSpacePositionTop);

    vec2 screenSpaceUpDirection = (screenSpacePositionTop.xy / screenSpacePositionTop.w) - (screenSpacePositionBottom.xy / screenSpacePositionBottom.w);
    vec2 screenSpaceRightDirection = normalize(vec2(-screenSpaceUpDirection.y, screenSpaceUpDirection.x));

    vec4 position = mix(screenSpacePositionTop, screenSpacePositionBottom, a_texcoord0.y);
    position.xy += (0.5 - a_texcoord0.x) * screenSpaceRightDirection * Dimensions.x;

    vec2 occlusionUV = ((worldSpacePosition.xz + ViewPosition.xz) * (1.0 / 64.0)) + vec2_splat(0.5);
    float occlusionHeight = (worldSpacePosition.y + ViewPosition.y - 0.5) * (1.0 / 255.0);

    float fogIntensity = calculateFogIntensity(position.z, FogAndDistanceControl.z, FogAndDistanceControl.x, FogAndDistanceControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

    v_color0 = a_color0;
    v_fog = fog;
    v_occlusionHeight = occlusionHeight;
    v_occlusionUV = occlusionUV;
    v_texcoord0 = texcoord0;
    v_worldPos = worldSpacePosition;
    gl_Position = position;
}
