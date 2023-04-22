package net.minestom.server;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.advancements.AdvancementManager;
import net.minestom.server.adventure.bossbar.BossBarManager;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.exception.ExceptionManager;
import net.minestom.server.extensions.ExtensionManager;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.listener.manager.PacketListenerManager;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.PacketProcessor;
import net.minestom.server.network.socket.Server;
import net.minestom.server.recipe.RecipeManager;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.snapshot.*;
import net.minestom.server.terminal.MinestomTerminal;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.utils.PacketUtils;
import net.minestom.server.utils.async.AsyncUtils;
import net.minestom.server.utils.collection.MappedCollection;
import net.minestom.server.world.DimensionTypeManager;
import net.minestom.server.world.biomes.BiomeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ServerProcessImpl implements ServerProcess {
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerProcessImpl.class);

    private final ExceptionManager exception;
    private final ExtensionManager extension;
    private final ConnectionManager connection;
    private final PacketProcessor packetProcessor;
    private final PacketListenerManager packetListener;
    private final InstanceManager instance;
    private final BlockManager block;
    private final CommandManager command;
    private final RecipeManager recipe;
    private final TeamManager team;
    private final GlobalEventHandler eventHandler;
    private final SchedulerManager scheduler;
    private final BenchmarkManager benchmark;
    private final DimensionTypeManager dimension;
    private final BiomeManager biome;
    private final AdvancementManager advancement;
    private final BossBarManager bossBar;
    private final TagManager tag;
    private final Server server;

    private final Ticker ticker;

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    public ServerProcessImpl() throws IOException {
        this.exception = new ExceptionManager();
        this.extension = new ExtensionManager(this);
        this.connection = new ConnectionManager();
        this.packetProcessor = new PacketProcessor();
        this.packetListener = new PacketListenerManager(this);
        this.instance = new InstanceManager();
        this.block = new BlockManager();
        this.command = new CommandManager();
        this.recipe = new RecipeManager();
        this.team = new TeamManager();
        this.eventHandler = new GlobalEventHandler();
        this.scheduler = new SchedulerManager();
        this.benchmark = new BenchmarkManager();
        this.dimension = new DimensionTypeManager();
        this.biome = new BiomeManager();
        this.advancement = new AdvancementManager();
        this.bossBar = new BossBarManager();
        this.tag = new TagManager();
        this.server = new Server(packetProcessor);

        this.ticker = new TickerImpl();
    }

    @Override
    public ConnectionManager connection() {
        return connection;
    }

    @Override
    public InstanceManager instance() {
        return instance;
    }

    @Override
    public BlockManager block() {
        return block;
    }

    @Override
    public CommandManager command() {
        return command;
    }

    @Override
    public RecipeManager recipe() {
        return recipe;
    }

    @Override
    public TeamManager team() {
        return team;
    }

    @Override
    public GlobalEventHandler eventHandler() {
        return eventHandler;
    }

    @Override
    public SchedulerManager scheduler() {
        return scheduler;
    }

    @Override
    public BenchmarkManager benchmark() {
        return benchmark;
    }

    @Override
    public DimensionTypeManager dimension() {
        return dimension;
    }

    @Override
    public BiomeManager biome() {
        return biome;
    }

    @Override
    public AdvancementManager advancement() {
        return advancement;
    }

    @Override
    public BossBarManager bossBar() {
        return bossBar;
    }

    @Override
    public ExtensionManager extension() {
        return extension;
    }

    @Override
    public TagManager tag() {
        return tag;
    }

    @Override
    public ExceptionManager exception() {
        return exception;
    }

    @Override
    public PacketListenerManager packetListener() {
        return packetListener;
    }

    @Override
    public PacketProcessor packetProcessor() {
        return packetProcessor;
    }

    @Override
    public Server server() {
        return server;
    }

    @Override
    public Ticker ticker() {
        return ticker;
    }

    @Override
    public void start(SocketAddress socketAddress) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }

        extension.start();
        extension.gotoPreInit();

        LOGGER.info("Starting " + MinecraftServer.getBrandName() + " server.");

        extension.gotoInit();

        // Init server
        try {
            server.init(socketAddress);
        } catch (IOException e) {
            exception.handleException(e);
            throw new RuntimeException(e);
        }

        // Start server
        server.start();

        extension.gotoPostInit();

        LOGGER.info(MinecraftServer.getBrandName() + " server started successfully.");

        if (MinecraftServer.isTerminalEnabled()) {
            MinestomTerminal.start();
        }
        // Stop the server on SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public void stop() {
        if (!stopped.compareAndSet(false, true))
            return;
        LOGGER.info("Stopping " + MinecraftServer.getBrandName() + " server.");
        LOGGER.info("Unloading all extensions.");
        extension.shutdown();
        scheduler.shutdown();
        connection.shutdown();
        server.stop();
        LOGGER.info("Shutting down all thread pools.");
        benchmark.disable();
        MinestomTerminal.stop();
        LOGGER.info(MinecraftServer.getBrandName() + " server stopped successfully.");
    }

    @Override
    public boolean isAlive() {
        return started.get() && !stopped.get();
    }

    @Override
    public ServerSnapshot updateSnapshot(SnapshotUpdater updater) {
        List<AtomicReference<InstanceSnapshot>> instanceRefs = new ArrayList<>();
        Int2ObjectOpenHashMap<AtomicReference<EntitySnapshot>> entityRefs = new Int2ObjectOpenHashMap<>();
        for (Instance instance : instance.getInstances()) {
            instanceRefs.add(updater.reference(instance));
            for (Entity entity : instance.entities()) {
                entityRefs.put(entity.getEntityId(), updater.reference(entity));
            }
        }
        return new SnapshotImpl.Server(MappedCollection.plainReferences(instanceRefs), entityRefs);
    }

    private final class TickerImpl implements Ticker {
        @Override
        public CompletableFuture<Void> tick(long nanoTime) {
            final long msTime = System.currentTimeMillis();

            scheduler().processTick();

            // Waiting players update (newly connected clients waiting to get into the server)
            connection().updateWaitingPlayers();

            // Keep Alive Handling
            connection().handleKeepAlive(msTime);

            // Server tick (chunks/entities)
            serverTick(msTime);

            // Flush all waiting packets
            PacketUtils.flush();

            return AsyncUtils.VOID_FUTURE;
        }

        private void serverTick(long tickStart) {
            // Tick all instances
            Collection<Instance> instances = instance().getInstances();
            CompletableFuture<?>[] futures = new CompletableFuture[instances.size()];
            int i = 0;
            for (Instance instance : instances) {
                futures[i++] = instance.tick(tickStart);
            }
            try {
                CompletableFuture.allOf(futures).get();
            } catch (InterruptedException | ExecutionException e) {
                exception().handleException(e);
            }
        }
    }
}
