$input v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_texcoords, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>
#include <MinecraftRenderer.Materials/BannerUtil.dragonh>

uniform vec4 ActorFPEpsilon;
uniform vec4 HudOpacity;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
#if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY_ALPHA_TEST)
    gl_FragColor = vec4_splat(0.0);
    return;
#else

    vec4 baseTex = texture2D(s_MatTexture, v_texcoords.zw);
#if defined(TINTING)
    vec4 patternTex = texture2D(s_MatTexture, v_texcoords.xy);
    vec4 diffuse = applyBannerTint(baseTex, patternTex, v_color0);
#else
    vec4 diffuse = baseTex;
#endif

    bool ditherDiscard = false;
    if (DitheringEnabledToggle.x != 0.0) {
        vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[0]);
    }

#if defined(ALPHA_TEST)
    if (ditherDiscard || diffuse.a < ActorFPEpsilon.x) {
        discard;
    }
#elif defined(TRANSPARENT) || defined(OPAQUE)
    if (ditherDiscard) {
        diffuse.a = 0.0;
    }
#endif

    diffuse.rgb *= v_light.rgb;
    diffuse.a *= HudOpacity.x;
    diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);

    gl_FragColor = diffuse;
#endif
}
