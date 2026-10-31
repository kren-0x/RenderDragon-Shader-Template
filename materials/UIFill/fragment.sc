$input v_color0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 TintColor;
uniform vec4 HudOpacity;

void main() {
    vec4 diffuse = v_color0 * TintColor;
    diffuse = applyHudOpacity(diffuse, HudOpacity);
    gl_FragColor = diffuse;
}
