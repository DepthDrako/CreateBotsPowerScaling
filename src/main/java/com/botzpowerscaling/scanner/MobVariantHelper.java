package com.botzpowerscaling.scanner;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class for getting detailed mob names including variants.
 * Examples: "Baby Zombie", "Charged Creeper", "Farmer Villager"
 */
public class MobVariantHelper {

    /**
     * Gets a detailed name for the entity including any variant information.
     *
     * @param entity The entity to get the name for
     * @return A detailed name string
     */
    public static String getDetailedName(LivingEntity entity) {
        if (entity == null) {
            return "Unknown";
        }

        // Players just use their username
        if (entity instanceof Player) {
            return entity.getName().getString();
        }

        StringBuilder name = new StringBuilder();
        String baseName = entity.getName().getString();

        // Check for baby variants (Mob has isBaby())
        if (entity instanceof Mob mob && mob.isBaby()) {
            name.append("Baby ");
        }

        // Check specific mob variants
        String variant = getVariantPrefix(entity);
        if (variant != null && !variant.isEmpty()) {
            name.append(variant).append(" ");
        }

        name.append(baseName);

        return name.toString();
    }

    /**
     * Gets the variant prefix for specific mob types.
     */
    private static String getVariantPrefix(LivingEntity entity) {
        // Creeper - charged
        if (entity instanceof Creeper creeper) {
            if (creeper.isPowered()) {
                return "Charged";
            }
        }

        // Mooshroom - red/brown
        if (entity instanceof MushroomCow mooshroom) {
            return mooshroom.getVariant() == MushroomCow.MushroomType.BROWN ? "Brown" : "Red";
        }

        // Sheep - color
        if (entity instanceof Sheep sheep) {
            return getColorName(sheep.getColor().getId());
        }

        // Cat - variant (simplified)
        if (entity instanceof Cat cat) {
            try {
                String variant = cat.getVariant().toString();
                if (variant.contains(":")) {
                    variant = variant.substring(variant.lastIndexOf(":") + 1);
                }
                return capitalizeFirst(variant.replace("_", " "));
            } catch (Exception e) {
                return null;
            }
        }

        // Fox - red/snow
        if (entity instanceof Fox fox) {
            return fox.getVariant() == Fox.Type.SNOW ? "Snow" : "Red";
        }

        // Rabbit - variant
        if (entity instanceof Rabbit rabbit) {
            return getRabbitVariantName(rabbit.getVariant());
        }

        // Axolotl - color
        if (entity instanceof Axolotl axolotl) {
            return getAxolotlVariantName(axolotl.getVariant());
        }

        // Frog - variant (simplified)
        if (entity instanceof Frog frog) {
            try {
                String variant = frog.getVariant().toString();
                if (variant.contains(":")) {
                    variant = variant.substring(variant.lastIndexOf(":") + 1);
                }
                return capitalizeFirst(variant);
            } catch (Exception e) {
                return null;
            }
        }

        // Parrot - color
        if (entity instanceof Parrot parrot) {
            return getParrotVariantName(parrot.getVariant());
        }

        // Villager - profession
        if (entity instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (profession != VillagerProfession.NONE) {
                return capitalizeFirst(profession.name().toLowerCase());
            }
        }

        // Zombie Villager - profession
        if (entity instanceof ZombieVillager zombieVillager) {
            VillagerProfession profession = zombieVillager.getVillagerData().getProfession();
            if (profession != VillagerProfession.NONE) {
                return capitalizeFirst(profession.name().toLowerCase());
            }
        }

        // Slime/Magma Cube - size
        if (entity instanceof Slime slime) {
            int size = slime.getSize();
            if (size <= 1) return "Tiny";
            if (size == 2) return "Small";
            if (size >= 4) return "Big";
            return "Medium";
        }

        // Wolf - angry/tamed
        if (entity instanceof Wolf wolf) {
            if (wolf.isAngry()) {
                return "Angry";
            }
            if (wolf.isTame()) {
                return "Tamed";
            }
        }

        // Bee - angry
        if (entity instanceof Bee bee) {
            if (bee.isAngry()) {
                return "Angry";
            }
        }

        // Iron Golem - player-created vs natural
        if (entity instanceof IronGolem golem) {
            if (golem.isPlayerCreated()) {
                return "Summoned";
            }
        }

        return null;
    }

    private static String getColorName(int colorId) {
        return switch (colorId) {
            case 0 -> "White";
            case 1 -> "Orange";
            case 2 -> "Magenta";
            case 3 -> "Light Blue";
            case 4 -> "Yellow";
            case 5 -> "Lime";
            case 6 -> "Pink";
            case 7 -> "Gray";
            case 8 -> "Light Gray";
            case 9 -> "Cyan";
            case 10 -> "Purple";
            case 11 -> "Blue";
            case 12 -> "Brown";
            case 13 -> "Green";
            case 14 -> "Red";
            case 15 -> "Black";
            default -> null;
        };
    }

    private static String getRabbitVariantName(Rabbit.Variant variant) {
        return switch (variant) {
            case BROWN -> "Brown";
            case WHITE -> "White";
            case BLACK -> "Black";
            case WHITE_SPLOTCHED -> "Spotted";
            case GOLD -> "Gold";
            case SALT -> "Salt";
            case EVIL -> "Killer";
        };
    }

    private static String getAxolotlVariantName(Axolotl.Variant variant) {
        return switch (variant) {
            case LUCY -> "Lucy";
            case WILD -> "Wild";
            case GOLD -> "Gold";
            case CYAN -> "Cyan";
            case BLUE -> "Blue";
        };
    }

    private static String getParrotVariantName(Parrot.Variant variant) {
        return switch (variant) {
            case RED_BLUE -> "Scarlet";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case YELLOW_BLUE -> "Yellow";
            case GRAY -> "Gray";
        };
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
