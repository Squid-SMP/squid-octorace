package org.orsa.octorace.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.orsa.octorace.Octorace.PARTY_MANAGER;

public class Party {
    private PartyManager manager;

    public ServerPlayer owner;
    public List<ServerPlayer> players;
    public List<ServerPlayer> invites;

    public Party(PartyManager manager, ServerPlayer owner) {
        this.manager = manager;
        this.owner = owner;
        players = new ArrayList<>();
        players.add(owner);

        invites = new ArrayList<>();
    }

    public void addPlayer(ServerPlayer player) {
        broadcast(player.getPlainTextName() + " has joined the party.");

        if (!players.contains(player)) {
            players.add(player);
        }

        invites.remove(player);
    }

    public void removePlayer(ServerPlayer player) {
        players.remove(player);
        broadcast(player.getPlainTextName() + " has left the party.");
    }

    protected void broadcast(String msg) {
        Component c = Component.literal(msg);
        for (var player : players) {
            player.sendSystemMessage(c);
        }
    }

    public void disband() {
        broadcast("Party has been disbanded.");
        manager.parties.remove(owner.getPlainTextName());
    }
}
