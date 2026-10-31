$input a_position, a_indices, a_texcoord0
$output v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform mat4 Bones[8];
uniform vec4 UVScale;

void main() {
    mat4 World = mul(u_model[0], Bones[int(a_indices)]);
    vec3 worldPosition = mul(World, vec4(a_position, 1.0)).xyz;
    v_texcoord0 = a_texcoord0 * UVScale.xy;
    gl_Position = jitterVertexPosition(worldPosition);
}
