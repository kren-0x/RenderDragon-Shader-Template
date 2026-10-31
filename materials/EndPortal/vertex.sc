$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING
$input i_data1, i_data2, i_data3
#endif
$output v_colorUV, v_encodedPlane, v_fog, v_parallaxUV

#include <bgfx_shader.sh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;

void main() {
#ifdef INSTANCING
    mat4 model;
    model[0] = vec4(i_data1.x, i_data2.x, i_data3.x, 0.0);
    model[1] = vec4(i_data1.y, i_data2.y, i_data3.y, 0.0);
    model[2] = vec4(i_data1.z, i_data2.z, i_data3.z, 0.0);
    model[3] = vec4(i_data1.w, i_data2.w, i_data3.w, 1.0);
    vec4 worldPos = mul(model, vec4(a_position, 1.0));
#else
    vec4 worldPos = mul(u_model[0], vec4(a_position, 1.0));
#endif
    vec3 wpos = worldPos.xyz;
    vec4 planeData = (a_color0 - vec4(0.5, 0.5, 0.5, 0.0)) * vec4(2.0, 2.0, 2.0, 32.0);
    vec3 normal = planeData.xyz;
    float planeIndex = planeData.w;
    vec3 ray = (wpos * (dot(wpos - (normal * planeIndex), normal) / dot(wpos, normal))) + ViewPositionAndTime.xyz;
    vec3 absNormal = abs(normal);
    float s = sin(planeIndex * 2.2439947);
    float c = cos(planeIndex * 2.2439947);
    mat2 rot = mat2(vec2(c, s), vec2(-s, c));
    vec2 projUV = ((ray.yz * absNormal.x) + (ray.xz * absNormal.y) + (ray.xy * absNormal.z)) * 0.0625;
    vec2 parallaxUV = mul(rot, projUV) + (vec2(c, s) * planeIndex);
    parallaxUV.y += ViewPositionAndTime.w * 0.00390625;
    parallaxUV = parallaxUV - 64.0 * trunc(parallaxUV / 64.0);

    v_colorUV = a_texcoord0;
    v_encodedPlane = a_color0.w;
    float fogDepth = length(wpos);
    v_fog = vec4(FogColor.xyz, clamp(((fogDepth / FogAndDistanceControl.z) - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0, 1.0));
    v_parallaxUV = parallaxUV;
    gl_Position = mul(u_viewProj, vec4(wpos, 1.0));
}
