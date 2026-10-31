$input a_color0, a_normal, a_position, a_texcoord0
$output v_clipPosition, v_color, v_fog, v_light, v_texCoords, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>

uniform vec4 FogControl;
uniform vec4 FogColor;
uniform vec4 SubPixelOffset;
uniform vec4 ZShiftValue;
uniform vec4 TileLightColor;
uniform vec4 OverlayColor;
uniform vec4 UVAnimation;

void main() {
    vec4 worldPos = mul(u_model[0], vec4(a_position, 1.0));
    mat4 proj = u_proj;
    proj[2].x += SubPixelOffset.x;
    proj[2].y -= SubPixelOffset.y;
    vec4 clipPos = mul(proj, mul(u_view, vec4(worldPos.xyz, 1.0)));
    vec4 pos = clipPos;
    uvec2 unormUV = uvec2(round(a_texcoord0 * 65535.0));
    vec2 uv = vec2(float((unormUV.x & 32767u) << uint(1)), float((unormUV.y & 32767u) << uint(1))) * 1.525902189314365386962890625e-05;
    uv.x += 3.0517578125e-05 * ((2.0 * float((unormUV.x & 32768u) >> uint(15))) - 1.0);
    uv.y += 3.0517578125e-05 * ((2.0 * float((unormUV.y & 32768u) >> uint(15))) - 1.0);
    pos.z += ZShiftValue.x;

#if LIT
    vec3 N = normalize(mul(u_model[0], a_normal).xyz);
    N.y *= TileLightColor.w;
    float yLight = (1.0 + N.y) * 0.275;
    float lightIntensity = yLight + (N.x * N.x * -0.1) + (N.z * N.z * 0.1) + 0.45;
    v_light = vec4(vec3_splat(lightIntensity) * TileLightColor.xyz, 1.0) + vec4(vec3_splat(OverlayColor.w * 0.35), 0.0);
#else
    v_light = vec4(0.0, 0.0, 0.0, 0.0);
#endif

    v_clipPosition = clipPos;
    v_color = a_color0;
    v_fog = calculateFog(FogColor, pos.z, FogControl);
    v_texCoords = UVAnimation.xy + (uv * UVAnimation.zw);
    v_worldPos = worldPos.xyz;
    gl_Position = pos;
}
