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
        input("theswitch", "capability.switch", required: true)
    }
    
    section("Open this garage door") {
        input("thedoor", "capability.garageDoorControl", required: true)
    }

    section("Detect garage door left open?") {
        input("recipients", "contact", title: "Send notifications to") {
            input("sendPushMessage", "enum", title: "Send a push notification?", options: ["No", "Yes"], required: true, defaultValue: "No")
        }
        
		input("detectOpenTimeout", "number", title: "Garage door left open timeout", required: false, defaultValue: 30)
	}
}

def installed() {
	log.debug("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	log.debug("Updated with settings: ${settings}")

	unsubscribe()

    syncSwitch()
    
	initialize()
    
    //detectDoorState()
}

def initialize() {
	log.trace("initialize()")
    
    subscribe(thedoor, "door.open", doorOpenHandler)
    subscribe(thedoor, "door.closed", doorClosedHandler)
    subscribe(thedoor, "door.opening", doorOpeningHandler)
    subscribe(thedoor, "door.closing", doorClosingHandler)
    subscribe(thedoor, "door.unknown", doorUnknownHandler)
    
	subscribe(theswitch, "switch.on", switchOnHandler)
    subscribe(theswitch, "switch.off", switchOffHandler)
}

def syncSwitch() {
	log.trace("syncSwitch()")
    
    def switchState = theswitch.currentState("switch").value
    def garageDoorState = getGarageDoorState()
    
    log.debug ("Switch state is: " + switchState + ". Garage door state is: " + garageDoorState)
    
    if (garageDoorState == "open" && switchState != "on") {
    	log.debug("Syncing switch to open")
        
        theswitch.on()
    } else if (garageDoorState == "closed" && switchState != "off") {
    	log.debug("Syncing switch to closed")
    	
        theswitch.off()
    }
}

// ========
// Handlers
// ========

def switchOnHandler(evt) {
	log.trace "switchOnHandler()"
    
    if (getGarageDoorState() != "open") {
		log.debug("Opening garage door")
    	openGarageDoor()
        runIn(2, validateGarageOpening)
    } else {
    	log.debug("Garage door already open. Not opening.")
    }
}

def switchOffHandler(evt) {
	log.trace("switchOffHandler()")
    
    if (getGarageDoorState() != "closed") {
		log.debug("Closing garage door")
    	closeGarageDoor();
        runIn(2, validateGarageClosing)
    } else {
    	log.debug("Garage door already closed. Not closing.")
    }
}

def doorOpenHandler(evt) {
	log.trace("doorOpenHandler()")
    
    syncSwitch()
    
    detectDoorState()
}

def doorClosedHandler(evt) {
	log.trace("doorClosedHandler()")
    
    syncSwitch()
}

def doorOpeningHandler(evt) {
	log.trace("doorOpeningHandler()")
}

def doorClosingHandler(evt) {
	log.trace("doorClosingHandler()")
}

def doorUnknownHandler(evt) {
	log.trace("doorUnknownHandler()")
}

// =======

def closeGarageDoor() {
	log.trace("closeGarageDoor()")
    
	thedoor.close()
}

def openGarageDoor() {
	log.trace("openGarageDoor()")

	thedoor.open()
}

def detectDoorState() {
	log.trace("detectDoorState()")
    
    if (getGarageDoorState() == "open" && settings.detectOpenTimeout != null) {
   		log.debug("Submitting open check in " + settings.detectOpenTimeout)
       	runIn(settings.detectOpenTimeout, checkDoor)
   	}
}

def checkDoor() {
	log.trace("checkDoor()")
    
    if (thedoor.currentState("door").value == "open") {
    	log.debug("Garage door still open")
        log.debug("sendPushMessage is " + settings.sendPushMessage)
        if (settings.sendPushMessage != "No") {
        	log.debug("Sending push message")
        	sendPush("Relay: Garage door left open")
    	} else {
        	log.debug("Push messages disabled")
        }
    }
}

def getGarageDoorState() {
	log.trace("getGarageDoorState()")
    
	def currentState = "unknown"
	def theDoorState = thedoor.currentState("door")
    
    if (theDoorState != null) {
    	currentState = theDoorState.value
    }
    
    log.debug("Garage door is currently " + currentState)
    
    return currentState
}

def validateGarageOpening() {
	log.trace("validateGarageOpening()")
    
    validateGarageState("opening")
}

def validateGarageClosing() {
	log.trace("validateGarageClosing()")
    
    validateGarageState("closing")
}

def validateGarageState(state) {
	log.trace("validateGarageState(\"" + state + "\")" )
    
    if (getGarageDoorState() != state) {
    	sendPush("Relay: Garage door not " + state)
    } else {
    	log.debug("Garage door is now " + state)
    }
}