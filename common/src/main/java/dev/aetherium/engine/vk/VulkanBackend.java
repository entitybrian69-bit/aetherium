package dev.aetherium.engine.vk;

import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.RenderBackend;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan13Features;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

/**
 * Vulkan 1.3 device bring-up: instance, physical-device selection (discrete > integrated > other), logical device
 * with graphics + compute queues, dynamic rendering + synchronization2 enabled.
 * <p>
 * STATUS: experimental. Presentation to Minecraft's GLFW window and the terrain pipeline are not wired; when this
 * backend cannot start, {@link dev.aetherium.engine.AetheriumRenderEngine} falls back to OpenGL automatically.
 * This mirrors the "OpenGL default, Vulkan experimental" state of roadmap versions 26.1-26.3.
 */
public final class VulkanBackend implements RenderBackend {
    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue computeQueue;
    private int graphicsFamily = -1;
    private int computeFamily = -1;
    private String deviceName = "unknown";
    private long deviceLocalBytes;
    private boolean available;

    @Override public Kind kind() { return Kind.VULKAN; }

    public static boolean loaderPresent() {
        try {
            VK.getFunctionProvider();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override public boolean initialize(AetheriumConfig config) {
        if (!loaderPresent()) {
            Aetherium.LOGGER.warn("[Aetherium] No Vulkan loader on this system");
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo app = VkApplicationInfo.calloc(stack)
                    .sType$Default()
                    .pApplicationName(stack.UTF8("Minecraft"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8("Aetherium"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                    .apiVersion(VK_API_VERSION_1_3);
            VkInstanceCreateInfo ici = VkInstanceCreateInfo.calloc(stack).sType$Default().pApplicationInfo(app);
            PointerBuffer pInstance = stack.mallocPointer(1);
            int r = vkCreateInstance(ici, null, pInstance);
            if (r != VK_SUCCESS) {
                Aetherium.LOGGER.warn("[Aetherium] vkCreateInstance failed: {}", r);
                return false;
            }
            instance = new VkInstance(pInstance.get(0), ici);

            if (!pickPhysicalDevice(stack)) { close(); return false; }
            if (!createDevice(stack)) { close(); return false; }
        }
        available = true;
        Aetherium.LOGGER.info("[Aetherium] Vulkan 1.3 device online: {} ({} MiB device-local)", deviceName, deviceLocalBytes >> 20);
        return true;
    }

    private boolean pickPhysicalDevice(MemoryStack stack) {
        IntBuffer count = stack.mallocInt(1);
        vkEnumeratePhysicalDevices(instance, count, null);
        if (count.get(0) == 0) { Aetherium.LOGGER.warn("[Aetherium] No Vulkan physical devices"); return false; }
        PointerBuffer devices = stack.mallocPointer(count.get(0));
        vkEnumeratePhysicalDevices(instance, count, devices);

        int bestScore = -1;
        for (int i = 0; i < count.get(0); i++) {
            VkPhysicalDevice pd = new VkPhysicalDevice(devices.get(i), instance);
            VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(pd, props);
            if (VK_VERSION_MAJOR(props.apiVersion()) < 1 || (VK_VERSION_MAJOR(props.apiVersion()) == 1 && VK_VERSION_MINOR(props.apiVersion()) < 3)) continue;
            int gfx = findQueue(pd, stack, VK_QUEUE_GRAPHICS_BIT, -1);
            if (gfx < 0) continue;
            int score = switch (props.deviceType()) {
                case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> 1000;
                case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> 500;
                case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> 100;
                default -> 10;
            };
            long local = deviceLocalMemory(pd, stack);
            score += (int) Math.min(500, local >> 24);
            if (score > bestScore) {
                bestScore = score;
                physicalDevice = pd;
                deviceName = props.deviceNameString();
                deviceLocalBytes = local;
                graphicsFamily = gfx;
                int comp = findQueue(pd, stack, VK_QUEUE_COMPUTE_BIT, gfx);
                computeFamily = comp < 0 ? gfx : comp;
            }
        }
        if (physicalDevice == null) Aetherium.LOGGER.warn("[Aetherium] No Vulkan 1.3-capable device with a graphics queue");
        return physicalDevice != null;
    }

    private static int findQueue(VkPhysicalDevice pd, MemoryStack stack, int flag, int avoidFamily) {
        IntBuffer n = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(pd, n, null);
        VkQueueFamilyProperties.Buffer fams = VkQueueFamilyProperties.calloc(n.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(pd, n, fams);
        int fallback = -1;
        for (int i = 0; i < fams.capacity(); i++) {
            if ((fams.get(i).queueFlags() & flag) == 0) continue;
            if (i != avoidFamily) return i;
            fallback = i;
        }
        return fallback;
    }

    private static long deviceLocalMemory(VkPhysicalDevice pd, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties mem = VkPhysicalDeviceMemoryProperties.calloc(stack);
        vkGetPhysicalDeviceMemoryProperties(pd, mem);
        long total = 0;
        for (int i = 0; i < mem.memoryHeapCount(); i++) {
            if ((mem.memoryHeaps(i).flags() & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) total += mem.memoryHeaps(i).size();
        }
        return total;
    }

    private boolean createDevice(MemoryStack stack) {
        int familyCount = graphicsFamily == computeFamily ? 1 : 2;
        VkDeviceQueueCreateInfo.Buffer queues = VkDeviceQueueCreateInfo.calloc(familyCount, stack);
        queues.get(0).sType$Default().queueFamilyIndex(graphicsFamily).pQueuePriorities(stack.floats(1.0f));
        if (familyCount == 2) queues.get(1).sType$Default().queueFamilyIndex(computeFamily).pQueuePriorities(stack.floats(0.5f));

        VkPhysicalDeviceVulkan13Features f13 = VkPhysicalDeviceVulkan13Features.calloc(stack).sType$Default()
                .dynamicRendering(true).synchronization2(true).maintenance4(true);
        VkPhysicalDeviceFeatures2 f2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default().pNext(f13.address());
        f2.features().multiDrawIndirect(true).drawIndirectFirstInstance(true).samplerAnisotropy(true);

        VkDeviceCreateInfo dci = VkDeviceCreateInfo.calloc(stack).sType$Default().pNext(f2.address()).pQueueCreateInfos(queues);
        PointerBuffer pDevice = stack.mallocPointer(1);
        int r = vkCreateDevice(physicalDevice, dci, null, pDevice);
        if (r != VK_SUCCESS) {
            Aetherium.LOGGER.warn("[Aetherium] vkCreateDevice failed: {}", r);
            return false;
        }
        device = new VkDevice(pDevice.get(0), physicalDevice, dci, VK13.VK_API_VERSION_1_3);
        PointerBuffer pQueue = stack.mallocPointer(1);
        vkGetDeviceQueue(device, graphicsFamily, 0, pQueue);
        graphicsQueue = new VkQueue(pQueue.get(0), device);
        vkGetDeviceQueue(device, computeFamily, 0, pQueue);
        computeQueue = new VkQueue(pQueue.get(0), device);
        return true;
    }

    @Override public void beginFrame(int framebufferWidth, int framebufferHeight) { /* presentation not wired */ }
    @Override public void runOcclusionPass(int depthTextureId, int width, int height, float[] viewProjection) { /* GL depth not importable */ }
    @Override public void endFrame() {}
    @Override public void applyConfig(AetheriumConfig config) {}

    public boolean isAvailable() { return available; }
    public VkDevice device() { return device; }
    public VkQueue graphicsQueue() { return graphicsQueue; }
    public VkQueue computeQueue() { return computeQueue; }
    public String deviceName() { return deviceName; }

    @Override public String describe() { return "Vulkan 1.3 [experimental] " + deviceName; }

    @Override public void close() {
        if (device != null) { vkDeviceWaitIdle(device); vkDestroyDevice(device, null); device = null; }
        if (instance != null) { vkDestroyInstance(instance, null); instance = null; }
        available = false;
    }
}
