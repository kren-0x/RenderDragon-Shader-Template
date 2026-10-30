$input v_clipPosition, v_color, v_fog, v_light, v_texCoords, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>
#include <MinecraftRenderer.Materials/MeshFallbackUtil.dragonh>

uniform vec4 AlphaMaskedTint;
uniform vec4 ChangeColor;
uniform vec4 CurrentColor;
uniform vec4 DiscardValue;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;
uniform vec4 HudOpacity;
uniform vec4 OverlayColor;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
#if defined(DEPTH_ONLY_ALPHA_TEST)
    vec4 diffuse = vec4_splat(1.0);
#if defined(USE_TEXTURES)
    diffuse = texture2D(s_MatTexture, v_texCoords);
#endif
    if (diffuse.a <= 0.0) {
        discard;
    }
    gl_FragColor = vec4_splat(0.0);
    return;
#else

    vec4 diffuse = vec4_splat(1.0);
#if defined(USE_TEXTURES)
    diffuse = texture2D(s_MatTexture, v_texCoords);
#endif

#if defined(ALPHA_TEST_DISCARD_VALUE) || defined(ALPHA_TEST_TINT_MASK)
    if (AlphaMaskedTint.x == 0.0) {
        bool ditherDiscard = false;
        if (DitheringEnabledToggle.x != 0.0) {
            vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
            ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[1]);
        }
#if defined(ALPHA_TEST_DISCARD_VALUE)
        if (ditherDiscard || diffuse.a < DiscardValue.x) {
            discard;
        }
#elif defined(ALPHA_TEST_TINT_MASK)
        if (ditherDiscard || diffuse.a <= 0.0) {
            discard;
        }
#endif
    }
#endif

#if defined(MULTI_COLOR_TINT)
    diffuse = applyMeshMultiColorChange(diffuse, v_color, ChangeColor.rgb);
    diffuse = applyOverlayColor(diffuse, OverlayColor);
#else
    diffuse = applyOverlayColor(diffuse, OverlayColor);
#endif

#if defined(LIT)
    diffuse = applyLighting(diffuse, v_light);
#endif

    if (AlphaMaskedTint.x != 0.0) {
        diffuse.rgb = CurrentColor.rgb * mix(diffuse.rgb, diffuse.rgb * v_color.rgb, diffuse.a) * v_color.a;
        diffuse.a = 1.0;
    }
#if !defined(MULTI_COLOR_TINT)
    else {
        diffuse = CurrentColor * v_color * diffuse;
    }
#endif

    diffuse.a *= HudOpacity.x;
    diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);

    gl_FragColor = diffuse;
#endif
}
