$input v_color0, v_fog, v_occlusionHeight, v_occlusionUV, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

uniform vec4 OcclusionHeightOffset;

SAMPLER2D_AUTOREG(s_WeatherTexture);
SAMPLER2D_AUTOREG(s_OcclusionTexture);
SAMPLER2D_AUTOREG(s_LightingTexture);

float getOcclusionHeight(const vec4 occlusionTextureSample) {
    float height = occlusionTextureSample.g + (occlusionTextureSample.b * 255.0) - (OcclusionHeightOffset.x / 255.0);
    return height;
}

float getOcclusionLuminance(const vec4 occlusionTextureSample) {
    return occlusionTextureSample.r;
}

bool isOccluded(const vec2 occlusionUV, const float occlusionHeight, const float occlusionHeightThreshold) {
#if defined(NO_OCCLUSION)
    return false;
#else
    bool withinBounds = (occlusionUV.x >= 0.0 && occlusionUV.x <= 1.0 && occlusionUV.y >= 0.0 && occlusionUV.y <= 1.0);
    #if defined(FLIP_OCCLUSION)
        return withinBounds && (occlusionHeight > occlusionHeightThreshold);
    #else
        return withinBounds && (occlusionHeight < occlusionHeightThreshold);
    #endif
#endif
}

void main() {
    vec4 diffuse = texture2D(s_WeatherTexture, v_texcoord0);
    vec4 occlusionSample = texture2D(s_OcclusionTexture, v_occlusionUV);

    float occlusionLuminance = getOcclusionLuminance(occlusionSample);
    float occlusionHeightThreshold = getOcclusionHeight(occlusionSample);

    if (isOccluded(v_occlusionUV, v_occlusionHeight, occlusionHeightThreshold)) {
        diffuse.a = 0.0;
    } else {
        float mixAmount = (v_occlusionHeight - occlusionHeightThreshold) * 25.0;
        float uvX = occlusionLuminance - (mixAmount * occlusionLuminance);
        vec2 lightingUV = vec2(uvX, 1.0);
        vec3 light = texture2D(s_LightingTexture, lightingUV).rgb;
        diffuse.rgb *= light;
        diffuse.rgb = applyFog(diffuse.rgb, v_fog.rgb, v_fog.a);
    }
    gl_FragColor = diffuse;
}
