/*
 * Shattered Pixel Dungeon - Assist Edition
 * Assist 0.9.6 World Cycle.
 */
package com.shatteredpixel.shatteredpixeldungeon.levels;

/**
 * Persistent, presentation-only world-time / season / weather rules for Infinite World.
 *
 * This class deliberately does not regenerate terrain. All changes are presented
 * through lighting, particles, audio and visual-only tile overlays so Streaming
 * remains independent from the world cycle.
 */
public final class InfiniteWorldCycle {

    private InfiniteWorldCycle() {}

    public static final int SEASON_SPRING = 0;
    public static final int SEASON_SUMMER = 1;
    public static final int SEASON_AUTUMN = 2;
    public static final int SEASON_WINTER = 3;

    public static final int WEATHER_CLEAR = 0;
    public static final int WEATHER_CLOUDY = 1;
    public static final int WEATHER_RAIN = 2;
    public static final int WEATHER_STORM = 3;
    public static final int WEATHER_FOG = 4;
    public static final int WEATHER_SNOW = 5;
    public static final int WEATHER_WIND = 6;
    public static final int WEATHER_NONE = -1;

    public static final int CHANGE_LIGHT = 1;
    public static final int CHANGE_WEATHER = 1 << 1;
    public static final int CHANGE_SEASON = 1 << 2;
    public static final int CHANGE_DAY = 1 << 3;

    public static final float MINUTES_PER_HERO_ACTION = 5f;
    private static final float MINUTES_PER_DAY = 24f * 60f;
    private static final int DAYS_PER_SEASON = 7;

    public static void ensureInitialized(InfiniteWorldState state, long seed) {
        if (state.worldCycleInitialized) return;

        state.worldCycleInitialized = true;
        state.worldMinutes = 8f * 60f; // Day 1, 08:00.
        state.worldWeather = WEATHER_CLEAR;
        state.worldWeatherRemaining = 45f + deterministicRange(seed, 0, 0, 0, 45f);
        state.worldWeatherSequence = 0;
    }

    public static int advance(InfiniteWorldState state, float actionTime, long seed) {
        if (actionTime <= 0f) return 0;
        ensureInitialized(state, seed);

        int oldDay = day(state);
        int oldSeason = season(state);
        int oldWeather = state.worldWeather;
        int oldLightBand = lightBand(state);

        float elapsedMinutes = actionTime * MINUTES_PER_HERO_ACTION;
        state.worldMinutes += elapsedMinutes;
        state.worldWeatherRemaining -= elapsedMinutes;

        int guard = 0;
        while (state.worldWeatherRemaining <= 0f && guard++ < 16) {
            rollWeather(state, seed);
        }

        int changes = 0;
        if (oldDay != day(state)) changes |= CHANGE_DAY;
        if (oldSeason != season(state)) changes |= CHANGE_SEASON;
        if (oldWeather != state.worldWeather) changes |= CHANGE_WEATHER;
        if (oldLightBand != lightBand(state)) changes |= CHANGE_LIGHT;
        return changes;
    }

    private static void rollWeather(InfiniteWorldState state, long seed) {
        state.worldWeatherSequence++;

        int season = season(state);
        int roll = deterministicInt(seed, day(state), state.worldWeatherSequence, 9601, 100);

        int weather;
        switch (season) {
            case SEASON_SPRING:
                if (roll < 25) weather = WEATHER_CLEAR;
                else if (roll < 45) weather = WEATHER_CLOUDY;
                else if (roll < 75) weather = WEATHER_RAIN;
                else if (roll < 85) weather = WEATHER_STORM;
                else if (roll < 95) weather = WEATHER_FOG;
                else weather = WEATHER_WIND;
                break;
            case SEASON_SUMMER:
                if (roll < 45) weather = WEATHER_CLEAR;
                else if (roll < 60) weather = WEATHER_CLOUDY;
                else if (roll < 75) weather = WEATHER_RAIN;
                else if (roll < 90) weather = WEATHER_STORM;
                else if (roll < 93) weather = WEATHER_FOG;
                else weather = WEATHER_WIND;
                break;
            case SEASON_AUTUMN:
                if (roll < 25) weather = WEATHER_CLEAR;
                else if (roll < 45) weather = WEATHER_CLOUDY;
                else if (roll < 65) weather = WEATHER_RAIN;
                else if (roll < 73) weather = WEATHER_STORM;
                else if (roll < 83) weather = WEATHER_FOG;
                else weather = WEATHER_WIND;
                break;
            case SEASON_WINTER:
            default:
                if (roll < 20) weather = WEATHER_CLEAR;
                else if (roll < 40) weather = WEATHER_CLOUDY;
                else if (roll < 45) weather = WEATHER_RAIN;
                else if (roll < 50) weather = WEATHER_STORM;
                else if (roll < 60) weather = WEATHER_FOG;
                else if (roll < 90) weather = WEATHER_SNOW;
                else weather = WEATHER_WIND;
                break;
        }

        state.worldWeather = weather;

        float minDuration;
        float maxDuration;
        switch (weather) {
            case WEATHER_STORM:
                minDuration = 50f;
                maxDuration = 100f;
                break;
            case WEATHER_RAIN:
            case WEATHER_SNOW:
                minDuration = 70f;
                maxDuration = 150f;
                break;
            case WEATHER_WIND:
            case WEATHER_FOG:
                minDuration = 60f;
                maxDuration = 140f;
                break;
            default:
                minDuration = 80f;
                maxDuration = 180f;
                break;
        }

        float duration = deterministicRange(seed, day(state), state.worldWeatherSequence, 9602,
                maxDuration - minDuration);
        state.worldWeatherRemaining += minDuration + duration;
    }

    public static int day(InfiniteWorldState state) {
        return Math.max(1, (int)Math.floor(state.worldMinutes / MINUTES_PER_DAY) + 1);
    }

    public static int minuteOfDay(InfiniteWorldState state) {
        return Math.floorMod((int)Math.floor(state.worldMinutes), (int)MINUTES_PER_DAY);
    }

    public static int hour(InfiniteWorldState state) {
        return minuteOfDay(state) / 60;
    }

    public static int minute(InfiniteWorldState state) {
        return minuteOfDay(state) % 60;
    }

    public static int season(InfiniteWorldState state) {
        return Math.floorMod((day(state) - 1) / DAYS_PER_SEASON, 4);
    }

    /**
     * 0 night, 1 dawn, 2 day, 3 dusk.
     */
    public static int lightBand(InfiniteWorldState state) {
        int minute = minuteOfDay(state);
        if (minute >= 5 * 60 && minute < 7 * 60) return 1;
        if (minute >= 7 * 60 && minute < 17 * 60) return 2;
        if (minute >= 17 * 60 && minute < 19 * 60) return 3;
        return 0;
    }

    /**
     * Backrooms never inherit ordinary-world weather. The return value is also
     * what GameScene uses for particles and the audio policy.
     */
    public static int presentationWeather(InfiniteWorldState state, int anomaly) {
        return anomaly == 0 ? state.worldWeather : WEATHER_NONE;
    }

    public static boolean suppressesMusic(int weather) {
        return weather == WEATHER_RAIN
                || weather == WEATHER_STORM
                || weather == WEATHER_SNOW
                || weather == WEATHER_WIND;
    }

    public static int ambientLightColor(InfiniteWorldState state, int anomaly) {
        if (anomaly != 0) {
            switch (anomaly) {
                case 1:  return 0xE3C66B; // Level 0: fluorescent yellow.
                case 7:  return 0x101827; // Level 6: lights out.
                case 8:  return 0x173B5E; // Level 7: ocean blue.
                case 10: return 0x102340; // Level 9: fixed midnight.
                case 13: return 0xB8E9EE; // Level 37: cool pool light.
                case 14: return 0xC8B58E; // Level 94: aged model-town light.
                case 18: return 0x472D68; // Level 40: arcade violet.
                case 19: return 0xE38A49; // Level 48: fixed sunset.
                case 20: return 0xE7A8BC; // Level 974: soft pink interior.
                default: return 0xC7CAD0;
            }
        }

        int minute = minuteOfDay(state);
        int season = season(state);
        if (minute >= 5 * 60 && minute < 7 * 60) {
            switch (season) {
                case SEASON_SPRING: return 0x668C92;
                case SEASON_SUMMER: return 0x7C897C;
                case SEASON_AUTUMN: return 0x9B6E55;
                default: return 0x66829A;
            }
        }
        if (minute >= 7 * 60 && minute < 17 * 60) {
            switch (season) {
                case SEASON_SPRING: return 0xE5F0CB;
                case SEASON_SUMMER: return 0xFFE3A3;
                case SEASON_AUTUMN: return 0xE7B76D;
                default: return 0xD8ECF5;
            }
        }
        if (minute >= 17 * 60 && minute < 19 * 60) {
            switch (season) {
                case SEASON_SPRING: return 0xD78D68;
                case SEASON_SUMMER: return 0xE78B4E;
                case SEASON_AUTUMN: return 0xC55F35;
                default: return 0xB46B73;
            }
        }
        return season == SEASON_WINTER ? 0x203852 : 0x152842;
    }

    public static float ambientLightAlpha(InfiniteWorldState state, int anomaly) {
        if (anomaly != 0) {
            switch (anomaly) {
                case 1:  return 0.10f;
                case 7:  return 0.42f;
                case 8:  return 0.20f;
                case 10: return 0.34f;
                case 13: return 0.06f;
                case 14: return 0.09f;
                case 18: return 0.16f;
                case 19: return 0.12f;
                case 20: return 0.08f;
                default: return 0.05f;
            }
        }

        float alpha;
        int band = lightBand(state);
        switch (band) {
            case 1:  alpha = 0.22f; break;
            case 2:  alpha = 0.035f; break;
            case 3:  alpha = 0.25f; break;
            default: alpha = 0.42f; break;
        }

        switch (state.worldWeather) {
            case WEATHER_CLOUDY: alpha += 0.10f; break;
            case WEATHER_RAIN:   alpha += 0.16f; break;
            case WEATHER_STORM:  alpha += 0.26f; break;
            case WEATHER_FOG:    alpha += 0.18f; break;
            case WEATHER_SNOW:   alpha += 0.10f; break;
            case WEATHER_WIND:   alpha += 0.06f; break;
            default: break;
        }
        return Math.min(0.62f, alpha);
    }

    public static String seasonName(int season) {
        switch (season) {
            case SEASON_SPRING: return "春";
            case SEASON_SUMMER: return "夏";
            case SEASON_AUTUMN: return "秋";
            case SEASON_WINTER: return "冬";
            default: return "?";
        }
    }

    public static String periodName(InfiniteWorldState state) {
        switch (lightBand(state)) {
            case 1: return "黎明";
            case 2: return "白天";
            case 3: return "黄昏";
            default:return "夜晚";
        }
    }

    public static String anomalyName(int anomaly) {
        switch (anomaly) {
            case 1: return "Level 0";
            case 2: return "Level 1";
            case 3: return "Level 2";
            case 4: return "Level 3";
            case 5: return "Level 4";
            case 6: return "Level 5";
            case 7: return "Level 6";
            case 8: return "Level 7";
            case 9: return "Level 8";
            case 10:return "Level 9";
            case 11:return "Level 10";
            case 12:return "Level 11";
            case 13:return "Level 37";
            case 14:return "Level 94";
            case 15:return "Level 13";
            case 16:return "Level 18";
            case 17:return "Level 34";
            case 18:return "Level 40";
            case 19:return "Level 48";
            case 20:return "Level 974";
            default:return "普通世界";
        }
    }

    public static String anomalyEnvironmentName(int anomaly) {
        switch (anomaly) {
            case 1: return "固定荧光环境";
            case 7: return "固定黑暗环境";
            case 8: return "固定深海环境";
            case 10:return "固定午夜环境";
            case 13:return "固定冷白池厅";
            case 18:return "固定街机霓虹";
            case 19:return "固定落日环境";
            case 20:return "固定粉色室内";
            default:return "固定环境";
        }
    }

    public static String statusText(InfiniteWorldState state, int anomaly) {
        String time = String.format(java.util.Locale.ROOT, "%02d:%02d", hour(state), minute(state));
        String base = "第" + day(state) + "天  " + time + "  |  "
                + seasonName(season(state)) + "季";
        if (anomaly != 0) {
            return base + "  |  后室 " + anomalyName(anomaly) + " · " + anomalyEnvironmentName(anomaly);
        }
        return base + "  |  " + weatherName(state.worldWeather) + " · " + periodName(state);
    }

    public static String weatherName(int weather) {
        switch (weather) {
            case WEATHER_CLEAR:  return "晴";
            case WEATHER_CLOUDY: return "阴";
            case WEATHER_RAIN:   return "雨";
            case WEATHER_STORM:  return "雷雨";
            case WEATHER_FOG:    return "雾";
            case WEATHER_SNOW:   return "雪";
            case WEATHER_WIND:   return "大风";
            default: return "固定环境";
        }
    }

    public static String lightBandName(InfiniteWorldState state) {
        switch (lightBand(state)) {
            case 1: return "黎明";
            case 2: return "白天";
            case 3: return "黄昏";
            default:return "夜晚";
        }
    }

    public static String statusLine(InfiniteWorldState state, int anomaly) {
        ensureInitialized(state, 0L);
        String time = String.format(java.util.Locale.US, "%02d:%02d", hour(state), minute(state));
        int weather = presentationWeather(state, anomaly);
        return "第" + day(state) + "天 · "
                + seasonName(season(state)) + " · "
                + time + " · "
                + weatherName(weather);
    }

    public static String environmentLine(InfiniteWorldState state, int anomaly) {
        if (anomaly == 0) {
            return "普通世界 · " + lightBandName(state);
        }
        return "后室区域 · 固定环境";
    }

    private static int deterministicInt(long seed, int a, int b, int salt, int bound) {
        long h = mix(seed ^ ((long)a * 0x9E3779B97F4A7C15L)
                ^ ((long)b * 0xC2B2AE3D27D4EB4FL)
                ^ ((long)salt * 0x165667B19E3779F9L));
        return (int)Math.floorMod(h, (long)bound);
    }

    private static float deterministicRange(long seed, int a, int b, int salt, float range) {
        int value = deterministicInt(seed, a, b, salt, 10000);
        return (value / 9999f) * range;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
