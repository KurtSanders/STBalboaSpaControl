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
def version()   { return ["V1.0", "Original Code Base"] }
// End Version Information

String appVersion()	 { return "1.0" }
String appModified() { return "2019-02-03" }

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
            log.debug "state.devid 		= ${state.devid}"
            log.debug "state.publicip 	= ${state.publicip}"
            log.debug "state.hostname 	= ${state.hostname} => Preference: ${hostName}"
            spaDeviceOK = true
        } else {
            state.devid 	= null
        }
    }
    log.debug "spaDeviceOK: ${spaDeviceOK}"
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
                   title: "Select the FQDN or PUBLIC IP Address of your network?",
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
                        "Local IP: ${state.localip}\n" +
                        "Online: ${state.online}\n" +
                        "Last Msg: ${state.lastupdate}"
            }
        } else if (state.devid) {
            section("Spa's Status/Information") {
                paragraph "Error: Spa Not Found at ${hostName}"
                paragraph image: getAppImg("icons/failure-icon.png"),
                    required: false,
                    title : "Bad IP Address/DNS Name",
                    "Please Correct your home's public IP address"
            }
        }
        section ("${app.name} Information") {
            paragraph image: getAppImg("icons/SpaController.png"),
                title	: appAuthor(),
                    required: false,
                    "Version: ${version()[0]}\n" +
                    "Updates: ${version()[1]}"
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

def initialize() {
    setScheduler(schedulerFreq)
    subscribe(app, appTouchHandler)
//    updateStateVar()
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

/*
    if (debugVerbose) {
        def children = app.getChildDevices()
        def thisdevice
        log.debug "SmartApp $app.name has ${children.size()} child devices"
        thisdevice = children.findAll { it.typeName }.sort { a, b -> a.deviceNetworkId <=> b.deviceNetworkId }.each {
            log.info "${it} <-> DNI: ${it.deviceNetworkId}"
        }
    }

//    log.info "state.respdata   -> ${state.respdata}"
//    def String decodeString = state.respdata
//    def byte[] decoded = decodeString.decodeBase64()
//    log.debug "decoded => ${decoded}"
//    log.info "state.B64decoded -> ${state.B64decoded}"
//    def byte[] B64decoded
//    B64decoded = state.respdata.decodeBase64()
//    log.info "B64decoded -> ${B64decoded}"

*/
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

def refresh() {
    log.info "Executing Refresh Routine ID:${random()} at ${timestamp()}"
	main()
}

def main() {
    log.info "Executing Main Routine ID:${random()} at ${timestamp()}"
    updateStateVar("timestamp")
    getSpaCloudState()
    decodeSpaB64Data(state.B64decoded)
    updateDeviceStates()
    return
/*
    def stateVariables =  [
//        'devicetype',
//        'devid',
//        'hostname',
//        'lastupdate',
//        'localip',
//        'mac',
//        'online',
//        'publicip',
//        'tod',
        'temperature',
        'statusText',
        'connected',
        'heatMode',
        'switch',
        'spaPump1',
        'spaPump2',
        'thermostatOperatingState',
        'thermostatMode',
        'light',
        'heatingSetpoint'
    ]
    stateVariables.each {
        updateStateVar( it, "doctor")
    }
    return
*/
}

def updateStateVar(key='timestamp', value=null, cmd='put') {
    log.debug "Before ${cmd} Update for state.${key} = ${state.get(key)}"
    if (key=="timestamp") { value = timestamp() }
    log.info "state.put(${key} = ${value})"
    state."${key}" = "${value}"
    log.debug "After ${cmd} Update for state.${key} = ${state.get(key)}"
}

def getSpaCloudState() {
    infoVerbose("handler.getSpaCloudState() ----Started")

// Get array values from cloud for Hot Tub Status
	def byte[] B64decoded = null
    for (int i = 1; i < 4; i++) {
        log.debug "getOnlineData ${random()}: ${i} attempt..."
        B64decoded = getSpaCloudData()
        if (B64decoded) {
            log.debug "getOnlineData: Success, received '${B64decoded}' on ${i} attempt"
            break
        }
    }
    if (!B64decoded) {
    	log.error "getOnlineData: Failure, received '${B64decoded}':  Exiting..."
    	return false
    }
	return true
}

def byte[] getSpaCloudData() {
    debugVerbose("getSpaCloudData(): Start")
    def respdata
    def d = getChildDevice(DTHDNI())
    def byte[] B64decoded
    Date now = new Date()
    def timeString = new Date().format('EEE MMM d h:mm:ss a',location.timeZone)
    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${state.devid}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
    def respParams = [:]
    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: idigiHeaders(),
        'body'			: Web_postdata
    ]
    infoVerbose("Start httpPost =============")
    try {
        httpPost(params) {
            resp ->
            if(resp.status == 200) {
                debugVerbose("HttpPost Request was OK ${resp.status}")
                log.info "httpPost resp.data: ${resp.data}"
                respdata = resp.data
            } else {
                log.error "httpPost resp.status: ${resp.status}"
                return null
            }
        }
    }
    catch (Exception e)
    {
        debugVerbose("Catch HttpPost Error: ${e}")
        d.sendEvent(name: "connected",   	value: "offline", displayed: true)
        return null
    }
    if(respdata == "Device Not Connected") {
        log.error "HttpPost Request: ${respdata}"
        unschedule()
        d.sendEvent(name: "connected",   	value: "offline", displayed: true)
        state.statusText = "Spa Fatal Error ${respdata} at\n${timeString}"
        if (phone) {
            sendSms(phone, state.spaText)
        }
        return null
    }
    else {
        d.sendEvent(name: "connected",   	value: "online", displayed: true)
        state.statusText 			= "Spa data refreshed at\n${timeString}"
        state.respdata				= respdata.toString()
        state.B64decoded 			= respdata.decodeBase64()
        B64decoded 					= respdata.decodeBase64()
        log.debug "B64decoded: ${state.B64decoded}"
        // def byte[] B64decoded = B64encoded.decodeBase64()
        // def hexstring = B64decoded.encodeHex()
        // log.info "hexstring: ${hexstring}"
    }
    infoVerbose("getOnlineData: End")
    return B64decoded
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
    value: "${state.heatMode} - ${state.thermostatOperatingState.capitalize()} to ${state.heatingSetpoint}ºF on ${timeString}",
    displayed: false)
    infoVerbose("End: updateDeviceStates-------------")
}


def decodeSpaB64Data(byte[] d) {
    infoVerbose("Entering decodeSpaB64Data")
    def byte[] B64decoded = d
    log.debug "B64decoded = ${B64decoded}"
    def params = [:]
    def offset = 0

    //	Hot Tub Current Temperature ( <0 is Unavailable )
    offset = 6
    def spaCurTemp = B64decoded[offset]
    if (spaCurTemp < 0) {
        spaCurTemp = 0
    }
    updateStateVar("temperature",spaCurTemp)

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
    def ipAddress	 	= null
    if (isIP(hostName)) {
        ipAddress = hostName
    } else {
        ipAddress = convertHostnameToIPAddress(hostName)
    }
    if (ipAddress) {
        state.devid = getSpaDevId(ipAddress)
        log.debug "getHotTubDeviceID(): Received ${state.devid} from getSpaDevId(${ipAddress})"
    } else {
        log.error "Invalid public IP address provided"
        state.devid = null
        return false
    }
    if (state.devid) {
        log.debug "getHotTubDeviceID(): Valid Spa devID, Defining State Variables ${devID}, ${hostName}, ${ipAddress}"
        state.hostname 	= hostName
		state.publicip 	= ipAddress
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
                return null
            }
        }
    }
    catch (Exception e)
    {
        log.error "${e}"
        return null
    }
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

def setScheduler(schedulerFreq) {
    state.schedulerFreq = "${schedulerFreq}"
    switch(schedulerFreq) {
        case '0':
        unschedule()
        break
        case '1':
        runEvery1Minute(refresh)
        break
        case '5':
        runEvery5Minutes(refresh)
        break
        case '10':
        runEvery10Minutes(refresh)
        break
        case '15':
        runEvery15Minutes(refresh)
        break
        case '30':
        runEvery30Minutes(refresh)
        break
        case '60':
        runEvery1Hour(refresh)
        break
        case '180':
        runEvery3Hours(refresh)
        break
        default :
        infoVerbose("Unknown Schedule Frequency")
        unschedule()
        return
    }
    if(schedulerFreq=='0'){
        infoVerbose("UNScheduled all RunEvery")
    } else {
        infoVerbose("Scheduled RunEvery${schedulerFreq}Minute")
    }

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

def String convertHostnameToIPAddress(hostNameTMP) {
    def params = [
        uri			: "https://dns.google.com/resolve?name=" + hostNameTMP,
        contentType	: 'application/json'
    ]
    def ipAddress = null
    try {
        httpGet(params) { response ->
            infoVerbose("DNS Lookup Request, data=$response.data, status=$response.status")
            infoVerbose("DNS Lookup Result Status : ${response.data?.Status}")
            if (response.data?.Status == 0) { // Success
                for (answer in response.data?.Answer) { // Loop through results looking for the first IP address returned otherwise it's redirects
                    infoVerbose("Processing response: ${answer}")
                    infoVerbose("HostName ${answer.name} has IP Address of '${answer.data}'")
                    ipAddress = answer.data
                }
            } else {
                errorVerbose("DNS unable to resolve hostName ${response.data?.Question[0]?.name}, Error: ${response.data?.Comment}")
            }
        }
    } catch (Exception e) {
        errorVerbose("Unable to convert hostName to IP Address, Error: $e")
    }
    infoVerbose("Returning IP $retVal for HostName $hostName")
    return ipAddress
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


// Constant Declarations
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
String appAuthor()	 { return "SanderSoft™" }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/KurtSanders/STBalboaSpaControl/master/images/$imgName" }
String DTHName() { return "Balboa Spa Control Device" }
String DTHDNI() { return "bscd-${app.id}" }
Map idigiHeaders() {
    return [
        'UserAgent'		: 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie'		: 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization'	: 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
}
