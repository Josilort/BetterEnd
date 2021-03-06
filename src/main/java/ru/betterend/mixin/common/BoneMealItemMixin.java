package ru.betterend.mixin.common;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import ru.betterend.registry.EndBlocks;
import ru.betterend.registry.EndTags;
import ru.betterend.util.BlocksHelper;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
	private static final Direction[] DIR = BlocksHelper.makeHorizontal();
	private static final Mutable POS = new Mutable();

	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void beOnUse(ItemUsageContext context, CallbackInfoReturnable<ActionResult> info) {
		World world = context.getWorld();
		BlockPos blockPos = context.getBlockPos();
		if (!world.isClient) {
			if (world.getBlockState(blockPos).isIn(EndTags.END_GROUND)) {
				boolean consume = false;
				if (world.getBlockState(blockPos).getBlock() == Blocks.END_STONE) {
					BlockState nylium = beGetNylium(world, blockPos);
					if (nylium != null) {
						BlocksHelper.setWithoutUpdate(world, blockPos, nylium);
						consume = true;
					}
				}
				else {
					consume = beGrowGrass(world, blockPos);
				}
				if (consume) {
					if (!context.getPlayer().isCreative())
						context.getStack().decrement(1);
					world.syncWorldEvent(2005, blockPos, 0);
					info.setReturnValue(ActionResult.SUCCESS);
					info.cancel();
				}
			}
		}
	}
	
	private boolean beGrowGrass(World world, BlockPos pos) {
		int y1 = pos.getY() + 3;
		int y2 = pos.getY() - 3;
		boolean result = false;
		for (int i = 0; i < 64; i++) {
			int x = (int) (pos.getX() + world.random.nextGaussian() * 2);
			int z = (int) (pos.getZ() + world.random.nextGaussian() * 2);
			POS.setX(x);
			POS.setZ(z);
			for (int y = y1; y >= y2; y--) {
				POS.setY(y);
				BlockPos down = POS.down();
				if (world.isAir(POS) && !world.isAir(down)) {
					BlockState grass = beGetGrassState(world, down);
					if (grass != null) {
						BlocksHelper.setWithoutUpdate(world, POS, grass);
						result = true;
					}
					break;
				}
			}
		}
		return result;
	}
	
	private BlockState beGetGrassState(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == EndBlocks.END_MOSS || block == EndBlocks.END_MYCELIUM) {
			return world.random.nextBoolean() ? EndBlocks.CREEPING_MOSS.getDefaultState() : EndBlocks.UMBRELLA_MOSS.getDefaultState();
		}
		else if (block == EndBlocks.CAVE_MOSS) {
			return EndBlocks.CAVE_GRASS.getDefaultState();
		}
		else if (block == EndBlocks.CHORUS_NYLIUM) {
			return EndBlocks.CHORUS_GRASS.getDefaultState();
		}
		return null;
	}

	private void beShuffle(Random random) {
		for (int i = 0; i < 4; i++) {
			int j = random.nextInt(4);
			Direction d = DIR[i];
			DIR[i] = DIR[j];
			DIR[j] = d;
		}
	}

	private BlockState beGetNylium(World world, BlockPos pos) {
		beShuffle(world.random);
		for (Direction dir : DIR) {
			BlockState state = world.getBlockState(pos.offset(dir));
			if (BlocksHelper.isEndNylium(state))
				return state;
		}
		return null;
	}
}