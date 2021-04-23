package it.polimi.ingsw.controller;

import it.polimi.ingsw.exceptions.WrongActionException;
import it.polimi.ingsw.model.*;

import java.util.ArrayList;
import java.util.List;

public class DepotEffect implements LeaderEffect{
    private Resource resource;
    @Override
    public void doLeaderEffect(Player player, Action action) throws WrongActionException {
        List<LeaderCard> playerCards = player.getHandLeaderCards();
        boolean check = false;
        for(LeaderCard Lc : playerCards){
            if (Lc.getResource() == resource && Lc.getType() == LeaderType.DEPOT && Lc.isPlayed()) {
                check = true;
                break;
            }
        }
        if(!check) throw new WrongActionException("The player does not have the played leadCard");
        else{
            Warehouse warehouse = player.getBoard().getWarehouse();
            if(warehouse.getWarehouse().size() < NumOfShelf.values().length) {                                          //there is place for a new depot
                if (warehouse.checkDepot(resource))                                                                     //controls if there is already that depot in the Warehouse
                    warehouse.addDepot(resource);
            }
        }
    }
}