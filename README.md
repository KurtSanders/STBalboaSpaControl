# STBalboaSpaControl
SanderSoft™ 2019

---

#### Version 1.0.x
* This initial alpha version is functional VIEW STATUS Only.  Future updates will be provided to allow one to change the spa operating state (Pumps, Lights, Heat Mode, Set Temperature, etc).

## Description:

A custom SmartThings® SmartApp and Device Handlers (DTH) which provides a connection to ones compatible [Balboa™ spa controller](http://www.balboawatergroup.com/bwa) and WiFi module. 

This SmartThings application allows one to view the state of their [Balboa™ spa](http://www.balboawatergroup.com/bwa) with  [CloudControl™](https://www.bullfrogspas.com/cloudcontrol/).  

## My Spa Tile and Details View

<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/BalboaSpaControl/master/images/screenshots/MainScreen1.jpeg" width=200>
</p>

## Requirements:

1. A spa with a compatible [Balboa™ spa controller](http://www.balboawatergroup.com/bwa) and WiFi module. 
<p align="center">
<img src="https://raw.githubusercontent.com/KurtSanders/BalboaSpaControl/master/images/pics/wifi-module.png" width=200>
</p>
2. A supported mobile device with **ST Legacy Client**. *This app will not work in the new Samsung SmartThings App*. 
3. A working knowledge of the SmartThings IDE
	* Installing a SmartApp & DTH from a GitHub repository (see [SmartThings GitHub IDE integration documentation](https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html?highlight=github) for example instructions and use the Repository Owner, Name and Branch from installation instructions below)

## Installation & Configuration

**GitHub Repository Integration**

Create a new SmartThings Repository entry in your SmartThings IDE under 'Settings' with the following values:

| Owner | Name | Branch |
|------|:-------:|--------|
| kurtsanders | STBalboaSpaControl | master |

**Required Files in your SmartThings IDE Repository**

You will need to use 'Update from Repo' to install into your SmartThings IDE repository:

| IDE Repository    | Filename | Status | Version |
| :---: | :----------| :---:  | :---:  |
| My SmartApps      | kurtsanders : Balboa Spa Controller | **New**  | 1.0.0 |
| My Device Handler | kurtsanders : Balboa Spa Control Device | **New** | 1.0.0 |


**Instructions**

1. Using the 'Update from REPO' button in the 'My SmartApps' SmartThings IDE, check the 'Balboa Spa Controller' SmartApp and publish & press Save.  
2. Using the 'Update from REPO' button in the "My Device Handlers" SmartThings IDE, check the Balboa Spa Control Device and publish & press Save.  ([See GitHub IDE integration](https://docs.smartthings.com/en/latest/tools-and-ide/github-integration.html?highlight=github)) from this STBalboaSpaControl(master) repository to your SmartThings IDE.
3. Locate the Balboa Spa Control app in the MarketPlace/SmartApps/My Apps list and click to launch the smartapp.
4. Enter your [routers public IP address](https://www.google.com/search?q=whats+my+ip+address) or DNS name if you have this service set in your router or otherwise. 
