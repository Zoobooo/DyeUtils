#version 330

// Reimplementation of the look SkyOcean's case_screen.fsh gives its dungeon chest reveal: a sharp,
// magnified circle at the centre, everything outside it blurred, and the left and right edges sinking
// into darkness so the scrolling strip has no visible ends.
//
// Written from scratch rather than ported. SkyOcean's shader is MIT, but the post effect chain that
// drives it is not, so neither is copied here.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

const float TAU = 6.28318530718;

const float ZOOM = 1.5;
const float SHARP_RADIUS = 0.2;

const float BLUR_DIRECTIONS = 32.0;
const float BLUR_STEPS = 9.0;
const float BLUR_SIZE = 8.0;

const float EDGE_FADE = 0.2;
const vec4 EDGE_COLOUR = vec4(0.0, 0.0, 0.0, 0.8);

vec4 fade(vec4 colour, vec2 coord) {
    if (coord.x >= EDGE_FADE && coord.x <= 1.0 - EDGE_FADE) {
        return colour;
    }

    float progress = (coord.x < EDGE_FADE ? coord.x : 1.0 - coord.x) / EDGE_FADE;

    return mix(EDGE_COLOUR, colour, progress);
}

void main() {
    vec2 centre = vec2(0.5, 0.5);

    // Corrected for aspect, or the sharp region is an ellipse on any window that is not square.
    float aspect = InSize.x / InSize.y;
    vec2 offset = vec2(texCoord.x - centre.x, (texCoord.y - centre.y) / aspect);

    if (length(offset) / SHARP_RADIUS <= 1.0) {
        fragColor = vec4(texture(InSampler, centre + (texCoord - centre) / ZOOM).rgb, 1.0);

        return;
    }

    vec2 radius = BLUR_SIZE / InSize;
    vec4 blurred = fade(texture(InSampler, texCoord), texCoord);
    float samples = 1.0;

    for (float direction = 0.0; direction < TAU; direction += TAU / BLUR_DIRECTIONS) {
        for (float step = 1.0 / BLUR_STEPS; step <= 1.0; step += 1.0 / BLUR_STEPS) {
            vec2 coord = texCoord + vec2(cos(direction), sin(direction)) * radius * step;

            blurred += fade(texture(InSampler, coord), coord);
            samples += 1.0;
        }
    }

    // Counted rather than assumed. SkyOcean divides 289 samples by 273 and comes out about 6% bright.
    fragColor = blurred / samples;
}
