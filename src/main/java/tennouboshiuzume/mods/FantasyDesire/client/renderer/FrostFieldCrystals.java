package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 客户端冰晶簇：六向碰撞面采样、固定落点随机形状，不创建实体或修改方块。 */
final class FrostFieldCrystals {
    private static final int MAX_CLUSTERS = 128;
    private static final int TERRAIN_REFRESH_TICKS = 10;
    private final Map<SurfaceKey, Crystal> roots = new HashMap<>();
    private final long fieldSeed;
    private long lastUpdate = Long.MIN_VALUE;
    private Vec3 lastCenter = Vec3.ZERO;
    private float lastRadius;

    FrostFieldCrystals(long fieldSeed) {
        this.fieldSeed = fieldSeed;
    }

    void update(ClientLevel level, LivingEntity owner, Vec3 center, float radius) {
        long now = level.getGameTime();
        if (lastUpdate != Long.MIN_VALUE && now - lastUpdate < TERRAIN_REFRESH_TICKS
                && (now - lastUpdate < 2 || (lastCenter.distanceToSqr(center) < 1.0 && lastRadius == radius))) {
            return;
        }
        lastUpdate = now;
        lastCenter = center;
        lastRadius = radius;

        // 每个面至多 14×14 个候选；半径增大时增大间距，六向射线和最终晶簇数都有上限。
        double spacing = Math.max(3.0, radius / 6.0);
        double reachRadius = Math.max(0.0, radius - 0.25);
        List<Candidate> candidates = new ArrayList<>();
        for (Direction face : Direction.values()) {
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 tangent = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 bitangent = tangent.cross(normal);
            double centerU = center.dot(tangent);
            double centerV = center.dot(bitangent);
            int minU = Mth.floor((centerU - radius) / spacing);
            int maxU = Mth.floor((centerU + radius) / spacing);
            int minV = Mth.floor((centerV - radius) / spacing);
            int maxV = Mth.floor((centerV + radius) / spacing);
            for (int u = minU; u <= maxU; ++u) {
                for (int v = minV; v <= maxV; ++v) {
                    SurfaceKey key = new SurfaceKey(face, u, v);
                    long seed = fieldSeed ^ (long) u * 0x632BE59BD9B4E019L
                            ^ (long) v * 0x9E3779B97F4A7C15L ^ (long) face.ordinal() * 0x94D049BB133111EBL;
                    // 留空概率与坐标扰动独立，避免规则阵列；种子只在领域重新激活时改变。
                    if (random(seed + 19) < 0.18F) continue;
                    double du = (u + 0.12 + random(seed + 31) * 0.76) * spacing - centerU;
                    double dv = (v + 0.12 + random(seed + 47) * 0.76) * spacing - centerV;
                    double reachSquared = reachRadius * reachRadius - du * du - dv * dv;
                    if (reachSquared <= 0.0) continue;
                    Vec3 from = center.add(tangent.scale(du)).add(bitangent.scale(dv));
                    Vec3 to = from.subtract(normal.scale(Math.sqrt(reachSquared)));
                    if (from.y < level.getMinBuildHeight() || from.y >= level.getMaxBuildHeight()
                            || !level.hasChunkAt(BlockPos.containing(from))
                            || !level.hasChunkAt(BlockPos.containing(to))) continue;
                    // 从领域的截面朝外寻找最近表面：向下找地面、向上找天花板、横向找墙面。
                    BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, owner));
                    if (hit.getType() != HitResult.Type.BLOCK || hit.isInside() || hit.getDirection() != face
                            || hit.getLocation().distanceToSqr(center) >= reachRadius * reachRadius) continue;
                    Vec3 root = hit.getLocation().subtract(normal.scale(0.012));
                    candidates.add(new Candidate(key, root, seed));
                }
            }
        }
        // 全部面一起挑选，防止地面先占满配额，墙面与天花板没有机会生成。
        candidates.sort(Comparator.comparingDouble(c -> random(c.seed + 73)));
        Map<SurfaceKey, Crystal> next = new HashMap<>();
        for (Candidate candidate : candidates) {
            Crystal previous = roots.get(candidate.key);
            Crystal crystal = previous != null && previous.root.distanceToSqr(candidate.root) < 0.0001
                    ? previous : createCrystal(candidate.root, candidate.key.face, candidate.seed, now);
            next.put(candidate.key, crystal);
            if (next.size() >= MAX_CLUSTERS) break;
        }
        roots.clear();
        roots.putAll(next);
    }

    void render(Matrix4f viewProjection, Vec3 camera, Vec3 fieldCenter, float radius,
            float fieldAge, float pulseRadius, long now, float partialTick) {
        if (roots.isEmpty() || !FDShaderHandler.isFrostCrystalShaderLoaded()) return;
        ShaderInstance shader = FDShaderHandler.getFrostCrystalShader();
        if (shader == null) return;
        shader.safeGetUniform("ViewProj").set(viewProjection);
        shader.safeGetUniform("FieldCenter").set((float) fieldCenter.x, (float) fieldCenter.y, (float) fieldCenter.z);
        shader.safeGetUniform("FieldRadius").set(radius);
        shader.safeGetUniform("PulseRadius").set(pulseRadius);
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableCull();
        List<Crystal> sorted = new ArrayList<>(roots.values());
        sorted.sort(Comparator.comparingDouble((Crystal c) -> c.root.distanceToSqr(camera)).reversed());
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_TEX);
        int count = 0;
        for (Crystal crystal : sorted) {
            Vec3 root = crystal.root.subtract(camera);
            float distance = (float) root.distanceTo(fieldCenter);
            float bornAge = (float) Math.min(160.0, now - crystal.born + partialTick);
            float availableAge = Math.min(bornAge, fieldAge - distance / 9.0F * 20.0F);
            float boundaryFade = smooth((radius - distance) / 0.85F);
            // 只为贴近施法者的晶簇留出空间；头顶和墙面不会因为 XZ 坐标相同被误删。
            boundaryFade *= smooth((distance - 0.75F) / 0.5F);
            for (Shard shard : crystal.shards) {
                float growth = smooth((availableAge - shard.delayTicks) / shard.durationTicks);
                float fade = boundaryFade * growth;
                if (fade <= 0.001F) continue;
                for (CrystalVertex vertex : shard.vertices) {
                    emitVertex(builder, root, vertex, growth, fade);
                }
                ++count;
            }
        }
        if (count == 0) builder.end().release();
        else BufferUploader.drawWithShader(builder.end());
        RenderSystem.disableCull();
    }

    private static Crystal createCrystal(Vec3 root, Direction face, long seed, long born) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 tangent = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 bitangent = tangent.cross(normal);
        int shardCount = 2 + (int) (random(seed + 101) * 4.0F);
        List<Shard> shards = new ArrayList<>(shardCount);
        float clusterDelay = random(seed + 113) * 9.0F;
        for (int i = 0; i < shardCount; ++i) {
            long shardSeed = seed + (i + 1L) * 0x9E3779B97F4A7C15L;
            float height = 0.45F + random(shardSeed + 3) * 1.65F;
            float width = 0.075F + random(shardSeed + 5) * 0.20F;
            float angle = random(shardSeed + 7) * Mth.TWO_PI;
            float leanAngle = random(shardSeed + 11) * Mth.TWO_PI;
            // 相对附着面外法线倾斜 3~16 度，墙面侧长、天花板倒挂；不会偏转回实体方块内。
            double lean = Math.tan(Math.toRadians(3.0 + random(shardSeed + 13) * 13.0));
            float tipX = (float) (Math.cos(leanAngle) * lean * height);
            float tipZ = (float) (Math.sin(leanAngle) * lean * height);
            float shoulder = 0.48F + random(shardSeed + 17) * 0.30F;
            float ellipticity = 0.60F + random(shardSeed + 23) * 0.65F;
            int sides = 5 + (int) (random(shardSeed + 29) * 3.0F);
            Vec3[] bottom = new Vec3[sides];
            Vec3[] rim = new Vec3[sides];
            for (int side = 0; side < sides; ++side) {
                long sideSeed = shardSeed + side * 83L;
                double a = angle + (side + (random(sideSeed + 37) - 0.5) * 0.20) * Math.PI * 2.0 / sides;
                double r = width * (0.80 + random(sideSeed + 41) * 0.25);
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r * ellipticity;
                double shoulderY = height * (shoulder + (random(sideSeed + 43) - 0.5) * 0.08);
                bottom[side] = new Vec3(x * 0.58, 0, z * 0.58);
                rim[side] = new Vec3(x + tipX * shoulderY / height, shoulderY, z + tipZ * shoulderY / height);
            }
            Vec3 tip = new Vec3(tipX, height, tipZ);
            List<CrystalVertex> mesh = new ArrayList<>(sides * 9);
            for (int side = 0; side < sides; ++side) {
                int next = (side + 1) % sides;
                // 基底 (切向, 法线, 副切向) 保持右手系，六种附着朝向都使用相同的逆时针绕序。
                Vec3 faceNormal = rim[side].subtract(bottom[side]).cross(rim[next].subtract(bottom[side])).normalize();
                Vec3 worldNormal = tangent.scale(faceNormal.x).add(normal.scale(faceNormal.y)).add(bitangent.scale(faceNormal.z));
                float facet = 0.45F + 0.55F * (float) Math.max(0.0, worldNormal.dot(new Vec3(0.36, 0.8, 0.48)));
                addVertex(mesh, bottom[side], 0, 0, facet, tangent, normal, bitangent);
                addVertex(mesh, rim[side], 0, (float) (rim[side].y / height), facet, tangent, normal, bitangent);
                addVertex(mesh, rim[next], 1, (float) (rim[next].y / height), facet, tangent, normal, bitangent);
                addVertex(mesh, bottom[side], 0, 0, facet, tangent, normal, bitangent);
                addVertex(mesh, rim[next], 1, (float) (rim[next].y / height), facet, tangent, normal, bitangent);
                addVertex(mesh, bottom[next], 1, 0, facet, tangent, normal, bitangent);
                addVertex(mesh, rim[side], 0, (float) (rim[side].y / height), facet, tangent, normal, bitangent);
                addVertex(mesh, tip, 0.5F, 1, facet, tangent, normal, bitangent);
                addVertex(mesh, rim[next], 1, (float) (rim[next].y / height), facet, tangent, normal, bitangent);
            }
            shards.add(new Shard(mesh.toArray(CrystalVertex[]::new),
                    clusterDelay + random(shardSeed + 53) * 18.0F, 11.0F + random(shardSeed + 59) * 27.0F));
        }
        return new Crystal(root, born, shards);
    }

    private static void addVertex(List<CrystalVertex> mesh, Vec3 local, float u, float v, float facet,
            Vec3 tangent, Vec3 normal, Vec3 bitangent) {
        // 固定形状和方向只在落点出生时计算；逐帧等比长大和淡入，不重新抽取随机数。
        mesh.add(new CrystalVertex(tangent.scale(local.x).add(bitangent.scale(local.z)),
                normal.scale(local.y), u, v, facet));
    }

    private static void emitVertex(BufferBuilder builder, Vec3 root, CrystalVertex v, float growth, float fade) {
        // 侧向倾斜与法向高度同比增长，刚出生时也不会突然横躺在附着面上。
        float widthGrowth = growth;
        builder.vertex(root.x + v.lateral.x * widthGrowth + v.axial.x * growth,
                root.y + v.lateral.y * widthGrowth + v.axial.y * growth,
                root.z + v.lateral.z * widthGrowth + v.axial.z * growth)
                .color(v.facet, v.facet, v.facet, fade).uv(v.u, v.v).endVertex();
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float random(long key) {
        key = (key ^ (key >>> 30)) * 0xBF58476D1CE4E5B9L;
        key = (key ^ (key >>> 27)) * 0x94D049BB133111EBL;
        return ((key ^ (key >>> 31)) & 0xFFFFFFL) / 16777216.0F;
    }

    private record SurfaceKey(Direction face, int u, int v) {}
    private record Candidate(SurfaceKey key, Vec3 root, long seed) {}
    private record Crystal(Vec3 root, long born, List<Shard> shards) {}
    private record Shard(CrystalVertex[] vertices, float delayTicks, float durationTicks) {}
    private record CrystalVertex(Vec3 lateral, Vec3 axial, float u, float v, float facet) {}
}
