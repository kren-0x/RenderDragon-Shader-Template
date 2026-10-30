$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_color0, v_fog, v_texcoord0, v_worldPos

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/FogUtil.dragonh>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;

void main() {
    vec3 worldPosition;
#ifdef INSTANCING
    mat4 model = mtxFromCols(i_data1, i_data2, i_data3, vec4(0.0, 0.0, 0.0, 1.0));
    worldPosition = instMul(model, vec4(a_position, 1.0)).xyz;
#else
    worldPosition = mul(u_model[0], vec4(a_position, 1.0)).xyz;
#endif

    vec4 position = mul(u_viewProj, vec4(worldPosition, 1.0));
    float cameraDepth = position.z;
    float fogIntensity = calculateFogIntensity(cameraDepth, FogAndDistanceControl.z, FogAndDistanceControl.x, FogAndDistanceControl.y);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

    v_color0 = a_color0;
    v_fog = fog;
    v_texcoord0 = a_texcoord0;
    v_worldPos = worldPosition;
    gl_Position = position;
}
