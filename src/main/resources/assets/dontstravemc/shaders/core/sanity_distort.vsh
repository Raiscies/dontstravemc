#version 330

// 1.21.10 标准：必须声明 SamplerInfo，即使不一定用，防止 UBO 绑定错位
layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// 输出给 FSH
out vec2 texCoord;

void main() {
    // 官方蜘蛛网滤镜同款写法：利用位运算生成全屏覆盖的 UV
    // 覆盖范围：(0,0) -> (2,0) -> (0,2) 的大三角形，裁剪后即为全屏
    vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    vec4 pos = vec4(uv * vec2(2, 2) + vec2(-1, -1), 0, 1);

    gl_Position = pos;
    texCoord = uv;
}