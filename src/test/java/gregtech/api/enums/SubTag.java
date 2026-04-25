package gregtech.api.enums;

public final class SubTag {

    public static final SubTag METAL = new SubTag("METAL");

    private final String name;

    private SubTag(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}