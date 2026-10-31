$input v_texcoord0

#include <bgfx_shader.sh>

uniform vec4 MatColor;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
    vec4 tex = texture2D(s_MatTexture, v_texcoord0);
    if (tex.a < 0.5) {
        discard;
    }
    gl_FragColor = MatColor;
}
