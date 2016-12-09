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
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_outlet@3x.png")


preferences {
    section("Open garage door when this switch is flipped on") {
        input "theswitch", "capability.switch", required: true
    }
    
    section("Open this garage door") {
        input "thedoor", "capability.garageDoorControl", required: true
        //input "thedoor", "capability.doorControl", required: true
    }
    
    
    section("Detect garage door left open?"){
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["No", "Yes"], required: true, defaultValue: "No"
        }
        
		input "detectOpenTimeout", "number", title: "Garage door left open timeout", required: false, defaultValue: 10
	}
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	log.debug "sendPushMessage is " + sendPushMessage
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
    
    detectDoorState()
}

// ========
// Handlers
// ========

def switchOnHandler(evt) {
	log.trace "switchOnHandler()"
    logCurrentState()
	log.debug "Opening garage door"
    openGarageDoor()
}

def switchOffHandler(evt) {
	log.trace "switchOffHandler()"
	logCurrentState()
	log.debug "Closing garage door"
    closeGarageDoor();
}

def doorHandler(evt) {
	log.trace "doorHandler()"
	logCurrentState()
    
    if (evt.value == "closed") {
    	log.debug "DEBUG Garage door is closed"
    } else if (evt.value == "open") {
    	log.debug "DEBUG Garage door is open"
        detectDoorState()
    }
}

// =======

def logCurrentState() {
	log.trace ("logCurrentState()")
    
	def currentState = "unknown"
	def theDoorState = thedoor.currentState("door")
    
    if (theDoorState != null) {
    	currentState = theDoorState.value
    }
    
    log.debug "Garage door is currently " + currentState
}

def checkDoor() {
	log.trace("checkDoor()")
    
    if (thedoor.currentState("door").value == "closed") {
    	log.debug "Garage door still open"
        
        if (sendPushMessage != "No") {
        	log.debug("Sending push message")
        	//sendPush("Garage door left open")
    	}
    }
}

def closeGarageDoor() {
	thedoor.close()
}

def openGarageDoor() {
	thedoor.open()
}

def detectDoorState() {
	log.trace ("detectDoorState()")
    
    if (sendPushMessage != "No") {
	    if (thedoor.currentState("door").value == "open") {
    		log.debug "Garage door is open"
        	runIn(detectOpenTimeout, checkDoor)
    	} else {
        	log.debug "Garage door is closed"
        }
    } else {
    	log.debug "Push messages turned off"
    }
}