package space.subkek.customdiscs;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public class Schedulers {
  private final CustomDiscs plugin;
  public final Async async;
  public final Global global;
  public final Region region;

  public Schedulers(CustomDiscs plugin) {
    this.plugin = plugin;

    this.async = new Async(plugin.getServer().getAsyncScheduler());
    this.global = new Global(plugin.getServer().getGlobalRegionScheduler());
    this.region = new Region(plugin.getServer().getRegionScheduler());
  }

  public class Async {
    private final AsyncScheduler original;
    private Async(AsyncScheduler original) {
      this.original = original;
    }

    public @NotNull ScheduledTask runNow(@NotNull Consumer<ScheduledTask> task) {
      return original.runNow(plugin, task);
    }

    public @NotNull ScheduledTask runAtFixedRate(@NotNull Consumer<ScheduledTask> task, long initialDelay, long period, @NotNull TimeUnit unit) {
      return original.runAtFixedRate(plugin, task, initialDelay, period, unit);
    }

    public void cancelTasks() {
      original.cancelTasks(plugin);
    }
  }

  public class Global {
    private final GlobalRegionScheduler original;
    private Global(GlobalRegionScheduler original) {
      this.original = original;
    }

    public void cancelTasks() {
      original.cancelTasks(plugin);
    }
  }

  public class Region {
    private final RegionScheduler original;
    private Region(RegionScheduler original) {
      this.original = original;
    }

    public @NotNull ScheduledTask runAtFixedRate(@NotNull Location location, @NotNull Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
      return original.runAtFixedRate(plugin, location, task, initialDelayTicks, periodTicks);
    }
  }
}
