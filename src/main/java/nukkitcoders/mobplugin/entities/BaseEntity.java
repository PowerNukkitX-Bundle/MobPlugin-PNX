package nukkitcoders.mobplugin.entities;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityAgeable;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.MobPlugin;
import nukkitcoders.mobplugin.entities.monster.Monster;
import nukkitcoders.mobplugin.utils.Utils;

public abstract class BaseEntity extends EntityCreature implements EntityAgeable {

    protected int stayTime = 0;

    protected int moveTime = 0;

    public double moveMultifier = 1.0d;

    protected Vector3 target = null;

    protected Entity followTarget = null;

    protected boolean baby = false;

    private boolean movement = true;

    private boolean friendly = false;
    
    private int despawnTicks;

    private int maxJumpHeight = 1;

    protected int attackDelay = 0;

    public Item[] armor;

    public BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.setHealth(this.getMaxHealth());

        this.despawnTicks = MobPlugin.getInstance().pluginConfig.getInt("entities.despawn-ticks", 8000);
    }

    public abstract Vector3 updateMove(int tickDiff);

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public boolean isMovement() {
        return this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public void setMovement(boolean value) {
        this.movement = value;
    }

    public double getSpeed() {
        if (this.isBaby()) {
            return 1.2;
        }
        return 1;
    }

    public int getMaxJumpHeight() {
        return this.maxJumpHeight;
    }

    public Vector3 getTarget() {
        return this.target;
    }

    public void setTarget(Vector3 vec) {
        this.target = vec;
    }

    public Entity getFollowTarget() {
        return this.followTarget != null ? this.followTarget : (this.target instanceof Entity ? (Entity) this.target : null);
    }

    public void setFollowTarget(Entity target) {
        this.followTarget = target;

        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    @Override
    public boolean isBaby() {
        return this.baby;
    }

    public void setBaby(boolean baby) {
        this.baby = baby;
        this.setDataFlag(DATA_FLAGS, DATA_FLAG_BABY, baby);
        this.setScale((float) 0.5);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag.contains("Movement")) {
            this.setMovement(this.namedTag.getBoolean("Movement"));
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }

        if (this.namedTag.getBoolean("Baby")) {
            this.setBaby(true);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putBoolean("Baby", this.isBaby());
        this.namedTag.putBoolean("Movement", this.isMovement());
        this.namedTag.putShort("Age", this.age);
    }

    public boolean targetOption(EntityCreature creature, double distance) {
        if (this instanceof Monster) {
            if (creature instanceof Player) {
                Player player = (Player) creature;
                return (!player.closed) && player.spawned && player.isAlive() && player.isSurvival() && distance <= 80;
            }
            return creature.isAlive() && (!creature.closed) && distance <= 81;
        }
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        super.entityBaseTick(tickDiff);

        if (this.age > this.despawnTicks && !this.hasCustomName() && !(this instanceof Boss)) {
            this.close();
        }

        if (this instanceof Monster && this.attackDelay < 400) {
            this.attackDelay++;
        }

        return true;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
            return false;
        }

        if (this.fireProof && (source.getCause() == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || source.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            return false;
        }

        if (source instanceof EntityDamageByEntityEvent) {
            ((EntityDamageByEntityEvent) source).setKnockBack(0.25f);
        }

        super.attack(source);

        this.target = null;
        return true;
    }

    public int getMaxFallHeight() {
        if (!(this.target instanceof Entity)) {
            return 3;
        } else {
            int i = (int) (this.getHealth() - this.getMaxHealth() * 0.33F);
            i = i - (3 - this.getServer().getDifficulty()) * 4;

            if (i < 0) {
                i = 0;
            }

            return i + 3;
        }
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        double movX = dx * moveMultifier;
        double movY = dy;
        double movZ = dz * moveMultifier;

        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(dx, dy, dz) : this.boundingBox.addCoord(dx, dy, dz));
        for (AxisAlignedBB bb : list) {
            dx = bb.calculateXOffset(this.boundingBox, dx);
        }
        this.boundingBox.offset(dx, 0, 0);

        for (AxisAlignedBB bb : list) {
            dz = bb.calculateZOffset(this.boundingBox, dz);
        }
        this.boundingBox.offset(0, 0, dz);

        for (AxisAlignedBB bb : list) {
            dy = bb.calculateYOffset(this.boundingBox, dy);
        }
        this.boundingBox.offset(0, dy, 0);

        this.setComponents(this.x + dx, this.y + dy, this.z + dz);
        this.checkChunks();

        this.checkGroundState(movX, movY, movZ, dx, dy, dz);
        this.updateFallState(this.onGround);

        return true;
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (item.getId() == Item.NAME_TAG) {
            if (item.hasCustomName()) {
                this.setNameTag(item.getCustomName());
                this.setNameTagVisible(true);
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
                return true;
            }
        }
        return false;
    }

    @Override
    public Item[] getDrops() {
        if (this.hasCustomName()) {
            return new Item[]{Item.get(Item.NAME_TAG, 0, 1)};
        }
        return new Item[0];
    }

    protected float getMountedYOffset() {
        return getHeight() * 0.75F;
    }

    public Item[] getRandomArmor() {
        Item[] slots = new Item[4];
        Item helmet = new Item(0, 0, 0);
        Item chestplate = new Item(0, 0, 0);
        Item leggings = new Item(0, 0, 0);
        Item boots = new Item(0, 0, 0);

        switch (Utils.rand(1, 5)) {
            case 1:
                if (Utils.rand(1, 100) < 39) {
                    helmet = Item.get(Item.LEATHER_CAP, 0, Utils.rand(0, 1));
                }
                break;
            case 2:
                if (Utils.rand(1, 100) < 50) {
                    helmet = Item.get(Item.GOLD_HELMET, 0, Utils.rand(0, 1));
                }
                break;
            case 3:
                if (Utils.rand(1, 100) < 14) {
                    helmet = Item.get(Item.CHAIN_HELMET, 0, Utils.rand(0, 1));
                }
                break;
            case 4:
                if (Utils.rand(1, 100) < 3) {
                    helmet = Item.get(Item.IRON_HELMET, 0, Utils.rand(0, 1));
                }
                break;
            case 5:
                if (Utils.rand(1, 100) == 100) {
                    helmet = Item.get(Item.DIAMOND_HELMET, 0, Utils.rand(0, 1));
                }
                break;
        }

        slots[0] = helmet;

        if (Utils.rand(1, 4) != 1) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        chestplate = Item.get(Item.LEATHER_TUNIC, 0, Utils.rand(0, 1));
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        chestplate = Item.get(Item.GOLD_CHESTPLATE, 0, Utils.rand(0, 1));
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        chestplate = Item.get(Item.CHAIN_CHESTPLATE, 0, Utils.rand(0, 1));
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        chestplate = Item.get(Item.IRON_CHESTPLATE, 0, Utils.rand(0, 1));
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        chestplate = Item.get(Item.DIAMOND_CHESTPLATE, 0, Utils.rand(0, 1));
                    }
                    break;
            }
        }

        slots[1] = chestplate;

        if (Utils.rand(1, 2) == 2) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        leggings = Item.get(Item.LEATHER_PANTS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        leggings = Item.get(Item.GOLD_LEGGINGS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        leggings = Item.get(Item.CHAIN_LEGGINGS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        leggings = Item.get(Item.IRON_LEGGINGS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        leggings = Item.get(Item.DIAMOND_LEGGINGS, 0, Utils.rand(0, 1));
                    }
                    break;
            }
        }

        slots[2] = leggings;

        if (Utils.rand(1, 5) < 3) {
            switch (Utils.rand(1, 5)) {
                case 1:
                    if (Utils.rand(1, 100) < 39) {
                        boots = Item.get(Item.LEATHER_BOOTS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 2:
                    if (Utils.rand(1, 100) < 50) {
                        boots = Item.get(Item.GOLD_BOOTS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 3:
                    if (Utils.rand(1, 100) < 14) {
                        boots = Item.get(Item.CHAIN_BOOTS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 4:
                    if (Utils.rand(1, 100) < 3) {
                        boots = Item.get(Item.IRON_BOOTS, 0, Utils.rand(0, 1));
                    }
                    break;
                case 5:
                    if (Utils.rand(1, 100) == 100) {
                        boots = Item.get(Item.DIAMOND_BOOTS, 0, Utils.rand(0, 1));
                    }
                    break;
            }
        }

        slots[3] = boots;

        return slots;
    }
}
