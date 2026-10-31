$input v_fog, v_texcoord0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 UVOffset;

SAMPLER2D_AUTOREG(s_FlameAtlas);

void main() {
    vec4 diffuse = texture2D(s_FlameAtlas, v_texcoord0 + UVOffset.xy);
    if (diffuse.a < 0.5) {
        discard;
    }
    diffuse = applyFog(diffuse, v_fog);
    gl_FragColor = diffuse;
}
