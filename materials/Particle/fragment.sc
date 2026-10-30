$input v_color0, v_fog, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

SAMPLER2D_AUTOREG(s_ParticleTexture);

void main() {
    vec4 diffuse = texture2D(s_ParticleTexture, v_texcoord0);

#if defined(ALPHA_TEST)
    if (diffuse.a < 0.5) {
        discard;
    }
#endif

    diffuse *= v_color0;

#if defined(ALPHA_TEST)
    diffuse.a = 1.0;
#endif

    diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);

    gl_FragColor = diffuse;
}
