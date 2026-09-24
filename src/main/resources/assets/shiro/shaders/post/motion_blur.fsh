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
    int Samples;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float MAX_BLUR = 0.2;

// where this pixel was on screen last frame, found by rebuilding its position from depth and projecting it with last frame's camera
vec2 previousCoord(vec2 coord, float depth) {
    vec4 view = InverseProjection * vec4(coord * 2.0 - 1.0, depth, 1.0);
    vec3 world = (InverseView * vec4(view.xyz / view.w, 1.0)).xyz + CameraOffset;
    vec4 clip = PreviousProjection * PreviousView * vec4(world, 1.0);
    if (clip.w <= 0.0) return coord;
    return clip.xy / clip.w * 0.5 + 0.5;
}

float hash(vec2 position) {
    return fract(sin(dot(position, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    float depth = texture(SceneDepthSampler, texCoord).r;
    vec2 velocity = (texCoord - previousCoord(texCoord, depth)) * Strength;
    float speed = length(velocity);
    if (isnan(speed) || isinf(speed)) velocity = vec2(0.0);
    else if (speed > MAX_BLUR) velocity *= MAX_BLUR / speed;

    // samples spread along the motion, centered on the pixel and jittered so fewer samples don't band
    float offset = hash(gl_FragCoord.xy) - 0.5;
    vec3 total = vec3(0.0);
    for (int i = 0; i < Samples; i++) {
        float t = (float(i) + 0.5 + offset) / float(Samples) - 0.5;
        vec3 color = texture(SceneSampler, texCoord + velocity * t).rgb;
        total += color * color;
    }
    // averaged in squared space so bright edges don't dim as they smear
    fragColor = vec4(sqrt(total / float(Samples)), 1.0);
}
