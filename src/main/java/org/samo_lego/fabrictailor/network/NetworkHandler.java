package org.samo_lego.fabrictailor.network;

import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.samo_lego.fabrictailor.casts.TailoredPlayer;
import org.samo_lego.fabrictailor.util.TextTranslations;

import static org.samo_lego.fabrictailor.FabricTailor.THREADPOOL;
import static org.samo_lego.fabrictailor.FabricTailor.config;
import static org.samo_lego.fabrictailor.util.SkinFetcher.fetchSkinByName;

public class NetworkHandler {

    public static void onJoin(ServerGamePacketListenerImpl listener, PacketSender _sender, MinecraftServer _server) {
        var player = listener.getPlayer();
        if (ServerPlayNetworking.canSend(listener, SkinPackets.FT_HELLO)) {
            ServerPlayNetworking.send(player, SkinPackets.FT_HELLO, createHelloPacket(player.hasPermissions(2)));
        }

        THREADPOOL.submit(() -> {
            String value = ((TailoredPlayer) player).getSkinValue();
            String signature = ((TailoredPlayer) player).getSkinSignature();

            Property skinData = null;
            if (value == null) {

                if (!config.defaultSkin.applyToAll)
                    skinData = fetchSkinByName(player.getGameProfile().getName());

                if (skinData == null) {
                    value = config.defaultSkin.value;
                    signature = config.defaultSkin.signature;

                    if (!value.isEmpty() && !signature.isEmpty())
                        skinData = new Property(SkinManager.PROPERTY_TEXTURES, value, signature);
                }


            } else {
                skinData = new Property(SkinManager.PROPERTY_TEXTURES, value, signature);
            }
            // Try to set skin now
            if (skinData != null) {
                ((TailoredPlayer) player).setSkin(skinData, false);
            }
            ((TailoredPlayer) player).resetLastSkinChange();
        });
    }


    public static FriendlyByteBuf createHelloPacket(boolean allowDefaultSkinButton) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(allowDefaultSkinButton);

        return buf;
    }

    public static void changeVanillaSkinPacket(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, FriendlyByteBuf buf, PacketSender sender) {
        NetworkHandler.onSkinChangePacket(player, buf, () -> {
        });
    }

    public static void defaultSkinPacket(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, FriendlyByteBuf buf, PacketSender sender) {
        if (player.hasPermissions(2)) {
            String value = buf.readUtf();
            String signature = buf.readUtf();

            config.defaultSkin.value = value;
            config.defaultSkin.signature = signature;
            config.save();

            player.sendSystemMessage(
                    TextTranslations.create("command.fabrictailor.config.defaultSkin").withStyle(ChatFormatting.GREEN));
        }
    }

    public static void changeHDSkinPacket(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, FriendlyByteBuf buf, PacketSender sender) {
        NetworkHandler.onSkinChangePacket(player, buf, () ->
                player.displayClientMessage(TextTranslations.create("hint.fabrictailor.client_only")
                        .withStyle(ChatFormatting.DARK_PURPLE), false));
    }


    public static void onSkinChangePacket(ServerPlayer player, FriendlyByteBuf buf, Runnable callback) {
        long lastChange = ((TailoredPlayer) player).getLastSkinChange();
        long now = System.currentTimeMillis();

        if (now - lastChange > config.skinChangeTimer * 1000 || lastChange == 0) {
            ((TailoredPlayer) player).setSkin(buf.readProperty(), true);
            callback.run();
        } else {
            // Prevent skin change spamming
            MutableComponent timeLeft = Component.literal(String.valueOf((config.skinChangeTimer * 1000 - now + lastChange) / 1000))
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            player.displayClientMessage(
                    TextTranslations.create("command.fabrictailor.skin.timer.please_wait", timeLeft)
                            .withStyle(ChatFormatting.RED),
                    false
            );
        }
    }
}
