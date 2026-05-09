package org.orsa.octorace.interfaces;

import net.minecraft.world.level.block.Block;
import org.orsa.octorace.factory.BlockFactory;

public interface OrsaBlock<T extends Block> {
    void init(BlockFactory<T> factory);
}
