$input v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>

uniform vec4 AlphaMaskedTint;
uniform vec4 ChangeColor;
uniform vec4 OverlayColor;
uniform vec4 ColorBased;
uniform vec4 MultiplicativeTintColor;
uniform vec4 MatColor;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
#if defined(DEPTH_ONLY)
    gl_FragColor = vec4_splat(0.0);
    return;
#else
    vec4 albedo = texture2D(s_MatTexture, v_texcoord0);

    if (AlphaMaskedTint.x != 0.0) {
        albedo.rgb = mix(albedo.rgb, albedo.rgb * v_color0.rgb, albedo.a) * v_color0.a;
        albedo.a = 1.0;
    } else {
        albedo.rgb *= v_color0.rgb;
    }

    albedo *= MatColor;

    vec3 tinted = albedo.rgb * mix(vec3_splat(1.0), v_color0.rgb, ColorBased.x);

#if defined(MULTI_COLOR_TINT)
    vec2 colorMask = tinted.rg;
    vec3 multiTint = mix(tinted.r * ChangeColor.rgb, tinted.g * MultiplicativeTintColor.rgb, ceil(colorMask.g));
    albedo.rgb = mix(multiTint, OverlayColor.rgb, OverlayColor.a) * v_light.rgb;
#else
    vec4 baseTint = vec4(tinted, albedo.a);
    vec3 singleTint = mix(baseTint, baseTint * ChangeColor, v_color0.a).rgb;
    albedo.rgb = mix(singleTint, OverlayColor.rgb, OverlayColor.a) * v_light.rgb;
#endif

    bool ditherDiscard = false;
    if (DitheringEnabledToggle.x != 0.0) {
        vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[0]);
    }

#if defined(ALPHA_TEST)
    if (ditherDiscard || albedo.a < 0.5) {
        discard;
    }
#elif defined(TRANSPARENT)
    if (ditherDiscard) {
        albedo.a = 0.0;
    }
#endif

    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);
    gl_FragColor = albedo;
#endif
}
