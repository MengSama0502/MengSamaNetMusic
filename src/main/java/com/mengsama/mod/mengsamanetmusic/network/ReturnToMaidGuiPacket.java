package com.mengsama.mod.mengsamanetmusic.network;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mengsama.mod.mengsamanetmusic.compat.EntityMusicDevice;
import com.mengsama.mod.mengsamanetmusic.compat.TouhouLittleMaidExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 从女仆绑定的播放器菜单安全返回同一女仆的原生 TLM GUI。 */
public record ReturnToMaidGuiPacket(UUID maidId, int entityId, UUID instanceId) {
    public static void encode(ReturnToMaidGuiPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.maidId);
        buf.writeInt(packet.entityId);
        buf.writeUUID(packet.instanceId);
    }

    public static ReturnToMaidGuiPacket decode(FriendlyByteBuf buf) {
        return new ReturnToMaidGuiPacket(buf.readUUID(), buf.readInt(), buf.readUUID());
    }

    public static void handle(ReturnToMaidGuiPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> {
                var entity = sender.serverLevel().getEntity(packet.entityId);
                if (!(entity instanceof EntityMaid maid)
                        || !maid.getUUID().equals(packet.maidId)
                        || !maid.isOwnedBy(sender)
                        || !maid.isAlive()
                        || maid.isSleeping()
                        || sender.distanceToSqr(maid) >= 25.0
                        || maid.getTask() == null
                        || !TouhouLittleMaidExtension.MUSIC_TASK_UID.equals(maid.getTask().getUid())
                        || EntityMusicDevice.resolve(sender, packet.maidId, packet.entityId, packet.instanceId).isEmpty()) {
                    return;
                }
                maid.openMaidGui(sender, 0);
            });
        }
        context.setPacketHandled(true);
    }
}
