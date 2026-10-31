$input v_clipPosition, v_color, v_fog, v_light, v_texCoords, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 AlphaMaskedTint;
uniform vec4 ChangeColor;
uniform vec4 CurrentColor;
uniform vec4 DiscardValue;
uniform vec4 HudOpacity;
uniform vec4 OverlayColor;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
    vec4 diffuse = texture2D(s_MatTexture, v_texCoords);

#ifdef ALPHA_TEST_DISCARD_VALUE_BASED
    if (diffuse.a < DiscardValue.x) {
        discard;
    }
#elif defined(ALPHA_TEST_VERTEX_TINT_MASK_BASED)
    if (diffuse.a <= 0.0) {
        discard;
    }
#endif

    diffuse = applyOverlayColor(diffuse, OverlayColor);

#if LIT
    diffuse = applyLighting(diffuse, v_light);
#endif

    if (AlphaMaskedTint.x != 0.0) {
        diffuse.rgb = (CurrentColor.rgb * mix(diffuse.rgb, diffuse.rgb * v_color.rgb, diffuse.a)) * v_color.a;
        diffuse.a = 1.0;
    } else {
        diffuse = (CurrentColor * v_color) * diffuse;
    }

    diffuse = applyHudOpacity(diffuse, HudOpacity);
    diffuse = applyFog(diffuse, v_fog);
    gl_FragColor = diffuse;
}
