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
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.MimicRing;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldProgression;
import com.shatteredpixel.shatteredpixeldungeon.levels.InfiniteWorldRandomEvent;
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

import java.util.ArrayList;
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
    private RedButton btnRandomEvent;
    private CheckBox chkArtifact;
    private RedButton btnTeleport;
    private OptionSlider depthSlider;

    public WndAssist() {
        super();

        addToggle("无敌", SPDSettings.assistInvincible(), SPDSettings::assistInvincible);
        addToggle("物品/金币只增不减", SPDSettings.assistNoConsume(), SPDSettings::assistNoConsume);

        if (Dungeon.infiniteWorld && Dungeon.level instanceof InfiniteWorldLevel) {
            addToggle("无界旁观测试模式", SPDSettings.assistInfiniteSpectator(), value -> {
                SPDSettings.assistInfiniteSpectator(value);

                if (Dungeon.hero != null) {
                    Dungeon.hero.interrupt();
                }

                if (!value && Dungeon.level instanceof InfiniteWorldLevel) {
                    ((InfiniteWorldLevel)Dungeon.level).settleHeroAfterSpectator(Dungeon.hero);
                    if (Dungeon.hero != null && Dungeon.hero.sprite != null) {
                        Dungeon.hero.sprite.remove(
                                com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State.LEVITATING);
                        Dungeon.hero.sprite.idle();
                    }
                } else if (value && Dungeon.hero != null && Dungeon.hero.sprite != null) {
                    Dungeon.hero.sprite.add(
                            com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.State.LEVITATING);
                    Dungeon.hero.sprite.idle();
                }

                GameScene.setInfiniteSpectatorHeroLayer(value);
                if (Dungeon.hero != null && Dungeon.level != null) {
                    Dungeon.observe();
                }

                GLog.p(value
                        ? "无界旁观测试模式已开启：悬浮穿墙、20格测试视野、移动至少 ×8，怪物行动与自然刷新暂停。"
                        : "无界旁观测试模式已关闭。");
            });
        }

        addUpgradeRow();
        if (Dungeon.infiniteWorld) {
            addLevelTestRow();
            addBreakthroughTestRow();
            addRandomEventRow();
        }
        addSpeedRow();
        addItemGrantRow();

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
                if (Dungeon.extraChallenge || Dungeon.infiniteWorld) {
                    GLog.w(Dungeon.infiniteWorld
                            ? "无界地牢没有楼层，不能使用楼层传送。"
                            : "额外挑战为独立单层地图，不能使用楼层传送。");
                    return;
                }

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

    private void addLevelTestRow() {
        float y = pos + GAP;
        float half = (WIDTH - GAP) / 2f;

        RedButton levelUp = new RedButton("测试：等级+1", 8) {
            @Override
            protected void onClick() {
                if (Dungeon.hero == null) return;
                InfiniteWorldProgression.testLevelUp(Dungeon.hero);
            }
        };
        add(levelUp);
        levelUp.setRect(0, y, half, BTN_H);

        RedButton stageCap = new RedButton("测试：直升上限", 8) {
            @Override
            protected void onClick() {
                if (Dungeon.hero == null) return;
                InfiniteWorldProgression.testLevelToStageCap(Dungeon.hero);
            }
        };
        add(stageCap);
        stageCap.setRect(levelUp.right() + GAP, y, half, BTN_H);

        pos = levelUp.bottom();
    }

    private void addBreakthroughTestRow() {
        RedButton clear = new RedButton("测试：快速通关突破", 8) {
            @Override
            protected void onClick() {
                if (Dungeon.hero == null) return;
                InfiniteWorldProgression.testCompleteBreakthrough(Dungeon.hero);
            }
        };
        add(clear);
        clear.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = clear.bottom();
    }

    private void addRandomEventRow() {
        btnRandomEvent = new RedButton(randomEventText(), 8) {
            @Override
            protected void onClick() {
                showRandomEventPicker();
            }
        };
        add(btnRandomEvent);
        btnRandomEvent.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = btnRandomEvent.bottom();
    }

    private String randomEventText() {
        int type = InfiniteWorldRandomEvent.activeType();
        return type == InfiniteWorldRandomEvent.NONE
                ? "随机事件：自动 / 选择"
                : "随机事件：" + InfiniteWorldRandomEvent.eventName(type);
    }

    private void showRandomEventPicker() {
        Game.scene().addToFront(new WndOptions(
                "无限世界随机事件",
                "随机事件平时会自行出现，持续 500～5000 行动值且严格互斥。这里可以主动指定事件或立即关闭当前事件。",
                "怪物狂欢日",
                "暗无天日",
                "财源滚滚",
                "谁是卧底",
                "一路繁花",
                "关闭当前事件",
                "返回") {
            @Override
            protected void onSelect(int index) {
                if (index >= 0 && index < 5) {
                    InfiniteWorldRandomEvent.forceStart(index + 1);
                } else if (index == 5) {
                    InfiniteWorldRandomEvent.forceStop();
                }
                if (btnRandomEvent != null) btnRandomEvent.text(randomEventText());
            }
        });
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


    private void addItemGrantRow() {
        RedButton give = new RedButton("获取指定物品", 9) {
            @Override
            protected void onClick() {
                if (Dungeon.hero != null && Dungeon.level != null) {
                    showItemGrantRoot();
                }
            }
        };
        add(give);
        give.setRect(0, pos + GAP, WIDTH, BTN_H);
        pos = give.bottom();
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
        if (btnRandomEvent != null) btnRandomEvent.text(randomEventText());

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
                } else if (item instanceof MimicRing) {
                    MimicRing mimic = (MimicRing)item;
                    int beforeCharges = mimic.maxCharges();
                    item.upgrade(amount);
                    item.identify();
                    GLog.p("已刷新：" + mimic.name() + " ×" + amount
                            + "，当前能力：" + mimic.currentAbilityName()
                            + "，充能上限 " + beforeCharges + " → " + mimic.maxCharges());
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


    private static final int ITEM_PAGE_SIZE = 8;

    private void showItemGrantRoot() {
        Game.scene().addToFront(new WndOptions(
                "获取指定物品",
                "从游戏物品图鉴中选择物品，再输入要获得的数量。",
                "装备 / 神器 / 饰物",
                "消耗品 / 材料 / 钥匙",
                "返回") {
            @Override
            protected void onSelect(int index) {
                if (index == 0) {
                    showCatalogPicker(true);
                } else if (index == 1) {
                    showCatalogPicker(false);
                }
            }
        });
    }

    private ArrayList<Catalog> availableCatalogs(boolean equipment) {
        ArrayList<Catalog> source = equipment ? Catalog.equipmentCatalogs : Catalog.consumableCatalogs;
        ArrayList<Catalog> result = new ArrayList<>();

        for (Catalog catalog : source) {
            if (!catalogItemClasses(catalog).isEmpty()) {
                result.add(catalog);
            }
        }
        return result;
    }

    private ArrayList<Class<?>> catalogItemClasses(Catalog catalog) {
        ArrayList<Class<?>> result = new ArrayList<>();

        for (Class<?> cls : catalog.items()) {
            if (!Item.class.isAssignableFrom(cls)) continue;

            @SuppressWarnings({"rawtypes", "unchecked"})
            Item preview = (Item) Reflection.newInstance((Class) cls);
            if (preview != null) result.add(cls);
        }
        return result;
    }

    private void showCatalogPicker(final boolean equipment) {
        final ArrayList<Catalog> catalogs = availableCatalogs(equipment);
        String[] options = new String[catalogs.size() + 1];

        for (int i = 0; i < catalogs.size(); i++) {
            options[i] = catalogs.get(i).title();
        }
        options[options.length - 1] = "返回";

        Game.scene().addToFront(new WndOptions(
                equipment ? "装备类物品" : "消耗品与材料",
                "选择物品分类。",
                options) {
            @Override
            protected void onSelect(int index) {
                if (index >= 0 && index < catalogs.size()) {
                    showCatalogItems(catalogs.get(index), 0, equipment);
                } else {
                    showItemGrantRoot();
                }
            }
        });
    }

    private void showCatalogItems(final Catalog catalog, final int requestedPage, final boolean equipment) {
        final ArrayList<Class<?>> classes = catalogItemClasses(catalog);
        if (classes.isEmpty()) {
            showCatalogPicker(equipment);
            return;
        }

        final int pageCount = Math.max(1, (classes.size() + ITEM_PAGE_SIZE - 1) / ITEM_PAGE_SIZE);
        final int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        final int start = page * ITEM_PAGE_SIZE;
        final int itemCount = Math.min(ITEM_PAGE_SIZE, classes.size() - start);
        final boolean hasPrev = page > 0;
        final boolean hasNext = page + 1 < pageCount;

        int extra = 1 + (hasPrev ? 1 : 0) + (hasNext ? 1 : 0);
        String[] options = new String[itemCount + extra];

        for (int i = 0; i < itemCount; i++) {
            options[i] = itemDisplayName(classes.get(start + i));
        }

        int cursor = itemCount;
        final int prevIndex = hasPrev ? cursor++ : -1;
        if (hasPrev) options[prevIndex] = "上一页";
        final int nextIndex = hasNext ? cursor++ : -1;
        if (hasNext) options[nextIndex] = "下一页";
        final int backIndex = cursor;
        options[backIndex] = "返回分类";

        Game.scene().addToFront(new WndOptions(
                catalog.title(),
                "选择物品。" + (pageCount > 1 ? "  第 " + (page + 1) + "/" + pageCount + " 页" : ""),
                options) {
            @Override
            protected void onSelect(int index) {
                if (index >= 0 && index < itemCount) {
                    askItemGrantQuantity(classes.get(start + index), catalog, page, equipment);
                } else if (index == prevIndex) {
                    showCatalogItems(catalog, page - 1, equipment);
                } else if (index == nextIndex) {
                    showCatalogItems(catalog, page + 1, equipment);
                } else if (index == backIndex) {
                    showCatalogPicker(equipment);
                }
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String itemDisplayName(Class<?> cls) {
        Item preview = (Item) Reflection.newInstance((Class) cls);
        return preview == null ? cls.getSimpleName() : Messages.titleCase(preview.trueName());
    }

    private void askItemGrantQuantity(final Class<?> cls, final Catalog catalog,
                                      final int page, final boolean equipment) {
        final String itemName = itemDisplayName(cls);

        GameScene.show(new WndTextInput(
                "获取：" + itemName,
                "输入数量，范围 1～999。可堆叠物品会直接组成一组，不可堆叠物品会生成对应份数。",
                "1",
                3,
                false,
                "获取",
                "取消") {
            @Override
            public void onSelect(boolean positive, String text) {
                if (positive) {
                    try {
                        int amount = Integer.parseInt(text.trim());
                        if (amount < 1 || amount > 999) throw new NumberFormatException();
                        grantSpecificItem(cls, amount);
                    } catch (Exception e) {
                        GLog.w("请输入 1～999 的整数。");
                    }
                }
                showCatalogItems(catalog, page, equipment);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void grantSpecificItem(Class<?> cls, int amount) {
        if (Dungeon.hero == null || Dungeon.level == null) return;

        Item first = (Item) Reflection.newInstance((Class) cls);
        if (first == null) {
            GLog.w("这个物品无法生成。");
            return;
        }

        if (first instanceof Key) {
            Key key = (Key) first;
            key.depth = Dungeon.depth;
            key.quantity(amount);
            Notes.add(key);
            Catalog.setSeen(cls);
            GameScene.updateKeyDisplay();
            GLog.p("已获得：" + Messages.titleCase(first.trueName()) + " ×" + amount);
            return;
        }

        if (first instanceof Gold) {
            Dungeon.gold += amount;
            Catalog.setSeen(cls);
            GLog.p("已增加金币 ×" + amount + "，当前金币：" + Dungeon.gold);
            return;
        }

        if (first instanceof EnergyCrystal) {
            Dungeon.energy += amount;
            Catalog.setSeen(cls);
            GLog.p("已增加能量晶体 ×" + amount + "，当前能量：" + Dungeon.energy);
            return;
        }

        if (first instanceof Dewdrop) {
            Waterskin waterskin = Dungeon.hero.belongings.getItem(Waterskin.class);
            if (waterskin == null) {
                GLog.w("露珠不能存入普通背包。请先获取水袋，再用此功能添加露珠。");
                return;
            }

            int grantedDew = 0;
            for (int i = 0; i < amount && !waterskin.isFull(); i++) {
                Dewdrop dew = new Dewdrop();
                waterskin.collectDew(dew);
                grantedDew++;
            }
            Catalog.setSeen(cls);
            if (grantedDew > 0) {
                GLog.p("已向水袋加入露珠 ×" + grantedDew
                        + (grantedDew < amount ? "（水袋已满）" : ""));
            } else {
                GLog.w("水袋已经装满。");
            }
            return;
        }

        int granted = 0;

        if (first.stackable) {
            first.quantity(amount);
            first.identify();
            if (!first.collect()) {
                Dungeon.level.drop(first, Dungeon.hero.pos).sprite.drop();
            }
            granted = amount;
        } else {
            for (int i = 0; i < amount; i++) {
                Item item = i == 0 ? first : (Item) Reflection.newInstance((Class) cls);
                if (item == null) break;

                item.identify();
                if (!item.collect()) {
                    Dungeon.level.drop(item, Dungeon.hero.pos).sprite.drop();
                }
                granted++;
            }
        }

        Item.updateQuickslot();
        if (granted > 0) {
            GLog.p("已获得：" + Messages.titleCase(first.trueName()) + " ×" + granted);
        } else {
            GLog.w("物品生成失败。");
        }
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
