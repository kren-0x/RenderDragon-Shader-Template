$input v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 TintColor;
uniform vec4 HudOpacity;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
    vec4 tex = texture2D(s_MatTexture, v_texcoord0);
    vec4 diffuse = tex * TintColor;
    diffuse = applyHudOpacity(diffuse, HudOpacity);
    gl_FragColor = diffuse;
}
