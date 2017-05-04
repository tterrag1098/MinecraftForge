package net.minecraftforge.oredict;

import java.util.Collection;

public interface IItemAspects {
	
	public static final String ASPECT_MATERIAL = "material";
	public static final String ASPECT_SHAPE = "shape";

	boolean hasAspect(String aspect, String name);
	
	default boolean hasMaterial(String name) {
		return hasAspect(ASPECT_MATERIAL, name);
	}
	
	default boolean hasShape(String name) {
		return hasAspect(ASPECT_SHAPE, name);
	}

	Collection<String> getValues(String aspect);
	
	default Collection<String> getMaterials() {
		return getValues(ASPECT_MATERIAL);
	}
	
	default Collection<String> getShapes() {
		return getValues(ASPECT_SHAPE);
	}
	
	Collection<String> getAspects();
}
