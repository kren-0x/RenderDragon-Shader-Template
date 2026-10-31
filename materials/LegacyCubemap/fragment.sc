$input v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/ColorUtil.dragonh>

uniform vec4 ColorGrading_OptimizeGammaCorrection;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
    vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
#if defined(TRANSPARENT_DEGAMMA)
    diffuse.rgb = degammaColor(diffuse.rgb, ColorGrading_OptimizeGammaCorrection.x != 0.0);
#endif
    gl_FragColor = diffuse;
}
