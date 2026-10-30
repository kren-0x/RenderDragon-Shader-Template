$input v_clipPosition, v_color0, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ActorUtil.dragonh>

uniform vec4 TintedAlphaTestEnabled;
uniform vec4 MatColor;
uniform vec4 OverlayColor;
uniform vec4 ActorFPEpsilon;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture1);

void main() {
#if defined(DEPTH_ONLY_OPAQUE)
    gl_FragColor = vec4_splat(0.0);
    return;
#elif defined(DEPTH_ONLY_ALPHA_TEST)
    vec4 albedo = getActorAlbedoNoColorChange(v_texcoord0, s_MatTexture, s_MatTexture1, MatColor);
    float alpha = mix(albedo.a, albedo.a * OverlayColor.a, TintedAlphaTestEnabled.x);
    if (shouldDiscard(albedo.rgb, alpha, ActorFPEpsilon.x)) {
        discard;
    }
    gl_FragColor = vec4_splat(0.0);
    return;
#else
    gl_FragColor = vec4_splat(0.0);
    return;
#endif
}
