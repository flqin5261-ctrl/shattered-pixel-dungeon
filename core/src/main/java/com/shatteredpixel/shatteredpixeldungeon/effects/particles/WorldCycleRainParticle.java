/*
 * Shattered Pixel Dungeon - Assist Edition
 * Lightweight rain streak used by Assist 0.9.6 World Cycle.
 */
package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class WorldCycleRainParticle extends PixelParticle {

    public static final Emitter.Factory FACTORY = new Emitter.Factory() {
        @Override
        public void emit(Emitter emitter, int index, float x, float y) {
            ((WorldCycleRainParticle)emitter.recycle(WorldCycleRainParticle.class)).reset(x, y);
        }
    };

    public WorldCycleRainParticle() {
        super();
        lifespan = 0.42f;
        color(0x9BC8EA);
    }

    public void reset(float x, float y) {
        revive();

        this.x = x;
        this.y = y - Random.Float(8f, 24f);
        speed.set(Random.Float(-24f, -8f), Random.Float(190f, 260f));
        size = Random.Float(0.8f, 1.4f);
        left = lifespan;
        am = 0.65f;
    }

    @Override
    public void update() {
        super.update();
        float p = left / lifespan;
        am = Math.min(0.72f, p * 1.4f);
    }
}
