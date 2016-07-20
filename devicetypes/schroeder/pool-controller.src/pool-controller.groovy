/**
 *  Copyright 2015 David Schroeder
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
 *	ESP8266 Based Pool/Chlorine Pump
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
    definition (name: "Pool Controller", namespace: "schroeder", author: "Dave Schroeder") {
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Temperature Measurement"
        
        attribute "ip", "string"
        attribute "port", "string"
        attribute "PoolPumpMode", "string"
        attribute "PoolPumpOff", "string"
        attribute "PoolPumpSpeed1", "string"
        attribute "PoolPumpSpeed2", "string"
        attribute "PoolPumpSpeed3", "string"
        attribute "PoolPumpQuickClean", "string"
        attribute "ChlorinePumpOn", "string"
        attribute "ChlorinePumpSetpoint", "string"
        attribute "ChlorinePumpTime", "string"        
        attribute "ControllerState", "string"
        attribute "PoolPumpControlMode", "string"
        
        command setChlorinePumpOff
        command setChlorinePumpOn
        command setChlorinePumpSetpoint
        command setChlorinePumpTime
        command setPoolPumpOff
        command setPoolPumpSpeed1
        command setPoolPumpSpeed2
        command setPoolPumpSpeed3
        command setPoolPumpQuickClean
        command setPoolPumpControlModeManual
        command setPoolPumpControlModeAuto
 }

 // simulator metadata
 simulator {}

// UI tile definitions
tiles(scale: 2) {
        //Not really a lighting type, but the secondary controll only seams to work in lighting
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 3){
            tileAttribute ("device.ChlorinePumpOn", key: "PRIMARY_CONTROL") {
                 attributeState "on", label:'${name}', action:"setChlorinePumpOff", icon:"st.switches.switch.off", backgroundColor:"#79b821", nextState:"Off"
                 attributeState "off", label:'${name}', action:"setChlorinePumpOn", icon:"st.switches.switch.on", backgroundColor:"#ffffff", nextState:"On"
                 attributeState "offline", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ff0000"
 			}
            tileAttribute ("ChlorinePumpSetpoint", key: "SECONDARY_CONTROL") {
             	 attributeState("default", label: 'PWM Setpoint: ${currentValue}', unit:"")
 			}
        }

        standardTile("ChlorinePumpOn", "device.ChlorinePumpOn", width: 2, height: 2,
             canChangeIcon: true) {
            state "off", label: 'Cl ${name}', action: "setChlorinePumpOn",
                  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: 'Cl ${name}', action: "setChlorinePumpOff",
                  icon: "st.switches.switch.on",  backgroundColor: "#00E600"
            state "offline", label: '${name}',
                  icon: "st.switches.switch.off", backgroundColor: "#FF0000"     
        }
        
        valueTile("PoolPumpControlMode", "device.PoolPumpControlMode", width: 2, height: 1, decoration: "flat") {
            state "Auto", label: '${name}', action: "setPoolPumpControlModeManual",
               	  backgroundColor: "#FFFFFF", defaultState: true, foregroundColor: "#FF0000"
            state "Manual", label: '${name}', action: "setPoolPumpControlModeAuto",
               	  backgroundColor: "#FFFFFF", foregroundColor: "#00FF00"
        }      

	    controlTile("ChlorinePumpSliderControl", "device.ChlorinePumpSetpoint", "slider", height: 2, width: 4, inactiveLabel: false, range: "(0..255)") {
			state "level",  label: 'Chlorine Pump PWM ${currentValue}', action:"setChlorinePumpSetpoint"
		}
        
		valueTile("ChlorinePumpSetpoint", "device.ChlorinePumpSetpoint", width: 2, height: 2){
            state "ChlorinePumpSetpoint", label: 'Cl ${currentValue} %', unit:"%",
            	backgroundColors: [
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
		}
        
        controlTile("ChlorinePumpTimeSliderControl", "device.ChlorinePumpTime", "slider", height: 2, width: 4, inactiveLabel: false, range: "(1..60)") {
			state "level",  label: 'Chlorine Pump Time ${currentValue}', action:"setChlorinePumpTime"
		}
        
		valueTile("ChlorinePumpTime", "device.ChlorinePumpTime", width: 2, height: 2){
            state "ChlorinePumpTime", label: '${currentValue} s', unit:"s"
		}
	
    	valueTile("PumpOff", "device.PoolPumpOff", height: 2, width: 2, decoration: "flat") {
            state "off", label: "Off", action:"setPoolPumpOff", backgroundColor: "#ffffff"
            state "on", label: "Off", icon: "st.Weather.weather12", backgroundColor: "#00E600"
        }
        
        valueTile("PumpSpeed1", "device.PoolPumpSpeed1", height: 1, width: 1, decoration: "flat") {
            state "off", label: '1', action:"setPoolPumpSpeed1", backgroundColor: "#ffffff"
            state "on", label: '1', icon: "st.Weather.weather12", backgroundColor: "#00E600"
        }
        
        valueTile("PumpSpeed2", "device.PoolPumpSpeed2", height: 1, width: 1, decoration: "flat") {
            state "off", label: '2', action:"setPoolPumpSpeed2", backgroundColor: "#ffffff"
            state "on", label: '2', icon: "st.Weather.weather12", backgroundColor: "#00E600"
        }
        
        valueTile("PumpSpeed3", "device.PoolPumpSpeed3", height: 1, width: 1, decoration: "flat") {
            state "off", label: '3', action:"setPoolPumpSpeed3", backgroundColor: "#ffffff"
            state "on", label: '3', icon: "st.Weather.weather12", backgroundColor: "#00E600"
        }
        
        valueTile("PumpQuickClean", "device.PoolPumpQuickClean", height: 1, width: 1, decoration: "flat") {
            state "off", label: 'QC', action:"setPoolPumpQuickClean", backgroundColor: "#ffffff"
            state "on", label: 'QC', icon: "st.Weather.weather12", backgroundColor: "#00E600"
        }
   		
        valueTile("ControllerState", "device.ControllerState", height: 1, width: 2, decoration: "flat") {
            state "Idle",       label: 'Idle', backgroundColor: "#ffffff", defaultState: true
            state "Running",    label: 'Runing', backgroundColor: "#ffffff"
            state "PreInject",  label: 'Pre Inject', backgroundColor: "#ffffff"
            state "Inject",     label: 'Inject', backgroundColor: "#ffffff"
            state "PostInject", label: 'Post Inject', backgroundColor: "#ffffff"     
        }
        
		standardTile("refresh", "device.switch", inactiveLabel: false, height: 2, width: 2, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
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
        
		main(["temperature"])
        details(["temperature", "ControllerState", "ChlorinePumpOn", "PoolPumpControlMode",
        		"ChlorinePumpSliderControl",  "ChlorinePumpSetpoint",
				"ChlorinePumpTimeSliderControl", "ChlorinePumpTime",
        		"PumpOff", "PumpSpeed1", "PumpSpeed2", "refresh", "PumpSpeed3", "PumpQuickClean" ])
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
            log.error "HTTP response has no body"
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
    log.debug("parseData(${data})")

    def events = []
    if (data.containsKey("error_msg")) {
        log.error "Controller error: ${data.error_msg}"
        return null
    }

    if (data.containsKey("success")) {
        // this is POST response - ignore
        return null
    }

    if (data.containsKey("ChlorinePumpSetpoint")) {
    	def value = data.ChlorinePumpSetpoint.toFloat()
        if (device.currentState("ChlorinePumpSetpoint")?.value.toFloat() != value) {
            def ev = [
                name:   "ChlorinePumpSetpoint",
                value:  value,
            ]
            events << createEvent(ev)
        }
    }
    
    if (data.containsKey("ChlorinePumpTime")) {
    	def value = data.ChlorinePumpTime.toString()
        if (device.currentState("ChlorinePumpTime")?.value != value) {
            def ev = [
                name:   "ChlorinePumpTime",
                value:  value,
            ]
            events << createEvent(ev)
        }
    }
    
    if (data.containsKey("ChlorinePumpOn")) {
    	def value = parseMotorState(data.ChlorinePumpOn);
        if (device.currentState("ChlorinePumpOn")?.value != value) {
            def ev = [
                name:   "ChlorinePumpOn",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("PoolPumpMode")) {
    	def value = data.PoolPumpMode;
        def valueString = parsePumpMode(value)
        
        def pumpmode = [
                name:   "PoolPumpMode",
                value:  valueString,
                type:   'physical',
        ]
        events << createEvent(pumpmode)
        
        if (device.currentState("PoolPumpMode")?.value != valueString) {                    
        	def state = (value == 0) ? "on":"off"
            def off = [
                name:   "PoolPumpOff",
                value:  state,
            ]
        	events << createEvent(off)
            state = (value == 1) ? "on":"off"
            def s1 = [
                name:   "PoolPumpSpeed1",
                value:  state,
            ]
        	events << createEvent(s1)
            state = (value == 2) ? "on":"off"
            def s2 = [
                name:   "PoolPumpSpeed2",
                value:  state,
            ]
        	events << createEvent(s2)
            state = (value == 3) ? "on":"off"
            def s3 = [
                name:   "PoolPumpSpeed3",
                value:  state,
            ]
        	events << createEvent(s3)
            state = (value == 4) ? "on":"off"
            def qc = [
                name:   "PoolPumpQuickClean",
                value:  state,
            ]
        	events << createEvent(qc)
        }
    }
    
    if (data.containsKey("State")) {
    	def value = parseControllerState(data.State);
        log.debug("State: " + value);
        if (device.currentState("ControllerState")?.value != value) {
            def ev = [
                name:   "ControllerState",
                value:  value,
            ]
        	events << createEvent(ev)
        }
    }
    
    if (data.containsKey("Temperature")) {
    	def value = data.Temperature.toFloat();
        log.debug("Temperature: " + value);
        if (device.currentState("temperature")?.value != value) {
            def ev = [
                name:   "temperature",
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
    return result}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

def refresh() {
	log.debug "esp8266 refresh" 
	if (device.currentValue("ChlorinePumpOn").toString() != "offline") {    	
    	runIn(30, setOffline)    
    }

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
    //log.debug("apiPost(${path}, ${data})")

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

	if (device.currentValue("ChlorinePumpOn").toString() != "offline") {
    	runIn(30, setOffline)    
    }
    return new physicalgraph.device.HubAction(httpRequest)
}


private def writeDataValue(name, value) {
    //log.debug("writeDataValue(${name}, ${value})")

    def json = "{\"${name}\": ${value}}"
    def hubActions = [
        apiPost("/set", json),
    ]

    return hubActions
}

private def delayHubAction(ms) {
    return new physicalgraph.device.HubAction("delay ${ms}")
}

private def parseMotorState(val) {
    def values = [
        "off",      // 0
        "on"        // 1
    ]

    return values[val.toInteger()]
}

private def parsePumpMode(val) {
    def values = [
        "off",         // 0
        "speed1",      // 1
        "speed2",      // 2
        "speed3",      // 3
        "quickclean",  // 4
    ]

    return values[val.toInteger()]
}

private def parseControllerState(val) {
    def values = [
        "Idle",          // 0
        "Running",       // 1
        "PreInject",     // 2
        "Inject",        // 3
        "PostInject",    // 4
    ]

    return values[val.toInteger()]
}


def setPoolPumpControlModeManual() {
	//log.debug "SetPoolPumpControlModeManual"
	return sendEvent(
                name:   "PoolPumpControlMode",
                value:  "Manual"
            )
}

def setPoolPumpControlModeAuto() {
	//log.debug "SetPoolPumpControlModeAuto"
	return sendEvent(
                name:   "PoolPumpControlMode",
                value:  "Auto"
            )
}

def setChlorinePumpOn() {
	log.debug "Executing 'setChlorinePumpOn'"
    return writeDataValue("ChlorinePumpOn", true);
}

def setChlorinePumpOff() {
	log.debug "Executing 'setChlorinePumpOff'"
    return writeDataValue("ChlorinePumpOn", false);
}

def setChlorinePumpSetpoint(value) {
	log.debug "Executing 'setChlorinePumpSetpoint'"
    return writeDataValue("ChlorinePumpSetpoint", value);
}


def setChlorinePumpTime(value) {
	log.debug "Executing 'setChlorinePumpTime'"
    return writeDataValue("ChlorinePumpTime", value);
}

def setPoolPumpOff() {
	log.debug "Executing 'setPoolPumpOff'"
    return writeDataValue("PoolPumpMode", 0);
}

def setPoolPumpSpeed1() {
	log.debug "Executing 'setPoolPumpSpeed1'"
    return writeDataValue("PoolPumpMode", 1);
}

def setPoolPumpSpeed2() {
	log.debug "Executing 'setPoolPumpSpeed2'"
    return writeDataValue("PoolPumpMode", 2);
}

def setPoolPumpSpeed3() {
	log.debug "Executing 'setPoolPumpSpeed3'"
    return writeDataValue("PoolPumpMode", 3);
}

def setPoolPumpQuickClean() {
	log.debug "Executing 'setPoolPumpQuickClean'"
    return writeDataValue("PoolPumpMode", 4);
}

def setOffline() {
    log.debug "Executing 'setOffline'"
    
    sendEvent(name: "ChlorinePumpOn", value: "offline", descriptionText: "The device is offline")
    return writeDataValue("ChlorinePumpOn", false);
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
