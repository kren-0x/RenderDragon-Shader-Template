$input v_colorUV, v_encodedPlane, v_fog, v_parallaxUV

#include <bgfx_shader.sh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;

SAMPLER2D_AUTOREG(s_ColorTexture);
SAMPLER2D_AUTOREG(s_ParallaxTexture);

void main() {
    vec4 colorTex = texture2D(s_ColorTexture, v_colorUV);
    vec4 parallaxTex = texture2D(s_ParallaxTexture, v_parallaxUV);
    vec4 outColor;
    if ((v_encodedPlane * 32.0) > 31.0) {
        outColor = vec4(v_fog.xyz * v_fog.w, 1.0);
    } else {
        outColor = vec4((colorTex.xyz * (parallaxTex.xyz * (1.0 - v_encodedPlane))).xyz * (1.0 - v_fog.w), 0.0);
    }
    gl_FragColor = outColor;
}
