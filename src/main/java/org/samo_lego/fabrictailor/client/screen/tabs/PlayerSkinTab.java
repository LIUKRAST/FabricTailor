package org.samo_lego.fabrictailor.client.screen.tabs;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.samo_lego.fabrictailor.network.SkinPackets;
import org.samo_lego.fabrictailor.util.SkinFetcher;
import org.samo_lego.fabrictailor.util.TranslatedText;

import java.util.Optional;

public class PlayerSkinTab extends GuiComponent implements SkinTabType {

    private final TranslatedText TITLE;
    private final TranslatedText DESCRIPTION;
    private final ItemStack ICON;

    public PlayerSkinTab() {
        this.ICON = new ItemStack(Items.PLAYER_HEAD);
        this.DESCRIPTION = new TranslatedText("description.fabrictailor.title_player");
        this.TITLE = new TranslatedText("tab.fabrictailor.title_player");
    }

    @Override
    public TranslatedText getTitle() {
        return this.TITLE;
    }

    @Override
    public TranslatedText getDescription() {
        return this.DESCRIPTION;
    }

    @Override
    public ItemStack getIcon() {
        return this.ICON;
    }

    @Override
    public boolean hasSkinModels() {
        return false;
    }

    @Override
    public Optional<FriendlyByteBuf> getSkinChangePacket(String playername, boolean _ignored) {
        Property skinData = SkinFetcher.fetchSkinByName(playername);

        if (skinData == null)
            return Optional.empty();

        return Optional.of(SkinPackets.skin2ByteBuf(skinData));
    }
}
