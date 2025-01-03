package org.vivecraft.client.extensions;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelPartExtension {
    /**
     * set ModelPart scale, since older Minecraft versions lack that feature
     *
     * @param x x scale
     * @param y y scale
     * @param z z scale
     */
    void vivecraft$setScale(float x, float y, float z);

    /**
     * copies the ModelPart scale of parameters {@code this} to {@code other}
     */
    void vivecraft$copyScaleTo(ModelPart other);
}
