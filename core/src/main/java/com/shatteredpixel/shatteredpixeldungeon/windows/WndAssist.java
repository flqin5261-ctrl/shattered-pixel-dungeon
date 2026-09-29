/*
 * Shattered Pixel Dungeon - Assist Edition additions
 * Copyright (C) 2026
 *
 * Distributed under the GNU General Public License v3.0 or later,
 * consistent with the upstream project.
 */
package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;

import java.util.Locale;

public class WndAssist extends Window {

    private static final int WIDTH = 150;
    private static final int BTN_H = 18;
    private static final int SLIDER_H = 21;
    private static final int GAP = 2;

    private float pos = 0;
    private RedButton btnTeleport;
    private RedButton btnArtifact;
    private RedButton btnUpgrade;
    private RedButton btnUpgradeAmount;
    private RedButton btnSpeedAmount;
    private OptionSlider depthSlider;

    public WndAssist() {
        super();

        addToggle("无敌", SPDSettings.assistInvincible(), SPDSettings::assistInvincible);
        addToggle("物品/金币越用越多", SPDSettings.assistNoConsume(), SPDSettings::assistNoConsume);

        addToggle("启用指定装备强化", SPDSettings.assistWeapon10(), value -> {
            SPDSettings.assistWeapon10(value);
            updateButtons();
        });

        btnUpgradeAmount = new RedButton(upgradeAmountText(), 8) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistWeapon10()) return;
                editUpgradeAmount();
            }
        };
        add(btnUpgradeAmount);
        btnUpgradeAmount.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnUpgradeAmount.bottom();

        btnUpgrade = new RedButton(upgradeActionText(), 8) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistWeapon10() || Dungeon.hero == null) return;
                showUpgradePicker();
            }
        };
        add(btnUpgrade);
        btnUpgrade.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnUpgrade.bottom();

        addToggle("启用自定义移速", SPDSettings.assistSpeed(), value -> {
            SPDSettings.assistSpeed(value);
            updateButtons();
        });

        btnSpeedAmount = new RedButton(speedAmountText(), 8) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistSpeed()) return;
                editSpeedMultiplier();
            }
        };
        add(btnSpeedAmount);
        btnSpeedAmount.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnSpeedAmount.bottom();

        addToggle("允许指定层传送", SPDSettings.assistTeleport(), value -> {
            SPDSettings.assistTeleport(value);
            updateButtons();
        });

        addToggle("允许神器自选", SPDSettings.assistArtifact(), value -> {
            SPDSettings.assistArtifact(value);
            updateButtons();
        });

        depthSlider = new OptionSlider("目标楼层", "1", "25", 1, 25) {
            @Override
            protected void onChange() {
                // selection is read when the teleport button is pressed
            }
        };
        depthSlider.setSelectedValue(Dungeon.depth > 0 ? Math.min(25, Dungeon.depth) : 1);
        add(depthSlider);
        depthSlider.setRect(0, pos + GAP, WIDTH, SLIDER_H);
        pos = depthSlider.bottom();

        btnTeleport = new RedButton("传送到所选楼层", 9) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistTeleport() || Dungeon.hero == null || Dungeon.level == null) return;

                int target = depthSlider.getSelectedValue();
                if (target == Dungeon.depth && Dungeon.branch == 0) {
                    GLog.i("你已经在第 " + target + " 层。");
                    return;
                }

                hide();
                Level.beforeTransition();
                InterlevelScene.mode = InterlevelScene.Mode.RETURN;
                InterlevelScene.returnDepth = target;
                InterlevelScene.returnBranch = 0;
                InterlevelScene.returnPos = -1;
                Game.switchScene(InterlevelScene.class);
            }
        };
        add(btnTeleport);
        btnTeleport.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnTeleport.bottom();

        btnArtifact = new RedButton("选择并获得神器", 9) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistArtifact() || Dungeon.hero == null) return;
                showArtifactPicker();
            }
        };
        add(btnArtifact);
        btnArtifact.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnArtifact.bottom();

        updateButtons();
        resize(WIDTH, (int) pos);
    }

    private interface ToggleSetter {
        void set(boolean value);
    }

    private void addToggle(String label, boolean checked, ToggleSetter setter) {
        CheckBox box = new CheckBox(label) {
            @Override
            protected void onClick() {
                super.onClick();
                setter.set(checked());
            }
        };
        box.checked(checked);
        add(box);
        box.setRect(0, pos > 0 ? pos + GAP : 0, WIDTH, BTN_H);
        pos = box.bottom();
    }

    private String upgradeAmountText() {
        return "设置强化增量：+" + SPDSettings.assistUpgradeAmount();
    }

    private String upgradeActionText() {
        return "选择装备并 +" + SPDSettings.assistUpgradeAmount();
    }

    private String speedAmountText() {
        return String.format(Locale.US, "设置移动倍率：×%.2f", SPDSettings.assistSpeedMultiplier());
    }

    private void updateButtons() {
        boolean upgrade = SPDSettings.assistWeapon10();
        if (btnUpgrade != null) {
            btnUpgrade.enable(upgrade);
            btnUpgrade.text(upgradeActionText());
        }
        if (btnUpgradeAmount != null) {
            btnUpgradeAmount.enable(upgrade);
            btnUpgradeAmount.text(upgradeAmountText());
        }

        boolean speed = SPDSettings.assistSpeed();
        if (btnSpeedAmount != null) {
            btnSpeedAmount.enable(speed);
            btnSpeedAmount.text(speedAmountText());
        }

        if (btnTeleport != null) btnTeleport.enable(SPDSettings.assistTeleport());
        if (btnArtifact != null) btnArtifact.enable(SPDSettings.assistArtifact());
        if (depthSlider != null) depthSlider.enable(SPDSettings.assistTeleport());
    }

    private void editUpgradeAmount() {
        hide();
        GameScene.show(new WndTextInput(
                "自定义强化数值",
                "输入本次强化增量，例如 10 或 100。范围 1～1000。",
                Integer.toString(SPDSettings.assistUpgradeAmount()),
                4,
                false,
                "确定",
                "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (positive) {
                    try {
                        String cleaned = text.trim().replace("+", "");
                        int value = Integer.parseInt(cleaned);
                        if (value < 1 || value > 1000) throw new NumberFormatException();
                        SPDSettings.assistUpgradeAmount(value);
                        GLog.p("强化增量已设置为 +" + value);
                    } catch (Exception e) {
                        GLog.w("请输入 1～1000 的整数。");
                    }
                }
                GameScene.show(new WndAssist());
            }
        });
    }

    private void editSpeedMultiplier() {
        hide();
        GameScene.show(new WndTextInput(
                "自定义移动倍率",
                "可输入 1.5、2、5、10 等。范围 0.25～50。",
                String.format(Locale.US, "%.2f", SPDSettings.assistSpeedMultiplier()),
                6,
                false,
                "确定",
                "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (positive) {
                    try {
                        String cleaned = text.trim().toLowerCase(Locale.ROOT)
                                .replace("×", "").replace("x", "");
                        float value = Float.parseFloat(cleaned);
                        if (value < 0.25f || value > 50f) throw new NumberFormatException();
                        SPDSettings.assistSpeedPercent(Math.round(value * 100f));
                        GLog.p(String.format(Locale.US, "移动倍率已设置为 ×%.2f", SPDSettings.assistSpeedMultiplier()));
                    } catch (Exception e) {
                        GLog.w("请输入 0.25～50 之间的数字。");
                    }
                }
                GameScene.show(new WndAssist());
            }
        });
    }

    private void showUpgradePicker() {
        final int amount = SPDSettings.assistUpgradeAmount();

        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override
            public String textPrompt() {
                return "选择要强化 +" + amount + " 的装备";
            }

            @Override
            public boolean itemSelectable(Item item) {
                return item instanceof Weapon
                        || item instanceof Armor
                        || item instanceof Artifact
                        || item instanceof Ring;
            }

            @Override
            public void onSelect(Item item) {
                if (item == null) return;

                if (item instanceof Artifact) {
                    Artifact artifact = (Artifact)item;
                    artifact.identify();
                    int before = artifact.visiblyUpgraded();
                    int after = artifact.assistBoostVisibleLevel(amount);
                    GLog.p("已指定强化：" + artifact.name() + "，神器等级 " + before + " → " + after
                            + "（神器仍遵循自身等级上限）");
                } else {
                    int before = item.trueLevel();
                    long desired = (long)before + amount;
                    int after = desired > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)desired;
                    item.level(after);
                    item.identify();
                    GLog.p("已指定强化：" + item.name() + "，+" + before + " → +" + item.trueLevel());
                }
                Item.updateQuickslot();
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void showArtifactPicker() {
        final Class<?>[] classes = Generator.Category.ARTIFACT.classes;
        String[] names = new String[classes.length];

        for (int i = 0; i < classes.length; i++) {
            Artifact preview = (Artifact) Reflection.newInstance((Class) classes[i]);
            names[i] = preview == null ? classes[i].getSimpleName() : Messages.titleCase(preview.name());
        }

        Game.scene().addToFront(new WndOptions(
                "神器自选",
                "选择一个神器。已在背包中的同类神器不会重复生成。",
                names) {
            @Override
            protected void onSelect(int index) {
                if (Dungeon.hero == null || index < 0 || index >= classes.length) return;

                Class artifactClass = classes[index];
                if (Dungeon.hero.belongings.getItem(artifactClass) != null) {
                    GLog.w("你已经拥有这个神器。");
                    return;
                }

                Artifact artifact = (Artifact) Reflection.newInstance(artifactClass);
                if (artifact == null) return;

                artifact.identify();
                if (!artifact.collect()) {
                    Dungeon.level.drop(artifact, Dungeon.hero.pos).sprite.drop();
                }
                GLog.p("已获得：" + artifact.name());
            }
        });
    }
}
