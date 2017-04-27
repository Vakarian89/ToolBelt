package gigaherz.toolbelt;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import gigaherz.toolbelt.belt.ItemToolBelt;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Config
{
    private static final Set<String> blackListString = Sets.newHashSet();
    private static final Set<String> whiteListString = Sets.newHashSet();
    private static final Set<ItemStack> blackList = Sets.newHashSet();
    private static final Set<ItemStack> whiteList = Sets.newHashSet();

    public static boolean showBeltOnPlayers = true;

    public static boolean releaseToSwap = false;

    public static ConfigCategory display;
    public static ConfigCategory input;

    static void loadConfig(Configuration config)
    {
        Property bl = config.get("items", "blacklist", new String[0]);
        bl.setComment("List of items to disallow from placing in the belt.");

        Property wl = config.get("items", "whitelist", new String[0]);
        bl.setComment("List of items to force-allow placing in the belt. Takes precedence over blacklist.");

        Property releaseToSwapProperty = config.get("input", "releaseToSwap", false);
        releaseToSwapProperty.setComment("If set to TRUE, releasing the menu key (R) will activate the swap. Requires a click otherwise (default).");

        Property showBeltOnPlayersProperty = config.get("display", "showBeltOnPlayers", true);
        showBeltOnPlayersProperty.setComment("If set to FALSE, the belts and tools will NOT draw on players.");

        display = config.getCategory("display");
        display.setComment("Options for customizing the display of tools on the player");

        input = config.getCategory("input");
        input.setComment("Options for customizing the interaction with the radial menu");

        showBeltOnPlayers = showBeltOnPlayersProperty.getBoolean();

        releaseToSwap = releaseToSwapProperty.getBoolean();

        blackListString.addAll(Arrays.asList(bl.getStringList()));
        whiteListString.addAll(Arrays.asList(wl.getStringList()));
        if (!bl.wasRead() || !wl.wasRead() || !releaseToSwapProperty.wasRead() || !showBeltOnPlayersProperty.wasRead())
            config.save();
    }

    public static void postInit()
    {
        blackList.addAll(blackListString.stream().map(Config::parseItemStack).collect(Collectors.toList()));
        whiteList.addAll(blackListString.stream().map(Config::parseItemStack).collect(Collectors.toList()));
    }

    public static void refresh()
    {
        showBeltOnPlayers = display.get("showBeltOnPlayers").getBoolean();
        releaseToSwap = input.get("releaseToSwap").getBoolean();
    }

    private static final Pattern itemRegex = Pattern.compile("^(?<item>([a-zA-Z-0-9_]+:)?[a-zA-Z-0-9_]+)(?:@((?<meta>[0-9]+)|(?<any>any)))?$");

    @Nullable
    private static ItemStack parseItemStack(String itemString)
    {
        Matcher matcher = itemRegex.matcher(itemString);

        if (!matcher.matches())
        {
            ToolBelt.logger.warn("Could not parse item " + itemString);
            return null;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(matcher.group("item")));
        if (item == null)
        {
            ToolBelt.logger.warn("Could not parse item " + itemString);
            return null;
        }

        String anyString = matcher.group("meta");
        String metaString = matcher.group("meta");
        int meta = Strings.isNullOrEmpty(anyString)
                ? (Strings.isNullOrEmpty(metaString) ? 0 : Integer.parseInt(metaString))
                : OreDictionary.WILDCARD_VALUE;

        return new ItemStack(item, 1, meta);
    }

    public static boolean isItemStackAllowed(@Nullable final ItemStack stack)
    {
        if (stack == null)
            return true;

        if (whiteList.stream().anyMatch((s) -> OreDictionary.itemMatches(s, stack, false)))
            return true;

        if (blackList.stream().anyMatch((s) -> OreDictionary.itemMatches(s, stack, false)))
            return false;

        if (stack.getItem() instanceof ItemToolBelt)
            return false;

        if (stack.getMaxStackSize() != 1)
            return false;

        return true;
    }
}
