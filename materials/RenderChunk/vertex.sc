$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING
    $input i_data1, i_data2, i_data3
#endif

$output v_clipPosition, v_color0, v_ditheringAndMaskTinting, v_fog, v_lightmapUV, v_texcoord0, v_worldPos, v_worldPosition

#include <bgfx_shader.sh>
#include <MinecraftRenderer.Materials/TAAUtil.dragonh>

uniform vec4 RenderChunkFogAlpha;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;
uniform vec4 MeshContext;

void main() {
#ifdef INSTANCING
    mat4 model = mtxFromCols(i_data1, i_data2, i_data3, vec4(0.0, 0.0, 0.0, 1.0));
    vec3 worldPos = instMul(model, vec4(a_position, 1.0)).xyz;
#else
    vec3 worldPos = mul(u_model[0], vec4(a_position, 1.0)).xyz;
#endif

    vec4 color;
#ifdef RENDER_AS_BILLBOARDS
    worldPos += vec3_splat(0.5);
    vec3 viewDir = normalize(worldPos - ViewPositionAndTime.xyz);
    vec3 boardPlane = normalize(cross(vec3(0.0, 1.0, 0.0), viewDir));
    worldPos -= (cross(viewDir, boardPlane) * (a_color0.z - 0.5)) + (boardPlane * (a_color0.x - 0.5));
    color = vec4_splat(1.0);
#else
    color = a_color0;
#endif

    vec4 fogAndDist = FogAndDistanceControl;
    if (MeshContext.x > 0.5) {
        fogAndDist = vec4(0.99, 1.0, 100000.0, 100000.0);
    }

    uvec2 uv0_16 = uvec2(round(a_texcoord0 * 65535.0));
    vec2 uv0 = vec2(float((uv0_16.x & 32767u) << uint(1)), float((uv0_16.y & 32767u) << uint(1))) * 1.525902189314365386962890625e-05;
    uv0.x += 3.0517578125e-05 * ((2.0 * float((uv0_16.x & 32768u) >> uint(15))) - 1.0);
    uv0.y += 3.0517578125e-05 * ((2.0 * float((uv0_16.y & 32768u) >> uint(15))) - 1.0);

    uvec2 uv1_16 = uvec2(round(a_texcoord1 * 65535.0));
    float lightmapX = float((uv1_16.y >> 4u) & 15u) * 0.06666667;
    float lightmapY = float(uv1_16.y & 15u) * 0.06666667;
    vec2 lightmapUV = vec2(lightmapX, lightmapY);

    float ditherX = float((uv1_16.x & 256u) != 0u ? 1.0 : 0.0);
    float ditherY = float((uv1_16.y & 256u) != 0u ? 1.0 : 0.0);
    vec2 ditheringAndMaskTinting = vec2(ditherX, ditherY);

    vec4 position = jitterVertexPosition(worldPos);

    float camDist = length(ViewPositionAndTime.xyz - worldPos);
    float fogIntensity = clamp((((camDist / fogAndDist.z) + RenderChunkFogAlpha.x) - fogAndDist.x) / (fogAndDist.y - fogAndDist.x), 0.0, 1.0);
    vec4 fog = vec4(FogColor.rgb, fogIntensity);

#if defined(TRANSPARENT)
    if (a_color0.a < 0.95) {
        color.a = mix(a_color0.a, 1.0, clamp(camDist / FogAndDistanceControl.w, 0.0, 1.0));
    }
#endif

    v_clipPosition = position;
    v_worldPosition = vec4(worldPos, 1.0);
    v_color0 = color;
    v_ditheringAndMaskTinting = ditheringAndMaskTinting;
    v_fog = fog;
    v_lightmapUV = lightmapUV;
    v_texcoord0 = uv0;
    v_worldPos = worldPos;
    gl_Position = position;
}
