package com.bayviewglen.zork;

import java.util.HashMap;

public class Inventory {
	private HashMap<String, Item> items;
	private double currentWeight;
	private double maxWeight;
	
	public Inventory(double currentWeight, double maxWeight) {
		super();
		this.items = new HashMap<String, Item>();
		this.currentWeight = currentWeight;
		this.maxWeight = maxWeight;
	}
	
	public Inventory() {
		super();
		this.items = new HashMap<String, Item>();
	}

	public Inventory(Boolean isRoom) {
		if (isRoom)
			this.maxWeight = Double.MAX_VALUE;
		
	}
	public HashMap<String, Item> getItems() {
		return items;
	}
	public void setItems(HashMap<String, Item> items) {
		this.items = items;
	}
	public double getCurrentWeight() {
		return currentWeight;
	}
	public void setCurrentWeight(double currentWeight) {
		this.currentWeight = currentWeight;
	}
	public double getMaxWeight() {
		return maxWeight;
	}
	public void setMaxWeight(double maxWeight) {
		this.maxWeight = maxWeight;
	}
	
	// addItem - return boolean
	// hasItem - return boolean
	// dropItem- return Item
	// display Inventory - return String
	
	// 
	
	
}
