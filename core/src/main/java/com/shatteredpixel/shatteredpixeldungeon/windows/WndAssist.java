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

public class WndAssist extends Window {

    private static final int WIDTH = 140;
    private static final int BTN_H = 18;
    private static final int SLIDER_H = 21;
    private static final int GAP = 2;

    private float pos = 0;
    private RedButton btnTeleport;
    private RedButton btnArtifact;
    private RedButton btnUpgrade;
    private OptionSlider depthSlider;

    public WndAssist() {
        super();

        addToggle("无敌", SPDSettings.assistInvincible(), SPDSettings::assistInvincible);
        addToggle("物品/金币越用越多", SPDSettings.assistNoConsume(), SPDSettings::assistNoConsume);
        addToggle("启用指定装备 +10", SPDSettings.assistWeapon10(), value -> {
            SPDSettings.assistWeapon10(value);
            updateButtons();
        });
        addToggle("移动速度 ×2", SPDSettings.assistSpeed(), SPDSettings::assistSpeed);
        addToggle("允许指定层传送", SPDSettings.assistTeleport(), value -> {
            SPDSettings.assistTeleport(value);
            updateButtons();
        });
        addToggle("允许神器自选", SPDSettings.assistArtifact(), value -> {
            SPDSettings.assistArtifact(value);
            updateButtons();
        });

        btnUpgrade = new RedButton("选择武器/防具/神器/戒指 +10", 8) {
            @Override
            protected void onClick() {
                if (!SPDSettings.assistWeapon10() || Dungeon.hero == null) return;
                showUpgradePicker();
            }
        };
        add(btnUpgrade);
        btnUpgrade.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnUpgrade.bottom();

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

    private void updateButtons() {
        if (btnUpgrade != null) btnUpgrade.enable(SPDSettings.assistWeapon10());
        if (btnTeleport != null) btnTeleport.enable(SPDSettings.assistTeleport());
        if (btnArtifact != null) btnArtifact.enable(SPDSettings.assistArtifact());
        if (depthSlider != null) depthSlider.enable(SPDSettings.assistTeleport());
    }

    private void showUpgradePicker() {
        GameScene.selectItem(new WndBag.ItemSelector() {
            @Override
            public String textPrompt() {
                return "选择要强化 +10 的装备";
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
                    int after = artifact.assistBoostVisibleLevel(10);
                    GLog.p("已指定强化：" + artifact.name() + "，神器等级 " + before + " → " + after);
                } else {
                    int before = item.trueLevel();
                    item.level(before + 10);
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
