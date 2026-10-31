$input v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/CubemapUtil.dragonh>

SAMPLERCUBE_AUTOREG(s_MatTexture);

void main() {
    vec3 dir = getCubemapDirection(v_texcoord0);
    gl_FragColor = textureCube(s_MatTexture, dir);
}
