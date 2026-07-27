package com.mengsama.mod.mengsamanetmusic.block;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class MusicPlayerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    protected static final VoxelShape BLOCK_AABB = Block.box(2, 0, 2, 14, 6, 14);
    public static final BooleanProperty CYCLE_DISABLE = BooleanProperty.create("cycle_disable");

    public MusicPlayerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(0.5f).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(CYCLE_DISABLE, true));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MusicPlayerBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CYCLE_DISABLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof MusicPlayerBlockEntity te) {
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
        if (blockEntity instanceof MusicPlayerBlockEntity player) {
            if (signal != player.hasSignal()) {
                player.setSignal(signal);
                if (signal) {
                    ItemStack currentCd = player.getCurrentCd();
                    if (currentCd.isEmpty()) {
                        player.markDirty();
                        return;
                    }
                    SongInfo songInfo = MusicListItem.getSongInfo(currentCd);
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
        if (!(te instanceof MusicPlayerBlockEntity musicPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = playerIn.getMainHandItem();
        boolean isPlaylist = heldItem.getItem() instanceof MusicListItem;

        if (!worldIn.isClientSide) {
            if (isPlaylist) {

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
                    Component.translatable("block.mengsamanetmusic.music_player")
            ), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MusicPlayerBlockEntity musicPlayer) {
                if (!level.isClientSide) {
                    ModNetwork.sendToNearby(level, pos, new StopMusicPacketClient("block:" + level.dimension().location() + ":" + pos.asLong()));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> entityType) {
        return !level.isClientSide ? createTickerHelper(entityType, ModBlockEntities.MUSIC_PLAYER.get(), MusicPlayerBlockEntity::tick) : null;
    }

    @Nullable
    @SuppressWarnings("all")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> entityType, BlockEntityType<E> type, BlockEntityTicker<? super E> ticker) {
        return type == entityType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return BLOCK_AABB;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

}
