# Oasis_Utilities

This application creates end day reports for gas stations and is also a check database system that saves customer information and all the checks they have cashed. 

## Table of contents
* [Preview](#preview)
* [App Layout](#app-layout)
* [Features](#features)
* [Purpose](#purpose)
* [Tools](#tools)

## Preview

<img src="score-tracker-gif.gif" width="200" height="420"/>

## App Layout

Login Page            |  Home Page | Check Page | Customer Page
:-------------------------:|:-------------------------:|:-------------------------:|:-------------------------:
![login](https://user-images.githubusercontent.com/33325959/103325126-30413500-49ff-11eb-9836-6d9eb5746c7b.png)  | ![homepage](https://user-images.githubusercontent.com/33325959/103325123-2b7c8100-49ff-11eb-9797-47122e2c24a9.png) | ![checkspage](https://user-images.githubusercontent.com/33325959/103325129-333c2580-49ff-11eb-84e8-ca74c75c7a56.png) | ![customerpage](https://user-images.githubusercontent.com/33325959/103325132-359e7f80-49ff-11eb-89a7-36b354c23523.png)

## Features

* Login Page: 
	* Login to any of the 3 store, each with their own passwords verified by Firebase Authentication
	* User only needs to login once every 2 hours. Each time the app is started within the 2 hours, it will login in automatically
* Home Page: 
	* Create/Delete reports
	* Create custom reports based on reports already in the system to find out the month/year/custom-date range sales.
	* Sort reports by month/year/custom date range.
* Check Page: 
	* Add/Delete customers
	* Add/Delete/Edit Checks and their pictures
	* Searching capability to find a certain check or customer
	* Customer can be searched using name or last 4 ID numbers
	* Ability to filter checks/customers by specific store 
	* Show all stores customers/checks
* Customer Page:
	* Add/Delete/Edit customer ID picture
	* Add a new check cashed by customer and a oicture of the check
	* Edit/Delete customer checks and their information like company name, amount, and status of check (real or fake)

## Purpose
This app was created specifically for Alkhanshali Inc. and its gas stations. The purpose of this app is to digitize the end day reports to have a digital copy
in case of need and to create a check database system between the three stores. By having this check database, workers in each store can see if the customer
has cashed a check before or if any of there checks were fake and so on. This app also stores the customer information so the workers won't have to take 
any more of their information and as a result, saves time.
	
## Tools
* Android Studio
* Java
* kotlin
* Firebase
