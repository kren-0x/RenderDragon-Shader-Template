#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 TintColor;
uniform vec4 HudOpacity;

void main() {
    vec4 diffuse = TintColor;
    diffuse = applyHudOpacity(diffuse, HudOpacity);
    gl_FragColor = diffuse;
}
