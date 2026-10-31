$input v_color, v_texCoords

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 CurrentColor;
uniform vec4 DiscardValue;
uniform vec4 HudOpacity;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
#if defined(USE_TEXTURES)
    vec4 diffuse = texture2D(s_MatTexture, v_texCoords);
#else
    vec4 diffuse = vec4_splat(1.0);
#endif

#if defined(ALPHA_TEST_DISCARD_VALUE_BASED)
    if (diffuse.a < DiscardValue.x) {
        discard;
    }
#elif defined(ALPHA_TEST_VERTEX_TINT_MASK_BASED)
    if (diffuse.a <= 0.0) {
        discard;
    }
#endif

    diffuse = (CurrentColor * v_color) * diffuse;
    diffuse = applyHudOpacity(diffuse, HudOpacity);
    gl_FragColor = diffuse;
}
