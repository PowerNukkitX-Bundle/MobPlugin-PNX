package nukkitcoders.mobplugin.entities.animal.swimming;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class Tadpole extends Fish {

    public static final int NETWORK_ID = 133;

    public Tadpole(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public int getKillExperience() {
        return 0;
    }

    @Override
    int getBucketMeta() {
        return 13;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(6);
    }
}
