package space.subkek.customdiscs.util;

import lombok.Setter;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class TaskScheduler {
  private final ScheduledExecutorService executor;

  public TaskScheduler() {
    this.executor = Executors.newScheduledThreadPool(1);
  }

  public TaskScheduler(int corePoolSize) {
    this.executor = Executors.newScheduledThreadPool(corePoolSize);
  }

  @FunctionalInterface
  public interface Logger {
    void error(String message, Throwable throwable);
  }

  @Setter
  private Logger logger = (message, throwable) -> {
    System.err.println(message);
    if (throwable != null) throwable.printStackTrace(System.err);
  };

  public Task runNow(TaskRunnable task) {
    Task handle = new Task();

    handle.future = executor.submit(() -> {
      try {
        task.run(handle);
      } catch (Throwable e) {
        logger.error("Task threw an exception: ", e);
      }
    });

    return handle;
  }

  public Task runDelayed(TaskRunnable task, long delay, TimeUnit timeUnit) {
    Task handle = new Task();

    handle.future = executor.schedule(() -> {
      try {
        if (handle.cancelIfCancelled()) return;
        task.run(handle);
      } catch (Throwable e) {
        logger.error("Delayed task threw an exception: ", e);
      }
    }, delay, timeUnit);

    return handle;
  }

  public Task runAtFixedRate(TaskRunnable task, long delay, long period, TimeUnit timeUnit) {
    Task handle = new Task();

    handle.future = executor.scheduleAtFixedRate(() -> {
      try {
        if (handle.cancelIfCancelled()) return;
        task.run(handle);
      } catch (Throwable e) {
        logger.error("Fixed rate task threw an exception: ", e);
      }
    }, delay, period, timeUnit);

    return handle;
  }

  public void shutdown() {
    executor.shutdownNow();
  }

  @FunctionalInterface
  public interface TaskRunnable {
    void run(Task task);
  }

  public static class Task {
    private volatile boolean cancelled = false;
    private Future<?> future;

    public void cancel() {
      this.cancelled = true;
      future.cancel(false);
    }

    private boolean cancelIfCancelled() {
      if (!cancelled) return false;

      future.cancel(true);
      return true;
    }
  }
}
