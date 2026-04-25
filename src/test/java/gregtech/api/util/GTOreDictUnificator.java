package gregtech.api.util;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.SubTag;

public final class GTOreDictUnificator {

    private GTOreDictUnificator() {}

    public static Association getAssociation(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        String itemName = String.valueOf(stack.getItem().getUnlocalizedName());
        if (itemName.contains("iron_ingot")) {
            return new Association(true, new Prefix("ingot"), new MaterialStack(new Material(true, true)));
        }
        if (itemName.contains("gold_nugget")) {
            return new Association(true, new Prefix("nugget"), new MaterialStack(new Material(true, true)));
        }
        if (itemName.contains("stone")) {
            return new Association(false, new Prefix("block"), new MaterialStack(new Material(false, false)));
        }
        return null;
    }

    public static final class Association {

        public final Prefix mPrefix;
        public final MaterialStack mMaterial;
        private final boolean valid;

        public Association(boolean valid, Prefix prefix, MaterialStack material) {
            this.valid = valid;
            this.mPrefix = prefix;
            this.mMaterial = material;
        }

        public boolean hasValidPrefixMaterialData() {
            return valid;
        }
    }

    public static final class Prefix {

        private final String name;

        public Prefix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class MaterialStack {

        public final Material mMaterial;

        public MaterialStack(Material material) {
            this.mMaterial = material;
        }
    }

    public static final class Material {

        private final boolean metalItems;
        private final boolean metalTag;

        public Material(boolean metalItems, boolean metalTag) {
            this.metalItems = metalItems;
            this.metalTag = metalTag;
        }

        public boolean hasMetalItems() {
            return metalItems;
        }

        public boolean contains(SubTag tag) {
            return metalTag && tag == SubTag.METAL;
        }
    }
}