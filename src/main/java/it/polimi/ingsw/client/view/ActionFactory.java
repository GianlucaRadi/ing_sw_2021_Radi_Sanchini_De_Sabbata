package it.polimi.ingsw.client.view;

import it.polimi.ingsw.constants.Constants;
import it.polimi.ingsw.messages.Message;
import it.polimi.ingsw.messages.actions.*;
import it.polimi.ingsw.messages.clientMessages.EndTurn;
import it.polimi.ingsw.messages.clientMessages.internal.ChooseAction;
import it.polimi.ingsw.server.controller.leaders.DepotEffect;
import it.polimi.ingsw.server.controller.leaders.DiscountEffect;
import it.polimi.ingsw.server.controller.leaders.LeaderEffect;
import it.polimi.ingsw.server.controller.leaders.MarbleEffect;
import it.polimi.ingsw.server.model.*;
import it.polimi.ingsw.server.model.gameboard.DevSpaceSlot;
import it.polimi.ingsw.server.model.gameboard.NumOfShelf;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class ActionFactory {
    private final PrintStream output;
    private final Scanner input;
    private final CLI cli;

    public ActionFactory(PrintStream output, Scanner input, CLI cli) {
        this.output = output;
        this.input = input;
        this.cli = cli;
    }

    public Message createAction(int actionNumber) {
        switch (actionNumber) {
            case 0:
                return buildBuyResources();
            case 1:
                return buildBuyDevCard();
            case 2:
                return buildStartProduction();
            case 3:
                return buildDiscardLeadCard();
            case 4:
                return buildPlayLeadCard();
            case 5:
                return buildMoveResources();
            case 10:
                return new EndTurn();
        }
        return null;
    }

    private Action buildBuyResources() {
        output.print("Would you like to buy resources from a column or a row? [column/row]\n>");
        String selection = readInputString();
        MarketSelection marketSelection = null;
        while (true) {
            try {
                marketSelection = MarketSelection.valueOf(selection.toUpperCase());
                break;
            } catch (IllegalArgumentException e) {
                output.print("Please insert a valid input:\n>");
                selection = readInputString();
            }
        }
        int source = -1;
        int whiteMarbles;
        int firstType = 0;
        int secondType = 0;
        List<String> marbleToReceive;
        List<String> resToReceive = new ArrayList<>();
        List<LeaderEffect> leaderEffects = new ArrayList<>();
        List<ResourcePosition> result;
        if (marketSelection == MarketSelection.ROW) {
            do {
                output.print("Which row would you like to buy resources from? [1/2/3]\n>");
                source = readInputInt() - 1;
                if (source < 0 || source > 2) output.print("Please select a number between 1 and 3:\n>");
            } while (source < 0 || source > 2);
            whiteMarbles = (int) Arrays.stream(cli.getClientView().getMarket()[source])
                    .filter(s -> s.equalsIgnoreCase("WHITE")).count();
            marbleToReceive = Arrays.stream(cli.getClientView().getMarket()[source])
                    .filter(s -> !s.equalsIgnoreCase("WHITE")).collect(Collectors.toList());
        } else {
            do {
                output.print("Which column would you like to buy resources from? [1/2/3/4]\n>");
                source = readInputInt() - 1;
                if (source < 0 || source > 3) output.print("Please select a number between 1 and 4:\n>");
            } while (source < 0 || source > 3);
            whiteMarbles = (int) Arrays.stream(getColumn(cli.getClientView().getMarket(), source))
                    .filter(s -> s.equalsIgnoreCase("WHITE")).count();
            marbleToReceive = Arrays.stream(getColumn(cli.getClientView().getMarket(), source))
                    .filter(s -> !s.equalsIgnoreCase("WHITE")).collect(Collectors.toList());
        }
        for (String s : marbleToReceive) {
            switch (s.toUpperCase()) {
                case "GREY" -> resToReceive.add("STONE");
                case "YELLOW" -> resToReceive.add("COIN");
                case "BLUE" -> resToReceive.add("SHIELD");
                case "PURPLE" -> resToReceive.add("SERVANT");
                case "RED" -> resToReceive.add("FAITHPOINT");
            }
        }
        result = cli.askForLocation(resToReceive, true, true);
        int marbleLeaderNumber = (int) cli.getClientView().getOwnGameBoard().getPlayedCards()
                .stream().filter(p -> p.getType().equalsIgnoreCase("MARBLE")).count();
        if (marbleLeaderNumber == 2) {
            List<String> res = new ArrayList<>();
            for (LeadCardInfo l : cli.getClientView().getOwnGameBoard().getPlayedCards())
                res.add(l.getResource());
            output.println("You have two marble leaders, respectively of "
                    + res.get(0) + " and " + res.get(1) + " resource type, and " +
                    whiteMarbles + "white marbles.");
            do {
                output.print("How many of them would you like to convert to " + res.get(0) + "s?\n>");
                firstType = readInputInt();
                output.print("How many of them would you like to convert to " + res.get(0) + "s?\n>");
                secondType = readInputInt();
                if (firstType + secondType != whiteMarbles)
                    output.print("Please insert a valid amount (the sum of the resources has to be " +
                            whiteMarbles + ").\n");
            } while (firstType + secondType != whiteMarbles);
            List<String> s1 = new ArrayList<>();
            List<String> s2 = new ArrayList<>();
            for (int i = 0; i < firstType; i++)
                s1.add(res.get(0));
            for (int i = 0; i < firstType; i++)
                s1.add(res.get(1));
            List<ResourcePosition> rp1 = cli.askForLocation(s1, true, true);
            List<ResourcePosition> rp2 = cli.askForLocation(s2, true, true);
            leaderEffects.add(new MarbleEffect(firstType, Resource.valueOf(res.get(0).toUpperCase()), rp1));
            leaderEffects.add(new MarbleEffect(secondType, Resource.valueOf(res.get(1).toUpperCase()), rp2));
        } else if (marbleLeaderNumber == 1) {
            List<String> s1 = new ArrayList<>();
            List<ResourcePosition> rp1 = new ArrayList<>();
            Resource res = Resource.valueOf(cli.getClientView().getOwnGameBoard().getPlayedCards()
                    .stream().filter(p -> p.getType().equalsIgnoreCase("MARBLE"))
                    .collect(Collectors.toList()).get(0).getResource());
            for (int i = 0; i < whiteMarbles; i++)
                s1.add(res.toString());
            rp1 = cli.askForLocation(s1, true, true);
            leaderEffects.add(new MarbleEffect(whiteMarbles, res, rp1));
        }
        return new BuyResources(leaderEffects, source + 1, marketSelection, result);
    }

    private Action buildBuyDevCard() {
        Colour colour = null;
        int lev = -1;
        int slot = -1;
        List<ResourcePosition> res = new ArrayList<>();
        List<LeaderEffect> leaders = new ArrayList<>();
        while (true) {
            while (true) {
                output.print("What colour is the card you would like to buy? [Green/Blue/Yellow/Purple]\n>");
                String col = readInputString();
                try {
                    colour = Colour.valueOf(col);
                    break;
                } catch (InputMismatchException e) {
                    output.println("Please insert a valid colour.");
                }
            }
            do {
                output.print("What level is the card you would like to buy? [1/2/3]\n>");
                lev = readInputInt() - 1;
                if (lev < 0 || lev > 2) output.print("Please insert a valid number.");
            } while (lev < 0 || lev > 2);
            if (cli.getClientView().getDevDecks()[lev][colour.ordinal()] != null) break;
            else
                output.println("The selected deck is empty, please select another one.");
        }
        while (true) {
            output.print("In which slot would you like to put your card? [1/2/3]\n>");
            slot = readInputInt() - 1;
            if (slot >= 0 && slot <= 2 && checkDevSpaceSlot(slot, lev)) break;
            else if (slot < 0 || slot > 2) output.println("Please insert a valid number.");
            else output.println("The selected slot cannot host your card, please choose another one.");
        }
        List<String> req = cli.getClientView().getDevDecks()[lev][colour.ordinal()].getResourceRequirements();
        for (LeadCardInfo l : cli.getClientView().getOwnGameBoard().getPlayedCards()) {
            if (l.getType().equalsIgnoreCase("Discount")) {
                for (String s : req) {
                    if (s.equalsIgnoreCase(l.getResource())) {
                        output.print("You have a leader card which could grant you this card for one less "
                                + l.getResource().toLowerCase() + ", would you like to use this discount? [Y/N]\n>");
                        boolean discount;
                        while (true) {
                            String answer = readInputString();
                            if (answer.equalsIgnoreCase("Y")) {
                                discount = true;
                                break;
                            } else if (answer.equalsIgnoreCase("N")) {
                                discount = false;
                                break;
                            } else output.print("Please insert a valid input.\n>");
                        }
                        if (discount) {
                            req.remove(s);
                            leaders.add(new DiscountEffect(Resource.valueOf(s.toUpperCase())));
                        }
                        break;
                    }
                }
            }
        }
        output.println("Your card has the following requirements:\n" + cli.buildResourceString(req));
        res = cli.askForLocation(req, false, false);
        return new BuyDevCard(lev, colour, DevSpaceSlot.values()[slot], res, leaders);
    }

    private boolean checkDevSpaceSlot(int slot, int lev) {
        return (cli.getClientView().getOwnGameBoard().getDevSpace().get(slot).isEmpty() && lev == 1) ||
                (cli.getClientView().getOwnGameBoard().getDevSpace().get(slot).get(0).getLevel() == lev - 1);
    }

    private Action buildStartProduction() {
        return null;
    }

    private Action buildDiscardLeadCard() {
        output.print("Which leader card do you want do discard? [1/2]\n>");
        int index = readInputInt();
        while (index < 1 || index > 2) {
            output.println("Please select a valid number. [1/2]\n>");
            index = readInputInt();
        }
        while (index > cli.getClientView().getHand().size()) {
            output.print("Your hand does not contain that leader card. Please select a valid number.\n>");
            index = readInputInt();
        }
        return new DiscardLeadCard(index);
    }

    private Action buildPlayLeadCard() {
        output.print("Which leader card do you want do play? [1/2]\n>");
        int index = readInputInt();
        while (index < 1 || index > 2) {
            output.println("Please select a valid number. [1/2]\n>");
            index = readInputInt();
        }
        while (index > cli.getClientView().getHand().size()) {
            output.print("Your hand does not contain that leader card. Please select a valid number.\n>");
            index = readInputInt();
        }
        return new PlayLeadCard(index);
    }

    private Action buildMoveResources() {
        output.print("Please select the source shelf. [1/2/3/4/5] (4 and 5 represent the depot leaders)\n>");
        int source = readInputInt();
        while(source < 1 || source > 5) {
            output.print("Please select a valid source shelf. [1/2/3/4/5] (4 and 5 represent the depot leaders)\n>");
            source = readInputInt();
        }
        output.print("Please select the amount of resources you want to move.\n>");
        int quantity = readInputInt();
        while(quantity < 1 || quantity > 2) {
            output.print("Please select a valid amount of resources.\n>");
            quantity = readInputInt();
        }
        output.print("Please select the destination shelf. [1/2/3/4/5] (4 and 5 represent the depot leaders\n>");
        int dest = readInputInt();
        while(dest == source || dest < 1 || dest > 5) {
            if(dest == source)
                output.print("Destination shelf must be different from source shelf.\n>");
            else
                output.print("Please select a valid destination shelf. [1/2/3/4/5] (4 and 5 represent the depot leaders\n>");
            dest = readInputInt();
        }
        List<LeaderEffect> leaders = new ArrayList<>();
        List<LeadCardInfo> playedCards = cli.getClientView().getOwnGameBoard().getPlayedCards();
        for(LeadCardInfo leadCardInfo : playedCards) {
            if(leadCardInfo.getType().equalsIgnoreCase("DEPOT"))
                leaders.add(new DepotEffect(Resource.valueOf(leadCardInfo.getType())));
        }
        return new MoveResources(NumOfShelf.values()[source - 1], NumOfShelf.values()[dest - 1], quantity, leaders);
    }

    private String readInputString() {
        try {
            return input.nextLine();
        } catch (InputMismatchException e) {
            output.print("Please insert a valid input.\n>");
            input.next();
            return readInputString();
        }
    }

    private int readInputInt(){
        try{
            return Integer.parseInt(input.nextLine());
        } catch (InputMismatchException | NumberFormatException e){
            output.print("Please insert a valid input.\n>");
            input.next();
            return readInputInt();
        }
    }

    public String[] getColumn(String[][] array, int index) {
        String[] column = new String[3];
        for (int i = 0; i < column.length; i++) {
            column[i] = array[i][index];
        }
        return column;
    }
}
