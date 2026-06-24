package com.mengsama.mod.mengsamanetmusic.block;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModItems;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.NetworkHooks;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class PortableMusicPlayerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    protected static final VoxelShape SHAPE_NORTH = Block.box(2, 0, 8, 14, 12, 12);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(2, 0, 4, 14, 12, 8);
    protected static final VoxelShape SHAPE_EAST = Block.box(8, 0, 2, 12, 12, 14);
    protected static final VoxelShape SHAPE_WEST = Block.box(4, 0, 2, 8, 12, 14);

    public PortableMusicPlayerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(0.5f).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortableMusicPlayerBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PortableMusicPlayerBlockEntity player) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {

                if (tag.contains("Item")) {
                    player.getPlayerInv().deserializeNBT(tag.getCompound("Item"));
                }
                if (tag.contains("PlayIndex")) {
                    player.setPlayIndex(tag.getInt("PlayIndex"));
                }
                if (tag.contains("PlayMode")) {
                    player.setPlayMode(PlayMode.getMode(tag.getInt("PlayMode")));
                }
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof PortableMusicPlayerBlockEntity te) {
            ItemStack stackInSlot = te.getCurrentCd();
            if (!stackInSlot.isEmpty()) {
                if (te.isPlay()) {
                    return 15;
                }
                return 7;
            }
        }
        return 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos blockPos, Block block, BlockPos fromPos, boolean isMoving) {
        playerMusic(level, blockPos, level.hasNeighborSignal(blockPos));
    }

    private static void playerMusic(Level level, BlockPos blockPos, boolean signal) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof PortableMusicPlayerBlockEntity player) {
            if (signal != player.hasSignal()) {
                player.setSignal(signal);
                if (signal) {
                    ItemStack currentCd = player.getCurrentCd();
                    if (currentCd.isEmpty()) {
                        player.markDirty();
                        return;
                    }
                    SongInfo songInfo;
                    if (currentCd.getItem() instanceof MusicListItem) {
                        songInfo = MusicListItem.getSongInfo(currentCd);
                    } else {
                        songInfo = MusicCDItem.getSongInfo(currentCd);
                    }
                    if (songInfo != null) {
                        player.setPlayToClient(songInfo);
                    }
                }
                player.markDirty();
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player playerIn, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity te = worldIn.getBlockEntity(pos);
        if (!(te instanceof PortableMusicPlayerBlockEntity musicPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = playerIn.getMainHandItem();
        boolean isCD = heldItem.getItem() instanceof MusicCDItem || heldItem.getItem() instanceof MusicListItem;

        if (!worldIn.isClientSide) {
            if (isCD) {
                for (int i = 0; i < musicPlayer.getPlayerInv().getSlots(); i++) {
                    if (musicPlayer.getPlayerInv().getStackInSlot(i).isEmpty()) {
                        ItemStack copy = heldItem.copy();
                        copy.setCount(1);
                        musicPlayer.getPlayerInv().insertItem(i, copy, false);
                        if (!playerIn.isCreative()) {
                            heldItem.shrink(1);
                        }
                        musicPlayer.markDirty();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        if (!worldIn.isClientSide && playerIn instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (windowId, playerInv, p) -> new com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerPlaylistMenu(
                            windowId, playerInv, musicPlayer),
                    Component.translatable("item.mengsamanetmusic.music_player")
            ), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PortableMusicPlayerBlockEntity musicPlayer) {
                if (!level.isClientSide) {

                    ModNetwork.sendToNearby(level, pos, new StopMusicPacketClient(-1, ""));

                    if (!isMoving) {
                        ItemStack dropStack = new ItemStack(ModItems.MUSIC_PLAYER.get());
                        CompoundTag tag = dropStack.getOrCreateTag();
                        tag.put("Item", musicPlayer.getPlayerInv().serializeNBT());
                        tag.putInt("PlayIndex", musicPlayer.getPlayIndex());
                        tag.putInt("PlayMode", musicPlayer.getPlayMode().ordinal());
                        Block.popResource(level, pos, dropStack);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> entityType) {
        return !level.isClientSide ? createTickerHelper(entityType, ModBlockEntities.PORTABLE_MUSIC_PLAYER.get(), PortableMusicPlayerBlockEntity::tick) : null;
    }

    @Nullable
    @SuppressWarnings("all")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> entityType, BlockEntityType<E> type, BlockEntityTicker<? super E> ticker) {
        return type == entityType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
