/*
*  Name:	BWA Hot Tub Virtual Device Handler for Balboa 20P WiFi Module
*  Author: Kurt Sanders
*  Email:	Kurt@KurtSanders.com
*  Date:	3/2017
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
def version() { return ["V1.0", "Requires Balboa Spa Control Manager App"] }
// End Version Information
import groovy.time.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat;

def redColor 		= "#FF0000"
def greenColor 		= "#008000"
def whiteColor 		= "#FFFFFF"
def yellowColor 	= "#FFFF00"
def blueColor 		= "#0000FF"

metadata {
    definition (name: "Balboa Spa Control Device", namespace: "kurtsanders", author: "kurt@kurtsanders.com") {
        capability "Light"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Temperature Measurement"
        capability "Thermostat Heating Setpoint"


        attribute "tubStatus", 	"string"
        attribute "statusText", "string"
        attribute "schedulerFreq", "enum", ['0','1','5','10','15','30','60','180']
        attribute "spaPump1", "enum", ['Low','High','Off']
        attribute "spaPump2", "enum", ['Low','High','Off']
        attribute "heatMode", "enum", ['Rest','Ready/Rest','Ready']
        attribute "thermostatMode", "enum", ['off','heat']
        attribute "thermostatOperatingState", "enum", ['idle','heating']
        attribute "connected", "enum", ['online','offine']

        command "refresh"
		command "heatLevelUp"
		command "heatLevelDown"
        command "lightOn"
        command "lightOff"
    }
    tiles(scale: 2) {
        // Current Temperature Reading
        multiAttributeTile(name:"temperature", type:"generic", width:6, height:4, canChangeIcon: true) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState("default",label:'${currentValue}º',
                               backgroundColors:[
                                   [value: 0,   color: whiteColor],
                                   [value: 50,  color: navyColor],
                                   [value: 90,  color: blueColor],
                                   [value: 104, color: redColor]
                               ])
            }
            tileAttribute("tubStatus", key: "SECONDARY_CONTROL") {
                attributeState("tubStatus", label:'${currentValue}', defaultState: true)
            }
        }
        valueTile("heatingSetpoint", "device.heatingSetpoint",  decoration: "flat", width: 2, height: 1) {
            state("heatingSetpoint", label:'Set Temp:\n${currentValue}°F')
        }
        standardTile("thermostatOperatingState", "thermostatOperatingState", decoration: "flat", width: 2, height: 2) {
            state "idle", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/idle.png"
            state "heating", label:'${name}',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heating.png"
        }
        standardTile("thermostatMode", "thermostatMode", decoration: "flat", width: 2, height: 2,) {
            state "off",  label: 'Heat Off', icon: "st.Outdoor.outdoor19"
            state "heat", label: 'Heat On',
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/heatMode.png"
        }
        // Hot Tub Turn Pumps On/Off
        standardTile("switch", "device.switch",  width: 2, height: 2, decoration: "flat") {
            state "off", 		label:'Pumps ${currentValue}', action:"off", icon:"st.Outdoor.outdoor16", backgroundColor:"#ffffff"
            state "on", 		label:'Pumps ${currentValue}', action:"on",  icon:"st.Outdoor.outdoor16", backgroundColor:"#00a0dc"
        }
        // Turn SPA Lights On/Off
        standardTile("light", "device.light",  width: 2, height: 2, decoration: "flat") {
            state "on",  label:'On',  action: "lightOff", icon:"st.Lighting.light11", backgroundColor:"#00a0dc", nextstate: 'off'
            state "off", label:'Off', action: "lightOn",  icon:"st.Lighting.light13", backgroundColor:"#ffffff", nextstate: 'on'
        }
        // Network Connected Status
        standardTile("connected", "connected",  width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "offline",   label:'Offline', action:"open",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/offline.png",
                backgroundColor:yellowColor
            state "online", label:'Online', action:"closed",
                icon: "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/devicetypes/kurtsanders/bwa.src/icons/broadcast.png",
                backgroundColor:greenColor
        }
        standardTile("spaPump1", "spaPump1", inactiveLabel: false, decoration: "flat", width: 2, height: 2,) {
            state "Low", label: 'Jet1 Low', action:"spaPump1High",
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet1 High', action:"spaPump1Off",
                icon: "st.valves.water.open", backgroundColor: blueColor
            state "Off", label: 'Jet1 Off', action:"spaPump1Low",
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        standardTile("spaPump2", "spaPump2", inactiveLabel: false, decoration: "flat", width: 2, height: 2,) {
            state "Low", label: 'Jet2 Low', action:"spaPump2High",
                icon: "st.valves.water.open", backgroundColor: greenColor
            state "High", label: 'Jet2 High', action:"spaPump2Off",
                icon: "st.valves.water.open", backgroundColor: blueColor
            state "Off", label: 'Jet2 Off', action:"spaPump2Low",
                icon: "st.valves.water.closed", backgroundColor: whiteColor
        }
        // Hot Tub Heat Mode On/Off
        standardTile("heatMode", "heatMode", action:"heatModeReady" , inactiveLabel: false, decoration: "flat", width: 2, height: 2,) {
            state "Ready", 		label:'Ready', 	action:"heatModeRest", 	icon:"st.Kids.kids20", 	backgroundColor:"#ffffff", nextState:"Rest"
            state "Ready/Rest", label:'Ready', 	action:"heatModeRest", 	icon:"st.Kids.kids20", 	backgroundColor:"#ffffff", nextState:"Rest"
            state "Rest", 		label:'Rest', 	action:"heatModeReady", icon:"st.Kids.kids20", 	backgroundColor:"#00a0dc", nextState:"Ready"
        }
        // Descriptive Text
        valueTile("statusText", "statusText", decoration: "flat", width: 4, height: 1, wordWrap: true) {
            state "statusText", label: '${currentValue}', backgroundColor:whiteColor, action:"refresh"
        }
        valueTile("schedulerFreq", "schedulerFreq", decoration: "flat", inactiveLabel: false, width: 2, height: 1, wordWrap: true) {
            state "schedulerFreq", label: 'Refresh Every\n${currentValue} min(s)', action:"refresh"
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		standardTile("heatLevelDown", "device.heatingSetpoint", width: 2, height: 1, canChangeIcon: false, inactiveLabel: false, decoration: "flat") {
			state "heatLevelDown", label:'Heat', action:"heatLevelDown", icon: "http://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/heatDown.png", backgroundColor: "#ffffff"

		}
		standardTile("heatLevelUp", "device.heatingSetpoint", width: 2, height: 1, canChangeIcon: false, inactiveLabel: false, decoration: "flat") {
			state "heatLevelUp", label:'Heat', action:"heatLevelUp", icon: "http://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/heatUp.png", backgroundColor: "#ffffff"
		}
        main(["temperature"])
        details(
            [
                "temperature",
                "switch",
                "heatMode",
                "connected",
                "light",
                "thermostatOperatingState",
                "heatLevelUp",
                "heatLevelDown",
                "thermostatMode",
                "spaPump1",
                "spaPump2",
                "heatingSetpoint",
                "schedulerFreq",
                "refresh",
                "statusText"
            ]
        )
    }
}

def refresh() {
    Date now = new Date()
    def timeString = now.format("EEE MMM dd h:mm:ss a", location.timeZone)
    sendEvent(name: "statusText", value: "Cloud Refresh Requested at\n${timeString}...", "displayed":false)
    parent.main()
}

def installed() {
}

def on() {
    log.trace "Spa: Turning Pumps On"
//    parent.changeLights('on')
}
def off() {
    log.trace "Spa Turning Pumps Off"
//    parent.changeLights('off')
}

def light(direction) {
    log.trace "Spa: Turning Lights ${direction}"
    if (parent.changeLights(direction)) {
        sendEvent(name: "light", value: direction, isStateChange: true, display: true, displayed: true)
	}
}

def lightOn() {
    log.trace "Spa: Turning Lights On"
    if (parent.changeLights('on')) {
        sendEvent(name: "light", value: "on", isStateChange: true, display: true, displayed: true)
	}
}
def lightOff() {
    log.trace "Spa Turning Lights Off"
    if (parent.changeLights('off')) {
        sendEvent(name: "light", value: "off", isStateChange: true, display: true, displayed: true)
	}
}

void heatLevelUp() {
    parent.changeSetTemperature('up')
}

void heatLevelDown() {
    parent.changeSetTemperature('down')
}