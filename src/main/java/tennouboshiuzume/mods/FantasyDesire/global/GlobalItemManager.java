package tennouboshiuzume.mods.FantasyDesire.global;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;

public class GlobalItemManager {

    private static final File ROOT_DIR = new File("global_items");

    static {
        if (!ROOT_DIR.exists()) {
            ROOT_DIR.mkdirs();
        }
    }

    // 保存物品（覆盖玩家当前存储）
    public static void saveItemForPlayer(String playerName, ItemStack stack) {
        File file = new File(ROOT_DIR, playerName + ".nbt");

        CompoundTag root = new CompoundTag();
        CompoundTag itemTag = new CompoundTag();
        stack.save(itemTag);

        root.put("item", itemTag);

        try {
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载物品（加载后会自动删除文件，确保“唯一性”）
    public static ItemStack loadAndClearItemForPlayer(String playerName) {
        File file = new File(ROOT_DIR, playerName + ".nbt");
        if (!file.exists()) return ItemStack.EMPTY;

        try {
            CompoundTag root = NbtIo.readCompressed(file);
            if (root.contains("item")) {
                CompoundTag itemTag = root.getCompound("item");
                ItemStack stack = ItemStack.of(itemTag);

                // 删除文件，确保只可读取一次
                file.delete();

                return stack;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ItemStack.EMPTY;
    }

    // 是否已有物品保存
    public static boolean hasItemForPlayer(String playerName) {
        File file = new File(ROOT_DIR, playerName + ".nbt");
        return file.exists();
    }
}
