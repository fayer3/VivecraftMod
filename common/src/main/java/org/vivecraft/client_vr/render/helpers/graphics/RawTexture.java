package org.vivecraft.client_vr.render.helpers.graphics;

public abstract class RawTexture {

    public final int width;
    public final int height;
    public final String name;
    public final Format format;

    protected RawTexture(String name, int width, int height, Format format) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.format = format;
    }

    /**
     * @return image handle, casted to a long, even if it is an int
     */
    public abstract long getHandle();

    /**
     * deletes this texture
     */
    public abstract void destroy();

    public enum Format {
        // these are the formats that are supported by steamvr
        // https://github.com/ValveSoftware/openvr/wiki/Vulkan#image-formats
        R8G8B8A8_UNORM,
        R8G8B8A8_SRGB,
        B8G8R8A8_UNORM,
        B8G8R8A8_SRGB,
        R16G16B16A16_SFLOAT(true),
        R32G32B32_SFLOAT(true),
        R32G32B32A32_SFLOAT(true);
        //A2R10G10B10_UINT_PACK32;

        public final boolean sfloat;

        Format() {
            this.sfloat = false;
        }

        Format(boolean sfloat) {
            this.sfloat = sfloat;
        }
    }
}
