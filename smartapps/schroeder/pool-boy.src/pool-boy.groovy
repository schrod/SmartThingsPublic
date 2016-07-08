/**
 *  Pool Boy
 *
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
 */
definition(
    name: "Pool Boy",
    namespace: "Schroeder",
    author: "David Schroeder",
    description: "Pool Pump and Chlorine addtition scheduler",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "Pool Size"
}

preferences {
	
    section("Operation") {
        input "enabled", "boolean", title: "Enabled?"
        input "schedule", "text", title: "Schedule"
        input "injectTime", "time", title: "Chlorine Time"
        input "injectAmount", "decimal", title: "Chlorine Amount (ppm)"
    }
    
    section("Setup") {    
		input "poolController", "device.PoolController", title: "Pool Controller"
        input "tempSensor", "capability.temperatureMeasurement", title: "Temperature Sensor"
        input "lightSensor", "capability.illuminanceMeasurement", title: "Light Sensor"
        input "pumpEnergyUse", "capability.energyMeter", title: "Pump Feedback"
        input "pumpFeedback", "capability.powerMeter", title: "Pump Feedback"
        
        input "poolSize", "number", title: "Pool Size", defaultValue: "25000"
        input "concentration", "decimal", title: "Chlorine Concentration (%)", defaultValue: "12.5"
        input "electricRate", "decimal", title: "Electric Rage(\$/kWh)", defaultValue: "0.203"        
	}
    
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.avgTemp = 0
    state.avgLux = 0
    state.sunIsUp = true;
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
   
    subscribe(tempSensor, "temperatureMeasurement.temperature", temperatureHandler)
    subscribe(poolController, "PoolPumpMode", poolPumpModeHandler)
    subscribe(location, "sunset", sunsetHandler)
	subscribe(location, "sunrise", sunriseHandler)
    
    
    if (state.tempReadings == null) {
    	state.tempReadings = []
    }
    if (state.luxReadings == null) {
    	state.luxReadings = []
    }
    
    parseSchedule(schedule)
    log.debug("schedule ${state.schedule}")
    
    //Schedule updates
    //runEvery15Minutes(mainLoop)
    schedule("42 0/15 * * * ?", mainLoop)
    
    //Scheduled Injection
    schedule(injectTime, chlorineInjectionHandler)
}

def mainLoop() {
	if (state.sunIsUp) {    	
        state.tempReadings << tempSensor?.currentTemperature  //add temp to array of temperatures 
        state.avgTemp = state.tempReadings.sum()/state.tempReadings.size()                
        
        state.luxReadings << lightSensor?.currentIlluminance        
        state.avgLux = state.luxReadings.sum()/state.luxReadings.size();
        
        log.debug "average lux: ${String.format("%.2f",state.avgLux)}"
        log.debug "average temp: ${String.format("%.2f",state.avgTemp)}"
    }
    
    //assuming cronalogical order..
    def pumpControlMode = poolController.currentState("PoolPumpControlMode").value
    //log.debug("pump control mode: ${pumpControlMode}")
    if (enabled && pumpControlMode == "Auto") {
    	def time = getNow().toInteger();
    	def thisEntry = -2
        //log.debug "Time now: ${time}"
        state.schedule.eachWithIndex { entry, idx ->
        	if (thisEntry == -2 && entry.time.toInteger() > time) {
            	thisEntry = idx - 1
                //log.debug "Setting thisEntry ${entry.time} ${time} ${idx}"
            }        	
        }
		if (thisEntry == -2) {
        	//too early take last entry value
            //log.debug "Taking last entry: ${state.schedule.size()} ${state.schedule.getClass()} ${state.schedule}"
            thisEntry = state.schedule.size()-1
        }
        //log.debug "Schedule Entry: ${state.schedule[thisEntry]}"
        
        def pumpMode = poolController.currentState("PoolPumpMode").value
        //log.debug "Pool Pump Mode: ${pumpMode}"
        CheckPowerFeedback(pumpMode);
        
        switch (state.schedule[thisEntry].speed) {
        	case "OFF":
            	if (pumpMode != "off") {
                	log.debug("Turning off Pool Pump")
                	poolController.setPoolPumpOff()
                }
                break;
            case "SPEED1":
            	if (pumpMode != "speed1") {
                	log.debug("Setting Pool Pump to Speed 1")
                	poolController.setPoolPumpSpeed1()
                }
                break;
			case "SPEED2":
            	if (pumpMode != "speed2") {
                log.debug("Setting Pool Pump to Speed 2")
                	poolController.setPoolPumpSpeed2()
                }
                break;
            case "SPEED3":
            	if (pumpMode != "speed3") {
                	log.debug("Setting Pool Pump to Speed 3")
                	poolController.setPoolPumpSpeed3()
                }
                break;
            case "CLEAN":
            	if (pumpMode != "quickclean") {
                	log.debug("Setting Pool Pump to Quick Clean")
                	poolController.setPoolPumpQuickClean()
                }
                break;
            default:
            	log.warn("unknown pump speed: ${state.schedule[thisEntry].speed}")
    	}
    }
}

def getNow() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    //log.debug(cal.toString());
    def now = (cal.get(Calendar.HOUR_OF_DAY)*100) + cal.get(Calendar.MINUTE)
    return now;
}


def sendPoolNotification(message) {
    // check that contact book is enabled and recipients selected
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(message, recipients)
    } else if (phone) { // check that the user did select a phone number
        sendSmsMessage(phone, message)
    }
}

def CheckPowerFeedback(String pumpMode) {
	def pumpPower = pumpFeedback.currentState("power").value.toInteger()
    log.debug "Pump Feedback reported power = ${pumpPower}"
    //confirm power feedback matches commanded value
    def feedbackOk = true
    def message = "Warning: Pool Pump Power feedback (${pumpPower}) doesn't match mode (${pumpMode})"
   
    switch (pumpMode) {
     	case "off":       
         	feedbackOk = (pumpPower < 10)
             break
         case "speed1":	//1100?
         	feedbackOk = (pumpPower > 110 && pumpPower < 150)
            log.debug "feedbackOk ${feedbackOk}"            
            break
         case "speed2":  //1800?
         	feedbackOk = (pumpPower > 290 && pumpPower < 310)
            break
         case "speed3":  //2400
         	feedbackOk = (pumpPower > 1100 && pumpPower < 1400)
            break
         case "quickclean": //3450?
         	feedbackOk = (pumpPower > 1400)
            break
         default:
         	feedbackOk = false
            message = "Failed to get pump speed feedback, Pool Controller may be offline"
            break
    }
    
    //log.debug "CheckPowerFeedback ${pumpMode} ${feedbackOk}"
    
    if (!feedbackOk) {
    	if (state.feedbackErrorReportSent != true) {
        	state.feedbackErrorReportSent = true;
            sendPoolNotification(message)
            log.warn(message)
        }
    }
    else 
    {
    	state.feedbackErrorReportSent = false;
    }
}

def temperatureHandler(evt) {
	log.debug "temperatureHandler ${evt.name}"
}

def poolPumpModeHandler(evt) {
	log.debug "poolPumpModeHandler ${evt.name} ${evt.value}"
    state.poolPumpMode = evt.value
}

def sunsetHandler(evt) {
	log.debug "sunsetHandler ${evt.name}"
    state.sunIsUp = false;
    
	ReportDailyStatistics()
}

def sunriseHandler(evt) {
	log.debug "sunriseHandler ${evt.name}"
    state.sunIsUp = true;
    state.tempReadings = []
    state.luxReadings = []
}

def ReportDailyStatistics() {
	def energy = pumpEnergyUse.currentEnergy
    log.debug "current energy: ${energy}"
    def lastReportedEnergy = state.lastReportedEnergy
    if (lastReportedEnergy == null) {
    	lastReportedEnergy = 0
    }
    def energyUsedToday = energy //- lastReportedEnergy
    log.debug "daily energy: ${energyUsedToday}"
    state.lastReportedEnergy = energy
    
    def energyCost = energy * electricRate.toFloat();
    log.debug "Daily cost: ${energyCost}"
        
    def message = "PB Reports: AveTemp: ${String.format("%.1f F", state.avgTemp)} AveLux: ${String.format("%.1f", state.avgLux)} Energy Usage ${String.format("%.1f kWh", energyUsedToday)} (\$${energyCost.round(2)})"
    
    sendPoolNotification(message)
}
    
private def parseSchedule(String schedule) {
	state.schedule = []
	def parts = schedule.split(',')    
    parts.each { part ->    
		part = part.trim()        
        def subPart = part.split(' ')
        def entry = [
        	time: subPart[0],
            speed: subPart[1]
        ]
        log.debug("entry: ${entry}")
        state.schedule << entry
    }
}

def calculateChlorineNeeded() {
	//http://www.in.gov/isdh/files/Chemical_adjustment_pool.pdf
	def ozPerPPM = 1.3/(concentration.toFloat()/100)
    def ClToAdd = ozPerPPM * (poolSize.toFloat()/10000) * injectAmount.toFloat()
    log.debug "Calcuated Liquid Chlorine to add: ${String.format("%.2f", ClToAdd)}"
    return ClToAdd
}

def chlorineInjectionHandler() {
	log.debug("Running chlorineInjectionHandler()")
    
    def pumpPower = pumpFeedback.currentState("power").value
    
    if (pumpPower < 10) {
    	sendPoolNotification("Warning pump apears to be off, skipping Cl Injection")
    } else {
        def amountToAdd = calculateChlorineNeeded();

        //Todo - convert this to time on the injection pump.
        def pumpTime = 30

        poolController.setChlorinePumpTime(pumpTime) //run for 30 seconds
        poolController.setChlorinePumpOn()

        sendPoolNotification("Injecting ${String.format("%.2f", amountToAdd)} oz of CL by running pump for ${pumpTime}s")
    }
}