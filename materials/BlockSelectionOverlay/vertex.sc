$input a_position, a_texcoord0
$output v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>
#include <MinecraftRenderer.Materials/SelectionUtil.dragonh>

void main() {
    vec4 position = jitterVertexPosition(a_position);
    position = applySelectionDepthBias(position);
    v_texcoord0 = a_texcoord0;
    gl_Position = position;
}
