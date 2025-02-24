package com.gildedgames.aether;

import com.gildedgames.aether.client.AetherClient;
import com.gildedgames.aether.client.registry.AetherParticleTypes;
import com.gildedgames.aether.client.registry.AetherSoundEvents;
import com.gildedgames.aether.common.block.util.dispenser.DispenseDartBehavior;
import com.gildedgames.aether.common.entity.projectile.weapon.*;
import com.gildedgames.aether.common.item.materials.util.ISwetBallConversion;
import com.gildedgames.aether.common.item.miscellaneous.bucket.SkyrootWaterBucketItem;
import com.gildedgames.aether.core.AetherConfig;
import com.gildedgames.aether.common.registry.AetherRecipes;
import com.gildedgames.aether.core.data.*;
import com.gildedgames.aether.common.entity.tile.AltarTileEntity;
import com.gildedgames.aether.common.entity.tile.FreezerTileEntity;
import com.gildedgames.aether.common.registry.AetherAdvancements;
import com.gildedgames.aether.core.capability.AetherCapabilities;

import com.gildedgames.aether.core.network.AetherPacketHandler;
import com.gildedgames.aether.common.registry.*;
import com.gildedgames.aether.common.registry.AetherDimensions;
import com.gildedgames.aether.common.registry.AetherFeatures;
import com.gildedgames.aether.core.data.AetherLootTableData;
import net.minecraft.block.*;
import net.minecraft.data.DataGenerator;
import net.minecraft.dispenser.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.projectile.*;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.DispenserTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

import java.util.Random;

@Mod(Aether.MODID)
public class Aether
{
	public static final String MODID = "aether";
	public static final Logger LOGGER = LogManager.getLogger();

	public Aether() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::curiosSetup);
		modEventBus.addListener(this::dataSetup);

		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.addListener(EventPriority.NORMAL, AetherStructures::addDimensionalSpacing);

		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AetherClient::clientInitialization);
		
		DeferredRegister<?>[] registers = {
				AetherBlocks.BLOCKS,
				AetherEntityTypes.ENTITIES,
				AetherEffects.EFFECTS,
				AetherItems.ITEMS,
				AetherFeatures.FEATURES,
				AetherParticleTypes.PARTICLES,
				AetherPOI.POI,
				AetherSoundEvents.SOUNDS,
				AetherContainerTypes.CONTAINERS,
				AetherTileEntityTypes.TILE_ENTITIES,
				AetherRecipes.RECIPE_SERIALIZERS,
				AetherStructures.STRUCTURES
		};

		for (DeferredRegister<?> register : registers) {
			register.register(modEventBus);
		}

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AetherConfig.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AetherConfig.CLIENT_SPEC);
	}

	public void commonSetup(FMLCommonSetupEvent event) {
		AetherCapabilities.register();
		event.enqueueWork(() -> {
			AetherPacketHandler.register();

			AetherBlocks.registerPots();
			AetherBlocks.registerAxeStrippingBlocks();
			AetherBlocks.registerHoeTillingBlocks();
			AetherBlocks.registerFlammability();
			AetherBlocks.registerWoodTypes();
			AetherBlocks.registerFreezables();

			AetherEntityTypes.registerSpawnPlacements();

			AetherItems.registerAbilities();

			AetherLoot.load();

			AetherFeatures.registerConfiguredFeatures();
			AetherAdvancements.init();

			AetherStructures.registerStructures();
			AetherStructures.registerConfiguredStructures();

			registerDispenserBehaviors();
			registerComposting();
			registerFuels();
		});
	}

	public void curiosSetup(InterModEnqueueEvent event) {
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.NECKLACE.getMessageBuilder().icon(new ResourceLocation(Aether.MODID, "gui/slots/pendant")).build());
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.BACK.getMessageBuilder().icon(new ResourceLocation(Aether.MODID, "gui/slots/cape")).build());
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.RING.getMessageBuilder().icon(new ResourceLocation(Aether.MODID, "gui/slots/ring")).size(2).build());
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder("shield").icon(new ResourceLocation(Aether.MODID, "gui/slots/shield")).build());
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.HANDS.getMessageBuilder().icon(new ResourceLocation(Aether.MODID, "gui/slots/gloves")).build());
		InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> SlotTypePreset.CHARM.getMessageBuilder().icon(new ResourceLocation(Aether.MODID, "gui/slots/misc")).size(2).build());
	}

	public void dataSetup(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper helper = event.getExistingFileHelper();
		if (event.includeClient()) {
			generator.addProvider(new AetherBlockStateData(generator, helper));
			generator.addProvider(new AetherItemModelData(generator, helper));
			generator.addProvider(new AetherLangData(generator));
		}
		if (event.includeServer()) {
			generator.addProvider(new AetherRecipeData(generator));
			generator.addProvider(new AetherLootTableData(generator));
			AetherBlockTagData blockTags = new AetherBlockTagData(generator, helper);
			generator.addProvider(blockTags);
			generator.addProvider(new AetherItemTagData(generator, blockTags, helper));
			generator.addProvider(new AetherEntityTagData(generator, helper));
			generator.addProvider(new AetherFluidTagData(generator, helper));
			generator.addProvider(new AetherAdvancementData(generator));
		}
	}

	private void registerDispenserBehaviors() {
		DispenserBlock.registerBehavior(AetherItems.GOLDEN_DART.get(), new DispenseDartBehavior(AetherItems.GOLDEN_DART));
		DispenserBlock.registerBehavior(AetherItems.POISON_DART.get(), new DispenseDartBehavior(AetherItems.POISON_DART));
		DispenserBlock.registerBehavior(AetherItems.ENCHANTED_DART.get(), new DispenseDartBehavior(AetherItems.ENCHANTED_DART));
		DispenserBlock.registerBehavior(AetherItems.LIGHTNING_KNIFE.get(), new ProjectileDispenseBehavior()
		{
			@Override
			protected ProjectileEntity getProjectile(World world, IPosition position, ItemStack stack) {
				return Util.make(new LightningKnifeEntity(world), (projectile) -> {
					projectile.setPos(position.x(), position.y(), position.z());
					projectile.setItem(stack);
				});
			}

			@Override
			protected float getUncertainty() {
				return 1.5F;
			}
		});
		DispenserBlock.registerBehavior(AetherItems.HAMMER_OF_NOTCH.get(), new ProjectileDispenseBehavior()
		{
			@Override
			public ItemStack execute(IBlockSource blockSource, ItemStack stack) {
				World world = blockSource.getLevel();
				IPosition iposition = DispenserBlock.getDispensePosition(blockSource);
				Direction direction = blockSource.getBlockState().getValue(DispenserBlock.FACING);
				ProjectileEntity projectileentity = this.getProjectile(world, iposition, stack);
				projectileentity.shoot(direction.getStepX(), (float) direction.getStepY(), direction.getStepZ(), this.getPower(), this.getUncertainty());
				world.addFreshEntity(projectileentity);
				int damage = stack.getDamageValue();
				stack.setDamageValue(damage + 1);
				if (stack.getDamageValue() >= stack.getMaxDamage()) {
					stack.shrink(1);
				}
				return stack;
			}

			@Override
			protected ProjectileEntity getProjectile(World world, IPosition position, ItemStack stack) {
				HammerProjectileEntity hammerProjectile = new HammerProjectileEntity(world);
				hammerProjectile.setPos(position.x(), position.y(), position.z());
				return hammerProjectile;
			}

			@Override
			protected float getUncertainty() {
				return 1.0F;
			}
		});
		DispenserBlock.registerBehavior(AetherItems.SKYROOT_WATER_BUCKET.get(), new DefaultDispenseItemBehavior()
		{
			private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

			@Override
			public ItemStack execute(IBlockSource source, ItemStack stack) {
				SkyrootWaterBucketItem bucketItem = (SkyrootWaterBucketItem) stack.getItem();
				World world = source.getLevel();
				BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
				if (bucketItem.tryPlaceContainedLiquid(null, world, blockpos, null)) {
					return new ItemStack(AetherItems.SKYROOT_BUCKET.get());
				} else {
					return this.defaultDispenseItemBehavior.dispense(source, stack);
				}
			}
		});
		DispenserBlock.registerBehavior(AetherItems.SKYROOT_BUCKET.get(), new DefaultDispenseItemBehavior()
		{
			private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

			@Override
			public ItemStack execute(IBlockSource source, ItemStack stack) {
				IWorld iworld = source.getLevel();
				BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
				BlockState blockstate = iworld.getBlockState(blockpos);
				Block block = blockstate.getBlock();
				if (block instanceof IBucketPickupHandler) {
					Fluid fluid = ((IBucketPickupHandler)block).takeLiquid(iworld, blockpos, blockstate);
					if (!(fluid instanceof FlowingFluid)) {
						return super.execute(source, stack);
					} else {
						if (fluid == Fluids.WATER) {
							Item item = AetherItems.SKYROOT_WATER_BUCKET.get();
							stack.shrink(1);
							if (stack.isEmpty()) {
								return new ItemStack(item);
							} else {
								if (source.<DispenserTileEntity>getEntity().addItem(new ItemStack(item)) < 0) {
									this.defaultDispenseItemBehavior.dispense(source, new ItemStack(item));
								}
								return stack;
							}
						} else {
							return super.execute(source, stack);
						}
					}
				} else {
					return super.execute(source, stack);
				}
			}
		});
		DispenserBlock.registerBehavior(AetherItems.AMBROSIUM_SHARD.get(), new OptionalDispenseBehavior()
		{
			@Override
			protected ItemStack execute(IBlockSource source, ItemStack stack) {
				this.setSuccess(true);
				World world = source.getLevel();
				BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
				BlockState blockstate = world.getBlockState(blockpos);
				if (blockstate.getBlock().is(AetherTags.Blocks.ENCHANTABLE_GRASS_BLOCKS)) {
					world.setBlockAndUpdate(blockpos, AetherBlocks.ENCHANTED_AETHER_GRASS_BLOCK.get().defaultBlockState());
					stack.shrink(1);
				} else {
					this.setSuccess(false);
				}
				return stack;
			}
		});
		DispenserBlock.registerBehavior(AetherItems.SWET_BALL.get(), new OptionalDispenseBehavior()
		{
			@Override
			protected ItemStack execute(IBlockSource source, ItemStack stack) {
				this.setSuccess(true);
				World world = source.getLevel();
				BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
				if (!ISwetBallConversion.convertBlockWithoutContext(world, blockpos, stack)) {
					this.setSuccess(false);
				}
				return stack;
			}
		});
		IDispenseItemBehavior dispenseSpawnEgg = new DefaultDispenseItemBehavior()
		{
			@Override
			public ItemStack execute(IBlockSource source, ItemStack stack) {
				Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
				EntityType<?> entityType = ((SpawnEggItem)stack.getItem()).getType(stack.getTag());
				entityType.spawn(source.getLevel(), stack, null, source.getPos().relative(direction), SpawnReason.DISPENSER, direction != Direction.UP, false);
				stack.shrink(1);
				return stack;
			}
		};
		for (RegistryObject<Item> item : AetherItems.ITEMS.getEntries()) {
			if (item.get() instanceof SpawnEggItem) {
				DispenserBlock.registerBehavior(item.get(), dispenseSpawnEgg);
			}
		}
		DispenserBlock.registerBehavior(Items.FIRE_CHARGE, new OptionalDispenseBehavior()
		{
			@Override
			public ItemStack execute(IBlockSource source, ItemStack stack) {
				World world = source.getLevel();
				if (world.dimension() == AetherDimensions.AETHER_WORLD) {
					this.setSuccess(false);
				}
				else {
					this.setSuccess(true);
					Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
					IPosition iposition = DispenserBlock.getDispensePosition(source);
					double d0 = iposition.x() + direction.getStepX() * 0.3F;
					double d1 = iposition.y() + direction.getStepY() * 0.3F;
					double d2 = iposition.z() + direction.getStepZ() * 0.3F;
					Random random = world.random;
					double d3 = random.nextGaussian() * 0.05 + direction.getStepX();
					double d4 = random.nextGaussian() * 0.05 + direction.getStepY();
					double d5 = random.nextGaussian() * 0.05 + direction.getStepZ();
					world.addFreshEntity(Util.make(new SmallFireballEntity(world, d0, d1, d2, d3, d4, d5), (entity) -> entity.setItem(stack)));
					stack.shrink(1);
				}
				return stack;
			}

			@Override
			protected void playSound(IBlockSource source) {
				source.getLevel().levelEvent(this.isSuccess()? 1018 : 1001, source.getPos(), 0);
			}
		});
		DispenserBlock.registerBehavior(Items.FLINT_AND_STEEL, new OptionalDispenseBehavior()
		{
			@Override
			protected ItemStack execute(IBlockSource source, ItemStack stack) {
				World world = source.getLevel();
				if (world.dimension() == AetherDimensions.AETHER_WORLD) {
					this.setSuccess(false);
				}
				else {
					this.setSuccess(true);
					BlockPos blockpos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
					BlockState blockstate = world.getBlockState(blockpos);
					Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
					if (AbstractFireBlock.canBePlacedAt(world, blockpos, direction)) {
						world.setBlockAndUpdate(blockpos, Blocks.FIRE.defaultBlockState());
					}
					else if (CampfireBlock.canLight(blockstate)) {
						world.setBlockAndUpdate(blockpos, blockstate.setValue(BlockStateProperties.LIT, true));
					}
					else if (blockstate.isFlammable(world, blockpos, source.getBlockState().getValue(DispenserBlock.FACING).getOpposite())) {
						blockstate.catchFire(world, blockpos, source.getBlockState().getValue(DispenserBlock.FACING).getOpposite(), null);
						if (blockstate.getBlock() instanceof TNTBlock) {
							world.removeBlock(blockpos, false);
						}
					}
					else {
						this.setSuccess(false);
					}

					if (this.isSuccess() && stack.hurt(1, world.random, null)) {
						stack.setCount(0);
					}
				}
				return stack;
			}
		});
	}

	private void registerComposting() {
		ComposterBlock.add(0.3F, AetherBlocks.SKYROOT_LEAVES.get());
		ComposterBlock.add(0.3F, AetherBlocks.SKYROOT_SAPLING.get());
		ComposterBlock.add(0.3F, AetherBlocks.GOLDEN_OAK_LEAVES.get());
		ComposterBlock.add(0.3F, AetherBlocks.GOLDEN_OAK_SAPLING.get());
		ComposterBlock.add(0.3F, AetherBlocks.CRYSTAL_LEAVES.get());
		ComposterBlock.add(0.3F, AetherBlocks.CRYSTAL_FRUIT_LEAVES.get());
		ComposterBlock.add(0.3F, AetherBlocks.HOLIDAY_LEAVES.get());
		ComposterBlock.add(0.3F, AetherBlocks.DECORATED_HOLIDAY_LEAVES.get());
		ComposterBlock.add(0.3F, AetherItems.BLUE_BERRY.get());
		ComposterBlock.add(0.5F, AetherItems.ENCHANTED_BERRY.get());
		ComposterBlock.add(0.5F, AetherBlocks.BERRY_BUSH.get());
		ComposterBlock.add(0.5F, AetherBlocks.BERRY_BUSH_STEM.get());
		ComposterBlock.add(0.65F, AetherBlocks.WHITE_FLOWER.get());
		ComposterBlock.add(0.65F, AetherBlocks.PURPLE_FLOWER.get());
		ComposterBlock.add(0.65F, AetherItems.WHITE_APPLE.get());
	}

	private void registerFuels() {
		AltarTileEntity.addItemEnchantingTime(AetherItems.AMBROSIUM_SHARD.get(), 500);

		FreezerTileEntity.addItemFreezingTime(AetherBlocks.ICESTONE.get(), 500);
	}
}
