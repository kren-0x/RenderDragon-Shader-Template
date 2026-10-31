$input a_position, a_texcoord0
$output v_fog, v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 UVOffset;

void main() {
    vec4 pos = mul(u_viewProj, vec4(mul(u_model[0], vec4(a_position, 1.0)).xyz, 1.0));
    v_fog = calculateFog(FogColor, pos.z, FogAndDistanceControl);
    v_texcoord0 = a_texcoord0;
    gl_Position = pos;
}
