package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class OneChunk implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("onechunk");
    public static final int displace = 3;

    public static final GameRules.Key<GameRules.BooleanRule> ONECHUNK_ENABLED = GameRuleRegistry.register("oneChunkEnabled", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> ONECHUNK_CAN_EDIT_COMMON_CHUNK = GameRuleRegistry.register("oneChunkCanEditCommonChunk", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(false));

    public static Rectangle commonChunk = new Rectangle(-3248, 2880, 16, 16);

    public static Map<String, PlayerLimit> limits;

    static public double findAir(World world, BlockPos p)
    {
        int c = 0;
        for (int y = p.getY(); y < 318; y++) {
            p = p.up();
            if (!world.getBlockState(p).getMaterial().blocksMovement()) {
                c = y;
                break;
            }
        }
        for (int y = c; y > -64; y--) {
            p = p.down();
            if (world.getBlockState(p).getMaterial().blocksMovement())
                return y + 2;
        }

        LOGGER.warn("[ONECHUNK] Could not find air block for player at {}", p);
        if (world.getDimension().equals(DimensionType.THE_NETHER_ID))
        {
            return 64;
        }

        return 320;
    }

    public boolean isPosAllowed(PlayerEntity player, BlockPos blockPos) {
        if (player.isCreative())
            return true;

        MinecraftServer server = player.getServer();
        String name = player.getName().getString();
        if (limits.containsKey(name)) {
            PlayerLimit limit = limits.get(name);
            if (player.getWorld() == server.getWorld(World.NETHER))
            {
                limit = limit.getNether();
            }
            Rectangle playerChunk = new Rectangle(limit.chunkX + 1, limit.chunkZ + 1, 16, 16);
            Point p = new Point(blockPos.getX(), blockPos.getZ());
            if (!(commonChunk.contains(p) && server.getWorld(World.OVERWORLD).getGameRules().getBoolean(ONECHUNK_CAN_EDIT_COMMON_CHUNK)) && !playerChunk.contains(p))
                return false;
        }

        return true;
    }

    @Override
    public void onInitialize() {
        limits = new HashMap<>();
        limits.put("Lylycorne", new PlayerLimit(-3264, 2864));
        limits.put("BlueSansDouze", new PlayerLimit(-3248, 2864));
        limits.put("Rolphoe", new PlayerLimit(-3232, 2864));
        limits.put("LadaZ", new PlayerLimit(-3264, 2896));
        limits.put("TheInfraBlue", new PlayerLimit(-3264, 2880));
        Rectangle netherCommonChunk = new Rectangle(-400, 352, 16, 16);
        limits.put("Yetimiette", new PlayerLimit(-3232, 2880));
        limits.put("Nojash", new PlayerLimit(-3248, 2896));
        limits.put("MonsieurYam", new PlayerLimit(-3232, 2896));

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (!world.getGameRules().getBoolean(ONECHUNK_ENABLED))
                return ActionResult.PASS;

            if (isPosAllowed(playerEntity, blockPos))
                return ActionResult.PASS;

            return ActionResult.FAIL;
        });
        ServerTickEvents.START_SERVER_TICK.register((server -> {
            if (!server.getWorld(World.OVERWORLD).getGameRules().getBoolean(ONECHUNK_ENABLED))
                return;

            int tick = server.getTicks();
            if (tick % 5 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (player.isSpectator() || player.isCreative())
                        continue;

                    String name = player.getName().getString();
                    Rectangle currentCommonChunk = commonChunk;
                    if (limits.containsKey(name)) {
                        PlayerLimit limit = limits.get(name);
                        Vec3d pos = player.getPos();
                        if (player.getWorld() == server.getWorld(World.NETHER))
                        {
                            currentCommonChunk = netherCommonChunk;
                            limit = limit.getNether();
                        }
                        Point p = new Point((int) pos.x, (int) pos.z);
                        if (currentCommonChunk.contains(p))
                            continue;

                        int tpToX = 0;
                        int tpToZ = 0;

                        if (pos.x < limit.chunkX) {
                            tpToX = (int) (limit.chunkX + displace);
                        } else if (pos.x > limit.chunkMaxX) {
                            tpToX = (int) (limit.chunkMaxX - displace);
                        }

                        if (pos.z < limit.chunkZ) {
                            tpToZ = (int) (limit.chunkZ + displace);
                        } else if (pos.z > limit.chunkMaxZ) {
                            tpToZ = (int) (limit.chunkMaxZ - displace);
                        }

                        if (tpToX != 0 || tpToZ != 0) {
                            LOGGER.info("[ONECHUNK] Player outside chunk {}", limit);
                            if (tpToX == 0) tpToX = (int) pos.x;
                            if (tpToZ == 0) tpToZ = (int) pos.z;
                            BlockPos block = new BlockPos(tpToX, (int) pos.y, tpToZ);
                            double y = findAir(player.getWorld(), block);
                            LOGGER.info("[ONECHUNK] tp player {} to {} {} {}", player, tpToX, y, tpToZ);
                            player.teleport(tpToX, y, tpToZ);
                            player.sendMessage(Text.of("STAY IN YOUR CHUNK OMG!!!1"), true);
                        }
                    }
                }
            }
        }));

        LOGGER.info("Initialized OneChunk mod");
    }
}
