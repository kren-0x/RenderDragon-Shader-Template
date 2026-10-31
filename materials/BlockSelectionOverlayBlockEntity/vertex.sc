$input a_position, a_indices, a_texcoord0
$output v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/SelectionUtil.dragonh>

uniform mat4 Bones[8];

void main() {
    mat4 World = mul(u_model[0], Bones[int(a_indices)]);
    vec4 position = mul(u_viewProj, mul(World, vec4(a_position, 1.0)));
    position = applySelectionDepthBias(position);
    v_texcoord0 = a_texcoord0;
    gl_Position = position;
}
