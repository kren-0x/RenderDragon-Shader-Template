$input v_color0, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>

SAMPLER2D_AUTOREG(s_SkyTexture);
uniform vec4 SkyColor;

void main() {
    gl_FragColor = SkyColor * texture2D(s_SkyTexture, v_texcoord0);
}
