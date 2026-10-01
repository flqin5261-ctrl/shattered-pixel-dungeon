/*
 * Shattered Pixel Dungeon Assist
 */
package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GenesisEcho;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.Game;

/**
 * Dedicated 纵横八荒 artifact hot-slot.
 *
 * Single tap is intentionally delayed very slightly so a double tap can be
 * distinguished cleanly:
 * - single tap: use the currently selected artifact
 * - double tap: switch to the next equipped artifact
 */
public class GenesisArtifactButton extends InventorySlot {

    private static final float DOUBLE_TAP_WINDOW = 0.28f;

    private float tapTimer = 0f;
    private Artifact pendingArtifact = null;

    public GenesisArtifactButton() {
        super(null);
        showExtraInfo(false);
    }

    @Override
    public void update() {
        super.update();

        Artifact selected = selectedArtifact();
        if (item() != selected) {
            item(selected);
        }

        visible = GenesisEcho.active(Dungeon.hero);
        enable(visible && Dungeon.hero != null && Dungeon.hero.isAlive());

        if (tapTimer > 0f) {
            tapTimer -= Game.elapsed;
            if (tapTimer <= 0f) {
                Artifact toUse = pendingArtifact;
                pendingArtifact = null;
                tapTimer = 0f;
                useArtifact(toUse);
            }
        }
    }

    @Override
    protected void onClick() {
        if (Dungeon.hero == null || !Dungeon.hero.isAlive() || !Dungeon.hero.ready) return;
        Artifact selected = selectedArtifact();
        if (selected == null) return;

        if (tapTimer > 0f && pendingArtifact != null) {
            tapTimer = 0f;
            pendingArtifact = null;
            Artifact next = Dungeon.hero.belongings.cyclePrimaryArtifact();
            item(next);
            Item.updateQuickslot();
            if (next != null) {
                GLog.p("快捷神器切换为「" + next.name() + "」");
            }
            return;
        }

        pendingArtifact = selected;
        tapTimer = DOUBLE_TAP_WINDOW;
    }

    @Override
    protected boolean onLongClick() {
        Artifact selected = selectedArtifact();
        if (selected != null) {
            tapTimer = 0f;
            pendingArtifact = null;
            GameScene.show(new WndUseItem(null, selected));
            return true;
        }
        return false;
    }

    private Artifact selectedArtifact() {
        if (Dungeon.hero == null || !GenesisEcho.active(Dungeon.hero)) return null;
        return Dungeon.hero.belongings.displayArtifact();
    }

    private void useArtifact(Artifact artifact) {
        if (artifact == null || Dungeon.hero == null || !Dungeon.hero.ready
                || !Dungeon.hero.belongings.isStackEquipped(artifact)) {
            return;
        }

        if (artifact.defaultAction() != null) {
            artifact.execute(Dungeon.hero);
        } else {
            GameScene.show(new WndUseItem(null, artifact));
        }
    }
}
