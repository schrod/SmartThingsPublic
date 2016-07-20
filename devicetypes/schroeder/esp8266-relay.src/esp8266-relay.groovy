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
 *	ESP8266 Based Relay Controller / Humidity and Temp Sensor
 *
 *	Author: D. Schroeder
 *	Date: 2016-01-19
 */
 
 
metadata {
    definition (name: "ESP8266 Relay", namespace: "schroeder", author: "Dave Schroeder") {
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        
        attribute "currentIP", "string"

 }

 // simulator metadata
 simulator {}

// UI tile definitions
tiles(scale: 2) {
        //Not really a lighting type, but the secondary controll only seams to work in lighting
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 3){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                 attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.off", backgroundColor:"#79b821", nextState:"Off"
                 attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.on", backgroundColor:"#ffffff", nextState:"On"
                 attributeState "offline", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ff0000"
 			}
            tileAttribute ("currentIP", key: "SECONDARY_CONTROL") {
             	 attributeState("default", label: '${currentValue}', unit:"")
 			}
        }

        standardTile("switch", "device.switch", width: 2, height: 2,
             canChangeIcon: true) {
            state "off", label: '${name}', action: "switch.on",
                  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${name}', action: "switch.off",
                  icon: "st.switches.switch.on",  backgroundColor: "#00E600"
            state "offline", label: '${name}',
                  icon: "st.switches.switch.off", backgroundColor: "#FF0000"     
        }
          
          
		valueTile("temperature", "device.temperature", width: 2, height: 2){
            state "temperature", label: '${currentValue}°F', unit:"",
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

        valueTile("humidity", "device.humidity", width: 2, height: 2){
            state "humidity", label: '${currentValue}%', unit:"",
            	backgroundColors: [
                    [value: 0, color: "#202040"],
                    [value: 100, color: "#202080"]
                ]
		}
		

		standardTile("refresh", "device.switch", inactiveLabel: false, height: 2, width: 2, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main(["switch"])
        details(["rich-control", "temperature", "humidity", "refresh"])
	}
}

def listMethod() {
	log.debug "listMethod"
}

def updateMethod() {
	log.debug "updateMethod"
}

def postMethod() {
	log.debug "postMethod"
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    
    unschedule("setOffline")
    
	def msg = parseLanMessage(description)
    def json = msg.json
    def result = []
    
    result << creatEvent(name: "temperature", value: json.temp)
    result << creatEvent(name: "humidity", value: json.humidity)
    
    return result
    
    /*
	def headerString = msg.header

	if (!headerString) {
		log.debug "headerstring was null for some reason :("
    }
	def result = []
	def bodyString = msg.body
    def value = "";
	if (bodyString) {
        log.debug bodyString
        // default the contact and motion status to closed and inactive by default
        def allContactStatus = "closed"
        def allMotionStatus = "inactive"
        def json = msg.json;
        json?.house?.door?.each { door ->
            value = door?.status == 1 ? "open" : "closed"
            log.debug "${door.name} door status ${value}"
            // if any door is open, set contact to open
            if (value == "open") {
				allContactStatus = "open"
			}
			result << creatEvent(name: "temperature", value: allContactStatus)
        }
		//result << createEvent(name: "motion", value: allMotionStatus)
    }
    
    return result
    */
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

def refresh() {
	log.debug "esp8266 refresh" 
    
    sendEvent(name: "currentIP", value: getHostAddress())        
    sendEvent(name: "temperature", value: new Random().nextInt(100))
    sendEvent(name: "humidity", value: new Random().nextInt(100))
    sendEvent(name: "switch", value: "on")  
    
    log.debug device.getDataValue('subscriptionId')
    
    log.debug "Executing 'poll' ${getHostAddress()}"       
    poll()
    
    def result = subscribeToUpdateEvents()
    result
}

def poll() {
	log.debug "esp8266 poll"    
	if (device.currentValue("currentIP") != "Offline")
    	runIn(30, setOffline)
        
/*
	log.debug "Executing 'poll' ${getHostAddress()}"    
	setDeviceNetworkId(ip,port)
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/getstatus",
    	headers: [
        	HOST: "${getHostAddress()}"
    	]
	)
    */
}

def on() {
	log.debug "Executing 'on'"
    
}

def off() {
	log.debug "Executing 'off'"
}

def setOffline() {
	//sendEvent(name: "currentIP", value: "Offline", displayed: false)
    log.debug "Executing 'setOffline'"
    sendEvent(name: "switch", value: "offline", descriptionText: "The device is offline")
}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

def subscribeToUpdateEvents() {
  log.debug "Subscribe to the update events here"
  def result = []
  try {
    result << subscribeAction("/espEvent")
  } catch(e) {
    log.debug "Hit Exception $e subscribe"
  }
  result
}

private subscribeAction(path, callbackPath="") {
	log.trace "subscribe($path, $callbackPath)"
	def address = getCallBackAddress()
	def ip = getHostAddress()

	def result = new physicalgraph.device.HubAction(
		method: "SUBSCRIBE",
		path: path,
		headers: [
			HOST: ip,
			CALLBACK: "<http://${address}/notify$callbackPath>",
			NT: "upnp:event",
			TIMEOUT: "Second-28800"])
            

	log.trace "SUBSCRIBE $path $result"
	//log.trace "\n${result.action.encodeAsHTML()}"
	result
}

def lanResponseHandler(evt) {
	log.debug "I got back ${evt.name}"
  /*def descMap = parseDescriptionAsMap(evt.name)
  if (descMap.containsKey("body")) {
    def body = parseBase64Json(descMap["body"])
    if (body.containsKey("status")) {
      sendEvent(name: "contact", value: body.status)
    }*/
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	//device.deviceNetworkId = "$iphex:$porthex"
  	//log.debug "Device Network Id set to ${iphex}:${porthex}"
}

/*private getHostAddress() {
	return "${ip}:${port}"
}*/


private String convertHexToIP(hex) {
 	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
 	def ip = getDataValue("ip")
 	def port = getDataValue("port")
 	if (!ip || !port) {
 		def parts = device.deviceNetworkId.split(":")
 		if (parts.length == 2) {
 			ip = parts[0]
 			port = parts[1]
 		} else {
 			log.warn "Can't figure out ip and port for device: ${device.id}"
		 }
 	}
 	log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
 	return convertHexToIP(ip) + ":" + convertHexToInt(port)
    //return device.deviceNetworkId;
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
