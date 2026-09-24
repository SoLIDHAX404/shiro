#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D SceneSampler;
uniform sampler2D SceneDepthSampler;

layout(std140) uniform MotionBlurConfig {
    mat4 InverseProjection;
    mat4 InverseView;
    mat4 PreviousView;
    mat4 PreviousProjection;
    vec3 CameraOffset;
    float Strength;
    vec2 ScreenSize;
    int MaxSamples;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float FAR_DEPTH = 0.0;
const float MAX_BLUR = 0.4;
const float PIXELS_PER_SAMPLE = 2.0;

vec2 previousCoord(vec2 coord, float depth) {
    vec4 view = InverseProjection * vec4(coord * 2.0 - 1.0, depth, 1.0);
    vec3 world = (InverseView * vec4(view.xyz / view.w, 1.0)).xyz + CameraOffset;
    vec4 clip = PreviousProjection * PreviousView * vec4(world, 1.0);
    if (clip.w <= 0.0) return coord;
    return clip.xy / clip.w * 0.5 + 0.5;
}

float nearestDepth(ivec2 texel) {
    float depth = texelFetch(SceneDepthSampler, texel, 0).r;
    depth = max(depth, texelFetch(SceneDepthSampler, texel + ivec2(1, 0), 0).r);
    depth = max(depth, texelFetch(SceneDepthSampler, texel + ivec2(-1, 0), 0).r);
    depth = max(depth, texelFetch(SceneDepthSampler, texel + ivec2(0, 1), 0).r);
    return max(depth, texelFetch(SceneDepthSampler, texel + ivec2(0, -1), 0).r);
}

float jitter(vec2 position) {
    return fract(52.9829189 * fract(dot(position, vec2(0.06711056, 0.00583715))));
}

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);

    vec2 motion = texCoord - previousCoord(texCoord, nearestDepth(texel));
    vec2 turning = texCoord - previousCoord(texCoord, FAR_DEPTH);
    float turningSq = dot(turning, turning);
    vec2 velocity = turningSq > 1e-12 ? turning * (clamp(dot(motion, turning), 0.0, turningSq) / turningSq) : vec2(0.0);
    velocity *= Strength;

    float speed = length(velocity);
    if (isnan(speed) || speed * max(ScreenSize.x, ScreenSize.y) < 0.5) {
        fragColor = texelFetch(SceneSampler, texel, 0);
        return;
    }
    if (speed > MAX_BLUR) velocity *= MAX_BLUR / speed;

    int samples = clamp(int(ceil(length(velocity * ScreenSize) / PIXELS_PER_SAMPLE)), 4, MaxSamples);
    float offset = jitter(gl_FragCoord.xy) - 0.5;
    vec3 total = vec3(0.0);
    for (int i = 0; i < samples; i++) {
        vec3 color = texture(SceneSampler, texCoord + velocity * ((float(i) + 0.5 + offset) / float(samples) - 0.5)).rgb;
        total += color * color;
    }
    fragColor = vec4(sqrt(total / float(samples)), 1.0);
}
