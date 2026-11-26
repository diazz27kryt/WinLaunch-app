#version 330

#moj_import <minecraft:dynamictransforms.glsl>

// Win Visuals night sky. One opaque pass: dark gradient, nebulae, stars, horizon haze and a
// procedural animated aurora. Parameters travel in the standard dynamic-transform uniforms:
//   ColorModulator = (time seconds, aurora intensity, nebula intensity, stars intensity)
//   ModelOffset    = (haze amount, star rotation angle, sky brightness)

in vec3 skyDir;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash31(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise3(p);
        p = p * 2.03 + vec3(11.7, 5.3, 8.1);
        a *= 0.5;
    }
    return v;
}

vec3 rotateX(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
}

// Twinkling procedural stars on a 3D cell grid.
float stars(vec3 dir, float t) {
    float total = 0.0;
    for (int layer = 0; layer < 2; layer++) {
        float scale = layer == 0 ? 70.0 : 150.0;
        vec3 p = dir * scale;
        vec3 cell = floor(p);
        vec3 rnd = hash33(cell + float(layer) * 17.0);
        float present = layer == 0 ? 0.80 : 0.86;
        if (rnd.x > present) {
            vec3 pos = vec3(0.5) + (hash33(cell + 3.7) - 0.5) * 0.7;
            float d = length(fract(p) - pos);
            float size = layer == 0 ? 0.30 : 0.22;
            float star = smoothstep(size, 0.0, d);
            float twinkle = 0.65 + 0.35 * sin(t * (1.5 + rnd.y * 3.5) + rnd.z * 40.0);
            float bright = layer == 0 ? 0.45 + 0.9 * rnd.y * rnd.y : 0.25 + 0.5 * rnd.y;
            total += star * star * twinkle * bright;
        }
    }
    return total;
}

vec3 starTint(vec3 dir) {
    float h = hash31(floor(dir * 70.0));
    return mix(vec3(0.65, 0.78, 1.0), vec3(1.0, 0.92, 0.75), h);
}

// Layered curtain aurora. y is the sine of the elevation, u the azimuth as a unit vector
// (so there is no seam when the angle wraps around).
vec3 aurora(vec3 dir, float t) {
    float y = dir.y;
    if (y <= 0.0) return vec3(0.0);
    vec2 u = normalize(dir.xz + vec2(1e-4));
    vec3 col = vec3(0.0);

    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float seed = fi * 7.31;

        // Slowly breathing, wandering base height of the curtain.
        float wander = 0.07 * sin(u.x * 2.3 + u.y * 1.7 + t * 0.21 + fi * 1.9)
                     + 0.06 * noise3(vec3(u * 3.0, t * 0.07 + seed));
        float base = 0.16 + 0.13 * fi + wander;
        float d = y - base;
        if (d < -0.02) continue;

        // Bright sharp lower edge that fades out upwards.
        float edge = smoothstep(-0.015, 0.03, d);
        float fade = exp(-max(d, 0.0) * (4.8 - fi * 0.9));

        // Vertical rays: stretched noise that drifts sideways with time.
        float rays = noise3(vec3(u * (9.0 + fi * 3.0), t * 0.35 + seed));
        rays = 0.35 + 0.95 * rays * rays + 0.35 * noise3(vec3(u * 26.0, t * 0.8 + seed));
        // Patchy coverage so the curtains do not wrap the whole sky evenly.
        float coverage = smoothstep(0.30, 0.68, noise3(vec3(u * 1.7, t * 0.045 + seed * 1.3)));

        float strength = edge * fade * rays * coverage;
        // Colour: green at the base, cyan in the middle, violet / magenta at the top.
        float h = clamp(d * 3.2, 0.0, 1.0);
        vec3 c = mix(vec3(0.10, 1.00, 0.45), vec3(0.15, 0.85, 0.95), h);
        c = mix(c, vec3(0.65, 0.25, 0.95), smoothstep(0.35, 0.95, h) * 0.85);
        col += c * strength;
    }
    // Fade out near the horizon so it hangs above the terrain.
    col *= smoothstep(0.0, 0.12, y);
    return col;
}

void main() {
    vec3 dir = normalize(skyDir);
    float t = ColorModulator.x;
    float auroraAmt = ColorModulator.y;
    float nebulaAmt = ColorModulator.z;
    float starAmt = ColorModulator.w;
    float haze = ModelOffset.x;
    float starAngle = ModelOffset.y;
    float bright = ModelOffset.z;

    float y = dir.y;
    // Very dark blue-black gradient.
    float horizon = pow(1.0 - clamp(abs(y), 0.0, 1.0), 3.0);
    vec3 col = mix(vec3(0.004, 0.006, 0.018), vec3(0.022, 0.030, 0.070), horizon) * bright;
    if (y < 0.0) {
        col = mix(col, vec3(0.006, 0.008, 0.016), clamp(-y * 3.0, 0.0, 1.0));
    }

    vec3 sdir = rotateX(dir, starAngle);

    if (nebulaAmt > 0.0 && y > -0.1) {
        float n = fbm3(sdir * 2.6 + vec3(t * 0.004, 0.0, 0.0));
        float n2 = fbm3(sdir * 4.2 + 31.7);
        float cloud = smoothstep(0.46, 0.92, n);
        vec3 neb = mix(vec3(0.20, 0.06, 0.42), vec3(0.04, 0.22, 0.55), n2);
        neb += vec3(0.35, 0.10, 0.30) * smoothstep(0.62, 0.95, n2) * cloud;
        col += neb * cloud * 0.42 * nebulaAmt * smoothstep(-0.1, 0.25, y);
    }

    if (starAmt > 0.0 && y > -0.05) {
        float s = stars(sdir, t);
        col += starTint(sdir) * s * starAmt * smoothstep(-0.05, 0.08, y);
    }

    if (auroraAmt > 0.0) {
        col += aurora(dir, t) * auroraAmt * 0.9;
    }

    if (haze > 0.0) {
        col += vec3(0.10, 0.13, 0.24) * exp(-max(y, 0.0) * 8.0) * haze * 0.55;
    }

    // Soft tone mapping keeps bright aurora from clipping harshly.
    col = 1.0 - exp(-col * 1.5);
    fragColor = vec4(col, 1.0);
}
