$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_color0, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 CloudColor;
uniform vec4 DistanceControl;

void main() {
    vec3 worldPosition;
#ifdef INSTANCING
    mat4 model = mtxFromCols(i_data1, i_data2, i_data3, vec4(0.0, 0.0, 0.0, 1.0));
    worldPosition = instMul(model, vec4(a_position, 1.0)).xyz;
#else
    worldPosition = mul(u_model[0], vec4(a_position, 1.0)).xyz;
#endif

    vec4 color = a_color0 * CloudColor;
    float dist = length(worldPosition);
    color.a *= clamp(1.0 - max((dist / DistanceControl.x) - 0.9, 0.0), 0.0, 1.0);

    v_color0 = color;
    v_texcoord0 = a_texcoord0;
    v_worldPos = worldPosition;
    gl_Position = jitterVertexPosition(worldPosition);
}
