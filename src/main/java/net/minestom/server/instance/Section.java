package net.minestom.server.instance;

import net.minestom.server.instance.palette.Palette;
import org.jetbrains.annotations.NotNull;

public final class Section {
    private Palette blockPalette;
    private Palette biomePalette;
    private byte[] skyLight;
    private byte[] blockLight;

    private Section(Palette blockPalette, Palette biomePalette,
                    byte[] skyLight, byte[] blockLight) {
        this.blockPalette = blockPalette;
        this.biomePalette = biomePalette;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
    }

    public Section() {
        this(Palette.blocks(), Palette.biomes(),
                new byte[0], new byte[0]);
    }

    public Palette blockPalette() {
        return blockPalette;
    }

    public Palette biomePalette() {
        return biomePalette;
    }

    public byte[] getSkyLight() {
        return skyLight;
    }

    public void setSkyLight(byte[] skyLight) {
        this.skyLight = skyLight;
    }

    public byte[] getBlockLight() {
        return blockLight;
    }

    public void setBlockLight(byte[] blockLight) {
        this.blockLight = blockLight;
    }

    public void clear() {
        this.blockPalette = Palette.blocks();
        this.biomePalette = Palette.biomes();
        this.skyLight = new byte[0];
        this.blockLight = new byte[0];
    }

    @Override
    public @NotNull Section clone() {
        return new Section(blockPalette.clone(), biomePalette.clone(),
                skyLight.clone(), blockLight.clone());
    }
}
