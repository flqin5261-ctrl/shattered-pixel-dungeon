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
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
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
    private static final int MODIFY_W = 38;

    private float pos = 0;
    private RedButton btnUpgrade;
    private RedButton btnSpeed;
    private RedButton btnArtifact;
    private CheckBox chkArtifact;
    private RedButton btnTeleport;
    private OptionSlider depthSlider;

    public WndAssist() {
        super();

        addToggle("无敌", SPDSettings.assistInvincible(), SPDSettings::assistInvincible);
        addToggle("物品/金币只增不减", SPDSettings.assistNoConsume(), SPDSettings::assistNoConsume);

        addUpgradeRow();
        addSpeedRow();

        // Teleport UI intentionally stays the same.
        addToggle("允许指定层传送", SPDSettings.assistTeleport(), value -> {
            SPDSettings.assistTeleport(value);
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

        addArtifactRow();

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

    private void addUpgradeRow() {
        float y = pos > 0 ? pos + GAP : 0;
        float leftW = WIDTH - MODIFY_W - GAP;

        btnUpgrade = new RedButton(upgradeText(), 8) {
            @Override
            protected void onClick() {
                if (Dungeon.hero != null) showUpgradePicker();
            }
        };
        add(btnUpgrade);
        btnUpgrade.setRect(0, y, leftW, BTN_H);

        RedButton modify = new RedButton("修改", 8) {
            @Override
            protected void onClick() {
                editUpgradeAmount();
            }
        };
        add(modify);
        modify.setRect(btnUpgrade.right() + GAP, y, MODIFY_W, BTN_H);

        pos = btnUpgrade.bottom();
    }

    private void addSpeedRow() {
        float y = pos + GAP;
        float leftW = WIDTH - MODIFY_W - GAP;

        btnSpeed = new RedButton(speedText(), 8) {
            @Override
            protected void onClick() {
                SPDSettings.assistSpeed(!SPDSettings.assistSpeed());
                text(speedText());
                GLog.i("移动速度已" + (SPDSettings.assistSpeed() ? "开启" : "关闭"));
            }
        };
        add(btnSpeed);
        btnSpeed.setRect(0, y, leftW, BTN_H);

        RedButton modify = new RedButton("修改", 8) {
            @Override
            protected void onClick() {
                editSpeedMultiplier();
            }
        };
        add(modify);
        modify.setRect(btnSpeed.right() + GAP, y, MODIFY_W, BTN_H);

        pos = btnSpeed.bottom();
    }

    private void addArtifactRow() {
        float y = pos + GAP;
        float switchW = 44;
        float leftW = WIDTH - switchW - GAP;

        btnArtifact = new RedButton("神器", 9) {
            @Override
            protected void onClick() {
                if (SPDSettings.assistArtifact() && Dungeon.hero != null) {
                    showArtifactPicker();
                }
            }
        };
        add(btnArtifact);
        btnArtifact.setRect(0, y, leftW, BTN_H);

        chkArtifact = new CheckBox("开启") {
            @Override
            protected void onClick() {
                super.onClick();
                SPDSettings.assistArtifact(checked());
                updateButtons();
            }
        };
        chkArtifact.checked(SPDSettings.assistArtifact());
        add(chkArtifact);
        chkArtifact.setRect(btnArtifact.right() + GAP, y, switchW, BTN_H);

        pos = btnArtifact.bottom();
    }

    private String upgradeText() {
        return "强化 +" + SPDSettings.assistUpgradeAmount();
    }

    private String speedText() {
        return String.format(Locale.US, "移动速度 ×%.2f %s",
                SPDSettings.assistSpeedMultiplier(),
                SPDSettings.assistSpeed() ? "（开）" : "（关）");
    }

    private void updateButtons() {
        if (btnUpgrade != null) btnUpgrade.text(upgradeText());
        if (btnSpeed != null) btnSpeed.text(speedText());

        if (btnTeleport != null) btnTeleport.enable(SPDSettings.assistTeleport());
        if (depthSlider != null) depthSlider.enable(SPDSettings.assistTeleport());

        if (chkArtifact != null) chkArtifact.checked(SPDSettings.assistArtifact());
        if (btnArtifact != null) btnArtifact.enable(SPDSettings.assistArtifact());
    }

    private void editUpgradeAmount() {
        hide();
        GameScene.show(new WndTextInput(
                "强化数值",
                "输入每次要增加的强化等级，例如 10、100。范围 1～1000。",
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
                        GLog.p("强化数值已设置为 +" + value);
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
                "移动速度",
                "输入倍率，例如 1.5、2、5、10。范围 0.25～50。",
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
                        GLog.p(String.format(Locale.US, "移动速度已设置为 ×%.2f", SPDSettings.assistSpeedMultiplier()));
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
                // Match every normal upgradeable equipment family. Wands are included too;
                // the Mage's Staff is a Weapon and gets a special synchronization pass below.
                return item instanceof Weapon
                        || item instanceof Armor
                        || item instanceof Artifact
                        || item instanceof Ring
                        || item instanceof Wand;
            }

            @Override
            public void onSelect(Item item) {
                if (item == null) return;

                if (item instanceof Artifact) {
                    Artifact artifact = (Artifact)item;
                    artifact.identify();
                    int before = artifact.visiblyUpgraded();
                    int after = artifact.assistBoostVisibleLevel(amount);
                    GLog.p("已强化：" + artifact.name() + "，神器等级 " + before + " → " + after
                            + "（神器遵循自身等级上限）");
                } else {
                    int before = item.trueLevel();

                    // Do NOT write the raw level field here. Every equipment family can have
                    // upgrade side effects/caches. Calling upgrade() repeatedly is equivalent
                    // to real upgrade-scroll progression and invokes each subclass hook.
                    item.upgrade(amount);
                    item.identify();

                    // Mage's Staff owns an internal Wand. Its upgrade() already syncs on each
                    // step, and this final pass also repairs saves created by the old Assist
                    // implementation where only the outer staff level was changed.
                    if (item instanceof MagesStaff) {
                        ((MagesStaff)item).updateWand(false);
                    }

                    int after = item.trueLevel();
                    GLog.p("已强化：" + item.name() + "，+" + before + " → +" + after);
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
                "神器",
                "选择一个神器。已拥有的同类神器不会重复生成。",
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
