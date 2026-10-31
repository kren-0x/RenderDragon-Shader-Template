$input v_texcoord0, v_color0

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/UIUtil.dragonh>

uniform vec4 TintColor;
uniform vec4 HudOpacity;
uniform vec4 ChangeColor;

SAMPLER2D_AUTOREG(s_MatTexture);

void main() {
    vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);

#ifdef MULTI_COLOR_TINT
    vec2 colorMask = diffuse.rg;
    diffuse.rgb = colorMask.rrr * v_color0.rgb;
    diffuse.rgb = mix(diffuse.rgb, colorMask.ggg * ChangeColor.rgb, ceil(colorMask.g));
#else
    diffuse.rgb = mix(diffuse.rgb, diffuse.rgb * v_color0.rgb, diffuse.a);
#endif

#if !defined(BLEND)
    if (v_color0.a > 0.0) {
        diffuse.a = ceil(diffuse.a);
    }
#endif

#if defined(ALPHA_TEST)
    if (diffuse.a < 0.5) {
        discard;
    }
#endif

    diffuse *= TintColor;
    diffuse = applyHudOpacity(diffuse, HudOpacity);

    gl_FragColor = diffuse;
}
