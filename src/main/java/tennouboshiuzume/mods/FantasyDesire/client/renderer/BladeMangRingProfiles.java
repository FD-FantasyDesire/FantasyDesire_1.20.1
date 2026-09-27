package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 光环的模型专用参数；全部距离使用刀模型的局部单位。 */
public final class BladeMangRingProfiles {
    private static final float DEFAULT_START_X = -55.0F;
    private static final float DEFAULT_END_X = -155.0F;
    private static final Profile DEFAULT = new Profile(
            new ResourceLocation("slashblade", "model/blade.obj"),
            1.0F, 1.0F, 1.0F, DEFAULT_START_X, DEFAULT_END_X);

    // 每行依次为：模型、起始倍率、中间倍率、结束倍率、起始 X 坐标、结束 X 坐标。
    // 三个倍率控制光环半径和环宽，在分布区间的 0%、50%、100% 之间线性插值。
    // 坐标直接覆盖默认区间；未单独配置的模型使用 1/1/1 和 -55/-155。
    private static final List<Profile> PROFILES = List.of(
            new Profile(new ResourceLocation("fantasydesire", "models/sn.obj"),
                    2.4F, 2.2F, 2.2F, -85.0F, -280.0F),
            new Profile(new ResourceLocation("fantasydesire", "models/sn_huge.obj"),
                    24.0F, 22.0F, 22.0F, -850.0F, -2800.0F));

    private BladeMangRingProfiles() {
    }

    public static Profile forModel(WavefrontObject model) {
        if (model == null) {
            return DEFAULT;
        }
        // 比对本次实际绘制的模型，兼容事件替换；只查询已有缓存，不额外加载模型。
        // F3+T 后本体会替换缓存中的实例，这里不保留旧模型引用。
        var cache = BladeModelManager.getInstance().cache;
        for (Profile profile : PROFILES) {
            if (cache.getIfPresent(profile.model()) == model) {
                return profile;
            }
        }
        return DEFAULT;
    }

    public record Profile(ResourceLocation model, float startScale, float middleScale, float endScale,
            float startX, float endX) {
        public Profile {
            if (model == null || !validScale(startScale) || !validScale(middleScale) || !validScale(endScale)
                    || !Float.isFinite(startX) || !Float.isFinite(endX) || startX <= endX) {
                throw new IllegalArgumentException("光环模型参数无效：倍率须为正有限值，起点必须大于终点：" + model);
            }
        }

        /** 按光环数量等分区间，每个光环位于所属区段中心。 */
        public float progress(int index, int count) {
            if (count < 1 || index < 0 || index >= count) {
                throw new IllegalArgumentException("光环索引超出数量范围");
            }
            return (index + 0.5F) / count;
        }

        public float xAt(float progress) {
            float t = checkedProgress(progress);
            return interpolate(startX, endX, t);
        }

        public float scaleAt(float progress) {
            float t = checkedProgress(progress);
            return t <= 0.5F ? interpolate(startScale, middleScale, t * 2.0F)
                    : interpolate(middleScale, endScale, (t - 0.5F) * 2.0F);
        }

        private static boolean validScale(float value) {
            return Float.isFinite(value) && value > 0.0F && value <= 1024.0F;
        }

        private static float checkedProgress(float value) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("光环插值进度必须为有限值");
            }
            return Math.max(0.0F, Math.min(1.0F, value));
        }

        private static float interpolate(float from, float to, float progress) {
            return (1.0F - progress) * from + progress * to;
        }
    }
}
