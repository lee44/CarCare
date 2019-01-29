package com.apps.jlee.carcare;

public class Gas
{
    private int id;
    private double cost, amount, miles;
    private String dateRefilled;

    public Gas() {}
    public Gas(int id, double cost, double amount, double miles, String dateRefilled)
    {
        this.id = id;
        this.cost = cost;
        this.amount = amount;
        this.miles = miles;
        this.dateRefilled = dateRefilled;
    }

    public int getID()
    {
        return id;
    }

    public void setID(int id)
    {
        this.id = id;
    }

    public double getCost()
    {
        return cost;
    }

    public void setCost(double cost)
    {
        this.cost = cost;
    }

    public double getAmount()
    {
        return amount;
    }

    public void setAmount(double amount)
    {
        this.amount = amount;
    }

    public double getMiles()
    {
        return miles;
    }

    public void setMiles(double miles)
    {
        this.miles = miles;
    }

    public String getDateRefilled()
    {
        return dateRefilled;
    }

    public void setDateRefilled(String dateRefilled)
    {
        this.dateRefilled = dateRefilled;
    }

    public String toString()
    {
        return "\nID: "+id+"\nCost: "+cost+"\nMiles: "+miles+"\nAmount: "+amount+"\nDate: "+dateRefilled;
    }
}
