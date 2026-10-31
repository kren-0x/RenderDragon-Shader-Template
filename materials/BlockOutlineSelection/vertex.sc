$input a_position

#include <bgfx_shader.sh>

uniform vec4 SubPixelOffset;

void main() {
    mat4 proj = u_proj;
    proj[2].x += SubPixelOffset.x;
    proj[2].y -= SubPixelOffset.y;
    gl_Position = mul(proj, mul(u_view, vec4(a_position, 1.0)));
}
