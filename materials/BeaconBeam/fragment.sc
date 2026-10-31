$input v_color, v_fog, v_light, v_texCoords

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

uniform vec4 OverlayColor;

SAMPLER2D_AUTOREG(s_BeaconTexture);

void main() {
#if defined(TRANSPARENT)
    vec4 diffuse = texture2D(s_BeaconTexture, v_texCoords);
    diffuse = applyOverlayColor(diffuse, OverlayColor);
    diffuse *= v_color;
#else
    vec4 diffuse = applyOverlayColor(v_color, OverlayColor);
    diffuse = applyLighting(diffuse, v_light);
    diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);
#endif
    gl_FragColor = diffuse;
}
