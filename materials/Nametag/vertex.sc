$input a_color0, a_position
$output v_clipPosition, v_color, v_worldPos

#include <bgfx_shader.sh>

uniform vec4 SubPixelOffset;
uniform vec4 ZShiftValue;

void main() {
    vec4 worldPos = mul(u_model[0], vec4(a_position, 1.0));
    mat4 proj = u_proj;
    proj[2].x += SubPixelOffset.x;
    proj[2].y -= SubPixelOffset.y;
    vec4 clipPos = mul(proj, mul(u_view, vec4(worldPos.xyz, 1.0)));
    vec4 pos = clipPos;
    pos.z += ZShiftValue.x;
    v_clipPosition = clipPos;
    v_color = a_color0;
    v_worldPos = worldPos.xyz;
    gl_Position = pos;
}
