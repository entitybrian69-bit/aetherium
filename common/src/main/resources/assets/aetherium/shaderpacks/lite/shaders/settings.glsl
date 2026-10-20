// Aetherium Lite: settings shared by every program (options are read from here).

// ---- Plants
#define WAVING_PLANTS // Grass, flowers and crops sway in the wind.
#define WAVING_LEAVES // Leaves and vines sway too.
#define WAVING_STRENGTH 1.00 // How far plants and leaves move. [0.50 0.75 1.00 1.50 2.00 3.00]
#define WAVE_SPEED 1.00 // Plant and water wave speed. [0.50 0.75 1.00 1.50 2.00]

// ---- Water
#define WATER_FX // Water waves, sky reflection and sun glint.
#define WATER_WAVES 1.00 // Size of the ripples on the surface. [0.00 0.50 1.00 1.50 2.00]
#define WAVING_WATER // The water surface gently rises and falls.
#define WATER_REFLECTIONS 0.75 // How much the water mirrors the sky. [0.00 0.25 0.50 0.75 1.00]
#define WATER_CLARITY 0.62 // How solid water looks from above (lower is clearer). [0.30 0.45 0.62 0.80 0.95]
#define WATER_SUN_GLINT // Sparkle of the sun on the water.
#define WATER_TINT 0 // Water color. [0 1 2]

// ---- Sky
#define SKY_FX // Brighter horizon and a soft glow around the sun.
#define HORIZON_GLOW 0.45 // Brightness of the band above the horizon. [0.00 0.25 0.45 0.70 1.00]
#define SUN_GLOW 1.00 // Size and strength of the glow around the sun. [0.00 0.50 1.00 1.50 2.00]
//#define GODRAYS // Light shafts from the sun and moon. Costs a full-screen pass.
#define GODRAY_STRENGTH 1.00 // How bright the light shafts are. [0.50 1.00 1.50 2.00]
#define CLOUD_BRIGHTNESS 1.00 // Cloud brightness. [0.70 0.85 1.00 1.10]

// ---- Lighting
#define WARMTH 1.00 // Warm sunlight with cool shadows. [0.00 0.50 1.00 1.50]
#define TORCH_WARMTH 1.00 // Orange glow of torches, lava and other lights. [0.00 0.50 1.00 1.50 2.00]
//#define SHADOWS // Real-time sun shadows on terrain. Renders the world a second time!
#define SHADOW_DARKNESS 0.55 // How dark sun shadows are (lower is darker). [0.35 0.45 0.55 0.65 0.75]

// ---- Color
#define FILMIC // Filmic tone curve: richer colors, soft highlights. Off is closer to vanilla.
#define EXPOSURE 1.00 // Overall brightness. [0.80 0.90 1.00 1.10 1.20]
#define SATURATION 1.15 // Color saturation. [0.80 0.90 1.00 1.10 1.15 1.20 1.30]
#define CONTRAST 1.00 // Contrast. [0.90 1.00 1.10 1.20]
#define BLOOM // Soft glow around bright lights.
#define BLOOM_STRENGTH 0.50 // How strong the glow is. [0.25 0.35 0.50 0.70 1.00]
#define VIGNETTE // Darkens the screen corners slightly.
#define VIGNETTE_STRENGTH 0.45 // How dark the corners get. [0.20 0.30 0.45 0.60]
