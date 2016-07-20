/**
 *  Copyright 2016 David Schroeder
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	ESP8266 Based Pellet Stove Controller
 *
 *	Author: D. Schroeder
 *	Date: 2016-05-06
 */
 
 
import groovy.json.JsonSlurper
 
preferences {
    input("confIpAddr", "string", title:"Controller IP Address",
        required:true, displayDuringSetup: true)
    input("confTcpPort", "number", title:"Controller TCP Port",
        required:true, displayDuringSetup:true)
} 
 
metadata {
    definition (name: "Pellet Stove", namespace: "schroeder", author: "Dave Schroeder") {
    	capability "Actuator"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
        capability "Thermostat"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        
        attribute "port", "string"
        attribute "humidity", "string"
        attribute "contact0", "string"
        attribute "contact1", "string"
        
        command "switchMode"
        command "contact0On"
        command "contact0Off"
        command "contact1On"
        command "contact1Off"
 }

 // simulator metadata
 simulator {}

// UI tile definitions
tiles {
	   valueTile("temperature", "device.temperature", width: 1, height: 1) {
			state("temperature", label:'${currentValue}°',
				backgroundColors:[
					[value: 32, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 92, color: "#d04e00"],
					[value: 98, color: "#bc2323"]
				]
			)
		}
		standardTile("mode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "off", label:'${name}', action:"switchMode", nextState:"to_heat"
			state "heat", label:'${name}', action:"switchMode", nextState:"to_cool"
			state "to_heat", label: "heat", action:"switchMode", nextState:"to_cool"
			state "...", label: "...", action:"off", nextState:"off"
		}

	    controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "setHeatingSetpoint", action:"setHeatingSetpoint", backgroundColor:"#d04e00"
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue}° heat', backgroundColor:"#ffffff"
		}
		
        valueTile("humidity", "device.humidity", inactiveLabel: false, decoration: "flat") {
			state "humidity", label:'${currentValue}% humidity', unit:""
		}
		
		standardTile("refresh", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        standardTile("contact0", "device.contact0", canChangeIcon: true) {
    		state "off", label: 'C1 ${name}', action: "contact0On",
          		icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    		state "on", label: 'C1 ${name}', action: "contact0Off",
          		icon: "st.switches.switch.on", backgroundColor: "#00E600"                
		}
        standardTile("contact1", "device.contact1", canChangeIcon: true) {
    		state "off", label: 'C2 ${name}', action: "contact1On",
          		icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    		state "on", label: 'C2 ${name}', action: "contact1Off",
          		icon: "st.switches.switch.on", backgroundColor: "#00E600"                
		}
        
		main "temperature"
		details(["temperature", "humidity", "mode", "heatSliderControl", "heatingSetpoint", "contact0", "contact1", "refresh"])		
	}
}

def updated() {
    log.info "Pool Controller ${textVersion()}. ${textCopyright()}"
	log.debug("$device.displayName updated with settings: ${settings.inspect()}")

    state.hostAddress = "${settings.confIpAddr}:${settings.confTcpPort}"
    
    STATE()
}

// parse events into attributes
def parse(String message) {
	//log.debug "Parsing '${message}'"
   
    unschedule("setOffline")

    def msg = stringToMap(message)
    
    //update the DNI if needed
    if (device.deviceNetworkId != msg.mac) {
    	device.deviceNetworkId = msg.mac
    }
    
    if (msg.headers) {
        // parse HTTP response headers
        def headers = new String(msg.headers.decodeBase64())
        def parsedHeaders = parseHttpHeaders(headers)
        //log.debug("parsedHeaders: ${parsedHeaders}")
        if (parsedHeaders.status != 200) {
            log.error "Server error: ${parsedHeaders.reason}"
            return null
        }

        // parse HTTP response body
        if (!msg.body) {
            //log.error "HTTP response has no body"
            return null
        }

        def body = new String(msg.body.decodeBase64())
        def slurper = new JsonSlurper()
        def data = slurper.parseText(body)

        return parseData(data)
	} else {
    	log.debug("No Headers ${msg.headers}");
    }
    
    return null
}


private def parseData(Map data) {
    //log.debug("parseData(${data})")

    def events = []
    if (data.containsKey("error_msg")) {
        log.error "Controller error: ${data.error_msg}"
        return null
    }

    if (data.containsKey("success")) {
        // this is POST response - ignore
        return null
    }

    if (data.containsKey("temp")) {
    	def value = data.temp.toFloat()
        if (device.currentState("temperature")?.value?.toFloat() != value) {
            def ev = [
                name:   "temperature",
                value:  value,
            ]
            events << createEvent(ev)
        }
    }
    
    if (data.containsKey("humidity")) {
    	def value = data.humidity.toFloat();
        if (device.currentState("humidity")?.value.toFloat() != value) {
            def ev = [
                name:   "humidity",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("contact0")) {
    	def value = data.contact0         
        if ((device.currentState("contact0")?.value == "on") != value) {
            def ev = [
                name:   "contact0",
                value:  value ? "on" : "off",
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("contact1")) {
    	def value = data.contact1
        if ((device.currentState("contact1")?.value == "on") != value) {
            def ev = [
                name:   "contact1",
                value:  value ? "on" : "off",
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("tempSetpoint")) {
    	def value = data.tempSetpoint.toFloat();
        if (device.currentState("heatingSetpoint")?.value.toFloat() != value) {
            def ev = [
                name:   "heatingSetpoint",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
 
    if (data.containsKey("mode")) {
    	def value = data.mode;
        
        if (device.currentState("thermostatMode")?.value != value) {
            def ev = [
                name:   "thermostatMode",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("operatingState")) {
    	def value = data.operatingState;        
        if (device.currentState("thermostatOperatingState")?.value != value) {
            def ev = [
                name:   "thermostatOperatingState",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
 
    log.debug("events: ${events}")
    return events
}


private parseHttpHeaders(String headers) {
    def lines = headers.readLines()
    def status = lines.remove(0).split()

	def stat = 0
    def reason = ""
    
	if (status[0] == "HTTP/1.1") {
    	stat = status[1].toInteger()
        reason = status[2]
    } else {
    	stat = 200
    }
    def result = [
    	protocol:   status[0],
        status:     stat,
        reason:     reason
    ]
    return result
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

def refresh() {
	log.debug "esp8266 refresh" 
	runIn(30, setOffline)    
    
    return apiGet()
}

def poll() {   
	log.debug "Executing 'poll' ${state.hostAddress}"    
    return refresh()
}

def apiGet() {
     def headers = [
        HOST:       state.hostAddress,
        Accept:     "*/*"
    ]

    def httpRequest = [
        method:     'GET',
        path:       "/getstatus",
        headers:    headers
    ]

	return new physicalgraph.device.HubAction(httpRequest)
}

private apiPost(String path, data) {
    log.debug("apiPost(${path}, ${data})")

    def headers = [
        HOST:       state.hostAddress,
        Accept:     "*/*"
    ]

    def httpRequest = [
        method:     'POST',
        path:       path,
        headers:    headers,
        body:       data
    ]

    return new physicalgraph.device.HubAction(httpRequest)
}


private def writeDataValue(name, value) {
    log.debug("writeDataValue(${name}, ${value})")

    def json = "{\"${name}\": ${value}}"
    def hubActions = [
        apiPost("/set", json),
        //delayHubAction(1000),
        //apiGet()
    ]

    return hubActions
}

private def delayHubAction(ms) {
    return new physicalgraph.device.HubAction("delay ${ms}")
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private def textVersion() {
    return "Version 0.0.1 (05/07/2016)"
}

private def textCopyright() {
    return "Copyright (c) 2016 Dave Schroeder"
}

private def STATE() {
    log.trace "state: ${state}"
    log.trace "deviceNetworkId: ${device.deviceNetworkId}"   
}

def setHeatingSetpoint(value)
{
	log.debug("setHeatingSetpoint '${value}'")
 
    return writeDataValue("tempSetpoint", value);
}

def setThermostatMode(String value) {
	log.debug("setThermostatMode '${value}'")

	return writeDataValue("mode", value);
}

def setContact(String contact, String value) {
	log.debug("setContact '${contact}' '${value}'")

	return writeDataValue(contact, value);
}

def modes() {
	["off", "heat"]
}

def switchMode() {
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	state.lastTriedMode = nextMode
    
    setThermostatMode(nextMode);
}

def switchToMode(nextMode) {
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
	} else {
		log.debug("no mode method '$nextMode'")
	}
}

def off() {
	setThermostatMode("off");
}

def heat() {
	setThermostatMode("heat");
}

def contact0On() {
	setContact("contact0", "true");
}

def contact0Off() {
	setContact("contact0", "false");
}

def contact1On() {
	setContact("contact1", "true");
}

def contact1Off() {
	setContact("contact1", "false");
}

def on() {
	return heat()
}
