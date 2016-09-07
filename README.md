This component is the android library connecting to the classic Bluetooth. 
This library provides the following features.
	1. Retrieve bluetooth-based senor MAC address which is prepopulated in the database. MAC addresses are fetched from the Android in JSON format.
	
	2. Creates a bluetooth socket based on the sensor MAC address. Android creates a bluetooth socket as many as the number of sensors. 
	
	3. Receives sensor data.
	
	4. When disconnected, it closes all the resources (socket, input/output stream) related to the bluetooth.
	
	5. Tries to reconnect every 10 minutes if bluetooth is available.
	
	6. If available, reconnect is done.
