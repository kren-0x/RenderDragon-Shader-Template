$input v_color0, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>

SAMPLER2D_AUTOREG(s_SunMoonTexture);
uniform vec4 SunMoonColor;

void main() {
    gl_FragColor = SunMoonColor * texture2D(s_SunMoonTexture, v_texcoord0);
}
