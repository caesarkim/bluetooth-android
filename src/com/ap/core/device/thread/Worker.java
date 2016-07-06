/**
 * AgingPlace
 */
package com.ap.core.device.thread;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public abstract class Worker extends Thread {
	
	public void stopWorker() {
		//
	}
	
	public void cancel() {
		this.interrupt();
	}
}
