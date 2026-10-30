$input v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>

uniform vec4 ColorBased;
uniform vec4 ChangeColor;
uniform vec4 MatColor;
uniform vec4 OverlayColor;
uniform vec4 MultiplicativeTintColor;
uniform vec4 ActorFPEpsilon;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture1);

void main() {
#if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY_ALPHA_TEST)
    gl_FragColor = vec4_splat(0.0);
    return;
#else

    vec4 albedo = getActorAlbedoNoColorChange(v_texcoord0, s_MatTexture, s_MatTexture1, MatColor);

#if defined(CHANGE_COLOR_MULTI)
    albedo = applyMultiColorChange(albedo, ChangeColor.rgb, MultiplicativeTintColor.rgb);
#elif defined(CHANGE_COLOR)
    albedo = applyColorChange(albedo, ChangeColor, albedo.a);
    albedo.a *= ChangeColor.a;
#endif

    vec4 tintTex = texture2D(s_MatTexture1, v_texcoord0);
    vec3 tintColor = tintTex.rgb * MultiplicativeTintColor.rgb;
    vec3 blendedRgb = mix(albedo.rgb, tintColor, tintTex.a);

    bool ditherDiscard = false;
    if (DitheringEnabledToggle.x != 0.0) {
        vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[0]);
    }

#if defined(ALPHA_TEST)
    if (ditherDiscard || (albedo.a + tintTex.a) < ActorFPEpsilon.x) {
        discard;
    }
#elif defined(TRANSPARENT)
    if (ditherDiscard) {
        albedo.a = 0.0;
    }
#endif

    albedo.rgb = mix(blendedRgb * mix(vec3_splat(1.0), v_color0.rgb, ColorBased.x), OverlayColor.rgb, OverlayColor.a) * v_light.rgb;
    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);

    gl_FragColor = albedo;
#endif
}
