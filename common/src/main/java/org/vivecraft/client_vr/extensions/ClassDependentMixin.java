package org.vivecraft.client_vr.extensions;

/**
 * A mixin that should only apply if the given Class is present
 * when implementing this, the Class needs a static field
 * {@code private static final String vivecraft$dependentClass = "";}
 */
public interface ClassDependentMixin {
    /*
     * needs to be in the implementing class
     */
    // @Unique
    // private static final String vivecraft$dependentClass = "";
}
