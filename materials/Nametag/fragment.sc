$input v_color

#include <bgfx_shader.sh>

uniform vec4 CurrentColor;
uniform vec4 OverlayColor;

void main() {
    vec3 overlay = mix(vec3(1.0, 1.0, 1.0), OverlayColor.xyz, OverlayColor.w);
    gl_FragColor = (CurrentColor * v_color) * vec4(overlay, 1.0);
}
