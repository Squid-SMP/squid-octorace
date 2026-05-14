package org.orsa.octorace.factory;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public interface ManufacturedItem<T extends Item> {
    void init(ItemFactory<T> factory);
}
