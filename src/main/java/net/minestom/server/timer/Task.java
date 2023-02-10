package net.minestom.server.timer;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.function.Supplier;

public sealed interface Task permits TaskImpl {
    int id();

    ExecutionType executionType();

    Scheduler owner();

    /**
     * Unpark the tasks to be executed during next processing.
     */
    void unpark();

    boolean isParked();

    void cancel();

    boolean isAlive();

    final class Builder {
        private final Scheduler scheduler;
        private final Runnable runnable;
        private ExecutionType executionType = ExecutionType.SYNC;
        private TaskSchedule delay = TaskSchedule.immediate();
        private TaskSchedule repeat = TaskSchedule.stop();

        Builder(Scheduler scheduler, Runnable runnable) {
            this.scheduler = scheduler;
            this.runnable = runnable;
        }

        public Builder executionType(ExecutionType executionType) {
            this.executionType = executionType;
            return this;
        }

        public Builder delay(TaskSchedule schedule) {
            this.delay = schedule;
            return this;
        }

        public Builder repeat(TaskSchedule schedule) {
            this.repeat = schedule;
            return this;
        }

        public Task schedule() {
            var runnable = this.runnable;
            var delay = this.delay;
            var repeat = this.repeat;
            var executionType = this.executionType;
            return scheduler.submitTask(new Supplier<>() {
                boolean first = true;

                @Override
                public TaskSchedule get() {
                    if (first) {
                        first = false;
                        return delay;
                    }
                    runnable.run();
                    return repeat;
                }
            }, executionType);
        }

        public Builder delay(Duration duration) {
            return delay(TaskSchedule.duration(duration));
        }

        public Builder delay(long time, TemporalUnit unit) {
            return delay(Duration.of(time, unit));
        }

        public Builder repeat(Duration duration) {
            return repeat(TaskSchedule.duration(duration));
        }

        public Builder repeat(long time, TemporalUnit unit) {
            return repeat(Duration.of(time, unit));
        }
    }

}
