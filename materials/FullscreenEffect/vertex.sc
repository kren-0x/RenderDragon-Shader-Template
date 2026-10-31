$input a_color0, a_position, a_texcoord0
$output v_color, v_fog, v_light, v_texCoords

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/DynamicUtil.dragonh>

uniform vec4 SubPixelOffset;
uniform vec4 UVAnimation;
uniform vec4 ZShiftValue;

void main() {
    mat4 proj = u_proj;
    proj[2].x += SubPixelOffset.x;
    proj[2].y -= SubPixelOffset.y;
    vec4 worldPos = mul(u_model[0], vec4(a_position, 1.0));
    vec4 clipPos = mul(proj, mul(u_view, vec4(worldPos.xyz, 1.0)));
    vec2 uv = unpackTextureCoords(a_texcoord0);
    clipPos.z += ZShiftValue.x;

    v_color = a_color0;
    v_fog = vec4_splat(0.0);
    v_light = vec4_splat(0.0);
    v_texCoords = applyUvAnimation(uv, UVAnimation);
    gl_Position = clipPos;
}
