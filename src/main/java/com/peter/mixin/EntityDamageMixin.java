package com.peter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.peter.HideAndSeek;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import net.minecraft.entity.damage.DamageSource;

@Mixin({ LivingEntity.class })
public abstract class EntityDamageMixin {

	@Inject(method = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V", at = @At(value = "HEAD"), cancellable = true)
	private void onDamage(DamageSource source, float amount, CallbackInfo info) {
		if (HideAndSeek.manager.checkDamage((Entity) (Object) this, source)) {
			// info.setReturnValue(false);
			info.cancel();
		}
	}
}