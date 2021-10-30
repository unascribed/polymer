package eu.pb4.polymer.mixin.entity;

import eu.pb4.polymer.api.utils.PolymerUtils;
import eu.pb4.polymer.api.entity.PolymerEntity;
import eu.pb4.polymer.api.item.PolymerItemUtils;
import eu.pb4.polymer.impl.other.InternalEntityHelpers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public class EntityTrackerUpdateS2CPacketMixin {
    @Shadow @Mutable
    private List<DataTracker.Entry<?>> trackedValues;

    @Inject(method = "<init>(ILnet/minecraft/entity/data/DataTracker;Z)V", at = @At("TAIL"))
    private void polymer_removeInvalidEntries(int id, DataTracker tracker, boolean forceUpdateAll, CallbackInfo ci) {
        Entity entity = ((DataTrackerAccessor) tracker).getTrackedEntity();

        if (entity instanceof PolymerEntity polymerEntity) {
            List<DataTracker.Entry<?>> legalTrackedData = InternalEntityHelpers.getExampleTrackedDataOfEntityType((polymerEntity.getPolymerEntityType()));

            if (legalTrackedData.size() > 0) {
                List<DataTracker.Entry<?>> entries = new ArrayList<>();
                for (DataTracker.Entry<?> entry : this.trackedValues) {
                    for (DataTracker.Entry<?> trackedData : legalTrackedData) {
                        if (trackedData.getData().getId() == entry.getData().getId() && entry.get().getClass().isInstance(trackedData.get())) {
                            entries.add(entry);
                            break;
                        }
                    }
                }

                polymerEntity.modifyTrackedData(entries);
                this.trackedValues = entries;
            } else {
                List<DataTracker.Entry<?>> list = new ArrayList<>();
                for (DataTracker.Entry<?> entry : this.trackedValues) {
                    if (entry.getData().getId() <= 13) {
                        list.add(entry);
                    }
                }

                polymerEntity.modifyTrackedData(list);
                this.trackedValues = list;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "getTrackedValues", at = @At("RETURN"), cancellable = true)
    private void polymer_replaceItemsWithVirtualOnes(CallbackInfoReturnable<List<DataTracker.Entry<?>>> cir) {
        if (MinecraftClient.getInstance().getServer() != null) {
            List<DataTracker.Entry<?>> list = new ArrayList<>();
            ServerPlayerEntity player = PolymerUtils.getPlayer();

            for (DataTracker.Entry<?> entry : cir.getReturnValue()) {
                if (entry.get() instanceof ItemStack stack) {
                    list.add(new DataTracker.Entry(entry.getData(), PolymerItemUtils.getPolymerItemStack(stack, player)));
                } else {
                    list.add(entry);
                }
            }

            cir.setReturnValue(list);
        }
    }}
