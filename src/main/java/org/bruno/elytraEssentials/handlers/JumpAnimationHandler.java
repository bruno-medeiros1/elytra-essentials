package org.bruno.elytraEssentials.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class JumpAnimationHandler {

    private final FoliaHelper foliaHelper;
    private final Logger logger;
    private static int nextEntityId = 200000;

    public JumpAnimationHandler(FoliaHelper foliaHelper, Logger logger) {
        this.foliaHelper = foliaHelper;
        this.logger = logger;
    }

    /**
     * Creates a visual effect where the blocks under the player jump up and fall back down.
     * This is triggered at the moment of a charged jump launch.
     * @param player The player who is launching.
     */
    public void playLaunchAnimation(Player player) {
        Location center = player.getLocation();
        int radius = 1;
        List<Block> blocksToAnimate = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = center.clone().subtract(0, 1, 0).add(x, 0, z).getBlock();
                if (block.getType().isSolid()) {
                    blocksToAnimate.add(block);
                }
            }
        }

        if (blocksToAnimate.isEmpty()) return;

        for (Block block : blocksToAnimate) {
            Location loc = block.getLocation();
            BlockData blockData = block.getBlockData();

            int entityId = generateUniqueEntityId();
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(blockData);

            WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                    entityId,
                    UUID.randomUUID(),
                    EntityTypes.FALLING_BLOCK,
                    new com.github.retrooper.packetevents.protocol.world.Location(loc.getX(), loc.getY(), loc.getZ(), 0f, 0f),
                    0f,
                    state.getGlobalId(),
                    Vector3d.zero()
            );

            Vector3d motion = randomExplosionVector(loc, center);
            spawnPacket.setVelocity(Optional.of(motion));

            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

            // remove after 1.5s
            foliaHelper.runTaskLater(player, () -> {
                WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
            }, 30L);
        }
    }

    private Vector3d randomExplosionVector(Location loc, Location center) {
        Vector direction = loc.clone().add(0.5, 0, 0.5).toVector().subtract(center.toVector()).normalize();

        double explosionStrength = 0.35;
        double upwardPop = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.3; // 0.3–0.6

        direction = direction.multiply(explosionStrength);

        return new Vector3d(direction.getX(), upwardPop, direction.getZ());
    }

    private static synchronized int generateUniqueEntityId() {
        return nextEntityId++;
    }
}
