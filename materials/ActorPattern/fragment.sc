$input v_clipPosition, v_color0, v_fog, v_light, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>
#include <MinecraftRenderer.Materials/DitherUtil.dragonh>
#include <MinecraftRenderer.Materials/PatternUtil.dragonh>

uniform vec4 ActorFPEpsilon;
uniform vec4 HudOpacity;
uniform vec4 DitherParams;
uniform vec4 DitherParams2[3];
uniform vec4 DitheringEnabledToggle;
uniform vec4 PatternColors[7];
uniform vec4 PatternCount;
uniform vec4 PatternUVOffsetsAndScales[7];

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture2);

void main() {
#if defined(DEPTH_ONLY) || defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY_ALPHA_TEST)
    gl_FragColor = vec4_splat(0.0);
    return;
#else

    vec4 albedo = texture2D(s_MatTexture, v_texcoord0);

    bool ditherDiscard = false;
    if (DitheringEnabledToggle.x != 0.0) {
        vec3 camPos = mul(u_invView, vec4(0.0, 0.0, 0.0, 1.0)).xyz;
        ditherDiscard = evaluateDitherDiscard(v_clipPosition, v_worldPos, camPos, u_view, DitherParams, DitherParams2[0]);
    }

#if defined(TINTING)
    albedo = applyActorPatterns(albedo, v_texcoord0, s_MatTexture2, PatternCount.x, PatternColors, PatternUVOffsetsAndScales);
#endif

#if defined(ALPHA_TEST)
    if (ditherDiscard || albedo.a < ActorFPEpsilon.x) {
        discard;
    }
#elif defined(TRANSPARENT) || defined(OPAQUE)
    if (ditherDiscard) {
        albedo.a = 0.0;
    }
#endif

#if !defined(UI_ENTITY)
    albedo.rgb *= v_light.rgb;
#endif

    albedo.a *= HudOpacity.x;
    albedo.rgb = applyFog(albedo.rgb, v_fog.rgb, v_fog.a);

    gl_FragColor = albedo;
#endif
}
