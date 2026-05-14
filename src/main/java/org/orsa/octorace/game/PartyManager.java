package org.orsa.octorace.game;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {
    public Map<String,Party> parties = new HashMap<>();

    public void createParty(ServerPlayer owner) {
        var party = new Party(owner);
        parties.put(owner.getPlainTextName(), party);
    }

    public Boolean playerOwnsParty(ServerPlayer player) {
        return parties.containsKey(player.getPlainTextName());
    }

    public Party getPlayerParty(ServerPlayer player) {
        for (Party party : parties.values()) {
            if (party.players.contains(player)) {
                return party;
            }
        }
        return null;
    }

    public void createInvite(ServerPlayer inviter, ServerPlayer invited) {
        var partyName = inviter.getPlainTextName();
        var party = parties.get(partyName);
        party.invites.add(invited);
    }

    public boolean tryJoinParty(ServerPlayer player, ServerPlayer partyOwner) {
        var partyName = partyOwner.getPlainTextName();
        var party = parties.get(partyName);

        if (party == null) {
            return false;
        }

        if (!party.invites.contains(player)) {
            return false;
        }

        party.addPlayer(player);
        return true;
    }
}
