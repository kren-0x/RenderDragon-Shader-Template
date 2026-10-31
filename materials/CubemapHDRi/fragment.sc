$input v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ColorUtil.dragonh>
#include <MinecraftRenderer.Materials/CubemapUtil.dragonh>

SAMPLERCUBE_AUTOREG(s_MatTexture);

void main() {
    vec3 dir = getCubemapDirection(v_texcoord0);
    vec4 sampledColor = textureCube(s_MatTexture, dir);
    gl_FragColor = fastDegamma(sampledColor);
}
