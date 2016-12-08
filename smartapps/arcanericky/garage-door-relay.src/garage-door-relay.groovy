/**
 *  Garage Door Relay
 *
 *  Copyright 2016 Ricky Pike
 *
 */
definition(
    name: "Garage Door Relay",
    namespace: "arcanericky",
    author: "Ricky Pike",
    description: "Link a switch to a garage door opener. Especially for use with Google Home.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Open garage door when this switch is flipped on") {
        input "theswitch", "capability.switch", required: true
    }
    
    section("Open this garage door") {
        input "thedoor", "capability.garageDoorControl", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(theswitch, "switch.on", switchOnHandler)
    subscribe(theswitch, "switch.off", switchOffHandler)
    
    subscribe(thedoor, "door", doorHandler)
}

def switchOnHandler(evt) {
	log.debug "switchOnHandler called: $evt"
    logCurrentState()
	log.debug "Opening garage door"
    thedoor.close()
}

def switchOffHandler(evt) {
	log.debug "switchOffHandler called: $evt"
	logCurrentState()
	log.debug "Closing garage door"
    thedoor.close()
}

def logCurrentState() {
	def currentState = thedoor.currentState("door")
    log.debug "Garage door is currently " + currentState.value
}

def doorHandler() {
	logCurrentState()
}