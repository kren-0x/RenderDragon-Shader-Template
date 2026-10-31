$input a_position

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

void main() {
    gl_Position = jitterVertexPosition(a_position);
}
