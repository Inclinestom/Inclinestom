package net.minestom.server.utils.math;

public class IntRange extends Range<Integer> {

    public IntRange(int minimum, int maximum) {
        super(minimum, maximum);
    }

    public IntRange(int value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInRange(Integer value) {
        return value >= this.getMinimum() && value <= this.getMaximum();
    }

    public boolean isInRange(int value) {
        return value >= this.getMinimum() && value <= this.getMaximum();
    }
}
