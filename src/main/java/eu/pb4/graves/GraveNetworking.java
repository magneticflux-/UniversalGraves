package eu.pb4.graves;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import eu.pb4.polymer.api.networking.PolymerPacketUtils;
import eu.pb4.polymer.api.networking.PolymerSyncUtils;
import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.text.LocalizableText;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GraveNetworking {
    public static final Identifier SERVER_HELLO = new Identifier("universal_graves", "hello");
    public static final Identifier SERVER_GRAVE = new Identifier("universal_graves", "grave");
    public static final Identifier SERVER_UI = new Identifier("universal_graves", "set_ui");

    public static boolean canReceive(@Nullable ServerPlayNetworkHandler handler) {
        return handler != null && PolymerPacketUtils.getSupportedVersion(handler, SERVER_GRAVE) != -1 && ConfigManager.getConfig().canClientSide;
    }

    public static boolean canReceiveGui(@Nullable ServerPlayNetworkHandler handler) {
        return handler != null &&  PolymerPacketUtils.getSupportedVersion(handler, SERVER_UI) == 0;
    }

    public static void sendConfig(ServerPlayNetworkHandler handler) {
        var version = PolymerPacketUtils.getSupportedVersion(handler, SERVER_HELLO);

        if (version == 0) {
            var buf = PolymerPacketUtils.buf(0);
            var config = ConfigManager.getConfig();
            buf.writeBoolean(config.canClientSide);
            buf.writeString(config.style.networkName);
            buf.writeBoolean(config.configData.playerHeadTurnIntoSkulls);
            PolymerPacketUtils.sendPacket(handler, SERVER_HELLO, buf);
        }
    }

    public static NetworkingConfig readConfig(int version, PacketByteBuf buf) {
        if (version == 0) {
            var enabled = buf.readBoolean();
            var style = buf.readString();
            var playerHeadsTurnIntoSkull = buf.readBoolean();

            return new NetworkingConfig(enabled, style, playerHeadsTurnIntoSkull);
        }

        return new NetworkingConfig(false, "", false);
    }

    public static boolean sendGrave(ServerPlayNetworkHandler handler, BlockPos blockPos, boolean locked, VisualGraveData data, Map<String, Text> placeholders, @Nullable Text[] textOverrides) {
        var version = PolymerPacketUtils.getSupportedVersion(handler, SERVER_HELLO);
        var config = ConfigManager.getConfig();

        if (version == 0 && config.canClientSide) {
            var target = (LocalizationTarget) handler.getPlayer();

            var texts = textOverrides != null ? textOverrides : locked ? config.signProtectedText : config.signText;

            var buf = PolymerPacketUtils.buf(0);
            buf.writeBlockPos(blockPos);
            buf.writeNbt(data.toNbt());
            buf.writeVarInt(texts.length);

            for (var text : texts) {
                buf.writeString(Text.Serializer.toJson(
                        LocalizableText.asLocalizedFor(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders), target)
                ));
            }

            PolymerPacketUtils.sendPacket(handler, SERVER_GRAVE, buf);
            return true;
        }

        return false;
    }

    public static void sendGraveUi(ServerPlayNetworkHandler handler) {
        var version = PolymerPacketUtils.getSupportedVersion(handler, SERVER_HELLO);
        if (version != -1) {
            PolymerPacketUtils.sendPacket(handler, SERVER_UI, PolymerPacketUtils.buf(version));
        }
    }

    public static NetworkingGrave readGrave(int version, PacketByteBuf buf) {
        if (version >= 0) {
            var pos = buf.readBlockPos();
            var data = VisualGraveData.fromNbt(buf.readNbt());
            var size = buf.readVarInt();

            var texts = new ArrayList<Text>();

            for (int i = 0; i < size; i++) {
                texts.add(Text.Serializer.fromLenientJson(buf.readString()));
            }

            return new NetworkingGrave(pos, data, texts);
        }

        return null;
    }

    public static void initialize() {
        PolymerPacketUtils.registerServerPacket(SERVER_HELLO, 0);
        PolymerPacketUtils.registerServerPacket(SERVER_GRAVE, 0);
        PolymerPacketUtils.registerServerPacket(SERVER_UI, 0);

        PolymerSyncUtils.ON_SYNC_CUSTOM.register(((handler, full) -> {
            sendConfig(handler);
        }));
    }

    public static record NetworkingConfig(boolean enabled, String style, boolean playerHeadsTurnSkull) { }
    public static record NetworkingGrave(BlockPos pos, VisualGraveData data, List<Text> displayText) { }
}
