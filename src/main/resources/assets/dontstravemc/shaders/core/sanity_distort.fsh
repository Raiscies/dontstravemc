#version 330

uniform sampler2D InSampler;
in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform SanityConfig {
    float Sanity;      // 1.0(正常) -> 0.0(疯了)
    float Time;
    vec2 _Padding;
};

out vec4 fragColor;

const float RANGE = 0.00625;
const float TIME_SCALE = 10.0;

void main() {
    // 1. 计算理智值强度
    float intensity = 1.0 - smoothstep(0.1, 0.7, Sanity);

    // 2. 基础 UV 变换 (抖动 + 呼吸)
    vec2 jitterOffset = vec2(
    cos(Time * TIME_SCALE + 0.25),
    sin(Time * TIME_SCALE)
    ) * RANGE * intensity;

    vec2 centeredUV = texCoord - vec2(0.5);
    float fisheyePulse = (1.0 + sin(Time * 2.0)) * 0.5;
    vec2 lensVec = centeredUV * 0.05 * fisheyePulse * intensity;

    // 3. 中心屏蔽蒙版 (确保重影和抖动不影响准星)
    float dist = length(centeredUV);
    float innerRadius = 0.2;
    float outerRadius = 0.6;
    float distortionMask = clamp((dist - innerRadius) / (outerRadius - innerRadius), 0.0, 1.0);

    // 4. 重影逻辑 (Ghosting / Double Vision)
    // 根据时间缓慢张开和收缩重影
    // ghostSpread 决定了重影“分”得多开
    float ghostSpread = 0.015 * intensity * (0.5 + 0.5 * sin(Time * 0.5));

    // 我们计算三个采样位置：中心、左偏、右偏
    vec2 uvMain  = mix(texCoord, texCoord + jitterOffset + lensVec, distortionMask);
    vec2 uvLeft  = mix(texCoord, texCoord + jitterOffset + lensVec + vec2(-ghostSpread, 0.0), distortionMask);
    vec2 uvRight = mix(texCoord, texCoord + jitterOffset + lensVec + vec2(ghostSpread, 0.0), distortionMask);

    // 多次采样
    vec4 colMain  = texture(InSampler, uvMain);
    vec4 colLeft  = texture(InSampler, uvLeft);
    vec4 colRight = texture(InSampler, uvRight);

    // 5. 颜色合成 (Alpha Blending 模拟重影)
    // 理智越低，两侧重影的占比越高
    float ghostAlpha = 0.3 * intensity;
    vec3 baseColor = colMain.rgb;
    // 将左右两侧的图像以透明度叠加到主图像上
    baseColor = mix(baseColor, colLeft.rgb, ghostAlpha);
    baseColor = mix(baseColor, colRight.rgb, ghostAlpha);

    // 6. 色彩修正 (饥荒风格)
    float gray = dot(baseColor, vec3(0.299, 0.587, 0.114));
    vec3 dsColor = mix(baseColor, vec3(gray), intensity * 0.9);
    dsColor *= mix(1.0, 1.1, intensity);
    dsColor *= mix(vec3(1.0), vec3(1.0, 0.8, 0.8), intensity);

    // 7. 边缘压暗
    float vignette = 1.0 - smoothstep(0.4, 1.2, dist * (1.0 + intensity * 0.5));

    fragColor = vec4(dsColor * vignette, 1.0);
}