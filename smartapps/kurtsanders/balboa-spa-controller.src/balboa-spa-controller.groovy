/*
* Bullfrog/Balboa Spa Control Manager for WiFi connected Cloud Access Module
* Tested on BullFrog Model A7L
* 2019 (c) SanderSoft™
*
* Author:   Kurt Sanders
* Email:	Kurt@KurtSanders.com
* Date:	    3/2017
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
*/
import groovy.time.*
import java.text.SimpleDateFormat;

// Start Version Information
String version()	 	{ return "1.0.2" }
String appModified() 	{ return "2019-09-17" }
// End Version Information

definition(
    name: 		"Balboa Spa Controller",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"Control and monitor your direct WiFi connected Bullfrog™/Balboa® Spa anywhere via SmartThings®",
    category: 	"My Apps",
    iconUrl: 	getAppImg("icons/SpaController.png"),
    iconX2Url: 	getAppImg("icons/SpaControllerX2.png"),
    iconX3Url: 	getAppImg("icons/SpaControllerX3.png"),
    singleInstance: true
)
preferences {
    page(name:"mainMenu")
    page(name:"mainOptions")
}

def mainMenu() {
    def spaDeviceOK = false
    def imageName = getAppImg("icons/failure-icon.png")
    if (hostName) {
        log.info "Calling subroutine: getHotTubDeviceID()"
        if (getHotTubDeviceID()) {
            imageName    = getAppImg("icons/success-icon.png")
            spaDeviceOK = true
        } else {
            state.devid 	= null
            spaDeviceOK = false
        }
    }
    dynamicPage(name: "mainMenu",
                title: "Spa Network & Location Information",
                nextPage: (spaDeviceOK)?"mainOptions":null,
                submitOnChange: true,
                install: false,
                uninstall: true)
    {
        section ("Spa WiFi Information") {
            input ( name: "hostName",
                   type: "text",
                   title: "Select your PUBLIC IP4 Address of your router?",
                   submitOnChange: true,
                   multiple: false,
                   required: true
                  )
        }
        if (spaDeviceOK) {
            section {
                href(name: "Spa Controller Options",
                     page: "mainOptions",
                     description: "Complete Spa Controller Options")
            }
            section("Spa's Status/Information") {
                paragraph "Device Type: ${state.devicetype}"
                paragraph image: getAppImg(imageName),
                    required: false,
                    title : "Public IP: ${state.publicip}",
                        "DevID: ${state.devid}\n" +
                        "Mac: ${state.mac}\n" +
                        "Spa IP: ${state.localip}\n" +
                        "Online: ${state.online}\n" +
                        "Last Msg: ${state.lastupdate}"
            }
        } else if (state.devid == null) {
            section("Spa's Status/Information") {
                paragraph "Error: Spa Not Found at Public IP: ${hostName}"
                paragraph image: getAppImg("icons/failure-icon.png"),
                    required: false,
                    title : "Bad IP Address Entered",
                        "Please Enter a Valid IP4 Public address (nnn.nnn.nnn.nnn) of your home router"
            }
        }
        section ("${app.name} Information") {
            paragraph image: getAppImg("icons/SpaController.png"),
                title	: appAuthor(),
                    required: false,
                    "Version: ${version()}\n" +
                    "Date: ${appModified()}\n"
            href(name: "hrefReadme",
                 title: "${appNameVersion()} Setup/Read Me Page",
                 required: false,
                 style: "external",
                 url: "https://github.com/KurtSanders/STBalboaSpaControl/blob/master/README.md#stbalboaspacontrol",
                 description: "tap to view the Setup/Read Me page")

        }
        section(hideable: true, hidden: true, "Optional: SmartThings IDE Live Logging Levels") {
            input ( name: "debugVerbose", type: "bool",
                   title: "Show Debug Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
            input ( name: "infoVerbose", type: "bool",
                   title: "Show Info Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
            input ( name: "errorVerbose", type: "bool",
                   title: "Show Error Info Messages in IDE",
                   description: "Verbose Mode",
                   required: false
                  )
        }

    }
}

def mainOptions() {
    dynamicPage(name: "mainOptions",
                title: "Spa Controller Options",
                install: true,
                uninstall: false)
    {
        section("Spa Refresh Update Interval") {
            input ( name: "schedulerFreq",
                   type: "enum",
                   title: "Run Ambient Weather Station Refresh Every (X mins)?",
                   options: ['0':'Off','1':'1 min','2':'2 mins','3':'3 mins','4':'4 mins','5':'5 mins','10':'10 mins','15':'15 mins','30':'Every ½ Hour','60':'Every Hour','180':'Every 3 Hours'],
                   required: true
                  )
            mode ( title: "Limit Polling Hot Tub to specific ST mode(s)",
                  image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                  required: false
                 )
        }
        section("Spa Alert Notifications") {
            input ( name	: "phone",
                   type		: "phone",
                   title	: "Text Messages for Alerts (optional)",
                   description: "Mobile Phone Number",
                   required: false
                  )
        }
        section() {
            label ( name: "name",
                   title: "This SmartApp's Name",
                   state: (name ? "complete" : null),
                   defaultValue: "${app.name}",
                   required: false
                  )
        }
    }
}

def initialize() {
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
}

def installed() {
    add_bwa_ChildDevice()
    initialize()
}
def uninstalled() {
    log.info "Removing ${app.name}..."
    remove_bwa_ChildDevice()
    log.info "Good-Bye..."
}
def updated() {
    unsubscribe()
    initialize()
}

def appTouchHandler(evt="") {
    log.info "App Touch ${random()}: '${evt.descriptionText}' at ${timestamp()}"
    main()
}

def main() {
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
    updateStateVar("timestamp")
    if (!getSpaCloudData()) { return }
    decodeSpaB64Data()
    updateDeviceStates()
}

def changeSetTemperature(direction) {
    log.info "Executing changeSetTemperature($direction) Routine ID:${random()} at ${timestamp()}"
    main()
    def nextlevel = (direction=='up')?state.heatingSetpoint.toInteger() + 1:state.heatingSetpoint.toInteger() - 1
    log.debug "nextlevel = ${nextlevel}"
    if (SpaSetTargetTemperature(nextlevel)) {
        updateStateVar("heatingSetpoint", nextlevel)
		send("heatingSetpoint")
    } else {
        main()
    }
}

def changeLights(direction) {
    log.debug "Executing changeLights($direction) Routine ID:${random()} at ${timestamp()}"
    def lightButton = "17"
    main()
    //    PressButton("17")  17 = LED
    if (direction != state.light) {
    log.debug "Changing light from '${state.light}' to '${direction}'"
        if (PressButton(lightButton)) {
            log.debug "Lights successfully turned '${direction}'"
            updateStateVar("light", direction)
            send([name : "light", value : state.light])
        } else {
            main()
        }
    } else {
        log.debug "Lights are already ${state.light}"
        send([name: "light", value: state.light])
        return false
    }
    return true
}

def SpaSetTargetTemperature(newsetvalue) {
    log.debug "Setting Hot Tub Set Temp from ${state.heatingSetpoint} to ${newsetvalue}"
    def body = \
    '<sci_request version="1.0"><data_service><targets>' + \
    '<device id="' + state.devid + '"/></targets><requests>' + \
    '<device_request target_name="SetTemp">' + newsetvalue + '.000000</device_request>' + \
    '</requests></data_service></sci_request>'
    return BalboaHttpRequest(body)
}

def SetPumps(direction) {
    //    PressButton("X")  # where X = 4 = Jet1, or X = 5  = Jet2
    def LEDButton = "17"
    def Pump1 = "4"
    def Pump2 = "5"
/*
    if (SetLEDLightsOn) { return PressButton(LEDButton) }
    if (SetPumpsOn) {
        if (state.spaPump1 == "Off") {
            PressButton(Pump1)
            PressButton(Pump1)
            } else if (state.spaPump1 == "Low") {
            PressButton(Pump1)
            } else if (
        if Spa_Dict_Status["Pump"][1] == "Off":
            PressButton(Pump2)
            PressButton(Pump2)
        elif Spa_Dict_Status["Pump"][1] == "Low":
            PressButton(Pump2)
    else:  # Pumps Off
        if Spa_Dict_Status["Pump"][0] == "Low":
            PressButton(Pump1)
            PressButton(Pump1)
        elif Spa_Dict_Status["Pump"][0] == "High":
            PressButton(Pump1)
        if Spa_Dict_Status["Pump"][1] == "Low":
            PressButton(Pump2)
            PressButton(Pump2)
        elif Spa_Dict_Status["Pump"][1] == "High":
            PressButton(Pump2)
    */
}

def BalboaHttpRequest(body) {
    log.debug "body: ${body}"
    def params = [
        'uri'			: Web_idigi_post(),
        'headers'		: idigiHeaders(),
        'body'			: body
    ]
    infoVerbose("Start httpPost for BalboaHttpRequest =============")
    try {
        httpPost(params) {
            resp ->
            if(resp.status != 200) {
                log.error "HttpPost resp.status: ${resp.status}"
                return false
            }
        }
    }
    catch (Exception e)
    {
        debugVerbose("Catch HttpPost Error: ${e}")
        return false
    }
    return true
}

def getSpaCloudData() {
    debugVerbose("getSpaCloudData(): Start")
    def d = getChildDevice(DTHDNI())
    def byte[] B64decoded
    Date now = new Date()
    def timeString = new Date().format('EEE MMM d h:mm:ss a',location.timeZone)
    def respParams = [:]
    def respdata
    boolean connected = false
    def params = [
        'uri'			: Web_idigi_post(),
        'headers'		: idigiHeaders(),
        'body'			: Web_postdata()
    ]
    infoVerbose("Start httpPost =============")
    try {
        httpPost(params) {
            resp ->
            if(resp.status == 200) {
                debugVerbose("HttpPost Request was OK ${resp.status}")
                log.info "httpPost resp.data: ${resp.data}"
                respdata = resp.data
                connected = true
            } else {
                log.error "httpPost resp.status: ${resp.status}"
                respdata = null
                return connected
            }
        }
    }
    catch (Exception e)
    {
        debugVerbose("Catch HttpPost Error: ${e}")
        respdata = null
        return connected
    }
    if(respdata == "Device Not Connected") {
        log.error "HttpPost Request: ${respdata}"
        unschedule()
        state.statusText = "Spa Fatal Error ${respdata} at\n${timeString}"
        if (phone) {
            sendSms(phone, state.spaText)
        }
    }
    else {
		connected = true
        state.statusText 			= "Spa data refreshed at\n${timeString}"
        state.respdata				= respdata.toString()
        state.B64decoded 			= respdata.decodeBase64()
        log.debug "state.B64decoded: ${state.B64decoded}"
    }
    d.sendEvent(name: "connected",   	value: connected?'online':'offline', displayed: true)
    infoVerbose("getOnlineData: End")
    return connected
}

def updateDeviceStates() {
    infoVerbose("Start: updateDeviceStates-------------")
    infoVerbose("Sending Device Updates to Virtual Spa Tile")
    Date now = new Date()
    def timeString = new Date().format("M/d 'at' h:mm:ss a",location.timeZone).toLowerCase()
    def d = getChildDevice(DTHDNI())
    d.sendEvent(name: "temperature", value: state.temperature, displayed: true)
    d.sendEvent(name: "switch",    	value: state.switch, displayed: true)
    d.sendEvent(name: "heatMode", 	value: state.heatMode, displayed: true)
    d.sendEvent(name: "light", value: state.light, displayed: true)
    d.sendEvent(name: "thermostatOperatingState", value: state.thermostatOperatingState, displayed: true)
    d.sendEvent(name: "thermostatMode", value: state.thermostatMode, displayed: true)
    d.sendEvent(name: "spaPump1", value: state.spaPump1, displayed: true)
    d.sendEvent(name: "spaPump2", value: state.spaPump2, displayed: true)
    d.sendEvent(name: "heatingSetpoint", value: state.heatingSetpoint, displayed: true)
    d.sendEvent(name: "statusText", value: "${state.statusText}", displayed: false)
    d.sendEvent(name: "schedulerFreq", value: "${schedulerFreq}", displayed: false)
    d.sendEvent(name: "tubStatus",
    value: "${state.heatMode} - ${state.thermostatOperatingState.capitalize()} to ${state.heatingSetpoint}${state.tempunits} on ${timeString}",
    displayed: false)
    infoVerbose("End: updateDeviceStates-------------")
}


def decodeSpaB64Data() {
    infoVerbose("Entering decodeSpaB64Data")
    def B64decoded = state.B64decoded
    def params = [:]
    def offset = 0

    //	Hot Tub Current Temperature ( <0 is Unavailable )
    offset = 6
    def spaCurTemp = B64decoded[offset]
    if (spaCurTemp < 0) {
        spaCurTemp = 0
    }
    updateStateVar("temperature",spaCurTemp)
    offset = 13
    updateStateVar("tempunits",(B64decoded[offset])?'ºC':'ºF')

    //  Hot Tub Mode State
    offset = 9
    def modeStateDecodeArray = ["Ready","Rest","Ready/Rest"]
    updateStateVar("heatMode",modeStateDecodeArray[B64decoded[offset]])
    //	Hot Tub Pump1 and Pump2 Status
    offset = 15
    def pumpDecodeArray = []
    updateStateVar("switch","on")
    switch (B64decoded[offset]) {
        case 0:
        infoVerbose("Pump1: Off, Pump2: Off")
        pumpDecodeArray=["Off","Off"]
        updateStateVar("switch","off")
        break
        case 1:
        infoVerbose("Pump1: Low, Pump2: Off")
        pumpDecodeArray=["Low","Off"]
        break
        case 2:
        infoVerbose("Pump1: High, Pump2: Off")
        pumpDecodeArray=["High","Off"]
        break
        case 4:
        infoVerbose("Pump1: Off, Pump2: Low")
        pumpDecodeArray=["Off","Low"]
        break
        case 5:
        infoVerbose("Pump1: Low, Pump2: Low")
        pumpDecodeArray=["Low","Low"]
        break
        case 6:
        infoVerbose("Pump1: High, Pump2: Low")
        pumpDecodeArray=["High","Low"]
        break
        case 8:
        infoVerbose("Pump1: Off, Pump2: High")
        pumpDecodeArray=["Off","High"]
        break
        case 9:
        infoVerbose("Pump1: Low, Pump2: High")
        pumpDecodeArray=["Low","High"]
        break
        case 10:
        infoVerbose("Pump1: High, Pump2: High")
        pumpDecodeArray=["High","High"]
        break
        default :
        infoVerbose("Pump Mode: Unknown")
        pumpDecodeArray=["Off","Off"]
        updateStateVar("switch","off")
    }
    updateStateVar("spaPump1", pumpDecodeArray[0])
    updateStateVar("spaPump2", pumpDecodeArray[1])

    //	Hot Tub Heat Mode
    offset = 17
    if (B64decoded[offset]>0) {
        updateStateVar("thermostatOperatingState","heating")
        updateStateVar("thermostatMode","heat")
    }
    else {
        updateStateVar("thermostatOperatingState","idle")
        updateStateVar("thermostatMode", "off")
}

//	Hot Tub LED Lights
    offset = 18
    if (B64decoded[offset]>0) {
        infoVerbose("LED On")
        updateStateVar("light", "on")
    }
    else {
        updateStateVar("light", "off")
    }

	// Hot Tub Set Temperature
    offset = 24
    // params << ["heatingSetpoint": B64decoded[offset] + '°F\nSet Mode']
    updateStateVar("heatingSetpoint", "${B64decoded[offset].toInteger()}")
}

def getHotTubDeviceID() {
	def regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    if (hostName.findAll(regex)) {
        state.devid = getSpaDevId(hostName)
        log.debug "getHotTubDeviceID(): Received ${state.devid} from getSpaDevId(${hostName})"
    } else {
        log.error "Invalid public IP4 address provided"
        state.devid = null
        return false
    }
    if (state.devid) {
        log.debug "getHotTubDeviceID(): Valid Spa devID, Defining State Variables ${devID}, ${hostName}"
        state.hostname 	= hostName
		state.publicip 	= hostName
    } else {
        log.error "getHotTubDeviceID(): Invalid Spa devID, Erased state variables because the Spa devid = ${devID}"
        state.hostname 	= null
		state.publicip 	= null
        return false
    }
    return true
}

def getSpaDevId(ipAddress) {
    def respdata
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'")
    def tf = new java.text.SimpleDateFormat('EEE MMM d h:mm:ss a')
    def url   	= "https://my.idigi.com/ws/DeviceCore/.json?condition=dpGlobalIp='" + ipAddress + "'"
    def params = [
        'uri'			: url,
        'headers'		: idigiHeaders(),
        'contentType'	: 'application/json'
    ]
    log.debug "Start httpGet ============="
    try {
        httpGet(params) { resp ->
            if ( (resp.status == 200) && (resp.data.items) ) {
                log.debug "SpaDevId resp.data = ${resp.data}"
                respdata = resp.data
            }
            else {
                log.error "HttpGet Request for devID got http status ${resp.status}"
                respdata = null
                return null
            }
        }
    }
    catch (Exception e)
    {
        log.error "${e}"
        return null
    }
    if (respdata) {
        def lastUpdateTime 	= dtf.parse(respdata.items?.dpLastUpdateTime[0])
        def loc = getTwcLocation()?.location
        tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))
        state.lastupdate 	= "${tf.format(lastUpdateTime)}"
        state.devid 		= respdata.items?.devConnectwareId[0]
        state.devicetype 	= respdata.items?.dpDeviceType[0]
        state.mac 			= respdata.items?.devMac[0]
        state.localip 		= respdata.items?.dpLastKnownIp[0]
        state.online 		= respdata.items?.dpConnectionStatus[0]=='1'?'True':'False'
        return state.devid
    }
    return null
}

def setScheduler(schedulerFreq) {
    state.schedulerFreq = "${schedulerFreq}"
    switch(schedulerFreq) {
        case '0':
        unschedule()
        break
        case '1':
        runEvery1Minute('main')
        break
        case '5':
        runEvery5Minutes('main')
        break
        case '10':
        runEvery10Minutes('main')
        break
        case '15':
        runEvery15Minutes('main')
        break
        case '30':
        runEvery30Minutes('main')
        break
        case '60':
        runEvery1Hour('main')
        break
        case '180':
        runEvery3Hours('main')
        break
        default :
        log.error "Unknown Schedule Frequency '${schedulerFreq}'"
        return
    }
    if(schedulerFreq=='0'){
        infoVerbose("UNScheduled all RunEvery")
    } else {
        infoVerbose("Scheduled RunEvery${schedulerFreq}Minute")
    }
    send('schedulerFreq')
}

def boolean isIP(String str)
{
    try {
        String[] parts = str.split("\\.");
        if (parts.length != 4) return false;
        for (int i = 0; i < 4; ++i)
        {
            int p = Integer.parseInt(parts[i]);
            if (p > 255 || p < 0) return false;
        }
        return true;
    } catch (Exception e){return false}
}

def tubAction(feature, command) {
    infoVerbose("SmartApp tubAction----- Started")
    infoVerbose("tubAction command -> ${feature} ${command}")
    def d = getChildDevice(DTHDNI())
    switch(feature) {
        case 'switch':
        if (d.switchState.value!=command) {
            infoVerbose("Turning Hot Tub '${feature.toUpperCase()}' from '${d.switchState.value.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${d.switchState.value.toUpperCase()}'")
        }
        break
        case 'heatMode':
        if (state.heatMode!=command) {
            log.debug "Turning Heat '${feature}' from '${state.heatMode}' to '${command}'"
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.heatMode.toUpperCase()}'")
        }
        break
        case 'spaPump1':
        if (state.spaPump1!=command) {
            infoVerbose("Turning '${feature.toUpperCase()}' from '${state.spaPump1.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.spaPump1.toUpperCase()}'")
        }
        break
        case 'spaPump2':
        if (state.spaPump2!=command) {
            infoVerbose("Turning '${feature.toUpperCase()}' from '${state.spaPump2.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.spaPump2.toUpperCase()}'")
        }
        break
        default :
        infoVerbose("default tubAction action for ${feature} ${command}")
    }
    infoVerbose("SmartApp tubAction----- End")
}

def add_bwa_ChildDevice() {
    // add Hot Tub BWA device
    if (!getChildDevice(DTHDNI())) {
        log.debug "Creating a NEW device named 'My Spa' as ${DTHName()} with DNI: ${DTHDNI()}"
        try {
            addChildDevice("kurtsanders", DTHName(), DTHDNI(), null, ["name": "My Spa", label: "My Spa", completedSetup: true])
        } catch(e) {
            errorVerbose("The Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${e}'.  Please install this DTH device in the IDE's 'My Device Handlers'")
            return false
        }
        debugVerbose("Success: Added a new device named 'My Spa' as ${DTHName()} with DNI: ${DTHDNI()}")
    } else {
        debugVerbose("Verification: Device exists named 'My Spa' as ${DTHName()} with DNI: ${DTHDNI()}")
    }
}
def remove_bwa_ChildDevice() {
    getAllChildDevices().each {
        log.debug "Deleting Spa device: ${it.deviceNetworkId}"
        try {
            deleteChildDevice(it.deviceNetworkId)
        }
        catch (e) {
            log.debug "${e} deleting the Spa device: ${it.deviceNetworkId}"
        }
    }
}

def PressButton(buttonNumber) {
    def body = \
    '<sci_request version="1.0"><data_service><targets>' + \
    '<device id="' + state.devid + '"/></targets><requests>' + \
    '<device_request target_name="Button">' + buttonNumber + '</device_request>' + \
    '</requests></data_service></sci_request>'
    return BalboaHttpRequest(body)
}

def send(name) {
    def mapdata = ["name" : name, "value" : state."${name}"]
    def d = getChildDevice(DTHDNI())
    def statusElements = ["heatMode","thermostatOperatingState","heatingSetpoint","tempunits"]
    Date now = new Date()
    def timeString = new Date().format("M/d 'at' h:mm:ss a",location.timeZone).toLowerCase()
    d.sendEvent(mapdata)
    if (statusElements.contains(name)) {
        d.sendEvent(name: "tubStatus",
                    value: "${state.heatMode} - ${state.thermostatOperatingState.capitalize()} to ${state.heatingSetpoint}${state.tempunits} on ${timeString}",
                    displayed: false)
    }
}

def timestamp() {
    Date datenow = new Date()
    def tf = new java.text.SimpleDateFormat('EEE MMM d h:mm:ss a')
    def loc = getTwcLocation()?.location
    tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))
    return tf.format(datenow)
}

def random() {
    def runID = new Random().nextInt(10000)
//    if (state?.runID == runID as String) {
//        log.warn "DUPLICATE EXECUTION RUN AVOIDED: Current runID: ${runID} Past runID: ${state?.runID}"
//        return
//    }
//    state.runID = runID
    return runID
}

def updateStateVar(key='timestamp', value=null, cmd='put') {
    if (key=="timestamp") { value = timestamp() }
    state."${key}" = "${value}"
}

// Constant Declarations
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String appNameVersion() 		{ return "Balboa Spa Controller ${version()}" }
String appAuthor()	 			{ return "SanderSoft™" }
String getAppImg(imgName) 		{ return "https://raw.githubusercontent.com/KurtSanders/STBalboaSpaControl/master/images/$imgName" }
String DTHName() 				{ return "Balboa Spa Control Device" }
String DTHDNI() 				{ return "bscd-${app.id}" }
Map idigiHeaders() {
    return [
        'UserAgent'		: 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie'		: 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization'	: 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
}
String Web_idigi_post()  { return "https://developer.idigi.com/ws/sci" }
String Web_postdata() 	 { return '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${state.devid}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
}