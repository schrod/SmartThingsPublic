/**
 *  Copyright 2016
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
 *  Thermostat Mode Controller
 *
 *  Author: D. Schroeder
 */
definition(
    name: "Thermostat Mode Controller",
    namespace: "schroeder",
    author: "D. Schroeder",
    description: "Control the thermostat mode based on temperature",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the thermostat... "){
		input "thermostat", "capability.thermostat", title: "Thermostat", multiple: false
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
    section("Set the desired swing..."){
		input "swing", "decimal", title: "Set Swing"
	}
    section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
	}
    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification when mode chages?"
    }
}

def installed()
{
	log.debug "Installing Thermostat Mode Controller"
	subscribe(sensor, "temperature", temperatureHandler)        
    schedule("42 0/5 * * * ?", checkMode)    
    state.mode = "off"
}

def updated()
{
	log.debug "Updating Thermostat Mode Controller"
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
}

def temperatureHandler(evt)
{
    def currentTemp = sensor.currentTemperature    
	log.debug "temperatureHandler ${currentTemp}, ${setpoint}, ${swing}"
    if (mode == "cool") {
		// air conditioner
		if (currentTemp - setpoint >= swing) {			
        	setThermostat("cool")            
		}
		else if (setpoint - currentTemp >= swing) {
        	setThermostat("off")
		}
	}
	else {
		// heater
		if (setpoint - currentTemp >= swing) {
			setThermostat("heat")
		}
		else if (currentTemp - setpoint >= swing) {
			setThermostat("off")
		}
	}
}

def checkMode() {
	def currentMode = thermostat.latestValue("thermostatMode") as String
	log.debug "Checking thermostat mode ${currentMode} ${state.mode}"
	if (state.mode != null && currentMode != state.mode)
    {
    	log.debug "Thermostat mode not correct, retrying"
        setThermostat(state.mode);
    }
}

def setThermostat(newMode) {
	if (thermostat.latestValue("thermostatMode") != newMode) {
        log.debug "Setting Thermostat mode to: $newMode"
        if (sendPush) {
            sendPush("Setting thermostat to ${newMode}")
        }

        state.mode = newMode

        if (newMode == "off") {
            thermostat.off()
        } else if (newMode == "cool") {    	
            thermostat.cool()
        } else if (newMode == "heat") {
            thermostat.heat()
        }
	}
}
    