package com.tibolatte.greeny.worldgen.tree;

import com.mojang.serialization.Codec;
import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class GreenyTreeDecorator extends TreeDecorator {

    public static final GreenyTreeDecorator INSTANCE = new GreenyTreeDecorator();
    public static final Codec<GreenyTreeDecorator> CODEC = Codec.unit(() -> INSTANCE);

    public static final DeferredRegister<TreeDecoratorType<?>> DECORATOR_TYPES = DeferredRegister.create(ForgeRegistries.TREE_DECORATOR_TYPES, Greeny.MODID);
    public static final RegistryObject<TreeDecoratorType<GreenyTreeDecorator>> GREENY_DECORATOR = DECORATOR_TYPES.register("greeny_decorator", () -> new TreeDecoratorType<>(CODEC));

    @Override
    protected TreeDecoratorType<?> type() { return GREENY_DECORATOR.get(); }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        List<BlockPos> logs = context.logs();
        if (logs.isEmpty()) return;

        // 1. ANALYZE THE SKELETON
        logs.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
        BlockPos basePos = logs.get(0);
        BlockPos topPos = logs.get(logs.size() - 1);
        int height = topPos.getY() - basePos.getY();

        // 2. THE HEART (Hidden Deep)
        // We hide it in the lower-middle section, encased in wood.
        if (height > 5) {
            BlockPos heartPos = logs.get(height / 3);
            context.setBlock(heartPos, BlockRegistry.AXIOM_HEART_CORE.get().defaultBlockState());
        }

        // 3. THE CLAW ROOTS (The Anchor)
        // These are massive roots that start high up (2-3 blocks) and arch down into the ground.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            // 75% chance per side
            if (random.nextFloat() < 0.75f) {
                // Start 2 blocks up the trunk
                BlockPos start = basePos.above(2).relative(dir);
                generateClawRoot(context, start, dir, random);
            }
        }

        // 4. THE CHALICE CROWN (The Antenna)
        // Instead of random branches, we generate 4 main "Arms" that spiral out and up.
        // This creates a majestic "bowl" shape at the top.
        generateChaliceArm(context, topPos, Direction.NORTH, random);
        generateChaliceArm(context, topPos, Direction.SOUTH, random);
        generateChaliceArm(context, topPos, Direction.EAST, random);
        generateChaliceArm(context, topPos, Direction.WEST, random);
    }

    // --- GENERATORS ---

    private void generateClawRoot(Context context, BlockPos start, Direction dir, RandomSource random) {
        BlockPos.MutableBlockPos curr = start.mutable();
        // Arch OUT then DOWN
        int length = 3 + random.nextInt(2);

        for (int i = 0; i < length + 2; i++) {
            // Place Ancient Root
            if (canReplace(context, curr)) {
                context.setBlock(curr, BlockRegistry.ANCIENT_ROOT.get().defaultBlockState());
                // Fill below to make it look solid
                if (canReplace(context, curr.below())) {
                    context.setBlock(curr.below(), BlockRegistry.ANCIENT_ROOT.get().defaultBlockState());
                }
            }

            // Movement Logic: Arching
            if (i == 0) curr.move(dir); // Move out
            else curr.move(Direction.DOWN).move(dir); // Move down and out (diagonal)

            // If we hit ground, stop
            if (!canReplace(context, curr.below())) break;
        }
    }

    private void generateChaliceArm(Context context, BlockPos start, Direction dir, RandomSource random) {
        BlockPos.MutableBlockPos curr = start.mutable();
        BlockState log = BlockRegistry.WHISPERING_OAK_LOG.get().defaultBlockState();

        // Arms are long: 5 to 7 blocks
        int length = 5 + random.nextInt(3);

        for (int i = 0; i < length; i++) {
            // Shape: Curve OUT, then go purely UP
            if (i < 2) {
                curr.move(dir).move(Direction.UP); // Diagonal Out/Up
            } else {
                curr.move(Direction.UP); // Straight Up like a pillar
                // Slight twist
                if (random.nextBoolean()) curr.move(dir.getClockWise());
            }

            if (canReplace(context, curr)) {
                context.setBlock(curr, log);
            }
        }

        // MASSIVE LEAF CLOUD AT THE TIP
        generateLeafCloud(context, curr, random);
    }

    private void generateLeafCloud(Context context, BlockPos center, RandomSource random) {
        BlockState leaves = BlockRegistry.WHISPERING_OAK_LEAVES.get().defaultBlockState();
        // A flattened sphere (Cloud)
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    // Distance check for rounded shape
                    if (x*x + y*y*2 + z*z <= 8 + random.nextInt(2)) {
                        if (random.nextFloat() > 0.2f) { // 80% density
                            BlockPos pos = center.offset(x, y, z);
                            if (canReplace(context, pos)) {
                                context.setBlock(pos, leaves);
                            }
                        }
                    }
                }
            }
        }
    }

    // Safe replacement check
    private boolean canReplace(Context context, BlockPos pos) {
        return context.isAir(pos) || !context.level().isStateAtPosition(pos, BlockState::canOcclude);
    }
}