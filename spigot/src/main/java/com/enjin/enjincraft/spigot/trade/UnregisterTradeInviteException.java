package com.enjin.enjincraft.spigot.trade;

import com.enjin.enjincraft.spigot.player.EnjPlayer;

public class UnregisterTradeInviteException extends RuntimeException {

    public UnregisterTradeInviteException(EnjPlayer inviter, EnjPlayer invitee) {
        super(String.format("Failed to remove trade invites for either inviter (%s) or invitee (%s) where one was suppose to exist",
                inviter.getBukkitPlayer().getName(),
                invitee.getBukkitPlayer().getName()));
    }

}
