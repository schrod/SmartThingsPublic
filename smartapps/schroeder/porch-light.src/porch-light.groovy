definition(
    name: "Porch Light",
    namespace: "Schroeder",
    author: "David Schroeder",
    description: "Turn porch light on for 5 minutes if motion is detected after sunset",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When it opens..."){
		input "motion", "capability.motionSensor"
        input "switch1", "capability.contactSensor"
	}
	section("Turn on a lightfor 5 minutes..."){
		input "light", "capability.switch"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(motion, "motion.active", motionHandler)
    subscribe(switch1, "contact.open", motionHandler)
    subscribe(motion, "motion.inactive", motionInactiveHandler)
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(motion, "motion", motionHandler)
    subscribe(switch1, "contact.open", motionHandler)
}

def motionHandler(evt) {
	if(isNight()) {
        light.on()
        //def fiveMinuteDelay = 60 * 5
        //runIn(fiveMinuteDelay, turnOffLight)
    }
}

def motionInactiveHandler(evt) {
	if(isNight()) {
        def fiveMinuteDelay = 60 * 3
        runIn(fiveMinuteDelay, turnOffLight)
    }
}

def turnOffLight() {
	light.off()
}

def isNight() {
	def s = getSunriseAndSunset(zipCode : "06231")

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	//log.debug "riseTime: $riseTime"
	//log.debug "setTime: $setTime"
    //log.debug "nowTime: $now"

	if(now.after(setTime) || now.before(riseTime)) {
    	log.debug "is Night"
    	return true
    } else {
        log.debug "Not night"
    	return false
    }
}