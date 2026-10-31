$input v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/CracksUtil.dragonh>

SAMPLER2D_AUTOREG(s_CracksTexture);

void main() {
    vec4 diffuse = texture2D(s_CracksTexture, v_texcoord0);
    if (shouldDiscardCrack(diffuse.a)) {
        discard;
    }
    gl_FragColor = diffuse;
}
