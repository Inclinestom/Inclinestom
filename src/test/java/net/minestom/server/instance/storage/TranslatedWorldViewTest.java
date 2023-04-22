package net.minestom.server.instance.storage;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TranslatedWorldViewTest {
    @Test
    public void properties() {
        WorldView.Mutable section = WorldView.section();
        WorldView.Mutable mutable = WorldView.mutable();

        WorldView translatedSection = WorldView.translate(section, new Vec(8, 8, 8));
        WorldView translatedMutable = WorldView.translate(mutable, new Vec(8, 8, 8));

        section.setBlock(0, 0, 0, Block.STONE);
        mutable.setBlock(0, 0, 0, Block.STONE);

        assertEquals(translatedSection.getBlock(8, 8, 8), Block.STONE, "Translated section block is not " + Block.STONE);
        assertEquals(translatedMutable.getBlock(8, 8, 8), Block.STONE, "Translated mutable block is not " + Block.STONE);
    }
}
