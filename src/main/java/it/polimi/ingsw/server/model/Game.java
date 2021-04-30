package it.polimi.ingsw.server.model;

import it.polimi.ingsw.server.observer.Observable;

import java.util.*;

public class Game extends Observable {
    private final LeaderDeck leaderDeck;
    private final DevDeck[] devDecks;
    private final List<Player> players;
    private final List<Player> winners;
    private final Market market;
    private boolean finalTurn;

    public Game() {
        leaderDeck = new LeaderDeck();
        devDecks = new DevDeck[12];
        int j = 0;
        for (int i = 1; i <= 3; i++) {
            for (Colour colour : Colour.values()) {
                devDecks[j] = new DevDeck(i, colour);
                j++;
            }
        }
        market = new Market();
        players = new ArrayList<>();
        winners = new ArrayList<>();
    }

    public void addPlayer(Player newPlayer) {
        players.add(newPlayer);
    }

    public Player getPlayerByNickname(String nickname) {
        for (Player player : players) {
            if (nickname.equals(player.getNickname())) {
                return player;
            }
        }
        return null;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public DevDeck[] getDevDecks() {
        return Arrays.copyOf(devDecks, devDecks.length);
    }

    public LeaderDeck getLeaderDeck() {
        return leaderDeck;
    }

    public DevCard drawDevCard(Colour colour, int level) {
        return devDecks[(level - 1) * Colour.values().length + colour.ordinal()].drawCard();
    }

    public Market getMarket() {
        return market;
    }

    public boolean isFinalTurn() {
        return finalTurn;
    }

    public void setFinalTurn(boolean finalTurn) {
        this.finalTurn = finalTurn;
    }

    public void setWinners(List<Player> winners) {
        this.winners.addAll(winners);
    }

    public List<Player> getWinners() {
        return winners;
    }
}