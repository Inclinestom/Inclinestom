package net.minestom.server.timer;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents a scheduler that will execute tasks with a precision based on its ticking rate.
 * If precision is important, consider using a JDK executor service or any third party library.
 * <p>
 * Tasks are by default executed in the caller thread.
 */
public sealed interface Scheduler permits SchedulerImpl, SchedulerManager {
    static Scheduler newScheduler() {
        return new SchedulerImpl();
    }

    /**
     * Process scheduled tasks based on time to increase scheduling precision.
     * <p>
     * This method is not thread-safe.
     */
    void process();

    /**
     * Advance 1 tick and call {@link #process()}.
     * <p>
     * This method is not thread-safe.
     */
    void processTick();

    /**
     * Submits a new task with custom scheduling logic.
     * <p>
     * This is the primitive method used by all scheduling shortcuts,
     * {@code task} is immediately executed in the caller thread to retrieve its scheduling state
     * and the task will stay alive as long as {@link TaskSchedule#stop()} is not returned (or {@link Task#cancel()} is called).
     *
     * @param task          the task to be directly executed in the caller thread
     * @param executionType the execution type
     * @return the created task
     */
    Task submitTask(Supplier<TaskSchedule> task, ExecutionType executionType);

    default Task submitTask(Supplier<TaskSchedule> task) {
        return submitTask(task, ExecutionType.SYNC);
    }

    default Task.Builder buildTask(Runnable task) {
        return new Task.Builder(this, task);
    }

    default Task scheduleTask(Runnable task,
                                       TaskSchedule delay, TaskSchedule repeat,
                                       ExecutionType executionType) {
        return buildTask(task).delay(delay).repeat(repeat).executionType(executionType).schedule();
    }

    default Task scheduleTask(Runnable task, TaskSchedule delay, TaskSchedule repeat) {
        return scheduleTask(task, delay, repeat, ExecutionType.SYNC);
    }

    default Task scheduleNextTick(Runnable task, ExecutionType executionType) {
        return buildTask(task).delay(TaskSchedule.nextTick()).executionType(executionType).schedule();
    }

    default Task scheduleNextTick(Runnable task) {
        return scheduleNextTick(task, ExecutionType.SYNC);
    }

    default Task scheduleNextProcess(Runnable task, ExecutionType executionType) {
        return buildTask(task).delay(TaskSchedule.immediate()).executionType(executionType).schedule();
    }

    default Task scheduleNextProcess(Runnable task) {
        return scheduleNextProcess(task, ExecutionType.SYNC);
    }
}
