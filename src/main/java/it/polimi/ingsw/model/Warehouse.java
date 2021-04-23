package it.polimi.ingsw.model;

import it.polimi.ingsw.controller.Place;
import it.polimi.ingsw.exceptions.WrongActionException;

import java.util.*;

public class Warehouse {
    private final List<ResourceQuantity> warehouse = new ArrayList<>();
    private final int initialDim;

    public Warehouse(int warehouseDim){
        for(int i = 0; i < warehouseDim; i++){
            warehouse.add(new ResourceQuantity(0, Resource.EMPTY));
        }
        initialDim = warehouseDim;
    }
    /*returns a copy of the warehouse*/
    public List<ResourceQuantity> getWarehouse(){
        return new ArrayList<>(warehouse);
    }
    /*returns a copy of a shelf of the warehouse (considering also depots)*/
    public ResourceQuantity getShelf(NumOfShelf numOfShelf) {
        ResourceQuantity shelf = warehouse.get(numOfShelf.ordinal());
        return new ResourceQuantity(shelf.getQuantity(), shelf.getResource());
    }
    /*store the resources in the shelves*/
    public void incrementResource (List <ResourcePosition> outputRes) {
        List<ResourcePosition> storableRes = new ArrayList<>(outputRes);
        storableRes.removeIf(Rp -> Rp.getResource() == Resource.FAITHPOINT);                                            //faithpoints are not involved in this entire check
        storableRes.removeIf(Rp -> Rp.getPlace() == Place.TRASH_CAN);                                                   //Resources that should be discarded are not interested in this method.
        NumOfShelf numOfShelf;
        ResourceQuantity shelf;

        for(ResourcePosition Rp : storableRes){
            numOfShelf = Rp.getShelf();
            shelf = warehouse.get(numOfShelf.ordinal());
            if(shelf.getResource() == Resource.EMPTY) shelf.setResource(Rp.getResource());
            shelf.setQuantity(shelf.getQuantity() + Rp.getQuantity());                                                  //Rp.getQuantity() = 1
        }
    }
    /*controls if the resources can be stored*/
    public void checkIncrement(List <ResourcePosition> outputRes) throws WrongActionException {                    //used in the checkAction of BuyResources                                  //this method will be called only be the checkAction in BuyResources
        List<ResourcePosition> storableRes = new ArrayList<>(outputRes);
        storableRes.removeIf(Rp -> Rp.getResource() == Resource.FAITHPOINT);                                            //faithpoint are not interested by this entire check
        storableRes.removeIf(Rp -> Rp.getPlace() == Place.TRASH_CAN);                                                   //Resources that should be discarded are not interested in this method.
        //check on the storableRes itself
        for(int i = 0; i < storableRes.size(); i++){
            ResourcePosition Rp = storableRes.get(i);
            for(int j = i + 1; j < storableRes.size(); j++){
                if(storableRes.get(j).getResource() != Rp.getResource() && storableRes.get(j).getShelf() == Rp.getShelf())
                    throw new WrongActionException("The resources number " +i+ " and " +j+ " cannot be stored because they are different, but the player wanted to store them in the same shelf.");
                else if(storableRes.get(j).getResource() == Rp.getResource() && storableRes.get(j).getShelf() != Rp.getShelf())
                    throw new WrongActionException("The resources number " +i+ " and " +j+ " cannot be stored because they are of the same type, but the player wanted to store them in two different shelves.");
            }
        }
        NumOfShelf numOfShelf;
        ResourcePosition Rp;
        ResourceQuantity shelf;
        int dimShelf;
        //check on the disposition of the resources in the shelves
        for (int i = 0; i < storableRes.size(); i++) {
            Rp = storableRes.get(i);
            numOfShelf = Rp.getShelf();
            if (Rp.getPlace() == Place.CHEST)
                throw new WrongActionException("All the resources must be stored in the warehouse.");
            else if (numOfShelf.ordinal() >= warehouse.size())
                throw new WrongActionException("The resource number " + i + " cannot be stored because the indicated shelf does not exist.");
            else if (Rp.getResource() == Resource.EMPTY)
                throw new WrongActionException("The empty resource is not storable.");
            else {
                if (numOfShelf.ordinal() < initialDim) dimShelf = numOfShelf.ordinal() + 1;                             //dimension of a "normal" shelf
                else dimShelf = 2;                                                                                      //dimension of a "depot"
                shelf = warehouse.get(numOfShelf.ordinal());
                if (shelf.getResource() != Resource.EMPTY && shelf.getResource() != Rp.getResource())
                    throw new WrongActionException("The resource number " + i + " cannot be stored because there is already another type of resource in the selected shelf.");
                else if (!checkOtherShelves(Rp.getResource(), numOfShelf))                                              //example: if I want to add two coins in the third shelf, but there is already one coin in the second shelf, then I have to discard two coins.
                    throw new WrongActionException("The resource number " + i + " cannot be stored because there is already another shelf storing the same type of resource.");
                else if (calculateQuantity(storableRes, Rp) > dimShelf - shelf.getQuantity())
                    throw new WrongActionException("The shelf number " + numOfShelf.toString() + " does not have enough space to store the indicated resources.");
            }
        }
    }
    /*returns the number of resources, contained in inputRes, of the same type of Rp and from / to the same shelf of Rp*/
    private int calculateQuantity(List <ResourcePosition> inputRes, ResourcePosition Rp){                          //it computes the number of nodes in a given list of ResourcePosition that have the same Resource and numOfShelf
        int quantity = 0;
        for(ResourcePosition r : inputRes){
            if(r.getResource() == Rp.getResource() && r.getShelf() == Rp.getShelf()) quantity++;
        }
        return quantity;
    }
    /*remove the resources from the shelves*/
    public void decrementResource (List <ResourcePosition> inputRes) {
        List<ResourcePosition> removableRes = new ArrayList<>(inputRes);
        removableRes.removeIf(Rp -> Rp.getPlace() != Place.WAREHOUSE);                                                 //Resources that should be discarded are not interested in this method.
        NumOfShelf numOfShelf;
        ResourceQuantity shelf;

        for(ResourcePosition Rp : removableRes){
            numOfShelf = Rp.getShelf();
            shelf = warehouse.get(numOfShelf.ordinal());
            if(shelf.getQuantity() == 1) shelf.setResource(Resource.EMPTY);
            shelf.setQuantity(shelf.getQuantity() - Rp.getQuantity());                                                  //Rp.getQuantity() = 1
        }
    }
    /*controls if the resources can be removed*/
    public void checkDecrement(List<ResourcePosition> inputRes) throws WrongActionException {                                                   //used in checkAction of BuyDevCard, StartProduction.
        List <ResourcePosition> removableRes = new ArrayList<>(inputRes);
        removableRes.removeIf(Rp -> Rp.getPlace() != Place.WAREHOUSE);

        NumOfShelf numOfShelf;
        ResourcePosition Rp;
        ResourceQuantity shelf;

        for (int i = 0; i < removableRes.size(); i++) {
            numOfShelf = removableRes.get(i).getShelf();
            Rp = removableRes.get(i);
            if (numOfShelf.ordinal() >= warehouse.size())
                throw new WrongActionException("The resource number " + i + " cannot be removed because the indicated shelf does not exist.");
            else if (Rp.getResource() == Resource.EMPTY)
                throw new WrongActionException("The resource number " + i + " cannot be removed because the empty resource is not storable.");
            else {                                                                                    //dimension of a "depot"
                shelf = warehouse.get(numOfShelf.ordinal());
                if (shelf.getResource() != Rp.getResource())
                    throw new WrongActionException("The resource number " + i + " cannot be removed because the resource is not in the indicated shelf.");
                else if (calculateQuantity(removableRes, Rp) >= shelf.getQuantity())
                    throw new WrongActionException("The shelf number " +numOfShelf.toString()+ " does not have enough " +Rp.getResource().toString()+ "S.");
            }
        }
    }
    /*moves a certain number of resources (quantity) from srcShelf to destShelf*/
    public void moveResource (NumOfShelf srcShelf, NumOfShelf destShelf, int quantity){
        ResourceQuantity shelfSrc = warehouse.get(srcShelf.ordinal());
        List <ResourcePosition> inputRes = new ArrayList<>();
        List <ResourcePosition> outputRes = new ArrayList<>();
        for(int i = 0; i < quantity; i++) {
            inputRes.add(new ResourcePosition(1, shelfSrc.getResource(), Place.WAREHOUSE, srcShelf));
            outputRes.add(new ResourcePosition(1,shelfSrc.getResource(), Place.WAREHOUSE, destShelf));
        }
        decrementResource(inputRes);
        incrementResource(outputRes);
    }
    /*controls if the resources can be moved*/
    public void checkMove (NumOfShelf srcShelf, NumOfShelf destShelf, int quantity) throws WrongActionException{
        if (srcShelf.ordinal() >= warehouse.size() || destShelf.ordinal() >= warehouse.size())
            throw new WrongActionException("One of the specified shelves does not exist.");
        else {
            ResourceQuantity shelfSrc = warehouse.get(srcShelf.ordinal());
            ResourceQuantity shelfDest = warehouse.get(destShelf.ordinal());
            int dimShelfDest;
            if (destShelf.ordinal() < initialDim)
                dimShelfDest = destShelf.ordinal() + 1;                                                                 //dimension of a "normal" shelf
            else dimShelfDest = 2;                                                                                      //dimension of a "depot"
            if (shelfSrc.getResource() == Resource.EMPTY)
                throw new WrongActionException("The shelf indicated as source is empty.");
            else if (shelfSrc.getQuantity() < quantity)
                throw new WrongActionException("The specified quantity is greater than the actual number of resources in the source shelf.");
            else if (srcShelf.ordinal() < initialDim && destShelf.ordinal() < initialDim && quantity != shelfSrc.getQuantity())
                throw new WrongActionException("When moving resources between the first three shelves, all the resources from the source shelf must be moved.");
            else if (shelfDest.getResource() != Resource.EMPTY && shelfDest.getResource() != shelfSrc.getResource())    //rule of same resource in the same shelf.
                throw new WrongActionException("Different resources cannot be stored in the same shelf.");
            else if (dimShelfDest - shelfDest.getQuantity() < quantity)
                throw new WrongActionException("There is not enough space in the shelf indicated as destination.");
        }
    }
    /*controls if there are some "normal" shelves storing resources of type resource, apart from numOfShelf*/
    private boolean checkOtherShelves(Resource resource, NumOfShelf numOfShelf){                                        //only used in addResource
        boolean check = true;
        for(int i = 0; i < initialDim; i++){
            if(i != numOfShelf.ordinal()){
                if(warehouse.get(i).getResource() == resource) {
                    check = false;
                    break;
                }
            }
        }
        return check;
    }
    /*adds a "depot" shelf*/
    public void addDepot(Resource resource){
        warehouse.add(new ResourceQuantity(0, resource));
    }
    /*controls if there is already a depot storing the same resource*/
    public boolean checkDepot(Resource resource){
        boolean check = true;
        for(int i = initialDim; i < warehouse.size(); i++){
            if(warehouse.get(i).getResource() == resource){
                check = false;
                break;
            }
        }
        return check;
    }
    /*returns the number of resources of the same type of resource*/
    public int getAvailability(Resource resource){
        int supply = 0;
        for (ResourceQuantity shelf : warehouse){
            if (shelf.getResource() == resource) supply += shelf.getQuantity();
        }
        return supply;
    }

}