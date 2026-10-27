$input v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>

uniform vec4 ColorBased;
uniform vec4 ChangeColor;
uniform vec4 UseAlphaRewrite;
uniform vec4 TintedAlphaTestEnabled;
uniform vec4 MatColor;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 MultiplicativeTintColor;
uniform vec4 ActorFPEpsilon;
uniform vec4 HudOpacity;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture1);
SAMPLER2D_AUTOREG(s_MatTexture2);

void main() {
#if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE)
    gl_FragColor = vec4_splat(0.0);
    return;
#else

    vec4 albedo = getActorAlbedoNoColorChange(v_texcoord0, s_MatTexture, s_MatTexture1, MatColor);

    bool ditherDiscard = false;
    if (DitheringEnabledToggle.x != 0.0) {
        vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[0]);
    }

#if defined(ALPHA_TEST)
    float alpha = mix(albedo.a, albedo.a * OverlayColor.a, TintedAlphaTestEnabled.x);
    if (ditherDiscard || shouldDiscard(albedo.rgb, alpha, ActorFPEpsilon.x)) {
        discard;
    }
#endif

#if defined(CHANGE_COLOR_MULTI)
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#elif defined(CHANGE_COLOR)
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a *= ChangeColor.a;
#endif

#if defined(ALPHA_TEST)
    albedo.a = max(UseAlphaRewrite.x, albedo.a);
#endif

    vec4 tex1 = texture2D(s_MatTexture1, v_texcoord0);
    vec4 tex2 = texture2D(s_MatTexture2, v_texcoord0);

#if defined(COLOR_SECOND_TEXTURE)
    albedo.rgb = mix(albedo.rgb, tex1.rgb, tex1.a);
    if (tex2.a > ActorFPEpsilon.x) {
        vec3 tintedTex2 = mix(tex2.rgb, tex2.rgb * ChangeColor.rgb, tex2.a);
        albedo.rgb = tintedTex2;
    }
#else
    albedo.rgb = mix(albedo.rgb, tex1.rgb, tex1.a);
    albedo.rgb = mix(albedo.rgb, tex2.rgb, tex2.a);
#endif

    albedo = applyActorDiffuse(albedo, v_color0.rgb, v_light, ColorBased.x, OverlayColor);

#if defined(TRANSPARENT)
    albedo = applyHudOpacity(albedo, HudOpacity.x);
    if (ditherDiscard) {
        albedo.a = 0.0;
    }
#endif

    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);
    gl_FragColor = albedo;
#endif
}
